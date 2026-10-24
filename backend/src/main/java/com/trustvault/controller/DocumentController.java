package com.trustvault.controller;

import com.trustvault.dto.PermissionRequest;
import com.trustvault.model.*;
import com.trustvault.repo.*;
import com.trustvault.service.*;
import com.trustvault.storage.FileStorage;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentRepository docs;
    private final DocumentPermissionRepository perms;
    private final AuthorizationService auth;
    private final AuditService audit;
    private final FileStorage storage;
    private final AppUserRepository users;

    public DocumentController(
            DocumentRepository d,
            DocumentPermissionRepository p,
            AuthorizationService a,
            AuditService au,
            FileStorage s,
            AppUserRepository u) {
        docs = d;
        perms = p;
        auth = a;
        audit = au;
        storage = s;
        users = u;
    }

    private AppUser me(Authentication a) {
        return auth.user(a.getName());
    }

    @GetMapping
    @Transactional
    public List<Map<String, Object>> list(
            Authentication a,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        AppUser u = me(a);

        audit.log(
                u,
                AuditAction.SEARCH,
                null,
                null,
                null,
                "Search name=" + name +
                        " category=" + category +
                        " owner=" + owner
        );

        Instant fromDate = parseDate(from);
        Instant toDate = parseDate(to);
        if (fromDate != null && toDate != null && !fromDate.isBefore(toDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The 'from' date must be earlier than the 'to' date");
        }
        // Apply the owner filter for every role; the repository authorization predicate
        // still limits results to documents this user is allowed to discover.
        String ownerFilter = blank(owner);
        return docs.search(
                u.getId(),
                u.getRole() == Role.ADMIN,
                blank(name),
                blank(category),
                ownerFilter,
                fromDate,
                toDate
        ).stream().map(d -> view(d, u)).toList();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public Map<String, Object> upload(
            Authentication a,
            @RequestParam MultipartFile file,
            @RequestParam String name,
            @RequestParam String category) throws Exception {

        AppUser u = me(a);

        if (u.getRole() == Role.VIEWER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Viewers cannot upload"
            );
        }

        String cleanName = name == null ? "" : name.trim();
        String cleanCategory = category == null ? "" : category.trim();

        if (cleanName.isBlank()
                || cleanName.length() > 180
                || cleanCategory.isBlank()
                || cleanCategory.length() > 80) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Name/category is invalid"
            );
        }

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        if (file.getSize() > 10485760) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds the 10MB limit");
        }

        String detectedType = detectContentType(file);
        if (detectedType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported or malformed file. Upload a valid PDF, PNG, JPEG, TXT or DOCX file.");
        }

        Document d = new Document();
        d.setName(cleanName);
        d.setCategory(cleanCategory);
        d.setOwner(u);
        d.setContentType(detectedType);

        var saved = storage.save(
                file,
                UUID.randomUUID().toString()
        );

        d.setStorageKey(saved.key());
        d.setSizeBytes(saved.size());
        d.setSha256(saved.sha256());

        try {
            d = docs.save(d);
        } catch (RuntimeException ex) {
            // Do not leave an unreferenced private file when metadata persistence fails.
            try { storage.delete(saved.key()); } catch (Exception cleanup) { ex.addSuppressed(cleanup); }
            throw ex;
        }

        audit.log(
                u,
                AuditAction.UPLOAD,
                d.getId(),
                null,
                null,
                "Uploaded " + d.getName() +
                        " sha256=" + d.getSha256()
        );

        return view(d, u);
    }

    @GetMapping("/{id}")
    @Transactional
    public Map<String, Object> get(
            Authentication a,
            @PathVariable String id) {

        AppUser u = me(a);
        Document d = auth.document(id);

        auth.requireRead(u, d);

        audit.log(
                u,
                AuditAction.VIEW,
                id,
                null,
                null,
                "Document metadata viewed"
        );

        return view(d, u);
    }

    @GetMapping("/{id}/download")
    @Transactional
    public ResponseEntity<InputStreamResource> download(
            Authentication a,
            @PathVariable String id) throws Exception {

        AppUser u = me(a);
        Document d = auth.document(id);

        auth.requireRead(u, d);

        audit.log(
                u,
                AuditAction.DOWNLOAD,
                id,
                null,
                null,
                "Document downloaded"
        );

        var r = new InputStreamResource(
                storage.open(d.getStorageKey())
        );

        return ResponseEntity
                .ok()
                .contentType(MediaType.parseMediaType(d.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(d.getName(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .contentLength(d.getSizeBytes())
                .body(r);
    }

    @PutMapping("/{id}")
    @Transactional
    public Map<String, Object> update(
            Authentication a,
            @PathVariable String id,
            @RequestBody Map<String, String> body) {

        AppUser u = me(a);
        Document d = auth.document(id);

        auth.requireEdit(u, d);

        if (body.get("name") != null) {
            String n = body.get("name").trim();

            if (n.isBlank() || n.length() > 180) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid name"
                );
            }

            d.setName(n);
        }

        if (body.get("category") != null) {
            String c = body.get("category").trim();

            if (c.isBlank() || c.length() > 80) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid category"
                );
            }

            d.setCategory(c);
        }

        d.touch();
        docs.save(d);

        audit.log(
                u,
                AuditAction.UPDATE,
                id,
                null,
                null,
                "Metadata updated"
        );

        return view(d, u);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void delete(
            Authentication a,
            @PathVariable String id) throws Exception {

        AppUser u = me(a);
        Document d = auth.document(id);

        auth.requireOwnerOrAdmin(u, d);

        d.setDeleted(true);
        docs.save(d);

        perms.deleteAll(
                perms.findByDocumentId(id)
        );

        storage.delete(d.getStorageKey());

        audit.log(
                u,
                AuditAction.DELETE,
                id,
                null,
                null,
                "Document deleted and active grants removed"
        );
    }

    @GetMapping("/{id}/permissions")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> permissions(
            Authentication a,
            @PathVariable String id) {

        AppUser u = me(a);
        Document d = auth.document(id);

        auth.requireOwnerOrAdmin(u, d);

        return perms.findByDocumentId(id)
                .stream()
                .map(p -> {
                    Map<String, Object> m =
                            new LinkedHashMap<>();

                    m.put("id", p.getId());
                    m.put("userId", p.getUser().getId());
                    m.put("email", p.getUser().getEmail());
                    m.put("fullName", p.getUser().getFullName());
                    m.put("permission", p.getPermission());
                    m.put("grantedAt", p.getGrantedAt());

                    return m;
                })
                .toList();
    }

    @PostMapping("/{id}/permissions")
    @Transactional
    public Map<String, Object> grant(
            Authentication a,
            @PathVariable String id,
            @Valid @RequestBody PermissionRequest r) {

        AppUser u = me(a);
        Document d = auth.document(id);

        auth.requireOwnerOrAdmin(u, d);

        AppUser target = users
                .findById(r.userId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        if (target.getId().equals(d.getOwner().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Owner already has access"
            );
        }

        var existing = perms.findByDocumentIdAndUserId(id, target.getId());
        var p = existing.orElseGet(() -> new DocumentPermission(d, target, r.permission(), u));
        boolean changed = existing.isPresent() && p.getPermission() != r.permission();
        p.setPermission(r.permission());
        p = perms.save(p);

        audit.log(
                u,
                changed ? AuditAction.PERMISSION_CHANGED : AuditAction.GRANT_ACCESS,
                id,
                target.getId(),
                null,
                "Granted " + r.permission() +
                        " to " + target.getEmail() +
                        "; reason=" + r.reason().trim()
        );

        return Map.of(
                "id", p.getId(),
                "userId", target.getId(),
                "email", target.getEmail(),
                "permission", p.getPermission()
        );
    }

    @DeleteMapping("/{id}/permissions/{userId}")
    @Transactional
    public void revoke(
            Authentication a,
            @PathVariable String id,
            @PathVariable String userId) {

        AppUser u = me(a);
        Document d = auth.document(id);

        auth.requireOwnerOrAdmin(u, d);

        if (perms
                .findByDocumentIdAndUserId(id, userId)
                .isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Permission not found"
            );
        }

        perms.deleteByDocumentIdAndUserId(
                id,
                userId
        );

        audit.log(
                u,
                AuditAction.REVOKE_ACCESS,
                id,
                userId,
                null,
                "Access revoked immediately"
        );
    }

    private String detectContentType(MultipartFile file) {
        try {
            byte[] b = file.getBytes();
            String original = Objects.toString(file.getOriginalFilename(), "").toLowerCase(Locale.ROOT);
            if (original.endsWith(".pdf") && b.length >= 5 && b[0]=='%' && b[1]=='P' && b[2]=='D' && b[3]=='F' && b[4]=='-') return "application/pdf";
            if (original.endsWith(".png") && b.length >= 8 && (b[0]&255)==137 && b[1]==80 && b[2]==78 && b[3]==71 && b[4]==13 && b[5]==10 && b[6]==26 && b[7]==10) return "image/png";
            if ((original.endsWith(".jpg") || original.endsWith(".jpeg")) && b.length >= 4 && (b[0]&255)==255 && (b[1]&255)==216 && (b[2]&255)==255 && (b[b.length-2]&255)==255 && (b[b.length-1]&255)==217) return "image/jpeg";
            if (original.endsWith(".docx") && isDocx(b)) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            if (original.endsWith(".txt")) {
                for (byte value : b) if (value == 0) return null;
                java.nio.charset.StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                        .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                        .decode(java.nio.ByteBuffer.wrap(b));
                return "text/plain";
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isDocx(byte[] bytes) {
        if (bytes.length < 4 || bytes[0] != 'P' || bytes[1] != 'K' || bytes[2] != 3 || bytes[3] != 4) return false;
        boolean contentTypes = false, documentXml = false;
        try (java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(bytes))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("[Content_Types].xml".equals(entry.getName())) contentTypes = true;
                if ("word/document.xml".equals(entry.getName())) documentXml = true;
                if (contentTypes && documentXml) return true;
            }
        } catch (Exception ignored) { return false; }
        return false;
    }

    private Map<String, Object> view(
            Document d,
            AppUser u) {

        PermissionLevel permission =
                auth.permissionFor(u, d);

        Map<String, Object> m =
                new LinkedHashMap<>();

        m.put("id", d.getId());
        m.put("name", d.getName());
        m.put("category", d.getCategory());
        m.put("ownerId", d.getOwner().getId());
        m.put("owner", d.getOwner().getFullName());
        m.put("contentType", d.getContentType());
        m.put("sizeBytes", d.getSizeBytes());
        m.put("sha256", d.getSha256());
        m.put("createdAt", d.getCreatedAt());
        m.put("updatedAt", d.getUpdatedAt());
        m.put(
                "myPermission",
                permission == null
                        ? "NONE"
                        : permission.name()
        );
        m.put(
                "canEdit",
                auth.canEdit(u, d)
        );

        return m;
    }

    private String blank(String s) {
        return s == null || s.isBlank()
                ? null
                : s;
    }

    private Instant parseDate(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(s);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid date; use ISO-8601"
            );
        }
    }
}