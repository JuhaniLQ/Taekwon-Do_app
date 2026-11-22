package fi.tkd.itfun.techniques

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import fi.tkd.itfun.shared.rememberEdgeFeedbackModifier

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TechniqueCardList(
    state: LazyListState,
    userScrollEnabled: Boolean,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    overscrollEffect: OverscrollEffect = ScrollableDefaults.overscrollEffect(),
    content: LazyListScope.() -> Unit
) {
    val edgeModifier = if (userScrollEnabled) {
        rememberEdgeFeedbackModifier(overscrollEffect)
    } else {
        Modifier
    }

    // Handle the native effect here so its activation does not depend on the list's height.
    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        LazyColumn(
            state = state,
            userScrollEnabled = userScrollEnabled,
            contentPadding = contentPadding,
            modifier = modifier.then(edgeModifier),
            content = content
        )
    }
}
