package fi.tkd.itfun.quiz

import fi.tkd.itfun.data.db.BodyPartEntity
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class MixedVocabularyQuizTest {
    private val body = listOf(
        VocabularyTerm("ELBOW", "Elbow", "Palkup", VocabularySource.BODY_PART),
        VocabularyTerm("FOREARM", "Forearm", "Palmok", VocabularySource.BODY_PART),
        VocabularyTerm("KNEE", "Knee", "Murup", VocabularySource.BODY_PART),
        VocabularyTerm("PALM", "Palm", "Sonbadak", VocabularySource.BODY_PART),
        VocabularyTerm("FOOT", "Foot", "Bal", VocabularySource.BODY_PART)
    )
    private val numbers = listOf(
        VocabularyTerm("numbers:9", "9", "ahop", VocabularySource.TERMINOLOGY),
        VocabularyTerm("numbers:8", "8", "yodul", VocabularySource.TERMINOLOGY),
        VocabularyTerm("numbers:10", "10", "yol", VocabularySource.TERMINOLOGY),
        VocabularyTerm("numbers:11", "11", "yol hana", VocabularySource.TERMINOLOGY),
        VocabularyTerm("numbers:20", "20", "sumul", VocabularySource.TERMINOLOGY)
    )
    private val settings = QuizSettings(lessChoices = true, moreTime = true, bodyPartsEnabled = true, theoryEnabled = true,
        stanceFootprintEnabled = false, stanceImageEnabled = false, noRepeats = false)

    @Test
    fun vocabularyChoiceCountsDoNotChangeTechniqueChoiceCounts() {
        assertEquals(5, settings.vocabularyAnswerChoiceCount)
        assertEquals(3, settings.answerChoiceCount(TechniqueQuestionFormat.VIDEO_TO_NAME))
        assertEquals(10, settings.copy(lessChoices = false, moreTime = false).vocabularyAnswerChoiceCount)
        assertEquals(6, settings.copy(lessChoices = false, moreTime = false).answerChoiceCount(TechniqueQuestionFormat.VIDEO_TO_NAME))
        assertEquals(3, settings.copy(lessChoices = false, moreTime = false).answerChoiceCount(TechniqueQuestionFormat.NAME_TO_VIDEO))
    }

    @Test
    fun eachCheckboxFiltersBothPromptsAndAnswersEvenWhenThePoolContainsBothSources() {
        for (source in VocabularySource.entries) {
            val selected = settings.copy(bodyPartsEnabled = source == VocabularySource.BODY_PART,
                theoryEnabled = source == VocabularySource.TERMINOLOGY)
            repeat(20) { seed ->
                val question = createVocabularyQuestion(body + numbers, selected, random = Random(seed))!!
                assertEquals(source, question.correctTerm.source)
                assertTrue(question.choices.all { it.source == source })
            }
        }
    }

    @Test
    fun numberPromptCanHaveBodyPartsAmongItsKoreanAnswers() {
        val excluded = (body + numbers).filter { it.nameEn != "9" }.map {
            VocabularyQuestionKey(TranslationQuestionFormat.ENGLISH_TO_KOREAN, it.nameEn.lowercase())
        }.toSet()
        val question = createVocabularyQuestion(body + numbers, settings.copy(lessChoices = false, moreTime = false), excluded)!!
        assertEquals("9", question.promptText)
        assertEquals("ahop", question.answerNames[question.correctIndex])
        assertTrue(question.answerNames.contains("Palkup"))
        assertEquals(10, question.choices.size)
    }

    @Test
    fun koreanBodyPartPromptCanHaveNumericAnswers() {
        val excluded = (body + numbers).filter { it.nameKo != "Palkup" }.map {
            VocabularyQuestionKey(TranslationQuestionFormat.KOREAN_TO_ENGLISH, it.nameKo.lowercase())
        }.toSet()
        val question = createVocabularyQuestion(body + numbers, settings.copy(lessChoices = false, moreTime = false), excluded,
            format = TranslationQuestionFormat.KOREAN_TO_ENGLISH)!!
        assertEquals("Palkup", question.promptText)
        assertEquals("Elbow", question.answerNames[question.correctIndex])
        assertTrue(question.answerNames.contains("9"))
        assertEquals("Palkup = Elbow", question.answerExplanation)
    }

    @Test
    fun noRepeatsAsksEachBodyPartAndNumberOnceInEachDirection() {
        val options = settings.copy(noRepeats = true)
        val rows = body.map { BodyPartEntity(it.code, "ARM", false, it.nameEn, it.nameKo) }
        var session = QuizSession.start(emptyList(), options, bodyParts = rows, terminology = numbers)
        val seen = mutableSetOf<VocabularyQuestionKey>()
        val counts = mutableMapOf<VocabularySource, Int>()
        while (session.question != null) {
            val question = session.question as VocabularyQuizQuestion
            assertTrue(seen.add(question.promptKey))
            counts[question.correctTerm.source] = (counts[question.correctTerm.source] ?: 0) + 1
            session = session.next(emptyList(), options, bodyParts = rows, terminology = numbers)
        }
        assertEquals(20, session.answeredCount)
        assertEquals(10, counts[VocabularySource.BODY_PART])
        assertEquals(10, counts[VocabularySource.TERMINOLOGY])
    }
}
