/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.external.component

import com.intellij.ProjectTopics
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.{ModuleRootEvent, ModuleRootListener}
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.{PsiTreeChangeAdapter, PsiTreeChangeEvent}
import com.intellij.ui.EditorNotifications
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import me.fornever.haskeletor.action.HaskellReformatAction
import me.fornever.haskeletor.annotator.HaskellAnnotator
import me.fornever.haskeletor.core.HaskeletorBundle
import me.fornever.haskeletor.core.notifications.HaskellNotificationGroup
import me.fornever.haskeletor.external.repl.StackRepl.LibType
import me.fornever.haskeletor.external.repl.StackReplsManager
import me.fornever.haskeletor.notification.ConfigFileWatcher
import me.fornever.haskeletor.projectmodel.HaskellProjectManager
import me.fornever.haskeletor.psi.HaskellPsiExtensions._
import me.fornever.haskeletor.psi.HaskellPsiUtil
import me.fornever.haskeletor.psi.stubs.types.HaskellFileElementType
import me.fornever.haskeletor.settings.HTool.{Hlint, Hoogle, Ormolu}
import me.fornever.haskeletor.settings.{GlobalInfo, HTool, HaskellSettingsState}
import me.fornever.haskeletor.stack.{HaskellToolInstaller, StackBuilder}
import me.fornever.haskeletor.stackyaml.StackYamlComponent
import me.fornever.haskeletor.util._
import me.fornever.haskeletor.util.index.{HaskellFileIndex, HaskellModuleNameIndex}

import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}

object StackProjectManager {

  import me.fornever.haskeletor.util.ScalaUtil._

  def isInitializing(project: Project): Boolean = {
    getStackProjectManager(project).exists(_.initializing)
  }

  def isHoogleAvailable(project: Project): Option[String] = {
    getStackProjectManager(project).flatMap(_.hoogleAvailable)
  }

  def isHlintAvailable(project: Project): Option[String] = {
    getStackProjectManager(project).flatMap(_.hlintAvailable)
  }

  def isOrmoluAvailable(project: Project): Option[String] = {
    getStackProjectManager(project).flatMap(_.ormoluAvailable)
  }

  def isInstallingHaskellTools(project: Project): Boolean = {
    getStackProjectManager(project).exists(_.installingHaskellTools)
  }

  def isHaddockBuilding(project: Project): Boolean = {
    getStackProjectManager(project).exists(_.haddockBuilding)
  }

  def setHaddockBuilding(project: Project, state: Boolean): Unit = {
    getStackProjectManager(project).foreach(_.haddockBuilding = state)
  }

  def isPreloadingAllLibraryIdentifiers(project: Project): Boolean = {
    getStackProjectManager(project).exists(_.preloadingAllLibraryIdentifiers)
  }

  private def start(project: Project, workingDirectory: Path): Unit = {
    init(project, workingDirectory)
  }

  def restart(project: Project): Unit = {
    HaskellFileUtil.saveFiles(project)

    val workingDirectory = findWorkingDirectory(project)
    if (workingDirectory.isEmpty) {
      logger.error("Stack working directory cannot be determined for project " + project.getName)
      return
    }
    init(project, workingDirectory.get, restart = true)
  }

  def getStackProjectManager(project: Project): Option[StackProjectManager] = {
    project.isDisposed.optionNot(project.getComponent(classOf[StackProjectManager]))
  }

  def getProjectLibraryFileWatcher(project: Project): Option[ProjectLibraryFileWatcher] = {
    getStackProjectManager(project).map(_.projectLibraryFileWatcher)
  }

  def launchInstallHaskellTools(project: Project, update: Boolean): Unit = {
    getStackProjectManager(project).foreach(_.installingHaskellTools = true)

    def isToolAvailable(tool: HTool) = {
      if (HaskellSettingsState.useCustomTools) {
        tool match {
          case Hlint => HaskellSettingsState.hlintPath
          case Hoogle => HaskellSettingsState.hooglePath
          case Ormolu => HaskellSettingsState.ormoluPath
        }
      } else {
        if (GlobalInfo.toolPath(tool).exists()) {
          Some(GlobalInfo.toolPath(tool).getAbsolutePath)
        } else {
          None
        }
      }
    }

    def isHLintInstalled: Boolean = {
      GlobalInfo.toolPath(HTool.Hlint).exists()
    }

    val forceMakeChanges = update || !isHLintInstalled
    val useSystemGhc = !StackYamlComponent.isNixEnabled(project) && HaskellSettingsState.useSystemGhc

    HaskellToolInstaller.getInstance(project)
      .launchInstallHaskellTools(
        forceMakeChanges,
        useSystemGhc,
        HaskeletorBundle.message("progress.installing-tools.title"),
        () => {
          getStackProjectManager(project).foreach(_.hlintAvailable = isToolAvailable(HTool.Hlint))

          getStackProjectManager(project).foreach(_.hoogleAvailable = isToolAvailable(HTool.Hoogle))

          getStackProjectManager(project).foreach(_.ormoluAvailable = isToolAvailable(HTool.Ormolu))
        },
        () => getStackProjectManager(project).foreach(_.installingHaskellTools = false)
      )
  }

