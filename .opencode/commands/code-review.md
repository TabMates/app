---
description: Code review (architecture + bugs) of current branch changes vs a base branch (default main)
agent: code-reviewer
subtask: true
---

Perform a code review (architecture + bugs) of the current branch.

Base branch: $ARGUMENTS (if empty, use `main`)

Follow the canonical reviewer brief in `.claude/skills/code-review/reviewer-instructions.md`: compute
the diff against the base branch (committed since merge-base plus uncommitted changes), load
`AGENTS.md` and the skill files relevant to the touched layers, run the architecture pass and the bug
pass, insert findings as inline `REVIEW(…)` comments at the offending lines, and return the summary
report.
