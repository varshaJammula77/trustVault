# TrustVault Security Test Playbook

Use this playbook against the TrustVault local or deployed environment. These tests are for verifying the application's own security controls.

## 1. Viewer Cannot Read an Owner's Document

1. Login as the owner.
2. Note an owner-only document ID.
3. Login as the viewer.
4. Request the document metadata endpoint using that ID.
5. Attempt the download endpoint using the same ID.

Expected:

- Server returns `403`.
- The document is not returned.
- A `PERMISSION_DENIED` audit event is created where the relevant denial is audited.

## 2. IDOR/BOLA Attempt

1. Login as the viewer.
2. Identify a document the viewer is authorized to access.
3. Replace its document ID with the ID of a document the viewer has not been granted.
4. Send the request directly to the API rather than relying on the frontend.

Expected:

- The server performs a fresh authorization check for the requested document.
- Unauthorized access returns `403`.
- Changing the document ID cannot bypass authorization.

The frontend is not considered a security boundary.

## 3. Immediate Revocation

1. Grant the viewer `READ` access to a document.
2. Verify that the viewer can access/download it.
3. Revoke the viewer's access.
4. Keep the same browser session and JWT.
5. Attempt the protected operation again.

Expected:

- The request is rejected with `403`.
- Revocation takes effect without requiring a new login.
- The revoke action is recorded in the audit ledger.

## 4. READ Does Not Imply EDIT

1. Grant the viewer `READ` permission only.
2. Attempt:

```text
PUT /api/documents/{id}
```

directly using the viewer's JWT.

Expected:

- Server returns `403`.
- The document metadata is not changed.
- The denied operation is audited where applicable.

## 5. Viewer Cannot Manage Permissions

1. Login as a viewer.
2. Call the document permission endpoint directly.

For example:

```text
POST /api/documents/{id}/permissions
```

Expected:

- Server returns `403`.
- The viewer cannot grant or revoke document permissions.

## 6. Search Does Not Enumerate Unauthorized Documents

1. Login as a viewer.
2. Identify a document owned by another user that has not been shared.
3. Search using the known document name or another identifying filter.

Expected:

- The unauthorized document is absent from the API response.
- The backend, rather than React, prevents the metadata from being returned.

## 7. Audit Verification

As an administrator:

1. Open Security Center.
2. Open the Audit Ledger.
3. Review the displayed audit-chain status.
4. Confirm that audit events contain sequence and hash-link information.

The application should only report the audit chain as cryptographically verified when the stored events successfully pass the implemented cryptographic verification.

### Tampering experiment

For a local-only test environment:

1. Record the original audit row.
2. Modify an audit field directly in PostgreSQL.
3. Run the application's audit verification.
4. Confirm that cryptographic verification detects the modification.
5. Restore the original database state.

Do not perform this experiment against the final deployed database.

**Historical audit limitation:** The current development database contains historical events from iterative development whose hashes are not all currently revalidated against the final canonicalization. Do not claim that the entire historical ledger has been cryptographically verified until those records are regenerated or migrated and successfully verified.

## 8. Upload Validation

Attempt uploads that should be rejected, including:

- Empty file
- File larger than 10 MB
- Unsupported or malformed file type

Expected:

- Upload is rejected.
- The invalid document does not become accessible as a normal stored document.

## 9. Disabled-User Check

1. Login as a user who is allowed to be disabled.
2. Obtain a valid JWT.
3. As an administrator, disable that user.
4. Reuse the previously issued JWT for a subsequent API request.

Expected:

- The request is rejected.
- A disabled account cannot continue using an otherwise valid JWT.

## 10. Automated Authorization Regression Tests

Run the project's automated test suite.

Verify that it covers:

- Cross-user document access
- Unauthorized document reads
- Unauthorized downloads
- Permission revocation
- File-storage security
- Audit tampering behavior

The authorization tests should fail if the relevant server-side authorization control is removed or bypassed.

## Evidence Rule

For each security test, record only results that were actually observed.

Do not claim a test passed unless it was executed against the current implementation.

Do not fabricate screenshots, API responses, test counts, commit links, or security results.