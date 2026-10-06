// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui

import android.app.Application
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import dev.benica.corvidcontacts.ui.theme.CorvidContactsTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Base for tests of a single composable, run under Robolectric. Screens are found by their visible
 * text, from the same string resources the app uses, so a reworded string doesn't break a test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
abstract class ComposeTestBase {

    @get:Rule
    val compose = createComposeRule()

    protected val context: Context = ApplicationProvider.getApplicationContext()

    protected fun text(@StringRes id: Int, vararg args: Any): String = context.getString(id, *args)

    protected fun show(content: @Composable () -> Unit) {
        compose.setContent { CorvidContactsTheme { content() } }
    }

    protected fun node(@StringRes id: Int, vararg args: Any): SemanticsNodeInteraction =
        compose.onNodeWithText(text(id, *args))

    protected fun node(text: String): SemanticsNodeInteraction = compose.onNodeWithText(text)

    protected fun isShown(text: String): Boolean =
        compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()

    protected fun isShown(@StringRes id: Int, vararg args: Any): Boolean = isShown(text(id, *args))
}
