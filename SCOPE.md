<!--
Copyright (c) 2026 Zar

This file is part of Zelly's Nexus <https://github.com/VictorGugug/Zelly-s-Nexus>.

Zelly's Nexus is licensed under the PolyForm Noncommercial License 1.0.0,
plus this project's additional terms. Both are included in full in the
LICENSE file at the root of this repository.
-->

# Project Scope

This is the authoritative description of what Zelly's Nexus is meant to
be. It exists so contributors, forks, and independent projects can judge
for themselves whether a change fits within scope, per LICENSE and
CONTRIBUTING.md. This document only records what has actually been
decided; it is expanded as decisions are made, not filled in advance.

## What the project is

Zelly's Nexus is an all-in-one Minecraft plugin, formerly known as
Zaynr Nexus. The project name itself is not final and may still change
during development. It is a solo project that I maintain.

## Platform target

- Minecraft: Java Edition 26.x, including 26.3. No older Minecraft
  versions are supported.
- Server software: Paper and its forks only (Paper, Purpur, Folia).
  No Spigot support.
- Possible compatibility with Minecraft: Bedrock Edition through the
  Geyser and Floodgate integration layer. Not guaranteed until
  implemented and tested.
- Any other server software: none planned.

## Core interface direction

The plugin is intended to provide dialog menus for Java Edition servers,
using new dialog UI windows rather than only inventory-based GUIs. The
exact layout of these dialogs is still being defined; the general
direction is styled after the dialog menu approach used in BlockProt
Reloaded, another project I maintain.

The visual identity (boot screen, color palette) is planned around a
pastel blue theme, replacing an earlier red pixel-art boot screen
reference.

## License and governance

- Licensed under the PolyForm Noncommercial License 1.0.0, plus this
  project's own additional terms, both in full in LICENSE: no commercial
  use, no closed derivatives, independent projects allowed only outside
  this scope document, forks must stay linked to this repository or be
  removed, attribution preserved.
- An issue must be opened before starting a fork or an independent
  project, to decide whether the work proceeds as a fork, a pull
  request, or by hand directly against this repository.
- Contributions follow CONTRIBUTING.md: pull requests are the required
  path for in-scope changes, kept small and incremental; AI-assisted
  contributions are allowed under the conditions listed there, including
  a required changelog entry per change and the required license header
  on every created file.

## Planned, not yet in scope for implementation

These are acknowledged future directions, not committed features yet:

- An English/Spanish translation system, with community translation
  support for additional languages.

## Out of scope

Nothing has been explicitly ruled out yet beyond what is inherent to the
platform target and license above. This section will be filled in as
real out-of-scope requests come up through issues, rather than guessed
at in advance.
