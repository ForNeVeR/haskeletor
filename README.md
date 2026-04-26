<!--
SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>

SPDX-License-Identifier: Apache-2.0
-->

![Plugin Logo][image.logo] Haskeletor [![Download][badge.plugin]][marketplace.plugin]
=====================================================================================
Haskeletor is a Haskell plugin for IntelliJ IDEA.

It supports code highlighting, code completion, compilation error highlighting — everything you need from an IDE for Haskell.

This plugin is based on [the work of Rik van der Kleij and intellij-haskell contributors](https://github.com/rikvdkleij/intellij-haskell).

Installation
------------
### From JetBrains Marketplace
1. Open the IDE **Plugins** settings page.
2. Search for **Haskeletor** in the search bar.
3. Click **Install** to install the plugin.

Alternatively, [download the plugin from the Marketplace in the browser][marketplace.plugin].

### From the Releases Page
1. Download the latest release from the [Releases page][releases].
2. Use the **Install Plugin from Disk** action to install it into your IDE.

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

### Prerequisites
- Install the latest version of [Stack](https://github.com/commercialhaskell/stack); use `stack upgrade` to confirm you are on the latest version.

### Importing An Existing Project
1. Open your project folder in an IntelliJ-based IDE. It will import all the source files under a default module kind.
2. Go to the **Project Structure** dialog.
3. Make sure you have set up a Haskell SDK on **Platform Settings** → **SDKs** page.

   If there's no Haskell SDK set yet, press the **+** button, choose **Add Stack executable from disk** and enter a path to your `stack` or `stack.exe` file.

   ![Haskell SDK setup][images.haskell-sdk]
4. Choose this SDK as the default for the project on the **Project Settings** → **Project** page.

   ![Default Haskell SDK][images.project-sdk]
5. Go to the **Modules** page.
6. Delete the root module of default kind (the one with the name of the project) and create a new **Haskell** module for the project.
   1. Press the **+** button.
   2. Choose **New Module**.
   3. In the dialog that appears, choose **Haskell module**. Set up the content root as the project folder. The module name is not important, choose anything (e.g. the project folder name).
7. After all set, you might need to reopen the project in the IDE.
8. Haskeletor will compile the project and set up the required tools on startup.

### Remarks
1. IntelliJ's Build action is not (yet) implemented. Project is built when the project is opened and when needed, e.g. when library code is changed and the user navigates to test code;
2. `About Haskell Project` in `Help` menu shows which Haskell GHC/tools are used by the plugin for the project;
3. GHC depends on `libtinfo-dev`. On Ubuntu you can install it with `sudo apt-get install libtinfo-dev`;
4. Haskell tools depend on `libgmp3-dev zlib1g-dev`. On Ubuntu you can install them with `sudo apt-get install libgmp3-dev zlib1g-dev`;
5. Cabal's internal libraries are not (yet) supported;
6. Cabal's common stanzas are not (yet) supported;
7. The Haskell tools are built in an IntelliJ sandbox with LTS-19. So they have no dependency on Stackage resolvers in your projects. After Stackage LTS-13 minor updates one can use `Haskell`>`Update Haskell tools`;
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
- [Maintainer Guide][docs.maintaining]

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
- 2024-2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>

<!-- REUSE-IgnoreEnd -->

[badge.plugin]: https://img.shields.io/jetbrains/plugin/v/31233.svg
[docs.contributing]: CONTRIBUTING.md
[docs.license]: LICENSE.txt
[docs.maintaining]: MAINTAINING.md
[image.logo]: logo/icon_intellij_haskell_32.png
[images.haskell-sdk]: docs/haskell-sdk.png
[images.project-sdk]: docs/project-sdk.png
[marketplace.plugin]: https://plugins.jetbrains.com/plugin/31233-haskeletor
[releases]: https://github.com/ForNeVeR/haskeletor/releases
[reuse.spec]: https://reuse.software/spec-3.3/
