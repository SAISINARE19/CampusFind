package com.campusfind.service;

import com.campusfind.entity.Student;
import com.campusfind.repository.StudentRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class StudentSessionService {
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final int TOKEN_BYTES = 32;

  private final StudentRepository students;

  public StudentSessionService(StudentRepository students) {
    this.students = students;
  }

  public String issue(Student student) {
    byte[] tokenBytes = new byte[TOKEN_BYTES];
    RANDOM.nextBytes(tokenBytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    student.setSessionTokenHash(hash(token));
    student.setSessionExpiresAt(LocalDateTime.now().plusHours(12));
    students.save(student);
    return token;
  }

  public Optional<Student> authenticate(String authorizationHeader) {
    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      return Optional.empty();
    }
    String token = authorizationHeader.substring("Bearer ".length()).trim();
    if (token.isEmpty()) {
      return Optional.empty();
    }
    return students.findBySessionTokenHash(hash(token))
        .filter(student -> student.getSessionExpiresAt() != null
            && student.getSessionExpiresAt().isAfter(LocalDateTime.now()));
  }

  public static String hash(String token) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available.", exception);
    }
  }
}
