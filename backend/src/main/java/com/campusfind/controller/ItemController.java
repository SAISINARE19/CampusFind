package com.campusfind.controller;
import com.campusfind.entity.CampusItem;
import com.campusfind.entity.Student;
import com.campusfind.repository.CampusItemRepository;
import com.campusfind.repository.CampusClaimRepository;
import com.campusfind.service.ItemImageStorageService;
import com.campusfind.service.EmailVerificationService;
import com.campusfind.service.StudentSessionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.mail.MessagingException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
@RestController @RequestMapping("/api/items")
public class ItemController {
 private static final Logger log=LoggerFactory.getLogger(ItemController.class);
 private final CampusItemRepository items;
 private final CampusClaimRepository claims;
 private final StudentSessionService sessions;
 private final ItemImageStorageService imageStorage;
 private final EmailVerificationService email;
 private final ObjectMapper objectMapper;
 public ItemController(CampusItemRepository items,CampusClaimRepository claims,StudentSessionService sessions,ItemImageStorageService imageStorage,EmailVerificationService email,ObjectMapper objectMapper){this.items=items;this.claims=claims;this.sessions=sessions;this.imageStorage=imageStorage;this.email=email;this.objectMapper=objectMapper;}
 public record ItemRequest(@NotBlank @Size(max=140) String title,@NotBlank String type,@NotBlank String category,@NotNull LocalDate date,@NotBlank @Size(max=180) String location,@NotBlank @Size(max=2000) String description,String reporter){}
 public record SecurityItemResponse(Long id,String title,String type,String category,LocalDate date,
     String location,String description,String status,String reporter,String reporterEmail,
     String imageUrl,LocalDateTime createdAt){}
 public record SecurityEmailRequest(
     @NotBlank @Size(max=200) String subject,
     @NotBlank @Size(max=5000) String body){}
 @GetMapping public List<CampusItem> list(){return items.findByStatusIgnoreCaseOrderByCreatedAtDesc("APPROVED");}
 @PostMapping public ResponseEntity<?> create(
     @RequestPart("report") String reportJson,
     @RequestPart(value="image",required=false) MultipartFile image,
     @RequestHeader(value="Authorization",required=false) String authorizationHeader){
  Student student=sessions.authenticate(authorizationHeader).orElse(null);
  if(student==null)return unauthorized();
  ItemRequest req;
  try{
   req=objectMapper.readValue(reportJson,ItemRequest.class);
  }catch(JsonProcessingException exception){
   return ResponseEntity.badRequest().body(Map.of("message","Invalid report data."));
  }
  if(req.title()==null||req.title().isBlank()||req.title().length()>140
      ||req.type()==null||req.category()==null||req.date()==null
      ||req.location()==null||req.location().isBlank()||req.location().length()>180
      ||req.description()==null||req.description().isBlank()||req.description().length()>2000){
   return ResponseEntity.badRequest().body(Map.of("message","Please complete all report fields and check their lengths."));
  }
  if(!List.of("LOST","FOUND").contains(req.type().toUpperCase()))return ResponseEntity.badRequest().body(Map.of("message","Type must be LOST or FOUND."));
  CampusItem item=new CampusItem();item.setTitle(req.title().trim());item.setType(req.type().toUpperCase());item.setCategory(req.category().trim());item.setDate(req.date());item.setLocation(req.location().trim());item.setDescription(req.description().trim());item.setReporter(student.getName());item.setReporterEmail(student.getEmail().trim().toLowerCase(java.util.Locale.ROOT));item.setStatus("PENDING");
  try{
   item.setImageUrl(imageStorage.store(image));
  }catch(IllegalArgumentException exception){
   return ResponseEntity.badRequest().body(Map.of("message",exception.getMessage()));
  }catch(java.io.IOException exception){
   return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message","Could not save the report image."));
  }
  return ResponseEntity.status(HttpStatus.CREATED).body(items.save(item));
 }

 @GetMapping("/pending")
 public ResponseEntity<?> pending(
     @RequestHeader(value="Authorization",required=false) String authorizationHeader){
  Student securityUser=securityUser(authorizationHeader);
  if(securityUser==null)return forbiddenOrUnauthorized(authorizationHeader);
  return ResponseEntity.ok(items.findByStatusIgnoreCaseOrderByCreatedAtDesc("PENDING"));
 }

 @GetMapping("/security")
 public ResponseEntity<?> allForSecurity(
     @RequestHeader(value="Authorization",required=false) String authorizationHeader){
  Student securityUser=securityUser(authorizationHeader);
  if(securityUser==null)return forbiddenOrUnauthorized(authorizationHeader);
  List<SecurityItemResponse> reports=items.findAllByOrderByCreatedAtDesc().stream()
      .map(item->new SecurityItemResponse(item.getId(),item.getTitle(),item.getType(),
          item.getCategory(),item.getDate(),item.getLocation(),item.getDescription(),
          item.getStatus(),item.getReporter(),item.getReporterEmail(),item.getImageUrl(),
          item.getCreatedAt()))
      .toList();
  return ResponseEntity.ok(reports);
 }

 @GetMapping("/mine")
 public ResponseEntity<?> mine(
     @RequestHeader(value="Authorization",required=false) String authorizationHeader){
  Student student=sessions.authenticate(authorizationHeader).orElse(null);
  if(student==null)return unauthorized();
  return ResponseEntity.ok(items.findByReporterEmailIgnoreCaseOrderByCreatedAtDesc(student.getEmail()));
 }

