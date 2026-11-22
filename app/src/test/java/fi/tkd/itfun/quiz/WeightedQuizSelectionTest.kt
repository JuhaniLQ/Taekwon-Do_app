package fi.tkd.itfun.quiz

import fi.tkd.itfun.data.db.BodyPartEntity
import fi.tkd.itfun.data.db.ItemEntity
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class WeightedQuizSelectionTest {
    private val techniques = (1..6).map { code ->
        ItemEntity(techniqueCode = code, name = "Technique $code", koreanName = "Korean $code",
            categoryId = 1, videoKey = "video$code", remoteVideoVersion = 1,
            beltLevel = 9, localVideoPath = "/video$code.mp4")
    }
    private val body = (1..5).map { BodyPartEntity("body$it", "ARM", false, "Body $it", "Part $it") }
    private val terms = (1..5).map {
        VocabularyTerm("term$it", "Term $it", "Word $it", VocabularySource.TERMINOLOGY)
    }
    private val stances = techniques.take(2).map { it.copy(videoKey = "stance${it.techniqueCode}") }
    private val images = stances.associate { it.videoKey to StanceQuizImages("footprint", "photo") }
    private val settings = QuizSettings(lessChoices = true, moreTime = true, selectedBeltGups = setOf(9), techniquesEnabled = true,
        theoryEnabled = true, bodyPartsEnabled = true, noRepeats = true)

    // Control only the pool-selection ticket; answer generation still uses a seeded RNG.
    private class TicketRandom(private val ticket: Int) : Random() {
        private val delegate = Random(42)
        var firstBound: Int? = null
        override fun nextBits(bitCount: Int) = delegate.nextBits(bitCount)
        override fun nextInt(until: Int): Int {
            if (firstBound != null) return delegate.nextInt(until)
            firstBound = until
            return ticket.also { require(it in 0 until until) }
        }
    }

    @Test
    fun eachEligiblePromptGetsOneTicketIncludingBothDirectionsAndMixedVocabulary() {
        // Six techniques, ten mixed vocabulary terms and two stances, each in two formats.
        for (ticket in 0 until 36) {
            val random = TicketRandom(ticket)
            val question = QuizSession.start(techniques, settings, random, body, stances, images, terms).question
            assertEquals(36, random.firstBound)
            when (ticket) {
                in 0..11 -> assertTrue(question is TechniqueQuizQuestion)
                in 12..31 -> assertTrue(question is VocabularyQuizQuestion)
                else -> assertTrue(question is StanceQuizQuestion)
            }
        }
    }

    @Test
    fun weightsShrinkWithUnaskedPromptsWhilePreviouslyAskedItemsRemainAnswerOptions() {
        val session = QuizSession.start(techniques, settings, Random(1), body, stances, images, terms).copy(
            askedPrompts = TechniqueQuestionFormat.entries.flatMap { format ->
                techniques.dropLast(1).map {
                    TechniqueQuestionKey(format, if (format == TechniqueQuestionFormat.NAME_TO_VIDEO)
                        it.koreanName.lowercase() else it.videoKey)
                }
            }.toSet(),
            askedVocabularyPrompts = TranslationQuestionFormat.entries.flatMap { format ->
                (body.map { it.asVocabularyTerm() } + terms).dropLast(1).map {
                    VocabularyQuestionKey(format, if (format == TranslationQuestionFormat.ENGLISH_TO_KOREAN)
                        it.nameEn.lowercase() else it.nameKo.lowercase())
                }
            }.toSet(),
            askedStancePrompts = StanceQuestionFormat.entries.map { StanceQuestionKey(it, stances.first().videoKey) }.toSet()
        )
        // One remaining item in each pool, in two formats each.
        for (ticket in 0 until 6) {
            val random = TicketRandom(ticket)
            val question = session.next(techniques, settings, random, body, stances, images, terms).question
            assertEquals(6, random.firstBound)
            when (ticket) {
                in 0..1 -> assertEquals(3, (question as TechniqueQuizQuestion).videos.size)
                in 2..3 -> assertEquals(5, (question as VocabularyQuizQuestion).choices.size)
                else -> assertEquals(2, (question as StanceQuizQuestion).choices.size)
            }
        }
    }
}
