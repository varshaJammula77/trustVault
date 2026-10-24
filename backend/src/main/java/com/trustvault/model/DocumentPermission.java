package com.trustvault.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "document_permissions",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"document_id", "user_id"}
    )
)
public class DocumentPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PermissionLevel permission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "granted_by_id", nullable = false)
    private AppUser grantedBy;

    @Column(nullable = false)
    private Instant grantedAt;

    public DocumentPermission() {
    }

    public DocumentPermission(
            Document document,
            AppUser user,
            PermissionLevel permission,
            AppUser grantedBy) {

        this.document = document;
        this.user = user;
        this.permission = permission;
        this.grantedBy = grantedBy;
        this.grantedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public Document getDocument() {
        return document;
    }

    public AppUser getUser() {
        return user;
    }

    public PermissionLevel getPermission() {
        return permission;
    }

    public AppUser getGrantedBy() {
        return grantedBy;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public void setPermission(PermissionLevel permission) {
        this.permission = permission;
    }
}