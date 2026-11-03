package com.mondiro.tikgrab.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import android.util.Size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.mondiro.tikgrab.TikApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

private val cache = object : LruCache<String, Bitmap>(48 * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap) = value.byteCount
}

/** Ảnh thu nhỏ của file trong Thư viện. */
@Composable
fun rememberThumb(uri: Uri): State<Bitmap?> {
    val ctx = LocalContext.current
    val key = uri.toString()
    return produceState(cache.get(key), key) {
        if (value == null) value = withContext(Dispatchers.IO) { load(ctx, uri)?.also { cache.put(key, it) } }
    }
}

private fun load(ctx: Context, uri: Uri): Bitmap? = runCatching {
    ctx.contentResolver.loadThumbnail(uri, Size(360, 640), null)
}.getOrNull()

/** Ảnh bìa video từ mạng (lúc đang tải). */
@Composable
fun rememberRemote(url: String?): State<Bitmap?> = produceState(url?.let { cache.get(it) }, url) {
    if (url != null && value == null) value = withContext(Dispatchers.IO) {
        runCatching {
            TikApi.http.newCall(Request.Builder().url(url).header("User-Agent", TikApi.UA).build()).execute().use { r ->
                r.body?.bytes()?.let { b ->
                    val o = BitmapFactory.Options().apply { inSampleSize = 2 }
                    BitmapFactory.decodeByteArray(b, 0, b.size, o)
                }
            }
        }.getOrNull()?.also { cache.put(url, it) }
    }
}
