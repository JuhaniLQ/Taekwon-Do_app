package fi.tkd.itfun.compendium

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import fi.tkd.itfun.data.db.CompendiumSectionEntity
import fi.tkd.itfun.data.db.DbProvider
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import fi.tkd.itfun.data.db.CompendiumEntryEntity
import fi.tkd.itfun.data.db.CompendiumEntryImageDao
import fi.tkd.itfun.ui.theme.Green
import fi.tkd.itfun.ui.theme.Navy1
import fi.tkd.itfun.ui.theme.Red
import fi.tkd.itfun.ui.theme.Yellow
import kotlinx.coroutines.launch
import fi.tkd.itfun.R
import fi.tkd.itfun.shared.opacity
import fi.tkd.itfun.shared.rememberEdgeFeedbackModifier
import fi.tkd.itfun.techniques.TKDItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Compendium(
    stanceItems: List<TKDItem>,
    patternItems: List<TKDItem>,
    compendiumSections: List<CompendiumSectionEntity>,
    beltGup: Int,
    selectedTabKey: String,
    selectedEntryKey: String?,
    onSelectedTabChange: (String) -> Unit,
    onSelectedEntryChange: (String?) -> Unit,
    expandedEntries: SnapshotStateMap<String, Boolean>
) {
    LaunchedEffect(selectedEntryKey) {
        selectedEntryKey?.let { key ->
            expandedEntries.clear()
            expandedEntries[key] = true
        }
    }

    val availableStances = remember(stanceItems, beltGup) {
        stanceItems.filter { it.beltLevel >= beltGup }
    }

    val availablePatterns = remember(patternItems, beltGup) {
        patternItems.filter { it.beltLevel >= beltGup }
    }

    val selectedDynamicSection = compendiumSections
        .firstOrNull { it.sectionKey == selectedTabKey }

    val context = LocalContext.current
    val db = remember { DbProvider.get(context) }
    val imageDao = db.compendiumEntryImageDao()

    val dynamicEntries by if (selectedDynamicSection != null) {
        db.compendiumEntryDao()
            .getEntriesForSection(selectedDynamicSection.sectionKey)
            .collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    val visibleDynamicEntries = remember(
        dynamicEntries,
        selectedTabKey,
        availableStances,
        availablePatterns
    ) {
        when (selectedTabKey) {
            "stances" -> {
                val allowedKeys = availableStances.map { it.videoKey }.toSet()
                dynamicEntries.filter { it.entryKey in allowedKeys }
            }

            "patterns" -> {
                val allowedKeys = availablePatterns.map { it.videoKey }.toSet()
                dynamicEntries.filter { it.entryKey in allowedKeys }
            }

            else -> dynamicEntries
        }
    }

    val tabs = remember(compendiumSections) {
        listOf(
            CompendiumTab(
                key = "tenets",
                title = "Tenets",
                isStatic = true
            ),
            CompendiumTab(
                key = "oath",
                title = "Oath",
                isStatic = true
            ),
            CompendiumTab(
                key = "beltSystem",
                title = "Belt System",
                isStatic = true
            ),
            CompendiumTab(
                key = "selfDefense",
                title = "Self-defence",
                isStatic = true
            )

        ) + compendiumSections.map { section ->
            CompendiumTab(
                key = section.sectionKey,
                title = section.titleEn
            )
        }
    }

    val selectedTab = tabs.firstOrNull { it.key == selectedTabKey } ?: tabs.first()
    val scrollState = rememberScrollState()
    val edgeModifier = key(selectedTab.key) {
        rememberEdgeFeedbackModifier(ScrollableDefaults.overscrollEffect())
    }
    val scope = rememberCoroutineScope()
    var viewportHeightPx by remember { mutableIntStateOf(0) }
    var contentHeightPx by remember { mutableIntStateOf(0) }
    val entryOffsets = remember { mutableStateMapOf<String, Int>() }

    val targetOffset = selectedEntryKey?.let { entryOffsets[it] }

    LaunchedEffect(selectedEntryKey, targetOffset) {
        if (selectedEntryKey != null && targetOffset != null) {
            scrollState.animateScrollTo(targetOffset)
            onSelectedEntryChange(null)
        }
    }

    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        // LEFT: CONTENT AREA
        Box(
            modifier = Modifier
                .weight(5f)
                .fillMaxHeight()
                .padding(top = 10.dp, bottom = 10.dp, start = 1.dp)

                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(13.dp)
                )
        ) {
            CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .onSizeChanged { viewportHeightPx = it.height }
                        .then(edgeModifier)
                ) {
                    when (selectedTab.key) {

                        "tenets" -> Box(
                            modifier = Modifier.padding(
                                top = 1.dp,
                                bottom = 1.dp,
                                start = 7.dp,
                                end = 15.dp
                            )
                        ) {
                            TenetsContent(
                                scrollState = scrollState,
                                onContentHeightChanged = { contentHeightPx = it }
                            )
                        }

                        "oath" -> Box(
                            modifier = Modifier.padding(
                                top = 1.dp,
                                bottom = 1.dp,
                                start = 7.dp,
                                end = 15.dp
                            )
                        ) {
                            OathContent(
                                scrollState = scrollState,
                                onContentHeightChanged = { contentHeightPx = it }
                            )
                        }

                        "beltSystem" -> Box(
                            modifier = Modifier.padding(
                                top = 1.dp,
                                bottom = 1.dp,
                                start = 7.dp,
                                end = 15.dp
                            )
                        ) {
                            BeltSystemContent(
                                scrollState = scrollState,
                                onContentHeightChanged = { contentHeightPx = it }
                            )
                        }

                        "selfDefense" -> Box(
                            modifier = Modifier.padding(
                                top = 1.dp,
                                bottom = 1.dp,
                                start = 7.dp,
                                end = 15.dp
                            )
                        ) {
                            SelfDefenseContent(
                                scrollState = scrollState,
                                onContentHeightChanged = { contentHeightPx = it }
                            )
                        }

                        else -> {
                            val shouldTintFirstImage = selectedTabKey == "stances"
                            val showEntryTitle = selectedTabKey == "patterns"
                            when {
                                visibleDynamicEntries.size == 1 && visibleDynamicEntries.first().termsJson == null -> {
                                    val entry = visibleDynamicEntries.first()
                                    val entryImages by imageDao
                                        .getImagesForEntry(entry.entryKey)
                                        .collectAsState(initial = emptyList())
                                    CompendiumEntryContent(
                                        title = if (showEntryTitle) {
                                            entry.titleEn ?: selectedTab.title
                                        } else {
                                            selectedTab.title
                                        },
                                        description = entry.descriptionEn,
                                        imagePaths = entryImages.mapNotNull { it.localPath },
                                        scrollState = scrollState,
                                        onContentHeightChanged = { contentHeightPx = it }
                                    )
                                }

                                visibleDynamicEntries.isNotEmpty() -> {
                                    CompendiumEntryListContent(
                                        entries = visibleDynamicEntries,
                                        expandedEntries = expandedEntries,
                                        imageDao = imageDao,
                                        scrollState = scrollState,
                                        onContentHeightChanged = { contentHeightPx = it },
                                        shouldTintFirstImage = shouldTintFirstImage,
                                        entryOffsets = entryOffsets
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (contentHeightPx > viewportHeightPx) {
                VerticalScrollbar(
                    scrollState = scrollState,
                    viewportHeightPx = viewportHeightPx,
                    contentHeightPx = contentHeightPx,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(vertical = 8.dp)
                        .offset(x = (-3).dp)
                        .width(6.dp)
                )
            }
        }

        // RIGHT: TAB COLUMN
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            tabs.forEach { tab ->
                GenreTab(
                    text = tab.title,
                    isSelected = tab.key == selectedTab.key,
                    onClick = {
                        if (tab.key != selectedTabKey) {
                            expandedEntries.clear()
                            onSelectedEntryChange(null)
                            onSelectedTabChange(tab.key)

                            scope.launch {
                                scrollState.scrollTo(0)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun CompendiumEntryListContent(
    entries: List<CompendiumEntryEntity>,
    expandedEntries: SnapshotStateMap<String, Boolean>,
    imageDao: CompendiumEntryImageDao,
    scrollState: ScrollState,
    onContentHeightChanged: (Int) -> Unit,
    shouldTintFirstImage: Boolean,
    entryOffsets: SnapshotStateMap<String, Int>
) {
    var contentCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var pendingScrollEntry by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
        Column(
            modifier = Modifier
                .onGloballyPositioned { contentCoords = it }
                .onSizeChanged { onContentHeightChanged(it.height) }
                .padding(bottom = 100.dp)
        ) {
            entries.forEach { entry ->
                val expanded = expandedEntries[entry.entryKey] ?: false
                var entryHeightPx by remember(entry.entryKey) { mutableIntStateOf(0) }
                var headerHeightPx by remember(entry.entryKey) { mutableIntStateOf(0) }
                Box(
                    modifier = Modifier
                        .padding(top = 1.dp, bottom = 1.dp, start = 3.dp, end = 13.dp)
                        .then(
                            if (expanded) {
                                Modifier.border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary.opacity(50f),
                                    shape = RoundedCornerShape(13.dp)
                                )
                            } else {
                                Modifier
                            }
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(start = 4.dp, end = 2.dp)
                            .onSizeChanged { entryHeightPx = it.height }
                            .onGloballyPositioned { entryCoords ->
                                val parent = contentCoords ?: return@onGloballyPositioned
                                val top = parent.localPositionOf(entryCoords, Offset.Zero).y.toInt()
                                entryOffsets[entry.entryKey] = top
                                if (pendingScrollEntry == entry.entryKey) {
                                    // Use the position after the previous subsection has collapsed.
                                    pendingScrollEntry = null
                                    scope.launch { scrollState.scrollTo(top) }
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(1f)
                                .offset {
                                    val top = entryOffsets[entry.entryKey] ?: 0
                                    val offset = if (expanded) {
                                        (scrollState.value - top).coerceIn(
                                            0,
                                            (entryHeightPx - headerHeightPx).coerceAtLeast(0)
                                        )
                                    } else {
                                        0
                                    }
                                    IntOffset(0, offset)
                                }
                                .onSizeChanged { headerHeightPx = it.height }
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable {
                                    pendingScrollEntry = if (expanded) null else entry.entryKey
                                    expandedEntries.clear()
                                    if (!expanded) {
                                        expandedEntries[entry.entryKey] = true
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = entry.titleEn ?: entry.entryKey,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Bold
                            )

                            Icon(
                                painter = painterResource(
                                    if (expanded) R.drawable.expand_less else R.drawable.expand_more
                                ),
                                contentDescription = null
                            )
                        }

                        if (expanded) {
                            val images by imageDao
                                .getImagesForEntry(entry.entryKey)
                                .collectAsState(initial = emptyList())

                            CompendiumExpandableEntryBody(
                                description = entry.descriptionEn,
                                termsJson = entry.termsJson,
                                imagePaths = images.mapNotNull { it.localPath },
                                shouldTintFirstImage = shouldTintFirstImage
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun CompendiumExpandableEntryBody(
    description: String,
    imagePaths: List<String>,
    shouldTintFirstImage: Boolean,
    termsJson: String? = null
) {
    Column {
        imagePaths.getOrNull(0)?.let { path ->
            CompendiumImage(
                path = path,
                tint = if (shouldTintFirstImage) {
                    if (isSystemInDarkTheme()) Color.White else Color.Black
                } else {
                    null
                }
            )
        }

        if (termsJson != null) CompendiumTerms(termsJson) else Paragraph(description)

        imagePaths.getOrNull(1)?.let { path ->
            CompendiumImage(path)
        }
    }
}

@Composable
fun GenreTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val offsetX = if (isSelected) (-15).dp else (-1).dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        GenreCompendium(
            text = text,
            isSelected = isSelected,
            onClick = onClick,
            modifier = Modifier
                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                .requiredWidth(180.dp)
                .offset(x = offsetX)
        )
    }
}

@Composable
fun GenreCompendium(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else Color.Transparent
            )
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 10.dp),
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TenetsContent(scrollState: ScrollState,
                  modifier: Modifier = Modifier,
                  onContentHeightChanged: (Int) -> Unit) {
    var expandedTenet by remember { mutableStateOf<String?>(null) }
    var pendingScrollTenet by remember { mutableStateOf<String?>(null) }
    var contentCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val scope = rememberCoroutineScope()
    val tenets = listOf(
        TenetSection(
            title = "Courtesy (ye ui)",
            paragraphs = listOf(
                "Taekwon-Do students should attempt to practice the following elements of courtesy to build their noble character and to conduct training in an orderly manner as well."
            ),
            numberedItems = listOf(
                "1. To promote the spirit of mutual concessions.",
                "2. To be ashamed of one's vices and calling out those of others.",
                "3. To be polite to one another.",
                "4. To encourage the sense of justice and humanity.",
                "5. To distinguish instructor from student, senior from junior, and elder from younger.",
                "6. To behave one's self according to the proper etiquette.",
                "7. To respect others' possessions.",
                "8. To handle matters with fairness and sincerity.",
                "9. To refrain from giving or accepting any gift when in doubt."
            )
        ),
        TenetSection(
            title = "Integrity (yom chi)",
            paragraphs = listOf(
                "One must be able to define right and wrong and have the conscience, if wrong, to feel guilt.",
                "Listed are some examples, where integrity is lacking."
            ),
            numberedItems = listOf(
                "1. The instructor who misrepresents himself and his art by presenting improper techniques to his students because of a lack of knowledge or apathy.",
                "2. The student who misrepresents himself by \"fixing\" breaking materials before demonstrations.",
                "3. The instructor who camouflages bad techniques with luxurious training halls and false flattery to his students.",
                "4. The student who requests rank from an instructor, or attempts to purchase it.",
                "5. The student who gains rank for ego purposes or the feeling of power.",
                "6. The instructor who teaches and promotes his art for materialistic gains.",
                "7. The student whose actions do not live up to his/her words.",
                "8. The student who feels ashamed to seek opinions from his juniors."
            )
        ),
        TenetSection(
            title = "Perseverance (in nae)",
            paragraphs = listOf(
                "An old Oriental saying goes, \"Patience leads to virtue or merit. One can make a peaceful home by being patient for 100 times.\"",
                "Certainly, happiness and prosperity will come to the patient person as he strives hard to achieve his goals. Whether it is a higher degree or the perfection of a technique, one must set his goal and constantly persevere to achieve it. Robert Bruce learned his lesson of perseverance from the persistent efforts of a lowly spider. It was this perseverance and tenacity that finally enabled him to free Scotland from the rule of the English king in the fourteenth century.",
                "One of the most important secrets in becoming a leader of Taekwon-Do is to overcome every adversity by perseverance. Confucius once said: \"One who is impatient in trivial matters can seldom achieve success in matters of great importance.\""
            )
        ),
        TenetSection(
            title = "Self-control (guk gi)",
            paragraphs = listOf(
                "This tenet is extremely important inside and outside of the dojang, whether conducting one's self in free sparring or in one's personal affairs. A loss of self-control in free sparring can be disastrous to both student and opponent. An inability to live and work within one's capability or sphere is also a lack of self-control.",
                "According to Lao Tzu, \"The term stronger is the person who wins over one's self rather than someone else.\""
            )
        ),
        TenetSection(
            title = "Indomitable spirit (baekjul boolgool)",
            paragraphs = listOf(
                "\"Here lie 300, who did their duty,\" a simple epitaph for one of the greatest acts of courage known to mankind.",
                "Although facing the superior forces of Xerxes, Leonidas and his 300 Spartans at Thermopylae showed the world the true meaning of indomitable spirit. It is shown when a courageous person and his principles are pitted against overwhelming odds.",
                "A serious student of taekwon-do will at all times be modest and honest. If confronted with injustice he will deal with the belligerent without any fear or hesitation at all, with indomitable spirit, regardless of whoever and whatever the number may be."
            )
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Column(
            modifier = Modifier
                .onSizeChanged { onContentHeightChanged(it.height) }
                .onGloballyPositioned { contentCoords = it }
        ) {
            SectionTitle("Tenets")
            Text(
                text = "Kohteliaisuus (ye ui)\nRehellisyys (yom chi)\nUutteruus (in nae)\nItsehillintä (guk gi)\nLannistumaton henki (baekjul boolgool)",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Paragraph("The success or failure of Taekwon-Do training depends largely on how one observes and implements these Tenets of Taekwon-Do. These should serve as a guide for all serious students of the martial art.")

            tenets.forEach { tenet ->
                val expanded = expandedTenet == tenet.title
                var sectionTopPx by remember(tenet.title) { mutableIntStateOf(0) }
                var sectionHeightPx by remember(tenet.title) { mutableIntStateOf(0) }
                var headerHeightPx by remember(tenet.title) { mutableIntStateOf(0) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (expanded) {
                                Modifier.border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary.opacity(50f),
                                    shape = RoundedCornerShape(13.dp)
                                )
                            } else {
                                Modifier
                            }
                        )
                        .onSizeChanged { sectionHeightPx = it.height }
                        .onGloballyPositioned { sectionCoords ->
                            val parent = contentCoords ?: return@onGloballyPositioned
                            val top = parent.localPositionOf(sectionCoords, Offset.Zero).y.toInt()
                            sectionTopPx = top
                            if (pendingScrollTenet == tenet.title) {
                                pendingScrollTenet = null
                                scope.launch { scrollState.scrollTo(top) }
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(1f)
                            .offset {
                                val offset = if (expanded) {
                                    (scrollState.value - sectionTopPx).coerceIn(
                                        0,
                                        (sectionHeightPx - headerHeightPx).coerceAtLeast(0)
                                    )
                                } else {
                                    0
                                }
                                IntOffset(0, offset)
                            }
                            .onSizeChanged { headerHeightPx = it.height }
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable {
                                expandedTenet = if (expanded) null else tenet.title
                                pendingScrollTenet = expandedTenet
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tenet.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            painter = painterResource(
                                if (expanded) R.drawable.expand_less else R.drawable.expand_more
                            ),
                            contentDescription = if (expanded) "Collapse" else "Expand"
                        )
                    }
                    if (expanded) {
                        tenet.paragraphs.forEach { Paragraph(it) }
                        tenet.numberedItems.forEach { NumberedItem(it) }
                    }
                }
            }
        }
    }
}

private data class TenetSection(
    val title: String,
    val paragraphs: List<String>,
    val numberedItems: List<String> = emptyList()
)

@Composable
fun OathContent(scrollState: ScrollState,
                  modifier: Modifier = Modifier,
                  onContentHeightChanged: (Int) -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Column(
            modifier = Modifier.onSizeChanged {
                onContentHeightChanged(it.height)
            }
        ) {
            SectionTitle("Taekwon-Do Oath")

            Paragraph("Lupaan noudattaa Taekwon-Don pääperiaatteita.")

            Paragraph("Tulen kunnioittamaan opettajaa ja ylempiä vöitä.")

            Paragraph("Lupaan olla käyttämättä Taekwon-Doa väärin.")

            Paragraph("Tulen olemaan puolestapuhuja vapaudelle ja oikeudenmukaisuudelle.")

            Paragraph("Autan rakentamaan rauhallisempaa maailmaa.")

            Spacer(modifier = Modifier.height(30.dp))

            Paragraph("I shall observe the tenets of Taekwon-Do.")

            Paragraph("I shall respect the instructors and seniors.")

            Paragraph("I shall never misuse Taekwon-Do.")

            Paragraph("I shall be a champion of freedom and justice.")

            Paragraph("I shall build a more peaceful world.")

        }
    }
}

@Composable
fun BeltSystemContent(
    scrollState: ScrollState,
    onContentHeightChanged: (Int) -> Unit
) {
    val boxSize = 64.dp
    val cornerShape = RoundedCornerShape(12.dp)
    val borderColor = MaterialTheme.colorScheme.onSurface

    val beltItems = listOf(
        Color.White to "Signifies innocence, as that of a beginning student who has no previous knowledge of Taekwon-Do.",
        Yellow to "Signifies the Earth from which a plant sprouts and takes root as the Taekwon-Do foundation is being laid.",
        Green to "Signifies the plant’s growth as Taekwon-Do skill begins to develop.",
        Navy1 to "Signifies the Heaven toward which the plant matures into a towering tree as training in Taekwon-Do progresses.",
        Red to "Signifies danger, cautioning the student to exercise control and warning the opponent to stay away.",
        Color.Black to "Opposite of white, signifying maturity and proficiency in Taekwon-Do. It also indicates the wearer’s imperviousness to darkness and fear."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged {
                    onContentHeightChanged(it.height)
                },
        ) {
            SectionTitle("Belt System")
            Paragraph(
                "The white dobok symbolizes purity of mind and intention. It represents honesty, discipline, and the principle that a student acts only when necessary and with integrity.\n" +
                        "\n" +
                        "White is also the traditional color of Korean national dress, reflecting Taekwon-Do’s cultural roots.\n" +
                        "\n" +
                        "The “Taekwon-Do” inscription on the back, shaped like a tree, symbolizes growth, life, evolution, and strength.\n" +
                        "\n" +
                        "The ITF emblem on the front represents longevity and the worldwide spirit of Taekwon-Do. Its fist symbolizes martial spirit and strength, while the extended lines reflect harmony and balance.\n"
            )
            beltItems.forEach { (beltColor, text) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(boxSize)
                            .clip(cornerShape)
                            .background(
                                color = beltColor,
                                shape = cornerShape
                            )
                            .border(
                                width = 1.dp,
                                color = borderColor,
                                shape = cornerShape
                            )
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Paragraph(
                        text = text,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Paragraph("The belt is wrapped around the waist for three symbolic reasons:")
            NumberedItem("1. To pursue one goal once it has been determined. (Oh Do Il Kwan)")
            NumberedItem("2. To serve one master with unwavering loyalty. (Il Pyon Dan Shim)")
            NumberedItem("3. To achieve victory with a single decisive blow. (Il Kyok Pil Sung)")
            Paragraph("The belt should be tied firmly, with both ends of equal length, and worn at a height that aligns properly with the dobok side splits.\n" +
                    "\n" +
                    "For master-degree black-border doboks, the belt should follow the border line and form its upper edge.")
        }
    }
}

data class SelfDefenseBeltItem(
    val color: Color,
    val title: String,
    val sections: List<SelfDefenseSection>
)

data class SelfDefenseSection(
    val title: String,
    val items: List<SelfDefenseBullet>
)

data class SelfDefenseBullet(
    val text: String,
    val subItems: List<String> = emptyList()
)

@Composable
fun SelfDefenseContent(
    scrollState: ScrollState,
    onContentHeightChanged: (Int) -> Unit
) {
    var expandedBelt by remember { mutableStateOf<String?>(null) }
    var pendingScrollBelt by remember { mutableStateOf<String?>(null) }
    var contentCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val scope = rememberCoroutineScope()
    val boxSize = 64.dp
    val cornerShape = RoundedCornerShape(12.dp)
    val borderColor = MaterialTheme.colorScheme.onSurface

    val items = listOf(
        SelfDefenseBeltItem(
            color = Color.White,
            title = "White belt 10 gup",
            sections = listOf(
                SelfDefenseSection(
                    title = "Falls and recovery",
                    items = listOf(
                        SelfDefenseBullet("Hard backward fall from squat"),
                        SelfDefenseBullet("Hard forward fall from kneeling"),
                        SelfDefenseBullet("Hard fall to the side"),
                        SelfDefenseBullet("Tactical recovery: both hands, opposite hand and leg, or no hands")
                    )
                ),
                SelfDefenseSection(
                    title = "Escapes",
                    items = listOf(
                        SelfDefenseBullet(
                            text = "One-hand wrist grab",
                            subItems = listOf(
                                "Small step closer, grip own fist, pull free, exit"
                            )
                        ),
                        SelfDefenseBullet(
                            text = "Two-hand grab",
                            subItems = listOf(
                                "Cross hands, step closer, hands toward attacker’s face, exit"
                            )
                        ),
                        SelfDefenseBullet(
                            text = "Handshake",
                            subItems = listOf(
                                "Turn attacker’s thumb outward, knife-hand to thumb joint, exit"
                            )
                        )
                    )
                ),
                SelfDefenseSection(
                    title = "Concepts",
                    items = listOf(
                        SelfDefenseBullet("Avoiding situations"),
                        SelfDefenseBullet("Use of voice, self-defense ready stance, creating distance"),
                        SelfDefenseBullet("Escaping from grabs"),
                        SelfDefenseBullet("Basics of falling"),
                        SelfDefenseBullet("Tactical recovery")
                    )
                )
            )
        ),
        SelfDefenseBeltItem(
            color = Yellow,
            title = "Yellow belt 8 gup",
            sections = listOf(
                SelfDefenseSection(
                    title = "Falls",
                    items = listOf(
                        SelfDefenseBullet("Hard forward breakfall"),
                        SelfDefenseBullet("Hard backward breakfall"),
                        SelfDefenseBullet("Forward rolling ukemi"),
                        SelfDefenseBullet("Backward rolling ukemi")
                    )
                ),
                SelfDefenseSection(
                    title = "Escapes",
                    items = listOf(
                        SelfDefenseBullet(
                            text = "Two-hand chest grab",
                            subItems = listOf(
                                "Softening, simultaneous strike to both hands"
                            )
                        ),
                        SelfDefenseBullet(
                            text = "Two-hand front choke",
                            subItems = listOf(
                                "Softening, control with opposite hand, knifehand-guided takedown"
                            )
                        ),
                        SelfDefenseBullet(
                            text = "Front hair grab",
                            subItems = listOf(
                                "Thumb lock, hand control, takedown"
                            )
                        )
                    )
                ),
                SelfDefenseSection(
                    title = "Concepts",
                    items = listOf(
                        SelfDefenseBullet("Escaping from grabs with counterattack"),
                        SelfDefenseBullet("Breakfalls"),
                        SelfDefenseBullet("Right of self-defence")
                    )
                )
            )
        ),
        SelfDefenseBeltItem(
            color = Green,
            title = "Green belt 6 gup",
            sections = listOf(
                SelfDefenseSection(
                    title = "Defense against strikes and kicks",
                    items = listOf(
                        SelfDefenseBullet(
                            text = "Straight punch",
                            subItems = listOf(
                                "Outside evasion, strike to a vital point"
                            )
                        ),
                        SelfDefenseBullet(
                            text = "Hook punch",
                            subItems = listOf(
                                "Inside block with simultaneous counter-technique (e.g. strike to the neck or face)",
                                "Shoulder control, off-balancing and leg sweep"
                            )
                        ),
                        SelfDefenseBullet(
                            text = "Front kick, turning kick",
                            subItems = listOf(
                                "Evasion, blocking"
                            )
                        )
                    )
                ),
                SelfDefenseSection(
                    title = "Concepts",
                    items = listOf(
                        SelfDefenseBullet("Defending against punches or kicks"),
                        SelfDefenseBullet("Takedown")
                    )
                )
            )
        ),
        SelfDefenseBeltItem(
            color = Navy1,
            title = "Blue belt 4 gup",
            sections = listOf(
                SelfDefenseSection(
                    title = "Ground self-defense",
                    items = listOf(
                        SelfDefenseBullet(
                            text = "Attacker sitting on the stomach",
                            subItems = listOf(
                                "Trap the hands, bridge, establish control"
                            )
                        ),
                        SelfDefenseBullet(
                            text = "Attacker between the legs",
                            subItems = listOf(
                                "Trap the hands and then hip motion (shrimp) upper leg pinned to attacker's stomach and lower leg stays grounded pulling inside, scissor kick and establish control"
                            )
                        ),
                        SelfDefenseBullet(
                            text = "Attacker standing, defender on the ground",
                            subItems = listOf(
                                "Protection against kicks and punches",
                                "Counter-techniques"
                            )
                        )
                    )
                ),
                SelfDefenseSection(
                    title = "Chokeholds (neck locks)",
                    items = listOf(
                        SelfDefenseBullet(
                            text = "Standing, from behind / side",
                            subItems = listOf(
                                "Breath, chin to chest, hand between, soften with strike to a vital point, head release"
                            )
                        )
                    )
                ),
                SelfDefenseSection(
                    title = "Concepts",
                    items = listOf(
                        SelfDefenseBullet("Defending on the ground"),
                        SelfDefenseBullet("Defending against chokeholds")
                    )
                )
            )
        ),
        SelfDefenseBeltItem(
            color = Red,
            title = "Red belt 2 gup",
            sections = listOf(
                SelfDefenseSection(
                    title = "Defense against a knife",
                    items = listOf(
                        SelfDefenseBullet(
                            text = "Stab to the midsection",
                            subItems = listOf(
                                "Guide the attacking hand to the side, secure control with both hands, wrist takedown, establish control"
                            )
                        ),
                        SelfDefenseBullet(
                            text = "Diagonal slash",
                            subItems = listOf(
                                "Evasion, block, gain control of the knife"
                            )
                        ),
                        SelfDefenseBullet(
                            text = "Knife at the side of the neck",
                            subItems = listOf(
                                "Move away from the blade, gain control of the knife hand"
                            )
                        )
                    )
                ),
                SelfDefenseSection(
                    title = "Concepts",
                    items = listOf(
                        SelfDefenseBullet("Defending against a knife"),
                        SelfDefenseBullet("Control techniques")
                    )
                )
            )
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { onContentHeightChanged(it.height) }
                .onGloballyPositioned { contentCoords = it }
        ) {
            SectionTitle("Self-defence")
            Paragraph("Self-defence training in Taekwon-Do prepares the student to respond to different types of assault situations with control, awareness, and appropriate force. The right of self-defence allows a person to protect themselves when necessary, but the response must always be proportionate to the threat. A defensive action should aim to stop the assault and create safety, not to cause unnecessary harm.")
            Paragraph("Unlike techniques and patterns, self-defence methods are not strictly limited by belt rank. However, each belt level introduces progressively more advanced approaches and counters. At minimum, a student should be familiar with the self-defence techniques of their own belt level and all levels below it, building a practical foundation that can be applied responsibly according to the situation.")
            items.forEach { item ->
                val expanded = expandedBelt == item.title
                var sectionTopPx by remember(item.title) { mutableIntStateOf(0) }
                var sectionHeightPx by remember(item.title) { mutableIntStateOf(0) }
                var headerHeightPx by remember(item.title) { mutableIntStateOf(0) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (expanded) {
                                Modifier.border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary.opacity(50f),
                                    shape = RoundedCornerShape(13.dp)
                                )
                            } else {
                                Modifier
                            }
                        )
                        .onSizeChanged { sectionHeightPx = it.height }
                        .onGloballyPositioned { sectionCoords ->
                            val parent = contentCoords ?: return@onGloballyPositioned
                            val top = parent.localPositionOf(sectionCoords, Offset.Zero).y.toInt()
                            sectionTopPx = top
                            if (pendingScrollBelt == item.title) {
                                pendingScrollBelt = null
                                scope.launch { scrollState.scrollTo(top) }
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(1f)
                            .offset {
                                val offset = if (expanded) {
                                    (scrollState.value - sectionTopPx).coerceIn(
                                        0,
                                        (sectionHeightPx - headerHeightPx).coerceAtLeast(0)
                                    )
                                } else {
                                    0
                                }
                                IntOffset(0, offset)
                            }
                            .onSizeChanged { headerHeightPx = it.height }
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable {
                                expandedBelt = if (expanded) null else item.title
                                pendingScrollBelt = expandedBelt
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(boxSize)
                                .clip(cornerShape)
                                .background(item.color, cornerShape)
                                .border(
                                    width = 1.dp,
                                    color = borderColor,
                                    shape = cornerShape
                                )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = item.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            painter = painterResource(
                                if (expanded) R.drawable.expand_less else R.drawable.expand_more
                            ),
                            contentDescription = if (expanded) "Collapse" else "Expand"
                        )
                    }

                    if (expanded) {
                        Spacer(modifier = Modifier.height(10.dp))

                        item.sections.forEach { section ->
                            Text(
                                text = section.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
                            )

                            section.items.forEach { bullet ->
                                BulletItem(bullet.text)

                                bullet.subItems.forEach { subItem ->
                                    SubBulletItem(subItem)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BulletItem(text: String) {
    Text(
        text = "•  $text",
        fontSize = 18.sp,
        lineHeight = 26.sp,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun SubBulletItem(text: String) {
    Text(
        text = "◦  $text",
        fontSize = 17.sp,
        lineHeight = 25.sp,
        modifier = Modifier.padding(start = 20.dp, bottom = 4.dp)
    )
}

data class CompendiumTab(
    val key: String,
    val title: String,
    val isStatic: Boolean = false
)

@Composable
fun CompendiumEntryContent(
    title: String,
    description: String,
    imagePaths: List<String>,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    onContentHeightChanged: (Int) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                top = 1.dp,
                bottom = 1.dp,
                start = 7.dp,
                end = 15.dp)
    ) {
        Column(
            modifier = Modifier.onSizeChanged {
                onContentHeightChanged(it.height)
            }
        ) {
            SectionTitle(title)


            imagePaths.getOrNull(0)?.let { path ->
                CompendiumImage(path)
            }

            Paragraph(description)

            imagePaths.getOrNull(1)?.let { path ->
                CompendiumImage(path)
            }
        }
    }
}

@Composable
fun CompendiumImage(
    path: String,
    tint: Color? = null,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(path) {
        BitmapFactory.decodeFile(path)?.asImageBitmap()
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp)
                .padding(vertical = 12.dp),
            contentScale = ContentScale.Fit,
            colorFilter = tint?.let { ColorFilter.tint(it) }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 18.dp, bottom = 10.dp)
    )
}

@Composable
private fun Paragraph(
    text: String,
    modifier: Modifier = Modifier
) {
    val parsedText = text.replace("\\n\\n", "\n\n")
    val urlRegex = Regex("""https?://\S+""")

    val annotatedText = buildAnnotatedString {
        var lastIndex = 0

        urlRegex.findAll(parsedText).forEach { match ->
            append(parsedText.substring(lastIndex, match.range.first))

            withLink(LinkAnnotation.Url(match.value)) {
                withStyle(
                    SpanStyle(
                        color = Color.Blue,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(match.value)
                }
            }

            lastIndex = match.range.last + 1
        }

        if (lastIndex < parsedText.length) {
            append(parsedText.substring(lastIndex))
        }
    }

    Text(
        text = annotatedText,
        fontSize = 18.sp,
        lineHeight = 28.sp,
        modifier = modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun NumberedItem(text: String) {
    Text(
        text = text,
        fontSize = 18.sp,
        lineHeight = 28.sp,
        modifier = Modifier.padding(start = 18.dp, bottom = 8.dp)
    )
}

@Composable
fun VerticalScrollbar(
    scrollState: ScrollState,
    viewportHeightPx: Int,
    contentHeightPx: Int,
    modifier: Modifier = Modifier
) {
    val isScrolling = scrollState.isScrollInProgress

    val thumbHeightFraction =
        if (contentHeightPx <= viewportHeightPx || contentHeightPx == 0) {
            1f
        } else {
            viewportHeightPx.toFloat() / contentHeightPx.toFloat()
        }

    val maxValue = scrollState.maxValue.coerceAtLeast(1)
    val progress = scrollState.value.toFloat() / maxValue.toFloat()

    Box(
        modifier = modifier.background(
            if (isScrolling)
                MaterialTheme.colorScheme.onSurface.opacity(10f)
            else
                Color.Transparent,
            shape = RoundedCornerShape(3.dp)
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val trackHeightPx = constraints.maxHeight.toFloat()
            val thumbHeightPx = trackHeightPx * thumbHeightFraction
            val maxOffset = trackHeightPx - thumbHeightPx
            val offsetPx = maxOffset * progress

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(LocalDensity.current) { thumbHeightPx.toDp() })
                    .offset { IntOffset(0, offsetPx.toInt()) }
                    .background(
                        MaterialTheme.colorScheme.onSurface.opacity(50f),
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}
