package scot.raven.titanpad.settings.ui.gesture

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
import scot.raven.titanpad.core.constants.GestureConstants
import scot.raven.titanpad.settings.ui.PreferenceCategory
import scot.raven.titanpad.settings.ui.SettingsState
import scot.raven.titanpad.settings.ui.SliderPreferenceItem
import scot.raven.titanpad.settings.ui.SwitchPreferenceItem
import kotlin.math.max
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrollSettingsScreen(
    settingsState: SettingsState,
    onNavigateBack: () -> Unit
) {
    val uiState by settingsState.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scroll Options") },
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
            PreferenceCategory(title = "Behavior") {
                SwitchPreferenceItem(
                    title = "Natural Scrolling",
                    subtitle = "Use content-based scrolling instead of standard scrolling",
                    checked = uiState.useNaturalScrolling,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(useNaturalScrolling = v)
                        }
                    }
                )

                SwitchPreferenceItem(
                    title = "Multi-touch Scrolling",
                    subtitle = "Use two fingers to scroll",
                    checked = uiState.scrollMultitouchEnabled,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(scrollMultitouchEnabled = v)
                        }
                    },
                )

                SwitchPreferenceItem(
                    title = "Scroll Area",
                    subtitle = "Use part of the touchpad for scrolling",
                    checked = uiState.scrollAreaEnabled,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(scrollAreaEnabled = v)
                        }
                    },
                )
            }

            PreferenceCategory(title = "Scroll Area") {
                SliderPreferenceItem(
                    title = "Top",
                    value = uiState.scrollAreaTopPercent,
                    valueRange = 0.toFloat()..100.toFloat(),
                    valueText = "${uiState.scrollAreaTopPercent.toInt()}%",
                    onValueChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(scrollAreaTopPercent = v)
                        }
                    },
                    enabled = uiState.scrollAreaEnabled
                )

                SliderPreferenceItem(
                    title = "Left",
                    value = uiState.scrollAreaLeftPercent,
                    valueRange = 0.toFloat()..100.toFloat(),
                    valueText = "${uiState.scrollAreaLeftPercent.toInt()}%",
                    onValueChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(scrollAreaLeftPercent = v)
                        }
                    },
                    enabled = uiState.scrollAreaEnabled
                )

                SliderPreferenceItem(
                    title = "Bottom",
                    value = uiState.scrollAreaBottomPercent,
                    valueRange = 0.toFloat()..100.toFloat(),
                    valueText = "${uiState.scrollAreaBottomPercent.toInt()}%",
                    onValueChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(scrollAreaBottomPercent = v)
                        }
                    },
                    enabled = uiState.scrollAreaEnabled
                )

                SliderPreferenceItem(
                    title = "Right",
                    value = uiState.scrollAreaRightPercent,
                    valueRange = 0.toFloat()..100.toFloat(),
                    valueText = "${uiState.scrollAreaRightPercent.toInt()}%",
                    onValueChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(scrollAreaRightPercent = v)
                        }
                    },
                    enabled = uiState.scrollAreaEnabled
                )
            }
        }
    }
}