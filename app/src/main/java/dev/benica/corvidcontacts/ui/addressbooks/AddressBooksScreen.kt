// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.addressbooks

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.AddressBookEntity
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCScreenFrame
import dev.benica.corvidcontacts.ui.contacts.common_ui.ScreenChrome
import dev.benica.corvidcontacts.ui.contacts.contact_list.components.dialogs.CreateAddressBookDialog
import dev.benica.corvidcontacts.ui.theme.Dimens
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Every address book in a reorderable list. Tapping one opens its [AddressBookSettingsScreen];
 * the add button creates a new one. This is the primary way to manage address books, reached from
 * the contact list's filter sheet and linked from Settings.
 */
@Composable
fun AddressBooksScreen(
    addressBooks: List<AddressBookEntity>,
    hasServerConnection: Boolean,
    onBookClick: (AddressBookEntity) -> Unit,
    onUpdateOrder: (List<AddressBookEntity>) -> Unit,
    onCreateAddressBook: suspend (String, Color, Boolean, String?) -> Result<AddressBookEntity>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showScaffold: Boolean = true,
    onChromeChange: ((ScreenChrome) -> Unit)? = null,
) {
    val context = LocalContext.current
    val submit = rememberSubmitState()
    val hapticFeedback = LocalHapticFeedback.current
    val genericErrorMessage = stringResource(R.string.settings_address_book_generic_error)
    val title = stringResource(R.string.settings_section_address_books)
    val hasContactsPermission = rememberHasContactsPermission()

    var showCreateDialog by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val list = addressBooks.toMutableList()
        list.add(to.index, list.removeAt(from.index))
        onUpdateOrder(list)
        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    val addIcon: @Composable () -> Unit = {
        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.settings_address_book_add))
    }

    CCScreenFrame(
        title = title,
        onBack = onBack,
        showScaffold = showScaffold,
        onChromeChange = onChromeChange,
        modifier = modifier,
        fabContent = addIcon,
        onFabClick = { showCreateDialog = true },
    ) { padding ->
        LazyColumn(
            state = lazyListState,
            // The same inset the app Settings page gets from its sections, without section headers.
            contentPadding = PaddingValues(horizontal = Dimens.lgSpacing, vertical = Dimens.lgSpacing),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            itemsIndexed(addressBooks, key = { _, book -> book.href }) { _, book ->
                ReorderableItem(reorderableState, key = book.href) { isDragging ->
                    Surface(
                        shadowElevation = if (isDragging) 4.dp else 0.dp,
                        modifier = Modifier.alpha(if (book.isVisible) 1f else 0.5f)
                    ) {
                        val bookColor = Color(book.colorInt)
                        val status = listOfNotNull(
                            if (book.isLocal) stringResource(R.string.settings_address_book_local_badge) else null,
                            if (!book.isVisible) stringResource(R.string.settings_address_book_hidden_label) else null,
                            if (book.shareWithSystem && hasContactsPermission) {
                                stringResource(
                                    R.string.address_book_shared_label_with_level,
                                    stringResource(book.systemContactsLevel.titleRes())
                                )
                            } else null,
                        )
                        ListItem(
                            headlineContent = {
                                Text(book.displayName ?: stringResource(R.string.settings_address_book_unnamed))
                            },
                            supportingContent = if (status.isNotEmpty()) {
                                { Text(status.joinToString(" · ")) }
                            } else null,
                            leadingContent = {
                                Surface(shape = CircleShape, color = bookColor.copy(alpha = 0.15f)) {
                                    Icon(
                                        imageVector = ContactColors.getIconForAddressBook(book.displayName, book.iconName),
                                        contentDescription = null,
                                        tint = bookColor,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            },
                            trailingContent = {
                                // Nothing to reorder with a single book, so no handle.
                                if (addressBooks.size > 1) {
                                    Icon(
                                        Icons.Rounded.DragHandle,
                                        contentDescription = stringResource(R.string.settings_address_book_reorder),
                                        modifier = Modifier.draggableHandle(
                                            onDragStarted = {
                                                hapticFeedback.performHapticFeedback(
                                                    HapticFeedbackType.GestureThresholdActivate
                                                )
                                            },
                                            onDragStopped = {
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                            }
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.clickable { onBookClick(book) },
                        )
                    }
                }
            }
        }
    }


    if (showCreateDialog) {
        CreateAddressBookDialog(
            isSubmitting = submit.isSubmitting,
            hasServerConnection = hasServerConnection,
            existingColors = addressBooks.map { it.colorInt },
            onConfirm = { name, color, forceLocal, iconName ->
                submit.run {
                    if (onCreateAddressBook(name, color, forceLocal, iconName).isSuccess) {
                        showCreateDialog = false
                    } else {
                        Toast.makeText(context, genericErrorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { if (!submit.isSubmitting) showCreateDialog = false }
        )
    }
}
