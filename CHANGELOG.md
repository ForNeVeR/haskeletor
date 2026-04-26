<!--
SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>

SPDX-License-Identifier: Apache-2.0
-->

Changelog
=========

The format is based on [Keep a Changelog v1.1.0][keep-a-changelog]. See [the README file][docs.readme] for more details on how the project is versioned.

## [Unreleased] (2.0.0)
### Removed
- **Import Project** functionality (was tied too tightly to IntelliJ IDEA).

### Fixed
- Incorrect SDK path setup on Windows (`Illegal char <:> at index 6: Some(…)`).

## [1.0.1] - 2026-04-18
### Fixed
- **Show Error** action (and some otheres that relied on PSI access) has been [fixed](https://github.com/ForNeVeR/haskeletor/pull/40).
- Fix the action icons.
- Migrate away from internal IntelliJ API (no longer allowed to be used for the plugins uploaded to the Marketplace).

## [1.0.0] - 2026-04-12
This is the initial plugin release under the current name.

Haskeletor is a fork of [intellij-haskell][] (by @rikvdkleij and contributors), and at this point it is a functional equivalent of the latest-released beta, [1.0.0-beta88][intellij-haskell.latest] of said plugin, with [the changes](https://github.com/ForNeVeR/haskeletor/compare/46e18618f349083512249bb1be910b61ddc800b1...v1.0.0) summarized below.

### Changed
- **(Requirement update!)** The minimal supported version of IntelliJ IDEA is 2026.1 (partial credit to @rikvdkleij).
- Various dependency updates, including the Haskell tools from the Stackage LTS-19 (partial credit to @rikvdkleij).

### Fixed
- [intellij-haskell#671](https://github.com/rikvdkleij/intellij-haskell/pull/671): fix how the replacement text is produced from a hlint output. Thanks to @Thecentury!
- [intellij-haskell#681](https://github.com/rikvdkleij/intellij-haskell/pull/681/): new default location of the Stack executable on Windows. Thanks to @SimonIT!
- [#22: HaskellModuleNameIndex deserialization violates equals / hashCode contract](https://github.com/ForNeVeR/haskeletor/issues/22).
- [#25](https://github.com/ForNeVeR/haskeletor/issues/25): add progress indicator for Haskell tool installation.
- [#21: No display name specified in plugin descriptor XML file for configurable me.fornever.haskeletor.settings.HaskellConfigurable](https://github.com/ForNeVeR/haskeletor/issues/21).

[docs.readme]: README.md
[intellij-haskell.latest]: https://github.com/rikvdkleij/intellij-haskell/releases/tag/v1.0.0-beta88
[intellij-haskell]: https://github.com/rikvdkleij/intellij-haskell
[keep-a-changelog]: https://keepachangelog.com/en/1.1.0/

[1.0.0]: https://github.com/ForNeVeR/haskeletor/commits/v1.0.0
[1.0.1]: https://github.com/ForNeVeR/haskeletor/compare/v1.0.0...v1.0.1
[Unreleased]: https://github.com/ForNeVeR/haskeletor/compare/v1.0.1...HEAD
