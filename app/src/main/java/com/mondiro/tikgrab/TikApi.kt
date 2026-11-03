package com.mondiro.tikgrab

import kotlinx.coroutines.delay
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class TikInfo(
    val id: String,
    val title: String,
    val author: String,
    val cover: String?,
    val duration: Int,
    val videoUrl: String?,
    val watermarkUrl: String?,
    val musicUrl: String?,
    val images: List<String>,
)

class FriendlyException(msg: String) : IOException(msg)

object TikApi {
    const val UA = "Mozilla/5.0 (Linux; Android 15; SM-F966B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0 Mobile Safari/537.36"
    private const val BASE = "https://www.tikwm.com"

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val linkRegex = Regex("""https?://[^\s"'<>]*tiktok\.com/[^\s"'<>]*""", RegexOption.IGNORE_CASE)

    /** Tìm link TikTok trong một đoạn chữ (TikTok chia sẻ kèm chữ mô tả). */
    fun extractUrl(text: String?): String? =
        text?.let { linkRegex.find(it)?.value?.trimEnd('.', ',', ')', '!', '?') }

    suspend fun fetch(url: String): TikInfo {
        var lastError: Exception? = null
        repeat(4) { attempt ->
            try {
                return fetchOnce(url)
            } catch (e: RateLimited) {
                lastError = e
                delay(1300L * (attempt + 1))
            } catch (e: FriendlyException) {
                throw e
            } catch (e: IOException) {
                lastError = e
                delay(800L * (attempt + 1))
            }
        }
        throw FriendlyException(
            if (lastError is RateLimited) "Máy chủ đang bận, thử lại sau ít giây"
            else "Không kết nối được, kiểm tra mạng rồi thử lại"
        )
    }

    private class RateLimited : IOException()

    private fun fetchOnce(url: String): TikInfo {
        val body = FormBody.Builder().add("url", url).add("hd", "1").build()
        val req = Request.Builder().url("$BASE/api/").post(body)
            .header("User-Agent", UA)
            .header("Accept", "application/json")
            .build()
        http.newCall(req).execute().use { r ->
            if (r.code == 429) throw RateLimited()
            if (!r.isSuccessful) throw IOException("HTTP ${r.code}")
            val j = JSONObject(r.body?.string().orEmpty())
            val code = j.optInt("code", -1)
            if (code != 0) {
                val msg = j.optString("msg")
                if (msg.contains("limit", true)) throw RateLimited()
                throw FriendlyException("Không lấy được video (link sai, video riêng tư hoặc đã bị xoá)")
            }
            val d = j.getJSONObject("data")
            val author = d.optJSONObject("author")
            val imgs = d.optJSONArray("images")
            val images = buildList { if (imgs != null) for (i in 0 until imgs.length()) abs(imgs.optString(i))?.let(::add) }
            return TikInfo(
                id = d.optString("id").ifBlank { System.currentTimeMillis().toString() },
                title = d.optString("title"),
                author = author?.optString("unique_id")?.takeIf { it.isNotBlank() }
                    ?: author?.optString("nickname").orEmpty(),
                cover = abs(d.optString("cover")) ?: abs(d.optString("origin_cover")),
                duration = d.optInt("duration", 0),
                videoUrl = abs(d.optString("hdplay")) ?: abs(d.optString("play")),
                watermarkUrl = abs(d.optString("wmplay")),
                musicUrl = abs(d.optString("music")),
                images = images,
            )
        }
    }

    /** Link tải đôi khi trả về dạng "/video/..." → thêm tên miền. */
    private fun abs(u: String?): String? = when {
        u.isNullOrBlank() || u == "null" -> null
        u.startsWith("http") -> u
        u.startsWith("/") -> BASE + u
        else -> "$BASE/$u"
    }
}
