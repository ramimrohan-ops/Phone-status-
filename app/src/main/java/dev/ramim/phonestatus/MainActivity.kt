package dev.ramim.phonestatus

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.glance.appwidget.updateAll
import dev.ramim.phonestatus.data.StatsViewModel
import dev.ramim.phonestatus.ui.Dashboard
import dev.ramim.phonestatus.ui.PhoneStatusTheme
import dev.ramim.phonestatus.ui.rememberLiveHz
import dev.ramim.phonestatus.widget.StatusWidget
import dev.ramim.phonestatus.widget.StatusWidgetReceiver
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val vm: StatsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        setContent {
            PhoneStatusTheme {
                val stats by vm.stats.collectAsStateWithLifecycle()
                val thermal by vm.thermal.collectAsStateWithLifecycle()
                val liveHz by rememberLiveHz()
                Dashboard(
                    stats = stats,
                    thermal = thermal,
                    liveHz = liveHz,
                    onAddWidget = { requestPinWidget(this) },
                    onRetryRoot = vm::retryRoot,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh any widgets already on the home screen whenever the app is opened.
        lifecycleScope.launch { StatusWidget().updateAll(applicationContext) }
    }

    private fun requestPinWidget(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)
        if (mgr.isRequestPinAppWidgetSupported) {
            mgr.requestPinAppWidget(ComponentName(ctx, StatusWidgetReceiver::class.java), null, null)
        } else {
            Toast.makeText(
                ctx,
                "Long-press your home screen → Widgets → Phone Status",
                Toast.LENGTH_LONG,
            ).show()
        }
    }
}
