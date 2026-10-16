package com.gamevault.app.data.catalog

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiKeyOnboardingStoreTest {
    @Test fun skipOrSaveCompletesOnThisInstallation() = runBlocking {
        val dir = Files.createTempDirectory("gv-onboarding")
        try {
            val file = dir.resolve("local-marker").toFile()
            val store = ApiKeyOnboardingStore(file)
            assertFalse(store.hasCompleted())
            store.markCompleted()
            assertTrue(store.hasCompleted())
            assertTrue(ApiKeyOnboardingStore(file).hasCompleted()) // app relaunch
            // Reinstall gives a fresh no-backup directory, so it shows again.
            assertFalse(ApiKeyOnboardingStore(dir.resolve("fresh-install-marker").toFile()).hasCompleted())
        } finally {
            dir.toFile().deleteRecursively()
        }
    }
}
