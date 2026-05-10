/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.projectmodel

import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListenerBackgroundable
import com.intellij.openapi.vfs.newvfs.events.*
import com.jetbrains.rd.util.reactive.IOptPropertyView
import com.jetbrains.rd.util.reactive.OptProperty
import com.jetbrains.rd.util.reactive.hasValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import me.fornever.haskeletor.core.intellij.ProjectScope
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.pathString

@Service(Service.Level.PROJECT)
class HaskellProjectManager(private val project: Project) {

    companion object {
        @JvmStatic
        fun getInstance(project: Project): HaskellProjectManager = project.service()
    }

    private val _configFiles = mutableSetOf<Path>()
    private val _configFilesLock = Any()

    private val _isHaskellProject = OptProperty<Boolean>()

    fun findCabalFiles(): List<Path> = synchronized(_configFilesLock) {
        _configFiles.filter { it.extension.equals("cabal", ignoreCase = true) }
    }

    /**
     * A Haskell project is a project that contains at least one `.cabal` or `stack.yaml` file under its content roots.
     */
    val isHaskellProject: IOptPropertyView<Boolean> = _isHaskellProject

    init {
        project.messageBus.connect(ProjectScope.get(project)).apply {
            subscribe(
                @Suppress("UnstableApiUsage")
                VirtualFileManager.VFS_CHANGES_BG,
                createVfsListener()
            )
            subscribe(ModuleRootListener.TOPIC, object : ModuleRootListener {
                override fun rootsChanged(event: ModuleRootEvent) {
                    progressModuleRootChange()
                }
            })
        }
        launchInitialCheck()
    }

    private fun launchInitialCheck() {
        ProjectScope.get(project).launch(Dispatchers.IO) {
            readAction {
                val roots = ProjectRootManager.getInstance(project).contentRootsFromAllModules
                logger.info("Started scanning ${roots.size} project content roots.")
                val time = System.currentTimeMillis()
                try {
                    for (root in roots) {
                        coroutineContext.ensureActive()
                        findConfigFiles(root).forEach { stackFile ->
                            processConfigFile(stackFile, exists = true)
                        }
                    }
                } catch (e: CancellationException) {
                    logger.info("Full scan of VFS cancelled. Wasted ${System.currentTimeMillis() - time} ms.")
                    throw e
                } finally {
                    logger.info("Finished scanning ${roots.size} project content roots in ${System.currentTimeMillis() - time} ms.")
                }
            }
        }
    }

    private fun findConfigFiles(directory: VirtualFile): List<VirtualFile> {
        val result = mutableListOf<VirtualFile>()
        VfsUtil.processFileRecursivelyWithoutIgnored(directory) {
            if (isConfigFile(it.toNioPath())) {
                result.add(it)
            }

            true
        }

        return result
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
        // We could do better than just compare with ignoreCase, but it's the simplest cross-platform solution
        // available so far.
        return path.extension.equals("cabal", ignoreCase = true)
            || path.name.equals("stack.yaml", ignoreCase = true)
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

    private fun progressModuleRootChange() {
        // TODO: In theory, it's possible that a new module root is added without VFS modification — should be handled somehow.
        val fileProjectIndex = ProjectFileIndex.getInstance(project)

        synchronized(_configFilesLock) {
            for (path in _configFiles) {
                val file = VirtualFileManager.getInstance().findFileByNioPath(path) ?: run {
                    logger.error("Cannot find file \"${path.pathString}\" in VFS.")
                    continue
                }

                if (!fileProjectIndex.isInContent(file)) {
                    _configFiles.remove(path)
                    logger.info("Unregistered a config file due to module root change: \"${path.pathString}\".")
                }
            }

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
