package com.kaleo.watts

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaleo.watts.ui.C
import com.kaleo.watts.ui.WattsScreen

class MainActivity : ComponentActivity() {
    private val vm: WattsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = C.Accent, background = C.Bg, surface = C.Bg, onBackground = C.Text, onSurface = C.Text,
                    surfaceVariant = C.Surface, onSurfaceVariant = C.Muted,
                ),
            ) {
                val screenOn by vm.keepScreenOn.collectAsStateWithLifecycle()
                LaunchedEffect(screenOn) {
                    if (screenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                Box(Modifier.fillMaxSize().safeDrawingPadding()) { WattsScreen(vm) }
            }
        }
    }
}
