package dev.ramim.phonestatus.data

data class BatteryInfo(
    val level: Int = 0,
    val charging: Boolean = false,
    val plug: String = "On battery",
    val tempC: Float = 0f,
    val voltageV: Float = 0f,
    val currentMa: Int? = null,
    val health: String = "Unknown",
    val technology: String = "",
)

data class RamInfo(
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val swapTotalBytes: Long = 0L,
    val swapUsedBytes: Long = 0L,
) {
    val fraction: Float get() = if (totalBytes > 0) usedBytes.toFloat() / totalBytes else 0f
}

data class StorageInfo(
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
) {
    val freeBytes: Long get() = (totalBytes - usedBytes).coerceAtLeast(0L)
    val fraction: Float get() = if (totalBytes > 0) usedBytes.toFloat() / totalBytes else 0f
}

data class DisplayInfo(
    val currentHz: Float = 60f,
    val supportedHz: List<Int> = emptyList(),
    val width: Int = 0,
    val height: Int = 0,
    val dpi: Int = 0,
)

data class Stats(
    val battery: BatteryInfo = BatteryInfo(),
    val ram: RamInfo = RamInfo(),
    val storage: StorageInfo = StorageInfo(),
    val display: DisplayInfo = DisplayInfo(),
)

data class ThermalZone(val name: String, val celsius: Float)

enum class RootState { UNKNOWN, GRANTED, DENIED }

data class ThermalInfo(
    val cpuC: Float? = null,
    val gpuC: Float? = null,
    val hottest: ThermalZone? = null,
    val sensorCount: Int = 0,
    val viaRoot: Boolean = false,
    val rootState: RootState = RootState.UNKNOWN,
)
