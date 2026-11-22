package fi.tkd.itfun.quiz

import fi.tkd.itfun.data.db.ItemEntity
import java.util.Locale
import kotlin.random.Random

internal enum class StanceQuestionFormat { FOOTPRINT, STANCE_IMAGE }

internal data class StanceQuizImages(val footprintPath: String?, val stancePath: String?) {
    fun path(format: StanceQuestionFormat) =
        if (format == StanceQuestionFormat.FOOTPRINT) footprintPath else stancePath
}

internal data class StanceQuestionKey(val format: StanceQuestionFormat, val stanceKey: String)

internal data class StanceQuizQuestion(
    val choices: List<ItemEntity>,
    override val correctIndex: Int,
    override val studySeconds: Int,
    val format: StanceQuestionFormat,
    val imagePath: String
) : QuizQuestion {
    val correctStance get() = choices[correctIndex]
    val promptKey get() = StanceQuestionKey(format, correctStance.videoKey)
    val promptText get() = if (format == StanceQuestionFormat.FOOTPRINT)
        "Which stance has the following footprint?" else "What is this stance?"
    val answerNames get() = choices.map { it.koreanName }
    val answerExplanation get() = "${correctStance.koreanName} = ${correctStance.name}"
}

internal fun createStanceQuestion(
    pool: List<ItemEntity>,
    settings: QuizSettings,
    excludedPrompts: Set<StanceQuestionKey> = emptySet(),
    previousPrompt: StanceQuestionKey? = null,
    random: Random = Random.Default,
    format: StanceQuestionFormat = StanceQuestionFormat.FOOTPRINT,
    images: Map<String, StanceQuizImages> = emptyMap()
): StanceQuizQuestion? {
    val (available, candidates) = stanceQuestionPool(pool, settings, excludedPrompts, format, images)
    if (candidates.isEmpty()) return null
    val choiceCount = minOf(settings.textAnswerChoiceCount, available.size)
    val correct = candidates.filter { StanceQuestionKey(format, it.videoKey) != previousPrompt }
        .ifEmpty { candidates }.random(random)
    val choices = (available.filter { it.techniqueCode != correct.techniqueCode }.shuffled(random)
        .take(choiceCount - 1) + correct).shuffled(random)
    return StanceQuizQuestion(choices, choices.indexOf(correct), settings.studySeconds, format,
        images.getValue(correct.videoKey).path(format)!!)
}

internal fun stanceQuestionPool(
    pool: List<ItemEntity>,
    settings: QuizSettings,
    excludedPrompts: Set<StanceQuestionKey>,
    format: StanceQuestionFormat,
    images: Map<String, StanceQuizImages>
): QuizQuestionPool<ItemEntity> {
    val available = pool.filter {
        it.beltLevel in settings.selectedBeltGups && it.name.isNotBlank() && it.koreanName.isNotBlank()
    }.distinctBy { it.name.trim().lowercase(Locale.ROOT) }
        .distinctBy { it.koreanName.trim().lowercase(Locale.ROOT) }
    val candidates = available.filter {
        StanceQuestionKey(format, it.videoKey) !in excludedPrompts &&
            !images[it.videoKey]?.path(format).isNullOrBlank()
    }
    return QuizQuestionPool(available, candidates)
}
