package fi.tkd.itfun.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import fi.tkd.itfun.ui.theme.Blue
import fi.tkd.itfun.R

@Composable
fun Genre(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .widthIn(max = 155.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else Color.Transparent
            )
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(15.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ){
        Text(
            modifier = Modifier
                .padding(start = 5.dp, end = 5.dp)
                .clipToBounds(),
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
fun ImageFilterButton(
    imageResId: Int,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(30))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else Color.Transparent
            )
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(30))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(imageResId),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.fillMaxSize().padding(6.dp)
        )
    }
}

@Composable
fun Genrecopy(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .widthIn(max = 155.dp)

            .clip(RoundedCornerShape(30))
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(13.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ){
        Text(
            modifier = Modifier
                .padding(5.dp)
                .clipToBounds() ,
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 22.sp,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

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
            .clip(CircleShape)
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
                painter = painterResource(images[categoryName] ?: R.drawable.questionmark),
                contentDescription = categoryName,
                modifier = Modifier.fillMaxSize()
            )

            // Black wash overlay when NOT selected
            if (!isSelected) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.opacity(40f))
                )
            }
        }
    }
}

@Composable
fun SearchBar(
    searchInput: TextFieldState,
    modifier: Modifier = Modifier,
    resultCounts: List<Pair<String, Int>> = emptyList(),
    totalItems: Int = 0,
    filters: @Composable () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val keepScrollInSearch = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource) =
                Offset(available.x, 0f)

            override suspend fun onPostFling(consumed: Velocity, available: Velocity) =
                Velocity(available.x, 0f)
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.onSurface.opacity(10f))
            .padding(horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            state = searchInput,
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            onKeyboardAction = { focusManager.clearFocus() },
            textStyle = LocalTextStyle.current.copy(
                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                color = Blue
            ),
            modifier = Modifier.weight(1f).nestedScroll(keepScrollInSearch),
            decorator = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (searchInput.text.isEmpty()) {
                        Text(
                            "Search",
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                            color = Blue
                        )
                    }
                    innerTextField()
                }
            }
        )

        // Clear button (X)
        if (searchInput.text.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Blue.opacity(10f))
                    .clickable { searchInput.clearText() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "X",
                    fontSize = 18.sp,
                    color = Blue
                )
            }
        }
        filters()
        if (resultCounts.isNotEmpty()) {
            TechniqueResultCounter(resultCounts, totalItems)
        }
    }
}

@Composable
private fun TechniqueResultCounter(counts: List<Pair<String, Int>>, totalItems: Int) {
    val style = LocalTextStyle.current.copy(
        fontSize = 13.sp,
        lineHeight = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val gap = with(density) { measurer.measure(" ", style).size.width.toDp() }
    val numberWidth = with(density) {
        measurer.measure("0".repeat(maxOf(2, totalItems.toString().length)), style).size.width.toDp()
    }
    Row(modifier = Modifier.padding(start = 1.dp)) {
        Text(
            text = counts.joinToString("\n") { it.first },
            style = style,
            textAlign = TextAlign.Start,
            softWrap = false,
            maxLines = counts.size
        )
        Spacer(Modifier.width(gap))
        Text(
            text = counts.joinToString("\n") { it.second.toString() },
            modifier = Modifier.width(numberWidth),
            style = style,
            textAlign = TextAlign.Start,
            softWrap = false,
            maxLines = counts.size
        )
    }
}
