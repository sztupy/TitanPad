package scot.raven.titanpad.settings.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import scot.raven.titanpad.R
import scot.raven.titanpad.settings.ui.PermissionStatusBanner
import scot.raven.titanpad.settings.ui.PreferenceCategory
import scot.raven.titanpad.settings.ui.SettingsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisclosureScreen(
    onNavigateToSettingsPage: () -> Unit,
    onNavigateBack: () -> Unit,
    settingsState: SettingsState,
) {
    val uiState by settingsState.uiState.collectAsState()

    if (uiState.disclosureAccepted) {
        onNavigateToSettingsPage()
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Disclosure") },
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
                PreferenceCategory(title = "Disclosure") {
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
                                    text = "TitanPad is an application allowing you to use your Titan 2's trackpad as a mouse, scroll wheel or joystick.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    text = "For this application to work it requires extended access to your device via both Accessibility Services and Shizuku. These services allow the application to:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    text = "1. Determine which application is running in the foreground.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    text = "2. Create an overlay on the screen displaying a mouse cursor.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    text = "3. Determine if the element under the mouse cursor is clickable or not.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    text = "4. Send tap and click events to the system at the place where the mouse cursor is.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    text = "5. Access your keyboard trackpad's and back screen's input.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    text = "6. Create virtual input devices to send touch, tap and click events.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    text = "The app doesn't use Accessibility Services nor Shizuku for any other reason. It also does not collect, store or send any personal information over the Internet or to other applications.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    text = "If you consent with the app's usage of the above, please click Consent below. You can also refuse consent in which case the application will exit.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                PreferenceCategory(title = "Consent") {
                    PermissionStatusBanner(
                        title = "Do not consent",
                        status = false,
                        onClickAction = onNavigateBack,
                        failText = "I do not consent to the above. Please exit the app.",
                        hideIcon = true
                    )

                    PermissionStatusBanner(
                        title = "Consent",
                        status = true,
                        onClickAction = {
                            settingsState.updateGlobalPreference(true) { settings, v ->
                                settings.copy(disclosureAccepted = v)
                            }
                        },
                        passText = "I consent to all of the above",
                        hideIcon = true
                    )
                }
            }
        }
    }
}