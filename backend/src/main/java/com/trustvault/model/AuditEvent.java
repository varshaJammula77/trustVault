package com.trustvault.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "audit_events",
    indexes = {
        @Index(name = "idx_audit_time", columnList = "createdAt"),
        @Index(name = "idx_audit_sequence", columnList = "sequenceNumber")
    }
)
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false, unique = true, updatable = false)
    private Long sequenceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    private AppUser actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    private String documentId;
    private String targetUserId;
    private String ipAddress;

    @Column(nullable = false, columnDefinition = "text")
    private String details;

    @Column(nullable = false)
    private String previousHash;

    @Column(nullable = false)
    private String eventHash;

    public AuditEvent() {
    }

    public AuditEvent(
            AppUser actor,
            AuditAction action,
            String documentId,
            String targetUserId,
            String ip,
            String details,
            String previousHash) {

        this.createdAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        this.actor = actor;
        this.action = action;
        this.documentId = documentId;
        this.targetUserId = targetUserId;
        this.ipAddress = ip;
        this.details = details == null ? "" : details;
        this.previousHash = previousHash;
    }

    public String canonical() {
        return previousHash
                + "|" + createdAt.toEpochMilli()
                + "|" + (actor == null ? "ANONYMOUS" : actor.getId())
                + "|" + action
                + "|" + String.valueOf(documentId)
                + "|" + String.valueOf(targetUserId)
                + "|" + String.valueOf(ipAddress)
                + "|" + details;
    }

    public String getId() {
        return id;
    }

    public void setSequenceNumber(Long n) {
        this.sequenceNumber = n;
    }

    public Long getSequenceNumber() {
        return sequenceNumber;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public AppUser getActor() {
        return actor;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getDetails() {
        return details;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public String getEventHash() {
        return eventHash;
    }

    public void setEventHash(String h) {
        this.eventHash = h;
    }
}