package com.campusfind.controller;

import com.campusfind.entity.Student;
import com.campusfind.repository.StudentRepository;
import com.campusfind.service.EmailVerificationService;
import com.campusfind.service.MesEmailPolicy;
import com.campusfind.service.StudentSessionService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private static final Logger log = LoggerFactory.getLogger(AuthController.class);

  private final StudentRepository students;
  private final MesEmailPolicy policy;
  private final EmailVerificationService verification;
  private final BCryptPasswordEncoder encoder;
  private final StudentSessionService sessions;

  public AuthController(
      StudentRepository students,
      MesEmailPolicy policy,
      EmailVerificationService verification,
      BCryptPasswordEncoder encoder,
      StudentSessionService sessions) {
    this.students = students;
    this.policy = policy;
    this.verification = verification;
    this.encoder = encoder;
    this.sessions = sessions;
  }

  public record RegisterRequest(
      @NotBlank @Size(max = 100) String name,
      @NotBlank @Email String email,
      @NotBlank @Size(min = 8, max = 100) String password) {}

  public record LoginRequest(
      @NotBlank @Email String email,
      @NotBlank @Size(min = 8, max = 100) String password) {}

  public record PasswordResetRequest(@NotBlank @Email String email) {}

  public record PasswordResetCompletionRequest(
      @NotBlank String token,
      @NotBlank @Size(min = 8, max = 100) String password) {}

  public record EmailRequest(@NotBlank @Email String email) {}

  public record VerifyRequest(
      @NotBlank @Email String email,
      @NotBlank @Pattern(regexp = "[0-9]{6}") String code) {}

  @PostMapping("/register")
  public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
    String email = request.email().trim().toLowerCase();
    if (!policy.isValid(email)) {
      return ResponseEntity.badRequest().body(Map.of(
          "message", "Only @mes.ac.in and @student.mes.ac.in email addresses are allowed."));
    }

    Student student = students.findByEmailIgnoreCase(email).orElse(null);
    if (student != null && student.isEmailVerified()) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
          "message", "An account with this email already exists."));
    }
    if (student == null) {
      student = new Student();
      student.setName(request.name().trim());
      student.setEmail(email);
      student.setPasswordHash(encoder.encode(request.password()));
    }
    student.setRole(policy.isSecurityAdmin(email) ? "SECURITY" : "STUDENT");

    return issueCode(student, HttpStatus.ACCEPTED);
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
    String email = request.email().trim().toLowerCase();
    Student student = students.findByEmailIgnoreCase(email).orElse(null);
    if (student == null || !policy.isValid(email)
        || !encoder.matches(request.password(), student.getPasswordHash())) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
          "message", "Email or password is incorrect."));
    }
    if (!student.isEmailVerified()) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
          "message", "Verify your email before signing in."));
    }
    student.setRole(policy.isSecurityAdmin(email) ? "SECURITY" : "STUDENT");
    String token = sessions.issue(student);
    return ResponseEntity.ok(Map.of(
        "name", student.getName(),
        "email", student.getEmail(),
        "role", student.getRole(),
        "token", token));
  }

  @PostMapping("/logout")
  public ResponseEntity<?> logout(
      @org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false)
      String authorizationHeader) {
    sessions.authenticate(authorizationHeader).ifPresent(student -> {
      student.setSessionTokenHash(null);
      student.setSessionExpiresAt(null);
      students.save(student);
    });
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<?> forgotPassword(@Valid @RequestBody PasswordResetRequest request) {
    String email = request.email().trim().toLowerCase();
    Student student = students.findByEmailIgnoreCase(email).orElse(null);
    if (student == null || !student.isEmailVerified() || !policy.isValid(email)) {
      return ResponseEntity.ok(Map.of(
          "message", "If a verified account exists for that email, a reset link will be sent."));
    }

    String token = verification.createPasswordResetToken();
    student.setPasswordResetTokenHash(verification.hashPasswordResetToken(token));
    student.setPasswordResetExpiresAt(verification.passwordResetExpiryTime());
    students.save(student);

    try {
      verification.sendPasswordResetLink(student.getEmail(), token);
    } catch (MailException | MessagingException | IllegalStateException exception) {
      log.warn("Failed to send password reset email.", exception);
      student.setPasswordResetTokenHash(null);
      student.setPasswordResetExpiresAt(null);
      students.save(student);
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
          "message", "Could not send the password reset email. Check mail settings and try again."));
    }

    return ResponseEntity.ok(Map.of(
        "message", "If a verified account exists for that email, a reset link will be sent."));
  }

  @PostMapping("/reset-password")
  public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordResetCompletionRequest request) {
    Student student = students.findByPasswordResetTokenHash(
        verification.hashPasswordResetToken(request.token())).orElse(null);
    if (student == null || student.getPasswordResetExpiresAt() == null
        || !student.getPasswordResetExpiresAt().isAfter(LocalDateTime.now())) {
      return ResponseEntity.badRequest().body(Map.of(
          "message", "This password reset link is invalid or expired. Request a new one."));
    }

    student.setPasswordHash(encoder.encode(request.password()));
    student.setPasswordResetTokenHash(null);
    student.setPasswordResetExpiresAt(null);
    student.setSessionTokenHash(null);
    student.setSessionExpiresAt(null);
    students.save(student);
    return ResponseEntity.ok(Map.of("message", "Your password has been changed. You can now sign in."));
  }

  @PostMapping("/resend-code")
  public ResponseEntity<?> resendCode(@Valid @RequestBody EmailRequest request) {
    String email = request.email().trim().toLowerCase();
    if (!policy.isValid(email)) {
      return ResponseEntity.badRequest().body(Map.of(
          "message", "Only @mes.ac.in and @student.mes.ac.in email addresses are allowed."));
    }

    Student student = students.findByEmailIgnoreCase(email).orElse(null);
    if (student == null || student.isEmailVerified()) {
      return ResponseEntity.ok(Map.of(
          "message", "If the address has a pending account, a new code will be sent."));
    }
    return issueCode(student, HttpStatus.OK);
  }

  @PostMapping("/verify")
  public ResponseEntity<?> verify(@Valid @RequestBody VerifyRequest request) {
    String email = request.email().trim().toLowerCase();
    Student student = students.findByEmailIgnoreCase(email).orElse(null);
    if (student == null || !policy.isValid(email)) {
      return invalidCode();
    }
    if (student.isEmailVerified()) {
      return ResponseEntity.ok(Map.of("message", "Email is already verified."));
    }

    if (student.getVerificationAttempts() >= 5) {
      student.setVerificationCodeHash(null);
      student.setVerificationExpiresAt(null);
      students.save(student);
      return invalidCode();
    }

    LocalDateTime expiresAt = student.getVerificationExpiresAt();
    if (expiresAt == null || !expiresAt.isAfter(LocalDateTime.now())) {
      student.setVerificationCodeHash(null);
      student.setVerificationExpiresAt(null);
      students.save(student);
      return invalidCode();
    }
    if (!verification.matches(request.code(), student.getVerificationCodeHash())) {
      student.setVerificationAttempts(student.getVerificationAttempts() + 1);
      students.save(student);
      return invalidCode();
    }

    student.setEmailVerified(true);
    student.setVerificationCodeHash(null);
    student.setVerificationExpiresAt(null);
    student.setVerificationAttempts(0);
    student.setRole(policy.isSecurityAdmin(email) ? "SECURITY" : "STUDENT");
    String token = sessions.issue(student);
    students.save(student);
    return ResponseEntity.ok(Map.of(
        "message", "Email verified. Your account is confirmed.",
        "name", student.getName(),
        "email", student.getEmail(),
        "role", student.getRole(),
        "token", token));
  }

  private ResponseEntity<?> issueCode(Student student, HttpStatus successStatus) {
    LocalDateTime sentAt = student.getVerificationSentAt();
    if (sentAt != null && sentAt.isAfter(LocalDateTime.now().minusMinutes(1))) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
          "message", "Wait one minute before requesting another verification code."));
    }

    String code = verification.createCode();
    student.setVerificationCodeHash(verification.hashCode(code));
    student.setVerificationExpiresAt(verification.expiryTime());
    student.setVerificationSentAt(LocalDateTime.now());
    student.setVerificationAttempts(0);
    students.save(student);

    try {
      verification.sendCode(student.getEmail(), code);
    } catch (MailException | MessagingException | IllegalStateException exception) {
      log.warn("Failed to send email verification message.", exception);
      student.setVerificationCodeHash(null);
      student.setVerificationExpiresAt(null);
      student.setVerificationSentAt(null);
      students.save(student);
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
          "message", "Could not send the verification email. Check SMTP settings and try again."));
    }

    return ResponseEntity.status(successStatus).body(Map.of(
        "message", "Verification code sent. It expires in 10 minutes."));
  }

  private ResponseEntity<?> invalidCode() {
    return ResponseEntity.badRequest().body(Map.of(
        "message", "The verification code is invalid or expired. Request a new code and try again."));
  }
}