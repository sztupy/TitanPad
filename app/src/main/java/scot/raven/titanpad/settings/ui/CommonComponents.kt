package scot.raven.titanpad.settings.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.datastore.core.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.encodeUtf8
import rikka.shizuku.Shizuku
import scot.raven.titanpad.R
import scot.raven.titanpad.core.logs.Logger
import scot.raven.titanpad.core.util.KeyCodeUtil
import scot.raven.titanpad.cursor.domain.InputType
import scot.raven.titanpad.settings.domain.UsageConfig
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Arrays
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.min


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
    onClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    Surface(
        onClick = { if (onClick == null) onCheckedChange(!checked) else onClick() },
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
            if (onDeleteClick != null && !checked) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
fun TextFieldDialog(
    initialValue: String,
    onUpdate: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String,
    subtitle: String,
    label: String
) {
    var value by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialValue,
                selection = TextRange(initialValue.length)
            )
        )
    }
    var isError by remember { mutableStateOf(false) }

    val saveAction = {
        if (!isError) {
            onUpdate(value.text)
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(subtitle)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { input ->
                        val filtered = input.text
                        val newSelection = TextRange(
                            start = minOf(filtered.length, input.selection.start),
                            end = minOf(filtered.length, input.selection.end)
                        )

                        value = TextFieldValue(
                            text = filtered,
                            selection = newSelection,
                            composition = input.composition
                        )

                        isError = filtered.isEmpty()
                    },
                    label = { Text(label) },
                    singleLine = true,
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text(
                                text = "Invalid value",
                                color = Color.Red,
                                modifier = Modifier.padding(top = 4.dp)
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
fun InputSelectorItem(
    title: String,
    selectedInputType: InputType,
    onOptionSelected: (InputType) -> Unit
) {
    DropdownPreferenceItem(
        title = title,
        subtitle =
            when (selectedInputType) {
                InputType.OFF -> "Disabled"
                InputType.SOFTWARE_MOUSE -> "Software Mouse"
                InputType.HARDWARE_MOUSE -> "Emulated Hardware Mouse"
                InputType.HARDWARE_WHEEL -> "Emulated Hardware Mouse Wheel"
                InputType.SOFTWARE_SCROLL -> "Software Scroll Assistant"
                InputType.HARDWARE_SCROLL -> "Emulated Hardware Scroll Assistant"
                InputType.HARDWARE_JOYSTICK -> "Emulated Hardware Joystick"
            },
        selectedOption = selectedInputType,
        options =
            listOf(
                InputType.OFF to "Disabled",
                InputType.SOFTWARE_MOUSE to "Mouse (software)",
                InputType.HARDWARE_MOUSE to "Mouse (hardware)",
                InputType.HARDWARE_WHEEL to "Mouse Wheel (hardware)",
                InputType.SOFTWARE_SCROLL to "Scroll (software)",
                InputType.HARDWARE_SCROLL to "Scroll (hardware)",
                InputType.HARDWARE_JOYSTICK to "Joystick",
            ),
        onOptionSelected = onOptionSelected,
    )
}
@Composable
fun PermissionStatusBanner(
    title: String,
    status: Boolean?,
    onClickAction: () -> Unit,
    passText: String = "Permission Granted",
    failText: String = "Permission Required",
    hideIcon: Boolean = false
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
                if (!hideIcon) {
                    Icon(
                        imageVector = if (status == null) Icons.Default.Info else if (status) Icons.Default.Check else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (status == null) Color(0xFF0492C2) else if (status) Color(
                            0xFF4CAF50
                        ) else Color(0xFFFF9800),
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
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
                if (!hideIcon && (status == null || !status)) {
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
            "No activation key set"
        } else {
            "Current: ${KeyCodeUtil.keyCodeToString(currentKeyCode)}"
        }

    SimplePreferenceItem(
        title = title,
        subtitle = subtitle,
        onClick = onCaptureKey,
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
    icon: ImageVector?,
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
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
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
    var appIcon by remember { mutableStateOf<ImageBitmap?>(null) }

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
                Image(
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                Logger.d("Launching PickVisualMedia")
                pickVisualMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                return
            } catch (e: Exception) {
                Logger.e("PickVisualMedia launch failed, falling back to Intent", e)
            }
        }

        val intents = mutableListOf<Intent>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intents += Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                type = "image/*"
                putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, 1)
            }
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

@Composable
fun rememberDocumentCreateLauncher(
    settingsState: SettingsState,
    coroutineScope: CoroutineScope,
    inputCallback: ((String) -> Unit) -> Unit,
    fileName: String,
    context: Context
): () -> Unit {
    val intentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        Logger.d(uri.toString())
        coroutineScope.launch {
            if (uri?.path != null) {
                try {
                    inputCallback { backupData ->
                        try {
                            context.contentResolver.openOutputStream(uri)?.use { output ->
                                output.write(backupData.encodeUtf8().toByteArray())
                            }
                            settingsState.showToast("Backup created!")
                        } catch (e: Exception) {
                            settingsState.showToast("Could not create backup!")
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    settingsState.showToast("Could not create backup!")
                    e.printStackTrace()
                }
            } else {
                settingsState.showToast("Could not create backup!")
            }
        }
    }

    return {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intentLauncher.launch(
                "$fileName-${
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                }.json"
            )
        } else {
            intentLauncher.launch("$fileName-${System.currentTimeMillis()}.json")
        }
    }
}

fun readBytes(input: InputStream): ByteArray? {
    val len = Int.MAX_VALUE

    var bufs: MutableList<ByteArray>? = null
    var result: ByteArray? = null
    var total = 0
    var remaining = len
    var n: Int
    do {
        var buf = ByteArray(min(remaining, 8192))
        var nread = 0

        // read to EOF which may read more or less than buffer size
        while ((input.read(
                buf, nread,
                min(buf.size - nread, remaining)
            ).also { n = it }) > 0
        ) {
            nread += n
            remaining -= n
        }

        if (nread > 0) {
            if (Int.MAX_VALUE - 8 - total < nread) {
                throw OutOfMemoryError("Required array size too large")
            }
            if (nread < buf.size) {
                buf = Arrays.copyOfRange(buf, 0, nread)
            }
            total += nread
            if (result == null) {
                result = buf
            } else {
                if (bufs == null) {
                    bufs = ArrayList()
                    bufs.add(result)
                }
                bufs.add(buf)
            }
        }
        // if the last call to read returned -1 or the number of bytes
        // requested have been read then break
    } while (n >= 0 && remaining > 0)

    if (bufs == null) {
        if (result == null) {
            return ByteArray(0)
        }
        return if (result.size == total) result else result.copyOf(total)
    }

    result = ByteArray(total)
    var offset = 0
    remaining = total
    for (b in bufs) {
        val count = min(b.size, remaining)
        System.arraycopy(b, 0, result, offset, count)
        offset += count
        remaining -= count
    }

    return result
}

@Composable
fun rememberDocumentLoaderLauncher(
    settingsState: SettingsState,
    coroutineScope: CoroutineScope,
    outputCallback: (String) -> Unit,
    context: Context
): () -> Unit {
    val intentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        Logger.d(uri.toString())
        coroutineScope.launch {
            if (uri?.path != null) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val data = readBytes(input)
                        val inputData = data?.toString(Charset.forName("UTF-8")) ?: ""
                        outputCallback(inputData)
                    }
                } catch (e: Exception) {
                    settingsState.showToast("Could not restore backup!")
                    e.printStackTrace()
                }
            } else {
                settingsState.showToast("Could not restore backup!")
            }
        }
    }

    return {
        intentLauncher.launch(arrayOf("application/json"))
    }
}
