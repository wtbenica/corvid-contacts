// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.common_ui

import android.content.res.Configuration
import android.net.Uri
import android.util.Base64
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.twotone.AutoMode
import androidx.compose.material.icons.twotone.CrisisAlert
import androidx.compose.material.icons.twotone.Landslide
import androidx.compose.material.icons.twotone.Person
import androidx.compose.material.icons.twotone.SouthAmerica
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.extensions.active
import dev.benica.corvidcontacts.extensions.background
import dev.benica.corvidcontacts.extensions.border
import dev.benica.corvidcontacts.extensions.borderFocused
import dev.benica.corvidcontacts.extensions.complementary
import dev.benica.corvidcontacts.extensions.container
import dev.benica.corvidcontacts.extensions.surface
import dev.benica.corvidcontacts.extensions.surfaceVariant
import dev.benica.corvidcontacts.extensions.text
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.NotoMonoFontFamily
import dev.benica.corvidcontacts.ui.theme.currentThemeColor
import dev.benica.corvidcontacts.ui.theme.isDarkTheme
import java.io.File

/**
 * A container with a header and bordered card styling.
 */
@Composable
fun ContactSection(
    title: String,
    baseColor: Color = currentThemeColor(),
    icon: ImageVector? = null,
    @StringRes contentDescription: Int? = null,
    onAdd: (() -> Unit)? = null,
    @StringRes onAddLabel: Int? = null,
    padding: PaddingValues = PaddingValues(
        horizontal = Dimens.innerSpacing,
        vertical = Dimens.xlSpacing
    ),
    content: @Composable ColumnScope.() -> Unit,
) {
    CCCardBordered(
        baseColor = baseColor,
        padding = padding
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Dimens.medSpacing)
        ) {
            PrimaryHeader(
                title = title,
                baseColor = baseColor,
                onAdd = onAdd,
                onAddLabel = onAddLabel,
                icon = icon,
                iconDescription = contentDescription,
            )

            Column(verticalArrangement = Arrangement.spacedBy(Dimens.medSpacing)) {
                content()
            }
        }
    }
}

/**
 * A simpler section with a primary header.
 */
@Composable
fun Section(
    title: String,
    modifier: Modifier = Modifier,
    baseColor: Color = currentThemeColor(),
    icon: ImageVector? = null,
    @StringRes contentDescription: Int? = null,
    onAdd: (() -> Unit)? = null,
    @StringRes onAddLabel: Int? = null,
    itemSpacing: Dp = Dimens.lgSpacing,
    headerModifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        SecondaryHeader(
            title = title,
            modifier = headerModifier,
            baseColor = baseColor,
            icon = icon,
            iconDescription = contentDescription,
            onAdd = onAdd,
            onAddLabel = onAddLabel,
        )

        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(itemSpacing)
        ) {
            content()
        }
    }
}

/**
 * A toggle for revealing/hiding a section's less-common fields, placed at the bottom of the
 * fields it controls rather than as a "+" in the section header - the header "+" is reserved for
 * genuinely adding a new item to a list (a phone number, a relationship), which "show me the
 * Prefix field" isn't.
 */
@Composable
fun ExpandFieldsRow(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    baseColor: Color = currentThemeColor(),
) {
    CCTextButton(
        text = if (expanded) {
            stringResource(R.string.edit_action_show_fewer_fields)
        } else {
            stringResource(R.string.edit_action_show_more_fields)
        },
        icon = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
        onClick = onToggle,
        modifier = modifier,
        baseColor = baseColor,
    )
}

/**
 * Default [TextFieldColors] for contact input fields.
 */
