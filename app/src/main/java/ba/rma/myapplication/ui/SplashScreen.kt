package ba.rma.myapplication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ba.rma.myapplication.data.CardRepository
import ba.rma.myapplication.data.SettingsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The first screen shown on launch.
 *
 * It displays the brand for a short, fixed moment while it tries (best-effort) to refresh the
 * catalog from the server in the background. After a small delay it decides where to go:
 * - if the user has already finished onboarding -> straight to home,
 * - otherwise -> the onboarding flow.
 *
 * @param onGoToOnboarding navigate to the first-run onboarding flow.
 * @param onGoToHome navigate to the main home shell.
 */
@Composable
fun SplashScreen(onGoToOnboarding: () -> Unit, onGoToHome: () -> Unit) {
    val context = LocalContext.current

    // Run once when the splash appears. We kick off a best-effort catalog refresh as a child
    // coroutine (fire-and-forget) so the splash timing does not depend on the network. Then we wait
    // a fixed 1200ms for branding, and finally route based on the saved onboarding flag.
    LaunchedEffect(Unit) {
        // Fire-and-forget: refreshCatalog never throws, so we don't need to guard it.
        launch { CardRepository.get(context).refreshCatalog() }

        delay(1200)

        if (SettingsManager(context).onboardingDone) {
            onGoToHome()
        } else {
            onGoToOnboarding()
        }
    }

    // Simple branded, centered layout.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.primary
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Sličice",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = "Tvoja kolekcija na jednom mjestu",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(top = 32.dp)
                )
            }
        }
    }
}
