package fi.tkd.itfun.quiz

import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fi.tkd.itfun.techniques.TechniqueVideo
import fi.tkd.itfun.compendium.CompendiumImage
import fi.tkd.itfun.ui.theme.Green
import fi.tkd.itfun.ui.theme.Red
import kotlinx.coroutines.delay

@Composable
internal fun QuizQuestionScreen(question: QuizQuestion, onNext: () -> Unit, onAnswered: (Int) -> Unit) {
    val technique = question as? TechniqueQuizQuestion
    val translation = question as? TranslationQuizQuestion
    val stance = question as? StanceQuizQuestion
    val videoPrompt = technique?.format == TechniqueQuestionFormat.VIDEO_TO_NAME
    val textAnswers = videoPrompt || translation != null || stance != null
    val names = when (question) {
        is TechniqueQuizQuestion -> question.videos.map { it.koreanName }
        is TranslationQuizQuestion -> question.answerNames
        is StanceQuizQuestion -> question.answerNames
    }
    var studying by remember { mutableStateOf(true) }
    var secondsLeft by remember { mutableIntStateOf(question.studySeconds) }
    var selectedChoice by remember { mutableIntStateOf(0) }
    var videoPlaying by remember { mutableStateOf(true) }
    var submittedAnswer by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(studying) {
        if (studying) {
            while (secondsLeft > 0) {
                delay(1_000)
                secondsLeft--
            }
            studying = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(start = 8.dp, end = 8.dp, top = 90.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (studying) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    when {
                        stance != null -> stance.promptText
                        translation?.format == TranslationQuestionFormat.KOREAN_TO_ENGLISH ->
                            "What does this mean?\n\n${translation.promptText}"
                        translation != null -> "What is this in Korean?\n\n${translation.promptText}"
                        videoPrompt -> "What is this technique called?"
                        else -> "Which technique is called \n\n ${technique!!.correctTechnique.koreanName}?"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                if (stance != null) {
                    CompendiumImage(
                        path = stance.imagePath,
                        tint = if (stance.format == StanceQuestionFormat.FOOTPRINT) {
                            if (isSystemInDarkTheme()) Color.White else Color.Black
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                    Spacer(Modifier.height(24.dp))
                }
                if (videoPrompt) {
                    TechniqueVideo(
                        technique!!.correctTechnique.localVideoPath!!,
                        modifier = Modifier.fillMaxWidth(0.8f).clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.height(24.dp))
                }
                Text("$secondsLeft s")
            }
            QuizActionButton(
                text = "Ready",
                modifier = Modifier.padding(bottom = QuizActionButtonHeight),
                onClick = { studying = false }
            )
        } else {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (textAnswers) {
                    names.forEachIndexed { index, name ->
                        val selected = index == selectedChoice
                        val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                        Row(
                            modifier = Modifier.fillMaxWidth(0.8f)
                                .align(Alignment.CenterHorizontally)
                                .clip(RoundedCornerShape(12.dp))
                                .border(3.dp, borderColor, RoundedCornerShape(12.dp))
                                .selectable(selected = selected, role = Role.RadioButton) {
                                    selectedChoice = index
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${'A' + index})")
                            Text(name, modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                } else {
                    technique!!.videos.forEachIndexed { index, item ->
                        key(item.videoKey) {
                            val selected = index == selectedChoice
                            val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                            Box(Modifier.fillMaxWidth()) {
                                TechniqueVideo(
                                    item.localVideoPath!!,
                                    modifier = Modifier.fillMaxWidth(0.8f)
                                        .align(Alignment.Center)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(3.dp, borderColor, RoundedCornerShape(12.dp)),
                                    playWhenReady = selected && videoPlaying,
                                    onClick = {
                                        if (selected) videoPlaying = !videoPlaying
                                        else {
                                            selectedChoice = index
                                            videoPlaying = true
                                        }
                                    }
                                )
                                Box(
                                    modifier = Modifier.align(Alignment.CenterStart)
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .border(3.dp, borderColor, CircleShape)
                                        .selectable(selected = selected, role = Role.RadioButton) {
                                            selectedChoice = index
                                            videoPlaying = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${'A' + index})")
                                }
                            }
                        }
                    }
                }
            }
            val answer = submittedAnswer
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Selected answer: ${'A' + (answer ?: selectedChoice)}")
                if (answer != null) {
                    val correct = answer == question.correctIndex
                    Text(
                        if (correct) "✓" else "✗",
                        color = if (correct) Green else Red,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 8.dp).semantics {
                            contentDescription = if (correct) "Correct answer" else "Incorrect answer"
                        }
                    )
                }
            }
            if (answer != null) {
                val correctLabel = 'A' + question.correctIndex
                Text(
                    if (answer == question.correctIndex) "The correct answer is $correctLabel, good job!"
                    else "Ooops! The correct answer is $correctLabel",
                    textAlign = TextAlign.Center
                )
                if (stance != null) {
                    Text(stance.answerExplanation, textAlign = TextAlign.Center)
                } else if (translation != null) {
                    Text(
                        translation.answerExplanation,
                        textAlign = TextAlign.Center
                    )
                } else if (!videoPrompt) {
                    Text(
                        "(${technique!!.correctTechnique.koreanName})",
                        textAlign = TextAlign.Center
                    )
                }
            }
            QuizActionButton(
                text = if (answer == null) "Submit" else "Next",
                onClick = {
                    if (submittedAnswer == null) {
                        submittedAnswer = selectedChoice
                        onAnswered(selectedChoice)
                    } else onNext()
                }
            )
        }
    }
}

internal val QuizActionButtonHeight = 48.dp

@Composable
internal fun QuizActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.size(width = 120.dp, height = QuizActionButtonHeight),
        enabled = enabled
    ) {
        Text(text)
    }
}
