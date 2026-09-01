// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_edit.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.extensions.oklch
import dev.benica.corvidcontacts.extensions.surfaceVariant
import dev.benica.corvidcontacts.extensions.toOklch
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCAlertDialog
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCTextButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.ContactAvatar
import dev.benica.corvidcontacts.ui.contacts.common_ui.HueSlider
import dev.benica.corvidcontacts.ui.contacts.common_ui.HueSliderDefaults
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.ThemePreview

@Composable
fun ContactAvatarPicker(
    selectedColor: Color?,
    displayName: String,
    defaultColor: Color,
    photoUrl: String?,
    onColorSelected: (Color?) -> Unit,
    onPhotoClick: () -> Unit,
    onRemovePhoto: () -> Unit,
    enabled: Boolean,
    seed: String? = null,
    size: Dp = 96.dp,
) {
    var showDialog by remember { mutableStateOf(false) }

    // Capture state when the dialog opens so we can "Cancel" (Undo).
    var originalColor by remember { mutableStateOf<Color?>(null) }
    var originalPhotoUrl by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .size(size)
            .clickable(enabled = enabled) {
                originalColor = selectedColor
                originalPhotoUrl = photoUrl
                showDialog = true
            }
    ) {
        // Ease in step with the text fields.
        val resolvedColor by animateColorAsState(
            targetValue = selectedColor ?: defaultColor,
            animationSpec = spring(
                dampingRatio = 1f,
                stiffness = 3800f
            ),
            label = "avatarColor"
        )
        ContactAvatar(
            displayName = displayName,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    end = Dimens.smSpacing,
                    bottom = Dimens.smSpacing
                ),
            baseColor = resolvedColor,
            photoUrl = photoUrl,
            id = seed
        )

        if (enabled) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size / 7 * 2)
                    .border(
                        width = 0.dp,
                        color = resolvedColor,
                        shape = CircleShape
                    ),
                shape = CircleShape,
                color = resolvedColor.surfaceVariant(),
                shadowElevation = Dimens.xsSpacing,
                tonalElevation = Dimens.xsSpacing
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.edit_avatar_settings),
                        tint = resolvedColor,
                        modifier = Modifier.size(size / 5)
                    )
                }
            }
        }

        if (showDialog) {
            AvatarPickerDialog(
                onPhotoClick = onPhotoClick,
                onRemovePhoto = onRemovePhoto,
                photoUrl = photoUrl,
                displayName = displayName,
                seed = seed,
                bookColor = defaultColor,
                onColorSelected = onColorSelected,
                selectedColor = selectedColor,
                onDismissRequest = { showDialog = false },
                onCancel = {
                    onColorSelected(originalColor)
                    if (photoUrl != originalPhotoUrl) {
                        onRemovePhoto()
                    }
                    showDialog = false
                }
            )
        }
    }
}


@Composable
private fun AvatarPickerDialog(
    onPhotoClick: () -> Unit,
    onRemovePhoto: () -> Unit,
    photoUrl: String?,
    displayName: String,
    seed: String?,
    bookColor: Color,
    onColorSelected: (Color?) -> Unit,
    selectedColor: Color?,
    onDismissRequest: () -> Unit,
    onCancel: () -> Unit,
) {
    val currentThemeColor = selectedColor ?: bookColor
    var hue by remember { mutableFloatStateOf(currentThemeColor.toOklch().h) }

    CCAlertDialog(
        onDismissRequest = onCancel,
        title = R.string.edit_avatar_edit_dialog_title,
        confirmButton = R.string.action_save,
        onConfirm = onDismissRequest,
        dismissButton = R.string.action_cancel,
        baseColor = currentThemeColor,
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Dimens.lgSpacing),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Live Preview
                ContactAvatar(
                    displayName = displayName,
                    photoUrl = photoUrl,
                    baseColor = if (selectedColor == null) bookColor else oklch(
                        HueSliderDefaults.LIGHTNESS,
                        HueSliderDefaults.CHROMA,
                        hue
                    ),
                    id = seed,
                    size = 120.dp,
                )

                // Contact Photo Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Dimens.xsSpacing)
                ) {
                    Text(
                        stringResource(R.string.edit_avatar_settings),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.smSpacing)
                    ) {
                        CCTextButton(
                            text = R.string.edit_action_pick_photo,
                            icon = Icons.Rounded.AddPhotoAlternate,
                            onClick = onPhotoClick,
                            modifier = Modifier.weight(1f),
                        )

                        CCTextButton(
                            text = R.string.edit_action_remove_photo,
                            icon = Icons.Rounded.Clear,
                            onClick = onRemovePhoto,
                            modifier = Modifier.weight(1f),
                            enabled = !photoUrl.isNullOrBlank()
                        )
                    }
                }

                HorizontalDivider()

                // Contact Color Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Dimens.xsSpacing),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        stringResource(R.string.edit_avatar_theme_colors),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    HueSlider(
                        hue = hue,
                        onHueChange = {
                            hue = it
                            onColorSelected(
                                oklch(
                                    HueSliderDefaults.LIGHTNESS,
                                    HueSliderDefaults.CHROMA,
                                    it
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )

                    CCTextButton(
                        text = R.string.edit_action_use_book_color,
                        icon = Icons.Rounded.Refresh,
                        onClick = {
                            onColorSelected(null)
                            hue = bookColor.toOklch().h
                        },
                        enabled = selectedColor != null
                    )
                }
            }
        }
    )
}

@ThemePreview
@Composable
fun ContactAvatarPickerPreview() {
    CorvidContactsTheme {
        ContactAvatarPicker(
            selectedColor = ContactColors.palette[2],
            displayName = "John Doe",
            defaultColor = ContactColors.palette[4],
            photoUrl = null,
            onColorSelected = {},
            onPhotoClick = {},
            onRemovePhoto = {},
            enabled = true,
        )
    }
}

@ThemePreview
@Composable
fun AvatarPickerDialogPreview() {
    CorvidContactsTheme {
        AvatarPickerDialog(
            onPhotoClick = {},
            onRemovePhoto = {},
            photoUrl = null,
            displayName = "Rimbaugh McTurty",
            seed = null,
            bookColor = ContactColors.palette[1],
            onColorSelected = { },
            selectedColor = ContactColors.palette[3],
            onDismissRequest = { },
            onCancel = { }
        )
    }
}
