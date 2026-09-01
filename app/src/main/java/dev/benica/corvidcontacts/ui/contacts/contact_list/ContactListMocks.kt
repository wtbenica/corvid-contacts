// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_list

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.data.local.ContactEntity
import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import dev.benica.corvidcontacts.data.model.Email
import dev.benica.corvidcontacts.data.model.Phone
import dev.benica.corvidcontacts.data.repository.AddressBookUploadResult
import dev.benica.corvidcontacts.ui.contacts.ContactsUiState

internal val mockContacts = listOf(
    ContactEntity(
        id = "1",
        displayName = "John Doe",
        firstName = "John",
        lastName = "Doe",
        emails = listOf(
            Email(
                value = "john@example.com",
                type = "home"
            )
        ),
        phones = listOf(
            Phone(
                value = "123/456-7890",
                type = "home"
            )
        ),
        photoUrl = null,
        etag = null,
    ),
    ContactEntity(
        id = "2",
        displayName = "Jane Doe",
        firstName = "Jane",
        lastName = "Doe",
        emails = listOf(
            Email(
                value = "jane@example.com",
                type = "home"
            )
        ),
        phones = listOf(
            Phone(
                value = "987/654-3210",
                type = "home"
            )
        ),
        photoUrl = null,
        etag = null
    ),
    ContactEntity(
        id = "3",
        displayName = "Bob Smith",
        firstName = "Bob",
        lastName = "Smith",
        emails = listOf(
            Email(
                value = "bob@example.com",
                type = "home"
            )
        ),
        phones = listOf(
            Phone(
                value = "555-555-5555",
                type = "home"
            )
        ),
        photoUrl = null,
        etag = null,
        colorInt = Color(0xFF6200EE).toArgb()
    )
)

internal val mockUiState = ContactsUiState.Success(mockContacts.map {
    ContactWithAddressBook(
        it,
        null
    )
})

internal val mockListActions = ContactListActions(
    onToggleSelection = {},
    onClearSelection = {},
    onDeselectAll = {},
    onSelectAll = {},
    onArchiveSelected = {},
    onDeleteSelected = {},
    onMoveSelected = {},
    onShareSelected = { "" },
    onRefresh = {},
    onSetUpSync = {},
    onLogout = {},
    onDeleteLocalData = {},
    onExportContacts = { "" },
    onToggleArchived = {},
    onSearchQueryChange = {},
    onGroupSelected = {},
    onSelectAddressBook = {},
    onUpdateAddressBookAppearance = { _, _, _ -> },
    onUpdateAddressBookOrder = {},
    onCreateAddressBook = { _, _, _ ->
        Result.success(
            AddressBookEntity(
                href = "preview",
                displayName = "Preview",
                colorInt = 0xFF6200EE.toInt()
            )
        )
    },
    onRenameAddressBook = { _, _ -> Result.success(Unit) },
    onDeleteAddressBook = { Result.success(Unit) },
    onUploadLocalAddressBook = { _, _ ->
        Result.success(
            AddressBookUploadResult(
                uploadedCount = 0,
                failedCount = 0,
                fullyCompleted = true
            )
        )
    },
    onToggleAddressBookVisibility = {},
    onUpdateGroupOrder = {},
    onRenameGroup = { _, _ -> },
    onAddSelectedToGroup = {},
    onRemoveSelectedFromGroup = {},
    onStartSelectingContact = {},
    onCancelSelectingContact = {},
    onClearSelectingContact = {},
    onContactSelected = {},
    onContactClick = { _ -> },
    onAddContact = {},
    onSettingsClick = {},
    onShareSelf = {},
    onShareSelfViaQr = {},
)
