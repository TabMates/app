---
name: code-review
description: |
  Code review of the current branch changes compared to a base branch (default main). Delegates the
  review to a fresh-context reviewer agent that checks the diff against the project's architecture
  skills and AGENTS.md — layer responsibility violations, architectural shortcuts, race conditions,
  leaks, and other misbehavior. Findings are delivered as a single markdown report under
  `.claude/reviews/`; no code is modified. Use this skill whenever the user asks to review their
  changes, branch, or diff against a base branch. Trigger on phrases like "review my changes",
  "code review", "review against main", "review my branch", "check my branch", "pre-MR review",
  "pre-PR review", or "review the diff".
---

# Code Review (TabMates)

Workflow for the **orchestrating** agent. You do not perform the review yourself — you set it up,
relay the result, and offer to fix.

## 1. Determine the base branch

Default `main`. The user may override (`staging`, `release/x`, a feature branch, a tag). Take the
base from the user's phrasing ("review against `design`") without asking when it is unambiguous.

## 2. Verify the base exists

```bash
git fetch origin <base>          # best effort — may fail offline
git rev-parse --verify origin/<base>
```

If `origin/<base>` is unavailable (offline, no remote ref), fall back to the local `<base>` branch.
If neither exists, ask the user which base to use — do not guess.

## 3. Delegate to a fresh-context reviewer — do NOT review inline

The review must not be biased by the conversation that produced the changes.

A `code-reviewer` agent is defined for both supported tools:

| Tool | Agent definition | Direct invocation |
|---|---|---|
| Claude Code | `.claude/agents/code-reviewer.md` | launch via the Agent/Task tool with `subagent_type: code-reviewer` |
| opencode | `.opencode/agents/code-reviewer.md` | `/code-review [base]` (`.opencode/commands/code-review.md`, `subtask: true`) |

- **If the tool supports subagents** (Claude Code's Agent tool, opencode's task/subtask): launch the
  `code-reviewer` agent with a prompt containing the base branch name, any focus areas the user
  mentioned, and the instruction to follow `.claude/skills/code-review/reviewer-instructions.md`.
- **If there is no subagent mechanism**: tell the user to open a fresh chat/session with the
  `code-reviewer` agent. Only as a last resort, follow `reviewer-instructions.md` directly in the
  current context.

## 4. Relay the report pointer

The reviewer returns a short block — report path, totals, one-sentence verdict, top 🔴 findings.
Print it verbatim. No softening, no filtering, no reordering, no added praise.

Do not open the report and re-summarize it in chat: the file is the artifact, and the user reads it
there.

## 5. Offer to fix

Offer to fix 🔴 findings; fix 🟡 on request. Fixes happen in the **main context**, not in the
reviewer: read the report file, work through the findings in severity order, and locate each one by
its **Anchor** excerpt rather than its line number — lines shift as earlier fixes land.

There is no cleanup step. The reviewer never touched the working tree, so nothing has to be stripped
before commit.

## Reviewer contract

The complete reviewer brief — diff computation, path→skill mapping, architecture checklist, bug
checklist, report path, report format, and return value — lives in
**`.claude/skills/code-review/reviewer-instructions.md`**.

That file is the **single source of truth**. The tool-specific definitions —
`.claude/agents/code-reviewer.md`, `.opencode/agents/code-reviewer.md`, and
`.opencode/commands/code-review.md` — are thin wrappers that point at it. Never duplicate its content
into a wrapper; update the brief instead.

This skill itself is shared: opencode loads skills from `.claude/skills/<name>/SKILL.md` for Claude
Code compatibility, so both tools read the same skill and the same brief. There is no `.opencode/skills/`
copy, and adding one would fork the source of truth.

## Notes

- The reviewer's **only** write is its report file under `.claude/reviews/`. It never edits, deletes,
  reorders, renames, stages, or commits anything, and never leaves comments in the code.
- Review covers committed changes since the merge-base **plus** uncommitted working-tree changes
  (staged and unstaged).
- Scope is the diff only — no "while we're here" refactor proposals.
- Reports are gitignored throwaway artifacts, one per run. Prune `.claude/reviews/` freely.
