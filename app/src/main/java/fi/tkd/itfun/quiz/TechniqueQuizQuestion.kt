package fi.tkd.itfun.quiz

import fi.tkd.itfun.data.db.ItemEntity
import java.util.Locale
import kotlin.random.Random

internal enum class TechniqueQuestionFormat { NAME_TO_VIDEO, VIDEO_TO_NAME }

internal data class TechniqueQuestionKey(val format: TechniqueQuestionFormat, val prompt: String)

private fun ItemEntity.promptKey(format: TechniqueQuestionFormat) = TechniqueQuestionKey(
    format,
    if (format == TechniqueQuestionFormat.NAME_TO_VIDEO) koreanName.trim().lowercase(Locale.ROOT) else videoKey
)

internal data class TechniqueQuizQuestion(
    val videos: List<ItemEntity>,
    override val correctIndex: Int,
    val format: TechniqueQuestionFormat = TechniqueQuestionFormat.NAME_TO_VIDEO,
    override val studySeconds: Int = QuizTiming.NORMAL_STUDY_SECONDS
) : QuizQuestion {
    val correctTechnique: ItemEntity get() = videos[correctIndex]
    val key: TechniqueQuestionKey get() = correctTechnique.promptKey(format)
}

internal fun createTechniqueQuestion(
    pool: List<ItemEntity>,
    previousVideoKey: String? = null,
    random: Random = Random.Default,
    format: TechniqueQuestionFormat = TechniqueQuestionFormat.NAME_TO_VIDEO,
    choiceCount: Int = 3,
    excludedPrompts: Set<TechniqueQuestionKey> = emptySet(),
    studySeconds: Int = QuizTiming.NORMAL_STUDY_SECONDS
): TechniqueQuizQuestion? {
    val (available, candidates) = techniqueQuestionPool(pool, format, choiceCount, excludedPrompts)
    if (candidates.isEmpty()) return null
    fun ItemEntity.normalizedName() = koreanName.trim().lowercase(Locale.ROOT)

    val correct = candidates.filter { it.videoKey != previousVideoKey }
        .ifEmpty { candidates }.random(random)
    val alternatives = available.filter {
        it.videoKey != correct.videoKey && it.normalizedName() != correct.normalizedName()
    }
    val sameCategory = alternatives.filter { it.categoryId == correct.categoryId }.shuffled(random)
    val otherCategories = alternatives.filter { it.categoryId != correct.categoryId }.shuffled(random)
    val distractors = (sameCategory + otherCategories)
        .distinctBy { it.normalizedName() }.take(choiceCount - 1)
    val videos = (distractors + correct).shuffled(random)
    return TechniqueQuizQuestion(videos, videos.indexOf(correct), format, studySeconds)
}

internal fun techniqueQuestionPool(
    pool: List<ItemEntity>,
    format: TechniqueQuestionFormat,
    choiceCount: Int,
    excludedPrompts: Set<TechniqueQuestionKey>
): QuizQuestionPool<ItemEntity> {
    val available = pool.filter { it.koreanName.isNotBlank() }
        .distinctBy { it.videoKey }
    fun ItemEntity.normalizedName() = koreanName.trim().lowercase(Locale.ROOT)
    val candidates = if (available.distinctBy { it.normalizedName() }.size < choiceCount) emptyList() else
        available.filter { it.promptKey(format) !in excludedPrompts }.distinctBy { it.promptKey(format) }
    return QuizQuestionPool(available, candidates)
}
