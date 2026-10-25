# TrustVault — Short Write-up

## What I Built

TrustVault is a secure document-sharing platform where document access is controlled by the server and important security decisions are recorded in a tamper-evident audit ledger.

The implementation uses React, Spring Boot, PostgreSQL, JWT/BCrypt authentication, and private file storage.

## Permission Model

There are three roles:

- **ADMIN** — system and security oversight, user administration, and administrative activity views.
- **OWNER** — manages documents they own and controls named access.
- **VIEWER** — accesses only documents explicitly granted to them.

At the document level there are two grants:

- `READ` — metadata and download access.
- `EDIT` — read access plus metadata modification.

Delete is intentionally owner/admin-only because it is destructive.

Access is represented as a direct user-to-document relationship. I deliberately did not implement folder inheritance because direct grants are easier to reason about and revoke within a seven-day security-focused build.

## The Central Security Decision

The frontend is never the authorization boundary.

Protected document operations perform authorization checks on the backend using the authenticated user and current document permission state. Document permissions are not stored in the JWT.

This means permission revocation is effective for subsequent requests even when the viewer still has a valid JWT.

## What Is Distinctive About This Implementation

### 1. Reasoned Access Grants

When an owner grants access, TrustVault requires a short business reason. The reason is recorded with the permission-change audit information, making the audit trail more useful than simply recording that a permission changed.

### 2. Evidence Fingerprints

Uploaded files receive a SHA-256 content fingerprint. The fingerprint is displayed on the document card and recorded in the upload audit event. It provides an integrity reference without exposing the private storage key.

### 3. Security Center

Administrators get an operational view containing audit-chain status, enabled users, recent downloads, recent denied attempts, and recent permission changes.

These security signals are derived from the audit ledger used for investigation.

### 4. Authorization-Filtered Search

Search applies authorization filtering on the server side. Unauthorized documents are not returned to the browser, preventing metadata enumeration through the search interface.

## AI Usage and Correction

AI was used as a development assistant for architecture, implementation ideas, threat modeling, testing, debugging, and documentation. It was not treated as a security authority.

The development review pattern was:

`AI suggestion → independent review → attack scenario → correction → regression test`

Examples included reviewing stale document permissions, testing direct API IDOR/BOLA attempts, examining whether the audit design was actually tamper-evident, and verifying that search could not disclose unauthorized metadata.

The actual prompt, output, correction, and decision history is maintained in `PROMPTS.md` using the real development record.

## What I Deliberately Did Not Build

- Public guest sharing links
- Folder permission inheritance
- Full-text indexing of document contents
- Full version recovery
- External malware scanning
- Independent WORM audit storage

These were deliberately deferred because the core evaluation focuses on authorization, immediate revocation, and auditability.

## Known Limitations

The SHA-256 audit chain provides tamper detection by linking events together, but it is not an independent immutable root of trust. A production system should replicate the audit stream to an append-only/WORM system controlled by a separate security principal.

The seven-day build uses private Docker-backed file storage. Production should use durable private object storage together with stronger content validation and malware scanning.

The existing development database also contains historical audit records created during iterative development. Their hashes are not currently treated as fully cryptographically revalidated against the final canonicalization until the records are regenerated or migrated. This limitation is documented rather than presenting historical verification as stronger than the available evidence supports.

## What I Would Do Next

1. Add an independent append-only audit sink.
2. Add malware/content scanning.
3. Move private files to durable object storage.
4. Add login rate limiting and account lockout controls.
5. Add distributed audit-write serialization for multi-instance deployments.
6. Add document versions and recovery using the same authorization model.