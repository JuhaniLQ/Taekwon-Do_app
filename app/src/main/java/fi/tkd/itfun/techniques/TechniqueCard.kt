package fi.tkd.itfun.techniques

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import fi.tkd.itfun.data.db.DbProvider
import fi.tkd.itfun.data.db.PatternVideoDetailsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import fi.tkd.itfun.R
import fi.tkd.itfun.shared.Genrecopy
import fi.tkd.itfun.shared.opacity

@Composable
internal fun Technique(
    tkdItem: TKDItem,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOpenCompendiumEntry: (String) -> Unit,
    modifier: Modifier = Modifier,
    isHovering: Boolean = false,
    onHoverClick: () -> Unit = {},
    playbackSpeed: Float = 1f
) {
    val isPattern = tkdItem.category == TKDItemCategory.ALL_PATTERNS ||
        tkdItem.category == TKDItemCategory.SPARRING

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface

        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary
        ),
        modifier = modifier
            .padding(horizontal = 8.dp)
            .padding(vertical = if (isHovering) 0.dp else 1.dp)
            .then(
                if (!isPattern) {
                    Modifier.clickable {
                        onExpandedChange(!expanded)
                    }
                } else {
                    Modifier
                }
            )
    ) {
        if (!isPattern) {
            CardContent(
                tkdItem,
                expanded,
                onExpandToggle = { onExpandedChange(!expanded) },
                isHovering = isHovering,
                onHoverClick = onHoverClick,
                playbackSpeed = playbackSpeed
            )
        } else {
            CardContent_pattern(
                tkdItem,
                onOpenCompendiumEntry,
                playbackSpeed
            )
        }
    }
}

