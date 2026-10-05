// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.addressbooks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Runs one action at a time and says while it is running, so a repeated tap can't start it twice. */
@Stable
class SubmitState internal constructor(private val scope: CoroutineScope) {
    var isSubmitting by mutableStateOf(false)
        private set

    fun run(block: suspend () -> Unit) {
        if (isSubmitting) return
        isSubmitting = true
        scope.launch {
            try {
                block()
            } finally {
                isSubmitting = false
            }
        }
    }
}

@Composable
fun rememberSubmitState(): SubmitState {
    val scope = rememberCoroutineScope()
    return remember(scope) { SubmitState(scope) }
}
