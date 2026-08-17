# AGENTS.md

## Implementation Planning

These rules apply only when the request requires modifying, adding, removing, or refactoring source code.

Before modifying any source code:

1. Check whether `PLAN.md` exists at the project root.

2. If `PLAN.md` exists:
   - Read `PLAN.md` before analyzing or modifying source code.
   - Check whether the current request continues, changes, or expands an existing task.
   - If the request continues an existing task, continue that task instead of creating a duplicate.
   - Review the existing task status, requirements, approach, checklist, and notes before continuing.

3. If `PLAN.md` does not exist:
   - Create `PLAN.md` at the project root before modifying source code.

4. For every source-code change:
   - Create a new plan item if the request is unrelated to existing tasks.
   - Update the existing plan item if the request continues or expands an existing task.
   - Set the relevant task to `IN PROGRESS` before modifying source code.

5. Do not modify source code before `PLAN.md` has been created or updated for the current request.

6. During implementation:
   - Keep the plan synchronized with important discoveries, decisions, scope changes, and implementation progress.
   - Update the checklist as steps are completed.
   - Record blockers or important technical findings when they affect the implementation.

7. After implementation:
   - Verify the changes according to the applicable Android rules and project requirements.
   - Update the plan with the actual implementation result.
   - Mark the task as `DONE` only after verification is complete.

8. Keep completed tasks in `PLAN.md` as project history unless explicitly asked to remove them.

9. Do not create duplicate plan items for the same ongoing task.

Requests that only require explanation, analysis, debugging guidance, code review, documentation, or answering questions do not require `PLAN.md` unless source code will actually be modified.

## Release Notes

Khi task liên quan đến tag hoặc release mới, agent phải kiểm tra tag/commit liên quan và tự tạo release note trong `docs/`.

- Tên file: `docs/release-notes-<version>.md`, ví dụ `docs/release-notes-3.1.6.md`.
- Nội dung phải dựa trên `git log` và `git diff` của tag/commit liên quan; không tự suy đoán thay đổi.
- Nếu người dùng giới hạn phạm vi, ví dụ "chỉ commit hôm nay", chỉ dùng đúng phạm vi đó.
- Giữ format giống các release note có sẵn: tiêu đề, tóm tắt, `Thay đổi`, `Nâng cấp`, `Migration` nếu cần, và link commit/changelog.
- Chỉ cập nhật `README.md` khi người dùng yêu cầu thêm link vào danh sách tài liệu.

## CodeGraph

In repositories indexed by CodeGraph (a `.codegraph/` directory exists at the repo root), reach for it before grep/find or reading files when you need to understand or locate code.

- MCP tool: use `codegraph_explore` when available.
- Shell fallback: `codegraph explore "<symbol names or question>"`.
- If there is no `.codegraph/` directory, skip CodeGraph entirely.
