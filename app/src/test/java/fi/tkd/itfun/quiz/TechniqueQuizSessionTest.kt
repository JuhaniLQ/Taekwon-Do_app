package fi.tkd.itfun.quiz

import fi.tkd.itfun.data.db.ItemEntity
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class QuizSessionTest {
    @Test
    fun choicesAndReviewTimeCanBeChangedIndependently() {
        for (lessChoices in listOf(false, true)) {
            for (moreTime in listOf(false, true)) {
                val settings = QuizSettings(lessChoices = lessChoices, moreTime = moreTime)
                assertEquals(if (lessChoices) 3 else 6, settings.textAnswerChoiceCount)
                assertEquals(if (lessChoices) 5 else 10, settings.vocabularyAnswerChoiceCount)
                assertEquals(if (moreTime) QuizTiming.NORMAL_STUDY_SECONDS else QuizTiming.HARD_STUDY_SECONDS,
                    settings.studySeconds)
                assertEquals(3, settings.answerChoiceCount(TechniqueQuestionFormat.NAME_TO_VIDEO))
            }
        }
    }

    private val pool = (1..6).map { code ->
        ItemEntity(
            techniqueCode = code, name = "English $code", koreanName = "Korean $code",
            categoryId = 1, videoKey = "video$code", remoteVideoVersion = 1,
            beltLevel = 9, localVideoPath = "/videos/video$code.mp4"
        )
    }

    @Test
    fun techniquesCheckboxControlsQuestionAvailabilityEvenWithOtherCategoriesSelected() {
        val settings = QuizSettings(
            techniquesEnabled = false, theoryEnabled = true, bodyPartsEnabled = true,
            techniqueNameToVideoEnabled = true, techniqueVideoToNameEnabled = true
        )
        assertTrue(settings.enabledTechniqueFormats.isEmpty())
        assertNull(QuizSession.start(pool, settings).question)
        assertNotNull(QuizSession.start(pool, settings.copy(techniquesEnabled = true)).question)
    }

    @Test
    fun noRepeatsExhaustsBothFormatsSeparatelyAndKeepsOldPromptsAsDistractors() {
        val items = pool.take(3)
        val settings = QuizSettings(lessChoices = true, moreTime = true, techniquesEnabled = true, noRepeats = true, techniqueNameToVideoEnabled = true)
        val random = Random(1)
        var session = QuizSession.start(items, settings, random)
        val seen = mutableSetOf<TechniqueQuestionKey>()
        while (session.question != null) {
            val question = session.question!! as TechniqueQuizQuestion
            assertTrue("Repeated prompt", seen.add(question.key))
            // Even when only one prompt is left, previously asked items remain answer options.
            assertEquals(3, question.videos.size)
            assertTrue(question.videos.all { it in items })
            session = session.next(items, settings, random)
        }
        assertEquals(6, session.answeredCount)
        assertEquals(3, seen.count { it.format == TechniqueQuestionFormat.NAME_TO_VIDEO })
        assertEquals(3, seen.count { it.format == TechniqueQuestionFormat.VIDEO_TO_NAME })
    }

    @Test
    fun defaultSettingsAllowMoreThanTwentyAndKeepNormalDifficulty() {
        val settings = QuizSettings(lessChoices = true, moreTime = true, techniquesEnabled = true, noRepeats = false)
        var session = QuizSession.start(pool, settings)
        repeat(30) {
            val question = session.question as TechniqueQuizQuestion
            assertEquals(3, question.videos.size)
            assertEquals(QuizTiming.NORMAL_STUDY_SECONDS, session.question!!.studySeconds)
            session = session.next(pool, settings)
        }
        assertNotNull(session.question)
    }

    @Test
    fun hardModeUsesSixTextChoicesButThreeVideoChoicesAndFiveSecondsForBoth() {
        val text = QuizSession.start(pool, QuizSettings(techniquesEnabled = true, lessChoices = false, moreTime = false, techniqueNameToVideoEnabled = false)).question!! as TechniqueQuizQuestion
        assertEquals(TechniqueQuestionFormat.VIDEO_TO_NAME, text.format)
        assertEquals(6, text.videos.size)
        assertEquals(QuizTiming.HARD_STUDY_SECONDS, text.studySeconds)

        val video = QuizSession.start(pool, QuizSettings(techniquesEnabled = true,
            lessChoices = false, moreTime = false, techniqueVideoToNameEnabled = false, techniqueNameToVideoEnabled = true
        )).question!! as TechniqueQuizQuestion
        assertEquals(3, video.videos.size)
        assertEquals(QuizTiming.HARD_STUDY_SECONDS, video.studySeconds)
    }

    @Test
    fun hardModeFillsMissingChoicesFromOtherCategories() {
        val items = pool.map { it.copy(categoryId = if (it.techniqueCode <= 3) 1 else 2) }
        val question = QuizSession.start(items, QuizSettings(techniquesEnabled = true, lessChoices = false, moreTime = false, techniqueNameToVideoEnabled = false)).question!! as TechniqueQuizQuestion
        assertEquals(6, question.videos.size)
        assertEquals(3, question.videos.count { it.categoryId == question.correctTechnique.categoryId })
    }

    @Test
    fun noRepeatsCanEndBeforeTwenty() {
        val settings = QuizSettings(lessChoices = true, moreTime = true, techniquesEnabled = true, noRepeats = true, techniqueNameToVideoEnabled = false)
        var session = QuizSession.start(pool.take(3), settings)
        repeat(3) { session = session.next(pool.take(3), settings) }
        assertNull(session.question)
        assertEquals(3, session.answeredCount)
    }
}
