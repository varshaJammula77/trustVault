# TrustVault API

All endpoints except `POST /api/auth/login` and `GET /actuator/health` require a valid, unexpired JWT.

Document authorization is enforced server-side using the current user identity, role, and current document-permission state from PostgreSQL.

## Authentication

### `POST /api/auth/login` — public

Request JSON:

```json
{
  "email": "owner@trustvault.local",
  "password": "Owner@12345"
}
```

Success:

```json
{
  "token": "<JWT>",
  "user": {
    "id": "...",
    "email": "...",
    "fullName": "...",
    "role": "OWNER"
  }
}
```

Invalid credentials or disabled accounts return `401`.

Send subsequent requests with:

```text
Authorization: Bearer <JWT>
```

## Documents

| Method | Endpoint | Authentication / authorization | Request / behavior |
|---|---|---|---|
| GET | `/api/documents` | Any authenticated user; results are filtered by ownership, ADMIN role, or explicit grant | Optional query parameters: `name`, `category`, `owner`, `from`, `to`. Dates are ISO-8601 instants; `to` is exclusive. |
| POST | `/api/documents` | OWNER or ADMIN | `multipart/form-data`: `file`, `name`, `category`. Maximum file size is 10 MB. |
| GET | `/api/documents/{id}` | Owner, ADMIN, or explicit READ/EDIT grant | Returns authorized document metadata. |
| GET | `/api/documents/{id}/download` | Owner, ADMIN, or explicit READ/EDIT grant | Streams the private file after the current authorization check. |
| PUT | `/api/documents/{id}` | Owner, ADMIN, or explicit EDIT grant | JSON with optional `name` and/or `category`. |
| DELETE | `/api/documents/{id}` | Owner or ADMIN | Soft-deletes the document, removes active grants, and deletes the primary stored file. |
| GET | `/api/documents/{id}/permissions` | Owner or ADMIN | Lists explicit grants for the document. |
| POST | `/api/documents/{id}/permissions` | Owner or ADMIN | JSON: `userId`, `permission` (`READ` or `EDIT`), `reason` (5–200 characters). Existing grants are updated. |
| DELETE | `/api/documents/{id}/permissions/{userId}` | Owner or ADMIN | Revokes the user's current access. |

The `owner` filter is applied **after/beside the authorization predicate** and cannot widen a user's access.

`from` is inclusive and `to` is exclusive.

Supported upload types are validated by the application's upload-validation logic. The configured maximum upload size is 10 MB.

## Users and Administration

| Method | Endpoint | Access | Behavior |
|---|---|---|---|
| GET | `/api/users` | OWNER or ADMIN | Lists enabled users available for named grants. |
| POST | `/api/users` | ADMIN | Creates a user. Request fields: `email`, `password`, `fullName`, `role`. |
| PATCH | `/api/users/{id}/disable` | ADMIN | Disables another user. Self-disable is rejected. Existing authentication is rejected on subsequent requests. |
| GET | `/api/admin/audit` | ADMIN | Returns recent audit events and the server's audit-chain verification status. |
| GET | `/api/admin/security-summary` | ADMIN | Returns security summary metrics such as users, downloads, denied attempts, permission changes, and audit-chain status. |
| GET | `/api/admin/users` | ADMIN | Lists users. |

## Common Errors

- `400` — Invalid parameters, metadata, unsupported/malformed upload, empty upload, or invalid permission request.
- `401` — Missing, invalid, or expired JWT, or disabled/missing account.
- `403` — Authenticated user lacks the required role or document permission.
- `404` — Document, user, or grant not found.
- `409` — Duplicate email when creating a user.
- `413` — Upload exceeds the configured 10 MB limit.
- `500` — Unexpected server failure. Detailed exceptions are logged server-side and are not returned to the client.

## Security Notes

- Authorization is enforced by the backend; the frontend is not trusted to decide access.
- Document permissions are checked against current database state for protected document operations.
- Permission revocation therefore takes effect for subsequent requests using the same JWT.
- The storage key is not exposed as a public URL.
- The download endpoint performs authorization before opening the stored file.
- Search applies authorization filtering together with the requested search filters so unauthorized documents are not returned.
- Security-sensitive actions are recorded in the audit ledger.
- Audit-chain verification status is exposed to administrators. Historical audit records must not be described as fully cryptographically verified unless they have been successfully verified against the current canonicalization.