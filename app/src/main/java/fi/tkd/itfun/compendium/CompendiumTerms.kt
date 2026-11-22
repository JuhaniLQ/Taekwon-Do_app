package fi.tkd.itfun.compendium

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.json.JSONArray

internal fun parseCompendiumTerms(termsJson: String): List<Pair<String, String>> {
    val array = JSONArray(termsJson)
    return List(array.length()) { index ->
        val term = array.getJSONObject(index)
        term.get("en").toString() to term.getString("ko")
    }
}

@Composable
internal fun CompendiumTerms(termsJson: String) {
    val terms = remember(termsJson) { parseCompendiumTerms(termsJson) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        terms.forEach { (english, korean) ->
            Row {
                Text(english, modifier = Modifier.width(48.dp), textAlign = TextAlign.End)
                Spacer(Modifier.width(16.dp))
                Text(korean)
            }
        }
    }
}
