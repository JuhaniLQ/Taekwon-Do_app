package fi.tkd.itfun

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import fi.tkd.itfun.ui.theme.Blue
import kotlin.math.max

@Composable
fun Genre(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .widthIn(max = 155.dp)

            .clip(RoundedCornerShape(30))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.darker() else MaterialTheme.colorScheme.primary)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ){
        Text(
            modifier = Modifier
                .padding(10.dp)
                .clipToBounds() ,
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
                if (isSelected) MaterialTheme.colorScheme.primary.darker()
                else MaterialTheme.colorScheme.primary
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(imageResId),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(
                if (isSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.surface
            ),
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
                color = MaterialTheme.colorScheme.onPrimary,
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
            color = MaterialTheme.colorScheme.surface,
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
                        .background(Color.Black.copy(alpha = 0.3f))
                )
            }
        }
    }
}

@Composable
fun SearchBar(
    searchInput: String,
    onInputChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    resultSummary: String = ""
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
            color = Blue
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
                            color = Blue.copy(alpha = 0.9f)
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
                            .background(Blue.copy(alpha = 0.15f))
                            .clickable { onInputChange("") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "X",
                            fontSize = 18.sp,
                            color = Blue
                        )
                    }
                }
                if (resultSummary.isNotEmpty()) {
                    Text(
                        text = resultSummary,
                        fontSize = 13.sp,
                        lineHeight = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    )
}
