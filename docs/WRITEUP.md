# TrustVault — Short Write-up

## What I built

TrustVault is a secure document-sharing platform where document access is controlled at the server and important security decisions are recorded in a tamper-evident audit ledger.

The implementation uses React, Spring Boot, PostgreSQL, JWT/BCrypt and private file storage.

## Permission model

There are three roles:

- **ADMIN** — system/security oversight and administrative activity views.
- **OWNER** — manages documents they own and controls named access.
- **VIEWER** — accesses only documents explicitly granted to them.

At the document level there are two grants:

- `READ` — metadata/download access.
- `EDIT` — read plus metadata modification.

Delete is intentionally owner/admin-only because it is destructive.

Access is a direct user-to-document relationship. I deliberately did not implement folder inheritance because a direct grant is easier to reason about and revoke within a seven-day security-focused build.

## The central security decision

The frontend is never the authorization boundary.

Every protected document operation resolves the current authenticated user and current document permission from the backend/database. Document permissions are not stored in the JWT. This means revocation takes effect on the next request even when the viewer still has a valid token.

## What is distinctive about this implementation

### 1. Reasoned access grants

When an owner grants access, TrustVault requires a short business reason. The reason is stored in the permission-change audit event. This makes the audit trail more useful than simply recording “permission changed”.

### 2. Evidence fingerprints

Uploaded files receive a SHA-256 content fingerprint. The fingerprint is displayed on the document card and recorded in the upload audit event. It gives an operator a simple integrity reference without exposing the private storage key.

### 3. Security Center

Administrators get a small operational view containing audit-chain status, enabled users, recent downloads, recent denied attempts and recent permission changes. These are deliberately derived from the same audit ledger used for investigation.

### 4. Authorization-filtered search

Search is restricted in the server-side query itself. Unauthorized documents are not returned to the browser and therefore cannot leak through metadata enumeration.

## AI usage and correction

AI was used as a development assistant for architecture, implementation ideas, threat modeling, testing and documentation. It was not treated as a security authority.

The most important review pattern was:

`AI suggestion → independent review → attack scenario → correction → regression test`.

Examples include rejecting stale document permissions in JWTs, checking for IDOR/BOLA through direct API requests, questioning whether a database-only audit log was tamper-evident, and checking that search could not disclose unauthorized metadata.

The actual prompt/output/correction history belongs in `PROMPTS.md` and must be maintained with real dates and commit links.

## What I deliberately did not build

- Public guest sharing links
- Folder permission inheritance
- Full-text indexing of document contents
- Full version recovery
- External malware scanning
- Independent WORM audit storage

These were deliberately deferred because the core evaluation is the correctness of authorization, revocation and auditability.

## Known limitations

The SHA-256 chain makes audit tampering detectable but is not an independent immutable root of trust. A production system should replicate the audit stream to an append-only/WORM system controlled by a separate security principal.

The seven-day build uses a private Docker volume for files. Production should use durable private object storage, plus content-signature validation and malware scanning.

## What I would do next

1. Add an independent append-only audit sink.
2. Add malware/content scanning.
3. Move private files to durable object storage.
4. Add login rate limiting and account lockout controls.
5. Add distributed audit-write serialization for multi-instance deployments.
6. Add document versions and recovery with the same authorization model.
