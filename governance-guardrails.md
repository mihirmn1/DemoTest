

## Approved workflows (phase 1)
1) Code understanding
2) Safe refactor proposals (behavior-preserving)
3) Test generation for targeted coverage

Avoid early:
- Big net-new feature generation in sensitive modules
- Automated merges

## Review expectations
- Codex output is a proposal; human review is mandatory.
- Standard PR + CI gates remain authoritative.
- Prefer small diffs, test-backed refactors.

## Monitoring
Adoption: active users by team, usage by workflow.
Value: PR cycle time, onboarding time, test coverage deltas.
Risk: corrections, escalations, sensitive module access trends.

## Sound bites
- “Codex proposes; humans approve.”
- “No auto-merge — same PR and CI gates.”
- “Harness provides visibility and standardization at scale.”
EOF