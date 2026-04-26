/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.module

import com.intellij.ide.impl.OpenProjectTaskBuilder
import com.intellij.ide.util.projectWizard._
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.{ApplicationManager, WriteAction}
import com.intellij.openapi.module.{ModifiableModuleModel, Module, ModuleType}
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots._
import com.intellij.openapi.roots.libraries.{Library, LibraryTablesRegistrar, LibraryUtil}
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.roots.ui.configuration.{ModulesProvider, SdkComboBox, SdkComboBoxModel}
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.platform.templates.TemplateModuleBuilder
import com.intellij.util.ui.JBUI
import me.fornever.haskeletor.GlobalInfo
import me.fornever.haskeletor.cabal.PackageInfo
import me.fornever.haskeletor.core.HaskeletorBundle
import me.fornever.haskeletor.core.notifications.HaskellNotificationGroup
import me.fornever.haskeletor.external.component.{HaskellComponentsManager, LibraryPackageInfo}
import me.fornever.haskeletor.external.execution.{CommandLine, StackCommandLine}
import me.fornever.haskeletor.icons.HaskellIcons
import me.fornever.haskeletor.sdk.HaskellSdkType
import me.fornever.haskeletor.settings.HaskellSettingsState
import me.fornever.haskeletor.stackyaml.StackYamlComponent
import me.fornever.haskeletor.util.{FutureUtil, HaskellFileUtil, HaskellProjectUtil, ScalaUtil}

import java.awt.{GridBagConstraints, GridBagLayout}
import java.io.File
import java.nio.file.Path
import javax.swing._
import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

class HaskellModuleBuilder extends TemplateModuleBuilder(null, HaskellModuleType.getInstance, List().asJava) {

  private var cabalInfo: PackageInfo = _

  def setCabalInfo(cabalInfo: PackageInfo): Unit = {
    this.cabalInfo = cabalInfo
  }

  private[this] var isNewProjectWithoutExistingSources = false

  override def getModuleType: ModuleType[_ <: ModuleBuilder] = HaskellModuleType.getInstance

  override def isSuitableSdkType(sdkType: SdkTypeId): Boolean = {
    sdkType == HaskellSdkType.getInstance
  }

  override def getNodeIcon: Icon = HaskellIcons.HaskellLogo

  override def setupRootModel(rootModel: ModifiableRootModel): Unit = {
    rootModel.setSdk(HaskellSdkType.findOrCreateSdk())
    rootModel.inheritSdk()

    val contentEntry = doAddContentEntry(rootModel)
    val project = rootModel.getProject

    if (isNewProjectWithoutExistingSources) {

      createStackProject(project)

      val packageRelativePath = StackYamlComponent.getPackagePaths(project).flatMap(_.headOption)
      packageRelativePath.flatMap(pp => HaskellModuleBuilder.createCabalInfo(rootModel.getProject, project.getBasePath, pp)) match {
        case Some(ci) => cabalInfo = ci
        case None => throw new Exception(s"Couldn't create Haskell module due to failure retrieving or parsing Cabal file for package path `${project.getBasePath}`")
      }
    }

    if (contentEntry != null) {
      HaskellModuleBuilder.addSourceFolders(cabalInfo, contentEntry)

      val stackWorkDirectory = HaskellModuleBuilder.getStackWorkDirectory(this)
      stackWorkDirectory.mkdir()
      Option(LocalFileSystem.getInstance.refreshAndFindFileByIoFile(stackWorkDirectory)).foreach(f =>
        contentEntry.addExcludeFolder(f)
      )
    }
  }

  // Only called in case new project without existing Stack project
  override def getCustomOptionsStep(context: WizardContext, parentDisposable: Disposable): ModuleWizardStep = {
    isNewProjectWithoutExistingSources = true
    new HaskellModuleWizardStep(context, this)
  }

  override def createModule(moduleModel: ModifiableModuleModel): Module = {
    ModuleBuilder.deleteModuleFile(getModuleFilePath)
    val moduleType = getModuleType
    val module = moduleModel.newModule(getModuleFilePath, moduleType.getId)
    val project = module.getProject

    setupModule(module)
    module
  }

  override def getBuilderId: String = {
    val moduleType = getModuleType()
    if (moduleType == null) null else moduleType.getId
  }

  override def createWizardSteps(wizardContext: WizardContext, modulesProvider: ModulesProvider): Array[ModuleWizardStep] = {
    Array()
  }

