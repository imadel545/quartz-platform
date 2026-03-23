# Quality Gate Skill

Use this skill before considering a task complete.

## Required final checklist
- Is the scope respected?
- Are architecture boundaries preserved?
- Are names explicit and maintainable?
- Is business logic kept out of UI/controllers?
- Are assumptions documented?
- Were relevant validations run?
- Does the summary clearly state remaining gaps?
- Would this change be understandable by another serious engineer?

## Refuse low-quality completion if:
- the code compiles only in theory
- the implementation is mostly placeholders
- architecture was broken to move faster
- validation was skipped without explanation
- the change introduces hidden debt in a foundational area