// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.settings.sections

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.ui.settings.SettingsSection
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme

/**
 * The birthday reminders switch, shared by Settings and onboarding. Turning it on asks for the
 * notification permission first, and the switch only reads as on while that permission is held.
 */
@Composable
fun BirthdayRemindersSection(
    enabled: Boolean,
    onToggled: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        onToggled(granted)
    }

    SettingsSection(
        title = stringResource(R.string.birthday_reminders_title)
    ) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.birthday_reminders_title)) },
            supportingContent = { Text(stringResource(R.string.birthday_reminders_description)) },
            trailingContent = {
                Switch(
                    checked = enabled && hasPermission,
                    onCheckedChange = { wantsOn ->
                        if (!wantsOn || hasPermission) {
                            onToggled(wantsOn)
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BirthdayRemindersSectionPreview() {
    CorvidContactsTheme {
        BirthdayRemindersSection(enabled = true, onToggled = {})
    }
}
