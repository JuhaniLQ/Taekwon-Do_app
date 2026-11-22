package fi.tkd.itfun.quiz

import fi.tkd.itfun.data.db.ItemEntity
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class TechniqueQuizQuestionTest {
    @Test
    fun choicesShareACategoryAndContainTheNamedTechnique() {
        val pool = (1..12).map { item(it, category = if (it <= 6) 1 else 2) }
        repeat(50) { seed ->
            val question = createTechniqueQuestion(pool, random = Random(seed))!!
            assertEquals(3, question.videos.size)
            assertEquals(1, question.videos.map { it.categoryId }.distinct().size)
            assertEquals(3, question.videos.map { it.videoKey }.distinct().size)
            assertEquals(3, question.videos.map { it.koreanName }.distinct().size)
            assertTrue(question.correctIndex in 0..2)
            assertTrue(question.correctTechnique in pool)
        }
    }

    @Test
    fun duplicateNamesAndVideosCannotCreateAmbiguousChoices() {
        val pool = listOf(
            item(1), item(2, name = " technique 1 "),
            item(3, video = "video1"), item(4), item(5)
        )
        val question = createTechniqueQuestion(pool, random = Random(1))!!
        assertEquals(3, question.videos.map { it.videoKey }.distinct().size)
        assertEquals(setOf("technique 1", "technique 4", "technique 5"),
            question.videos.map { it.koreanName.trim().lowercase() }.toSet())
    }

    @Test
    fun smallerCategoriesUseFallbackAndPreviousPromptDoesNotRepeat() {
        val pool = listOf(item(1, category = 2), item(2, category = 2)) + (3..5).map { item(it) }
        repeat(20) { seed ->
            val question = createTechniqueQuestion(pool, "video3", Random(seed))!!
            val category = question.correctTechnique.categoryId
            assertEquals(if (category == 2L) 2 else 3, question.videos.count { it.categoryId == category })
            assertNotEquals("video3", question.correctTechnique.videoKey)
        }
    }

    @Test
    fun returnsNoQuestionWhenThereAreNotEnoughDistinctChoicesOverall() {
        assertNull(createTechniqueQuestion(listOf(item(1), item(2))))
        assertNull(createTechniqueQuestion(listOf(item(1), item(2, name = "Technique 1"), item(3))))
    }

    @Test
    fun fiveBlocksNeedOneOtherCategoryForSixChoicesButNoneForThree() {
        val blocks = (1..5).map { item(it) }
        val kicks = (6..10).map { item(it, category = 2) }
        val excluded = kicks.map {
            TechniqueQuestionKey(TechniqueQuestionFormat.NAME_TO_VIDEO, it.koreanName.lowercase())
        }.toSet()
        repeat(20) { seed ->
            val six = createTechniqueQuestion(blocks + kicks, random = Random(seed), choiceCount = 6, excludedPrompts = excluded)!!
            assertEquals(1L, six.correctTechnique.categoryId)
            assertEquals(5, six.videos.count { it.categoryId == 1L })
            assertEquals(1, six.videos.count { it.categoryId == 2L })

            val three = createTechniqueQuestion(blocks + kicks, random = Random(seed), excludedPrompts = excluded)!!
            assertTrue(three.videos.all { it.categoryId == 1L })
        }
    }

    private fun item(code: Int, category: Long = 1, name: String = "Technique $code", video: String = "video$code") =
        ItemEntity(
            techniqueCode = code, name = "English $code", koreanName = name,
            categoryId = category, videoKey = video, remoteVideoVersion = 1,
            beltLevel = 9, localVideoPath = "/videos/$video.mp4"
        )
}
