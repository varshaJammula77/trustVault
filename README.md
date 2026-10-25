# TrustVault — Secure Document Sharing with a Verifiable Access Ledger

TrustVault is a security-first document platform built for the seven-day challenge. Its core promise is simple: **a document is reachable only through a server-side authorization decision, and meaningful access decisions leave an audit trail.**

## What makes this implementation distinctive

- **Permission-as-data:** document access is resolved from PostgreSQL on protected requests. JWTs carry identity, not document permissions.
- **Immediate revocation:** removing a document grant changes the next authorization decision immediately, even when the user's JWT is still valid.
- **Access-filtered search:** the search query itself restricts results to documents the current user can access; unauthorized document metadata is not returned to React.
- **Evidence fingerprint:** every uploaded file receives a SHA-256 content fingerprint displayed in the UI and recorded in the audit event.
- **Reasoned access grants:** owners record a short business reason when granting READ/EDIT access; the reason is preserved in the permission-change audit event.
- **Security Center:** administrators can inspect operational trust signals including audit status, denied attempts, downloads, and permission changes.
- **Tamper-evident audit ledger:** audit events store previous and current hashes together with stable sequence numbers. The implementation detects modification of chained events.
- **Adversarial tests:** service-level authorization, search isolation, storage traversal, file readback, and audit-tampering tests are included.

## Stack

- React + Vite + Lucide
- Java 21 + Spring Boot 3.5
- PostgreSQL 16
- Spring Security + JWT + BCrypt
- Private filesystem storage inside a Docker volume
- JUnit + Spring Boot Test + H2 for automated authorization tests

## Roles

| Role | Capabilities |
|---|---|
| ADMIN | View authorized documents, inspect Security Center and audit ledger, administer users |
| OWNER | Upload, edit/delete owned documents, grant/revoke named access |
| VIEWER | Read/download only documents explicitly granted |

`READ` grants viewing/downloading. `EDIT` grants modification in addition to reading. Destructive delete remains owner/admin-only.

## Security model

For every protected document request:

```text
Bearer JWT
   ↓
Identify current user
   ↓
Load document from database
   ↓
Evaluate owner/admin/current permission
   ↓
ALLOW → continue
DENY  → 403 + PERMISSION_DENIED audit event
```

The frontend is not a security boundary. A user can modify the React UI or call the API directly and still has to pass the backend authorization decision.

### Why JWT does not contain document permissions

A permission can be revoked at any time. If `READ` were stored in a JWT, a previously issued token could remain authorized after revocation.

TrustVault therefore treats the JWT as **identity** and PostgreSQL as the **current authorization source of truth**.

### Private file storage

Files are stored outside the public web root under generated opaque storage keys. The browser receives an API download operation rather than a filesystem path or public object URL.

### Audit trail

Each audit event stores `previousHash` and `eventHash`, together with a stable sequence number. The event hash is calculated from the previous hash and canonical event fields.

This provides **tamper detection** for the audit chain. It is not equivalent to absolute immutability against an administrator who can rewrite the entire database. Production hardening could add an independent append-only or WORM audit sink.

## Runtime correctness notes

Document listing, metadata viewing, downloads, permission listing, and administrator audit mapping access lazy JPA relationships. Their controller operations keep an explicit read transaction open while mapping entities to response data, while `spring.jpa.open-in-view` remains disabled.

This addresses a potential `LazyInitializationException` path that could otherwise surface as HTTP 500 for list/audit responses.

Runtime verification confirmed the important authorization behavior:

- owner, viewer, and admin login flows work;
- viewers receive only documents they are authorized to access;
- authorized document downloads work;
- revoking a viewer's permission immediately removes access while the JWT remains valid;
- unauthorized document metadata is not returned through the protected search/list operation.

## Validation

Uploads currently enforce:

- non-empty files and a 10 MB application limit;
- extension plus signature checks for PDF, PNG, and JPEG;
- DOCX ZIP structure checks for `[Content_Types].xml` and `word/document.xml`;
- UTF-8 text validation for TXT files;
- bounded document/category names;
- generated opaque storage keys;
- SHA-256 calculated from actual file bytes;
- cleanup of partially written/orphaned files after storage or metadata failure.

These checks reduce malformed-file acceptance but are not a malware scanner or complete parser-level validation. Antivirus scanning and content-disarm/reconstruction should be added before production use.

## Demo credentials

```text
Admin  admin@trustvault.local  / Admin@12345
Owner  owner@trustvault.local  / Owner@12345
Viewer viewer@trustvault.local / Viewer@12345
```

These are demonstration credentials for the challenge deployment. They are intended for judge evaluation only and should not be reused for any production deployment.

## Run with Docker

Requirements: Docker Desktop with WSL 2 enabled and ports `5173`, `5432`, and `8081` available.

From the folder containing `docker-compose.yml`:

```powershell
Copy-Item .env.example .env
docker compose up --build
```

Open:

```text
http://localhost:5173
```

The browser uses the Nginx same-origin `/api` proxy. The backend is also exposed at:

```text
http://localhost:8081
```

for local API testing.

The PostgreSQL host port is `5432`.

The checked-in Compose defaults are for local demonstration only. Change `POSTGRES_PASSWORD` and `JWT_SECRET` in `.env` before any network-exposed deployment. Keep `.env` out of Git.

Stop the application:

```powershell
docker compose down
```

Data remains in Docker volumes unless the volumes are explicitly removed.

To reset the demonstration database and uploaded files:

```powershell
docker compose down -v
```

**Warning:** the `-v` option is destructive.

## Development without Docker

Start PostgreSQL first, then:

```powershell
cd backend
mvn spring-boot:run
```

In a second terminal:

```powershell
cd frontend
npm install
npm run dev
```

## Test

From the `backend` directory:

```powershell
mvn test
```

The current automated test run completed successfully:

- **11 tests**
- **0 failures**
- **0 errors**
- **0 skipped**
- **BUILD SUCCESS**

The tests provide regression coverage for service-level authorization and permission behavior, authorization-filtered search, storage traversal rejection, stored-file fingerprint/readback, and audit hash tampering.

These tests do not replace complete end-to-end testing of every endpoint or deployment-specific security verification.

## API documentation

See [`docs/API.md`](docs/API.md).

## Threat model

See [`THREATS.md`](THREATS.md).

## AI review record

See [`PROMPTS.md`](PROMPTS.md).

The AI record documents actual prompts, observations, corrections, implementation work, and verification. It does not invent historical interactions or Git commit history.

## Five-minute demo

See [`docs/DEMO.md`](docs/DEMO.md).

## Seven-day engineering decisions

The following features were deliberately not included within the challenge timeframe:

- public guest links;
- folder inheritance;
- full-text document indexing;
- document version recovery;
- external malware scanning.

The direct named-grant model was chosen because it is easier to reason about, revoke, and audit within the challenge timeframe.

## Submission information

### GitHub repository

GitHub: https://github.com/varshaJammula77/trustVault

### Live deployment

Frontend: https://trustvault-web.onrender.com

Backend API: https://trustvault-myu3.onrender.com

### Commit history

The repository contains the actual development commit history, including security, audit-chain, documentation, and configuration changes.

Repository: https://github.com/varshaJammula77/trustVault