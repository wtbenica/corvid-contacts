// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.ui.EULA_URL
import dev.benica.corvidcontacts.ui.PRIVACY_POLICY_URL
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCCardBordered
import dev.benica.corvidcontacts.ui.contacts.common_ui.CCWidthClampedBox
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.ThemePreview

@Composable
fun WelcomeScreen(
    onSignInClick: () -> Unit,
    onUseLocallyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CCWidthClampedBox(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.innerSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.smSpacing)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_about_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(150.dp)
                        .align(Alignment.CenterHorizontally)
                )
                Text(
                    text = stringResource(R.string.welcome_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.welcome_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // The one real decision on this screen: where contacts live. Both choices look the
            // same on purpose, since neither is the "right" one.
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.smSpacing)
            ) {
                Text(
                    text = stringResource(R.string.welcome_choose_prompt),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                WelcomeChoiceCard(
                    icon = Icons.Rounded.PhoneAndroid,
                    title = stringResource(R.string.welcome_action_use_locally),
                    description = stringResource(R.string.welcome_use_locally_desc),
                    onClick = onUseLocallyClick,
                )
                WelcomeChoiceCard(
                    icon = Icons.Rounded.CloudSync,
                    title = stringResource(R.string.welcome_action_sign_in),
                    description = stringResource(R.string.welcome_sign_in_desc),
                    onClick = onSignInClick,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.height(Dimens.smSpacing))
            LegalLinksFooter()

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LegalLinksFooter() {
    val linkStyle = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline
        )
    )
    Text(
        text = buildAnnotatedString {
            append(stringResource(R.string.welcome_legal_prefix))
            append(" ")
            withLink(LinkAnnotation.Url(PRIVACY_POLICY_URL, linkStyle)) {
                append(stringResource(R.string.about_section_privacy_policy))
            }
            append(" ")
            append(stringResource(R.string.welcome_legal_and))
            append(" ")
            withLink(LinkAnnotation.Url(EULA_URL, linkStyle)) {
                append(stringResource(R.string.common_terms_of_use))
            }
            append(".")
        },
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.medSpacing)
    )
}

@Composable
private fun WelcomeChoiceCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    CCCardBordered(
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(Dimens.innerSpacing),
            horizontalArrangement = Arrangement.spacedBy(Dimens.lgSpacing),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@ThemePreview
@Composable
fun WelcomeScreenPreview() {
    CorvidContactsTheme {
        WelcomeScreen(
            onSignInClick = {},
            onUseLocallyClick = {}
        )
    }
}
