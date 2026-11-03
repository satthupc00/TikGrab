package com.mondiro.tikgrab

import kotlinx.coroutines.flow.MutableStateFlow

/** Trạng thái tải hiện tại, dùng chung giữa service và giao diện. */
data class JobState(
    val url: String,
    val title: String = "",
    val author: String = "",
    val cover: String? = null,
    val duration: Int = 0,
    val progress: Float = 0f,
    val phase: Phase = Phase.RESOLVING,
    val message: String = "",
    val queued: Int = 0,
) {
    enum class Phase { RESOLVING, DOWNLOADING, DONE, ERROR }
}

object DownloadBus {
    val job = MutableStateFlow<JobState?>(null)
    /** Tăng mỗi khi thư viện thay đổi để giao diện tải lại. */
    val libraryVersion = MutableStateFlow(0)
}
