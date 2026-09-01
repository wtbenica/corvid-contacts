// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.settings

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.AirlineSeatLegroomReduced
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dev.benica.corvidcontacts.BuildConfig
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.ui.EULA_URL
import dev.benica.corvidcontacts.ui.PRIVACY_POLICY_URL
import dev.benica.corvidcontacts.ui.contacts.common_ui.BackNavButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCIconButton
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCScaffold
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCWidthClampedBox
import dev.benica.corvidcontacts.ui.contacts.common_ui.ScreenChrome
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onShowLicenses: () -> Unit,
    modifier: Modifier = Modifier,
    showScaffold: Boolean = true,
    onChromeChange: ((ScreenChrome) -> Unit)? = null,
) {
    val context = LocalContext.current
    fun openUrl(url: String) {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                url.toUri()
            )
        )
    }

    val chromeTitle = stringResource(R.string.about_title)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.outerSpacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_about_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(150.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.about_description),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            InfoCard(
                title = stringResource(R.string.about_section_license),
                icon = Icons.Rounded.Description,
                content = stringResource(R.string.about_license_text)
            )

            Spacer(modifier = Modifier.height(32.dp))

            InfoCard(
                title = stringResource(R.string.about_section_libraries),
                icon = Icons.Rounded.Description,
                content = stringResource(R.string.about_action_show_licenses),
                action = SimpleAction(
                    icon = Icons.Rounded.ChevronRight,
                    contentDescription = R.string.about_action_show_licenses,
                    action = onShowLicenses
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            InfoCard(
                title = stringResource(R.string.about_section_privacy_policy),
                icon = Icons.Rounded.Description,
                content = stringResource(R.string.about_privacy_policy_text),
                action = SimpleAction(
                    icon = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = R.string.about_action_open_in_browser,
                    action = { openUrl(PRIVACY_POLICY_URL) }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            InfoCard(
                title = stringResource(R.string.about_section_eula),
                icon = Icons.Rounded.Description,
                content = stringResource(R.string.about_eula_text),
                action = SimpleAction(
                    icon = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = R.string.about_action_open_in_browser,
                    action = { openUrl(EULA_URL) }
                )
            )
        }
        }
    }

    if (showScaffold) {
        CCScaffold(
            modifier = modifier,
            title = chromeTitle,
            navigationIcon = chromeNavigationIcon,
            content = bodyContent
        )
    } else {
        bodyContent(PaddingValues())
    }
}

data class SimpleAction(
    val action: () -> Unit,
    val icon: ImageVector,
    val contentDescription: Int,
)

@Composable
private fun InfoCard(
    title: String,
    icon: ImageVector,
    content: String,
    action: SimpleAction? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.medium
    ) {
        Row {
            Column(
                modifier = Modifier
                    .padding(Dimens.innerSpacing)
                    .weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            action?.let {
                CCIconButton(
                    icon = it.icon,
                    contentDescription = it.contentDescription,
                    onClick = it.action,
                    modifier = Modifier.padding(
                        top = Dimens.innerSpacing,
                        end = Dimens.innerSpacing
                    )
                )
            }
        }
    }
}

@Preview
@Composable
fun AboutScreenPreview() {
    CorvidContactsTheme {
        AboutScreen(
            onBack = {},
            onShowLicenses = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview
@Composable
fun InfoCardPreview() {
    CorvidContactsTheme {
        InfoCard(
            title = "Whatevs",
            icon = Icons.Outlined.AirlineSeatLegroomReduced,
            content = "This is a preview of an info card",
            action = SimpleAction(
                action = {},
                icon = Icons.Rounded.ChevronRight,
                contentDescription = R.string.about_section_license
            )
        )
    }
}