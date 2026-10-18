package com.campusfind.service;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.util.Locale;
@Component
public class MesEmailPolicy {
 private final String securityAdminEmail;
 public MesEmailPolicy(@Value("${campusfind.security.admin-email:}") String securityAdminEmail){
  this.securityAdminEmail=securityAdminEmail.trim().toLowerCase(Locale.ROOT);
 }
 public boolean isValid(String email){
  if(email==null)return false;
  String normalized=email.trim().toLowerCase(Locale.ROOT);
  return normalized.matches("^[a-z0-9._%+-]+@(?:student\\.)?mes\\.ac\\.in$")
      || (!securityAdminEmail.isBlank() && normalized.equals(securityAdminEmail));
 }
 public boolean isSecurityAdmin(String email){
  return email!=null && !securityAdminEmail.isBlank()
      && securityAdminEmail.equals(email.trim().toLowerCase(Locale.ROOT));
 }
}