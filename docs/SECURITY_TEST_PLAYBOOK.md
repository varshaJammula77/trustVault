# TrustVault Security Test Playbook

Use this against your own local deployment. These are not tests against any third-party system.

## 1. Viewer cannot read an owner's document

1. Login as owner.
2. Note an owner-only document ID.
3. Login as viewer.
4. Call the document metadata/download endpoint with that ID.
5. Expected: `403` and a `PERMISSION_DENIED` audit event.

## 2. IDOR/BOLA attempt

Take a document ID the viewer can access and replace it with another document ID. Repeat the metadata/download request.

Expected: the server makes a fresh authorization decision for the requested document; the UI is irrelevant.

## 3. Immediate revocation

1. Grant viewer `READ`.
2. Verify access works.
3. Revoke access.
4. Keep the same browser session/token.
5. Attempt the same download again.

Expected: `403` immediately.

## 4. READ does not imply EDIT

Grant `READ` only. Call `PUT /api/documents/{id}` directly.

Expected: `403` and a denial audit event.

## 5. Viewer cannot manage permissions

Call the permission endpoint directly as viewer.

Expected: `403`.

## 6. Search does not enumerate documents

As a viewer, search for a known owner-only document name.

Expected: the document is absent from the API response, not merely hidden by React.

## 7. Audit verification

As admin, open Security Center and Audit Ledger. The chain should report `CHAIN VALID` / `SHA-256 audit chain verified`.

For a local-only experiment, changing an audit row directly in PostgreSQL should make verification fail. Restore the database afterward.

## 8. Upload validation

Try:

- an empty file;
- a file over 10 MB;
- an unsupported MIME type.

Expected: rejection before the document becomes accessible.

## 9. Disabled-user check

Admin disables a non-admin account. Try to reuse that account's JWT.

Expected: subsequent API requests are rejected because the JWT filter checks the current enabled status.