@Composable
fun contactTextFieldColors(
    baseColor: Color = Color.Unspecified,
): TextFieldColors {
    val actualColor = if (baseColor == Color.Unspecified) currentThemeColor() else baseColor
    val neutralColor = Color(0xFF888888)

    return OutlinedTextFieldDefaults.colors(
        focusedLabelColor = actualColor,
        unfocusedLabelColor = Color(0xFF787878),
        focusedPlaceholderColor = Color(0xFF787878),
        unfocusedPlaceholderColor = Color(0xFF787878),
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedBorderColor = if (isDarkTheme()) neutralColor.active() else neutralColor,
        unfocusedBorderColor = if (isDarkTheme()) neutralColor.container() else neutralColor.copy(
            alpha = 0.4f
        ),
        focusedTrailingIconColor = actualColor.borderFocused(),
        unfocusedTrailingIconColor = actualColor.border(),
        cursorColor = actualColor.borderFocused(),
        selectionColors = TextSelectionColors(
            handleColor = actualColor.borderFocused(),
            backgroundColor = actualColor.surface()
        )
    )
}

/**
 * Default [DatePickerColors] for birthday pickers.
 */
@Composable
fun contactDatePickerColors(
    baseColor: Color = Color.Unspecified,
): DatePickerColors {
    val actualColor = if (baseColor == Color.Unspecified) currentThemeColor() else baseColor

    return DatePickerDefaults.colors(
        containerColor = actualColor.background(),
        selectedYearContainerColor = actualColor,
        disabledSelectedYearContainerColor = actualColor,
        selectedDayContainerColor = actualColor,
        disabledSelectedDayContainerColor = actualColor,
        todayContentColor = actualColor,
        todayDateBorderColor = actualColor,
        dayInSelectionRangeContainerColor = actualColor,
        dividerColor = actualColor.complementary(),
    )
}

/**
 * Renders a contact's avatar, prioritizing photo then initials.
 */
@Composable
fun ContactAvatar(
    displayName: String?,
    modifier: Modifier = Modifier,
    baseColor: Color = currentThemeColor(),
    photoUrl: String? = null,
    hasPhoto: Boolean = false,
    selected: Boolean = false,
    id: String? = null,
    size: Dp = 40.dp,
    shape: Shape = MaterialTheme.shapes.small,
    borderWidth: Dp = size / 20,
    showSelectionOverlay: Boolean = false,
    initials: String? = null,
) {
    val context = LocalContext.current
    // Resolved eagerly so a hasPhoto=true contact whose local file doesn't actually exist falls
    // through to initials instead of rendering a blank image.
    val photoData = remember(
        photoUrl,
        id,
        hasPhoto
    ) {
        if (photoUrl != null) {
            if (photoUrl.startsWith("data:")) {
                try {
                    val base64 = photoUrl.substringAfter("base64,")
                    Base64.decode(
                        base64,
                        Base64.DEFAULT
                    )
                } catch (_: Exception) {
                    null
                }
            } else photoUrl.takeUnless { it.startsWith("http") }
        } else if (hasPhoto && id != null) {
            val file = File(
                File(
                    context.filesDir,
                    "photos"
                ),
                "$id.jpg"
            )
            if (file.exists()) Uri.fromFile(file) else null
        } else null
    }

    Surface(
        shape = shape,
        color = baseColor.surfaceVariant(),
        modifier = modifier
            .size(size)
            .border(
                width = borderWidth,
                color = baseColor,
                shape = shape
            )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (selected && showSelectionOverlay) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = Color.White
                )
            } else if (photoData != null) {
                val painter = rememberAsyncImagePainter(
                    model = ImageRequest
                        .Builder(context)
                        .data(photoData)
                        .crossfade(true)
                        .build()
                )

                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (!displayName.isNullOrBlank()) {
                AvatarInitials(
                    displayName = displayName,
                    textColor = baseColor,
                    initials = initials,
                )
            } else {
                Icon(
                    imageVector = Icons.TwoTone.Person,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = baseColor
                )
            }
        }
    }
}

/**
 * Displays initials styled for an avatar.
 */
