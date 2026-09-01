// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_edit.sections

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCOutlinedTextField
import dev.benica.corvidcontacts.ui.contacts.common_ui.ContactSection
import dev.benica.corvidcontacts.ui.contacts.common_ui.Section
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.ThemePreview

/**
 * Section for editing work-related information like company and job title. Not collapsible -
 * with only two fields, a collapsed state would just be an empty card with a toggle in it.
 */
@Composable
fun EditWorkSection(
    company: String,
    onCompanyChange: (String) -> Unit,
    jobTitle: String,
    onJobTitleChange: (String) -> Unit,
    isReadOnly: Boolean,
) {
    ContactSection(
        title = stringResource(R.string.edit_section_work),
        content = {
            Section(
                title = stringResource(R.string.common_company),
                icon = Icons.Outlined.Business,
                modifier = Modifier.padding(vertical = Dimens.medSpacing),
                itemSpacing = 0.dp
            ) {
                CCOutlinedTextField(
                    value = company,
                    onValueChange = onCompanyChange,
                    label = stringResource(R.string.common_company),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isReadOnly,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                )
            }

            Section(
                title = stringResource(R.string.common_job_title),
                icon = Icons.Outlined.WorkOutline,
                modifier = Modifier.padding(vertical = Dimens.medSpacing),
                itemSpacing = 0.dp,
                headerModifier = Modifier.heightIn(min = Dimens.fieldMinHeight)
            ) {
                CCOutlinedTextField(
                    value = jobTitle,
                    onValueChange = onJobTitleChange,
                    label = stringResource(R.string.common_job_title),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    enabled = !isReadOnly,
                )
            }
        }
    )
}

@ThemePreview
@Composable
fun EditWorkSectionPreview() {
    CorvidContactsTheme {
        EditWorkSection(
            company = "Google",
            onCompanyChange = {},
            jobTitle = "Software Engineer",
            onJobTitleChange = {},
            isReadOnly = false,
        )
    }
}
