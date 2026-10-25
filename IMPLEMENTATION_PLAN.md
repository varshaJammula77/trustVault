# TrustVault Implementation Plan

This document tracks the implementation areas required for the TrustVault project.  
Commit history must reflect the work actually performed. Do not back-date or fabricate commits.

## Implementation Areas

1. **Establish TrustVault architecture**
   - Project structure
   - Docker configuration
   - PostgreSQL database
   - React frontend shell

2. **Add authentication and role model**
   - BCrypt password hashing
   - JWT authentication
   - ADMIN, OWNER, and VIEWER roles

3. **Enforce document-level authorization**
   - Centralized authorization checks
   - Owner-based access
   - Direct document permissions
   - Protection against IDOR/access-control bypass

4. **Add private storage and upload validation**
   - Private server-side file storage
   - Opaque storage keys
   - File type and size validation
   - SHA-256 file fingerprints

5. **Implement immediate permission revocation**
   - Permission checked from the database for every protected operation
   - Revoked users lose access immediately

6. **Implement tamper-evident audit logging**
   - Sequential audit events
   - Chained SHA-256 hashes
   - Audit verification
   - Logging of successful and denied security-sensitive actions

7. **Add authorization-filtered search and Security Center**
   - Search only documents the current user is authorized to access
   - Prevent metadata leakage
   - Security activity and permission-change visibility

8. **Add security regression tests**
   - IDOR protection tests
   - Unauthorized document access tests
   - Permission revocation tests
   - File-storage security tests
   - Audit tampering tests

9. **Complete security and project documentation**
   - Threat model
   - API documentation
   - Setup instructions
   - Accepted risks and limitations
   - AI-development record in `PROMPTS.md`

10. **Verify deployment and demonstration flow**
    - Docker deployment
    - Authentication and role flows
    - Upload/download/delete
    - Permission grant/revoke
    - Unauthorized-access behavior
    - Audit/security-center verification
    - Final demo flow

## Evidence Rule

Every Git commit must correspond to work that was actually performed.

Do not create historical commits to make the project appear to have been developed in a particular sequence.

Keep `PROMPTS.md` updated with the real AI prompt, the resulting work, any manual corrections, and the corresponding real commit link when available.

## Current Status

Implementation and verification have been performed across the areas above. The remaining submission work should be tracked separately in the TrustVault submission checklist.