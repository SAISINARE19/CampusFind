package com.campusfind.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="students", uniqueConstraints=@UniqueConstraint(columnNames="email"))
public class Student {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private String name;
 @Column(nullable=false, unique=true) private String email;
 @Column(nullable=false) private String passwordHash;
 @Column(nullable=false) private String role="STUDENT";
 @Column(nullable=false) private LocalDateTime createdAt=LocalDateTime.now();
 @Column(nullable=false, columnDefinition="BOOLEAN NOT NULL DEFAULT FALSE") private boolean emailVerified;
 private String verificationCodeHash;
 private LocalDateTime verificationExpiresAt;
 @Column(nullable=false, columnDefinition="INTEGER NOT NULL DEFAULT 0") private int verificationAttempts;
 private LocalDateTime verificationSentAt;
 private String passwordResetTokenHash;
 private LocalDateTime passwordResetExpiresAt;
 private String sessionTokenHash;
 private LocalDateTime sessionExpiresAt;
 public Long getId(){return id;} public String getName(){return name;} public void setName(String v){name=v;}
 public String getEmail(){return email;} public void setEmail(String v){email=v;}
 public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String v){passwordHash=v;}
 public String getRole(){return role;} public void setRole(String v){role=v;}
 public LocalDateTime getCreatedAt(){return createdAt;}
 public boolean isEmailVerified(){return emailVerified;} public void setEmailVerified(boolean v){emailVerified=v;}
 public String getVerificationCodeHash(){return verificationCodeHash;} public void setVerificationCodeHash(String v){verificationCodeHash=v;}
 public LocalDateTime getVerificationExpiresAt(){return verificationExpiresAt;} public void setVerificationExpiresAt(LocalDateTime v){verificationExpiresAt=v;}
 public int getVerificationAttempts(){return verificationAttempts;} public void setVerificationAttempts(int v){verificationAttempts=v;}
 public LocalDateTime getVerificationSentAt(){return verificationSentAt;} public void setVerificationSentAt(LocalDateTime v){verificationSentAt=v;}
 public String getPasswordResetTokenHash(){return passwordResetTokenHash;} public void setPasswordResetTokenHash(String v){passwordResetTokenHash=v;}
 public LocalDateTime getPasswordResetExpiresAt(){return passwordResetExpiresAt;} public void setPasswordResetExpiresAt(LocalDateTime v){passwordResetExpiresAt=v;}
 public String getSessionTokenHash(){return sessionTokenHash;} public void setSessionTokenHash(String v){sessionTokenHash=v;}
 public LocalDateTime getSessionExpiresAt(){return sessionExpiresAt;} public void setSessionExpiresAt(LocalDateTime v){sessionExpiresAt=v;}
}