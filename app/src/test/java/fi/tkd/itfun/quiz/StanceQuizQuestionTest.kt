package fi.tkd.itfun.quiz

import fi.tkd.itfun.data.db.ItemEntity
import org.junit.Assert.*
import org.junit.Test

class StanceQuizQuestionTest {
    private val stances = (1..8).map {
        ItemEntity(techniqueCode = it, name = "Stance $it", koreanName = "Sogi $it",
            categoryId = 1, videoKey = "stance$it", remoteVideoVersion = 1, beltLevel = if (it <= 5) 9 else 7)
    }
    private val images = stances.associate {
        it.videoKey to StanceQuizImages("${it.videoKey}_footprint.png", "${it.videoKey}_photo.png")
    }
    private fun createQuestion(
        pool: List<ItemEntity>, settings: QuizSettings,
        format: StanceQuestionFormat = StanceQuestionFormat.FOOTPRINT
    ) = createStanceQuestion(pool, settings, format = format, images = images)

    private val settings = QuizSettings(lessChoices = true, moreTime = true, theoryEnabled = true, selectedBeltGups = setOf(9))

    @Test
    fun promptsAndAllChoicesRespectIndividuallySelectedBelts() {
        repeat(30) {
            val question = createQuestion(stances, settings)!!
            assertTrue(question.choices.all { it.beltLevel == 9 })
            assertEquals(3, question.choices.size)
        }
        val other = createQuestion(stances, settings.copy(selectedBeltGups = setOf(7)))!!
        assertTrue(other.choices.all { it.beltLevel == 7 })
    }

    @Test
    fun hardModeUsesAllFiveAvailableStancesInsteadOfRequiringSix() {
        val question = createQuestion(stances, settings.copy(lessChoices = false, moreTime = false))!!
        assertEquals(5, question.choices.size)
        assertEquals(QuizTiming.HARD_STUDY_SECONDS, question.studySeconds)
        val six = createQuestion(stances, settings.copy(lessChoices = false, moreTime = false, selectedBeltGups = setOf(9, 7)))!!
        assertEquals(6, six.choices.size)
    }

    @Test
    fun smallerPoolsUseAvailableChoicesAndEmptyPoolsCannotCreateQuestions() {
        assertEquals(2, createQuestion(stances.take(2), settings)!!.choices.size)
        assertEquals(1, createQuestion(stances.take(1), settings.copy(lessChoices = false, moreTime = false))!!.choices.size)
        assertNull(createQuestion(emptyList(), settings))
        assertNull(createQuestion(stances, settings.copy(selectedBeltGups = emptySet())))
    }

    @Test
    fun eachImageFormatUsesItsOwnImageAndAlwaysOffersKoreanNames() {
        for (format in StanceQuestionFormat.entries) {
            val question = createQuestion(stances, settings, format)!!
            val correct = question.correctStance
            assertEquals(images.getValue(correct.videoKey).path(format), question.imagePath)
            assertEquals(question.choices.map { it.koreanName }, question.answerNames)
            assertEquals(correct.koreanName, question.answerNames[question.correctIndex])
            assertEquals("${correct.koreanName} = ${correct.name}", question.answerExplanation)
            assertEquals(if (format == StanceQuestionFormat.FOOTPRINT)
                "Which stance has the following footprint?" else "What is this stance?", question.promptText)
        }
    }

    @Test
    fun missingFirstImageNeverUsesTheSecondImageAsAFootprint() {
        val secondOnly = stances.associate { it.videoKey to StanceQuizImages(null, "${it.videoKey}.png") }
        assertNull(createStanceQuestion(stances, settings, images = secondOnly))
        val photo = createStanceQuestion(stances, settings, format = StanceQuestionFormat.STANCE_IMAGE, images = secondOnly)!!
        assertEquals("${photo.correctStance.videoKey}.png", photo.imagePath)
        assertNull(createStanceQuestion(stances, settings, images = emptyMap()))
    }

    @Test
    fun theoryCheckboxEnablesStancesAndNoRepeatsTracksBothImageFormats() {
        assertNull(QuizSession.start(emptyList(), settings.copy(theoryEnabled = false), stances = stances, stanceImages = images).question)
        val options = settings.copy(noRepeats = true)
        var session = QuizSession.start(emptyList(), options, stances = stances, stanceImages = images)
        val seen = mutableSetOf<StanceQuestionKey>()
        while (session.question != null) {
            val question = session.question as StanceQuizQuestion
            assertTrue(seen.add(question.promptKey))
            assertTrue(question.choices.all { it.beltLevel in options.selectedBeltGups })
            session = session.next(emptyList(), options, stances = stances, stanceImages = images)
        }
        assertEquals(10, session.answeredCount)
        assertEquals(5, seen.count { it.format == StanceQuestionFormat.FOOTPRINT })
        assertEquals(5, seen.count { it.format == StanceQuestionFormat.STANCE_IMAGE })
    }
}
