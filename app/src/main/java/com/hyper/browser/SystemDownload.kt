package com.hyper.browser

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import java.net.URLDecoder

/** 下载文件名工具（原内联下载器仅保留的有用部分）。 */
object DownloadNames {

    fun fileNameFrom(url: String, disposition: String?): String {
        val raw = disposition?.let { d ->
            Regex("filename\\*?=(?:UTF-8''|\")?([^\";]+)").find(d)?.groupValues?.get(1)?.trim()
        } ?: runCatching { Uri.parse(url).lastPathSegment }.getOrNull()
        val name = runCatching { URLDecoder.decode(raw ?: "", "UTF-8") }
            .getOrDefault(raw ?: "")
            .ifBlank { "download_${System.currentTimeMillis()}" }
        return truncateName(name.replace(Regex("[\\\\/:*?\"<>|]"), "_"), 80)
    }

    /** 页面给定的原始文件名：只去掉路径分隔符和控制字符。 */
    fun originalName(raw: String): String {
        val cleaned = raw.replace(Regex("[\\\\/\\u0000-\\u001F]"), "_").trim()
        return truncateName(cleaned.ifBlank { "download_${System.currentTimeMillis()}" }, 255)
    }

    private fun truncateName(name: String, max: Int): String {
        if (name.length <= max) return name
        val dot = name.lastIndexOf('.')
        val ext = if (dot > 0 && name.length - dot <= 10) name.substring(dot) else ""
        return name.substring(0, (max - ext.length).coerceAtLeast(1)) + ext
    }

    fun withExtension(name: String, mime: String?): String {
        if (Regex("\\.[A-Za-z0-9]{1,8}$").containsMatchIn(name)) return name
        val type = mime?.substringBefore(';')?.trim()?.lowercase()
        if (type.isNullOrBlank() || type == "application/octet-stream") return name
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
        return if (ext.isNullOrBlank()) name else truncateName("$name.$ext", 80)
    }
}

/** 系统下载管理：HTTP 下载交给 DownloadManager，页面内文件登记进系统「下载」集合。 */
object SystemDownload {

    fun enqueue(context: Context, url: String, userAgent: String?, fileName: String, mimeType: String?) {
        runCatching {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                if (!userAgent.isNullOrBlank()) addRequestHeader("User-Agent", userAgent)
                if (!mimeType.isNullOrBlank()) setMimeType(mimeType.substringBefore(';').trim())
                setTitle(fileName)
                setDescription("由浏览器发起")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
        }
    }

    /** 打开系统「下载」应用（下载管理）。 */
    fun openDownloadsApp(context: Context) {
        val primary = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val ok = runCatching { context.startActivity(primary); true }.getOrDefault(false)
        if (!ok) {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("content://downloads/all_downloads"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    fun saveDataUri(context: Context, dataUri: String, fileName: String): Boolean = runCatching {
        val comma = dataUri.indexOf(',')
        if (comma < 0) return@runCatching false
        val meta = dataUri.substringAfter("data:").substringBefore(',')
        if (!meta.contains("base64", ignoreCase = true)) return@runCatching false
        val bytes = android.util.Base64.decode(dataUri.substring(comma + 1), android.util.Base64.DEFAULT)
        saveBytes(context, bytes, fileName, meta.substringBefore(';').ifBlank { null })
        true
    }.getOrDefault(false)

    fun saveFile(context: Context, file: java.io.File, fileName: String, mime: String?): Boolean =
        runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mime ?: "application/octet-stream")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val target = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return@runCatching false
            context.contentResolver.openOutputStream(target)?.use { out ->
                file.inputStream().use { it.copyTo(out, 64 * 1024) }
            } ?: return@runCatching false
            context.contentResolver.update(
                target,
                ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
                null, null,
            )
            true
        }.getOrDefault(false)

    private fun saveBytes(context: Context, bytes: ByteArray, fileName: String, mime: String?) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mime ?: "application/octet-stream")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val target = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("无法创建下载文件")
        context.contentResolver.openOutputStream(target)?.use { it.write(bytes) }
            ?: error("无法写入下载文件")
        context.contentResolver.update(
            target,
            ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
            null, null,
        )
    }
}
