# `doc/` — overview

Guide to the subfolders of this directory. Detailed rules for a subfolder live in that subfolder's own
`README.md` when it has one; this file only gives a short description and, where relevant, a pointer.

| Folder          | Contents                                                                                   |
|-----------------|---------------------------------------------------------------------------------------------|
| `doc/arch/`     | Technical architecture documentation of the project (descriptive, current state). See the English-language convention below. |
| `doc/recipes/` | Reusable recipes with a reference implementation ("convert X the way Y was converted"): steps, checklist, framework facts needed on the way, pitfalls. Not a description of the current system (that is `doc/arch/`) and no status tracking (that belongs in a plan in `doc/plan/`). A recipe can be removed once the work it describes is done everywhere. |
| `doc/plan/`     | Design/implementation plans. Rules for filing, lifecycle and referencing: see `doc/plan/README.md`. (Medium-term, plan management is expected to move from this file-based folder to an MCP server; until then, the file-based convention applies.) |
| `doc/analysis/` | Analyses of concrete problems/proposals, sometimes with an implementation plan attached.     |
| `doc/g2k/`      | "Good to know" — preserved debugging/research findings, not actively maintained, kept so the next investigation into the same area doesn't have to reconstruct them from scratch. |

## Using `doc/arch/` (for AI assistants — read before working in this project)

## Convention for `doc/arch/`

Any documentation or plan is written and maintained in **English** as a
matter of principle — regardless of the language of this file itself or of other `doc/` subfolders. New
documents in this folder, as well as additions to existing documents, are to be written directly in English,
not in another language with a later translation.

## Convention for `doc/recipes/`

Recipes are written in **English**, like `doc/arch/`. A recipe names its reference implementation and the files involved, but does not keep a
list of what is already converted or still open: that status lives in a plan (reference it by file name, see `doc/plan/README.md`).

The word "recipe" is reserved for this folder. In `doc/arch/`, a step-by-step procedure for extending the current system is called a "How to"
section (e.g. "How to: add a new importer") and a recurring structure a "pattern"; neither is a recipe, as it belongs to one part of the system.
