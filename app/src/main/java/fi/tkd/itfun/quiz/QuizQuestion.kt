package fi.tkd.itfun.quiz

internal data class QuizQuestionPool<T>(val answers: List<T>, val prompts: List<T>)

internal sealed interface QuizQuestion {
    val correctIndex: Int
    val studySeconds: Int
}
