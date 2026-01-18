package fi.tkd.itfun

import android.net.Uri
import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import java.io.File


@Composable
fun Techniques(
               tkdItemsRoom: List<TKDItem>,
               selectedTab: MutableState<String>,
               selectedPattern: MutableState<String>,
               selectedFilters: MutableState<Set<String>>,
               patternStr: String,
               beltGup: Int) {

    val availablePatternNames by remember(tkdItemsRoom, beltGup) {
        derivedStateOf {
            tkdItemsRoom
                .asSequence()
                .filter { it.category == TKDItemCategory.ALL_PATTERNS }
                .mapNotNull { item ->
                    val name = item.pattern.firstOrNull() ?: return@mapNotNull null
                    name to item.beltLevel
                }
                .filter { (_, requiredBelt) -> requiredBelt >= beltGup }
                .map { (name, _) -> name }
                .distinct()
                .sorted()
                .toList()
        }
    }

    val expandedByCode = remember { mutableStateMapOf<Int, Boolean>() }

    val isSubMenuVisible = remember { mutableStateOf(false) }

    val filterCategories = TKDItemCategory.entries
    val searchInput = rememberSaveable { mutableStateOf("") }      // what user types
    val searchQuery = remember { mutableStateOf("") }              // debounced version
    val listState = rememberLazyListState()

    LaunchedEffect(searchInput.value) {
        delay(130)
        searchQuery.value = searchInput.value
    }
    LaunchedEffect(
        searchQuery.value,
        selectedFilters.value,
        selectedPattern.value,
        selectedTab.value,
        beltGup) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(
        selectedFilters.value,
        selectedPattern.value,
        selectedTab.value,
        beltGup
    ) {
        expandedByCode.clear()
    }


    Column {
        // Tabs Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp, top = 5.dp, start = 3.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Genre(
                    text = "All",
                    isSelected = selectedTab.value == "All",
                    onClick = {
                        selectedTab.value = "All"
                        selectedPattern.value = ""
                        isSubMenuVisible.value = false
                    }
                )

                val patternLabel =
                    if (selectedPattern.value.isEmpty()) patternStr else selectedPattern.value

                // Pattern button: ONLY opens submenu, does NOT change selectedTab
                Genre(
                    text = patternLabel,
                    isSelected = selectedTab.value == patternStr,  // true only AFTER submenu selection
                    onClick = {
                        isSubMenuVisible.value = true              // just open submenu
                    }
                )
            }

            // pushes filters to the right edge
            Spacer(modifier = Modifier.weight(1f))

            // filter icons
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
            ) {
                items(filterCategories) { category ->
                    CategoryFilterImage(
                        categoryName = category.name,
                        images = images.mapKeys { it.key.name },
                        isSelected = selectedFilters.value.contains(category.name),
                        onClick = {
                            selectedFilters.value =
                                if (selectedFilters.value.contains(category.name)) {
                                    selectedFilters.value - category.name
                                } else {
                                    selectedFilters.value + category.name
                                }
                        },
                    )
                }
            }
        }

        Card(
            modifier = Modifier.padding(start=50.dp)
        ) {
            MaterialTheme(
                shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))
            ) {
                DropdownMenu(
                    expanded = isSubMenuVisible.value,
                    onDismissRequest = { isSubMenuVisible.value = false },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.darker(30f))
                ) {
                    availablePatternNames.forEach { pattern ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = pattern,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 22.sp
                                )
                            },
                            onClick = {
                                selectedPattern.value = pattern
                                isSubMenuVisible.value = false
                                // reset filters so the user sees the full pattern list first
                                selectedFilters.value = emptySet()
                                selectedTab.value = patternStr
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                        )

                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 5.dp, top = 5.dp, start = 15.dp, end = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchBar(
                searchInput = searchInput.value,
                onInputChange = { searchInput.value = it }
            )
        }



// derive filtered list ONCE
        val currentTab = selectedTab.value
        val currentPattern = selectedPattern.value
        val activeFilters = selectedFilters.value

        val filteredItems by remember(
            tkdItemsRoom,
            currentTab,
            currentPattern,
            activeFilters,
            searchQuery.value,
            beltGup
        ) {
            derivedStateOf {
                val q = searchQuery.value.trim().lowercase()
                tkdItemsRoom.filter { tkdItem ->
                    val matchesTab = when (currentTab) {
                        "All" -> true
                        patternStr ->
                            currentPattern.isEmpty() ||
                                    tkdItem.pattern.contains(currentPattern)
                        else -> true
                    }

                    val matchesCategory =
                        activeFilters.isEmpty() || activeFilters.contains(tkdItem.category.name)

                    val matchesSearch =
                        q.isEmpty() ||
                                tkdItem.name.lowercase().contains(q) ||          // English name
                                tkdItem.koreanName.lowercase().contains(q)       // Korean name

                    val matchesBelt = tkdItem.beltLevel >= beltGup

                    matchesTab && matchesCategory && matchesSearch && matchesBelt
                }
            }
        }

        LazyColumn(state = listState,
            modifier = Modifier.padding(bottom = 8.dp)) {
            items(
                items = filteredItems,
                key = { it.techniqueCode }
            ) { tkdItem ->
                val expanded = expandedByCode[tkdItem.techniqueCode] ?: false
                Technique(
                    tkdItem = tkdItem,
                    expanded = expanded,
                    onExpandedChange = { newValue ->
                        expandedByCode[tkdItem.techniqueCode] = newValue
                    }
                )
            }
        }
    }
}

