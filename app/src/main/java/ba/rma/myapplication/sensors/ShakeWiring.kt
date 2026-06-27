package ba.rma.myapplication.sensors

import android.content.Context
import android.hardware.SensorManager
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/**
 * Compose wiring for SHAKE-TO-OPEN.
 *
 * This file shows the recommended college-level way to connect the [ShakeDetector] (which knows
 * about Android sensors) to a Compose screen (which knows about UI). It deliberately keeps two
 * concerns separate:
 *   1. ShakeToOpen()      -- the "plumbing": registers/unregisters the sensor for the screen's
 *                            lifetime and calls back when a shake happens.
 *   2. OpenPackFallback() -- a plain button so the feature is testable on an emulator with no
 *                            accelerometer (and as a convenience on real devices too).
 */

/**
 * Registers a [ShakeDetector] for as long as this composable is on screen, and invokes [onShake]
 * (on the main thread) whenever the phone is shaken.
 *
 * HOW THE LIFECYCLE WORKS (this is the important part):
 *  - DisposableEffect runs its block when the composable first enters the composition, and runs
 *    onDispose when it leaves. That is exactly "screen appeared" / "screen went away", which is
 *    when we want to start and stop listening to the sensor. This is the clean college-level
 *    pattern -- no manual Activity onResume/onPause juggling needed.
 *  - We pass a constant key (Unit) to DisposableEffect so it does NOT re-run on every
 *    recomposition. If we used a changing key, Compose would unregister and re-register the
 *    sensor constantly, which is the classic "leaks / re-registers on recomposition" bug.
 *  - onDispose calls unregister(), so the listener can never leak.
 *
 * THREADING:
 *  - The detector's callback fires on a background sensor thread. We must not touch Compose
 *    state from there. We solve this simply: onShake here should ultimately push work onto the
 *    ViewModel (e.g. viewModel.openPackFromShake()), and the ViewModel uses viewModelScope.launch
 *    to do the network/Room work and update mutableStateOf on the main dispatcher. So this
 *    composable just forwards the event; it does not do heavy work on the sensor thread.
 *
 * RECOMPOSITION SAFETY for the callback:
 *  - The screen may recompose and hand us a NEW lambda each time. We do NOT want to tear down the
 *    sensor just because the lambda's identity changed. rememberUpdatedState keeps a stable holder
 *    whose .value is always the latest onShake, while the DisposableEffect itself stays alive.
 *
 * @param onShake invoked once per detected shake. Keep it lightweight (just kick off the
 *                ViewModel call). The caller is responsible for the isOpening guard (see below).
 */
@Composable
fun ShakeToOpen(onShake: () -> Unit) {
    val context: Context = LocalContext.current

    // Always read the freshest onShake, even across recompositions, without restarting the effect.
    val currentOnShake = rememberUpdatedState(onShake)

    // Build the detector ONCE and keep it across recompositions with remember.
    // We capture currentOnShake.value inside the lambda so the detector always calls the latest one.
    val detector = remember {
        val sensorManager =
            context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        ShakeDetector(
            sensorManager = sensorManager,
            onShake = {
                // This runs on the sensor (background) thread. We only forward to the latest
                // callback; the real work happens in the ViewModel on the main dispatcher.
                currentOnShake.value.invoke()
            }
        )
    }

    // Register when the screen appears, unregister when it disappears. Unit key => runs once.
    DisposableEffect(Unit) {
        detector.register()
        onDispose {
            detector.unregister()
        }
    }
}

/**
 * Visible fallback so the feature can be used/tested WITHOUT shaking -- essential on emulators,
 * which usually have no accelerometer, and handy for quick manual testing on real devices.
 *
 * Pass the SAME action you give to ShakeToOpen's onShake so both paths open a pack identically.
 *
 * @param onOpenClick   what to do when the button is tapped (same as the shake action).
 * @param isOpening     true while a pack request is already in flight. We DISABLE the button so
 *                      the user cannot fire a second request on top of the first. The shake path
 *                      must enforce the same guard (the ViewModel should ignore a shake when
 *                      isOpening is already true) -- this protects the 100 req/hour rate limit.
 * @param alwaysShow    if true the button is shown even on devices that have an accelerometer.
 *                      Set this from ShakeDetector.hasAccelerometer() at the call site: show the
 *                      button when there is NO accelerometer, and optionally always.
 */
@Composable
fun OpenPackFallback(
    onOpenClick: () -> Unit,
    isOpening: Boolean,
    alwaysShow: Boolean
) {
    // We keep the visibility decision at the call site (it knows hasAccelerometer); here we just
    // render the button. The alwaysShow flag lets the caller force it on.
    if (alwaysShow) {
        Button(
            onClick = onOpenClick,
            // Disable while a request is in flight so we cannot double-open. This is the UI half
            // of the "shaking while a pack request is in flight" guard.
            enabled = !isOpening
        ) {
            // Give the user feedback about the in-flight state instead of a dead-looking button.
            // Text is in Bosnian to match the rest of the app's UI.
            if (isOpening) {
                Text(text = "Otvaram...")
            } else {
                Text(text = "Otvori paket")
            }
        }
    }
}

/*
 * ---------------------------------------------------------------------------------------------
 * EXAMPLE call site (for reference -- belongs in your pack screen, not compiled here).
 * Shows how the two pieces fit together with the isOpening guard living in the ViewModel.
 *
 *   @Composable
 *   fun PackScreen(viewModel: PackViewModel) {
 *       // Decide whether to show the manual button. We need a detector instance just to ask
 *       // hasAccelerometer(); creating a throwaway one for the check is fine and cheap, but the
 *       // cleaner approach is to expose hasAccelerometer from ShakeToOpen. For college-level
 *       // simplicity we just always show the fallback button too:
 *       val showButton = true   // or: !detector.hasAccelerometer()
 *
 *       // Wire the shake. The ViewModel decides whether to actually open (it ignores the event
 *       // if a request is already in flight -- the single source of truth for the guard).
 *       ShakeToOpen(onShake = { viewModel.openPackFromShake() })
 *
 *       // Manual fallback uses the exact same action and the same isOpening flag.
 *       OpenPackFallback(
 *           onOpenClick = { viewModel.openPackFromShake() },
 *           isOpening = viewModel.isOpening.value,
 *           alwaysShow = showButton
 *       )
 *   }
 *
 * And in the ViewModel the in-flight guard (so a shake mid-request is ignored):
 *
 *   var isOpening = mutableStateOf(false)
 *       private set
 *
 *   fun openPackFromShake() {
 *       if (isOpening.value) return            // already opening -> ignore extra shakes/taps
 *       isOpening.value = true
 *       viewModelScope.launch {
 *           try {
 *               // call /api/random-players/{count}, add results to Room counts...
 *           } finally {
 *               isOpening.value = false        // always clear, even on error
 *           }
 *       }
 *   }
 * ---------------------------------------------------------------------------------------------
 */
