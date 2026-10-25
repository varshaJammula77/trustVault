# TrustVault Submission Checklist

Before submitting, personally verify each item. Do not claim an item is complete until you have tested it or have the required evidence.

Do not fabricate commits, test results, deployment status, or historical evidence.

## Required deliverables

- [ ] Public GitHub repository
- [ ] Git history contains only real commits corresponding to work actually performed
- [ ] Live URL
- [ ] Seeded demo data
- [ ] README with setup and credentials
- [ ] `THREATS.md`
- [ ] `PROMPTS.md` containing the actual prompts/results/corrections
- [ ] Automated tests
- [ ] Database schema/setup through the application/Docker deployment
- [ ] `docs/API.md`
- [ ] Five-minute demo video
- [ ] Short write-up

## Security verification

- [ ] Viewer cannot access an unshared document
- [ ] Direct API IDOR attempt returns 403
- [ ] Unauthorized download returns 403
- [ ] Unauthorized edit returns 403
- [ ] Unauthorized delete returns 403
- [ ] Unauthorized permission change returns 403
- [ ] Denied attempts are audited
- [ ] Grant creates an audit event
- [ ] Grant requires a reason
- [ ] Revoke creates an audit event
- [ ] Revocation works with the same JWT/session
- [ ] Search does not leak unauthorized metadata
- [ ] Files are not publicly reachable
- [ ] Audit chain cryptographically verifies all stored historical events
- [ ] Automated authorization tests fail if the relevant check is removed

## Honesty checks

- [ ] Every feature claimed in README actually works
- [ ] `PROMPTS.md` contains real interactions, not invented history
- [ ] Commit dates/history were not fabricated
- [ ] Known limitations are documented
- [ ] I can explain and modify the authorization logic live