// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts

import androidx.annotation.StringRes
import dev.benica.corvidcontacts.R
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.cert.CertPathValidatorException
import java.security.cert.CertificateException
import java.security.cert.CertificateExpiredException
import javax.net.ssl.SSLPeerUnverifiedException

/**
 * The message that tells the user why a sync failed, or just [R.string.common_error_sync_title]
 * when the cause isn't one they can act on.
 */
@StringRes
internal fun syncFailureMessage(error: Throwable?): Int {
    val causes = generateSequence(error) { it.cause }.toList()
    return when {
        causes.any { it is CertificateExpiredException } -> R.string.sync_error_certificate_expired
        causes.any {
            it is CertificateException || it is CertPathValidatorException || it is SSLPeerUnverifiedException
        } -> R.string.sync_error_certificate

        causes.any {
            it is UnknownHostException || it is ConnectException || it is SocketTimeoutException
        } -> R.string.sync_error_unreachable

        else -> R.string.common_error_sync_title
    }
}
