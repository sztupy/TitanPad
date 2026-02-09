package scot.raven.titanpad.settings.ui.setup

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import scot.raven.titanpad.settings.ui.PreferenceCategory
import scot.raven.titanpad.settings.ui.SettingsState
import scot.raven.titanpad.settings.ui.SimplePreferenceItem
import scot.raven.titanpad.settings.ui.SwitchPreferenceItem
import scot.raven.titanpad.settings.ui.startActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupOptionsScreen(
    settingsState: SettingsState,
    onNavigateBack: () -> Unit
) {
    val uiState by settingsState.uiState.collectAsState()
    var showPassthroughDialog by remember { mutableStateOf(false) }
    var showTouchscreenDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Titan 2 Setup") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {

            PreferenceCategory(title = "Assistant") {
                SimplePreferenceItem(
                    title = "Scroll Assistant",
                    subtitle = "Disable Scroll Assistant",
                    onClick = {
                        if (!startActivity("com.agui.settings/.touchpad.ScrollAssistantActivity")) {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    }
                )

                SimplePreferenceItem(
                    title = "Cursor Assistant",
                    subtitle = "Disable Cursor Assistant",
                    onClick = {
                        if (!startActivity("com.agui.settings/.touchpad.CusorMoveAssistantActivity")) {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    }
                )
            }

            PreferenceCategory(title = "Keyboard") {
                SimplePreferenceItem(
                    title = "Disable upper left button's shortcut",
                    subtitle = "Set Func1 to Programmable key -> None",
                    onClick = {
                        if (!startActivity("com.agui.shortcutsettings/.ui.EntryAppActivity")) {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    }
                )

                SimplePreferenceItem(
                    title = "Disable lower left button's shortcut",
                    subtitle = "Set Func2 to Programmable key -> None",
                    onClick = {
                        if (!startActivity("com.agui.shortcutsettings/.ui.EntryAppActivity")) {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    }
                )
            }
        }
    }
}