  private def ghcOptions(project: Project) = {
    if (HaskellProjectUtil.setNoDiagnosticsShowCaretFlag(project)) {
      Seq("--ghc-options", "-fno-diagnostics-show-caret")
    } else {
      Seq()
    }
  }

  private def init(project: Project, workingDirectory: Path, restart: Boolean = false): Unit = {
    val haskellProjectManager = HaskellProjectManager.getInstance(project)
    if (haskellProjectManager.isHaskellProject.getValueOrNull == true) {
      if (isInitializing(project)) {
        HaskellNotificationGroup.logWarningBalloonEvent(project, "Action not possible whilst project is initializing")
      } else {
        getStackProjectManager(project).foreach(_.initializing = true)
        if (restart) {
          HaskellNotificationGroup.logInfoEvent(project, "Restarting Haskell project")
        } else {
          HaskellNotificationGroup.logInfoEvent(project, "Initializing Haskell project")
        }

        if (getStackProjectManager(project).exists(_.installingHaskellTools == false)) {
          getStackProjectManager(project).foreach(_.hlintAvailable = None)
          getStackProjectManager(project).foreach(_.hoogleAvailable = None)
          getStackProjectManager(project).foreach(_.ormoluAvailable = None)
          launchInstallHaskellTools(project, update = false)
        }

        StackBuilder.getInstance(project).launchBuildWorkflow(
          workingDirectory,
          () => HaskellComponentsManager.findStackComponentInfos(project)
            .filter(_.stanzaType == LibType)
            .map(_.target)
            .asJava,
          () => ghcOptions(project).asJava,
          (progressIndicator: ProgressIndicator) => {
            try {
              if (restart) {
                val projectRepls = StackReplsManager.getRunningProjectRepls(project)
                progressIndicator.setText("Busy stopping REPLs")
                StackReplsManager.getGlobalRepl(project).foreach(_.exit())
                StackReplsManager.getGlobalRepl2(project).foreach(_.exit())
                projectRepls.foreach(_.exit())

                progressIndicator.setText("Busy cleaning cache")
                HaskellComponentsManager.invalidateGlobalCaches(project)

                ApplicationManager.getApplication.runReadAction(ScalaUtil.runnable {
                  getStackProjectManager(project).foreach(_.initStackReplsManager(workingDirectory))
                })
              }

              val replsLoad = ApplicationManager.getApplication.executeOnPooledThread(ScalaUtil.runnable {
                StackReplsManager.getReplsManager(project).foreach(_.projectReplTargets.filter(_.stanzaType == LibType).foreach { info =>
                  progressIndicator.setText("Busy starting project REPL " + info.targetsName)
                  StackReplsManager.getProjectRepl(project, info) match {
                    case Some(r) if r.available => HaskellNotificationGroup.logInfoEvent(project, s"REPL ${info.targetsName} is started")
                    case _ => HaskellNotificationGroup.logWarningEvent(project, s"REPL ${info.targetsName} isn't started")
                  }
                  Thread.sleep(1000) // Have to wait between starting the REPLs otherwise timeouts while starting
                })

                val projectFiles = ApplicationUtil.runReadActionWithFileAccess(project,
                  if (project.isDisposed) {
                    Iterable()
                  } else {
                    HaskellFileIndex.findProjectHaskellFiles(project)
                  }, "Finding project files with imported module names")

                val projectFilesWithImportedModuleNames = projectFiles match {
                  case Right(files) => Some(files.map(pf => (pf, ApplicationUtil.runReadAction(HaskellPsiUtil.findImportDeclarations(pf), Some(project)).flatMap(id => ApplicationUtil.runReadAction(id.getModuleName, Some(project))))))
                  case Left(_) =>
                    HaskellNotificationGroup.logInfoEvent(project, "Couldn't retrieve project files")
                    None
                }

                projectFilesWithImportedModuleNames match {
                  case Some(fm) =>
                    fm.foreach { case (pf, moduleNames) =>
                      HaskellPsiUtil.findModuleName(pf).foreach(BrowseModuleComponent.findModuleIdentifiersSync(project, _))

                      moduleNames.foreach(mn => HaskellModuleNameIndex.findFilesByModuleName(project, mn))
                      HaskellNotificationGroup.logInfoEvent(project, "Loading module identifiers " + moduleNames.mkString(", "))
                      moduleNames.foreach(mn => BrowseModuleComponent.findModuleIdentifiersSync(project, mn))
                    }
                  case None => HaskellNotificationGroup.logInfoEvent(project, "Couldn't load module identifiers due to timeout")
                }
              })

              progressIndicator.setText("Busy starting global Stack REPL")
              StackReplsManager.getGlobalRepl(project)

              progressIndicator.setText("Busy starting global Stack REPL2")
              StackReplsManager.getGlobalRepl2(project)

              progressIndicator.setText("Busy preloading global project info")
              GlobalProjectInfoComponent.findGlobalProjectInfo(project)

              progressIndicator.setText("Busy preloading library packages info")
              LibraryPackageInfoComponent.preloadLibraryPackageInfos(project)

              progressIndicator.setText("Busy preloading Stack component info cache")
              val preloadStackComponentInfoCache = ApplicationManager.getApplication.executeOnPooledThread(ScalaUtil.callable {
                HaskellComponentsManager.preloadStackComponentInfoCache(project)
              })

              progressIndicator.setText("Busy preloading library identifiers")
              val preloadLibraryIdentifiersCacheFuture = ApplicationManager.getApplication.executeOnPooledThread(ScalaUtil.runnable {
                if (!project.isDisposed) {
                  HaskellComponentsManager.preloadLibraryIdentifiersCaches(project)
                }
              })

              progressIndicator.setText("Busy preloading all library identifiers")
              ApplicationManager.getApplication.executeOnPooledThread(ScalaUtil.runnable {
                if (!project.isDisposed) {
                  getStackProjectManager(project).foreach(_.preloadingAllLibraryIdentifiers = true)
                  try {
                    HaskellComponentsManager.preloadAllLibraryIdentifiersCaches(project)

                    if (!project.isDisposed) {
                      HaskellNotificationGroup.logInfoEvent(project, "Restarting global REPL to release memory")
                      StackReplsManager.getGlobalRepl(project).foreach(_.restart())
                    }
                  } finally {
                    getStackProjectManager(project).foreach(_.preloadingAllLibraryIdentifiers = false)
                  }
                }
              })

              if (!project.isDisposed) {
                val messageBus = project.getMessageBus
                val notifications = EditorNotifications.getInstance(project)
                messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, new ConfigFileWatcher(project, notifications))


                messageBus.connect().subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
                  override def rootsChanged(event: ModuleRootEvent): Unit = {
                    notifications.updateAllNotifications()
                  }
                })

                getStackProjectManager(project).map(_.projectLibraryFileWatcher).foreach { watcher =>
                  messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, watcher)
                }
              }

              HaskellPsiUtil.getPsiManager(project).foreach(_.addPsiTreeChangeListener(new PsiTreeChangeAdapter {

                private def invalidateInfo(event: PsiTreeChangeEvent): Unit = {
                  if (Option(event.getParent).flatMap(p => Option(p.getNode)).exists(_.getElementType != HaskellFileElementType.Instance) || Option(event.getNewChild).isDefined) {
                    Option(event.getFile).foreach(f => {
                      if (Option(event.getNewChild).orElse(Option(event.getParent)).flatMap(HaskellPsiUtil.findImportDeclarations).isDefined) {
                        // Have to refresh because import declarations can be changed
                        FileModuleIdentifiers.refresh(f)
                      }
                    })
                  }
                }

                override def childReplaced(event: PsiTreeChangeEvent): Unit = {
                  invalidateInfo(event)
                }

                override def childrenChanged(event: PsiTreeChangeEvent): Unit = {
                  invalidateInfo(event)
                }

                override def childRemoved(event: PsiTreeChangeEvent): Unit = {
                  invalidateInfo(event)
                }
              }))

              progressIndicator.setText("Busy preloading caches")
              if (!preloadStackComponentInfoCache.isDone || !preloadLibraryIdentifiersCacheFuture.isDone || !replsLoad.isDone) {
                FutureUtil.waitForValue(project, preloadStackComponentInfoCache, "preloading project cache", 600)
                FutureUtil.waitForValue(project, preloadLibraryIdentifiersCacheFuture, "preloading library identifiers caches", 600)
                FutureUtil.waitForValue(project, replsLoad, "starting and loading REPLs", 600)
              }
            } finally {
              getStackProjectManager(project).foreach(_.initializing = false)
            }

            // Force-load the module in REPL when REPL can be started. IntelliJ could have wanted to load file (via HaskellAnnotator)
            // but the REPL couldn't be started yet.
            HaskellAnnotator.getNotLoadedFiles(project) foreach { psiFile =>
              HaskellNotificationGroup.logInfoEvent(project, s"${psiFile.getName} will be force-loaded")
              HaskellAnnotator.restartDaemonCodeAnalyzerForFile(psiFile)
            }

            if (!HoogleComponent.doesHoogleDatabaseExist(project)) {
              HoogleComponent.showHoogleDatabaseDoesNotExistNotification(project)
            }

            StackReplsManager.getReplsManager(project).foreach(_.modulePackageInfos.foreach { case (cabalInfo) =>
              val intersection = cabalInfo.sourceRoots.toSeq.intersect(cabalInfo.testSourceRoots.toSeq)
              if (intersection.nonEmpty) {
                intersection.foreach(p => {
                  HaskellNotificationGroup.logWarningBalloonEvent(project, s"Source folder `$p` is defined both as Source and Test Source")
                })
              }
            })
            HaskellNotificationGroup.logInfoEvent(project, "Finished initializing Haskell project")
          }
        )
      }
    }
  }

  private def findWorkingDirectory(project: Project): Option[Path] = {
    val stackFiles = HaskellProjectManager.getInstance(project).findStackFiles().asScala
    val workingDirectories = stackFiles
      .flatMap(it => Option(it.getParent))
      .distinct
      .toIndexedSeq
    workingDirectories.size match {
      case 0 =>
        StackProjectManager.logger.warn("Stack working directory cannot be determined for project " + project.getName)
        None
      case 1 => Some(workingDirectories.head)
      case _ =>
        StackProjectManager.logger.warn(
          s"Multiple stack working directories found for project ${project.getName}:" +
            s" ${workingDirectories.mkString(", ")}"
        )
        Some(workingDirectories.head)
    }
  }

  private val logger = Logger.getInstance(this.getClass)
}

