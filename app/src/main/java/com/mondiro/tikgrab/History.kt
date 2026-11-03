package com.mondiro.tikgrab

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class SavedItem(
    val id: String,
    val uri: Uri,
    val kind: Saver.Kind,
    val author: String,
    val title: String,
    val duration: Int,
    val time: Long,
)

/** Danh sách file đã tải (lưu trong bộ nhớ riêng của app). */
object History {
    private val lock = Any()
    private fun file(ctx: Context) = File(ctx.filesDir, "history.json")

    fun load(ctx: Context): List<SavedItem> = synchronized(lock) {
        val f = file(ctx)
        if (!f.exists()) return emptyList()
        runCatching {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                runCatching {
                    SavedItem(
                        id = o.getString("id"),
                        uri = Uri.parse(o.getString("uri")),
                        kind = Saver.Kind.valueOf(o.getString("kind")),
                        author = o.optString("author"),
                        title = o.optString("title"),
                        duration = o.optInt("duration"),
                        time = o.optLong("time"),
                    )
                }.getOrNull()
            }
        }.getOrDefault(emptyList())
    }

    private fun save(ctx: Context, items: List<SavedItem>) {
        val arr = JSONArray()
        items.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id); put("uri", it.uri.toString()); put("kind", it.kind.name)
                put("author", it.author); put("title", it.title); put("duration", it.duration); put("time", it.time)
            })
        }
        val f = file(ctx)
        val tmp = File(f.parentFile, f.name + ".tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(f)
    }

    fun add(ctx: Context, items: List<SavedItem>) = synchronized(lock) {
        save(ctx, items + load(ctx))
    }

    fun remove(ctx: Context, uris: Set<Uri>) = synchronized(lock) {
        save(ctx, load(ctx).filterNot { it.uri in uris })
    }

    /** Bỏ những file người dùng đã xoá ở ngoài app (vd trong Thư viện). */
    fun prune(ctx: Context): List<SavedItem> = synchronized(lock) {
        val all = load(ctx)
        val alive = all.filter { Saver.exists(ctx, it.uri) }
        if (alive.size != all.size) save(ctx, alive)
        alive
    }

    fun findVideo(ctx: Context, id: String): SavedItem? =
        load(ctx).firstOrNull { it.id == id && it.kind != Saver.Kind.AUDIO && Saver.exists(ctx, it.uri) }
}
