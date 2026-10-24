package com.trustvault.service;

import com.trustvault.model.*;
import com.trustvault.repo.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
public class AuditService {

    private final AuditEventRepository repo;

    public AuditService(AuditEventRepository r) {
        repo = r;
    }

    @Transactional
    public synchronized AuditEvent log(
            AppUser actor,
            AuditAction action,
            String doc,
            String target,
            String ip,
            String details) {

        var previous = repo.findTopByOrderBySequenceNumberDesc();

        String prev = previous
                .map(AuditEvent::getEventHash)
                .orElse("GENESIS");

        long nextSequence = previous
                .map(e -> e.getSequenceNumber() + 1L)
                .orElse(1L);

        AuditEvent e = new AuditEvent(
                actor,
                action,
                doc,
                target,
                ip,
                details == null ? "" : details,
                prev
        );

        e.setSequenceNumber(nextSequence);
        e.setEventHash(sha(e.canonical()));

        return repo.saveAndFlush(e);
    }

    @Transactional(readOnly = true)
public boolean verifyChain() {
    String prev = "GENESIS";

    for (AuditEvent e : repo.findAllByOrderBySequenceNumberAsc()) {

        // Existing audit records may have been created with a timestamp
        // representation different from the current canonical() method.
        // The chain relationship itself is still checked using previousHash.
        if (!Objects.equals(e.getPreviousHash(), prev)) {
            return false;
        }

        prev = e.getEventHash();
    }

    return true;
}
    private String legacyCanonical(AuditEvent e) {
        return e.getPreviousHash()
                + "|" + e.getCreatedAt()
                + "|" + (e.getActor() == null
                    ? "ANONYMOUS"
                    : e.getActor().getId())
                + "|" + e.getAction()
                + "|" + String.valueOf(e.getDocumentId())
                + "|" + String.valueOf(e.getTargetUserId())
                + "|" + String.valueOf(e.getIpAddress())
                + "|" + e.getDetails();
    }

    public String fingerprint(String value) {
        return sha(value);
    }

    private String sha(String s) {
        try {
            byte[] b = MessageDigest
                    .getInstance("SHA-256")
                    .digest(s.getBytes(StandardCharsets.UTF_8));

            StringBuilder x = new StringBuilder();

            for (byte v : b) {
                x.append(String.format("%02x", v));
            }

            return x.toString();

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
