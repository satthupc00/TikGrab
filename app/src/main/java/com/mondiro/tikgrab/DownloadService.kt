package com.mondiro.tikgrab

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.VibratorManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

/** Tải video ở chế độ nền, hiện tiến độ trên thanh thông báo. */
class DownloadService : Service() {

    companion object {
        private const val EXTRA_URL = "url"
        private const val EXTRA_QUIET = "quiet"
        private const val NOTIF_PROGRESS = 1

        /** [fromShare] = true khi gọi từ nút Chia sẻ: báo kết quả bằng toast vì không mở app. */
        fun start(ctx: Context, url: String, fromShare: Boolean) {
            val i = Intent(ctx, DownloadService::class.java)
                .putExtra(EXTRA_URL, url).putExtra(EXTRA_QUIET, !fromShare)
            ctx.startForegroundService(i)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queue = Channel<Pair<String, Boolean>>(Channel.UNLIMITED)
    private val pending = AtomicInteger(0)
    @Volatile private var lastStartId = 0
    private val main = Handler(Looper.getMainLooper())
    private lateinit var nm: NotificationManager
    private lateinit var prefs: Prefs

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        nm = getSystemService(NotificationManager::class.java)
        prefs = Prefs(this)
        scope.launch { for ((url, quiet) in queue) process(url, quiet) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        ServiceCompat.startForeground(
            this, NOTIF_PROGRESS, progressNotif("Đang chuẩn bị…", "", -1f),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
        val url = intent?.getStringExtra(EXTRA_URL)
        if (url == null) {
            if (pending.get() == 0) finishIfIdle()
            return START_NOT_STICKY
        }
        pending.incrementAndGet()
        DownloadBus.job.value?.let { DownloadBus.job.value = it.copy(queued = pending.get() - 1) }
        queue.trySend(url to intent.getBooleanExtra(EXTRA_QUIET, false))
        return START_NOT_STICKY
    }

    private suspend fun process(url: String, quiet: Boolean) {
        var state = JobState(url = url, queued = pending.get() - 1)
        fun emit(s: JobState) { state = s; DownloadBus.job.value = s }
        emit(state)
        try {
            val info = TikApi.fetch(url)
            emit(state.copy(title = info.title, author = info.author, cover = info.cover,
                duration = info.duration, phase = JobState.Phase.DOWNLOADING))

            val existing = History.findVideo(this, info.id)
            if (existing != null) {
                emit(state.copy(phase = JobState.Phase.DONE, progress = 1f, message = "Video này đã có trong thư viện"))
                notifyDone(info, existing.uri, already = true, quiet = quiet)
                return
            }

            val base = prefs.baseFolder
            val name = "tikgrab_${Saver.safeName(info.author)}_${info.id}"
            val saved = mutableListOf<SavedItem>()
            val now = System.currentTimeMillis()

            if (info.images.isNotEmpty()) {
                // Bài đăng dạng ảnh
                info.images.forEachIndexed { i, img ->
                    val uri = Saver.download(this, img, Saver.Kind.IMAGE, base, "${name}_${i + 1}") { p ->
                        val total = (i + p.coerceAtLeast(0f)) / info.images.size
                        progress(state.copy(progress = total), "Đang tải ảnh ${i + 1}/${info.images.size}") { emit(it) }
                    }
                    saved += SavedItem(info.id, uri, Saver.Kind.IMAGE, info.author, info.title, 0, now - i)
                }
            } else {
                val urls = listOfNotNull(info.videoUrl, info.watermarkUrl).distinct()
                if (urls.isEmpty()) throw FriendlyException("Không tìm thấy file video")
                var uri: Uri? = null
                var err: Exception? = null
                for (u in urls) {
                    try {
                        uri = Saver.download(this, u, Saver.Kind.VIDEO, base, name) { p ->
                            progress(state.copy(progress = p), "Đang tải video HD…") { emit(it) }
                        }
                        break
                    } catch (e: Exception) { err = e }
                }
                if (uri == null) throw err ?: FriendlyException("Tải video thất bại")
                saved += SavedItem(info.id, uri, Saver.Kind.VIDEO, info.author, info.title, info.duration, now)
            }

            if (prefs.saveMp3 && info.musicUrl != null) {
                runCatching {
                    val mu = Saver.download(this, info.musicUrl, Saver.Kind.AUDIO, base, "${name}_music") { p ->
                        progress(state.copy(progress = p), "Đang tải nhạc…") { emit(it) }
                    }
                    saved += SavedItem(info.id, mu, Saver.Kind.AUDIO, info.author, info.title, info.duration, now - 100)
                }
            }

            History.add(this, saved)
            DownloadBus.libraryVersion.value++
            emit(state.copy(phase = JobState.Phase.DONE, progress = 1f, message = "Đã lưu vào ${Saver.folderFor(
                if (info.images.isNotEmpty()) Saver.Kind.IMAGE else Saver.Kind.VIDEO, base)}"))
            notifyDone(info, saved.first().uri, already = false, quiet = quiet)
            if (prefs.vibrate) vibrate()
        } catch (e: Exception) {
            val msg = (e as? FriendlyException)?.message ?: "Tải thất bại, thử lại nhé"
            emit(state.copy(phase = JobState.Phase.ERROR, message = msg))
            notifyError(url, msg, quiet)
        } finally {
            if (pending.decrementAndGet() == 0) finishIfIdle()
        }
    }

    private var lastNotif = 0L
    private inline fun progress(s: JobState, text: String, emit: (JobState) -> Unit) {
        emit(s)
        val now = System.currentTimeMillis()
        if (now - lastNotif > 400) {
            lastNotif = now
            nm.notify(NOTIF_PROGRESS, progressNotif(text, s.author.let { if (it.isBlank()) "" else "@$it" }, s.progress))
        }
    }

    private fun progressNotif(title: String, text: String, p: Float): Notification =
        NotificationCompat.Builder(this, App.CH_PROGRESS)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle(title)
            .setContentText(if (p >= 0) "$text · ${(p * 100).toInt()}%".trimStart(' ', '·') else text)
            .setProgress(100, (p.coerceIn(0f, 1f) * 100).toInt(), p < 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setColor(0xFFE5383B.toInt())
            .setContentIntent(openAppIntent())
            .build()

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun notifyDone(info: TikInfo, uri: Uri, already: Boolean, quiet: Boolean) {
        val isImage = info.images.isNotEmpty()
        val mime = if (isImage) "image/*" else "video/*"
        val view = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        val share = Intent.createChooser(
            Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Chia sẻ"
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val code = info.id.hashCode()
        val title = when {
            already -> "Đã có trong thư viện"
            isImage -> "Đã lưu ${info.images.size} ảnh ✓"
            else -> "Đã lưu video ✓"
        }
        val n = NotificationCompat.Builder(this, App.CH_DONE)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle(title)
            .setContentText(listOf(info.author.takeIf { it.isNotBlank() }?.let { "@$it" }, "Chạm để xem")
                .filterNotNull().joinToString(" · "))
            .setColor(0xFFE5383B.toInt())
            .setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(this, code, view, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            .addAction(0, "Chia sẻ", PendingIntent.getActivity(this, code + 1, share, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            .build()
        nm.notify(code, n)
        if (!quiet) toast("TikGrab: $title")
    }

    private fun notifyError(url: String, msg: String, quiet: Boolean) {
        val n = NotificationCompat.Builder(this, App.CH_DONE)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle("Không tải được video")
            .setContentText(msg)
            .setColor(0xFFE5383B.toInt())
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()
        nm.notify(url.hashCode(), n)
        if (!quiet) toast("TikGrab: $msg")
    }

    private fun toast(s: String) = main.post { Toast.makeText(applicationContext, s, Toast.LENGTH_SHORT).show() }

    @Suppress("DEPRECATION")
    private fun vibrate() = runCatching {
        val v = if (android.os.Build.VERSION.SDK_INT >= 31) getSystemService(VibratorManager::class.java)?.defaultVibrator
        else getSystemService(android.os.Vibrator::class.java)
        v?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
    }

    private fun finishIfIdle() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf(lastStartId)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
