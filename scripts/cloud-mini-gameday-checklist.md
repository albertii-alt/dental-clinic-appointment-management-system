# Cloud Mini Game-Day Checklist (30-45 Minutes)

Goal: Practice incident response without risky destructive actions.

## Scenario A: "Users report slow loading"

1. Run snapshot
- `./scripts/cloud-incident-snapshot.sh`

2. Validate baseline
- `./scripts/cloud-db-health-check.sh`
- `./scripts/cloud-security-audit.sh`

3. Confirm backup safety
- `./scripts/cloud-backup-status.sh`
- Verify latest backup path and time are recent enough

4. Team drill questions
- Who is incident lead?
- Who posts user updates?
- What is escalation trigger to use admin identity?

5. Close drill
- Fill `scripts/cloud-incident-log-template.md`
- Write one improvement action

## Scenario B: "Need emergency rollback confidence"

1. Confirm latest backup exists
2. Confirm restore drill runbook availability
3. Confirm who approves restore execution
4. Confirm post-restore validation checklist owner

## Success Criteria
- Team can run first-response steps in less than 10 minutes
- Incident log is completed clearly
- At least one improvement item is recorded
