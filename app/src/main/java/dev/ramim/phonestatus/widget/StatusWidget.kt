package dev.ramim.phonestatus.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.ramim.phonestatus.R
import dev.ramim.phonestatus.data.Stats
import dev.ramim.phonestatus.data.StatsReader
import dev.ramim.phonestatus.data.ThermalInfo
import dev.ramim.phonestatus.data.ThermalReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Primary = Color(0xFFF2F5FF)
private val Secondary = Color(0xFF9AA4C2)
private val BatteryGreen = Color(0xFF3DF5A7)
private val BatteryRed = Color(0xFFFF5470)
private val RamPurple = Color(0xFFA78BFA)
private val StorageAmber = Color(0xFFFFB454)
private val DisplayBlue = Color(0xFF4CC9F0)
private val TrackColor = Color(0x33FFFFFF)

class StatusWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp), // 2x2
            DpSize(250.dp, 110.dp), // 4x2
        ),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (stats, thermal) = withContext(Dispatchers.IO) {
            StatsReader.readAll(context) to ThermalReader.read()
        }
        val time = SimpleDateFormat("HH:mm", Locale.US).format(Date())
        provideContent { WidgetContent(stats, thermal, time) }
    }
}

/** Tap anywhere on the widget to refresh it. */
class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        StatusWidget().update(context, glanceId)
    }
}

private fun style(sizeSp: Int, color: Color, bold: Boolean = false) = TextStyle(
    color = ColorProvider(color),
    fontSize = sizeSp.sp,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
)

@Composable
private fun WidgetContent(stats: Stats, thermal: ThermalInfo, time: String) {
    val wide = LocalSize.current.width >= 200.dp
    Box(
        GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_bg))
            .padding(12.dp)
            .clickable(actionRunCallback<RefreshAction>()),
    ) {
        if (wide) WideLayout(stats, thermal, time) else CompactLayout(stats, thermal)
    }
}

@Composable
private fun CompactLayout(s: Stats, t: ThermalInfo) {
    val b = s.battery
    val color = if (b.level <= 15 && !b.charging) BatteryRed else BatteryGreen
    Column(GlanceModifier.fillMaxSize()) {
        Text("BATTERY", style = style(10, Secondary, bold = true))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${b.level}%", style = style(30, Primary, bold = true))
            if (b.charging) Text(" ⚡", style = style(18, color))
        }
        Spacer(GlanceModifier.height(2.dp))
        Bar(b.level / 100f, color)
        Spacer(GlanceModifier.height(6.dp))
        Text(
            "RAM ${(s.ram.fraction * 100).roundToInt()}% · SSD ${(s.storage.fraction * 100).roundToInt()}%",
            style = style(11, Primary),
        )
        Text(tempLine(s, t), style = style(11, Secondary))
    }
}

@Composable
private fun WideLayout(s: Stats, t: ThermalInfo, time: String) {
    val b = s.battery
    val color = if (b.level <= 15 && !b.charging) BatteryRed else BatteryGreen
    Row(GlanceModifier.fillMaxSize()) {
        Column(GlanceModifier.width(100.dp)) {
            Text("BATTERY", style = style(10, Secondary, bold = true))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${b.level}%", style = style(32, Primary, bold = true))
                if (b.charging) Text(" ⚡", style = style(18, color))
            }
            Spacer(GlanceModifier.height(2.dp))
            Bar(b.level / 100f, color)
            Spacer(GlanceModifier.height(6.dp))
            Text("${String.format(Locale.US, "%.1f", b.tempC)} °C", style = style(12, Primary))
            Text("Updated $time", style = style(9, Secondary))
        }
        Spacer(GlanceModifier.width(14.dp))
        Column(GlanceModifier.defaultWeight()) {
            MiniStat(
                "RAM",
                "${(s.ram.fraction * 100).roundToInt()}%",
                s.ram.fraction,
                RamPurple,
            )
            Spacer(GlanceModifier.height(7.dp))
            MiniStat(
                "Storage",
                "${(s.storage.fraction * 100).roundToInt()}%",
                s.storage.fraction,
                StorageAmber,
            )
            Spacer(GlanceModifier.height(7.dp))
            Text(
                "${s.display.currentHz.roundToInt()} Hz" + (t.cpuC?.let { " · CPU ${it.roundToInt()}°C" } ?: ""),
                style = style(12, DisplayBlue, bold = true),
            )
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, fraction: Float, color: Color) {
    Column(GlanceModifier.fillMaxWidth()) {
        Row(GlanceModifier.fillMaxWidth()) {
            Text(label, style = style(11, Secondary), modifier = GlanceModifier.defaultWeight())
            Text(value, style = style(11, Primary, bold = true))
        }
        Spacer(GlanceModifier.height(2.dp))
        Bar(fraction, color)
    }
}

@Composable
private fun Bar(fraction: Float, color: Color) {
    LinearProgressIndicator(
        progress = fraction.coerceIn(0f, 1f),
        modifier = GlanceModifier.fillMaxWidth(),
        color = ColorProvider(color),
        backgroundColor = ColorProvider(TrackColor),
    )
}

private fun tempLine(s: Stats, t: ThermalInfo): String {
    val temp = t.cpuC ?: s.battery.tempC
    val label = if (t.cpuC != null) "CPU" else "Bat"
    return "$label ${temp.roundToInt()}°C · ${s.display.currentHz.roundToInt()} Hz"
}
