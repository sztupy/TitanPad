package scot.raven.titanpad.settings.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import scot.raven.titanpad.BuildConfig
import scot.raven.titanpad.R
import scot.raven.titanpad.TitanPad
import scot.raven.titanpad.accessibility.AppAccessibilityService
import scot.raven.titanpad.core.logs.Logger
import scot.raven.titanpad.core.shizuku.ShizukuConnection
import scot.raven.titanpad.core.shizuku.ShizukuStatus
import scot.raven.titanpad.settings.domain.UsageConfig


/**
 * Main settings screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsState: SettingsState,
    activeConfiguration: StateFlow<String>,
    onNavigateToCursorSettings: (String) -> Unit,
    onNavigateToSetupOptions: () -> Unit,
) {
    val uiState by settingsState.uiState.collectAsState()
    val validationErrors by settingsState.validationErrors.collectAsState()
    val context = LocalContext.current
    var shizukuVersionValid by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

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

            if(uiState.showError) {
                NoteItem(uiState.errorMessage, Icons.Default.Warning, "Error")
            }

            if(uiState.showInvalidSettingError) {
                validationErrors.forEach {
                    NoteItem(it, Icons.Default.Warning, "Error")
                }
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

            val configurationStatus = activeConfiguration.collectAsState()
            PreferenceCategory(title = "Configurations") {
                NoteItem("Click on item to edit configuration. Use switcher to activate/deactivate.", Icons.Default.Info, "Information")

                SwitchPreferenceItem(
                    title = uiState.defaultConfigName,
                    subtitle = "",
                    onClick = {
                        onNavigateToCursorSettings("default")
                    },
                    onCheckedChange = { state ->
                        if (AppAccessibilityService.getInstance() == null) {
                            settingsState.showToast("Background system not running, enable Accessibility Services")
                        } else {
                            if (state)
                                AppAccessibilityService.activateStandardCursor(context, "default")
                            else
                                AppAccessibilityService.deactivateStandardCursor(context, "default")
                        }
                    },
                    checked = configurationStatus.value == "default"
                )

                uiState.configList.forEach { item ->
                    SwitchPreferenceItem(
                        title = item.value,
                        subtitle = "",
                        onClick = {
                            onNavigateToCursorSettings(item.key)
                        },
                        onDeleteClick = {
                            settingsState.deleteConfig(item.key)
                        },
                        onCheckedChange = { state ->
                            if (AppAccessibilityService.getInstance() == null) {
                                settingsState.showToast("Background system not running, enable Accessibility Services")
                            } else {
                                if (state)
                                    AppAccessibilityService.activateStandardCursor(
                                        context,
                                        item.key
                                    )
                                else
                                    AppAccessibilityService.deactivateStandardCursor(
                                        context,
                                        item.key
                                    )
                            }
                        },
                        checked = configurationStatus.value == item.key
                    )
                }

                SimplePreferenceItem(
                    title = "New Configuration",
                    subtitle = "Add new configuration to the list",
                    onClick = {
                        Logger.d("CLICKED")
                        settingsState.addConfig(UsageConfig.randomId())
                    }
                )

                DropdownPreferenceItem(
                    title = "Auto-start on Boot",
                    subtitle =
                        when (uiState.runAfterBoot) {
                            in uiState.configList.keys -> "Start up ${uiState.configList.getValue(uiState.runAfterBoot)} after boot or application restart"
                            "default" -> "Start up main config after boot or application restart"
                            "previous" -> "Start up last active config after boot or application restart"
                            else -> "Don't enable anything after boot or application restart"
                        },
                    selectedOption = uiState.runAfterBoot,
                    options =
                        listOf(
                            "" to "Disabled",
                            "previous" to "Last Active",
                            "default" to "Main Config",
                        ) +
                        uiState.configList.map{ e -> e.key to "Switch to ${e.value}" },
                    onOptionSelected = { value ->
                        settingsState.updateGlobalPreference(value) { settings, v ->
                            settings.copy(runAfterBoot = v)
                        }
                    },
                )
            }

            PreferenceCategory(title = "Keys") {
                SwitchPreferenceItem(
                    title = "Make the Func keys visible to applications",
                    subtitle = "Make the Func keys (the two buttons on the left side) visible to external apps, e.g. Key Mapper. Also enables these buttons to be used as Activation Keys in the config",
                    checked = uiState.alwaysRemapFuncKeys,
                    onCheckedChange = { value ->
                        settingsState.updateGlobalPreference(value) { settings, v ->
                            settings.copy(alwaysRemapFuncKeys = v)
                        }
                    },
                )

                SwitchPreferenceItem(
                    title = "Enable better compatibility",
                    subtitle = "Map Func1 to KEYCODE_F11 and Func2 to KEYCODE_F12 for better compatibility with external apps",
                    checked = uiState.alwaysRemapFuncKeysCompat,
                    onCheckedChange = { value ->
                        settingsState.updateGlobalPreference(value) { settings, v ->
                            settings.copy(alwaysRemapFuncKeysCompat = v)
                        }
                    },
                    enabled = uiState.alwaysRemapFuncKeys
                )
            }

            val backupLauncher = rememberDocumentCreateLauncher(
                settingsState = settingsState,
                coroutineScope = coroutineScope,
                context = LocalContext.current,
                fileName = "titanpad-full-backup",
                inputCallback = { callback ->
                    coroutineScope.launch {
                        val currentSettings =
                            TitanPad.getInstance().settingsRepository.getSettings().first()
                        val result = TitanPad.getInstance().settingsRepository.exportSettings(currentSettings)

                        callback(result)
                    }
                }
            )

            val restoreLauncher = rememberDocumentLoaderLauncher(
                settingsState = settingsState,
                coroutineScope = coroutineScope,
                context = LocalContext.current,
                outputCallback = { jsonData ->
                    coroutineScope.launch {
                        try {
                            if (TitanPad.getInstance().settingsRepository.importSettings(jsonData)) {
                                settingsState.showToast("Backup restored!")
                            } else {
                                settingsState.showToast("Could not restore backup!")
                            }
                        } catch (_: Exception) {
                            settingsState.showToast("Could not restore backup!")
                        }
                    }
                }
            )

            PreferenceCategory(title = "Backup and Restore") {
                SimplePreferenceItem(
                    title = "Backup Configuration",
                    subtitle = "Save your entire config to a file",
                    onClick = { backupLauncher() }
                )

                SimplePreferenceItem(
                    title = "Restore Configuration",
                    subtitle = "Load an existing full backup, replacing your entire config",
                    onClick = { restoreLauncher() }
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