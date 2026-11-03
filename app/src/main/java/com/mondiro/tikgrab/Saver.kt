package com.mondiro.tikgrab

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import okhttp3.Request
import java.io.IOException

/** Lưu file vào Thư viện máy (MediaStore) — không cần quyền bộ nhớ. */
object Saver {
    enum class Kind(val mime: String, val ext: String) {
        VIDEO("video/mp4", "mp4"), AUDIO("audio/mpeg", "mp3"), IMAGE("image/jpeg", "jpg")
    }

    fun folderFor(kind: Kind, base: String): String = when (kind) {
        Kind.VIDEO -> "$base/TikGrab"
        Kind.IMAGE -> if (base == Prefs.FOLDER_DCIM) "DCIM/TikGrab" else "Pictures/TikGrab"
        Kind.AUDIO -> "Music/TikGrab"
    }

    private fun collection(kind: Kind): Uri = when (kind) {
        Kind.VIDEO -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        Kind.AUDIO -> MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        Kind.IMAGE -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    }

    fun safeName(s: String): String =
        s.replace(Regex("""[^\p{L}\p{N}._-]+"""), "_").trim('_').take(40).ifBlank { "tiktok" }

    /** Tải [url] rồi ghi thẳng vào Thư viện. [onProgress] nhận 0..1, hoặc -1 khi chưa biết dung lượng. */
    fun download(
        ctx: Context, url: String, kind: Kind, base: String, fileName: String,
        onProgress: (Float) -> Unit,
    ): Uri {
        val cr = ctx.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.${kind.ext}")
            put(MediaStore.MediaColumns.MIME_TYPE, kind.mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, folderFor(kind, base))
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = cr.insert(collection(kind), values) ?: throw IOException("Không tạo được file")
        try {
            val req = Request.Builder().url(url)
                .header("User-Agent", TikApi.UA)
                .header("Referer", "https://www.tikwm.com/")
                .build()
            TikApi.http.newCall(req).execute().use { r ->
                if (!r.isSuccessful) throw IOException("HTTP ${r.code}")
                val body = r.body ?: throw IOException("Rỗng")
                val total = body.contentLength()
                val type = body.contentType()?.toString().orEmpty()
                if (type.startsWith("text/html")) throw IOException("Link tải không hợp lệ")
                cr.openOutputStream(uri)?.use { out ->
                    body.byteStream().use { input ->
                        val buf = ByteArray(64 * 1024)
                        var done = 0L
                        var last = 0L
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            val now = System.currentTimeMillis()
                            if (now - last > 150) {
                                last = now
                                onProgress(if (total > 0) done.toFloat() / total else -1f)
                            }
                        }
                        if (done < 1024) throw IOException("File tải về bị lỗi")
                    }
                } ?: throw IOException("Không ghi được file")
            }
            cr.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            onProgress(1f)
            return uri
        } catch (e: Exception) {
            runCatching { cr.delete(uri, null, null) }
            throw e
        }
    }

    fun exists(ctx: Context, uri: Uri): Boolean = runCatching {
        ctx.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns._ID), null, null, null)
            ?.use { it.moveToFirst() } ?: false
    }.getOrDefault(false)
}
