// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.common_ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.extensions.onBackground
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.ThemePreview
import dev.benica.corvidcontacts.ui.theme.currentThemeColor

/**
 * A dropdown menu box with an integrated text field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CCExposedDropdownMenuBox(
    textBoxLabel: String,
    currentValue: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit = {},
    onDismissRequest: () -> Unit = {},
    readOnly: Boolean = true,
    enabled: Boolean = true,
    showTrailingIcon: Boolean = true,
    trailingIcon: (@Composable () -> Unit)? = null,
    textFieldModifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    content: @Composable ColumnScope.() -> Unit,
) {
    fun dismiss() {
        onDismissRequest()
        onExpandedChange(false)
    }

    BackHandler(enabled = expanded) { dismiss() }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) onExpandedChange(it) },
        modifier = modifier,
    ) {
        CCOutlinedTextField(
            value = currentValue,
            onValueChange = onValueChange,
            readOnly = readOnly,
            enabled = enabled,
            label = textBoxLabel,
            modifier = textFieldModifier
                .fillMaxWidth()
                .menuAnchor(
                    if (readOnly) ExposedDropdownMenuAnchorType.PrimaryNotEditable
                    else ExposedDropdownMenuAnchorType.PrimaryEditable
                ),
            trailingIcon = trailingIcon ?: if (showTrailingIcon) {
                { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            } else null,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = ::dismiss,
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            content = content
        )
    }
}

/**
 * A theme-aware outlined text field with custom styling for CC.
 */
@Composable
fun CCOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    baseColor: Color = currentThemeColor(),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isFloating = isFocused || value.isNotEmpty()

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth(),
        enabled = enabled,
        readOnly = readOnly,
        label = ccTextFieldLabel(
            label,
            isFloating
        ),
        placeholder = ccTextFieldPlaceholder(placeholder),
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText?.let { { Text(it) } },
        isError = isError,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        shape = MaterialTheme.shapes.large,
        colors = contactTextFieldColors(baseColor),
        interactionSource = interactionSource,
    )
}

/**
 * [TextFieldValue] variant of [CCOutlinedTextField] - needed whenever a field reformats its own
 * text as the user types (e.g. inserting phone-number separators), since only [TextFieldValue]
 * carries cursor position and lets the caller pin it explicitly. The plain-[String] overload
 * above can't do this - Compose has to guess where the cursor goes after a same-callback text
 * replacement, and that guess is wrong often enough to visibly transpose digits.
 */
@Composable
fun CCOutlinedTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    baseColor: Color = currentThemeColor(),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isFloating = isFocused || value.text.isNotEmpty()

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth(),
        enabled = enabled,
        readOnly = readOnly,
        label = ccTextFieldLabel(
            label,
            isFloating
        ),
        placeholder = ccTextFieldPlaceholder(placeholder),
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText?.let { { Text(it) } },
        isError = isError,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        shape = MaterialTheme.shapes.large,
        colors = contactTextFieldColors(baseColor),
        interactionSource = interactionSource,
    )
}

@Composable
private fun ccTextFieldLabel(
    label: String?,
    isFloating: Boolean,
): (@Composable () -> Unit)? {
    if (label == null) return null
    return {
        val surfaceColor = MaterialTheme.colorScheme.surface
        Text(
            label,
            modifier = if (isFloating) {
                Modifier
                    .drawBehind {
                        val gap = 4.dp.toPx()
                        drawRect(
                            color = surfaceColor,
                            topLeft = Offset(
                                x = -gap,
                                y = size.height / 2 + 3
                            ),
                            size = Size(
                                width = size.width + gap * 2,
                                height = size.height / 2
                            )
                        )
                    }
                    .background(
                        color = surfaceColor,
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp
                        )
                    )
                    .padding(
                        horizontal = Dimens.smSpacing,
                        vertical = Dimens.minSpacing
                    )
            } else Modifier,
        )
    }
}

// No explicit color, so contactTextFieldColors()'s placeholder color actually applies.
private fun ccTextFieldPlaceholder(placeholder: String?): (@Composable () -> Unit)? =
    placeholder?.let {
        { Text(it) }
    }

@ThemePreview
@Composable
fun CCExposedDropdownMenuBoxPreview() {
    CorvidContactsTheme {
        val baseColor = ContactColors.palette[3]
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            CCExposedDropdownMenuBox(
                textBoxLabel = "Label",
                currentValue = "Current",
                expanded = true,
                onExpandedChange = {},
            ) {
                Column(modifier = Modifier.padding(Dimens.outerSpacing)) {
                    Text("Item 1", color = baseColor.onBackground())
                    Text("Item 2", color = baseColor.onBackground())
                    Text("Item 3", color = baseColor.onBackground())
                }
            }
        }
    }
}
