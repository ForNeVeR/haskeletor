<!--
SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>

SPDX-License-Identifier: Apache-2.0
-->

![logo](logo/icon_intellij_haskell_32.png) Haskeletor
=======================
Haskeletor is a Haskell plugin for IntelliJ IDEA.

It supports code highlighting, code completion, compilation error highlighting — everything you need from an IDE for Haskell.

This plugin is based on [the work of Rik van der Kleij and intellij-haskell contributors](https://github.com/rikvdkleij/intellij-haskell).

Installation
------------

### From Sources
Read [the contributor guide][docs.contributing]. Short version:
1. To run the debug version of the IDE with the installed plugin, run the following shell command:
   ```console
   $ ./gradlew runIde
   ```
2. To build a local version of the plugin for subsequent installation into a compatible IDE, run the following shell command:
   ```console
   $ ./gradlew buildPlugin
   ```
   Then, grab a plugin distribution from the `build/distributions` directory, and use the **Install Plugin from Disk** action to install it into your IDE.

Usage
-----
- Install the latest version of [Stack](https://github.com/commercialhaskell/stack); use `stack upgrade` to confirm you are on the latest version.
- Set up the project:
    - Make sure your Stack project builds without errors. Preferably by using: `stack build --test --haddock --no-haddock-hyperlink-source`;
    - After your project is built successfully, import an existing project by:
        - Inside IntelliJ use `File`>`New`>`Project from Existing Sources...` from the IntelliJ menu;
        - In the `Welcome to IntelliJ IDEA` dialog use `Open or Import Project`;
    - In the `New Project` wizard select `Import project from external model` and check `Haskell Stack`;
    - On the next page of wizard configure `Project SDK` by configuring `Haskell Tool Stack` by selecting a path to `stack` binary, e.g. `/usr/local/bin/stack` (you can use `which stack` on Linux or macOS or `where stack` on windows to find the path);
    - Finish wizard and project will be opened;
    - Wizard will automatically configure which folders are sources, test and which to exclude;
    - Plugin will automatically build Haskell Tools (HLint, Hoogle, Ormolu, and Stylish Haskell) to prevent incompatibility issues
    - Check `Project structure`>`Project settings`>`Modules` which folders to exclude (like `.stack-work` and `dist`) and which folders are `Source` and `Test` (normally `src` and `test`);
    - Plugin will automatically download library sources. They will be added as source libraries to module(s).
    - After changing the Cabal file and/or `stack.yaml` use `Haskell`>`Haskell`>`Update Settings and Restart REPLs` to download missing library sources and update the project settings;
    - The `Event Log` will display what's going on in the background. Useful when something fails. It's disabled by default.
      It can be enabled by checking the `Haskell Log` checkbox in the `Event Log`>`Settings` or `Settings`>`Appearance & Behavior`>`Notifications`;

### Remarks
1. IntelliJ's Build action is not (yet) implemented. Project is built when the project is opened and when needed, e.g. when library code is changed and the user navigates to test code;
2. `About Haskell Project` in `Help` menu shows which Haskell GHC/tools are used by the plugin for the project;
3. GHC depends on `libtinfo-dev`. On Ubuntu you can install it with `sudo apt-get install libtinfo-dev`;
4. Haskell tools depend on `libgmp3-dev zlib1g-dev`. On Ubuntu you can install them with `sudo apt-get install libgmp3-dev zlib1g-dev`;
5. Cabal's internal libraries are not (yet) supported;
6. Cabal's common stanzas are not (yet) supported;
7. The Haskell tools are built in an IntelliJ sandbox with LTS-16. So they have no dependency on Stackage resolvers in your projects. After Stackage LTS-13 minor updates one can use `Haskell`>`Update Haskell tools`;
8. Stack REPLs are running in the background. You can restart them by `Haskell`>`Update Settings and Restart REPLs`.

Plugin Features
---------------
- Syntax highlighting;
- Error/warning highlighting;
- Haskell Problems View. This tool window displays GHC messages for the currently edited files;
- Find usages of identifiers;
- Resolve references to identifiers;
- Code completion;
- In-place rename identifiers;
- View type info from (selected) expression;
- View sticky type info;
- View expression info;
- View quick documentation;
- View quick definition;
- Structure view;
- Goto to declaration (called `Navigate`>`Declaration` in IntelliJ menu);
- Navigate to declaration (called `Navigate`>`Class` in IntelliJ menu);
- Navigate to an identifier (called `Navigate`>` Symbol` in IntelliJ menu);
- Goto instance declaration (called `Navigate`>`Instance Declaration` in IntelliJ menu);
- Navigate to declaration or identifier powered by Hoogle (called `Navigate`>`Navigation by Hoogle` in IntelliJ menu);
- Inspection by HLint;
- Quick fixes for HLint suggestions;
- Show error action to view formatted messages. Useful in case message consists of multiple lines (Ctrl-F10, Meta-F10 on Mac OSX);
- Intention actions to add language extension (depends on compiler error), add top-level type signature (depends on compiler warning);
- Intention action to select which module to import if the identifier is not in scope;
- Default code formatting by `ormolu`. Alternatively by `stylish-Haskell`.
- Code completion for project module names, language extensions, and package names in Cabal file;
- Running REPL, tests, and executables via `Run Configurations`;
- Smart code completion on typed holes (since GHC 8.4);

Documentation
-------------
- [Contributor Guide][docs.contributing]

License
-------
The project is distributed under the terms of [the Apache 2.0 license][docs.license]
(unless a particular file states otherwise).

The license indication in the project's sources is compliant with the [REUSE specification v3.3][reuse.spec].

<!-- REUSE-IgnoreStart -->
<!-- Prepared with help of the PowerShell script:
  $licenses = pipx run reuse lint --json | ConvertFrom-Json
  $licenses.files | % { $_.copyrights.value } | Select-Object -Unique -->

Copyright holders:

- 2000-2015 JetBrains s.r.o.
- 2012-2014 Sergey Ignatov
- 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
- 2014-2022 Rik van der Kleij
- 2024-2026 Friedrich von Never <friedrich@fornever.me>
- 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>

<!-- REUSE-IgnoreEnd -->

[docs.contributing]: CONTRIBUTING.md
[docs.license]: LICENSE.txt
[reuse.spec]: https://reuse.software/spec-3.3/
