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

        String previousHash = "GENESIS";
        long expectedSequence = 1L;

        for (AuditEvent e : repo.findAllByOrderBySequenceNumberAsc()) {

            // Sequence must be continuous.
            if (!Objects.equals(e.getSequenceNumber(), expectedSequence)) {
                return false;
            }

            // Each event must point to the previous event's hash.
            if (!Objects.equals(e.getPreviousHash(), previousHash)) {
                return false;
            }

            // Recalculate the actual SHA-256 hash.
            String currentHash = sha(e.canonical());

            // Stored hash must exactly match the calculated hash.
            if (!Objects.equals(e.getEventHash(), currentHash)) {
                return false;
            }

            previousHash = e.getEventHash();
            expectedSequence++;
        }

        return true;
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