# Contributing to Zelly's Nexus

## Before Contributing

Read the [README](README.md), [SCOPE.md](SCOPE.md), and the
[LICENSE](LICENSE), including its project-specific additional terms,
before opening an issue or pull request. Contributions must remain
consistent with the project scope and license.

A pull request must show that its author has actually read
CONTRIBUTING.md, the license terms in LICENSE, README.md, and
SCOPE.md, and has at least a basic working knowledge of this
repository's structure and current state. This does not need to be
extensive, but it must be real: a pull request description that is
clearly disconnected from how the project actually works will be
sent back for correction before review.

## Pull Requests

Pull requests are the required path for changes that fit within the intended
scope of Zelly's Nexus. This includes improvements to the core plugin,
dialog menus, planned server software support, and compatibility work that
could reasonably be integrated into this repository.

Changes should include a clear description of the problem, the proposed
solution, affected compatibility targets, and the verification performed.

Pull requests are kept small. Add what you want to add gradually,
one focused piece at a time, rather than bundling unrelated changes
into a single large pull request. A larger pull request is only
accepted when the change is genuinely one indivisible piece of work
that does not make sense split apart.

## Use of AI

Using AI tools to help write a contribution is permitted, on the
condition that every one of the following is followed:

- The resulting code is ordered and clean, not a raw, unreviewed
  output.
- The change does not stray from, or attempt to expand, the
  project's scope.
- A changelog entry is always added to DOCS/, in a file named
  `x.x.x_CHANGELOG.md` for the version it belongs to, following the
  [Keep a Changelog](https://keepachangelog.com/) format and
  categories. Use [DOCS/0.0.0_CHANGELOG.md](DOCS/0.0.0_CHANGELOG.md)
  as the required template; propose the changelog entry there before
  writing the actual code change.
- Every file created starts with the required license notice header
  from [HEADER.txt](HEADER.txt).
- No explanatory text, comments, or narration are added inside a
  created or modified file. Files contain pure, clean code, since
  the changelog entry and a basic reading of the code already
  explain what changed. If useful, a short note describing the
  file's own purpose and how it connects to other files may be
  placed at the end of the file, but nowhere else.
- Pull requests stay small and incremental as described above,
  regardless of whether AI was used to help write them.

AI assistance is allowed because many people have ideas but not the
ability to program them, and this is one way to let those ideas move
forward and let people build the things they actually want to build.
Not everyone agrees with this stance, but it is a deliberate choice
to encourage learning, ambition, and contribution, provided every
rule above is actually followed.

## Independent Projects

An independent project based on Zelly's Nexus is permitted only when its
primary functionality is outside the project's intended scope. Features
that could reasonably be integrated must be proposed here through an issue or
pull request instead of being maintained as a separate competing project.

Being outside the project's scope does not exempt an independent
project from any other license term: noncommercial use, source
disclosure, attribution, and the fork rules below still apply in
full. Before starting a fork or an independent project, open an
issue first to clarify the intended changes. That issue is where it
gets decided whether the work proceeds as a fork, as a pull request
against this repository, or by hand directly against this repository
if the idea has no code yet.

This is kept strict because having several forks of the same project
scattered across different places, each drifting from the others, is
considered an unnecessary and unwanted outcome for this project.

## Forks

Forks must remain clearly linked to the original Zelly's Nexus repository and
must preserve attribution, the [LICENSE](LICENSE), and the relationship to
the original project. A fork that is not linked to the original repository is
not authorized under the project license and must be removed by its owner
immediately after notice.

## Commercial Use

Commercial use, monetization, paid services, closed-source paid products, and
other use for monetary compensation are not permitted. See the [LICENSE](LICENSE)
for the complete terms.

## Communication

Using a translator or an AI tool to write an issue, a pull request
description, or any other communication with this project is fine,
provided the result is clean, easy to read, and describes exactly what
is actually meant. Unedited AI output can wander from the point and
become hard to follow, so review it before submitting.

I am a native Spanish speaker and always welcome contact in Spanish,
but English or any other language is accepted without any problem; I
do my best to understand every message received, even when
communicating in another language is difficult for me.

See the Planned section of [README.md](README.md) for a short summary of
the plugin's own English/Spanish translation system. I keep detailed
design notes for it internally; they are not part of this public
repository.