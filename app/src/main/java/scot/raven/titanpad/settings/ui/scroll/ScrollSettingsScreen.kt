package scot.raven.titanpad.settings.ui.scroll

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import scot.raven.titanpad.core.util.BoundingBoxUtil
import scot.raven.titanpad.settings.ui.NoteItem
import scot.raven.titanpad.settings.ui.PreferenceCategory
import scot.raven.titanpad.settings.ui.SettingsState
import scot.raven.titanpad.settings.ui.SliderPreferenceItem
import scot.raven.titanpad.settings.ui.SwitchPreferenceItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrollSettingsScreen(
    settingsState: SettingsState,
    scrollId: Int,
    onNavigateBack: () -> Unit
) {
    val uiState by settingsState.uiState.collectAsState()
    val currentScroll = uiState.scrollSettings[scrollId]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scroll Settings ${when(scrollId) { 0 -> "Main Touchpad" 1 -> "Left Touchpad" 2 -> "Right Touchpad" 3 -> "Back Screen" else -> "" } }") },
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
                    title = "Vertical Scroll Lock",
                    subtitle = if (currentScroll.scrollOnlyVertically) "Emitting vertical scroll events only" else "Emitting vertical and horizontal scroll events",
                    checked = currentScroll.scrollOnlyVertically,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(
                                scrollSettings = settings.scrollSettings.mapIndexed { id, scroll ->
                                    if (id == scrollId) scroll.copy(
                                        scrollOnlyVertically = v
                                    ) else scroll.copy()
                                }
                            )
                        }
                    },
                )

                SliderPreferenceItem(
                    title = "Scroll Sensitivity",
                    value = currentScroll.touchSensitivity.toFloat(),
                    valueRange = 0f..10f,
                    valueText = "${currentScroll.touchSensitivity}",
                    onValueChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(
                                scrollSettings = settings.scrollSettings.mapIndexed { id, scroll ->
                                    if (id == scrollId) scroll.copy(
                                        touchSensitivity = v.toInt()
                                    ) else scroll.copy()
                                }
                            )
                        }
                    },
                    steps = 9,
                )
                NoteItem(
                    "Setting this to 10 will enable 'Slide and click' style functionality allowing taps",
                    icon = null,
                    "Sensitivity Info"
                )
            }
            PreferenceCategory(title = "Scroll Location") {
                NoteItem(
                    "The grey box shows where the scroll events will take place on the screen. Make sure it's size is similar to the size of the input for a natural scrolling experience.",
                    icon = null,
                    "Touch Info"
                )
                SliderPreferenceItem(
                    title = "Margin from top of screen",
                    value = currentScroll.topCropRegion.toFloat(),
                    valueRange = 0f..100f,
                    valueText = "${currentScroll.topCropRegion}%",
                    onValueChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(
                                scrollSettings = settings.scrollSettings.mapIndexed{ id, scroll ->
                                    if (id == scrollId) scroll.copy(
                                        topCropRegion = v.toInt()
                                    ) else scroll.copy()
                                }
                            )
                        }
                    },
                    steps = 19,
                )

                SliderPreferenceItem(
                    title = "Margin from bottom of screen",
                    value = currentScroll.bottomCropRegion.toFloat(),
                    valueRange = 0f..100f,
                    valueText = "${currentScroll.bottomCropRegion}%",
                    onValueChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(
                                scrollSettings = settings.scrollSettings.mapIndexed{ id, scroll ->
                                    if (id == scrollId) scroll.copy(
                                        bottomCropRegion = v.toInt()
                                    ) else scroll.copy()
                                }
                            )
                        }
                    },
                    steps = 19,
                )

                SliderPreferenceItem(
                    title = "Margin from left side of screen",
                    value = currentScroll.leftCropRegion.toFloat(),
                    valueRange = 0f..100f,
                    valueText = "${currentScroll.leftCropRegion}%",
                    onValueChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(
                                scrollSettings = settings.scrollSettings.mapIndexed{ id, scroll ->
                                    if (id == scrollId) scroll.copy(
                                        leftCropRegion = v.toInt()
                                    ) else scroll.copy()
                                }
                            )
                        }
                    },
                    steps = 19,
                )

                SliderPreferenceItem(
                    title = "Margin from right side of screen",
                    value = currentScroll.rightCropRegion.toFloat(),
                    valueRange = 0f..100f,
                    valueText = "${currentScroll.rightCropRegion}%",
                    onValueChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(
                                scrollSettings = settings.scrollSettings.mapIndexed{ id, scroll ->
                                    if (id == scrollId) scroll.copy(
                                        rightCropRegion = v.toInt()
                                    ) else scroll.copy()
                                }
                            )
                        }
                    },
                    steps = 19,
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f) // Ensure it is above
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val boundingBox = BoundingBoxUtil.screenBox(size.width, size.height, currentScroll.leftCropRegion.toFloat(), currentScroll.topCropRegion.toFloat(), currentScroll.rightCropRegion.toFloat(), currentScroll.bottomCropRegion.toFloat())

                drawRect(
                    color = Color.Gray.copy(alpha = 0.5f),
                    topLeft = Offset(boundingBox.x, boundingBox.y),
                    size = Size(boundingBox.width, boundingBox.height)
                )
            }
        }
    }
}