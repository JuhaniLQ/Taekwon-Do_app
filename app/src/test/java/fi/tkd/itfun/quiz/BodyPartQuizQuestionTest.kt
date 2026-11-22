package fi.tkd.itfun.quiz

import fi.tkd.itfun.data.db.BodyPartEntity
import fi.tkd.itfun.data.db.ItemEntity
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class VocabularyQuizQuestionTest {
    private val bodyParts = (1..10).map {
        BodyPartEntity("BODY_$it", listOf("HEAD", "TORSO", "ARM", "LEG")[(it - 1) % 4],
            false, "English $it", "Korean $it")
    }
    private fun createBodyQuestion(
        pool: List<BodyPartEntity>, settings: QuizSettings, random: Random = Random.Default
    ) = createVocabularyQuestion(pool.map { it.asVocabularyTerm() }, settings, random = random)

    private val settings = QuizSettings(lessChoices = true, moreTime = true, bodyPartsEnabled = true, noRepeats = false)

    @Test
    fun allRegionsCanSupplyPromptsAndChoices() {
        val seen = mutableSetOf<String>()
        repeat(100) { seed ->
            val question = createBodyQuestion(bodyParts, settings, random = Random(seed))!!
            assertEquals(5, question.choices.size)
            assertEquals(5, question.choices.map { it.nameKo }.distinct().size)
            seen += question.correctTerm.code
        }
        assertEquals(bodyParts.map { it.code }.toSet(), seen)
        // No single region has five entries, but the full pool still supplies a question.
        assertNotNull(createBodyQuestion(bodyParts.take(5), settings))
    }

    @Test
    fun hardModeHasTenChoicesAndSharedReviewTime() {
        val question = createBodyQuestion(bodyParts, settings.copy(lessChoices = false, moreTime = false))!!
        assertEquals(10, question.choices.size)
        assertEquals(QuizTiming.HARD_STUDY_SECONDS, question.studySeconds)
        assertNull(createBodyQuestion(bodyParts.take(9), settings.copy(lessChoices = false, moreTime = false)))
    }

    @Test
    fun koreanPromptUsesEnglishChoicesAndCanBeEnabledOnItsOwn() {
        val options = settings.copy(bodyPartEnglishToKoreanEnabled = false)
        var session = QuizSession.start(emptyList(), options, bodyParts = bodyParts)
        repeat(10) {
            val question = session.question as VocabularyQuizQuestion
            assertEquals(TranslationQuestionFormat.KOREAN_TO_ENGLISH, question.format)
            assertEquals(question.correctTerm.nameKo, question.promptText)
            assertEquals(question.choices.map { it.nameEn }, question.answerNames)
            assertEquals(question.correctTerm.nameEn, question.answerNames[question.correctIndex])
            session = session.next(emptyList(), options, bodyParts = bodyParts)
        }
    }

    @Test
    fun noRepeatsExhaustsBodyPromptsWhileRetainingThemAsWrongOptions() {
        val options = settings.copy(noRepeats = true)
        var session = QuizSession.start(emptyList(), options, bodyParts = bodyParts)
        val seen = mutableSetOf<VocabularyQuestionKey>()
        while (session.question != null) {
            val question = session.question as VocabularyQuizQuestion
            assertTrue(seen.add(question.promptKey))
            assertEquals(5, question.choices.size)
            session = session.next(emptyList(), options, bodyParts = bodyParts)
        }
        assertEquals(20, seen.size)
        assertEquals(10, seen.count { it.format == TranslationQuestionFormat.ENGLISH_TO_KOREAN })
        assertEquals(10, seen.count { it.format == TranslationQuestionFormat.KOREAN_TO_ENGLISH })
        assertEquals(20, session.answeredCount)
        assertNull(QuizSession.start(emptyList(), settings.copy(bodyPartsEnabled = false), bodyParts = bodyParts).question)
    }

    @Test
    fun ambiguousNamesCannotSupplyMultipleChoices() {
        val items = bodyParts.take(5)
        assertNull(createBodyQuestion(items.map { it.copy(nameKo = "Same") }, settings))
        assertNull(createBodyQuestion(items.map { it.copy(nameEn = "Same") }, settings))
    }

    @Test
    fun mixedQuizExhaustsAllTypes() {
        val videos = (1..5).map {
            ItemEntity(techniqueCode = it, name = "Technique $it", koreanName = "Name $it",
                categoryId = 1, videoKey = "video$it", remoteVideoVersion = 1, beltLevel = 9)
        }
        val options = settings.copy(techniquesEnabled = true, noRepeats = true)
        var session = QuizSession.start(videos, options, bodyParts = bodyParts)
        val seen = mutableSetOf<String>()
        while (session.question != null) {
            val question = session.question!!
            val key = when (question) {
                is VocabularyQuizQuestion -> "body:${question.promptKey}"
                is TechniqueQuizQuestion -> "technique:${question.key}"
                is StanceQuizQuestion -> "stance:${question.promptKey}"
            }
            assertTrue(seen.add(key))
            session = session.next(videos, options, bodyParts = bodyParts)
        }
        assertEquals(30, session.answeredCount)


    }
}
