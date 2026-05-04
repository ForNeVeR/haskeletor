/*
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.stack

import com.intellij.testFramework.assertInstanceOf
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import me.fornever.haskeletor.settings.HaskellSettingsPersistentStateComponent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.io.path.pathString

@TestApplication
class StackLocatorCacheTest {

    val project = projectFixture()

    @Test
    fun `Cache state starts as NotCached`() {
        val service = StackLocator.getInstance(project.get())
        assertInstanceOf<StackLocator.CacheState.NotCached>(service.cacheState)
    }

    @Test
    suspend fun `First read value gets cached`() {
        val path1 = Path("stack1/stack")
        val path2 = Path("stack2/stack")

        var pathFromEnv = path1
        val accessor = object : EnvironmentAccessor {
            override fun findExecutableInPath(baseName: String) = pathFromEnv
        }
        val service = StackLocator(project.get(), accessor)

        service.locateStack() // to trigger the cache
        assertEquals(StackLocator.CacheState.Cached(null, path1), service.cacheState)
        pathFromEnv = path2
        service.locateStack() // to trigger the cache
        assertEquals(StackLocator.CacheState.Cached(null, path1), service.cacheState)
    }

    @Test
    suspend fun `Changing the settings resets the value`() {
        val path = Path("stack/stack")
        val accessor = object : EnvironmentAccessor {
            override fun findExecutableInPath(baseName: String) = path
        }
        val service = StackLocator(project.get(), accessor)

        service.locateStack() // to trigger the cache
        assertEquals(StackLocator.CacheState.Cached(null, path), service.cacheState)

        val path2 = Path("newStack/stack")
        val settings = HaskellSettingsPersistentStateComponent.getInstance().state
        settings.stackPath = path2.pathString

        service.locateStack() // to trigger the cache
        assertEquals(StackLocator.CacheState.Cached(path2.pathString, path2), service.cacheState)
    }
}
