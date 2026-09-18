<!--
Copyright (c) 2026 Zar

This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.

Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
plus this project's additional terms. Both are included in full in the
LICENSE file at the root of this repository.
-->

# AGENTS.md

This file is for any AI tool used to help write a contribution to Zelly's
Nexus. It does not replace CONTRIBUTING.md or LICENSE; it restates their
binding requirements in a place an AI assistant is likely to read first.
Read CONTRIBUTING.md and LICENSE in full before writing any code for this
project. When this file and either of those disagree, CONTRIBUTING.md and
LICENSE win.

## Non-negotiable rules for AI-assisted contributions

- Every file created starts with the license notice header in HEADER.txt,
  with the current year filled in. See DOCS/0.0.0_CHANGELOG.md for how the
  header is placed at the top of a file.
- No explanatory comments, narration, or AI-style annotations inside a
  created or modified file. Code is pure and clean. If the file's purpose
  or connection to other files genuinely needs a note, that note goes at
  the very end of the file only, nowhere else.
- Every change ships with a changelog entry in DOCS/, named
  `x.x.x_CHANGELOG.md` for the version it belongs to, following the
  Keep a Changelog format. Use DOCS/0.0.0_CHANGELOG.md as the template.
- Do not change or expand the project's scope. The scope is described in
  README.md and, in full, in SCOPE.md. A feature outside that scope
  belongs in an issue first, not directly in a pull request.
- Keep pull requests small: one focused piece of work at a time. Only bundle
  multiple changes together when they form one indivisible piece of work.
- Before starting a fork or an independent project, an issue must be opened
  first to clarify the intended changes, per LICENSE and CONTRIBUTING.md.
- The project name in README.md is not final and may change during
  development; do not treat any specific name as permanent.
- Using a translator or an AI tool to write an issue or pull request
  description is fine, provided the result is clean, easy to read, and
  accurately describes what is actually meant. Do not submit vague or
  meandering AI output that is hard to understand.
- Internal development notes, drafts, and plans are confidential and not
  part of this repository. Never reference, link, paraphrase, or attempt
  to reconstruct internal materials in any issue, pull request, or file.

## Where things live

- README.md: project scope, current compatibility targets, license summary.
- SCOPE.md: the full, authoritative project scope definition.
- LICENSE: PolyForm Noncommercial License 1.0.0 plus this project's
  additional terms (no commercial use, no closed derivatives, scope and
  fork rules).
- CONTRIBUTING.md: full contribution process, including the AI usage
  clause this file summarizes.
- HEADER.txt: the exact license header text required at the top of every
  created file.
- DOCS/: one changelog file per released version (`x.x.x_CHANGELOG.md`).
- .github/ISSUE_TEMPLATE/: the required issue forms for bug reports and
  feature suggestions, including the pre-fork discussion step.
