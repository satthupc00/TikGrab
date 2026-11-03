package com.mondiro.tikgrab

import android.app.Application
import android.content.ClipboardManager
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object Latest : UpdateState
    data class Available(val release: Updater.Release) : UpdateState
    data class Downloading(val release: Updater.Release, val progress: Float) : UpdateState
    data class Failed(val message: String) : UpdateState
}

data class WhatsNew(val version: String, val date: String, val new: List<String>, val improved: List<String>, val fixed: List<String>)

enum class Filter(val label: String) { ALL("Tất cả"), VIDEO("Video"), IMAGE("Ảnh"), AUDIO("Nhạc") }

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx get() = getApplication<Application>()
    val prefs = Prefs(app)

    var url by mutableStateOf("")
    var clipSuggestion by mutableStateOf<String?>(null)
    var library by mutableStateOf<List<SavedItem>>(emptyList())
    var filter by mutableStateOf(Filter.ALL)
    var update by mutableStateOf<UpdateState>(UpdateState.Idle)
    var message by mutableStateOf<String?>(null)

    // cài đặt (giữ dạng state để giao diện tự cập nhật)
    var baseFolder by mutableStateOf(prefs.baseFolder)
        private set
    var saveMp3 by mutableStateOf(prefs.saveMp3)
        private set
    var autoClip by mutableStateOf(prefs.autoClipboard)
        private set
    var vibrate by mutableStateOf(prefs.vibrate)
        private set

    private var lastClipStamp = -1L
    private val handled = mutableSetOf<String>()

    init {
        viewModelScope.launch { DownloadBus.libraryVersion.collect { reload() } }
        checkUpdate(silent = true)
    }

    fun reload() = viewModelScope.launch {
        library = withContext(Dispatchers.IO) { History.prune(ctx) }
    }

    fun startDownload(link: String = url) {
        val u = TikApi.extractUrl(link)
        if (u == null) { message = "Link chưa đúng, hãy dán link TikTok"; return }
        handled += u
        if (clipSuggestion == u) clipSuggestion = null
        url = ""
        DownloadService.start(ctx, u, fromShare = false)
    }

    fun pasteFromClipboard() {
        val text = readClip()
        val u = TikApi.extractUrl(text)
        if (u != null) url = u else message = "Bộ nhớ tạm không có link TikTok"
    }

    /** Gọi khi app được focus: chỉ đọc khi có nội dung copy mới để tránh báo "đã dán" liên tục. */
    fun onFocus() {
        if (!autoClip) return
        val cm = ctx.getSystemService(ClipboardManager::class.java) ?: return
        val desc = cm.primaryClipDescription ?: return
        val stamp = desc.timestamp
        if (stamp == lastClipStamp) return
        lastClipStamp = stamp
        if (!desc.hasMimeType("text/*")) return
        val u = TikApi.extractUrl(readClip())
        if (u != null && u !in handled && u != url) clipSuggestion = u
    }

    private fun readClip(): String? = runCatching {
        val cm = ctx.getSystemService(ClipboardManager::class.java)
        cm?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(ctx)?.toString()
    }.getOrNull()

    fun dismissClip() { clipSuggestion?.let { handled += it }; clipSuggestion = null }

    fun removed(uris: Set<Uri>) = viewModelScope.launch {
        withContext(Dispatchers.IO) { History.remove(ctx, uris) }
        reload()
    }

    fun changeFolder(v: String) { prefs.baseFolder = v; baseFolder = v }
    fun changeMp3(v: Boolean) { prefs.saveMp3 = v; saveMp3 = v }
    fun changeAutoClip(v: Boolean) { prefs.autoClipboard = v; autoClip = v }
    fun changeVibrate(v: Boolean) { prefs.vibrate = v; vibrate = v }

    fun checkUpdate(silent: Boolean = false) {
        if (update is UpdateState.Checking || update is UpdateState.Downloading) return
        update = UpdateState.Checking
        viewModelScope.launch {
            update = try {
                val rel = Updater.latest()
                if (rel != null && Updater.isNewer(rel.version)) UpdateState.Available(rel) else UpdateState.Latest
            } catch (e: Exception) {
                if (!silent) message = "Không kiểm tra được bản cập nhật"
                if (silent) UpdateState.Idle else UpdateState.Failed("Không kiểm tra được")
            }
        }
    }

    fun installUpdate(rel: Updater.Release) {
        if (update is UpdateState.Downloading) return
        update = UpdateState.Downloading(rel, 0f)
        viewModelScope.launch {
            try {
                val f = Updater.download(ctx, rel) { p -> update = UpdateState.Downloading(rel, p) }
                update = UpdateState.Available(rel)
                if (!Updater.install(ctx, f)) message = "Hãy cho phép TikGrab cài ứng dụng, rồi bấm Cập nhật lại"
            } catch (e: Exception) {
                update = UpdateState.Available(rel)
                message = "Tải bản cập nhật thất bại, thử lại nhé"
            }
        }
    }

    val whatsNew: WhatsNew? by lazy {
        runCatching {
            val j = JSONObject(ctx.assets.open("whatsnew.json").bufferedReader().readText())
            fun list(k: String) = j.optJSONArray(k)?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
            WhatsNew(j.optString("version"), j.optString("date"), list("new"), list("improved"), list("fixed"))
        }.getOrNull()
    }
}