  override def createFinishingSteps(wizardContext: WizardContext, modulesProvider: ModulesProvider): Array[ModuleWizardStep] = {
    Array()
  }

  override def createProject(name: String, path: String): Project = {
    val builder = new OpenProjectTaskBuilder
    builder.setProjectName(name)
    builder.setUseDefaultProjectAsTemplate(true)
    ProjectManagerEx.getInstanceEx.newProject(
      Path.of(path),
      builder.build(_ => kotlin.Unit.INSTANCE)
    )
  }


  /** From `stack new` output:
    * Package names consist of one or more alphanumeric words separated by hyphens.
    * To avoid ambiguity with version numbers, each of these words must contain at least one letter.
    */
  override def validateModuleName(moduleName: String): Boolean = {
    val valid = moduleName.matches("""([a-zA-Z0-9]*[a-zA-Z]+[a-zA-Z0-9]*)(-([a-zA-Z0-9]*[a-zA-Z]+[a-zA-Z0-9]*))*""")
    if (valid) {
      true
    } else {
      throw new ConfigurationException("Package names should consist of one or more alphanumeric words separated by hyphens. To avoid ambiguity with version numbers, each of these words must contain at least one letter.")
    }
  }

  // To prevent first page of wizard is empty.
  override def isTemplateBased: Boolean = false

  private def createStackProject(project: Project): Unit = {
    val newProjectTemplateName = HaskellSettingsState.getNewProjectTemplateName
    val createModuleAction = ApplicationManager.getApplication.executeOnPooledThread(ScalaUtil.runnable {
      val processOutput = StackCommandLine.run(project, Seq("new", s"${project.getName}", "--bare", newProjectTemplateName, "-p", "author-email:Author email here", "-p", "author-name:Author name here", "-p", "category:App category here", "-p", "copyright:2019 Author name here", "-p", "github-username:Github username here"), timeoutInMillis = 60.seconds.toMillis, enableExtraArguments = false)
      processOutput match {
        case None => throw new Exception("Unknown error while creating new Stack project by using Stack command for creating new project on file system")
        case Some(output) =>
          if (output.getExitCode != 0) {
            throw new Exception(s"Error while creating new Stack project: ${output.getStdout} ${output.getStderr}")
          }
      }
    })
    FutureUtil.waitForValue(project, createModuleAction, "Creating Haskell module", 120) match {
      case None => HaskellNotificationGroup.logErrorBalloonEvent(project, s"Timeout while creating new Stack project by using: `stack new`")
      case Some(_) => ()
    }
  }
}

class HaskellModuleWizardStep(wizardContext: WizardContext, haskellModuleBuilder: HaskellModuleBuilder) extends ModuleWizardStep {

  private val sdkModel = SdkComboBoxModel.createSdkComboBoxModel(
    wizardContext.getProject,
    new ProjectSdksModel(),
    sdkTypeId => sdkTypeId == HaskellSdkType.getInstance,
    null,
    null
  )
  private val sdkChooser = new SdkComboBox(sdkModel)

  override def updateDataModel(): Unit = {
    haskellModuleBuilder.setModuleJdk(sdkChooser.getSelectedSdk)
  }

  override def validate(): Boolean = {
    if (sdkChooser.getSelectedSdk == null) {
      Messages.showErrorDialog("You can't create a Haskell project without Stack configured as SDK", "No Haskell Tool Stack specified")
      false
    } else {
      true
    }
  }

  override def getComponent: JComponent = {
    val panel = new JPanel(new GridBagLayout)
    panel.setBorder(BorderFactory.createEtchedBorder())
    panel.add(
      new JLabel(HaskeletorBundle.message("module-wizard.sdk-chooser.label")),
      new GridBagConstraints(
        0,
        0,
        1,
        1,
        0.0,
        0.0,
        GridBagConstraints.WEST,
        GridBagConstraints.NONE,
        JBUI.insetsRight(5),
        0,
        0)
    )

    panel.add(
      sdkChooser,
      new GridBagConstraints(
        0,
        1,
        1,
        1,
        1.0,
        0.0,
        GridBagConstraints.WEST,
        GridBagConstraints.HORIZONTAL,
        JBUI.insetsTop(5),
        0,
        0)
    )

    panel
  }
}

object HaskellModuleBuilder {

