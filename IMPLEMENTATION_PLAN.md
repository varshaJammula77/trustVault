# TrustVault Implementation Plan

Use this as the actual work/commit checklist. Do not back-date commits.

1. `feat: establish TrustVault architecture` — project structure, Docker, DB and basic React shell.
2. `feat: add authentication and role model` — BCrypt, JWT, ADMIN/OWNER/VIEWER.
3. `feat: enforce document-level authorization` — centralized checks and direct-grant model.
4. `feat: add private storage and upload validation` — opaque storage keys, limits, SHA-256 fingerprints.
5. `feat: add immediate permission revocation` — current DB lookup for every protected document operation.
6. `feat: add tamper-evident audit ledger` — stable sequence + chained SHA-256 hashes + verification.
7. `feat: add authorization-filtered search and Security Center` — prevent metadata leakage and add operational signals.
8. `test: add IDOR and revocation regression tests` — tests must fail when the relevant authorization control is removed.
9. `docs: complete threat model and API documentation` — record accepted risks and non-goals.
10. `chore: verify Docker deployment and demo flow` — clean-clone run and five-minute demo.

## Evidence rule

Every commit should correspond to work actually performed. Keep `PROMPTS.md` updated with the real AI prompt, output, correction and commit link.
