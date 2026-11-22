package fi.tkd.itfun.quiz

import fi.tkd.itfun.data.db.BodyPartEntity
import java.util.Locale
import kotlin.random.Random

internal enum class VocabularySource { BODY_PART, TERMINOLOGY }

internal data class VocabularyTerm(val code: String, val nameEn: String, val nameKo: String, val source: VocabularySource)

internal fun BodyPartEntity.asVocabularyTerm() = VocabularyTerm(code, nameEn, nameKo, VocabularySource.BODY_PART)

internal data class VocabularyQuestionKey(val format: TranslationQuestionFormat, val prompt: String)

private fun VocabularyTerm.promptText(format: TranslationQuestionFormat) =
    if (format == TranslationQuestionFormat.KOREAN_TO_ENGLISH) nameKo else nameEn

private fun VocabularyTerm.promptKey(format: TranslationQuestionFormat) =
    VocabularyQuestionKey(format, promptText(format).trim().lowercase(Locale.ROOT))

internal data class VocabularyQuizQuestion(
    val choices: List<VocabularyTerm>,
    override val correctIndex: Int,
    override val studySeconds: Int,
    override val format: TranslationQuestionFormat = TranslationQuestionFormat.ENGLISH_TO_KOREAN
) : TranslationQuizQuestion {
    val correctTerm get() = choices[correctIndex]
    override val promptText get() = correctTerm.promptText(format)
    val promptKey get() = correctTerm.promptKey(format)
    override val answerNames get() = choices.map {
        if (format == TranslationQuestionFormat.KOREAN_TO_ENGLISH) it.nameEn else it.nameKo
    }
    override val answerExplanation get() = "${correctTerm.nameKo} = ${correctTerm.nameEn}"
}

internal fun createVocabularyQuestion(
    pool: List<VocabularyTerm>,
    settings: QuizSettings,
    excludedPrompts: Set<VocabularyQuestionKey> = emptySet(),
    previousPrompt: VocabularyQuestionKey? = null,
    random: Random = Random.Default,
    format: TranslationQuestionFormat = TranslationQuestionFormat.ENGLISH_TO_KOREAN
): VocabularyQuizQuestion? {
    val (available, candidates) = vocabularyQuestionPool(pool, settings, excludedPrompts, format)
    if (candidates.isEmpty()) return null
    val correct = candidates.filter { it.promptKey(format) != previousPrompt }
        .ifEmpty { candidates }.random(random)
    val choices = (available.filter { it.code != correct.code }.shuffled(random)
        .take(settings.vocabularyAnswerChoiceCount - 1) + correct).shuffled(random)
    return VocabularyQuizQuestion(choices, choices.indexOf(correct), settings.studySeconds, format)
}

internal fun vocabularyQuestionPool(
    pool: List<VocabularyTerm>,
    settings: QuizSettings,
    excludedPrompts: Set<VocabularyQuestionKey>,
    format: TranslationQuestionFormat
): QuizQuestionPool<VocabularyTerm> {
    val available = pool.filter { settings.vocabularySourceEnabled(it.source) && it.nameEn.isNotBlank() && it.nameKo.isNotBlank() }
        .distinctBy { it.nameEn.trim().lowercase(Locale.ROOT) }
        .distinctBy { it.nameKo.trim().lowercase(Locale.ROOT) }
    val candidates = if (available.size < settings.vocabularyAnswerChoiceCount) emptyList() else
        available.filter { format in settings.vocabularyFormats(it.source) && it.promptKey(format) !in excludedPrompts }
    return QuizQuestionPool(available, candidates)
}
