# Instructions for GitHub Copilot & Coding Assistants

## Skills Available in this Repository

This repository is equipped with agent skills in `.agents/skills/`:

1. **planning-with-files** (`.agents/skills/planning-with-files/`):
   - For multi-step tasks or non-trivial refactorings, use persistent file-based planning on disk.
   - Maintain `task_plan.md` (active phase, goals, checkboxes), `progress.md` (completed milestones), and `findings.md` (architecture discoveries).
   - Read the plan at the start of any new session or turn and keep it updated.

2. **graphify** (`.agents/skills/graphify/`):
   - For architecture, call hierarchy, or module relationship questions, query the codebase graph via `graphify query "<question>"` when `graphify-out/` exists.
   - Use `graphify path "<A>" "<B>"` for relationship tracing and `graphify explain "<concept>"` for focused concepts.

3. **ccusage** (`.agents/skills/ccusage/`):
   - For inspecting token usage, model analytics, session duration, and cost estimation across agents, run `ccusage daily`, `ccusage weekly`, or `ccusage session`.

4. **ponytail** (`.agents/skills/ponytail/`):
   - Minimal complete change: solve the entire task with the least new code.
   - No boilerplate, type wrappers, or "for later" code.
