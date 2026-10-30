package fi.tkd.itfun

import android.content.Context
import androidx.datastore.preferences.core.edit
import fi.tkd.itfun.PrefKeys.CONTENT_READY
import fi.tkd.itfun.data.db.DbProvider
import fi.tkd.itfun.data.db.downloadAllCompendiumImages
import fi.tkd.itfun.data.db.downloadAllVideos
import kotlinx.coroutines.flow.first

class ContentRepository(context: Context) {
    private val appContext = context.applicationContext

    suspend fun isContentReady(): Boolean =
        appContext.dataStore.data.first()[CONTENT_READY] ?: false

    suspend fun prepareContent(
        contentAlreadyReady: Boolean,
        onProgress: (Float) -> Unit
    ) {
        DbProvider.ensureSeeded(appContext)

        if (!contentAlreadyReady) {
            downloadContent(onProgress)
            appContext.dataStore.edit { preferences ->
                preferences[CONTENT_READY] = true
            }
        }
    }

    suspend fun checkForUpdates(): DbProvider.UpdateResult =
        DbProvider.updateSeeding(appContext)

    suspend fun downloadContent(onProgress: (Float) -> Unit) {
        downloadAllVideos(appContext) { progress ->
            onProgress(progress * VIDEO_PROGRESS_WEIGHT)
        }
        downloadAllCompendiumImages(appContext) { progress ->
            onProgress(VIDEO_PROGRESS_WEIGHT + progress * (1f - VIDEO_PROGRESS_WEIGHT))
        }
    }

    private companion object {
        const val VIDEO_PROGRESS_WEIGHT = 0.9f
    }
}
