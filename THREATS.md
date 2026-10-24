# TrustVault Threat Model

This document distinguishes controls present in source from controls whose runtime tests remain to be executed. A source-code check is not proof that a deployment is secure.

| Threat | Impact | Implemented mitigation | Verification / status |
|---|---|---|---|
| Forged, invalid or expired JWT | Account impersonation | Signed JWT parsing with expiration; user reloaded from DB | Source inspected; automated HTTP tests pending |
| Disabled account with old JWT | Continued access after disable | JWT filter reloads enabled state on every request | Source inspected; request test pending |
| IDOR / document ID manipulation | Cross-user document disclosure or mutation | Centralized current-user/document permission checks | Authorization service tests exist; endpoint-level tests pending |
| Owner ID / role / permission body manipulation | Ownership or privilege escalation | Upload owner derived from authenticated principal; permission APIs owner/admin protected | Source inspected; direct API attack tests pending |
| Unauthorized download | Private file disclosure | Authorization before storage open; no public file URL | Source inspected; API integration test pending |
| Access after revocation | Stale access | Permission lookup uses current database state; no document permissions in JWT | Service test covers revoke; same-token HTTP test pending |
| Unauthorized search discovery | Private metadata disclosure | Repository query combines authorization predicate with name/category/owner/date filters | Search isolation test added; Maven execution pending |
| Oversized upload | Resource exhaustion | Spring multipart and 10 MB application limit; 413 response | Boundary test pending |
| Malformed or mislabeled file | Parser exploit / unexpected content | Signature checks for PDF/PNG/JPEG, DOCX ZIP entry checks, UTF-8 TXT validation | Source inspected; malformed-file tests pending |
| Path traversal | Read/write outside private storage | Restricted opaque storage-key alphabet, normalized root check, no-follow file open | Storage traversal tests added; execution pending |
| Partial upload / metadata failure | Orphaned private file | Failed storage writes cleaned; metadata-save failure deletes stored file | Source inspected; failure-injection test pending |
| Audit row modification | Loss of trustworthy history | SHA-256 previous-hash chain and full-chain verification | Tamper test added; execution pending |
| Audit deletion/reordering | Gaps or broken history | Sequence linkage and previous-hash verification | Tamper test added; execution pending |
| Concurrent audit writes | Forked chain / duplicate sequence | In-process synchronized writer and unique sequence constraint | Single-instance only; multi-instance serialization not implemented |
| Admin endpoint abuse | Exposure of security activity | Explicit server-side ADMIN check | Source inspected; API test pending |
| Secret leakage | Token/database compromise | Environment-variable configuration and ignore rules | Repository inspection required before commit; local defaults are demo-only |
| Brute-force login | Credential guessing | BCrypt password hashing | Rate limiting / lockout not implemented |
| File replacement at rest | Content integrity compromise | SHA-256 fingerprint recorded at upload | Download-time rehash and immutable storage not implemented |
| Malware in uploaded files | Malware execution by recipients | File format checks only | Antivirus scanning is not implemented |

## Residual limitations

1. The hash chain is tamper-evident, not immutable against a database superuser who can rewrite all events and hashes. External append-only/WORM anchoring is not implemented.
2. Audit serialization uses an in-process lock; it is not safe for multiple backend replicas without a database/distributed lock strategy.
3. No antivirus scanning, rate limiting, refresh-token rotation, MFA, or production secrets manager is included.
4. Full controller-level security coverage, Docker startup, clean-environment startup and frontend production build must be run in an environment with Maven, Docker and npm registry access.
5. Local demo credentials and Compose fallback secrets must be changed before public deployment.
