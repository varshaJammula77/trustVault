# TrustVault Threat Model

This document describes the main threats considered for TrustVault, the implemented mitigations, and the verification status observed during development.

A passing automated test provides regression coverage but does not by itself prove that a production deployment is secure. Deployment configuration, infrastructure security, credentials, and operational controls remain important.

| Threat | Impact | Implemented mitigation | Verification / status |
|---|---|---|---|
| Forged, invalid, or expired JWT | Account impersonation | Signed JWT validation with expiration; authenticated user is reloaded from the database | Implemented and exercised through authentication/runtime verification |
| Disabled account with old JWT | Continued access after account disable | JWT filter reloads the user's enabled state on protected requests | Implemented; behavior covered by authorization design |
| IDOR / document ID manipulation | Cross-user document disclosure or mutation | Server-side document ownership/permission checks are performed before protected operations | Authorization tests passed; runtime access isolation verified |
| Owner ID / role / permission body manipulation | Ownership or privilege escalation | Document ownership is derived from the authenticated principal; permission APIs require appropriate owner/admin authorization | Implemented and covered by authorization tests |
| Unauthorized download | Private file disclosure | Authorization occurs before opening the private stored file; files are not exposed through public URLs | Runtime authorization verified |
| Access after revocation | Stale access | Permissions are loaded from current database state; document permissions are not stored in JWTs | Immediate revocation verified with a still-valid JWT |
| Unauthorized search discovery | Private metadata disclosure | Repository search combines authorization restrictions with name/category/owner/date filters | Search-isolation regression test passed |
| Oversized upload | Resource exhaustion | Application-level 10 MB upload limit with appropriate rejection | Implemented; upload validation is covered by application logic |
| Malformed or mislabeled file | Parser exploit / unexpected content | Signature checks for PDF/PNG/JPEG, DOCX ZIP-entry validation, and UTF-8 TXT validation | Implemented; validation logic reviewed |
| Path traversal | Read/write outside private storage | Restricted opaque storage-key format, normalized storage-root validation, regular-file checks, and no-follow file access | Storage security tests passed |
| Partial upload / metadata failure | Orphaned private file | Failed storage writes and failed metadata persistence trigger cleanup | Implemented and reviewed |
| Audit row modification | Loss of trustworthy history | SHA-256 chained audit events with previous-hash linkage and sequence numbers | Audit tampering regression test passed; historical development data has a canonicalization compatibility issue documented in README/PROMPTS |
| Audit deletion/reordering | Gaps or broken history | Sequence linkage and previous-hash verification | Covered by audit-chain design and tampering regression coverage |
| Concurrent audit writes | Forked chain / duplicate sequence | Synchronized audit writer and unique sequence constraint | Protected for a single backend instance; multi-instance serialization requires additional coordination |
| Admin endpoint abuse | Exposure of security activity | Explicit server-side ADMIN authorization on administrative operations | Authorization controls implemented |
| Secret leakage | Token/database compromise | Environment-variable configuration and Git ignore rules for `.env` files | Repository reviewed; demonstration fallback secrets are not suitable for production |
| Brute-force login | Credential guessing | BCrypt password hashing | Rate limiting and account lockout are not implemented |
| File replacement at rest | Content integrity compromise | SHA-256 fingerprint recorded when the file is uploaded | Download-time rehashing and immutable storage are not implemented |
| Malware in uploaded files | Malware execution by recipients | File-format and content validation checks | Antivirus/content-disarm scanning is not implemented |

## Verification summary

The automated backend test suite was executed successfully:

- **11 tests**
- **0 failures**
- **0 errors**
- **0 skipped**
- **BUILD SUCCESS**

The tests include coverage for authorization and permission behavior, search isolation, storage traversal/readback security, and audit hash tampering.

Runtime verification also confirmed important security behavior, including:

- authentication for admin, owner, and viewer roles;
- authorization-filtered document visibility;
- authorized document download;
- immediate permission revocation;
- server-side enforcement independent of the React UI;
- administrator Security Center and audit views.

## Important audit-chain limitation

The application implements a SHA-256 hash chain in which each audit event references the previous event hash and contains a stable sequence number.

During development, the existing demonstration database contained historical audit records whose stored hashes did not consistently match the current cryptographic canonicalization logic. This was investigated rather than hidden.

Therefore, the implementation should **not claim that every historical development event has been successfully cryptographically revalidated** until those records are regenerated or formally migrated.

The audit design remains intended to make unauthorized modification detectable. A production deployment should also use an independent append-only/WORM audit destination so that an administrator with unrestricted database access cannot rewrite both the events and their hashes.

## Residual limitations

1. The audit hash chain is tamper-evident, but it is not immutable against a database superuser who can rewrite the entire datastore.
2. Audit serialization currently uses an in-process synchronization mechanism and is therefore designed for a single backend instance. Multi-instance deployment requires database-level or distributed serialization.
3. Antivirus scanning and content-disarm/reconstruction are not implemented.
4. Login rate limiting, account lockout, MFA, refresh-token rotation, and a production secrets manager are not implemented.
5. Download-time file rehashing and immutable object storage are not implemented.
6. Complete controller-level end-to-end coverage for every endpoint is not claimed.
7. Production deployment must use unique secrets and must not expose the demonstration credentials.
8. Docker, PostgreSQL, frontend build, network configuration, TLS, backups, and host-level filesystem permissions remain deployment responsibilities.
9. The current demonstration database should not be treated as the final production audit history until the historical hash-chain compatibility issue is resolved.

## Security design principles

TrustVault follows these core security principles:

- **Server-side authorization:** the backend, not React, decides whether an operation is allowed.
- **Current permission state:** document permissions are resolved from PostgreSQL rather than embedded in JWT claims.
- **Immediate revocation:** removing a grant affects the next authorization decision.
- **Least privilege:** viewers receive only explicitly granted document access.
- **Private storage:** uploaded files are kept outside the public web root.
- **Auditability:** important security actions and denied access attempts are recorded.
- **Defense in depth:** database authorization, application authorization, storage checks, validation, and audit logging provide multiple layers of protection.

## Production hardening recommendations

Before treating TrustVault as a production system, add:

- independent append-only/WORM audit storage;
- database/distributed locking for multi-instance audit writes;
- antivirus and content-disarm scanning;
- MFA and stronger account lifecycle controls;
- login rate limiting and account lockout;
- production secret management;
- TLS and secure cookie/token handling;
- immutable object storage or equivalent file-integrity controls;
- comprehensive API-level integration/security tests;
- centralized monitoring and alerting;
- secure backup and disaster-recovery procedures.