package ba.rma.myapplication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ba.rma.myapplication.data.ThemeMode
import ba.rma.myapplication.vm.SettingsViewModel

/**
 * Settings screen — lets the user configure how the app looks and how the album is displayed.
 *
 * Each setting reads its current value from [SettingsViewModel] (Compose state) and writes back
 * through the ViewModel's setter functions, which persist to SharedPreferences immediately.
 *
 * [onThemeChanged] is called whenever the theme changes so MainActivity can re-theme the whole app
 * live (without restarting).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onThemeChanged: (ThemeMode) -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Postavke") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Nazad"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ---- THEME (Svijetla / Tamna / Sistem) ----
            SettingsSection(title = "Tema") {
                // We list all theme options and render a radio button for each.
                val themeOptions = listOf(
                    ThemeMode.LIGHT to "Svijetla",
                    ThemeMode.DARK to "Tamna",
                    ThemeMode.SYSTEM to "Sistem"
                )
                themeOptions.forEach { (mode, label) ->
                    RadioRow(
                        text = label,
                        selected = viewModel.themeMode == mode,
                        onSelect = {
                            // Persist + update state, then tell MainActivity so the app re-themes live.
                            viewModel.updateThemeMode(mode)
                            onThemeChanged(mode)
                        }
                    )
                }
            }
        }
    }
}

/**
 * A titled card grouping a set of related settings. Keeps the screen tidy and readable.
 */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

/**
 * A single selectable row with a radio button and a label. The whole row is clickable.
 */
@Composable
private fun RadioRow(
    text: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(modifier = Modifier.height(0.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
