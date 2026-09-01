// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package dev.benica.corvidcontacts.utils

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dev.benica.corvidcontacts.BuildConfig
import java.io.File

/** Writes [vcard] to a cache file named [fileName] and launches Android's share sheet for it. */
fun shareVCard(
    context: Context,
    vcard: String,
    fileName: String,
    chooserTitle: String,
) {
    val cacheFile = File(
        context.cacheDir,
        fileName
    )
    cacheFile.writeText(vcard)
    val secureUri = FileProvider.getUriForFile(
        context,
        "${BuildConfig.APPLICATION_ID}.fileprovider",
        cacheFile
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/vcard"
        putExtra(
            Intent.EXTRA_STREAM,
            secureUri
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newRawUri(
            null,
            secureUri
        )
    }
    context.startActivity(
        Intent.createChooser(
            intent,
            chooserTitle
        )
    )
}
