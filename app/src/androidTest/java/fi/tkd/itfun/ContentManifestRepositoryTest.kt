package fi.tkd.itfun

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ContentManifestRepositoryTest {
    private lateinit var directory: File
    private var time = 1_000_000L
    private var requests = 0
    private var remote: JSONObject? = null

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        directory = Files.createTempDirectory(context.cacheDir.toPath(), "manifest-test").toFile()
        remote = manifest(2)
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    private fun repository(source: String = "test-manifest") = ContentManifestRepository(
        File(directory, "preview.json"), source, now = { time },
        fetch = { requests++; remote }
    )

    @Test
    fun cachedCheckSurvivesRepositoryRecreationAndExpiresAfter24Hours() = runBlocking {
        assertEquals(2, repository().check().manifest!!.getInt("version"))
        remote = manifest(3)
        time += 23 * 60 * 60 * 1000L
        assertEquals(2, repository().check().manifest!!.getInt("version"))
        assertEquals(1, requests)
        time += 60 * 60 * 1000L
        assertEquals(3, repository().check().manifest!!.getInt("version"))
        assertEquals(2, requests)
    }

    @Test
    fun explicitUpdateAlwaysFetchesAndRefreshesTheSavedPreview() = runBlocking {
        repository().check()
        remote = manifest(3)
        assertEquals(3, repository().check(force = true).manifest!!.getInt("version"))
        assertEquals(3, repository().cached().manifest!!.getInt("version"))
        repository().check()
        assertEquals(2, requests)
    }

    @Test
    fun offlineCheckKeepsSavedContentAndThrottlesRetriesButUpdateCanRetry() = runBlocking {
        repository().check()
        time += 24 * 60 * 60 * 1000L
        remote = null
        val offline = repository().check()
        assertTrue(offline.failed)
        assertEquals(2, offline.manifest!!.getInt("version"))
        repository().check()
        assertEquals(2, requests)
        remote = manifest(3)
        assertFalse(repository().check(force = true).failed)
        assertEquals(3, requests)
    }

    @Test
    fun changingTheManifestSourceDoesNotReuseTheOldPreview() = runBlocking {
        repository().check()
        remote = manifest(3)
        assertEquals(3, repository("other-source").check().manifest!!.getInt("version"))
        assertEquals(2, requests)
    }

    @Test
    fun failedFirstCheckIsThrottledAndDoesNotInventAvailableContent() = runBlocking {
        remote = null
        assertEquals(ManifestCheck(null, failed = true), repository().check())
        assertTrue(repository().check().failed)
        assertEquals(1, requests)
    }

    private fun manifest(version: Int) = JSONObject(
        """{"version":$version,"techniques":[],"bodyParts":[],"terms":{},"patternvideodetails":{}}"""
    )
}
