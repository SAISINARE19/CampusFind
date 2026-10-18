package com.campusfind.entity;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity @Table(name="campus_items")
public class CampusItem {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=140) private String title;
 @Column(nullable=false) private String type;
 @Column(nullable=false) private String category;
 @Column(nullable=false) private LocalDate date;
 @Column(nullable=false,length=180) private String location;
 @Column(nullable=false,length=2000) private String description;
 @Column(nullable=false) private String status="PENDING";
 private String reporter;
 @Column(name="reporter_email", length=254)
 private String reporterEmail;
 private String imageUrl;
 @Column(nullable=false) private LocalDateTime createdAt=LocalDateTime.now();
 public Long getId(){return id;} public String getTitle(){return title;} public void setTitle(String v){title=v;}
 public String getType(){return type;} public void setType(String v){type=v;}
 public String getCategory(){return category;} public void setCategory(String v){category=v;}
 public LocalDate getDate(){return date;} public void setDate(LocalDate v){date=v;}
 public String getLocation(){return location;} public void setLocation(String v){location=v;}
 public String getDescription(){return description;} public void setDescription(String v){description=v;}
 public String getStatus(){return status;} public void setStatus(String v){status=v;}
 public String getReporter(){return reporter;} public void setReporter(String v){reporter=v;}
 @JsonIgnore public String getReporterEmail(){return reporterEmail;} public void setReporterEmail(String v){reporterEmail=v;}
 public String getImageUrl(){return imageUrl;} public void setImageUrl(String v){imageUrl=v;}
 public LocalDateTime getCreatedAt(){return createdAt;}
}