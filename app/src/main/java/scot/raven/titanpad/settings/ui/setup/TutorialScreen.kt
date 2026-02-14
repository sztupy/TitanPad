package scot.raven.titanpad.settings.ui.setup

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
import scot.raven.titanpad.settings.ui.openNewTabWindow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tutorials") },
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
            PreferenceCategory(title = "Setup") {
                SimplePreferenceItem(
                    title = "Setup instructions",
                    subtitle = "How to set up Shizuku and TitanPad",
                    onClick = { openNewTabWindow("https://www.youtube.com/watch?v=Kr-tL90d--4", context) }
                )
            }

            PreferenceCategory(title = "Features") {
                SimplePreferenceItem(
                    title = "Scroll Assistant",
                    subtitle = "How to set up Scroll features",
                    onClick = { openNewTabWindow("https://www.youtube.com/watch?v=mMy_xteu18g", context) }
                )

                SimplePreferenceItem(
                    title = "Trackpad Mouse",
                    subtitle = "How the mouse emulation works",
                    onClick = { openNewTabWindow("https://www.youtube.com/watch?v=QzgKhxKGywY", context) }
                )

                SimplePreferenceItem(
                    title = "Auto Start on Boot",
                    subtitle = "What Auto Start on Boot does",
                    onClick = { openNewTabWindow("https://www.youtube.com/watch?v=xTqsaMGSJ6M", context) }
                )
            }

            PreferenceCategory(title = "Examples") {
                SimplePreferenceItem(
                    title = "Gamepad Emulation in Dolphin",
                    subtitle = "How to set up and use the analog Gamepad feature inside Dolphin Emulator",
                    onClick = { openNewTabWindow("https://www.youtube.com/watch?v=KmtyBWD3xmQ", context) }
                )

                SimplePreferenceItem(
                    title = "Virtual Desktop in Termux/X11",
                    subtitle = "How to set up the mouse and scroll wheel to make it work under Termux/X11",
                    onClick = { openNewTabWindow("https://www.youtube.com/watch?v=akeR65n24pI", context) }
                )
            }
        }
    }
}