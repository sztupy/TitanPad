package scot.raven.titanpad.settings.ui.setup

import scot.raven.titanpad.core.ui.KeyCaptureOverlay
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.IOException
import scot.raven.titanpad.core.constants.CursorConstants
import scot.raven.titanpad.core.logs.Logger
import scot.raven.titanpad.settings.domain.AppListType
import scot.raven.titanpad.settings.domain.UsageConfig
import scot.raven.titanpad.settings.ui.ClearKeyPreferenceItem
import scot.raven.titanpad.settings.ui.ColorPickerDialog
import scot.raven.titanpad.settings.ui.DropdownPreferenceItem
import scot.raven.titanpad.settings.ui.NoteItem
import scot.raven.titanpad.settings.ui.PreferenceCategory
import scot.raven.titanpad.settings.ui.SetKeyPreferenceItem
import scot.raven.titanpad.settings.ui.SettingsState
import scot.raven.titanpad.settings.ui.SimplePreferenceItem
import scot.raven.titanpad.settings.ui.SliderPreferenceItem
import scot.raven.titanpad.settings.ui.SwitchPreferenceItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import scot.raven.titanpad.accessibility.AppAccessibilityService
import scot.raven.titanpad.core.constants.ApplicationConstants
import scot.raven.titanpad.settings.ui.TextFieldDialog
import scot.raven.titanpad.settings.ui.startActivity
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID

