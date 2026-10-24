# TrustVault — Secure Document Sharing with a Verifiable Access Ledger

TrustVault is a security-first document platform built for the seven-day challenge. Its core promise is simple: **a document is reachable only through a server-side authorization decision, and every meaningful access decision leaves evidence.**

## What makes this implementation distinctive

- **Permission-as-data:** document access is resolved from PostgreSQL on every protected request. JWTs carry identity, not document permissions.
- **Immediate revocation:** removing a document grant changes the next authorization decision immediately, even when the user's JWT is still valid.
- **Access-filtered search:** the search query itself restricts results to documents the current user can access; unauthorized metadata never reaches React.
- **Evidence fingerprint:** every uploaded file receives a SHA-256 content fingerprint displayed in the UI and recorded in the audit event.
- **Reasoned access grants:** owners must record a short business reason when granting READ/EDIT access; the reason is preserved in the permission-change audit event.
- **Security Center:** administrators get a small set of operational trust signals (audit-chain status, denied attempts, downloads and permission changes in the last 24 hours).
- **Tamper-evident audit ledger:** events form a SHA-256 chain with a stable sequence number, making edits detectable and chain verification deterministic.
- **Adversarial tests:** service-level authorization, search isolation, storage traversal and audit-tampering tests are included. Controller/API-level coverage still needs to be expanded before a production security sign-off.

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
| ADMIN | View all authorized documents, inspect security center and audit ledger, administer users |
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

The frontend is not a security boundary. A user can hide or modify the React UI and still cannot bypass the backend authorization check.

### Why JWT does not contain document permissions

A permission can be revoked at any time. If `READ` were stored in a JWT, a previously issued token could remain authorized after revocation. TrustVault therefore treats the JWT as **identity** and PostgreSQL as the **current authorization source of truth**.

### Private file storage

Files are stored outside the public web root under generated opaque storage keys. The browser receives an API download operation, not a filesystem path or public object URL.

### Audit chain

Each event stores `previousHash` and `eventHash`. The hash is computed from the previous hash plus the event's canonical fields. A stable database sequence determines chain order, and audit writes are serialized in-process. The admin console can verify the entire chain.

This provides **tamper detection**, not absolute immutability against a database administrator who can rewrite the entire datastore. Production hardening would add an independent append-only/WORM sink.

## Runtime correctness notes

Document listing, metadata viewing, downloads, permission listing and admin audit mapping access lazy JPA relationships. Their controller operations now keep an explicit read transaction open while mapping entities to response data (`spring.jpa.open-in-view` remains disabled). This addresses a likely `LazyInitializationException` path that can surface as HTTP 500 for list/audit responses. The original runtime exception could not be reproduced in this environment, so use the included MockMvc regression test and inspect server logs locally.

## Validation

Uploads currently enforce:

- non-empty file and a 10 MB application limit (413 for oversize requests)
- extension plus signature checks for PDF, PNG and JPEG
- DOCX ZIP structure checks for `[Content_Types].xml` and `word/document.xml`
- UTF-8 text validation for TXT files
- bounded document/category names
- generated opaque storage keys and SHA-256 calculated from actual bytes
- cleanup of partially written/orphaned files on storage or metadata failure

These checks reduce malformed-file acceptance but are not a malware scanner or a complete parser-level validation. Add antivirus scanning and content-disarm/reconstruction before production use.

## Demo credentials

```text
Admin  admin@trustvault.local  / Admin@12345
Owner  owner@trustvault.local  / Owner@12345
Viewer viewer@trustvault.local / Viewer@12345
```

These are demonstration credentials only. Change them before any public deployment.

## Run with Docker

Requirements: Docker Desktop with WSL 2 enabled and ports 5173, 5432 and 8081 available. From the folder containing `docker-compose.yml`:

```powershell
Copy-Item .env.example .env
docker compose up --build
```

Open `http://localhost:5173`. The browser uses the Nginx same-origin `/api` proxy; the backend is also exposed at `http://localhost:8081` for local API testing. The PostgreSQL host port is `5432`.

The checked-in Compose defaults are for local demonstration only. Change `POSTGRES_PASSWORD` and `JWT_SECRET` in `.env` before any network-exposed deployment. Keep `.env` out of Git.

Stop:

```powershell
docker compose down
```

Data is kept in Docker volumes unless you explicitly remove the volumes. To reset the demo database and uploaded files (destructive): `docker compose down -v`.

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

From `backend`:

```powershell
mvn test
```

The test suite includes service-level role/permission behavior, authorization-filtered search, storage traversal rejection, stored-file fingerprint/readback, and audit hash tampering detection. These tests have been added to source but could not be executed in the current editing environment; run `mvn test` locally and inspect the result before submission. They do not replace full MockMvc/end-to-end tests for every endpoint.

## API documentation

See [`docs/API.md`](docs/API.md).

## Threat model

See [`THREATS.md`](THREATS.md).

## AI review record

See [`PROMPTS.md`](PROMPTS.md). **Do not invent historical AI interactions or commit history.** Add the actual prompts, observed output, corrections and commit links as you work.

## Five-minute demo

See [`docs/DEMO.md`](docs/DEMO.md).

## Seven-day engineering decisions

Deliberately not included: public guest links, folder inheritance, full-text document indexing, document version recovery and external malware scanning. The direct named-grant model was chosen because it is easier to reason about, revoke and audit within the challenge timeframe.
