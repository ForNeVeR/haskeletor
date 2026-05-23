/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.stack

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.NlsSafe
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Service(Service.Level.APP)
internal class StackCommandRunner {

    companion object {
        fun getInstance(): StackCommandRunner = service()
    }

    private val mutex = Mutex()
    suspend fun <T> executeInQueue(diagnosticName: @NlsSafe String, action: suspend () -> T): T {
        logger.info("Queueing a executed Stack command: $diagnosticName.")
        mutex.withLock {
            logger.info("Starting the queued Stack command: $diagnosticName.")
            try {
                return action()
            } finally {
                logger.info("Finished the queued Stack command: $diagnosticName.")
            }
        }
    }
}

private val logger = logger<StackCommandRunner>()
