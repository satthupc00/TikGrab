package com.mondiro.tikgrab

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException

/** Tự cập nhật từ GitHub Releases của repo satthupc00/tikgrab. */
object Updater {
    private const val API = "https://api.github.com/repos/satthupc00/tikgrab/releases/latest"

    data class Release(val version: String, val apkUrl: String, val notes: String)

    suspend fun latest(): Release? = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(API)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "TikGrab-Android")
            .build()
        TikApi.http.newCall(req).execute().use { r ->
            if (r.code == 404) return@withContext null
            if (!r.isSuccessful) throw IOException("HTTP ${r.code}")
            val j = JSONObject(r.body?.string().orEmpty())
            val tag = j.optString("tag_name").removePrefix("v").removePrefix("V")
            val assets = j.optJSONArray("assets")
            var apk: String? = null
            if (assets != null) for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.optString("name").endsWith(".apk", true)) { apk = a.optString("browser_download_url"); break }
            }
            if (tag.isBlank() || apk == null) null else Release(tag, apk, j.optString("body"))
        }
    }

    fun isNewer(remote: String, local: String = BuildConfig.VERSION_NAME): Boolean {
        val a = remote.split('.', '-').map { it.toIntOrNull() ?: 0 }
        val b = local.split('.', '-').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    suspend fun download(ctx: Context, rel: Release, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(ctx.cacheDir, "updates").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
        val out = File(dir, "TikGrab-${rel.version}.apk")
        val req = Request.Builder().url(rel.apkUrl).header("User-Agent", "TikGrab-Android").build()
        TikApi.http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw IOException("HTTP ${r.code}")
            val body = r.body ?: throw IOException("Rỗng")
            val total = body.contentLength()
            out.outputStream().use { o ->
                body.byteStream().use { i ->
                    val buf = ByteArray(64 * 1024); var done = 0L
                    while (true) {
                        val n = i.read(buf); if (n < 0) break
                        o.write(buf, 0, n); done += n
                        if (total > 0) onProgress(done.toFloat() / total)
                    }
                }
            }
        }
        out
    }

    /** Trả về false nếu cần người dùng bật quyền "Cài ứng dụng không rõ nguồn" trước. */
    fun install(ctx: Context, apk: File): Boolean {
        if (!ctx.packageManager.canRequestPackageInstalls()) {
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return false
        }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", apk)
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return true
    }
}
