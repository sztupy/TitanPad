package scot.raven.titanpad.settings.ui.scroll

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import scot.raven.titanpad.settings.ui.PreferenceCategory
import scot.raven.titanpad.settings.ui.SettingsState
import scot.raven.titanpad.settings.ui.SliderPreferenceItem
import scot.raven.titanpad.settings.ui.SwitchPreferenceItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelSettingsScreen(
    settingsState: SettingsState,
    scrollId: Int,
    onNavigateBack: () -> Unit
) {
    val uiState by settingsState.uiState.collectAsState()
    val currentScroll = uiState.wheelSettings[scrollId]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wheel Settings ${when(scrollId) { 0 -> "Main Touchpad" 1 -> "Left Touchpad" 2 -> "Right Touchpad" 3 -> "Back Screen" else -> "" } }") },
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
            PreferenceCategory(title = "Settings") {
                SwitchPreferenceItem(
                    title = "Vertical Wheel Lock",
                    subtitle = if (currentScroll.scrollOnlyVertically) "Emitting standard mouse wheel events only" else "Emitting standard and horizontal wheel events",
                    checked = currentScroll.scrollOnlyVertically,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(
                                wheelSettings = settings.wheelSettings.mapIndexed { id, scroll ->
                                    if (id == scrollId) scroll.copy(
                                        scrollOnlyVertically = v
                                    ) else scroll.copy()
                                }
                            )
                        }
                    },
                )

                SliderPreferenceItem(
                    title = "Touch Sensitivity",
                    value = currentScroll.touchSensitivity.toFloat(),
                    valueRange = 0f..10f,
                    valueText = "${currentScroll.touchSensitivity}",
                    onValueChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(
                                wheelSettings = settings.wheelSettings.mapIndexed { id, scroll ->
                                    if (id == scrollId) scroll.copy(
                                        touchSensitivity = v.toInt()
                                    ) else scroll.copy()
                                }
                            )
                        }
                    },
                    steps = 9,
                )

                SliderPreferenceItem(
                    title = "Wheel Speed",
                    value = currentScroll.speed.toFloat(),
                    valueRange = 0f..10f,
                    valueText = "${currentScroll.speed}",
                    onValueChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(
                                wheelSettings = settings.wheelSettings.mapIndexed { id, scroll ->
                                    if (id == scrollId) scroll.copy(
                                        speed = v.toInt()
                                    ) else scroll.copy()
                                }
                            )
                        }
                    },
                    steps = 9,
                )

                SwitchPreferenceItem(
                    title = "Wheel Momentum",
                    subtitle = if (currentScroll.momentum) "Wheel stops slowly after releasing finger" else "Wheel stops immediately after releasing finger",
                    checked = currentScroll.momentum,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(
                                wheelSettings = settings.wheelSettings.mapIndexed { id, scroll ->
                                    if (id == scrollId) scroll.copy(
                                        momentum = v
                                    ) else scroll.copy()
                                }
                            )
                        }
                    },
                )
            }
        }
    }
}