package com.mondiro.tikgrab

import android.content.Context

/** Cài đặt người dùng (lưu trong máy). */
class Prefs(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("tikgrab", Context.MODE_PRIVATE)

    /** "Movies" hoặc "DCIM" — thư mục gốc chứa TikGrab. */
    var baseFolder: String
        get() = sp.getString("folder", FOLDER_MOVIES) ?: FOLDER_MOVIES
        set(v) = sp.edit().putString("folder", v).apply()
    var saveMp3: Boolean
        get() = sp.getBoolean("mp3", false)
        set(v) = sp.edit().putBoolean("mp3", v).apply()
    var autoClipboard: Boolean
        get() = sp.getBoolean("clip", true)
        set(v) = sp.edit().putBoolean("clip", v).apply()
    var vibrate: Boolean
        get() = sp.getBoolean("vibrate", true)
        set(v) = sp.edit().putBoolean("vibrate", v).apply()
    var askedNotif: Boolean
        get() = sp.getBoolean("askedNotif", false)
        set(v) = sp.edit().putBoolean("askedNotif", v).apply()

    companion object {
        const val FOLDER_MOVIES = "Movies"
        const val FOLDER_DCIM = "DCIM"
    }
}
