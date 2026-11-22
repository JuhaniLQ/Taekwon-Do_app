package fi.tkd.itfun.quiz

import android.content.Context
import fi.tkd.itfun.data.db.DbProvider
import fi.tkd.itfun.data.db.ItemEntity
import fi.tkd.itfun.data.db.BodyPartEntity
import fi.tkd.itfun.techniques.TKDItemCategory
import fi.tkd.itfun.compendium.parseCompendiumTerms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal data class QuizContent(
    val settings: QuizSettings,
    val pool: List<ItemEntity>,
    val bodyParts: List<BodyPartEntity>,
    val stances: List<ItemEntity>,
    val stanceImages: Map<String, StanceQuizImages>,
    val terminology: List<VocabularyTerm>,
    val initialSession: QuizSession
)

internal suspend fun prepareQuiz(context: Context, settings: QuizSettings): QuizContent? =
    withContext(Dispatchers.IO) {
        val pool = if (settings.techniquesEnabled) loadTechniqueQuizPool(context, settings.selectedBeltGups) else emptyList()
        val bodyParts = if (settings.bodyPartsEnabled) DbProvider.get(context).bodyPartDao().getAllNow() else emptyList()
        val terminology = if (settings.theoryEnabled) {
            DbProvider.get(context).compendiumEntryDao().getAllNow()
                .filter { it.sectionKey == "terminology" && it.termsJson != null }
                .flatMap { entry ->
                    parseCompendiumTerms(entry.termsJson!!).map { (english, korean) ->
                        VocabularyTerm("${entry.entryKey}:$english", english, korean, VocabularySource.TERMINOLOGY)
                    }
                }
        } else emptyList()
        val stances = if (settings.theoryEnabled) loadStanceQuizPool(context, settings.selectedBeltGups) else emptyList()
        val stanceKeys = stances.map { it.videoKey }.toSet()
        val stanceImages = if (stanceKeys.isEmpty()) emptyMap() else {
            DbProvider.get(context).compendiumEntryImageDao().getAllNow()
                .filter { it.entryKey in stanceKeys }
                .groupBy { it.entryKey }
                .mapValues { (_, images) ->
                    fun imagePath(order: Int) = images.firstOrNull { it.sortOrder == order }?.localPath?.takeIf {
                        val file = File(it)
                        file.isFile && file.length() > 0
                    }
                    StanceQuizImages(imagePath(0), imagePath(1))
                }
        }
        val session = QuizSession.start(pool, settings, bodyParts = bodyParts, stances = stances,
            stanceImages = stanceImages, terminology = terminology)
        if (session.question == null) null else QuizContent(settings, pool, bodyParts, stances, stanceImages, terminology, session)
    }

internal suspend fun loadTechniqueQuizPool(context: Context, selectedBeltGups: Set<Int>): List<ItemEntity> =
    withContext(Dispatchers.IO) {
        val db = DbProvider.get(context)
        val categories = setOf(TKDItemCategory.DEFENSIVE.name, TKDItemCategory.OFFENSIVE.name, TKDItemCategory.KICKS.name)
        val categoryIds = db.categoryDao().getAllNow().filter { it.name in categories }.map { it.id }.toSet()
        db.itemDao().getAllNow().filter { item ->
            item.categoryId in categoryIds && item.beltLevel in selectedBeltGups &&
                item.localVideoPath?.let {
                    val file = File(it)
                    file.isFile && file.length() > 0
                } == true
        }.distinctBy { it.videoKey }
    }

internal suspend fun loadStanceQuizPool(context: Context, selectedBeltGups: Set<Int>): List<ItemEntity> =
    withContext(Dispatchers.IO) {
        val db = DbProvider.get(context)
        val stanceCategory = db.categoryDao().getAllNow().firstOrNull { it.name == TKDItemCategory.STANCES.name }
            ?: return@withContext emptyList()
        db.itemDao().getAllNow().filter {
            it.categoryId == stanceCategory.id && it.beltLevel in selectedBeltGups
        }
    }