@Composable
private fun AvatarInitials(
    displayName: String,
    textColor: Color,
    initials: String? = null,
) {
    val displayInitials = initials ?: run {
        val names = displayName.split(" ")
        val firstInitial = names
            .firstOrNull()
            ?.firstOrNull()
            ?.uppercase() ?: ""
        val lastInitial = if (names.size > 1) {
            names
                .lastOrNull()
                ?.firstOrNull()
                ?.uppercase() ?: ""
        } else ""
        "$firstInitial$lastInitial"
    }

    Text(
        text = displayInitials,
        modifier = Modifier.padding(3.dp),
        style = MaterialTheme.typography.titleLarge.copy(
            fontFamily = NotoMonoFontFamily,
            fontWeight = FontWeight.Bold,
        ),
        color = textColor,
        autoSize = TextAutoSize.StepBased(),
        textAlign = TextAlign.Justify,
    )
}

/**
 * A standard list item for CC screens.
 */
@Composable
fun CCListItem(
    headlineText: String,
    modifier: Modifier = Modifier,
    headlineIsMultiline: Boolean = false,
    icon: ImageVector? = null,
    @StringRes iconDescription: Int? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    baseColor: Color = currentThemeColor(),
    overlineText: String? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (RowScope.() -> Unit)? = null,
) {
    ListItem(
        headlineContent = {
            Text(
                headlineText,
                overflow = TextOverflow.Ellipsis,
                maxLines = if (headlineIsMultiline) Int.MAX_VALUE else 1,
            )
        },
        modifier = modifier,
        overlineContent = {
            if (!overlineText.isNullOrBlank())
                Text(
                    overlineText,
                    color = baseColor.text(),
                    style = MaterialTheme.typography.labelSmall
                )
        },
        supportingContent = supportingContent,
        leadingContent = {
            if (icon != null)
                Icon(
                    imageVector = icon,
                    contentDescription = iconDescription?.let { stringResource(it) },
                    tint = iconTint,
                )
            else
                Spacer(Modifier.width(24.dp))
        },
        trailingContent = {
            if (trailingContent != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        2.dp,
                        Alignment.End
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    content = trailingContent
                )
            }
        },
    )
}

@Composable
fun CCScrollableColumn(
    systemPadding: PaddingValues,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(Dimens.smSpacing),
    spacing: Dp = Dimens.lgSpacing,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(systemPadding)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

/**
 * Centers [content] and caps its width at [maxWidth], so forms/settings screens don't stretch
 * edge-to-edge on wide/tablet screens. A no-op on narrower screens, since the cap only binds
 * once available width exceeds it.
 */
@Composable
fun CCWidthClampedBox(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 600.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxHeight(),
        ) {
            content()
        }
    }
}


@Preview(device = "spec:width=411dp,height=891dp")
@Composable
fun ContactSectionPreview() {
    CorvidContactsTheme {
        val baseColor = ContactColors.palette[6]
        ContactSection(
            title = "Contact Section",
            baseColor = baseColor,
            icon = Icons.TwoTone.CrisisAlert,
        ) {
            Section(
                title = "Section",
                baseColor = baseColor,
                icon = Icons.TwoTone.Landslide,
            ) {
                ContactAvatar(
                    displayName = "Marky Mark",
                    baseColor = baseColor
                )

                CCListItem(
                    headlineText = "headlineText",
                    icon = Icons.TwoTone.AutoMode,
                    baseColor = baseColor,
                    overlineText = "overlineText",
                    supportingContent = {
                        Text("supportingContent")
                    },
                    trailingContent = {
                        Icon(
                            Icons.TwoTone.SouthAmerica,
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun ContactSectionDarkPreview() {
    CorvidContactsTheme {
        val baseColor = ContactColors.palette[6]
        ContactSection(
            title = "Contact Section",
            baseColor = baseColor,
            icon = Icons.TwoTone.CrisisAlert,
        ) {
            Section(
                title = "Section",
                baseColor = baseColor,
                icon = Icons.TwoTone.Landslide,
            ) {
                ContactAvatar(
                    displayName = "Marky Mark",
                    baseColor = baseColor
                )

                CCListItem(
                    headlineText = "headlineText",
                    icon = Icons.TwoTone.AutoMode,
                    baseColor = baseColor,
                    overlineText = "overlineText",
                    supportingContent = {
                        Text("supportingContent")
                    },
                    trailingContent = {
                        Icon(
                            Icons.TwoTone.SouthAmerica,
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}