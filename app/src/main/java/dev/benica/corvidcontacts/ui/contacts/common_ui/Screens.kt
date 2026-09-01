// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.common_ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.sharp.HourglassEmpty
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.ThemePreview

/**
 * A generic empty state component.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            description?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            action?.let {
                Spacer(modifier = Modifier.height(32.dp))
                it()
            }
        }
    }
}

/**
 * A centered loading indicator.
 */
@Composable
fun FullScreenLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Displays a sync failure message with optional retry.
 *
 * @param message The error message to display.
 * @param onRetry Callback for the retry action.
 */
@Composable
fun FullScreenSyncFailureError(
    message: String,
    onRetry: (() -> Unit)? = null,
) {
    val displayMessage = when {
        message.contains(
            "Step 1",
            true
        ) -> stringResource(R.string.common_error_step1)

        message.contains(
            "Step 2",
            true
        ) -> stringResource(R.string.common_error_step2)

        message.contains(
            "Step 3",
            true
        ) -> stringResource(R.string.common_error_step3)

        message.contains(
            "401",
            true
        ) -> stringResource(R.string.common_error_401)

        message.contains(
            "Date",
            true
        ) -> stringResource(R.string.common_error_birthday)

        else -> message
    }

    EmptyState(
        icon = Icons.Rounded.ErrorOutline,
        title = stringResource(R.string.common_error_sync_title),
        description = displayMessage,
        action = onRetry?.let {
            {
                CCButton(
                    text = R.string.common_try_again,
                    onClick = it,
                    modifier = Modifier.padding(top = Dimens.smSpacing)
                )
            }
        }
    )
}

@ThemePreview
@Composable
private fun EmptyStatePreview() {
    CorvidContactsTheme {
        EmptyState(
            icon = Icons.Sharp.HourglassEmpty,
            title = "I'm an empty state",
            description = "Description of a description",
        )
    }
}
