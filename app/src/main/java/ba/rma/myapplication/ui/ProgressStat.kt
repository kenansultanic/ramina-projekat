package ba.rma.myapplication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A small reusable progress row: a label on the left, "owned/total" on the right, and a horizontal
 * progress bar underneath. Used on the Suggestions screen for overall and per-team progress.
 *
 * These progress bars are also the app's simple data visualization of the collection's status.
 */
@Composable
fun ProgressStat(
    label: String,
    owned: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    // The bar wants a value between 0.0 and 1.0. Guard against dividing by zero when total is 0.
    val fraction = if (total > 0) {
        owned.toFloat() / total.toFloat()
    } else {
        0f
    }

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = owned.toString() + "/" + total.toString(), style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.height(4.dp))
        // The current Material 3 API takes the value as a lambda: progress = { fraction }.
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