class StackProjectManager(project: Project) extends ProjectComponent {

  override def getComponentName: String = "stack-project-manager"

  @volatile
  private var initializing = false

  @volatile
  private var hoogleAvailable: Option[String] = None

  @volatile
  private var hlintAvailable: Option[String] = None

  @volatile
  private var ormoluAvailable: Option[String] = None

  @volatile
  private var installingHaskellTools = false

  @volatile
  private var preloadingAllLibraryIdentifiers = false

  @volatile
  private var haddockBuilding = false

  @volatile
  private var replsManager: Option[StackReplsManager] = None

  private val projectLibraryFileWatcher = new ProjectLibraryFileWatcher(project)

  private val lifetimeDefinition = new LifetimeDefinition()
  private def lifetime = lifetimeDefinition.getLifetime

  def getStackReplsManager: Option[StackReplsManager] = {
    replsManager
  }

  private def initStackReplsManager(workingDirectory: Path): Unit = {
    replsManager = Option(new StackReplsManager(project, workingDirectory))
  }

  override def projectClosed(): Unit = {
    if (initialized.get()) {
      replsManager.foreach(_.getGlobalRepl.exit())
      replsManager.foreach(_.getGlobalRepl2.exit())
      replsManager.foreach(_.getRunningProjectRepls.foreach(_.exit()))
      HaskellComponentsManager.invalidateGlobalCaches(project)
    }
  }

