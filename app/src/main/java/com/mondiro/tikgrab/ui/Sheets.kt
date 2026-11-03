@file:OptIn(ExperimentalMaterial3Api::class)

package com.mondiro.tikgrab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mondiro.tikgrab.BuildConfig
import com.mondiro.tikgrab.MainViewModel
import com.mondiro.tikgrab.Prefs
import com.mondiro.tikgrab.UpdateState

@Composable
fun SettingsSheet(vm: MainViewModel, onDismiss: () -> Unit, onWhatsNew: () -> Unit, onUpdate: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = C.panel,
        sheetMaxWidth = 520.dp,
    ) {
        Column(Modifier.padding(horizontal = 18.dp).padding(bottom = 18.dp).verticalScroll(rememberScrollState())) {
            Text("Cài đặt", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = C.text)
            Group("Tải về")
            SettingRow("Thư mục lưu", "${vm.baseFolder}/TikGrab · hiện trong Thư viện") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip("Movies", vm.baseFolder == Prefs.FOLDER_MOVIES) { vm.setFolder(Prefs.FOLDER_MOVIES) }
                    Chip("DCIM", vm.baseFolder == Prefs.FOLDER_DCIM) { vm.setFolder(Prefs.FOLDER_DCIM) }
                }
            }
            SettingRow("Tải thêm nhạc MP3", "Lưu riêng file nhạc vào Music/TikGrab") { Sw(vm.saveMp3, vm::setMp3) }
            SettingRow("Tự nhận link khi mở app", "Đọc link TikTok vừa copy") { Sw(vm.autoClip, vm::setAutoClip) }
            SettingRow("Rung khi tải xong", "Rung nhẹ khi video đã lưu", divider = false) { Sw(vm.vibrate, vm::setVibrate) }

            Group("Phiên bản")
            val status = when (val u = vm.update) {
                UpdateState.Latest -> "Đã là bản mới nhất"
                UpdateState.Checking -> "Đang kiểm tra…"
                is UpdateState.Available -> "Có bản mới v${u.release.version}"
                is UpdateState.Downloading -> "Đang tải bản v${u.release.version}… ${(u.progress * 100).toInt()}%"
                is UpdateState.Failed -> u.message
                UpdateState.Idle -> "Chưa kiểm tra"
            }
            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("TikGrab v${BuildConfig.VERSION_NAME}", color = C.text, fontWeight = FontWeight.SemiBold)
                    Text(status, color = if (vm.update is UpdateState.Available) C.green else C.muted, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WideButton("What's new", Modifier.weight(1f)) { onWhatsNew() }
                WideButton(if (vm.update is UpdateState.Available) "Cập nhật ngay" else "Kiểm tra cập nhật",
                    Modifier.weight(1f), update = true, onClick = onUpdate)
            }
        }
    }
}

@Composable
private fun WideButton(text: String, modifier: Modifier, update: Boolean = false, onClick: () -> Unit) {
    PressScale(onClick) { m ->
        Row(
            modifier.then(m).height(44.dp).clip(RoundedCornerShape(10.dp)).background(C.chrome),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(if (update) Icons.Rounded.Refresh else Icons.Rounded.AutoAwesome, null, tint = C.text, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, color = C.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun Group(t: String) {
    Text(t.uppercase(), color = C.muted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp,
        modifier = Modifier.padding(top = 18.dp, bottom = 4.dp))
}

@Composable
private fun SettingRow(title: String, sub: String, divider: Boolean = true, end: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = C.text, fontWeight = FontWeight.SemiBold)
            Text(sub, color = C.muted, fontSize = 12.sp)
        }
        end()
    }
    if (divider) HorizontalDivider(color = C.line)
}

@Composable
private fun Sw(v: Boolean, on: (Boolean) -> Unit) = Switch(
    checked = v, onCheckedChange = on,
    colors = SwitchDefaults.colors(
        checkedThumbColor = Color.White, checkedTrackColor = C.red, checkedBorderColor = C.red,
        uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFF474C55), uncheckedBorderColor = Color(0xFF474C55),
    )
)

@Composable
private fun DialogFrame(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.widthIn(max = 460.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.panel)
                .padding(18.dp).navigationBarsPadding()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = C.text, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Close, "Đóng", tint = C.muted)
                }
            }
            content()
        }
    }
}

@Composable
fun WhatsNewDialog(vm: MainViewModel, onDismiss: () -> Unit, onUpdate: () -> Unit) {
    val w = vm.whatsNew
    DialogFrame("What's new", onDismiss) {
        val date = w?.date?.split("-")?.takeIf { it.size == 3 }?.let { "${it[2]}/${it[1]}/${it[0]}" } ?: ""
        Text("v${w?.version ?: BuildConfig.VERSION_NAME}" + if (date.isNotEmpty()) " · $date" else "",
            color = C.muted, fontSize = 12.5.sp)
        Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
            if (w != null) {
                Section("Mới", w.new); Section("Cải thiện", w.improved); Section("Sửa lỗi", w.fixed)
            }
        }
        val u = vm.update
        if (u is UpdateState.Available) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Có bản mới v${u.release.version}", color = C.green, fontSize = 13.sp, modifier = Modifier.weight(1f))
                PressScale(onUpdate) { m ->
                    Text("Cập nhật", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        modifier = m.clip(RoundedCornerShape(10.dp)).background(C.red).padding(horizontal = 14.dp, vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, lines: List<String>) {
    if (lines.isEmpty()) return
    Text(title, color = C.red, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
    lines.forEach {
        Row(Modifier.padding(vertical = 3.dp)) {
            Text("•", color = C.muted, modifier = Modifier.width(14.dp))
            Text(it, color = C.text, fontSize = 13.5.sp)
        }
    }
}

@Composable
fun UpdateDialog(vm: MainViewModel, onDismiss: () -> Unit) {
    val u = vm.update
    val rel = when (u) {
        is UpdateState.Available -> u.release
        is UpdateState.Downloading -> u.release
        else -> null
    }
    if (rel == null) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onDismiss() }
        return
    }
    DialogFrame("Có bản cập nhật", onDismiss) {
        Text("v${BuildConfig.VERSION_NAME} → v${rel.version}", color = C.green, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        val notes = rel.notes.lines().map { it.trim() }.filter { it.isNotEmpty() }
            .map { it.removePrefix("###").removePrefix("##").removePrefix("-").removePrefix("*").trim() }
        Column(Modifier.padding(top = 10.dp).heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
            notes.forEach { Text(it, color = C.text, fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp)) }
        }
        Spacer(Modifier.height(14.dp))
        if (u is UpdateState.Downloading) {
            Text("Đang tải… ${(u.progress * 100).toInt()}%", color = C.muted, fontSize = 12.5.sp)
            Spacer(Modifier.height(6.dp))
            androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(6.dp)).background(C.track)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth(u.progress.coerceIn(0f, 1f)).height(6.dp).background(C.green))
            }
        } else {
            PressScale({ vm.installUpdate(rel) }) { m ->
                Row(m.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp)).background(C.red),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Download, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Tải và cài bản mới", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
