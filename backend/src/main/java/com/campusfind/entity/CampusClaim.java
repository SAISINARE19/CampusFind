package com.campusfind.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "campus_claims")
public class CampusClaim {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "item_id", nullable = false)
  private CampusItem item;

  @Column(nullable = false, length = 30)
  private String kind;

  @Column(name = "claimant_name", nullable = false, length = 100)
  private String claimantName;

  @Column(name = "claimant_email", nullable = false, length = 254)
  private String claimantEmail;

  @Column(name = "verification_details", nullable = false, length = 2000)
  private String verificationDetails;

  @Column(name = "found_date")
  private LocalDate foundDate;

  @Column(name = "found_location", length = 180)
  private String foundLocation;

  @Column(nullable = false, length = 20)
  private String status = "PENDING";

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "reviewed_at")
  private LocalDateTime reviewedAt;

  @Column(name = "reviewed_by", length = 254)
  private String reviewedBy;

  public Long getId() { return id; }
  public CampusItem getItem() { return item; }
  public void setItem(CampusItem value) { item = value; }
  public String getKind() { return kind; }
  public void setKind(String value) { kind = value; }
  public String getClaimantName() { return claimantName; }
  public void setClaimantName(String value) { claimantName = value; }
  public String getClaimantEmail() { return claimantEmail; }
  public void setClaimantEmail(String value) { claimantEmail = value; }
  public String getVerificationDetails() { return verificationDetails; }
  public void setVerificationDetails(String value) { verificationDetails = value; }
  public LocalDate getFoundDate() { return foundDate; }
  public void setFoundDate(LocalDate value) { foundDate = value; }
  public String getFoundLocation() { return foundLocation; }
  public void setFoundLocation(String value) { foundLocation = value; }
  public String getStatus() { return status; }
  public void setStatus(String value) { status = value; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public LocalDateTime getReviewedAt() { return reviewedAt; }
  public void setReviewedAt(LocalDateTime value) { reviewedAt = value; }
  @JsonIgnore
  public String getReviewedBy() { return reviewedBy; }
  public void setReviewedBy(String value) { reviewedBy = value; }
}
