# Five-Minute TrustVault Demo

## 0:00 — Explain the promise

“TrustVault is built around two guarantees: the wrong user cannot reach a document, and important access decisions leave evidence.”

## 0:30 — Login as owner

Use `owner@trustvault.local / Owner@12345`. Upload a small PDF/TXT. Point out the SHA-256 fingerprint shown on the card.

## 1:15 — Grant named access

Open Manage access and grant the viewer `READ`. Explain that the permission is stored per document/user and is not embedded in the JWT.

## 1:45 — Viewer access

Login as `viewer@trustvault.local / Viewer@12345`. Show the document and download it.

## 2:20 — Attack the API

Using the browser/network tool or an API client, change the document ID to an unshared document. The backend returns `403` and creates a `PERMISSION_DENIED` audit event. Explain that changing the React UI cannot bypass this check.

## 3:00 — Immediate revocation

Return to owner, revoke viewer access, then use the same viewer session/token to attempt the download again. It must fail immediately.

## 3:40 — Admin security center

Login as admin. Show Security Center: chain status, denied attempts, downloads and permission changes in the last 24 hours.

## 4:15 — Audit ledger

Open Audit Ledger. Show `GRANT_ACCESS`, `DOWNLOAD`, `REVOKE_ACCESS` and `PERMISSION_DENIED`. Show that the SHA-256 chain verifies.

## 4:45 — Close with trade-offs

State honestly that the seven-day build uses a hash chain and private Docker storage. Production hardening would add independent WORM audit storage, malware scanning, durable private object storage, rate limiting and stronger secret management.
