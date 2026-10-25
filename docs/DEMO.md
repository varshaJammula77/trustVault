# Five-Minute TrustVault Demo

## 0:00 — Explain the Promise

> “TrustVault is built around two guarantees: the wrong user cannot reach a document, and important access decisions leave evidence.”

Briefly explain that authorization is enforced by the backend rather than the frontend.

## 0:30 — Login as Owner

Use:

`owner@trustvault.local / Owner@12345`

Upload a small PDF or TXT file.

Point out:

- Document name and category
- SHA-256 file fingerprint
- The file is stored privately rather than exposed through a public URL

## 1:15 — Grant Named Access

Open **Manage Access** and grant the viewer:

- User: `viewer@trustvault.local`
- Permission: `READ`
- Reason: provide a short legitimate reason such as `Demo access`

Explain that the permission is stored as a document/user relationship in the database and is not embedded in the JWT.

Point out that the permission change is recorded in the audit ledger.

## 1:45 — Viewer Access

Login as:

`viewer@trustvault.local / Viewer@12345`

Show that the viewer can see the shared document and download it.

Explain that the viewer cannot see documents that have not been shared with them.

## 2:20 — Attack the API

Using the browser network tools or an API client, attempt to access an unshared document by changing the document ID.

Expected result:

`403 Forbidden`

Explain:

> “The frontend is not the security boundary. The backend checks the current user's authorization for the requested document, so changing the ID in the request cannot bypass the permission check.”

Show the corresponding denied-attempt audit event if available.

## 3:00 — Immediate Revocation

Return to the owner account.

Revoke the viewer's access.

Without relying on a new login, use the same viewer session/token to attempt the protected document operation again.

Expected result:

`403 Forbidden`

Explain:

> “The permission is checked against current database state rather than being stored in the JWT, so revocation takes effect on the next protected request.”

## 3:40 — Admin Security Center

Login as:

`admin@trustvault.local / Admin@12345`

Open **Security Center**.

Show:

- Audit-chain status
- Enabled users
- Recent downloads
- Recent denied attempts
- Recent permission changes

Explain that these operational signals are derived from the audit ledger.

## 4:15 — Audit Ledger

Open **Audit Ledger**.

Show relevant events such as:

- `GRANT_ACCESS`
- `DOWNLOAD`
- `REVOKE_ACCESS`
- `PERMISSION_DENIED`

Explain that audit events contain chained SHA-256 hashes and sequence information.

**Important:** Do not claim that every historical development event has been cryptographically revalidated if the historical audit records have not yet been regenerated or migrated to the final canonicalization.

If the displayed chain status is valid for the current verified data, demonstrate that status and explain exactly what has been verified.

## 4:45 — Close With Trade-offs

State honestly:

> “This seven-day build focuses on server-side authorization, immediate permission revocation, private file storage, and a tamper-evident audit ledger. For production, I would add independent WORM audit storage, malware scanning, durable private object storage, rate limiting, stronger secret management, and additional operational controls.”

End by emphasizing that the key security property is **server-enforced authorization on every protected document operation**.