// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.common_ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.benica.corvidcontacts.ui.theme.Dimens

/**
 * Actions pinned to the bottom of a screen, above the keyboard and the navigation bar, at a
 * readable width. `imePadding` comes before `navigationBarsPadding` so the navigation bar isn't
 * counted twice while the keyboard is showing.
 */
@Composable
fun CCBottomActionBar(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = Dimens.lgSpacing,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding, vertical = Dimens.smSpacing),
                verticalArrangement = Arrangement.spacedBy(Dimens.xsSpacing),
                content = content
            )
        }
    }
}
