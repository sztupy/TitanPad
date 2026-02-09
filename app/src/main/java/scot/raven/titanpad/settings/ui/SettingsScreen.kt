package scot.raven.titanpad.settings.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.toColorInt
import scot.raven.titanpad.BuildConfig
import scot.raven.titanpad.R
import scot.raven.titanpad.core.constants.ApplicationConstants
import scot.raven.titanpad.core.shizuku.ShizukuConnection
import scot.raven.titanpad.core.shizuku.ShizukuStatus
import scot.raven.titanpad.settings.domain.UsageConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import rikka.shizuku.Shizuku
import scot.raven.titanpad.core.logs.Logger

/**
 * Main settings screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsState: SettingsState,
    onNavigateToCursorSettings: () -> Unit,
    onNavigateToDebugOptions: () -> Unit,
    onNavigateToAutoHideSettings: () -> Unit,
    onNavigateToCommonGestureSettings: () -> Unit,
    onNavigateToSetupOptions: () -> Unit
) {
    val uiState by settingsState.uiState.collectAsState()
    val context = LocalContext.current
    var shizukuVersionValid by remember { mutableStateOf(false) }

    LaunchedEffect(shizukuVersionValid) {
        shizukuVersionValid = withContext(Dispatchers.IO) {
            try {
                val packageInfo =
                    context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
                val versionString = packageInfo.versionName.orEmpty()
                Logger.d("Shizuku version found: $versionString")
                val versionName = versionString.split(".")
                if (versionName.size<3) {
                    false
                } else {
                    val major = versionName[0].toInt()
                    val minor = versionName[1].toInt()
                    val build = versionName[2].toInt()

                    if (major == 13 && minor == 6 && build == 0 && versionString.contains("thedjchi")) {
                        true
                    } else if (major == 13 && minor == 5 && build == 4) {
                        true
                    } else if (major > 13 || (major == 13 && minor > 6) || (major == 13 && minor == 6 && build > 0)) {
                        true
                    } else {
                        false
                    }
                }
            } catch (_: Exception) {
                false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
            if (BuildConfig.DEBUG) {
                NoteItem("Pre-Release Version", Icons.Default.Info, "Information")
            }

            PreferenceCategory(title = "Setup") {

                PermissionStatusBanner(
                    title = "Accessibility Service",
                    status = uiState.isAccessibilityServiceEnabled,
                    onClickAction = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                )

                val shizukuStatus = ShizukuConnection.statusFlow.collectAsState().value
                PermissionStatusBanner(
                    title = "Shizuku Service",
                    status = shizukuStatus == ShizukuStatus.READY,
                    onClickAction = {
                        when (shizukuStatus) {
                            ShizukuStatus.PERMISSION_REQUIRED -> ShizukuConnection.requestPermission()
                            else -> {}
                        }
                    }
                )

                PermissionStatusBanner(
                    title = "Shizuku MTK support",
                    status = shizukuVersionValid,
                    onClickAction = {
                        when (shizukuVersionValid) {
                            false -> openNewTabWindow(
                                "https://github.com/thedjchi/Shizuku/releases",
                                context
                            )

                            else -> {}
                        }
                    },
                    passText = "Shizuku version supported",
                    failText = "Shizuku's latest official version v13.6.0 doesn't work with MTK phones due to a bug. Either downgrade to v13.5.4, or use thedjchi's Shizuku fork which already contains a fix along with other improvements. Click here for the download link.",
                )

                PermissionStatusBanner(
                    title = "Disable Scroll Assistant",
                    status = null,
                    onClickAction = {
                        onNavigateToSetupOptions()
                    },
                    passText = "Disable built-in features like the Scroll Assistant",
                )
            }

            PreferenceCategory(title = "Input Modes") {
                SimplePreferenceItem(
                    title = "Standard Cursor",
                    subtitle =
                    if (uiState.cursorActivationKey == UsageConfig.KEY_NONE) {
                        "Unmapped"
                    } else {
                        "Mapped"
                    },
                    onClick = onNavigateToCursorSettings,
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

            PreferenceCategory(title = "About") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        Row(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(dimensionResource(R.dimen.padding_standard)),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Visit github.com/sztupy/TitanPad for instructions and updates.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clickable {
                                        openNewTabWindow("https://github.com/sztupy/TitanPad", context)
                                    }
                            )
                        }
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(dimensionResource(R.dimen.padding_standard)),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Visit https://ko-fi.com/sztupy if you want to send me a coffee",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clickable {
                                        openNewTabWindow("https://ko-fi.com/sztupy", context)
                                    }
                            )
                        }
                        Row(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(dimensionResource(R.dimen.padding_standard)),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Version: ${BuildConfig.VERSION_NAME}-${if (BuildConfig.DEBUG) "debug" else "release"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PreferenceCategory(
    title: String,
    content: @Composable () -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier =
            Modifier.padding(
                start = dimensionResource(R.dimen.padding_standard),
                top = dimensionResource(R.dimen.padding_standard),
                bottom = dimensionResource(R.dimen.padding_small),
            ),
        )
        content()
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_standard)))
    }
}

@Composable
fun SliderPreferenceItem(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    steps: Int = 0,
    onValueChange: (Float) -> Unit,
    enabled: Boolean = true
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_standard)),
        ) {
            Text(text = title)
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                enabled = enabled
            )
        }
    }
}

@Composable
fun SwitchPreferenceItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_standard)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.padding(start = dimensionResource(R.dimen.padding_small)),
                enabled = enabled
            )
        }
    }
}

@Composable
fun <T> DropdownPreferenceItem(
    title: String,
    subtitle: String? = null,
    selectedOption: T,
    options: List<Pair<T, String>>,
    onOptionSelected: (T) -> Unit,
    enabled: Boolean? = true
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText = options.find { it.first == selectedOption }?.second ?: "Select"
    val itemEnabled = enabled ?: true

    val titleColor = if (itemEnabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }

    val subtitleColor = if (itemEnabled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    Surface(
        onClick = { expanded = true },
        modifier = Modifier.fillMaxWidth(),
        enabled = itemEnabled
    ) {
        Row(
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_standard)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = titleColor)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = subtitleColor,
                    )
                }
            }

            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text(
                        text = selectedText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = subtitleColor,
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select",
                        tint = if (itemEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    options.forEach { (option, text) ->
                        DropdownMenuItem(
                            text = { Text(text) },
                            onClick = {
                                onOptionSelected(option)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusBanner(
    title: String,
    status: Boolean?,
    onClickAction: () -> Unit,
    passText: String = "Permission Granted",
    failText: String = "Permission Required",
) {
    Column {
        Surface(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (status == null) MaterialTheme.colorScheme.surfaceContainer else if (status) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(8.dp),
            onClick = onClickAction,
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (status == null) Icons.Default.Info else if (status) Icons.Default.Check else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (status == null) Color(0xFF0492C2) else if (status) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(10f, fill = true)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (status == null || status) passText else failText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (status == null || !status) {
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Configure",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
fun SetKeyPreferenceItem(
    title: String,
    currentKeyCode: Int,
    onCaptureKey: () -> Unit,
) {
    val subtitle =
        if (currentKeyCode == UsageConfig.KEY_NONE) {
            "Currently not mapped"
        } else {
            "Current: ${KeyEvent.keyCodeToString(currentKeyCode)}"
        }

    SimplePreferenceItem(
        title = title,
        subtitle = subtitle,
        onClick = onCaptureKey,
    )
}

@Composable
fun ClearKeyPreferenceItem(
    title: String = "Clear Activation Key",
    mode: String,
    onClearKey: () -> Unit,
) {
    SimplePreferenceItem(
        title = title,
        subtitle = "Unmaps $mode",
        onClick = onClearKey,
    )
}

@Composable
fun SimplePreferenceItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean? = true
) {
    val itemEnabled = enabled ?: true

    val titleColor = if (itemEnabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }

    val subtitleColor = if (itemEnabled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        enabled = itemEnabled
    ) {
        Row(
            modifier = Modifier.padding(dimensionResource(R.dimen.padding_standard)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = titleColor)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor,
                )
            }
        }
    }
}

@Composable
fun NoteItem(
    title: String,
    icon: ImageVector,
    contentDescription: String,
    color: Color? = null
) {
    Surface(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = color ?: Color.Transparent,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
            )
        }
    }
}

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val hasLauncherActivity: Boolean = false,
    val isHomeLauncher: Boolean = false,
    val version: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    settingsState: SettingsState,
    getter: (SettingsUiState) -> Set<String>,
    setter: (UsageConfig, Set<String>) -> UsageConfig,
    onNavigateBack: () -> Unit
) {
    val uiState by settingsState.uiState.collectAsState()
    val context = LocalContext.current
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showSystemApps by remember { mutableStateOf(false) }

    LaunchedEffect(showSystemApps) {
        isLoading = true
        installedApps = withContext(Dispatchers.IO) {
            loadInstalledApps(context.packageManager, showSystemApps)
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Applications") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(16.dp)
//            ) {
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Text(
//                        text = "Show system apps",
//                        modifier = Modifier.weight(1f),
//                        style = MaterialTheme.typography.bodyMedium
//                    )
//                    Switch(
//                        checked = showSystemApps,
//                        onCheckedChange = { showSystemApps = it }
//                    )
//                }
//
//                Text(
//                    text = "Showing ${installedApps.size} apps",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                    modifier = Modifier.padding(top = 4.dp)
//                )
//            }
//
//            HorizontalDivider()

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(installedApps) { app ->
                        AppItem(
                            app = app,
                            isSelected = app.packageName in getter(uiState),
                            onSelectionChanged = { isSelected ->
                                val newSet = if (isSelected) {
                                    getter(uiState) + app.packageName
                                } else {
                                    getter(uiState) - app.packageName
                                }
                                settingsState.updatePreference(newSet) { settings, v ->
                                    setter(settings, v)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppItem(
    app: AppInfo,
    isSelected: Boolean,
    onSelectionChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var appIcon by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(app.packageName) {
        withContext(Dispatchers.IO) {
            try {
                val drawable = context.packageManager.getApplicationIcon(app.packageName)
                val bitmap = drawable.toBitmap(48, 48)
                appIcon = bitmap.asImageBitmap()
            } catch (_: Exception) {}
        }
    }

    Surface(
        onClick = { onSelectionChanged(!isSelected) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (appIcon != null) {
                androidx.compose.foundation.Image(
                    bitmap = appIcon!!,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${app.packageName} ${app.version}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChanged
            )
        }
    }
}

suspend fun loadInstalledApps(packageManager: PackageManager, includeSystemApps: Boolean): List<AppInfo> {
    return withContext(Dispatchers.IO) {
        try {
            val allRelevantApps = mutableSetOf<ApplicationInfo>()

            val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val launcherActivities = packageManager.queryIntentActivities(launcherIntent, 0)
            val launcherPackages = launcherActivities.mapNotNull { it.activityInfo?.packageName }.toSet()

            launcherActivities.forEach { resolveInfo ->
                try {
                    val packageName = resolveInfo.activityInfo?.packageName
                    if (packageName != null) {
                        val appInfo = packageManager.getApplicationInfo(packageName, 0)
                        allRelevantApps.add(appInfo)
                    }
                } catch (_: Exception) {}
            }

            // Add stock launcher
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val homeActivities = packageManager.queryIntentActivities(homeIntent, 0)
            val homePackages = homeActivities.mapNotNull { it.activityInfo?.packageName }.toSet()

            homeActivities.forEach { resolveInfo ->
                try {
                    val packageName = resolveInfo.activityInfo?.packageName
                    if (packageName != null) {
                        val appInfo = packageManager.getApplicationInfo(packageName, 0)
                        allRelevantApps.add(appInfo)
                    }
                } catch (_: Exception) {}
            }

            allRelevantApps.mapNotNull { appInfo ->
                try {
                    val packageName = appInfo.packageName

                    val version = packageManager.getPackageInfo(packageName, 0).versionName

                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                            (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0

//                    if (!includeSystemApps && isSystemApp) return@mapNotNull null
                    if (appName.isBlank()) return@mapNotNull null

                    val isEnabled = try {
                        appInfo.enabled
                    } catch (e: Exception) {
                        true
                    }
                    if (!isEnabled) return@mapNotNull null

                    AppInfo(
                        packageName = packageName,
                        appName = appName,
                        isSystemApp = isSystemApp,
                        hasLauncherActivity = packageName in launcherPackages,
                        isHomeLauncher = packageName in homePackages,
                        version = version.orEmpty()
                    )
                } catch (e: Exception) {
                    null
                }
            }
                .distinctBy { it.packageName }
                .sortedWith(
                    compareBy { it.appName.lowercase() }
                )
        } catch (e: Exception) {
            emptyList()
        }
    }
}

@Composable
fun ColorPickerDialog(
    initialColorHex: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String
) {
    var colorHex by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialColorHex,
                selection = TextRange(initialColorHex.length)
            )
        )
    }
    var isError by remember { mutableStateOf(false) }

    val previewColor = try {
        Color("#${colorHex.text}".toColorInt())
        isError = false
        Color("#${colorHex.text}".toColorInt())
    } catch (e: Exception) {
        isError = true
        Color.Black
    }

    val saveAction = {
        if (!isError) {
            onColorSelected(colorHex.text)
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text("Enter a hex value. A preview is shown on the right.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = colorHex,
                    onValueChange = { input ->
                        val filtered = input.text.filter {
                            it.isDigit() || it in 'A'..'F' || it in 'a'..'f'
                        }.take(8)

                        val newSelection = TextRange(
                            start = minOf(filtered.length, input.selection.start),
                            end = minOf(filtered.length, input.selection.end)
                        )

                        colorHex = TextFieldValue(
                            text = filtered,
                            selection = newSelection,
                            composition = input.composition
                        )
                        try {
                            Color("#$filtered".toColorInt())
                            isError = false
                        } catch (e: Exception) {
                            isError = true
                        }
                    },
                    label = { Text("Hex value") },
                    singleLine = true,
                    isError = isError,
                    prefix = { Text("#") },
                    supportingText = {
                        if (isError) {
                            Text(
                                text = "Invalid hex code",
                                color = Color.Red,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    },
                    trailingIcon = {
                        if (!isError) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(previewColor)
                                    .border(2.dp, Color.Black)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (!isError) {
                                saveAction()
                            }
                        }
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!isError) saveAction()
                },
                enabled = !isError
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun openNewTabWindow(urls: String, context: Context) {
    val uris = urls.toUri()
    val intents = Intent(Intent.ACTION_VIEW, uris)
    val b = Bundle()
    b.putBoolean("new_window", true)
    intents.putExtras(b)
    context.startActivity(intents)
}

fun startActivity(activity: String): Boolean {
    val shizukuRunning = try { Shizuku.pingBinder() } catch (_: Exception) { false }
    val shizukuAuthorized = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Exception) { false }

    val shizukuAvailable = shizukuRunning && shizukuAuthorized
    if (shizukuAvailable) {
        try {
            Logger.e("Starting activity $activity")
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true

            newProcessMethod.invoke(
                null,
                arrayOf("am","start", "-n", activity),
                null,
                null
            )
        } catch (e: Exception) {
            Logger.e("Could not start activity $activity", e)
            return false
        }
        return true
    } else {
        Logger.e("Shizuku unavailable to start activity $activity")
        return false
    }
}