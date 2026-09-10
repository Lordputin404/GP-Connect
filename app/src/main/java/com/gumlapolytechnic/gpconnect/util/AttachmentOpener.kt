package com.gumlapolytechnic.gpconnect.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.gumlapolytechnic.gpconnect.data.model.EventAttachment
import java.io.File

/**
 * Opens a calendar event attachment in an external app.
 *
 * The binary is fetched into the private cache by the repository
 * (auth-governed by storage.rules) and then shared read-only through the
 * app's [FileProvider] with an `ACTION_VIEW` intent keyed to the attachment's
 * MIME type. No specific viewer is assumed: if the device has none, the
 * caller is told via [OpenAttachmentResult.NoViewer] so it can show a
 * user-friendly message instead of crashing.
 */
object AttachmentOpener {

    /** Name of the private cache subdirectory holding downloaded attachments. */
    const val CACHE_DIR = "eventAttachments"

    sealed interface OpenAttachmentResult {
        /** An external app received the view intent. */
        data object Opened : OpenAttachmentResult
        /** The download failed (network, rules, Storage). */
        data class Failed(val cause: Exception) : OpenAttachmentResult
        /** No installed app can view this MIME type. */
        data object NoViewer : OpenAttachmentResult
    }

    fun open(
        context: Context,
        attachment: EventAttachment,
        downloaded: File,
    ): OpenAttachmentResult {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            downloaded,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, attachment.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return try {
            context.startActivity(intent)
            OpenAttachmentResult.Opened
        } catch (exception: ActivityNotFoundException) {
            OpenAttachmentResult.NoViewer
        } catch (exception: Exception) {
            OpenAttachmentResult.Failed(exception)
        }
    }

    /** The cache directory downloads land in; created on first use. */
    fun cacheDirectory(context: Context): File =
        File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
}
