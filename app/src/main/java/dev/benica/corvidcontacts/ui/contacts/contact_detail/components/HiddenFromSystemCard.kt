// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_detail.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.extensions.background
import dev.benica.corvidcontacts.extensions.border
import dev.benica.corvidcontacts.extensions.borderFocused
import dev.benica.corvidcontacts.extensions.complementary
import dev.benica.corvidcontacts.extensions.onBackground
import dev.benica.corvidcontacts.extensions.onSurface
import dev.benica.corvidcontacts.extensions.surfaceVariant
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCCardBordered
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCIconButton
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.currentThemeColor

/**
 * Says that a contact was deleted from the phone's contacts in another app and is kept out of them,
 * and offers to put it back or to delete it from Corvid as well.
 */
@Composable
fun HiddenFromSystemCard(
    onShowAgain: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // An accent drawn from the contact's color, so the card stands out without leaving the theme.
    val accent = currentThemeColor()
    CCCardBordered(
        modifier = modifier.fillMaxWidth(),
        padding = PaddingValues(Dimens.smSpacing),
    ) {
        Column(
            modifier = Modifier.padding(Dimens.lgSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.smSpacing)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.smSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.detail_hidden_from_system_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                CCIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = R.string.detail_hidden_from_system_dismiss,
                    size = 32.dp,
                    color = accent.onSurface(),
                    onClick = onDismiss
                )
            }
            Text(
                text = stringResource(R.string.detail_hidden_from_system_message),
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.smSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CCButton(
                    text = R.string.detail_hidden_from_system_show,
                    baseColor = accent.onSurface(),
                    onClick = onShowAgain
                )

                CCButton(
                    text = R.string.detail_hidden_from_system_delete,
                    baseColor = accent.onBackground(),
                    onClick = onDelete
                )
            }
        }
    }
}


@Preview
@Composable
fun HiddenFromSystemCardPreview() {
    CorvidContactsTheme {
        HiddenFromSystemCard(onShowAgain = {}, onDelete = {}, onDismiss = {})
    }
}

@Preview(
    uiMode = UI_MODE_NIGHT_YES
)
@Composable
fun HiddenFromSystemCardNightPreview() {
    CorvidContactsTheme {
        HiddenFromSystemCard(onShowAgain = {}, onDelete = {}, onDismiss = {})
    }
}
