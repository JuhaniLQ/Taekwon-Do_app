package fi.tkd.itfun.quiz

internal object QuizTiming {
    const val NORMAL_STUDY_SECONDS = 15
    const val HARD_STUDY_SECONDS = 5
}

internal data class QuizSettings(
    val selectedBeltGups: Set<Int> = emptySet(),
    val techniquesEnabled: Boolean = true,
    val theoryEnabled: Boolean = true,
    val bodyPartsEnabled: Boolean = true,
    val techniqueVideoToNameEnabled: Boolean = true,
    val techniqueNameToVideoEnabled: Boolean = true,
    val bodyPartEnglishToKoreanEnabled: Boolean = true,
    val bodyPartKoreanToEnglishEnabled: Boolean = true,
    val terminologyEnglishToKoreanEnabled: Boolean = true,
    val terminologyKoreanToEnglishEnabled: Boolean = true,
    val stanceFootprintEnabled: Boolean = true,
    val stanceImageEnabled: Boolean = true,
    val noRepeats: Boolean = true,
    val lessChoices: Boolean = false,
    val moreTime: Boolean = false
) {
    val studySeconds: Int
        get() = if (moreTime) QuizTiming.NORMAL_STUDY_SECONDS else QuizTiming.HARD_STUDY_SECONDS

    val textAnswerChoiceCount: Int get() = if (lessChoices) 3 else 6

    val vocabularyAnswerChoiceCount: Int get() = if (lessChoices) 5 else 10

    fun answerChoiceCount(format: TechniqueQuestionFormat): Int =
        if (format == TechniqueQuestionFormat.VIDEO_TO_NAME) textAnswerChoiceCount else 3
}
