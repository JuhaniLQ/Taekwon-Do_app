package fi.tkd.itfun.quiz

import fi.tkd.itfun.data.db.ItemEntity
import fi.tkd.itfun.data.db.BodyPartEntity
import kotlin.random.Random

internal val QuizSettings.enabledTechniqueFormats: List<TechniqueQuestionFormat>
    get() = if (!techniquesEnabled) emptyList() else buildList {
        if (techniqueNameToVideoEnabled) add(TechniqueQuestionFormat.NAME_TO_VIDEO)
        if (techniqueVideoToNameEnabled) add(TechniqueQuestionFormat.VIDEO_TO_NAME)
    }

internal fun QuizSettings.vocabularySourceEnabled(source: VocabularySource) =
    if (source == VocabularySource.BODY_PART) bodyPartsEnabled else theoryEnabled

internal fun QuizSettings.vocabularyFormats(source: VocabularySource): List<TranslationQuestionFormat> =
    if (!vocabularySourceEnabled(source)) emptyList() else buildList {
        val toKorean = if (source == VocabularySource.BODY_PART) bodyPartEnglishToKoreanEnabled else terminologyEnglishToKoreanEnabled
        val toEnglish = if (source == VocabularySource.BODY_PART) bodyPartKoreanToEnglishEnabled else terminologyKoreanToEnglishEnabled
        if (toKorean) add(TranslationQuestionFormat.ENGLISH_TO_KOREAN)
        if (toEnglish) add(TranslationQuestionFormat.KOREAN_TO_ENGLISH)
    }

internal val QuizSettings.enabledVocabularyFormats: List<TranslationQuestionFormat>
    get() = VocabularySource.entries.flatMap { vocabularyFormats(it) }.distinct()

internal val QuizSettings.enabledStanceFormats: List<StanceQuestionFormat>
    get() = if (!theoryEnabled) emptyList() else buildList {
        if (stanceFootprintEnabled) add(StanceQuestionFormat.FOOTPRINT)
        if (stanceImageEnabled) add(StanceQuestionFormat.STANCE_IMAGE)
    }

internal data class QuizSession(
    val question: QuizQuestion?,
    val answeredCount: Int = 0,
    val askedPrompts: Set<TechniqueQuestionKey> = emptySet(),
    val askedVocabularyPrompts: Set<VocabularyQuestionKey> = emptySet(),
    val askedStancePrompts: Set<StanceQuestionKey> = emptySet()
) {
    fun next(pool: List<ItemEntity>, settings: QuizSettings, random: Random = Random.Default,
             bodyParts: List<BodyPartEntity> = emptyList(), stances: List<ItemEntity> = emptyList(),
             stanceImages: Map<String, StanceQuizImages> = emptyMap(),
             terminology: List<VocabularyTerm> = emptyList()): QuizSession {
        val current = question ?: return this
        val answered = answeredCount + 1
        return generate(pool, bodyParts, stances, stanceImages, terminology, settings, answered, askedPrompts, askedVocabularyPrompts,
            askedStancePrompts, current, random)
    }

    companion object {
        fun start(pool: List<ItemEntity>, settings: QuizSettings, random: Random = Random.Default,
                  bodyParts: List<BodyPartEntity> = emptyList(), stances: List<ItemEntity> = emptyList(),
                  stanceImages: Map<String, StanceQuizImages> = emptyMap(),
                  terminology: List<VocabularyTerm> = emptyList()) =
            generate(pool, bodyParts, stances, stanceImages, terminology, settings, 0, emptySet(), emptySet(), emptySet(), null, random)

        private fun generate(
            pool: List<ItemEntity>,
            bodyParts: List<BodyPartEntity>,
            stances: List<ItemEntity>,
            stanceImages: Map<String, StanceQuizImages>,
            terminology: List<VocabularyTerm>,
            settings: QuizSettings,
            answeredCount: Int,
            askedPrompts: Set<TechniqueQuestionKey>,
            askedVocabularyPrompts: Set<VocabularyQuestionKey>,
            askedStancePrompts: Set<StanceQuestionKey>,
            previousQuestion: QuizQuestion?,
            random: Random
        ): QuizSession {
            val excludedTechniques = if (settings.noRepeats) askedPrompts else emptySet()
            val excludedVocabulary = if (settings.noRepeats) askedVocabularyPrompts else emptySet()
            val excludedStances = if (settings.noRepeats) askedStancePrompts else emptySet()
            val generators: List<Pair<Int, () -> QuizQuestion?>> = settings.enabledTechniqueFormats.map { format ->
                val count = techniqueQuestionPool(pool, format, settings.answerChoiceCount(format), excludedTechniques).prompts.size
                count to
                {
                    createTechniqueQuestion(
                        pool = pool,
                        previousVideoKey = (previousQuestion as? TechniqueQuizQuestion)?.correctTechnique?.videoKey,
                        random = random,
                        format = format,
                        choiceCount = settings.answerChoiceCount(format),
                        excludedPrompts = excludedTechniques,
                        studySeconds = settings.studySeconds
                    )
                }
            }
            val vocabulary = bodyParts.map { it.asVocabularyTerm() } + terminology
            val vocabularyGenerators: List<Pair<Int, () -> QuizQuestion?>> = settings.enabledVocabularyFormats.map { format ->
                val count = vocabularyQuestionPool(vocabulary, settings, excludedVocabulary, format).prompts.size
                count to
                {
                    createVocabularyQuestion(vocabulary, settings,
                        excludedVocabulary,
                        (previousQuestion as? VocabularyQuizQuestion)?.promptKey, random, format)
                }
            }
            val stanceGenerators: List<Pair<Int, () -> QuizQuestion?>> = settings.enabledStanceFormats.map { format ->
                val count = stanceQuestionPool(stances, settings, excludedStances, format, stanceImages).prompts.size
                count to
                {
                    createStanceQuestion(stances, settings,
                        excludedStances,
                        (previousQuestion as? StanceQuizQuestion)?.promptKey, random, format, stanceImages)
                }
            }
            val allGenerators = generators + vocabularyGenerators + stanceGenerators
            val totalPrompts = allGenerators.sumOf { it.first }
            val question = if (totalPrompts == 0) null else {
                // One ticket per eligible prompt, rather than one per question format.
                var ticket = random.nextInt(totalPrompts)
                allGenerators.first { (count, _) ->
                    ticket -= count
                    ticket < 0
                }.second()
            }
            return QuizSession(
                question = question,
                answeredCount = answeredCount,
                askedPrompts = if (settings.noRepeats && question is TechniqueQuizQuestion) askedPrompts + question.key else askedPrompts,
                askedVocabularyPrompts = if (settings.noRepeats && question is VocabularyQuizQuestion) askedVocabularyPrompts + question.promptKey else askedVocabularyPrompts,
                askedStancePrompts = if (settings.noRepeats && question is StanceQuizQuestion) askedStancePrompts + question.promptKey else askedStancePrompts
            )
        }
    }
}
