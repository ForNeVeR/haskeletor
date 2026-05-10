/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.projectmodel

import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListenerBackgroundable
import com.intellij.openapi.vfs.newvfs.events.*
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.ProjectScope
import com.jetbrains.rd.util.reactive.IOptPropertyView
import com.jetbrains.rd.util.reactive.OptProperty
import com.jetbrains.rd.util.reactive.hasValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.pathString
import me.fornever.haskeletor.core.intellij.ProjectScope as ProjectCoroutineScope

@Service(Service.Level.PROJECT)
class HaskellProjectManager(private val project: Project) {

    companion object {
        @JvmStatic
        fun getInstance(project: Project): HaskellProjectManager = project.service()
    }

    private val _configFiles = mutableSetOf<Path>()
    private val _configFilesLock = Any()
    private var _configFilesRefreshGeneration = AtomicInteger()

    private val _isHaskellProject = OptProperty<Boolean>()

    // We could do better than just compare with ignoreCase, but it's the simplest cross-platform solution
    // available so far.
    private fun isStackFile(path: Path) = path.name.equals("stack.yaml", ignoreCase = true)
    private fun isCabalFile(path: Path) = path.extension.equals("cabal", ignoreCase = true)

    fun findStackFiles(): List<Path> = synchronized(_configFilesLock) {
        _configFiles.filter(::isStackFile)
    }

    fun findCabalFiles(): List<Path> = synchronized(_configFilesLock) {
        _configFiles.filter(::isCabalFile)
    }

    /**
     * A Haskell project is a project that contains at least one `.cabal` or `stack.yaml` file under its content roots.
     */
    val isHaskellProject: IOptPropertyView<Boolean> = _isHaskellProject

    init {
        project.messageBus.connect(ProjectCoroutineScope.get(project)).apply {
            subscribe(
                @Suppress("UnstableApiUsage")
                VirtualFileManager.VFS_CHANGES_BG,
                createVfsListener()
            )
            subscribe(ModuleRootListener.TOPIC, object : ModuleRootListener {
                override fun rootsChanged(event: ModuleRootEvent) {
                    launchInitialCheck()
                }
            })
        }
        launchInitialCheck()
    }

    private fun launchInitialCheck() {
        val generation = _configFilesRefreshGeneration.incrementAndGet()

        ProjectCoroutineScope.get(project).launch(Dispatchers.IO) {
            val time = System.currentTimeMillis()
            val configFiles = smartReadAction(project) { collectConfigFilesFromIndex() }

            if (generation != _configFilesRefreshGeneration.get()) {
                return@launch
            }

            logger.info(
                "Loaded ${configFiles.size} Haskell config files from index in ${System.currentTimeMillis() - time} ms."
            )
            replaceConfigFiles(configFiles)
        }
    }

    private fun collectConfigFilesFromIndex(): Set<Path> {
        val contentScope = ProjectScope.getContentScope(project)
        val cabalFiles = FilenameIndex.getAllFilesByExt(project, "cabal", contentScope)
        val stackFiles = FilenameIndex.getVirtualFilesByName("stack.yaml", false, contentScope)

        return (cabalFiles.asSequence() + stackFiles.asSequence())
            .map { it.toNioPath() }
            .filter(::isConfigFile)
            .toCollection(mutableSetOf())
    }

    @Suppress("UnstableApiUsage")
    private fun createVfsListener() = object : BulkFileListenerBackgroundable {
        override fun after(events: List<VFileEvent>) {
            for (event in events) {
                when (event) {
                    is VFileDeleteEvent -> processConfigFile(event.file, exists = false)
                    is VFileCreateEvent ->
                        event.file?.let { processConfigFile(it, exists = true) }
                            ?: logger.warn("VFileCreateEvent without file: \"${event.path}\".")
                    is VFileMoveEvent -> {
                        modifyConfigFileMap(Path(event.oldPath), shouldExist = false)
                        processConfigFile(event.file, exists = true)
                    }
                    is VFileCopyEvent -> processConfigFile(event.file, exists = true)
                    is VFileContentChangeEvent, is VFilePropertyChangeEvent -> {}
                    else -> {
                        logger.error("Unknown VFS event type: ${event::class.simpleName}.")
                    }
                }
            }
        }
    }

    private fun isConfigFile(path: Path): Boolean {
        return isStackFile(path) || isCabalFile(path)
    }

    private fun processConfigFile(file: VirtualFile, exists: Boolean) {
        if (!isConfigFile(file.toNioPath())) return

        val projectFileIndex = ProjectFileIndex.getInstance(project)
        val shouldExist = exists && projectFileIndex.isInContent(file)
        logger.trace {
            "Processing file \"${file.path}\"." +
                " Exists: $exists, isInContent: ${projectFileIndex.isInContent(file)}."
        }

        modifyConfigFileMap(file.toNioPath(), shouldExist)
    }

    private fun replaceConfigFiles(paths: Set<Path>) {
        synchronized(_configFilesLock) {
            _configFiles.clear()
            _configFiles.addAll(paths)
            _isHaskellProject.set(_configFiles.isNotEmpty())
        }
    }

    private fun modifyConfigFileMap(path: Path, shouldExist: Boolean) {
        if (!isConfigFile(path)) return

        synchronized(_configFilesLock) {
            val changed = if (shouldExist) {
                _configFiles.add(path).also { added ->
                    if (added) logger.info("Registered a new config file: \"${path.pathString}\".")
                }
            } else {
                _configFiles.remove(path).also { removed ->
                    if (removed) logger.info("Unregistered a config file: \"${path.pathString}\".")
                }
            }

            if (!_isHaskellProject.hasValue || changed) {
                _isHaskellProject.set(_configFiles.isNotEmpty())
            }
        }
    }
}

private val logger = logger<HaskellProjectManager>()
