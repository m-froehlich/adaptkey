# Plans: location and how to reference them

Plans are design/implementation documents. A plan's folder reflects its lifecycle state:

| Folder                | Meaning                              |
|-----------------------|--------------------------------------|
| `doc/plan/`           | open / in progress                   |
| `doc/plan/deferred/`  | parked indefinitely (not abandoned), not to be picked up in the near future |
| `doc/plan/done/`      | technically finished |

**`deferred/` is about time, not about priority.** A plan lies there for an indefinite time: kept so it is not forgotten,
but not to be picked up in the near future. A plan that merely is not being worked on right now stays in `doc/plan/`;
note any expected timing in its `Status:` header instead.

`doc/plan/backlog.md` is the list of ideas that have no plan of their own yet.

Moving a plan between these folders is normal and must **not** cause changes anywhere else.

## Rules

1. **Plan file names are unique across `doc/plan/`, `deferred/`, `done/`.** The file name alone identifies a plan.
   Never create a second plan with the same name in another state folder.
2. **Reference a plan by file name only, never by path or folder.** No `doc/plan/`, no `done/`, no `../plan/done/`.
   The state folder is not part of the plan's identity.
3. **Do not use relative Markdown links to plans**, as a link needs the folder. Write the name in backticks, e.g.
   `optional-properties.md` (in Java comments: `{@code optional-properties.md}`). Section hints may follow
   in words: `my-special-feature.md, "Frage 1"`.
4. **Resolve a reference by searching for the file name** in `doc/plan/` including its state folders, not by guessing the folder.
   Use the file-search tool that applies in your environment.
5. **Moving a plan** = `git mv` of the one file, nothing else. If a move makes other files change, a reference
   violates rule 2 and should be fixed to the name-only form.
6. Links *between* plans in the same folder tree are also name-only.
   If a plan name also exists outside `doc/plan/` (e.g. `frontend.md` in `doc/arch/`), say which one is meant in words
   ("the refactoring plan `frontend.md`").
7. **Inside a plan, refer to other files by project-root-relative path in backticks**, never by a relative link or
   `../` path: `doc/arch/frontend.md`, `src/de/qudus/qlibs/commons/util/Datez.java:42`, `app/res/publc/style/style.css`.
   Relative paths depend on the plan's own folder and would have to change whenever the plan moves. A section is named in
   words: `doc/arch/architecture.md`, section "Backup". A sibling project is written relative to the project root:
   `../qlibs-commons/src/...`.
8. Whether a plan is finished is answered by where the file is, not by what any reference says.
   Do not write "(done)" or "(open)" next to a reference either, as that state changes.
9. A plan's `Status:` header and the folder it lives in must always match; update the header when moving.
10. **A plan is moved to `deferred/` only on the user's explicit instruction.** Remarks like "not now" or "later" are not
    such an instruction: how long a plan is to wait is the user's decision, and an agent cannot judge it. When in doubt,
    leave the plan where it is and ask.

## Finding a plan

- By name: see rule 4.
- What is currently open: the files directly in `doc/plan/` (without `deferred/`, `done/`).
