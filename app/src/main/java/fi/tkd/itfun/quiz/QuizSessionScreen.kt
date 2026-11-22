package fi.tkd.itfun.quiz

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun QuizSessionScreen(content: QuizContent, onClose: () -> Unit) {
    var session by remember(content) { mutableStateOf(content.initialSession) }
    var currentAnswered by remember(content) { mutableStateOf(false) }
    var endedEarly by remember(content) { mutableStateOf(false) }
    var wrongAnswers by remember(content) { mutableStateOf(emptyList<QuizQuestion>()) }
    val current = session.question
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (current == null) {
            Column(
                modifier = Modifier.fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Quiz results", style = MaterialTheme.typography.headlineMedium)
                    if (session.answeredCount == 1) {
                        Text("1 question answered.")
                    }
                    else {
                        Text("${session.answeredCount} questions answered.")
                    }
                    if (!endedEarly) Text("No questions left to ask.")
                    if (wrongAnswers.isEmpty()) Text("All answers correct!")
                }
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    val vocabularyMistakes = wrongAnswers.filterIsInstance<TranslationQuizQuestion>()
                    if (vocabularyMistakes.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Words you answered incorrectly:")
                            vocabularyMistakes.forEach { question ->
                                Text("${question.promptText} = ${question.answerNames[question.correctIndex]}")
                            }
                        }
                    }
                    val techniqueAndStanceMistakes = wrongAnswers.mapNotNull { question ->
                        when (question) {
                            is TechniqueQuizQuestion -> question.correctTechnique.koreanName
                            is StanceQuizQuestion -> question.correctStance.koreanName
                            else -> null
                        }
                    }
                    if (techniqueAndStanceMistakes.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Techniques and/or stances you didn't recognize:")
                            techniqueAndStanceMistakes.forEach { Text(it) }
                        }
                    }
                }
            }
        } else {
            key(session.answeredCount) {
                QuizQuestionScreen(current, onNext = {
                    session = session.next(content.pool, content.settings, bodyParts = content.bodyParts,
                        stances = content.stances, stanceImages = content.stanceImages, terminology = content.terminology)
                    currentAnswered = false
                }, onAnswered = { selectedChoice ->
                    currentAnswered = true
                    if (selectedChoice != current.correctIndex) wrongAnswers = wrongAnswers + current
                })
            }
        }
        IconButton(
            onClick = {
                val answeredCount = session.answeredCount + if (currentAnswered) 1 else 0
                if (current == null || answeredCount < 1) {
                    onClose()
                } else {
                    endedEarly = true
                    session = session.copy(question = null, answeredCount = answeredCount)
                    currentAnswered = false
                }
            },
            modifier = Modifier.align(Alignment.TopEnd)
                .padding(8.dp)
                .semantics { contentDescription = if (current == null) "Return to quiz setup" else "End quiz" }
                .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
        ) {
            Text("X", color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
