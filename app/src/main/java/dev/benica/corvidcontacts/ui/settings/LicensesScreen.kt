// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.ui.contacts.common_ui.BackNavButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCScaffold
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCWidthClampedBox
import dev.benica.corvidcontacts.ui.contacts.common_ui.ScreenChrome
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showScaffold: Boolean = true,
    onChromeChange: ((ScreenChrome) -> Unit)? = null,
) {
    val chromeTitle = stringResource(R.string.licenses_title)
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
        CCWidthClampedBox(modifier = Modifier.padding(padding)) {
            val libraries by produceLibraries(R.raw.aboutlibraries)

            LibrariesContainer(
                libraries = libraries,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    if (showScaffold) {
        CCScaffold(
            title = chromeTitle,
            modifier = modifier,
            navigationIcon = chromeNavigationIcon,
            content = bodyContent
        )
    } else {
        bodyContent(PaddingValues())
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = true
)
@Composable
fun LicensesScreenPreview() {
    CorvidContactsTheme {
        LicensesScreen(
            onBack = {}
        )
    }
}