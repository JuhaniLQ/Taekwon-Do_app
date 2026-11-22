package fi.tkd.itfun.quiz

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import fi.tkd.itfun.BELT_LEVEL
import fi.tkd.itfun.R
import fi.tkd.itfun.dataStore
import fi.tkd.itfun.data.db.DbProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import fi.tkd.itfun.shared.BeltSelectionGrid
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun QuizScreen(onClose: () -> Unit) {
    var settings by remember { mutableStateOf(QuizSettings()) }
    var preparedQuiz by remember { mutableStateOf<QuizContent?>(null) }
    var isPreparing by remember { mutableStateOf(false) }
    var showInsufficientContent by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val startValidationToast = remember(context) {
        Toast.makeText(context, "", Toast.LENGTH_SHORT)
    }
    DisposableEffect(startValidationToast) {
        onDispose { startValidationToast.cancel() }
    }

    LaunchedEffect(Unit) {
        val savedGup = context.dataStore.data.first()[BELT_LEVEL] ?: 9
        settings = settings.copy(selectedBeltGups = (1..9).filter { it >= savedGup }.toSet())
    }

    if (showInsufficientContent) {
        AlertDialog(
            onDismissRequest = { showInsufficientContent = false },
            title = { Text("Not enough content") },
            text = {
                Text("Not enough content is available for these quiz settings. Choose more belts or adjust the quiz options.")
            },
            confirmButton = {
                TextButton(onClick = { showInsufficientContent = false }) { Text("OK") }
            }
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(Modifier.fillMaxSize()) {
            val quiz = preparedQuiz
            if (quiz != null) {
                QuizSessionScreen(content = quiz, onClose = { preparedQuiz = null })
            } else {
                Column(
                    Modifier.fillMaxSize()
                        .padding(bottom = 8.dp + QuizActionButtonHeight * 3)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp)
                ) {
                    Box(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp)) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back),
                                contentDescription = "Back to onboarding",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            "Quiz",
                            style = MaterialTheme.typography.displaySmall,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    Spacer(Modifier.height(36.dp))
                    Text(
                        "Choose which belts are included",
                        modifier = Modifier.padding(horizontal = 70.dp)
                    )
                    BeltSelectionGrid(
                        isLevelSelected = { level -> 10 - level in settings.selectedBeltGups },
                        onSelectLevel = { level ->
                            val gup = 10 - level
                            val selected = settings.selectedBeltGups
                            settings = settings.copy(
                                selectedBeltGups = if (gup in selected) selected - gup else selected + gup
                            )
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column {
                            QuizCategoryCheckbox("Techniques", settings.techniquesEnabled) {
                                settings = settings.copy(techniquesEnabled = it)
                            }
                            QuizCategoryCheckbox("Body parts", settings.bodyPartsEnabled) {
                                settings = settings.copy(bodyPartsEnabled = it)
                            }
                            QuizCategoryCheckbox("Compendium", settings.theoryEnabled) {
                                settings = settings.copy(theoryEnabled = it)
                            }
                        }
                        Column {
                            QuizCategoryCheckbox("No repeats", settings.noRepeats) {
                                settings = settings.copy(noRepeats = it)
                            }
                            QuizCategoryCheckbox("Less choices", settings.lessChoices) {
                                settings = settings.copy(lessChoices = it)
                            }
                            QuizCategoryCheckbox("More time", settings.moreTime) {
                                settings = settings.copy(moreTime = it)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    QuizSettingsSummary(settings, Modifier.fillMaxWidth().padding(horizontal = 60.dp))
                    Spacer(Modifier.height(16.dp))
                }
                QuizActionButton(
                    text = "Start",
                    onClick = {
                        val message = when {
                            !settings.techniquesEnabled && !settings.theoryEnabled && !settings.bodyPartsEnabled ->
                                "Can't start quiz without choosing a question category"
                            settings.selectedBeltGups.isEmpty() ->
                                "Can't start quiz without choosing any belts"
                            else -> null
                        }
                        if (message != null) {
                            startValidationToast.setText(message)
                            startValidationToast.show()
                        } else {
                            startValidationToast.cancel()
                            val selectedSettings = settings
                            isPreparing = true
                            scope.launch {
                                try {
                                    val content = prepareQuiz(context, selectedSettings)
                                    if (content == null) showInsufficientContent = true
                                    else preparedQuiz = content
                                } finally {
                                    isPreparing = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp + QuizActionButtonHeight*2),
                    enabled = !isPreparing
                )
            }
        }
    }
}

@Composable
private fun QuizSettingsSummary(settings: QuizSettings, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val videoCount by produceState<Int?>(
        initialValue = null, key1 = settings.selectedBeltGups, key2 = settings.techniquesEnabled
    ) {
        value = if (settings.techniquesEnabled) loadTechniqueQuizPool(context, settings.selectedBeltGups).size else 0
    }
    val bodyPartCount by produceState<Int?>(initialValue = null, key1 = settings.bodyPartsEnabled) {
        value = if (settings.bodyPartsEnabled) {
            withContext(Dispatchers.IO) { DbProvider.get(context).bodyPartDao().getAllNow().size }
        } else 0
    }
    Column(modifier) {
        Text("Technique videos: ${videoCount?.toString() ?: "..."}")
        Text("Body parts: ${bodyPartCount?.toString() ?: "..."}")
        Text("Review time: ${settings.studySeconds} seconds")
    }
}

@Composable
private fun QuizCategoryCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.toggleable(
            value = checked,
            role = Role.Checkbox,
            onValueChange = onCheckedChange
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}
