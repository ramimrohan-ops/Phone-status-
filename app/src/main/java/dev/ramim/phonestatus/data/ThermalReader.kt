package dev.ramim.phonestatus.data

import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Reads /sys/class/thermal. Normal apps are blocked from this by SELinux on most phones,
 * so we try `su` first (rooted phone) and fall back to a direct read.
 */
object ThermalReader {

    @Volatile private var rootState = RootState.UNKNOWN
    @Volatile private var lastDeniedAtMs = 0L
    private val pool = Executors.newCachedThreadPool()

    // One shell round trip: prints "type|raw_temp" for every thermal zone.
    private const val DUMP_CMD =
        "for z in /sys/class/thermal/thermal_zone*; do " +
            "echo \"\$(cat \$z/type 2>/dev/null)|\$(cat \$z/temp 2>/dev/null)\"; done"

    /** Call when the user taps "Retry root access". */
    fun resetRoot() {
        rootState = RootState.UNKNOWN
        lastDeniedAtMs = 0L
    }

    fun read(allowRoot: Boolean = true): ThermalInfo {
        var zones: List<ThermalZone> = emptyList()
        var viaRoot = false

        val retryDue = System.currentTimeMillis() - lastDeniedAtMs > 30_000L
        if (allowRoot && (rootState != RootState.DENIED || retryDue)) {
            // First attempt waits longer so there is time to tap "Grant" in the root prompt.
            val timeout = if (rootState == RootState.GRANTED) 3L else 15L
            val out = runSu(timeout)
            if (out != null) zones = parse(out)
            if (zones.isNotEmpty()) {
                rootState = RootState.GRANTED
                viaRoot = true
            } else {
                rootState = RootState.DENIED
                lastDeniedAtMs = System.currentTimeMillis()
            }
        }

        if (zones.isEmpty()) zones = readDirect()
        return summarize(zones, viaRoot)
    }

    private fun runSu(timeoutSec: Long): String? = try {
        val p = ProcessBuilder("su", "-c", DUMP_CMD).redirectErrorStream(true).start()
        val future = pool.submit<String> { p.inputStream.bufferedReader().use { it.readText() } }
        try {
            future.get(timeoutSec, TimeUnit.SECONDS)
        } catch (_: Exception) {
            p.destroy()
            future.cancel(true)
            null
        }
    } catch (_: Exception) {
        null // `su` binary not found
    }

    private fun readDirect(): List<ThermalZone> = try {
        File("/sys/class/thermal").listFiles { f -> f.name.startsWith("thermal_zone") }
            .orEmpty()
            .mapNotNull { dir ->
                try {
                    val type = File(dir, "type").readText().trim()
                    val raw = File(dir, "temp").readText().trim()
                    toZone(type, raw)
                } catch (_: Exception) {
                    null
                }
            }
    } catch (_: Exception) {
        emptyList()
    }

    private fun parse(out: String): List<ThermalZone> =
        out.lineSequence()
            .mapNotNull { line ->
                val idx = line.indexOf('|')
                if (idx <= 0) null else toZone(line.substring(0, idx).trim(), line.substring(idx + 1).trim())
            }
            .toList()

    private fun toZone(type: String, raw: String): ThermalZone? {
        val v = raw.toFloatOrNull() ?: return null
        val c = if (kotlin.math.abs(v) >= 1000f) v / 1000f else v
        if (c !in 0f..150f) return null // drop invalid / disabled sensors
        return ThermalZone(type.ifBlank { "zone" }, c)
    }

    private fun summarize(zones: List<ThermalZone>, viaRoot: Boolean): ThermalInfo {
        val cpu = zones.filter { it.name.contains("cpu", true) || it.name.contains("cluster", true) }
            .maxOfOrNull { it.celsius }
        val gpu = zones.filter { it.name.contains("gpu", true) }.maxOfOrNull { it.celsius }
        return ThermalInfo(
            cpuC = cpu,
            gpuC = gpu,
            hottest = zones.maxByOrNull { it.celsius },
            sensorCount = zones.size,
            viaRoot = viaRoot,
            rootState = rootState,
        )
    }
}
