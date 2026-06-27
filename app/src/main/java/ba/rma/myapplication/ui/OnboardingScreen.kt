package ba.rma.myapplication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ba.rma.myapplication.vm.OnboardingViewModel

/**
 * First-run onboarding screen.
 *
 * Shows a short welcome, lets the user pick a favorite national team from a list of radio buttons,
 * and offers two ways to move on:
 *  - "Preskoči" (skip): finishes onboarding without a favorite team.
 *  - "Nastavi" (continue): finishes onboarding with the currently selected team.
 *
 * Both buttons call [OnboardingViewModel.finish] and then [onDone] so navigation can move forward.
 *
 * @param onDone called once onboarding is complete (navigates to home).
 * @param viewModel supplied by default through the standard viewModel() factory.
 */
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    // The list of teams to choose from (distinct, sorted). May be empty on the very first launch.
    val teams by viewModel.teams.collectAsState()

    // Which team the user has currently selected. Empty string means "nothing picked yet".
    var selectedTeam by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Welcome header ---
        Text(
            text = "Dobro došli!",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Skupljaj sličice, popuni album i prati svoj napredak. " +
                "Odaberi svoju omiljenu reprezentaciju za početak.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Omiljena reprezentacija",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // --- Team picker (radio buttons) ---
        if (teams.isEmpty()) {
            // Empty/loading state: the catalog has not arrived yet, so there is nothing to pick.
            // The user can still skip and continue later.
            Text(
                text = "Učitavanje reprezentacija... Možeš preskočiti i odabrati kasnije.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            // One selectable row per team. The whole row is clickable for a comfortable tap target.
            for (team in teams) {
                val isSelected = team == selectedTeam
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { selectedTeam = team }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        // Click is handled by the whole Row above, so the button itself is null here.
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = team,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Action buttons ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Skip: finish onboarding without saving any favorite team.
            OutlinedButton(
                onClick = {
                    viewModel.finish("")
                    onDone()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Preskoči")
            }

            // Continue: finish onboarding with the currently selected team (may be empty if none).
            Button(
                onClick = {
                    viewModel.finish(selectedTeam)
                    onDone()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Nastavi")
            }
        }
    }
}
