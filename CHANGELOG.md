<!--
SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>

SPDX-License-Identifier: Apache-2.0
-->

Changelog
=========

The format is based on [Keep a Changelog v1.1.0][keep-a-changelog]. See [the README file][docs.readme] for more details on how the project is versioned.

## [Unreleased] (0.0.0)
This is the initial plugin release. Haskeletor is a fork of [intellij-haskell][] (by @rikvdkleij and contributors), and at this point it is a functional equivalent of the latest-released beta, [1.0.0-beta88][intellij-haskell.latest] of said plugin, with [the changes](https://github.com/ForNeVeR/haskeletor/compare/46e18618f349083512249bb1be910b61ddc800b1...HEAD) outlined below.

### Changed
- **(Requirement update!)** The minimal supported version of IntelliJ IDEA is 2023.3 (partial credit to @rikvdkleij).
- Various dependency updates, including the Haskell tools from the Stackage LTS-19 (partial credit to @rikvdkleij).

### Fixed
- [intellij-haskell#671](https://github.com/rikvdkleij/intellij-haskell/pull/671): fix how the replacement text is produced from a hlint output. Thanks to @Thecentury!
- [intellij-haskell#681](https://github.com/rikvdkleij/intellij-haskell/pull/681/): new default location of the Stack executable on Windows. Thanks to @SimonIT!

[docs.readme]: README.md
[intellij-haskell.latest]: https://github.com/rikvdkleij/intellij-haskell/releases/tag/v1.0.0-beta88
[intellij-haskell]: https://github.com/rikvdkleij/intellij-haskell
[keep-a-changelog]: https://keepachangelog.com/en/1.1.0/

[0.0.0]: https://github.com/ForNeVeR/haskeletor/commits/v0.0.0
[Unreleased]: https://github.com/ForNeVeR/haskeletor/compare/v0.0.0...HEAD
