package com.trustvault;

import com.trustvault.repo.AuditEventRepository;
import com.trustvault.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuditTamperTest {
    @Autowired AuditEventRepository events;
    @Autowired AuditService audit;

    @Test
    void changingHistoricalEventHashBreaksVerification() {
        var first = events.findAllByOrderBySequenceNumberAsc().get(0);
        String original = first.getEventHash();
        assertTrue(audit.verifyChain());
        first.setEventHash("0".repeat(64));
        events.saveAndFlush(first);
        assertFalse(audit.verifyChain());
        first.setEventHash(original);
        events.saveAndFlush(first);
        assertTrue(audit.verifyChain());
    }
}
