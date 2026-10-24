# TrustVault API

All endpoints except `POST /api/auth/login` and `GET /actuator/health` require a valid, unexpired JWT. The backend reloads the user and current role from PostgreSQL for each request. Document permissions are checked against current database state.

## Authentication

### `POST /api/auth/login` — public

Request JSON: `{"email":"owner@trustvault.local","password":"Owner@12345"}`.

Success: `{ "token": "<JWT>", "user": { "id": "...", "email": "...", "fullName": "...", "role": "OWNER" } }`. Invalid credentials or disabled accounts return `401`.

Send subsequent requests with `Authorization: Bearer <JWT>`.

## Documents

| Method | Endpoint | Authentication / authorization | Request / behavior |
|---|---|---|---|
| GET | `/api/documents` | Any authenticated user; results are owner/admin/grant filtered | Optional query parameters: `name`, `category`, `owner`, `from`, `to`. Dates are ISO-8601 instants; `to` is exclusive. |
| POST | `/api/documents` | OWNER or ADMIN | `multipart/form-data`: `file`, `name`, `category`. Max file size 10 MB. Allowed extensions/content checks: PDF, PNG, JPEG, TXT, DOCX. |
| GET | `/api/documents/{id}` | Owner, ADMIN, or explicit READ/EDIT grant | Returns authorized metadata only. |
| GET | `/api/documents/{id}/download` | Owner, ADMIN, or explicit READ/EDIT grant | Streams private file after current authorization check. |
| PUT | `/api/documents/{id}` | Owner, ADMIN, or EDIT grant | JSON with optional `name` and/or `category`. |
| DELETE | `/api/documents/{id}` | Owner or ADMIN | Soft-deletes metadata, removes active grants and deletes the primary stored file. |
| GET | `/api/documents/{id}/permissions` | Owner or ADMIN | Lists explicit grants. |
| POST | `/api/documents/{id}/permissions` | Owner or ADMIN | JSON: `userId`, `permission` (`READ` or `EDIT`), `reason` (5–200 chars). Existing grants are updated. |
| DELETE | `/api/documents/{id}/permissions/{userId}` | Owner or ADMIN | Revokes current access. |

The owner filter is applied to the already authorization-filtered result set for every role. It cannot widen a user's access. `from` is inclusive and `to` is exclusive.

## Users and administration

| Method | Endpoint | Access | Behavior |
|---|---|---|---|
| GET | `/api/users` | OWNER or ADMIN | Lists enabled users for named grants. |
| POST | `/api/users` | ADMIN | Creates a user. Request fields: `email`, `password`, `fullName`, `role`. |
| PATCH | `/api/users/{id}/disable` | ADMIN | Disables another user; self-disable is rejected. Existing JWTs fail on the next request. |
| GET | `/api/admin/audit` | ADMIN | Returns up to 500 recent audit events and chain verification status. |
| GET | `/api/admin/security-summary` | ADMIN | Returns user, download, denied-attempt, permission-change and chain metrics. |
| GET | `/api/admin/users` | ADMIN | Lists users. |

## Common errors

- `400` — invalid parameters, metadata, unsupported/malformed upload, empty upload, invalid permission request.
- `401` — missing, invalid, expired JWT or disabled/missing account.
- `403` — authenticated user lacks the required role or document permission.
- `404` — document, user or grant not found.
- `409` — duplicate email when creating a user.
- `413` — upload exceeds the configured 10 MB limit.
- `500` — unexpected server failure; detailed exception is logged server-side, not returned to the client.

## Security notes

The storage key is never returned as a public URL. The download endpoint checks current authorization before opening a file. Search is performed in the repository with the authorization predicate and requested filters in the same query.
