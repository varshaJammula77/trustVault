package com.trustvault.controller;

import com.trustvault.model.*;
import com.trustvault.repo.*;
import com.trustvault.service.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@RestController @RequestMapping("/api/admin")
public class AdminController {
    private final AuditEventRepository auditRepo; private final AuditService audit; private final AppUserRepository users; private final AuthorizationService auth;
    public AdminController(AuditEventRepository r,AuditService a,AppUserRepository u,AuthorizationService x){auditRepo=r;audit=a;users=u;auth=x;}
    private void admin(Authentication a){if(auth.user(a.getName()).getRole()!=Role.ADMIN)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Admin only");}

    @GetMapping("/audit")
    @Transactional(readOnly = true)
    public Map<String,Object> audit(Authentication a){
        admin(a);
        return Map.of("chainValid",audit.verifyChain(),"events",auditRepo.findTop500ByOrderBySequenceNumberDesc().stream().map(e -> { Map<String,Object> m=new LinkedHashMap<>(); m.put("id",e.getId()); m.put("sequence",Objects.toString(e.getSequenceNumber(),"")); m.put("createdAt",e.getCreatedAt()); m.put("actor",e.getActor()==null?"ANONYMOUS":e.getActor().getEmail()); m.put("action",e.getAction()); m.put("documentId",Objects.toString(e.getDocumentId(),"")); m.put("targetUserId",Objects.toString(e.getTargetUserId(),"")); m.put("ipAddress",Objects.toString(e.getIpAddress(),"")); m.put("details",e.getDetails()); m.put("hash",e.getEventHash()); return m; }).toList());
    }

    @GetMapping("/security-summary")
    @Transactional(readOnly = true)
    public Map<String,Object> summary(Authentication a){
        admin(a);
        Instant since=Instant.now().minus(24,java.time.temporal.ChronoUnit.HOURS);
        long denied=auditRepo.findAllByOrderBySequenceNumberAsc().stream().filter(e->e.getCreatedAt().isAfter(since)&&e.getAction()==AuditAction.PERMISSION_DENIED).count();
        long downloads=auditRepo.findAllByOrderBySequenceNumberAsc().stream().filter(e->e.getCreatedAt().isAfter(since)&&e.getAction()==AuditAction.DOWNLOAD).count();
        long permissionChanges=auditRepo.findAllByOrderBySequenceNumberAsc().stream().filter(e->e.getCreatedAt().isAfter(since)&&(e.getAction()==AuditAction.GRANT_ACCESS||e.getAction()==AuditAction.PERMISSION_CHANGED||e.getAction()==AuditAction.REVOKE_ACCESS)).count();
        return Map.of("chainValid",audit.verifyChain(),"users",users.count(),"enabledUsers",users.findAll().stream().filter(AppUser::isEnabled).count(),"denied24h",denied,"downloads24h",downloads,"permissionChanges24h",permissionChanges,"totalAuditEvents",auditRepo.count());
    }

    @GetMapping("/users")
    public List<Map<String,Object>> users(Authentication a){
        admin(a);
        return users.findAll().stream().map(u -> {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("email", u.getEmail());
            m.put("fullName", u.getFullName());
            m.put("role", u.getRole());
            m.put("enabled", u.isEnabled());
            return m;
        }).toList();
    }
}