  override def initComponent(): Unit = {}

  private val initialized = new AtomicBoolean()

  override def projectOpened(): Unit = {
    HaskellProjectManager.getInstance(project).isHaskellProject.advise(lifetime, isHaskellProject => {
      if (isHaskellProject && initialized.compareAndSet(false, true)) {
        ApplicationManager.getApplication.invokeLater(() => {
          initializeOnce()
        })
      }

      kotlin.Unit.INSTANCE
    })
  }

  private def initializeOnce(): Unit = {
    disableDefaultReformatAction()

    StackProjectManager.findWorkingDirectory(project) match {
      case Some(workingDirectory) =>
        initStackReplsManager(workingDirectory)
        if (replsManager.exists(_.componentTargets.isEmpty)) {
          Messages.showErrorDialog(
            project,
            s"Can't start project as no Cabal file was found (or could not be read)",
            "Can't start project"
          )
        } else {
          StackProjectManager.start(project, workingDirectory)
        }
      case None =>
    }
  }

  override def disposeComponent(): Unit = {
    lifetimeDefinition.terminate(true)
  }

  private def disableDefaultReformatAction(): Unit = {
    val actionManager = ActionManager.getInstance
    // Overriding IntelliJ's default shortcut for formatting
    actionManager.unregisterAction("ReformatCode")
    actionManager.registerAction("ReformatCode", new HaskellReformatAction)
  }
}
