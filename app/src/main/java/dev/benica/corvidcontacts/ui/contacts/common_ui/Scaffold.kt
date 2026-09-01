// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts.common_ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.benica.corvidcontacts.R
import dev.benica.corvidcontacts.extensions.background
import dev.benica.corvidcontacts.ui.theme.Dimens
import dev.benica.corvidcontacts.ui.theme.LocalThemeColor
import dev.benica.corvidcontacts.ui.theme.currentThemeColor

/**
 * A standard top app bar for the CC app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CCTopAppBar(
    title: String,
    icon: ImageVector? = null,
    navigationIcon: @Composable (() -> Unit) = { },
    actions: @Composable (RowScope.() -> Unit) = { },
    baseColor: Color = currentThemeColor(),
) {
    TopAppBar(
        title = {
            if (icon != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.smSpacing)
                ) {
                    Icon(icon, contentDescription = null)
                    Text(title)
                }
            } else {
                Text(title)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = baseColor.background(),
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

/**
 * Chrome (title, nav icon, actions, FAB) a screen exposes instead of drawing its own [CCScaffold]
 * when embedded in a shell that renders one shared Scaffold across multiple screens.
 */
data class ScreenChrome(
    val title: String,
    val navigationIcon: @Composable () -> Unit = {},
    val actions: @Composable RowScope.() -> Unit = {},
    val fabContent: (@Composable () -> Unit)? = null,
    val onFabClick: (() -> Unit)? = null,
)

/**
 * A navigation button to go back.
 */
@Composable
fun BackNavButton(onBack: () -> Unit) {
    CCIconButton(
        icon = Icons.AutoMirrored.Rounded.ArrowBack,
        contentDescription = R.string.common_back,
        onClick = onBack,
        color = MaterialTheme.colorScheme.onSurface
    )
}

/**
 * A standard scaffold for CC screens.
 */
@Composable
fun CCScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable (() -> Unit) = {},
    snackbarHost: @Composable (() -> Unit) = {},
    baseColor: Color = currentThemeColor(),
    onFabClick: (() -> Unit)? = null,
    fabContent: @Composable () -> Unit = {},
    fabPosition: FabPosition = FabPosition.End,
    content: @Composable (PaddingValues) -> Unit,
) {
    CompositionLocalProvider(LocalThemeColor provides baseColor) {
        Scaffold(
            modifier = modifier,
            topBar = topBar,
            bottomBar = bottomBar,
            snackbarHost = {
                Box(Modifier.imePadding()) {
                    snackbarHost()
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            floatingActionButton = {
                if (onFabClick != null)
                    CCFloatingActionButton(
                        onClick = onFabClick,
                        baseColor = baseColor,
                        content = fabContent
                    )
            },
            floatingActionButtonPosition = fabPosition,
            content = content
        )
    }
}

/**
 * A scaffold with a title and optional navigation icon and actions.
 */
@Composable
fun CCScaffold(
    title: String,
    modifier: Modifier = Modifier,
    topBarActions: @Composable (RowScope.() -> Unit) = { },
    navigationIcon: @Composable (() -> Unit) = { },
    bottomBar: @Composable (() -> Unit) = {},
    snackbarHost: @Composable (() -> Unit) = {},
    baseColor: Color = currentThemeColor(),
    onFabClick: (() -> Unit)? = null,
    fabContent: @Composable () -> Unit = { },
    fabPosition: FabPosition = FabPosition.End,
    content: @Composable (PaddingValues) -> Unit,
) {
    CCScaffold(
        modifier = modifier,
        topBar = {
            CCTopAppBar(
                title = title,
                navigationIcon = navigationIcon,
                actions = topBarActions,
                baseColor = baseColor
            )
        },
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        baseColor = baseColor,
        onFabClick = onFabClick,
        fabContent = fabContent,
        fabPosition = fabPosition,
        content = content
    )
}

