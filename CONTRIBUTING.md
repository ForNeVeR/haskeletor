<!--
SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
SPDX-FileCopyrightText: 2024-2026 Friedrich von Never <friedrich@fornever.me>

SPDX-License-Identifier: Apache-2.0
-->

Contributor Guide
=================

Development Prerequisites
-------------------------
For opening the project in an IDE/editor, JDK 17 or later might need to be installed.

For building, it is not required, as the JDK will be auto-downloaded by the Gradle wrapper.

Some of the tasks to work with the repository infrastructure will require you to install the following components:
- the [REUSE][reuse] tool;
- [PowerShell Core][powershell];
- [.NET SDK 10][dotnet-sdk] or later.

Building the Project
--------------------
Execute the following shell command:
```console
$ ./gradlew buildPlugin
```

Running Tests
-------------
Execute the following shell command:
```console
$ ./gradlew test
```

If you wish to perform a full automated check, including the integration tests and the plugin verification, execute the following shell command:
```console
$ ./gradlew check
```

Local Deployment
----------------
To run the development version of the plugin in IntelliJ IDEA, execute the following shell command:
```console
$ ./gradlew runIde
```

We also have a configuration to run Rider:
```console
$ ./gradlew runRider
```

If you want to install the locally-built plugin to your IDE, execute the following shell command:
```console
$ ./gradlew buildPlugin
```

Then install the plugin from `build/distributions` folder via **Plugins** → **Install plugin from disk** in the IntelliJ settings.

Opening in the IDE
------------------
1. Make sure you have a compatible version of the JDK set up for the project.
2. Open the project in IntelliJ IDEA.

Development Remarks
-------------------
This project uses several `.flex` files for lexer definitions, and `.bnf` files for parser definitions.

Read more on how to author these files in the documentation on [the Grammar-Kit project page][grammar-kit].

License Automation
------------------
<!-- REUSE-IgnoreStart -->
If the CI asks you to update the file licenses, follow one of these:
1. Update the headers manually (look at the existing files), something like this:
   ```csharp
   // SPDX-FileCopyrightText: %year% %your name% <%your contact info, e.g. email%>
   //
   // SPDX-License-Identifier: Apache-2.0
   ```
   (accommodate to the file's comment style if required).
2. Alternately, use the [REUSE][reuse] tool:
   ```console
   $ reuse annotate --license Apache-2.0 --copyright '%your name% <%your contact info, e.g. email%>' %file names to annotate%
   ```

(Feel free to attribute the changes to "haskeletor contributors <https://github.com/ForNeVeR/haskeletor>" instead of your name in a multi-author file, or if you don't want your name to be mentioned in the project's source: this doesn't mean you'll lose the copyright.)
<!-- REUSE-IgnoreEnd -->

File Encoding Changes
---------------------
If the automation asks you to update the file encoding (line endings or UTF-8 BOM) in certain files, run the following PowerShell script ([PowerShell Core][powershell] is recommended to run this script):
```console
$ pwsh -c "Install-Module VerifyEncoding -Repository PSGallery -RequiredVersion 2.3.0 -Force && Test-Encoding -AutoFix -ExcludeExtensions '.bat', '.DotSettings'"
```

The `-AutoFix` switch will automatically fix the encoding issues, and you'll only need to commit and push the changes.

GitHub Actions
--------------
If you want to update the GitHub Actions used in the project, edit the file that generated them: `scripts/github-actions.fsx`.

Then run the following shell command:
```console
$ dotnet fsi scripts/github-actions.fsx
```

[dotnet-sdk]: https://dotnet.microsoft.com/en-us/download
[grammar-kit]: https://github.com/JetBrains/Grammar-Kit
[powershell]: https://learn.microsoft.com/en-us/powershell/scripting/install/installing-powershell
[reuse]: https://reuse.software/
