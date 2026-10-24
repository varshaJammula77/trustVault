package com.trustvault.model;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="app_users")
public class AppUser {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private String id;
 @Column(nullable=false,unique=true) private String email;
 @Column(nullable=false) private String passwordHash;
 @Column(nullable=false) private String fullName;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private Role role;
 @Column(nullable=false) private boolean enabled=true;
 @Column(nullable=false) private Instant createdAt=Instant.now();
 public AppUser(){}
 public AppUser(String email,String passwordHash,String fullName,Role role){this.email=email;this.passwordHash=passwordHash;this.fullName=fullName;this.role=role;}
 public String getId(){return id;} public String getEmail(){return email;} public void setEmail(String v){email=v;} public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String v){passwordHash=v;} public String getFullName(){return fullName;} public void setFullName(String v){fullName=v;} public Role getRole(){return role;} public void setRole(Role v){role=v;} public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;} public Instant getCreatedAt(){return createdAt;}
}
