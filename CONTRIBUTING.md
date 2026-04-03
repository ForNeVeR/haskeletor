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
- [PowerShell Core][powershell].

How to build project
--------------------
1. Clone this project;
1. Go to root of project;
1. Start `sbt` from the shell which will download automatically the IntelliJ Community SDK with sources;
1. Install/enable the following plugins in IntelliJ: Plugin Devkit, Grammar-Kit and PsiViewer;
1. Import this project as an sbt project in IntelliJ;
1. Select `Build`>`Build Project`;


How to prepare plugin for deployment
------------------------------------
1. Right-click on top of `intellij-haskell.iml` inside `intellij-haskell` folder;
1. Select `Import module`;
1. Be sure `unmanaged-jars` dependency is set to `provided` inside `Project structure`>`Project settings`>`Modules`>`Dependencies` (btw, setting `provided` inside sbt file gives an error);
1. Right-click on top of `intellij-haskell` plugin module and select `Prepare Plugin Module 'intellij-haskell' for deployment`;


How to run/debug plugin inside IntelliJ
---------------------------------------
1. In `SDKs` create Jetbrains JDK by choosing `Add JDK...` and selecting the path to the JDK which is included in the IntelliJ application;
1. Create Project SDK in `Project structure`>`Project settings`>`Project` by using the `Add SDK` option and selecting `Intellij Platform Plugin SDK` and setting the path to the IntelliJ folder.
 Select as JDK the just created JDK of the previous step;
1. Set Plugin SDK settings right inside `Project structure`>`Platform settings`>`SDKs`. For example, set  SDK home path to `idea/142.5239.7` inside project root folder;
1. Set `Module-SDK` right for `intellij-haskell` plugin module inside `Project structure`>`Project structure`>`Project settings`>`Modules`;
1. To run plugin inside IntelliJ, the first-run configuration has to be created. Navigate to `Run`>`Edit configurations` and create `plugin` configuration for `intellij-haskell`;


Development remarks
-------------------
1. After making changes to `_HaskellLexer.flex`, run `Run Flex Generator`. This will generate `_HaskellLexer.java`;
1. After making changes to `haskell.bnf`, run `Generate Parser Code`. This will generate parser Java files in `gen` directory;
1. Never touch the generated code. They should be committed as-is.
1. Add `sources.zip` "inside" `idea`>[`idea build #`] to `Project structure`>`Project settings`>`Modules`>`Dependencies`>`unmanaged-jars` to see IntelliJ sources;


### After following the above steps the `Project Structure` should look like:
* ![Project](images/Project.png)
* ![Modules](images/Modules.png)
* ![IntelliJ Haskell module](images/Intellij%20Haskell%20module.png)
* ![IntelliJ SDK](images/IntelliJ%20SDK.png)
* ![Jetbrains JDK](images/Jetbrains%20JDK.png)
* ![Run configuration](images/Run%20configuration.png)

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

[powershell]: https://learn.microsoft.com/en-us/powershell/scripting/install/installing-powershell
[reuse]: https://reuse.software/
