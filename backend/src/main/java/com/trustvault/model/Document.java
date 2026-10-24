package com.trustvault.model;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="documents", indexes={@Index(name="idx_doc_name",columnList="name"),@Index(name="idx_doc_category",columnList="category"),@Index(name="idx_doc_created",columnList="createdAt")})
public class Document {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private String id;
 @Column(nullable=false) private String name;
 @Column(nullable=false) private String category;
 @Column(nullable=false) private String storageKey;
 @Column(nullable=false) private String contentType;
 @Column(nullable=false) private long sizeBytes;
 @Column(nullable=false) private String sha256;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) private AppUser owner;
 @Column(nullable=false) private Instant createdAt=Instant.now();
 @Column(nullable=false) private Instant updatedAt=Instant.now();
 @Column(nullable=false) private boolean deleted=false;
 public Document(){}
 public String getId(){return id;} public String getName(){return name;} public void setName(String v){name=v;} public String getCategory(){return category;} public void setCategory(String v){category=v;} public String getStorageKey(){return storageKey;} public void setStorageKey(String v){storageKey=v;} public String getContentType(){return contentType;} public void setContentType(String v){contentType=v;} public long getSizeBytes(){return sizeBytes;} public void setSizeBytes(long v){sizeBytes=v;} public String getSha256(){return sha256;} public void setSha256(String v){sha256=v;} public AppUser getOwner(){return owner;} public void setOwner(AppUser v){owner=v;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;} public void touch(){updatedAt=Instant.now();} public boolean isDeleted(){return deleted;} public void setDeleted(boolean v){deleted=v;touch();}
}