fun Color.darker(percent: Float): Color {
    return lerp(this, Color.Black, percent / 100)
}

@Composable
fun Genre(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .widthIn(max = 125.dp)

            .clip(RoundedCornerShape(30))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.darker(30f) else MaterialTheme.colorScheme.primary)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ){
        Text(
            modifier = Modifier.padding(10.dp).clipToBounds() ,
            text = text,
            color = if(isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
private fun Technique(
    tkdItem: TKDItem,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        modifier = modifier
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clickable { onExpandedChange(!expanded) }
    ) {
        CardContent(tkdItem, expanded, onExpandToggle = { onExpandedChange(!expanded) })
    }
}


data class TKDItem(
    val techniqueCode: Int,
    val name: String,
    val koreanName: String,
    val category: TKDItemCategory,
    val pattern: List<String>,
    val beltLevel: Int,
    val videoKey: String,
    val localVideoPath: String?
)

enum class TKDItemCategory {
    DEFENSIVE,
    OFFENSIVE,
    KICKS,
    ALL_PATTERNS,
    STANCES
}

private val images = mapOf(
    TKDItemCategory.KICKS to R.drawable.kicks,
    TKDItemCategory.OFFENSIVE to R.drawable.offensive,
    TKDItemCategory.DEFENSIVE to R.drawable.defensive,
    TKDItemCategory.ALL_PATTERNS to R.drawable.pattern,
    TKDItemCategory.STANCES to R.drawable.stances
)


@Composable
fun CategoryFilterImage(
    categoryName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    images: Map<String, Int>
) {
    val ringWidth = 3.dp

    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick)
    ) {

        // Outer ring (only when selected)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(
                        width = ringWidth,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
        }

        // Inner circle (image)
        val innerPadding = if (isSelected) ringWidth else 0.dp

        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(innerPadding)
                .clip(CircleShape)
        ) {
            Image(
                painter = painterResource(images[categoryName] ?: R.drawable.guy),
                contentDescription = categoryName,
                modifier = Modifier.fillMaxSize()
            )

            // White wash overlay when NOT selected
            if (!isSelected) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.White.copy(alpha = 0.3f))
                )
            }
        }
    }
}


@Composable
private fun CardContent(
    tkdItem: TKDItem,
    expanded: Boolean,
    onExpandToggle: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
        ) {
            Image(
                painter = painterResource(images[tkdItem.category] ?: R.drawable.guy),
                contentDescription = "dude",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )

            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(text = tkdItem.name, color = MaterialTheme.colorScheme.surface)
                Text(
                    text = tkdItem.koreanName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = MaterialTheme.colorScheme.surface,
                    fontSize = 22.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (expanded) R.drawable.expand_less else R.drawable.expand_more
                    ),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center)
                )

            }

        }

        if (expanded) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 1.dp,
                modifier = Modifier.padding(1.dp)
            ) {
                val hasVideo = remember(tkdItem.localVideoPath) {
                    !tkdItem.localVideoPath.isNullOrBlank() &&
                            File(tkdItem.localVideoPath).exists()
                }

                if (!hasVideo) {
                    Image(
                        painter = painterResource(id = R.drawable.tf),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    TechniqueVideo(
                        localVideoPath = tkdItem.localVideoPath!!,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

            }
        }
    }
}

@Composable
fun SearchBar(
    searchInput: String,
    onInputChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    BasicTextField(
        value = searchInput,
        onValueChange = onInputChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = { focusManager.clearFocus() }
        ),

                textStyle = LocalTextStyle.current.copy(
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(horizontal = 16.dp),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchInput.isEmpty()) {
                        Text(
                            "Search",
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                        )
                    }
                    innerTextField()
                }

                // Clear button (X)
                if (searchInput.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                            .clickable { onInputChange("") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "X",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun TechniqueVideo(
    localVideoPath: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val exoPlayer = remember(localVideoPath) {
        ExoPlayer.Builder(context).build().apply {
            val uri = Uri.fromFile(File(localVideoPath))

            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ALL
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = modifier.aspectRatio(16f / 9f),
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                player = exoPlayer

                setOnClickListener {
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                }
            }
        }
    )
}



@Composable
fun GifImage(
    modifier: Modifier = Modifier,
    gifResId: Int
) {
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .components {
            if (Build.VERSION.SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()

    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(gifResId)
            .build(),
        imageLoader = imageLoader
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.78f)
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}
