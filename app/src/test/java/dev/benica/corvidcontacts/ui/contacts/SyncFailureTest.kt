// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.ui.contacts

import dev.benica.corvidcontacts.R
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.cert.CertPathValidatorException
import java.security.cert.CertificateExpiredException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException

class SyncFailureTest {

    @Test
    fun `an expired certificate is named, however deep it is wrapped`() {
        val expired = SSLHandshakeException("Chain validation failed")
            .initCause(CertPathValidatorException("Could not validate", CertificateExpiredException("expired")))

        assertEquals(R.string.sync_error_certificate_expired, syncFailureMessage(expired))
    }

    @Test
    fun `a certificate that isn't trusted is named`() {
        val untrusted = SSLHandshakeException("Trust anchor not found")
            .initCause(CertPathValidatorException("Trust anchor for certification path not found."))

        assertEquals(R.string.sync_error_certificate, syncFailureMessage(untrusted))
        assertEquals(R.string.sync_error_certificate, syncFailureMessage(SSLPeerUnverifiedException("Hostname mismatch")))
    }

    @Test
    fun `a server that can't be reached is named`() {
        listOf(UnknownHostException(), ConnectException(), SocketTimeoutException()).forEach {
            assertEquals(R.string.sync_error_unreachable, syncFailureMessage(it))
            assertEquals(R.string.sync_error_unreachable, syncFailureMessage(IOException("failed", it)))
        }
    }

    @Test
    fun `other failures keep the plain title`() {
        assertEquals(R.string.common_error_sync_title, syncFailureMessage(Exception("Failed Step 1")))
        assertEquals(R.string.common_error_sync_title, syncFailureMessage(null))
    }
}