  def addSourceFolders(cabalInfo: PackageInfo, contentEntry: ContentEntry): Unit = {
    cabalInfo.sourceRoots.foreach(path => {
      Option(LocalFileSystem.getInstance.refreshAndFindFileByPath(FileUtil.toSystemIndependentName(path))).foreach(f =>
        contentEntry.addSourceFolder(f, false)
      )
    })

    cabalInfo.testSourceRoots.foreach(path => {
      Option(LocalFileSystem.getInstance.refreshAndFindFileByPath(FileUtil.toSystemIndependentName(path))).foreach(f =>
        contentEntry.addSourceFolder(f, true)
      )
    })
  }

  def createCabalInfo(project: Project, modulePath: String, packageRelativePath: String): Option[PackageInfo] = {
    val moduleDirectory = getModuleRootDirectory(packageRelativePath, modulePath)
    for {
      cabalFile <- getCabalFile(project, moduleDirectory)
      cabalInfo <- getCabalInfo(project, cabalFile)
    } yield cabalInfo
  }

  def getStackWorkDirectory(moduleBuilder: ModuleBuilder): File = {
    new File(moduleBuilder.getContentEntryPath, GlobalInfo.StackWorkDirName)
  }

  private def getDependencies(project: Project, module: Module, packageName: String, libraryDependencies: Seq[HaskellLibraryDependency]): Seq[HaskellDependency] = {
    val cabalInfo = HaskellComponentsManager.findCabalInfos(project).find(_.packageName == packageName)
    val libPackages = cabalInfo.flatMap(_.library.map(_.buildDepends)).getOrElse(Seq())
    val exePackages = cabalInfo.map(_.executables.flatMap(_.buildDepends)).getOrElse(Seq())
    val testPackages = cabalInfo.map(_.testSuites.flatMap(_.buildDepends)).getOrElse(Seq())
    val benchPackages = cabalInfo.map(_.benchmarks.flatMap(_.buildDepends)).getOrElse(Seq())

    val packages = (libPackages ++ exePackages ++ testPackages ++ benchPackages).distinct.filterNot(n => n == packageName || n == "rts")

    val projectModulePackageNames = HaskellComponentsManager.findProjectModulePackageNames(project)

    packages.flatMap(n => {
      projectModulePackageNames.find(_._2 == n).map(_._1) match {
        case None =>
          libraryDependencies.find(_.name == n) match {
            case Some(info) => Some(HaskellLibraryDependency(info.name, info.version))
            case None => None
          }
        case Some(m) =>
          val cabalInfo = HaskellComponentsManager.findCabalInfos(project).find(_.packageName == n)
          cabalInfo.map(ci => HaskellModuleDependency(ci.packageName, ci.packageVersion, m))
      }
    })
  }

  def getModuleRootDirectory(packagePath: String, modulePath: String): File = {
    if (packagePath == ".") {
      new File(modulePath).getCanonicalFile
    } else {
      new File(modulePath, packagePath).getCanonicalFile
    }
  }

  private def getCabalFile(project: Project, moduleDirectory: File): Option[File] = {
    HaskellProjectUtil.findCabalFile(moduleDirectory) match {
      case Some(f) => Option(f)
      case None =>
        Messages.showErrorDialog(s"Couldn't create Haskell module because Cabal file can't be found in `$moduleDirectory`", "Haskell module can't be created")
        None
    }
  }

  private def getCabalInfo(project: Project, cabalFile: File): Option[PackageInfo] = {
    PackageInfo.create(project, cabalFile) match {
      case Some(f) => Option(f)
      case None =>
        Messages.showErrorDialog(project, s"Couldn't create Haskell module because Cabal file `$cabalFile` can't be parsed", "Haskell module can't be created")
        None
    }
  }

  private def getDependsOnPackageInfos(libraryPackageInfos: Seq[LibraryPackageInfo], modulePackageInfos: Seq[LibraryPackageInfo]): Seq[LibraryPackageInfo] = {
    val libraryPackageInfoByName = libraryPackageInfos.map(pi => (pi.packageName, pi)).toMap

    @tailrec
    def go(packageInfos: Seq[LibraryPackageInfo], dependsOnPackageInfos: ListBuffer[LibraryPackageInfo]): Seq[LibraryPackageInfo] = {
      val dependsOn = packageInfos.flatMap { pi =>
        dependsOnPackageInfos += pi
        pi.dependsOnPackageIds.map(_.name).flatMap(libraryPackageInfoByName.get).filterNot(dependsOnPackageInfos.contains)
      }

      if (dependsOn.isEmpty) {
        dependsOnPackageInfos.toSeq
      } else {
        go(dependsOn.distinct, dependsOnPackageInfos)
      }
    }

    val dependsOnPackageInfos = ListBuffer[LibraryPackageInfo]()

    go(modulePackageInfos, dependsOnPackageInfos).filterNot(_.packageName == "rts")
  }