 @PatchMapping("/{id}/resolve")
 public ResponseEntity<?> resolve(
     @PathVariable Long id,
     @RequestHeader(value="Authorization",required=false) String authorizationHeader){
  if(id==null)return ResponseEntity.badRequest().body(Map.of("message","Invalid report id."));
  Student student=sessions.authenticate(authorizationHeader).orElse(null);
  if(student==null)return unauthorized();
  CampusItem item=items.findById(id).orElse(null);
  if(item==null)return ResponseEntity.notFound().build();
  if(item.getReporterEmail()==null||!item.getReporterEmail().equalsIgnoreCase(student.getEmail())){
   return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message","You can only resolve your own reports."));
  }
  if(!"APPROVED".equalsIgnoreCase(item.getStatus())){
   return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message","Only approved reports can be marked as resolved."));
  }
  item.setStatus("RESOLVED");
  return ResponseEntity.ok(items.save(item));
 }

 @DeleteMapping("/{id}/mine")
 public ResponseEntity<?> deleteMine(
     @PathVariable Long id,
     @RequestHeader(value="Authorization",required=false) String authorizationHeader){
  if(id==null)return ResponseEntity.badRequest().body(Map.of("message","Invalid report id."));
  Student student=sessions.authenticate(authorizationHeader).orElse(null);
  if(student==null)return unauthorized();
  CampusItem item=items.findById(id).orElse(null);
  if(item==null)return ResponseEntity.notFound().build();
  if(item.getReporterEmail()==null||!item.getReporterEmail().equalsIgnoreCase(student.getEmail())){
   return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message","You can only delete your own reports."));
  }
  claims.deleteByItem_Id(item.getId());
  items.delete(item);
  deleteImageIfPresent(item);
  return ResponseEntity.noContent().build();
 }

 public record ReviewRequest(@NotBlank String decision){}

 @PatchMapping("/{id}/review")
 public ResponseEntity<?> review(
     @PathVariable Long id,
     @Valid @RequestBody ReviewRequest request,
     @RequestHeader(value="Authorization",required=false) String authorizationHeader){
  if(id==null)return ResponseEntity.badRequest().body(Map.of("message","Invalid report id."));
  Student securityUser=securityUser(authorizationHeader);
  if(securityUser==null)return forbiddenOrUnauthorized(authorizationHeader);
  String decision=request.decision().trim().toUpperCase();
  if(!List.of("APPROVED","REJECTED").contains(decision)){
   return ResponseEntity.badRequest().body(Map.of("message","Decision must be APPROVED or REJECTED."));
  }
  CampusItem item=items.findById(id).orElse(null);
  if(item==null)return ResponseEntity.notFound().build();
  if(!"PENDING".equalsIgnoreCase(item.getStatus())){
   return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message","This report has already been reviewed."));
  }
  item.setStatus(decision);
  return ResponseEntity.ok(items.save(item));
 }

 @DeleteMapping("/{id}/security")
 public ResponseEntity<?> deleteForSecurity(
     @PathVariable Long id,
     @RequestHeader(value="Authorization",required=false) String authorizationHeader){
  if(id==null)return ResponseEntity.badRequest().body(Map.of("message","Invalid report id."));
  Student securityUser=securityUser(authorizationHeader);
  if(securityUser==null)return forbiddenOrUnauthorized(authorizationHeader);
  CampusItem item=items.findById(id).orElse(null);
  if(item==null)return ResponseEntity.notFound().build();
  claims.deleteByItem_Id(item.getId());
  items.delete(item);
  deleteImageIfPresent(item);
  return ResponseEntity.noContent().build();
 }

 @PostMapping("/{id}/security-email")
 public ResponseEntity<?> emailReporter(
     @PathVariable Long id,
     @Valid @RequestBody SecurityEmailRequest request,
     @RequestHeader(value="Authorization",required=false) String authorizationHeader){
  if(id==null)return ResponseEntity.badRequest().body(Map.of("message","Invalid report id."));
  Student securityUser=securityUser(authorizationHeader);
  if(securityUser==null)return forbiddenOrUnauthorized(authorizationHeader);
  CampusItem item=items.findById(id).orElse(null);
  if(item==null)return ResponseEntity.notFound().build();
  String recipient=item.getReporterEmail();
  if(recipient==null||recipient.isBlank()){
   return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
       "message","This report has no saved owner email address, so a message cannot be sent."));
  }
  String subject=request.subject().trim();
  String body=request.body().trim();
  if(subject.contains("\r")||subject.contains("\n")||body.isBlank()){
   return ResponseEntity.badRequest().body(Map.of(
       "message","Enter a subject on one line and a non-empty message."));
  }
  try{
   email.sendSecurityReportEmail(recipient,subject,body);
  }catch(MailException|MessagingException|IllegalStateException exception){
   log.warn("Campus Security could not send an email for report {}.",id,exception);
   return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
       "message","Could not deliver the email. Check the SMTP configuration and try again."));
  }
  return ResponseEntity.ok(Map.of("message","Email sent to the report owner."));
 }

 private void deleteImageIfPresent(CampusItem item){
  try{
   imageStorage.delete(item.getImageUrl());
  }catch(java.io.IOException|IllegalArgumentException exception){
   log.warn("Could not remove image for deleted campus item {}.",item.getId(),exception);
  }
 }

 private Student securityUser(String authorizationHeader){
  return sessions.authenticate(authorizationHeader)
      .filter(student->"SECURITY".equals(student.getRole()))
      .orElse(null);
 }

 private ResponseEntity<Map<String,String>> unauthorized(){
  return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
      .body(Map.of("message","Please sign in to submit an item report."));
 }

 private ResponseEntity<Map<String,String>> forbiddenOrUnauthorized(String authorizationHeader){
  if(sessions.authenticate(authorizationHeader).isEmpty()){
   return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
       .body(Map.of("message","Please sign in with the configured campus security account."));
  }
  return ResponseEntity.status(HttpStatus.FORBIDDEN)
      .body(Map.of("message","Campus security access is required."));
 }
}