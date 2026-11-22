package fi.tkd.itfun.quiz

internal enum class TranslationQuestionFormat { ENGLISH_TO_KOREAN, KOREAN_TO_ENGLISH }

internal sealed interface TranslationQuizQuestion : QuizQuestion {
    val format: TranslationQuestionFormat
    val promptText: String
    val answerNames: List<String>
    val answerExplanation: String
}
