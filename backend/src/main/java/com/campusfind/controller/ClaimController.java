package com.campusfind.controller;

import com.campusfind.entity.CampusClaim;
import com.campusfind.entity.CampusItem;
import com.campusfind.entity.Student;
import com.campusfind.repository.CampusClaimRepository;
import com.campusfind.repository.CampusItemRepository;
import com.campusfind.service.EmailVerificationService;
import com.campusfind.service.StudentSessionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import jakarta.mail.MessagingException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {
  private static final Logger log = LoggerFactory.getLogger(ClaimController.class);
  private final CampusClaimRepository claims;
  private final CampusItemRepository items;
  private final StudentSessionService sessions;
  private final EmailVerificationService email;

  public ClaimController(
      CampusClaimRepository claims,
      CampusItemRepository items,
      StudentSessionService sessions,
      EmailVerificationService email) {
    this.claims = claims;
    this.items = items;
    this.sessions = sessions;
    this.email = email;
  }

  public record SubmissionRequest(
      @NotNull Long itemId,
      @NotBlank String kind,
      @NotBlank @Size(min = 20, max = 2000) String verificationDetails,
      LocalDate foundDate,
      @Size(max = 180) String foundLocation) {}

  public record ReviewRequest(@NotBlank String decision) {}

  public record SubmissionResponse(Long id, String status, String message) {}

  @PostMapping
  public ResponseEntity<?> submit(
      @Valid @RequestBody SubmissionRequest request,
      @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
    Student student = sessions.authenticate(authorizationHeader).orElse(null);
    if (student == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("message", "Please sign in before submitting."));
    }

    String kind = request.kind().trim().toUpperCase(Locale.ROOT);
    if (!List.of("CLAIM", "FOUND_NOTICE").contains(kind)) {
      return ResponseEntity.badRequest()
          .body(Map.of("message", "Submission type must be CLAIM or FOUND_NOTICE."));
    }

    Long itemId = request.itemId();
    if (itemId == null) {
      return ResponseEntity.badRequest().body(Map.of("message", "An item id is required."));
    }
    CampusItem item = items.findById(itemId).orElse(null);
    if (item == null) {
      return ResponseEntity.notFound().build();
    }
    if (!"APPROVED".equalsIgnoreCase(item.getStatus())) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(Map.of("message", "Submissions are only accepted for approved reports."));
    }
    String itemType = item.getType().toUpperCase(Locale.ROOT);
    if (("CLAIM".equals(kind) && !"FOUND".equals(itemType))
        || ("FOUND_NOTICE".equals(kind) && !"LOST".equals(itemType))) {
      return ResponseEntity.badRequest().body(Map.of(
          "message", "Claims are for found-item reports; found notices are for lost-item reports."));
    }
    if (item.getReporterEmail() != null
        && item.getReporterEmail().equalsIgnoreCase(student.getEmail())) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(Map.of("message", "You cannot submit a match for your own report."));
    }

    String details = request.verificationDetails().trim();
    if (details.length() < 20) {
      return ResponseEntity.badRequest().body(Map.of(
          "message", "Add at least 20 characters of useful private verification details."));
    }
    LocalDate foundDate = request.foundDate();
    String foundLocation = request.foundLocation() == null
        ? null : request.foundLocation().trim();
    if ("FOUND_NOTICE".equals(kind)) {
      if (foundDate == null || foundLocation == null || foundLocation.isBlank()) {
        return ResponseEntity.badRequest().body(Map.of(
            "message", "Enter when and where you found the item."));
      }
      if (foundDate.isAfter(LocalDate.now())) {
        return ResponseEntity.badRequest()
            .body(Map.of("message", "The found date cannot be in the future."));
      }
    } else {
      foundDate = null;
      foundLocation = null;
    }

    if (claims.existsByItem_IdAndClaimantEmailIgnoreCaseAndKindIgnoreCaseAndStatusIgnoreCase(
        item.getId(), student.getEmail(), kind, "PENDING")) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(Map.of("message", "You already have a pending submission for this report."));
    }

    CampusClaim claim = new CampusClaim();
    claim.setItem(item);
    claim.setKind(kind);
    claim.setClaimantName(student.getName());
    claim.setClaimantEmail(student.getEmail().trim().toLowerCase(Locale.ROOT));
    claim.setVerificationDetails(details);
    claim.setFoundDate(foundDate);
    claim.setFoundLocation(foundLocation);
    CampusClaim saved = claims.save(claim);

    return ResponseEntity.status(HttpStatus.CREATED).body(new SubmissionResponse(
        saved.getId(), saved.getStatus(),
        "Sent privately to Campus Security for review. No contact details are shown publicly."));
  }

  @GetMapping("/pending")
  public ResponseEntity<?> pending(
      @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
    Student securityUser = securityUser(authorizationHeader);
    if (securityUser == null) {
      return forbiddenOrUnauthorized(authorizationHeader);
    }
    return ResponseEntity.ok(claims.findByStatusIgnoreCaseOrderByCreatedAtDesc("PENDING"));
  }

  @PatchMapping("/{id}/review")
  public ResponseEntity<?> review(
      @PathVariable Long id,
      @Valid @RequestBody ReviewRequest request,
      @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
    if (id == null) {
      return ResponseEntity.badRequest().body(Map.of("message", "Invalid submission id."));
    }
    Student securityUser = securityUser(authorizationHeader);
    if (securityUser == null) {
      return forbiddenOrUnauthorized(authorizationHeader);
    }
    String decision = request.decision().trim().toUpperCase(Locale.ROOT);
    if (!List.of("APPROVED", "REJECTED").contains(decision)) {
      return ResponseEntity.badRequest()
          .body(Map.of("message", "Decision must be APPROVED or REJECTED."));
    }

    CampusClaim claim = claims.findById(id).orElse(null);
    if (claim == null) {
      return ResponseEntity.notFound().build();
    }
    if (!"PENDING".equalsIgnoreCase(claim.getStatus())) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(Map.of("message", "This submission has already been reviewed."));
    }

    claim.setStatus(decision);
    claim.setReviewedAt(LocalDateTime.now());
    claim.setReviewedBy(securityUser.getEmail());
    CampusClaim reviewed = claims.save(claim);
    String message = "APPROVED".equals(decision)
        ? "Submission approved. Handoff notification sent to the parties."
        : "Submission rejected.";

    if ("APPROVED".equals(decision)) {
      try {
        email.sendClaimHandoffNotifications(reviewed);
        if (reviewed.getItem().getReporterEmail() == null
            || reviewed.getItem().getReporterEmail().isBlank()) {
          message = "Submission approved and claimant notified. The report has no saved owner "
              + "email; please coordinate with the reporter through Campus Security.";
        }
      } catch (MailException | MessagingException | IllegalStateException exception) {
        log.warn("Could not send claim handoff notifications for submission {}.", id, exception);
        message = "Submission approved, but notification email could not be delivered. "
            + "Please contact the parties through Campus Security.";
      }
    }
    return ResponseEntity.ok(Map.of(
        "id", reviewed.getId(), "status", reviewed.getStatus(), "message", message));
  }

  private Student securityUser(String authorizationHeader) {
    return sessions.authenticate(authorizationHeader)
        .filter(student -> "SECURITY".equals(student.getRole()))
        .orElse(null);
  }

  private ResponseEntity<Map<String, String>> forbiddenOrUnauthorized(
      String authorizationHeader) {
    if (sessions.authenticate(authorizationHeader).isEmpty()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("message", "Please sign in with the configured campus security account."));
    }
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(Map.of("message", "Campus security access is required."));
  }
}
