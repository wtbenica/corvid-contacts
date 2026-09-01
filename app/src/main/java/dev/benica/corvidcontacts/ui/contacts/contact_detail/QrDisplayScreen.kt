// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.contact_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lightspark.composeqr.QrCodeView
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.data.local.ContactWithAddressBook
import dev.benica.corvidcontacts.ui.contacts.ContactColors
import dev.benica.corvidcontacts.ui.contacts.common_ui.BackNavButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCScaffold
import dev.benica.corvidcontacts.ui.contacts.common_ui.ScreenChrome
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.ThemePreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrDisplayScreen(
    vcard: String,
    contactWithBook: ContactWithAddressBook?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showScaffold: Boolean = true,
    onChromeChange: ((ScreenChrome) -> Unit)? = null,
) {
    val displayName =
        contactWithBook?.contact?.getEffectiveDisplayName()
            ?: stringResource(R.string.list_error_no_name)
    val baseColor = ContactColors.resolveContactColor(contactWithBook)

    val chromeTitle = stringResource(R.string.common_qr_code)
    val chromeNavigationIcon: @Composable () -> Unit = { BackNavButton(onBack) }

    if (!showScaffold) {
        SideEffect {
            onChromeChange?.invoke(
                ScreenChrome(
                    title = chromeTitle,
                    navigationIcon = chromeNavigationIcon,
                )
            )
        }
    }

    val bodyContent: @Composable (PaddingValues) -> Unit = { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(padding)
                .padding(Dimens.smSpacing),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Dimens.outerSpacing),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = displayName,
                    modifier = Modifier.padding(Dimens.smSpacing),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )

                Surface(
                    modifier = Modifier
                        .size(350.dp),
                    color = Color.Black,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = Color.White,
                        shadowElevation = 8.dp
                    ) {
                        Box(
                            modifier = Modifier.padding(Dimens.innerSpacing),
                            contentAlignment = Alignment.Center
                        ) {
                            QrCodeView(
                                data = vcard,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.common_qr_scan_instruction),
                    modifier = Modifier.padding(Dimens.smSpacing),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }

    if (showScaffold) {
        CCScaffold(
            title = chromeTitle,
            modifier = modifier.fillMaxSize(),
            navigationIcon = chromeNavigationIcon,
            baseColor = baseColor,
            content = bodyContent
        )
    } else {
        bodyContent(PaddingValues())
    }
}

@ThemePreview
@Composable
private fun QrDisplayScreenPreview() {
    // Fixed vcardString to use proper newlines in the raw string for valid QR generation
    val vcardString = """
        BEGIN:VCARD
        VERSION:3.0
        FN:John Doe
        TEL;TYPE=CELL:+1234567890
        EMAIL:john.doe@example.com
        END:VCARD
    """.trimIndent()

    CorvidContactsTheme {
        QrDisplayScreen(
            contactWithBook = null,
            vcard = vcardString,
            onBack = {},
        )
    }
}