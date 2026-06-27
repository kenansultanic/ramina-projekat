package ba.rma.myapplication.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * The main screen the user sees after splash/onboarding.
 *
 * It is a classic tabbed layout: a top bar with the current tab's title + a Settings button, and a
 * bottom navigation bar with five tabs. We do NOT use nested navigation here; instead we simply keep
 * the selected tab index in state and swap the content with a when() block. Each tab creates its own
 * ViewModel with the default viewModel() factory, which scopes it to this home destination.
 *
 * @param onOpenDetail called with a player id when the user taps a card that should open detail.
 * @param onOpenSettings called when the user taps the gear icon in the top bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpenDetail: (Int) -> Unit, onOpenSettings: () -> Unit) {

    // Which tab is currently selected. rememberSaveable keeps it across rotation/process death.
    var selected by rememberSaveable { mutableIntStateOf(0) }

    // Bosnian titles for each tab, in the same order as the icons/content below.
    val titles = listOf("Album", "Paketi", "Statistika", "Favoriti", "Prijedlozi")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titles[selected]) },
                actions = {
                    // Gear icon -> open the settings screen.
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Postavke"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selected == 0,
                    onClick = { selected = 0 },
                    icon = { Icon(Icons.Filled.GridView, contentDescription = "Album") },
                    label = { Text("Album") }
                )
                NavigationBarItem(
                    selected = selected == 1,
                    onClick = { selected = 1 },
                    icon = { Icon(Icons.Filled.Casino, contentDescription = "Paketi") },
                    label = { Text("Paketi") }
                )
                NavigationBarItem(
                    selected = selected == 2,
                    onClick = { selected = 2 },
                    icon = { Icon(Icons.Filled.BarChart, contentDescription = "Statistika") },
                    label = { Text("Statistika") }
                )
                NavigationBarItem(
                    selected = selected == 3,
                    onClick = { selected = 3 },
                    icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favoriti") },
                    label = { Text("Favoriti") }
                )
                NavigationBarItem(
                    selected = selected == 4,
                    onClick = { selected = 4 },
                    icon = { Icon(Icons.Filled.Lightbulb, contentDescription = "Prijedlozi") },
                    label = { Text("Prijedlozi") }
                )
            }
        }
    ) { innerPadding ->
        // Swap the body based on the selected tab. We pass the Scaffold's innerPadding so content
        // is not hidden behind the top/bottom bars. Screens that take a Modifier get the padding
        // directly; the others are wrapped in a padded Box.
        when (selected) {
            0 -> AlbumScreen(
                onOpenDetail = onOpenDetail,
                modifier = Modifier.padding(innerPadding)
            )
            1 -> Box(Modifier.padding(innerPadding)) { PackScreen() }
            2 -> Box(Modifier.padding(innerPadding)) { StatsScreen() }
            3 -> Box(Modifier.padding(innerPadding)) { FavoritesScreen(onOpenDetail = onOpenDetail) }
            else -> Box(Modifier.padding(innerPadding)) { SuggestionsScreen() }
        }
    }
}
