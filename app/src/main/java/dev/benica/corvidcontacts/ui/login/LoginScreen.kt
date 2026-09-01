// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.ui.contacts.common_ui.BackNavButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCExposedDropdownMenuBox
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCIconButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCOutlinedTextField
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCScaffold
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCWidthClampedBox
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.ThemePreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    uiState: LoginUiState,
    modifier: Modifier = Modifier,
    savedServers: Set<String> = emptySet(),
    onRemoveSavedServer: (String) -> Unit = {},
    onLogin: (String, String, String, Boolean) -> Unit,
    prefilledUrl: String? = null,
    // Only non-null when reached via "Set Up Sync..."
    onBack: (() -> Unit)? = null,
) {
    var serverUrl by remember { mutableStateOf(prefilledUrl ?: "") }
    var username by remember { mutableStateOf("") }
    var appPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var serversExpanded by remember { mutableStateOf(false) }
    var fetchRemotePhotos by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    fun submit() {
        if (serverUrl.isNotBlank() && username.isNotBlank() && appPassword.isNotBlank()) {
            focusManager.clearFocus()
            onLogin(serverUrl, username, appPassword, fetchRemotePhotos)
        }
    }

    // Update serverUrl if prefilledUrl changes
    LaunchedEffect(prefilledUrl) {
        if (prefilledUrl != null) {
            serverUrl = prefilledUrl
        }
    }

    CCScaffold(
        modifier = modifier,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.login_title)) },
                navigationIcon = {
                    onBack?.let { BackNavButton(it) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        CCWidthClampedBox(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.outerSpacing),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.outerSpacing)
            ) {
                Spacer(modifier = Modifier.height(Dimens.outerSpacing))

            Text(
                text = stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            CCExposedDropdownMenuBox(
                textBoxLabel = stringResource(R.string.login_server_url),
                currentValue = serverUrl,
                expanded = serversExpanded && savedServers.isNotEmpty(),
                onExpandedChange = { serversExpanded = it },
                modifier = Modifier.fillMaxWidth(),
                onValueChange = { serverUrl = it },
                readOnly = false,
                enabled = uiState !is LoginUiState.Loading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Next,
                ),
            ) {
                savedServers.forEach { url ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = url,
                                    modifier = Modifier.weight(1f)
                                )
                                CCIconButton(
                                    icon = Icons.Rounded.Close,
                                    contentDescription = R.string.login_remove,
                                    onClick = {
                                        onRemoveSavedServer(url)
                                    }
                                )
                            }
                        },
                        onClick = {
                            serverUrl = url
                            serversExpanded = false
                        }
                    )
                }
            }

            CCOutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = stringResource(R.string.login_username),
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = uiState !is LoginUiState.Loading,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Next,
                ),
            )

            CCOutlinedTextField(
                value = appPassword,
                onValueChange = { appPassword = it },
                label = stringResource(R.string.login_app_password),
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    CCIconButton(
                        icon = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (passwordVisible) R.string.login_hide_password else R.string.login_show_password,
                        onClick = { passwordVisible = !passwordVisible }
                    )
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = uiState !is LoginUiState.Loading,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )

            if (uiState is LoginUiState.Error) {
                val message = if (uiState.formatArgs != null) {
                    stringResource(
                        uiState.resId,
                        uiState.formatArgs
                    )
                } else {
                    stringResource(uiState.resId)
                }
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(Dimens.outerSpacing))

            ListItem(
                headlineContent = { Text(stringResource(R.string.common_auto_load_remote_photos_title)) },
                supportingContent = { Text(stringResource(R.string.common_auto_load_remote_photos_description)) },
                trailingContent = {
                    Switch(
                        checked = fetchRemotePhotos,
                        onCheckedChange = { fetchRemotePhotos = it },
                        enabled = uiState !is LoginUiState.Loading
                    )
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
            )

            Spacer(modifier = Modifier.height(Dimens.lgSpacing))

            Button(
                onClick = { submit() },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                enabled = uiState !is LoginUiState.Loading && serverUrl.isNotBlank() && username.isNotBlank() && appPassword.isNotBlank()
            ) {
                if (uiState is LoginUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.login_button),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.lgSpacing))

            Text(
                text = stringResource(R.string.onboarding_setup_transparency_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = stringResource(R.string.login_sync_disclosure),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.smSpacing)
            )
            }
        }
    }
}

@ThemePreview
@Composable
fun LoginScreenPreview() {
    CorvidContactsTheme {
        LoginScreen(
            uiState = LoginUiState.Idle,
            onLogin = { _, _, _, _ -> }
        )
    }
}
