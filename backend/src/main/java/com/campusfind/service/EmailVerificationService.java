package com.campusfind.service;

import com.campusfind.entity.CampusClaim;
import com.campusfind.entity.CampusItem;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.time.LocalDateTime;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificationService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_LIFETIME_MINUTES = 10;
    private static final int PASSWORD_RESET_LIFETIME_MINUTES = 30;

    private final JavaMailSender mailSender;
    private final BCryptPasswordEncoder passwordEncoder;
    private final String fromAddress;
    private final String frontendUrl;

    public EmailVerificationService(
            JavaMailSender mailSender,
            BCryptPasswordEncoder passwordEncoder,
            @Value("${campusfind.mail.from:}") String fromAddress,
            @Value("${campusfind.frontend.url:http://localhost:5173}") String frontendUrl) {
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
        this.fromAddress = fromAddress;
        this.frontendUrl = frontendUrl;
    }

    public String createCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    public String hashCode(String code) {
        return passwordEncoder.encode(code);
    }

    public boolean matches(String code, String codeHash) {
        return codeHash != null && passwordEncoder.matches(code, codeHash);
    }

    public LocalDateTime expiryTime() {
        return LocalDateTime.now().plusMinutes(OTP_LIFETIME_MINUTES);
    }

    public String createPasswordResetToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public LocalDateTime passwordResetExpiryTime() {
        return LocalDateTime.now().plusMinutes(PASSWORD_RESET_LIFETIME_MINUTES);
    }

    public String hashPasswordResetToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    public void sendPasswordResetLink(String email, String token) throws MessagingException {
        String baseUrl = frontendUrl.replaceAll("/+$", "");
        String resetLink = baseUrl + "/?resetToken=" + token;
        sendMessage(email, "Reset your Campus Find password",
                "We received a request to reset your Campus Find password.\n\n"
                + "Open this link to choose a new password:\n" + resetLink
                + "\n\nThis link expires in 30 minutes. If you did not request a reset, ignore this email.");
    }

    public void sendCode(String email, String code) throws MessagingException {
        sendMessage(email, "Campus Find email verification",
                "Your Campus Find verification code is " + code
                        + ". It expires in 10 minutes. If you did not request this, ignore this email.");
    }

    public void sendClaimHandoffNotifications(CampusClaim claim) throws MessagingException {
        CampusItem item = claim.getItem();
        LinkedHashSet<String> recipients = new LinkedHashSet<>();
        recipients.add(claim.getClaimantEmail());
        if (item.getReporterEmail() != null && !item.getReporterEmail().isBlank()) {
            recipients.add(item.getReporterEmail());
        }

        for (String recipient : recipients) {
            sendMessage(recipient, "Campus Find: Campus Security reviewed an item match",
                    "Campus Security reviewed a possible match for \""
                    + item.getTitle() + "\".\n\n"
                    + "Please coordinate the handoff at the Campus Security desk. "
                    + "Your private verification details were not included in this email. "
                    + "For safety, do not share passwords, verification codes, or sensitive "
                    + "personal information.");
        }
    }

    public void sendSecurityReportEmail(String recipient, String subject, String body)
            throws MessagingException {
        sendMessage(recipient, subject, body);
    }

    private void sendMessage(String recipient, String subject, String text)
            throws MessagingException {
        String sender = fromAddress;
        if (sender == null || sender.isBlank()) {
            throw new IllegalStateException("SMTP_FROM or SMTP_USERNAME must be configured.");
        }
        String destination = recipient;
        if (destination == null || destination.isBlank()) {
            throw new IllegalArgumentException("Email recipient must be configured.");
        }
        String mailSubject = subject;
        if (mailSubject == null || mailSubject.isBlank()) {
            throw new IllegalArgumentException("Email subject must be configured.");
        }
        String mailText = text;
        if (mailText == null) {
            throw new IllegalArgumentException("Email content must be configured.");
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
        helper.setFrom(sender);
        helper.setTo(destination);
        helper.setSubject(mailSubject);
        helper.setText(mailText);
        mailSender.send(message);
    }
}