package com.mondiro.tikgrab

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

/** Nhận link từ nút Chia sẻ của TikTok → tải ngầm, đóng ngay, không hiện giao diện. */
class ShareActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = if (intent?.action == Intent.ACTION_SEND)
            intent.getStringExtra(Intent.EXTRA_TEXT) ?: intent.getStringExtra(Intent.EXTRA_SUBJECT) else null
        val url = TikApi.extractUrl(text)
        if (url == null) {
            Toast.makeText(this, "TikGrab: không thấy link TikTok", Toast.LENGTH_SHORT).show()
        } else {
            runCatching { DownloadService.start(this, url, fromShare = true) }
                .onSuccess { Toast.makeText(this, "TikGrab: đang tải video…", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(this, "TikGrab: không bắt đầu tải được", Toast.LENGTH_SHORT).show() }
        }
        finish()
        overridePendingTransition(0, 0)
    }
}
