package dev.focusgarden.app

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

fun main() = application {
    GardenRuntime.platform = DesktopGardenPlatform()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Focus Garden",
        state = WindowState(width = 430.dp, height = 860.dp),
    ) { FocusGardenApp() }
}
