package ba.rma.myapplication.sensors

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * SHAKE-TO-OPEN detector for feature (B).
 *
 * Listens to the device accelerometer and decides when the user has "shaken" the phone
 * hard enough to open a pack of stickers. When that happens it calls [onShake] exactly once
 * per shake (a cooldown prevents one physical shake from firing dozens of times).
 *
 * WHY a dedicated class instead of putting this logic inside the Activity/Composable:
 *  - The sensor math and the "is this a real shake" decision are kept in one readable place.
 *  - The class knows nothing about Compose or pack-opening; it just reports "the phone shook".
 *    Whoever creates the detector decides what to do (the [onShake] callback). This keeps the
 *    sensor code reusable and easy to test/reason about.
 *
 * IMPORTANT lifecycle note: this class does NOT register itself with the system. The owner
 * (our Compose wiring) is responsible for calling registerListener / unregisterListener at the
 * right times. We only expose [register] and [unregister] helpers so the owner does not have to
 * remember the exact SensorManager arguments.
 *
 * @param sensorManager the system SensorManager (get it with context.getSystemService).
 * @param onShake called on the MAIN logic flow whenever a valid shake is detected.
 *                NOTE: sensor callbacks arrive on a system/background thread, so the caller of
 *                onShake must be careful about touching UI state. See ShakeWiring.kt for how we
 *                hop back onto Compose state safely.
 */
class ShakeDetector(
    private val sensorManager: SensorManager,
    private val onShake: () -> Unit
) : SensorEventListener {

    // The accelerometer sensor, or null if this device has none (common on emulators).
    // We look it up once and keep it. getDefaultSensor returns null when the sensor is missing.
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // Timestamp (in milliseconds) of the last shake we ACCEPTED. Used for the cooldown so that
    // a single real-world shake, which produces many sensor events, only triggers onShake once.
    private var lastShakeTimeMs: Long = 0L

    companion object {
        // --- Tuning constants. These are chosen for a normal "give the phone a firm shake" gesture. ---

        // How strong the movement must be before we call it a shake.
        // This is a "g-force" multiplier: 1.0 would be just gravity (phone sitting still),
        // so a threshold above 1.0 means the phone must accelerate noticeably more than gravity.
        // 2.7 is a common, comfortable value: gentle hand movement (~1.3) is ignored, but a
        // deliberate shake easily passes it. Lower it if testers say shaking feels too hard.
        private const val SHAKE_THRESHOLD_G = 2.7f

        // Cooldown window in milliseconds. After we accept a shake we ignore further shakes for
        // this long. One physical shake lasts a few hundred ms and fires many sensor events;
        // 1000 ms guarantees the user gets exactly one pack per shake and cannot accidentally
        // burn through packs (and our API is rate-limited to 100 req/hour, so spamming is bad).
        private const val SHAKE_COOLDOWN_MS = 1000L
    }

    /**
     * True only if the hardware actually has an accelerometer. The UI uses this to decide whether
     * to show the manual "Open pack" fallback button (emulators with no sensor still need to be
     * testable). We expose it as a function rather than a property so the call site reads clearly.
     */
    fun hasAccelerometer(): Boolean {
        return accelerometer != null
    }

    /**
     * Start listening. Safe to call even if there is no accelerometer (it simply does nothing).
     *
     * We use SENSOR_DELAY_GAME: it gives frequent updates (good for catching a quick shake)
     * without the very high firehose rate of SENSOR_DELAY_FASTEST, which would waste battery and
     * give us more events than we need.
     */
    fun register() {
        // If the device has no accelerometer, there is nothing to register. Guarding here means
        // the rest of the code does not have to null-check everywhere.
        if (accelerometer == null) {
            return
        }
        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    /**
     * Stop listening. ALWAYS call this when the screen goes away, otherwise the listener leaks
     * and keeps the accelerometer (and our object) alive in the background, draining battery.
     */
    fun unregister() {
        // Passing 'this' unregisters this specific listener from all sensors it was attached to.
        // Calling it when we were never registered is harmless.
        sensorManager.unregisterListener(this)
    }

    /**
     * Called by the system every time the accelerometer reports new values. This runs on a
     * sensor/background thread, NOT the UI thread, so we only do quick math here and never touch
     * Compose state directly.
     */
    override fun onSensorChanged(event: SensorEvent) {
        // Defensive: make sure this really is an accelerometer event before reading the values.
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) {
            return
        }

        // The accelerometer reports acceleration along the three device axes, in m/s^2.
        // values[0] = X (left/right), values[1] = Y (up/down), values[2] = Z (front/back).
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Divide each axis by Earth's gravity so our numbers are in "g" units (multiples of
        // gravity) instead of raw m/s^2. This makes the threshold easy to reason about: when the
        // phone is still, gNet below comes out to about 1.0 (just gravity pulling down).
        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        // Combine the three axes into a single overall force using the 3D length (Pythagoras in
        // three dimensions). This is direction-independent: it does not matter WHICH way the user
        // shakes, only how hard. At rest this length is ~1.0 (the constant pull of gravity).
        val gForce =
            Math.sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

        // A real shake briefly pushes gForce well above the resting 1.0. If we are over the
        // threshold, it MIGHT be a shake -- but we still have to respect the cooldown.
        if (gForce > SHAKE_THRESHOLD_G) {
            val now = System.currentTimeMillis()

            // Cooldown / debounce: ignore this event if we already accepted a shake very recently.
            // Without this guard, a single shake (which crosses the threshold many times) would
            // call onShake repeatedly and open many packs at once.
            if (now - lastShakeTimeMs < SHAKE_COOLDOWN_MS) {
                return
            }

            // Accept the shake: remember when it happened so the next ones are debounced, then
            // notify the owner. Remember: this fires on a background thread.
            lastShakeTimeMs = now
            onShake()
        }
    }

    /**
     * Required by SensorEventListener. We do not care about accuracy changes for a shake gesture,
     * so we leave it empty (but must still provide the method).
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Intentionally empty: accuracy does not affect our simple shake detection.
    }
}
