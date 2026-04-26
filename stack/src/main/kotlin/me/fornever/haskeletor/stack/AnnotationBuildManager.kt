/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.stack

import com.intellij.build.BuildProgressListener
import com.intellij.build.BuildViewManager
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.events.FinishBuildEvent
import com.intellij.build.events.StartBuildEvent
import com.intellij.build.events.impl.SuccessResultImpl
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.ThreeState
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages synthetic "build" events for file re-annotation.
 * Each file re-annotation is treated as a mini-build, allowing IntelliJ's build view
 * to properly clear old problems and show new ones.
 */
@Service(Service.Level.PROJECT)
internal class AnnotationBuildManager(private val project: Project) {

    companion object {
        @JvmStatic
        fun getInstance(project: Project): AnnotationBuildManager = project.service()
    }

    private val buildIdStorage = AtomicInteger()
    private val buildViewManager = project.service<BuildViewManager>()

    /**
     * Triggers a mini-build event for file re-annotation.
     * This informs the build view that a "build" has completed, which clears old problems
     * for this pseudo-build session and allows new problems to be injected.
     *
     * @param fileName The name of the file being re-annotated (for display purposes)
     */
    fun startAnnotationBuild(fileName: String): AnnotationBuildSession {
        val buildId = buildIdStorage.getAndIncrement()
        val buildDescriptor = DefaultBuildDescriptor(
            buildId,
            "Annotation: $fileName",
            project.basePath ?: "",
            System.currentTimeMillis()
        )
        buildDescriptor.isNavigateToError = ThreeState.NO

        buildViewManager.onEvent(
            buildId,
            StartBuildEvent.builder(
                "Analyzing $fileName",
                buildDescriptor
            ).build()
        )

        return AnnotationBuildSessionImpl(buildId, buildViewManager)
    }
}

/**
 * Represents an active annotation build session.
 * Call [finish] when annotation is complete to trigger problem cleanup.
 */
interface AnnotationBuildSession {
    fun getBuildId(): Int
    fun finish()
}

/**
 * Implementation of AnnotationBuildSession.
 */
internal class AnnotationBuildSessionImpl(
    private val buildId: Int,
    private val buildProgressListener: BuildProgressListener
) : AnnotationBuildSession {
    override fun getBuildId(): Int = buildId

    /**
     * Finishes the annotation build session.
     * This tells the build view to clear old problems and prepare for new ones.
     */
    override fun finish() {
        buildProgressListener.onEvent(
            buildId,
            FinishBuildEvent.builder(
                buildId,
                "Annotation completed",
                SuccessResultImpl()
            ).build()
        )
    }
}