@Composable
private fun CardContent(
    tkdItem: TKDItem,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    isHovering: Boolean,
    onHoverClick: () -> Unit,
    playbackSpeed: Float
) {
    Column {
        Row(
            modifier = Modifier
                .padding(start=12.dp, top=6.dp, end=6.dp, bottom=6.dp)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val isSectionIcon = tkdItem.category == TKDItemCategory.SPECIAL ||
                    tkdItem.category == TKDItemCategory.CRUSHING
                Image(
                    painter = painterResource(techniqueCategoryImages[tkdItem.category] ?: R.drawable.questionmark),
                    contentDescription = "dude",
                    colorFilter = if (isSectionIcon) {
                        ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                    } else null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .then(
                            if (isSectionIcon) Modifier.background(MaterialTheme.colorScheme.surface)
                            else Modifier.border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier.size(32.dp)
                        .clip(RoundedCornerShape(6.5.dp))
                        .then(
                            if (isHovering) Modifier.background(MaterialTheme.colorScheme.primary)
                            else Modifier
                        )
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.5.dp))
                        .clickable(onClick = onHoverClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (isHovering) R.drawable.push_pin_filled
                            else R.drawable.push_pin_outline
                        ),
                        contentDescription = if (isHovering) "Return to list" else "Hover technique",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(text = tkdItem.name, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = tkdItem.koreanName,
                    minLines = 2,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp
                )
            }
            Box(modifier = Modifier.size(48.dp)) {
                Icon(
                    painter = painterResource(
                        if (expanded) R.drawable.expand_less
                        else R.drawable.expand_more
                    ),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

        }

        if (expanded) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 1.dp,
                modifier = Modifier.padding(1.dp)
            ) {
                val hasVideo =
                    !tkdItem.localVideoPath.isNullOrBlank() &&
                            File(tkdItem.localVideoPath).exists()

                if (!hasVideo) {
                    Image(
                        painter = painterResource(id = R.drawable.questionmark),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                        contentScale = ContentScale.FillBounds
                    )
                } else {

                    TechniqueVideo(
                        playbackSpeed = playbackSpeed,
                        localVideoPath = tkdItem.localVideoPath!!,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

            }
        }
    }
}

@Composable
private fun CardContent_pattern(
    tkdItem: TKDItem,
    onOpenCompendiumEntry: (String) -> Unit,
    playbackSpeed: Float
) {
    Column {
        Row(
            modifier = Modifier
                .padding(start=12.dp, top=4.dp, end=12.dp, bottom = 4.dp)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = tkdItem.koreanName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp
                )
                Text(text = tkdItem.name, color = MaterialTheme.colorScheme.onSurface)
            }
            if (tkdItem.category == TKDItemCategory.ALL_PATTERNS) Box(
                modifier = Modifier.align(Alignment.CenterVertically)
                    //.size(48.dp)
            ) {
                Genrecopy(
                    text = "Info",
                    onClick = {
                        onOpenCompendiumEntry(tkdItem.videoKey)
                    }
                )
            }
        }

        val context = LocalContext.current
        val db = remember { DbProvider.get(context) }

        var patternDetails by remember(tkdItem.videoKey) {
            mutableStateOf<PatternVideoDetailsEntity?>(null)
        }

        var segmentStartMs by remember(tkdItem.videoKey) { mutableStateOf<Long?>(null) }
        var segmentEndMs by remember(tkdItem.videoKey) { mutableStateOf<Long?>(null) }
        var currentIndex by remember { mutableIntStateOf(0) }
        var playTrigger by remember { mutableIntStateOf(0) }
        val hasVideo = !tkdItem.localVideoPath.isNullOrBlank() &&
                File(tkdItem.localVideoPath).exists()
        val showVideoControls = hasVideo
        Surface(
            shape = MaterialTheme.shapes.medium,
            shadowElevation = 1.dp,
            modifier = Modifier.padding(1.dp)
        ) {

            if (!hasVideo) {
                Image(
                    painter = painterResource(id = R.drawable.questionmark),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    contentScale = ContentScale.FillBounds
                )
            } else {
                TechniqueVideoSegment(
                    playbackSpeed = playbackSpeed,
                    localVideoPath = tkdItem.localVideoPath!!,
                    segmentStartMs = segmentStartMs,
                    segmentEndMs = segmentEndMs,
                    playTrigger = playTrigger,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }


        if (showVideoControls) {
            LaunchedEffect(showVideoControls, tkdItem.videoKey) {
                val details = withContext(Dispatchers.IO) {
                    db.patternVideoDetailsDao().getByVideoKey(tkdItem.videoKey)
                }
                patternDetails = details
            }

            patternDetails?.let { details ->
                val timecodes = remember(details.timecodesJson) {
                    val arr = org.json.JSONArray(details.timecodesJson)
                    List(arr.length()) { idx -> arr.getInt(idx) }   // ms
                }

                val shortDescriptions = remember(details.shortJson) {
                    val arr = org.json.JSONArray(details.shortJson)
                    List(arr.length()) { idx -> arr.getString(idx) }
                }

                val fullDescriptions = remember(details.fullJson) {
                    val arr = org.json.JSONArray(details.fullJson)
                    List(arr.length()) { idx -> arr.getString(idx) }
                }

                val count = timecodes.size

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(NumberWheelDimensions.Height)
                        .padding(horizontal = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // LEFT: wheel
                        NumberWheelPicker(
                            count = count,
                            labels = shortDescriptions,
                            modifier = Modifier
                                .weight(0.5f)
                                .fillMaxHeight(),
                            onCenteredIndexChanged = { idx ->
                                currentIndex = idx

                                segmentStartMs = timecodes.getOrNull(idx)?.toLong()
                                segmentEndMs   = timecodes.getOrNull(idx + 1)?.toLong()

                                playTrigger++
                            },
                            onReplayRequested = { idx ->
                                if (idx == currentIndex) {
                                    playTrigger++
                                }
                            }
                        )


                        Column(
                            modifier = Modifier
                                .weight(0.5f)
                                .fillMaxHeight()
                                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                        ) {
                            val fullText = fullDescriptions.getOrNull(
                                if (fullDescriptions.size == 1) 0 else currentIndex
                            )
                            var expandedFull by remember { mutableStateOf(false) }
                            val hasDiagram = tkdItem.category == TKDItemCategory.ALL_PATTERNS &&
                                tkdItem.pattern.firstOrNull()?.let { TkdPattern.fromString(it) != TkdPattern.UNKNOWN } == true

                            if (!fullText.isNullOrBlank()) {
                                val markerColor = MaterialTheme.colorScheme.primary.opacity()
                                val highlightedText = remember(fullText, markerColor) {
                                    buildAnnotatedString {
                                        append(fullText)
                                        Regex("\\([^)]*\\)").findAll(fullText).forEach { match ->
                                            addStyle(
                                                SpanStyle(background = markerColor),
                                                start = match.range.first,
                                                end = match.range.last + 1
                                            )
                                        }
                                    }
                                }
                                BoxWithConstraints(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    val density = LocalDensity.current
                                    val maxHeightPx = with(density) { maxHeight.toPx() }
                                    var textHeightPx by remember { mutableFloatStateOf(0f) }

                                    val maxImageSizePx = with(density) { 85.dp.toPx() }

                                    Column(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = highlightedText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = if (expandedFull) Int.MAX_VALUE else if (hasDiagram) 11 else 16,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { expandedFull = !expandedFull }
                                                .padding(bottom = 1.dp)
                                                .onGloballyPositioned { coordinates ->
                                                    textHeightPx = coordinates.size.height.toFloat()
                                                }
                                        )
                                        Spacer(modifier = Modifier.weight(1f))

                                        val remainingPx = (maxHeightPx - textHeightPx).coerceAtLeast(0f)
                                        val imageSizePx = remainingPx.coerceAtMost(maxImageSizePx)
                                        val imageSizeDp = with(density) { imageSizePx.toDp() }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (imageSizeDp > 0.dp && hasDiagram) {
                                                TkdPatternDiagram(name = tkdItem.pattern[0], modifier = Modifier.size(imageSizeDp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