  private def getModuleLibraryDependencies(moduleDependencies: Seq[HaskellDependency], libraryPackageInfos: Seq[LibraryPackageInfo]): Seq[HaskellLibraryDependency] = {
    val moduleLibraryPackageInfos = moduleDependencies.filter(_.isInstanceOf[HaskellLibraryDependency]).flatMap(d => libraryPackageInfos.find(_.packageName == d.name))
    val dependsOnPackageInfos = getDependsOnPackageInfos(libraryPackageInfos, moduleLibraryPackageInfos)
    dependsOnPackageInfos.map(pi => HaskellLibraryDependency(pi.packageName, pi.version))
  }

  def addLibrarySources(project: Project, update: Boolean): Unit = {
    val projectLibDirectory = HaskellProjectUtil.getProjectLibrarySourcesDirectory(project)
    if (update || getProjectLibraryTable(project).getLibraries.isEmpty || !projectLibDirectory.exists()) {
      HaskellSdkType.getStackPath(project).foreach(stackPath => {
        StackCommandLine.updateStackIndex(project)

        if (!projectLibDirectory.exists()) {
          FileUtil.createDirectory(projectLibDirectory)
        }

        val libraryPackageInfos = HaskellComponentsManager.findLibraryPackageInfos(project)
        val libraryDependencies = libraryPackageInfos.map(pi => HaskellLibraryDependency(pi.packageName, pi.version))

        val projectModulePackageNames = HaskellComponentsManager.findProjectModulePackageNames(project)

        val dependenciesByModule = for {
          (module, packageName) <- projectModulePackageNames
          moduleDependencies = getDependencies(project, module, packageName, libraryDependencies)
        } yield (module, moduleDependencies, getModuleLibraryDependencies(moduleDependencies, libraryPackageInfos))

        val projectLibraryDependencies = dependenciesByModule.flatMap(_._3).distinct

        downloadHaskellPackageSources(project, projectLibDirectory, stackPath, projectLibraryDependencies)

        setupProjectLibraries(project, projectLibraryDependencies, projectLibDirectory)

        dependenciesByModule.foreach { case (module, moduleDependencies, moduleLibraryDependencies) =>
          setupModuleLibraries(module, moduleDependencies ++ moduleLibraryDependencies)
        }
      })
    }
  }

  private def downloadHaskellPackageSources(project: Project, projectLibDirectory: File, stackPath: String, libraryDependencies: Seq[HaskellLibraryDependency]): Unit = {
    libraryDependencies.filterNot(libraryDependency => getPackageDirectory(projectLibDirectory, libraryDependency).exists()).foreach(libraryDependency => {
      val output = CommandLine.runInWorkDir(project, projectLibDirectory.getAbsolutePath, stackPath, Seq("--no-nix", "unpack", libraryDependency.nameVersion), 10000)
      if (output.getExitCode != 0 && libraryDependency.name != "ghc-boot-th") {
        HaskellNotificationGroup.logWarningBalloonEvent(project, s"Could not download sources for ${libraryDependency.nameVersion} | Output: ${output.getStderr}")
      }
    })
  }

  private def getPackageDirectory(projectLibDirectory: File, libraryDependency: HaskellLibraryDependency) = {
    new File(projectLibDirectory, libraryDependency.nameVersion)
  }

  private def getProjectLibraryTable(project: Project) = {
    LibraryTablesRegistrar.getInstance.getLibraryTable(project)
  }

