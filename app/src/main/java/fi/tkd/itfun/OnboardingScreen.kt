package fi.tkd.itfun

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

@Composable
fun OnboardingScreen(
    backgroundColor: Color,
    onOpenTechniques: () -> Unit,
    onOpenBodyParts: () -> Unit,
    onOpenCompendium: () -> Unit,
    onOpenQuiz: () -> Unit
) {
    var hoveredButton by remember { mutableStateOf<String?>(null) }

    var boxCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var techniquesBounds by remember { mutableStateOf<Rect?>(null) }
    var bodyPartsBounds by remember { mutableStateOf<Rect?>(null) }
    var compendiumBounds by remember { mutableStateOf<Rect?>(null) }
    var quizBounds by remember { mutableStateOf<Rect?>(null) }

    val infoText = when (hoveredButton) {
        "techniques" -> "Recall techniques and patterns through videos"
        "bodyParts" -> "Learn body parts and vital points in Korean"
        "compendium" -> "Study Taekwon-Do theory, history, and fundamentals"
        "quiz" -> "Put everything you've learned to the test"
        else -> "Choose a topic to get started"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .onGloballyPositioned { boxCoords = it }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pointer = event.changes.firstOrNull()

                        if (pointer == null || !pointer.pressed) {
                            hoveredButton = null
                            continue
                        }

                        val posInWindow =
                            boxCoords?.localToWindow(pointer.position) ?: continue

                        val newHoveredButton = when {
                            techniquesBounds?.contains(posInWindow) == true -> "techniques"
                            bodyPartsBounds?.contains(posInWindow) == true -> "bodyParts"
                            compendiumBounds?.contains(posInWindow) == true -> "compendium"
                            quizBounds?.contains(posInWindow) == true -> "quiz"
                            else -> null
                        }

                        if (hoveredButton != newHoveredButton) {
                            hoveredButton = newHoveredButton
                        }
                    }
                }
            }
    ) {
        Image(
            painter = painterResource(R.drawable.onboard_icon),
            contentDescription = null,
            modifier = Modifier
                .size(288.dp)
                .align(Alignment.Center)
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 230.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = infoText,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onOpenTechniques,
                modifier = Modifier.width(160.dp).onGloballyPositioned {
                    techniquesBounds = it.boundsInWindow()
                }
            ) {
                Text("Techniques")
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onOpenBodyParts,
                modifier = Modifier.width(135.dp).onGloballyPositioned {
                    bodyPartsBounds = it.boundsInWindow()
                }
            ) {
                Text("Body parts")
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onOpenCompendium,
                modifier = Modifier.width(110.dp).onGloballyPositioned {
                    compendiumBounds = it.boundsInWindow()
                },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Compendium")
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onOpenQuiz,
                modifier = Modifier.width(85.dp).onGloballyPositioned {
                    quizBounds = it.boundsInWindow()
                }
            ) {
                Text("Quiz")
            }
        }
    }
}