/**
 * Standard cursor settings screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CursorSettingsScreen(
    settingsState: SettingsState,
    onNavigateToCursorIcon: () -> Unit,
    onNavigateToLocationClickableIcon: () -> Unit,
    onNavigateToClickableAppsScreen: () -> Unit,
    onNavigateToDebugOptions: () -> Unit,
    onNavigateToAutoHideSettings: () -> Unit,
    onNavigateToCommonGestureSettings: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val uiState by settingsState.uiState.collectAsState()
    var showCursorKeyCaptureOverlay by remember { mutableStateOf(false) }
    var reservedKeys by remember { mutableStateOf(emptyMap<Int, String>()) }
    var showNameChangeDialog by remember { mutableStateOf(false) }
    var showColorPickerDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val currentKeyDescription =
        if (
            uiState.cursorActivationKey != UsageConfig.KEY_NONE &&
            reservedKeys.isNotEmpty() &&
            !reservedKeys[uiState.cursorActivationKey].isNullOrEmpty()
        ) {
            reservedKeys[uiState.cursorActivationKey]
        } else {
            null
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Input configuration - ${uiState.configName}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            PreferenceCategory(title = "Configuration") {
                SimplePreferenceItem(
                    title = "Configuration ID",
                    subtitle = uiState.configId,
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("TitanPad Config ID", uiState.configId)
                        clipboard.setPrimaryClip(clip)

                        settingsState.showToast("TitanPad Configuration ID copied to Clipboard")
                    }
                )

                SimplePreferenceItem(
                    title = "Configuration Name",
                    subtitle = uiState.configName,
                    onClick = { showNameChangeDialog = true }
                )



                if (showNameChangeDialog) {
                    TextFieldDialog(
                        title = "Configuration Name",
                        initialValue = uiState.configName,
                        subtitle = "Set configuration name",
                        label = "Name",
                        onUpdate = { configName ->
                            settingsState.updatePreference(configName) { settings, v ->
                                settings.copy(configName = v)
                            }
                        },
                        onDismiss = { showNameChangeDialog = false }
                    )
                }
            }

            PreferenceCategory(title = "Activation") {
                if (currentKeyDescription != null) {
                    NoteItem(
                        title = "\"$currentKeyDescription\" overridden and disabled",
                        icon = Icons.Default.Warning,
                        contentDescription = "Warning",
                        color = Color(0xFFFFF4E6),
                    )
                }

                SetKeyPreferenceItem(
                    title = "Set Activation Key",
                    currentKeyCode = uiState.cursorActivationKey,
                    onCaptureKey = {
                        settingsState.requestHideAllOverlays()
                        showCursorKeyCaptureOverlay = true
                    },
                )

                ClearKeyPreferenceItem(
                    mode = "standard cursor",
                    onClearKey = {
                        settingsState.requestHideAllOverlays()
                        settingsState.updateCursorActivationKey(UsageConfig.KEY_NONE)
                    },
                )

                if (showCursorKeyCaptureOverlay) {
                    KeyCaptureOverlay(
                        restrictedKeys = setOf(),
                        reservedKeys = reservedKeys,
                        onKeySelected = { settingsState.updateCursorActivationKey(it) },
                        onDismiss = { showCursorKeyCaptureOverlay = false },
                        showToast = { message -> settingsState.showToast(message) },
                    )
                }
            }

            PreferenceCategory(title = "Adaptive") {
                SwitchPreferenceItem(
                    title = "Show Location Clickable",
                    subtitle = "Attempt to indicate if current cursor location is clickable",
                    checked = uiState.checkClickable,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(checkClickable = v)
                        }
                    },
                )

                DropdownPreferenceItem(
                    title = "Application List Type",
                    subtitle =
                        when (uiState.clickableListType) {
                            AppListType.ALLOW_LIST -> "Show clickable locations only for selected apps"
                            AppListType.DENY_LIST -> "Do not show clickable locations for selected apps"
                        },
                    selectedOption = uiState.clickableListType,
                    options =
                        listOf(
                            AppListType.ALLOW_LIST to "Allow",
                            AppListType.DENY_LIST to "Deny"
                        ),
                    onOptionSelected = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(clickableListType = v)
                        }
                    },
                    enabled = uiState.checkClickable
                )

                SimplePreferenceItem(
                    title = "Select Applications",
                    subtitle = "${if (uiState.clickableListType == AppListType.ALLOW_LIST) "Show" else "Ignore"} in specific apps",
                    onClick = onNavigateToClickableAppsScreen,
                    enabled = uiState.checkClickable
                )
            }

            PreferenceCategory(title = "Appearance") {
                SliderPreferenceItem(
                    title = "Cursor Size",
                    value = uiState.cursorSize.toFloat(),
                    valueRange = CursorConstants.MIN_SIZE.toFloat()..CursorConstants.MAX_SIZE.toFloat(),
                    valueText = uiState.cursorSize.toString(),
                    onValueChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(cursorSize = v.toInt())
                        }
                    },
                    steps = 8,
                )

                SwitchPreferenceItem(
                    title = "Smooth Cursor Corners",
                    subtitle = "Round out the corners of the cursor",
                    checked = uiState.roundedCursorCorners,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(roundedCursorCorners = v)
                        }
                    },
                )

                SimplePreferenceItem(
                    title = "Cursor Color",
                    subtitle = "Current RGB hex value: #${uiState.standardCursorHex}",
                    onClick = { showColorPickerDialog = true }
                )

                SwitchPreferenceItem(
                    title = "Match Border to Body",
                    subtitle = "Replace black border and match cursor body color",
                    checked = uiState.standardCursorMatchBorder,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(standardCursorMatchBorder = v)
                        }
                    },
                )

                if (showColorPickerDialog) {
                    ColorPickerDialog(
                        initialColorHex = uiState.standardCursorHex,
                        onColorSelected = { newColorHex ->
                            settingsState.updatePreference(newColorHex) { settings, v ->
                                settings.copy(standardCursorHex = v)
                            }
                        },
                        onDismiss = { showColorPickerDialog = false },
                        title = "Cursor Color"
                    )
                }
            }

            PreferenceCategory(title = "Custom Icon") {
                SwitchPreferenceItem(
                    title = "Custom Cursor Icons",
                    subtitle = "Replace the default cursor icon with an image or gif",
                    checked = uiState.useCustomCursorIcon,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(useCustomCursorIcon = v)
                        }
                    },
                )

                SimplePreferenceItem(
                    title = "Cursor Icon",
                    subtitle = when {
                        uiState.cursorImagePath == null -> "No icon set, falling back to default icon"
                        else -> "Update icon"
                    },
                    onClick = onNavigateToCursorIcon,
                    enabled = uiState.useCustomCursorIcon
                )

                SimplePreferenceItem(
                    title = "Location Clickable Icon",
                    subtitle = when {
                        !uiState.checkClickable -> "Only applicable if \"Show Location Clickable\" is enabled"
                        uiState.clickableImagePath == null -> "Select icon, otherwise falling back to base custom icon"
                        else -> "Update icon"
                    },
                    onClick = onNavigateToLocationClickableIcon,
                    enabled = uiState.useCustomCursorIcon && uiState.checkClickable,
                )
            }

            PreferenceCategory(title = "Behavior") {
                SimplePreferenceItem(
                    title = "Auto-Hide Cursor Options",
                    subtitle = "Automatically hide and restore the cursor",
                    onClick = onNavigateToAutoHideSettings
                )

                SliderPreferenceItem(
                    title = "Activation Duration",
                    value = uiState.activationDuration.toFloat(),
                    valueRange = ApplicationConstants.MIN_ACTIVATION_HOLD_DURATION.toFloat()..ApplicationConstants.MAX_ACTIVATION_HOLD_DURATION.toFloat(),
                    valueText = "${uiState.activationDuration} ms",
                    onValueChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(activationDuration = v.toLong())
                        }
                    },
                    steps = 4,
                )

                if (uiState.activationDuration == 0L) {
                    NoteItem(
                        title = "Activation keys will be fully intercepted",
                        icon = Icons.Default.Warning,
                        contentDescription = "Warning",
                        color = Color(0xFFFFF4E6),
                    )
                    NoteItem(
                        title = "Standard cursor control scheme toggle will be disabled",
                        icon = Icons.Default.Warning,
                        contentDescription = "Warning",
                        color = Color(0xFFFFF4E6),
                    )
                }

                SwitchPreferenceItem(
                    title = "Show Notification Icon",
                    subtitle = "Show icon when cursor is activated",
                    checked = uiState.showNotification,
                    onCheckedChange = { value ->
                        settingsState.updatePreference(value) { settings, v ->
                            settings.copy(showNotification = v)
                        }
                    },
                )

                SimplePreferenceItem(
                    title = "Hardware mouse display settings",
                    subtitle = "Found under 'Display' -> 'Colour and Motion' -> 'Large mouse cursor'",
                    onClick = {
                        if (!startActivity("com.android.settings/.Settings\$ColorAndMotionActivity")) {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    }
                )

                SimplePreferenceItem(
                    title = "Hardware mouse sensitivity settings",
                    subtitle = "Found under 'System' -> 'Keyboard' -> 'Pointer Speed'",
                    onClick = {
                        if (!startActivity("com.android.settings/.Settings\$KeyboardSettingsActivity")) {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    }
                )
            }

            PreferenceCategory(title = "Gestures") {
                SimplePreferenceItem(
                    title = "Common Gesture Options",
                    subtitle = "Settings that apply to both scrolls and zooms",
                    onClick = onNavigateToCommonGestureSettings
                )
            }

            PreferenceCategory(title = "Advanced") {
                SimplePreferenceItem(
                    title = "Developer Options",
                    subtitle = "Additional configurable features",
                    onClick = onNavigateToDebugOptions
                )
            }
        }
    }
}

fun clearImage(imagePath: String?, onCleared: () -> Unit) {
    imagePath?.takeIf { it.isNotEmpty() }?.let { path ->
        File(path).takeIf { it.exists() }?.delete()
        Logger.d("Custom icon deleted")
    }
    onCleared()
}

suspend fun savePickedImage(
    context: Context,
    uri: Uri,
    oldPath: String?,
    onCleared: () -> Unit,
    updatePreference: (String) -> Unit
) {
    clearImage(oldPath, onCleared)
    val savedImagePath = saveImageToAppStorage(context, uri)
    updatePreference(savedImagePath)
}

suspend fun saveImageToAppStorage(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    val mimeType = context.contentResolver.getType(uri)

    val extension = MimeTypeMap.getSingleton()
        .getExtensionFromMimeType(mimeType)
        ?.lowercase(Locale.getDefault())
        ?.takeIf {
            it in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
        } ?: run {
        val uriPath = uri.path
        uriPath?.substringAfterLast('.')?.lowercase(Locale.getDefault())?.takeIf {
            it in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
        } ?: "png"
    }

    val fileName = "cursor_${UUID.randomUUID()}.$extension"
    val file = File(context.filesDir, fileName)

    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
    } catch (e: IOException) {
        Logger.e("Failed to save image from uri $uri to file ${file.name}", e)
        throw e
    }

    return@withContext file.absolutePath
}

@Composable
fun rememberUnifiedImagePickerLauncher(
    coroutineScope: CoroutineScope,
    context: Context,
    oldPath: String?,
    onCleared: () -> Unit,
    updatePreference: (String) -> Unit
): UnifiedImagePickerLauncher {
    val intentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                coroutineScope.launch {
                    try {
                        savePickedImage(context, uri, oldPath, onCleared, updatePreference)
                    } catch (e: Exception) {
                        Logger.e("Failed to process picked image (Intent)", e)
                    }
                }
            }
        }
    }

    val pickVisualMediaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                try {
                    savePickedImage(context, it, oldPath, onCleared, updatePreference)
                } catch (e: Exception) {
                    Logger.e("Failed to process picked image (PickVisualMedia)", e)
                }
            }
        }
    }

    return remember(intentLauncher, pickVisualMediaLauncher) {
        UnifiedImagePickerLauncher(intentLauncher, pickVisualMediaLauncher)
    }
}

class UnifiedImagePickerLauncher(
    private val intentLauncher: ActivityResultLauncher<Intent>,
    private val pickVisualMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest>
) {
    fun launch(context: Context) {
        try {
            Logger.d("Launching PickVisualMedia")
            pickVisualMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            return
        } catch (e: Exception) {
            Logger.e("PickVisualMedia launch failed, falling back to Intent", e)
        }

        val intents = mutableListOf<Intent>()
        intents += Intent(MediaStore.ACTION_PICK_IMAGES).apply {
            type = "image/*"
            putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, 1)
        }
        intents += listOf(
            Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            },
            Intent(Intent.ACTION_PICK).apply {
                setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            }
        )

        for (intent in intents) {
            try {
                if (intent.resolveActivity(context.packageManager) != null) {
                    Logger.d("Launching fallback intent: $intent")
                    intentLauncher.launch(intent)
                    return
                }
            } catch (e: Exception) {
                Logger.e("Failed to launch fallback intent", e)
            }
        }

        Logger.e("No available image pickers found")
    }
}