  private def setupProjectLibraries(project: Project, libraryDependencies: Seq[HaskellLibraryDependency], projectLibDirectory: File): Unit = {
    getProjectLibraryTable(project).getLibraries.foreach(library => {
      libraryDependencies.find(_.nameVersion == library.getName) match {
        case Some(_) =>
          if (library.getFiles(OrderRootType.SOURCES).isEmpty) {
            removeProjectLibrary(project, library)
          }
        case None => removeProjectLibrary(project, library)
      }
    })

    libraryDependencies.foreach(dependency => {
      val projectLibrary = getProjectLibraryTable(project).getLibraryByName(dependency.nameVersion)
      if (projectLibrary == null) {
        createProjectLibrary(project, dependency, projectLibDirectory)
      }
    })
  }

  private def findLibraryDependency(dependencies: Seq[HaskellDependency], name: String) = {
    dependencies.find {
      case d: HaskellLibraryDependency => d.nameVersion == name
      case _ => false
    }
  }

  private def findModuleDependency(dependencies: Seq[HaskellDependency], module: Module) = {
    dependencies.find {
      case d: HaskellModuleDependency => d.module == module
      case _ => false
    }
  }

  private def setupModuleLibraries(module: Module, moduleDependencies: Seq[HaskellDependency]): Unit = {
    val project = module.getProject

    ModuleRootModificationUtil.updateModel(module, (modifiableRootModel: ModifiableRootModel) => {
      modifiableRootModel.getOrderEntries.foreach {
        case e: LibraryOrderEntry => if (findLibraryDependency(moduleDependencies, e.getLibraryName).isEmpty) modifiableRootModel.removeOrderEntry(e)
        case e: ModuleOrderEntry => if (findModuleDependency(moduleDependencies, e.getModule).isEmpty) modifiableRootModel.removeOrderEntry(e)
        case _ => ()
      }
    })

    moduleDependencies.foreach {
      case d: HaskellLibraryDependency =>
        if (LibraryUtil.findLibrary(module, d.nameVersion) == null) {
          val projectLibrary = getProjectLibraryTable(project).getLibraryByName(d.nameVersion)
          if (projectLibrary == null) {
            HaskellNotificationGroup.logInfoEvent(project, "Could not find project library " + projectLibrary.getName)
          }
          addModuleLibrary(module, projectLibrary)
        }
      case d: HaskellModuleDependency =>
        ModuleRootModificationUtil.updateModel(module, (modifiableRootModel: ModifiableRootModel) => {
          if (module != d.module && modifiableRootModel.findModuleOrderEntry(d.module) == null) {
            modifiableRootModel.addModuleOrderEntry(d.module)
          }
        })
    }
  }

  private def removeProjectLibrary(project: Project, library: Library): Unit = {
    getProjectLibraryTable(project).getLibraries.find(_.getName == library.getName).foreach(library => {
      val model = getProjectLibraryTable(project).getModifiableModel
      model.removeLibrary(library)
      ApplicationManager.getApplication.invokeAndWait(ScalaUtil.runnable(WriteAction.run(() => model.commit())))
    })
  }

  private def createProjectLibrary(project: Project, libraryDependency: HaskellLibraryDependency, projectLibDirectory: File): Library = {
    val projectLibraryTableModel = getProjectLibraryTable(project).getModifiableModel
    val (libraryName, sourceRootPath) = (libraryDependency.nameVersion, getPackageDirectory(projectLibDirectory, libraryDependency))
    val library = projectLibraryTableModel.createLibrary(libraryName)
    val libraryModel = library.getModifiableModel
    val sourceRootUrl = HaskellFileUtil.getUrlByPath(sourceRootPath.getAbsolutePath)
    libraryModel.addRoot(sourceRootUrl, OrderRootType.CLASSES)
    libraryModel.addRoot(sourceRootUrl, OrderRootType.SOURCES)

    ApplicationManager.getApplication.invokeAndWait(ScalaUtil.runnable(WriteAction.run(() => libraryModel.commit())))
    ApplicationManager.getApplication.invokeAndWait(ScalaUtil.runnable(WriteAction.run(() => projectLibraryTableModel.commit())))
    library
  }

  private def addModuleLibrary(module: Module, library: Library): Unit = {
    ModuleRootModificationUtil.updateModel(module, (modifiableRootModel: ModifiableRootModel) => {
      modifiableRootModel.addLibraryEntry(library)
    })
  }

  trait HaskellDependency {
    def name: String

    def version: String

    def nameVersion = s"$name-$version"
  }

  case class HaskellLibraryDependency(name: String, version: String) extends HaskellDependency

  case class HaskellModuleDependency(name: String, version: String, module: Module) extends HaskellDependency

}
