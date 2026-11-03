@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.mondiro.tikgrab.ui

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mondiro.tikgrab.DownloadBus
import com.mondiro.tikgrab.Filter
import com.mondiro.tikgrab.JobState
import com.mondiro.tikgrab.MainViewModel
import com.mondiro.tikgrab.SavedItem
import com.mondiro.tikgrab.Saver
import com.mondiro.tikgrab.UpdateState

private val Shape16 = RoundedCornerShape(16.dp)
private val Shape14 = RoundedCornerShape(14.dp)
private val Shape12 = RoundedCornerShape(12.dp)

private val GRADS = listOf(
    Color(0xFFFF7E80) to Color(0xFF7B5CFF), Color(0xFF22C3A6) to Color(0xFF1747B8),
    Color(0xFFFFE680) to Color(0xFFE8930A), Color(0xFF7B5CFF) to Color(0xFF4B2BD6),
    Color(0xFF3D9BFF) to Color(0xFF22C3A6), Color(0xFFFF7E80) to Color(0xFFA3161A),
)

@Composable
fun MainScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val job by DownloadBus.job.collectAsStateWithLifecycle()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showWhatsNew by rememberSaveable { mutableStateOf(false) }
    var showUpdate by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Uri?>(null) }

    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        val u = pendingDelete
        if (r.resultCode == Activity.RESULT_OK && u != null) vm.removed(setOf(u))
        pendingDelete = null
    }
    fun delete(item: SavedItem) {
        try {
            ctx.contentResolver.delete(item.uri, null, null)
            vm.removed(setOf(item.uri))
        } catch (e: SecurityException) {
            val sender = when {
                Build.VERSION.SDK_INT >= 30 ->
                    MediaStore.createDeleteRequest(ctx.contentResolver, listOf(item.uri)).intentSender
                e is RecoverableSecurityException -> e.userAction.actionIntent.intentSender
                else -> null
            }
            if (sender != null) {
                pendingDelete = item.uri
                deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
            } else Toast.makeText(ctx, "Không xoá được file", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(vm.message) {
        vm.message?.let { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show(); vm.message = null }
    }

    val items = vm.library.filter {
        when (vm.filter) {
            Filter.ALL -> true
            Filter.VIDEO -> it.kind == Saver.Kind.VIDEO
            Filter.IMAGE -> it.kind == Saver.Kind.IMAGE
            Filter.AUDIO -> it.kind == Saver.Kind.AUDIO
        }
    }
    val filters = buildList {
        add(Filter.ALL); add(Filter.VIDEO)
        if (vm.library.any { it.kind == Saver.Kind.IMAGE }) add(Filter.IMAGE)
        add(Filter.AUDIO)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(C.chrome)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .imePadding()
    ) {
        TopRow(vm.update,
            onUpdate = {
                if (vm.update is UpdateState.Available || vm.update is UpdateState.Downloading) showUpdate = true
                else vm.checkUpdate()
            },
            onSettings = { showSettings = true })

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val wide = maxWidth >= 600.dp
            val tile: @Composable (SavedItem, Modifier) -> Unit = { it, m ->
                MediaTile(it, m, onOpen = { open(ctx, it) }, onShare = { share(ctx, it) }, onDelete = { delete(it) })
            }
            if (wide) {
                Row(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.width(340.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        InputCard(vm)
                        JobCard(job, onRetry = { vm.startDownload(it) })
                        ShareTip()
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(150.dp),
                        modifier = Modifier.weight(1f).fillMaxSize().clip(Shape16).background(C.stage),
                        contentPadding = PaddingValues(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        libraryHeader(vm, filters, items.size)
                        libraryItems(items, vm.library.isEmpty(), tile)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    full("input") { InputCard(vm) }
                    full("job") { JobCard(job, onRetry = { vm.startDownload(it) }) }
                    full("tip") { Box(Modifier.padding(top = 6.dp, bottom = 8.dp)) { ShareTip() } }
                    libraryHeader(vm, filters, items.size)
                    libraryItems(items, vm.library.isEmpty(), tile)
                }
            }
        }

        // Created by Mondiro — góc dưới phải
        Box(
            Modifier.fillMaxWidth()
                .drawBehind { drawLine(Color(0xFF2A2D33), androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, 0f), 1f) }
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 18.dp, vertical = 6.dp),
            contentAlignment = Alignment.CenterEnd
        ) { Text("Created by Mondiro", color = C.credit, fontSize = 11.sp) }
    }

    if (showSettings) SettingsSheet(vm, onDismiss = { showSettings = false },
        onWhatsNew = { showWhatsNew = true },
        onUpdate = {
            if (vm.update is UpdateState.Available) showUpdate = true else vm.checkUpdate()
        })
    if (showWhatsNew) WhatsNewDialog(vm, onDismiss = { showWhatsNew = false },
        onUpdate = { showWhatsNew = false; showUpdate = true })
    if (showUpdate) UpdateDialog(vm, onDismiss = { showUpdate = false })
}

private fun LazyGridScope.full(key: String, content: @Composable () -> Unit) =
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }

private fun LazyGridScope.libraryHeader(vm: MainViewModel, filters: List<Filter>, count: Int) {
    full("libhead") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Đã tải", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = C.text, modifier = Modifier.weight(1f))
                Text("$count file", color = C.muted, fontSize = 12.5.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                filters.forEach { f -> Chip(f.label, vm.filter == f) { vm.filter = f } }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

private fun LazyGridScope.libraryItems(
    items: List<SavedItem>, libraryEmpty: Boolean, tile: @Composable (SavedItem, Modifier) -> Unit,
) {
    if (items.isEmpty()) {
        full("empty") {
            Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.VideoLibrary, null, tint = C.muted, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(10.dp))
                Text(if (libraryEmpty) "Chưa có video nào" else "Không có file loại này",
                    color = C.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text("Dán link ở trên hoặc bấm Chia sẻ → TikGrab trong TikTok",
                    color = C.muted, fontSize = 12.5.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    } else {
        items(items, key = { it.uri.toString() }) { tile(it, Modifier.animateItem()) }
    }
}

// ───────────────────────── Top row ─────────────────────────

@Composable
private fun TopRow(state: UpdateState, onUpdate: () -> Unit, onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.weight(1f))
        AnimatedVisibility(state is UpdateState.Latest,
            enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(), exit = fadeOut()) {
            Text("latest", color = C.latest, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(C.latest.copy(alpha = .14f))
                    .padding(horizontal = 8.dp, vertical = 1.dp))
        }
        UpdateButton(state, onUpdate)
        IconButton(onClick = onSettings, modifier = Modifier.size(42.dp)) {
            Icon(Icons.Rounded.Settings, "Cài đặt", tint = C.text, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
fun UpdateButton(state: UpdateState, onClick: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "upd")
    val angle by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(800, easing = LinearEasing)), label = "spin")
    val pulse by inf.animateFloat(1f, .45f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse")
    IconButton(onClick = onClick, modifier = Modifier.size(42.dp)) {
        when (state) {
            is UpdateState.Downloading -> CircularProgressIndicator(
                progress = { state.progress }, color = C.green, trackColor = C.track,
                strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp))
            else -> Icon(
                Icons.Rounded.Refresh, "Kiểm tra cập nhật",
                tint = when (state) {
                    is UpdateState.Latest, is UpdateState.Available -> C.green
                    else -> C.text
                },
                modifier = Modifier.size(22.dp)
                    .rotate(if (state is UpdateState.Checking) angle else 0f)
                    .scale(if (state is UpdateState.Available) 0.9f + 0.1f * pulse else 1f)
                    .let { if (state is UpdateState.Available) it.drawBehind {
                        drawCircle(C.green.copy(alpha = .25f * pulse), radius = size.minDimension * .85f)
                    } else it }
            )
        }
    }
}

// ───────────────────────── Input ─────────────────────────

@Composable
private fun InputCard(vm: MainViewModel) {
    val focus = LocalFocusManager.current
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    Card {
        Text("Dán link TikTok", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = C.text)
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().height(52.dp).clip(Shape14).background(C.chrome)
                .border(if (focused) 2.dp else 1.dp, if (focused) C.red else C.line, Shape14)
                .padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f)) {
                if (vm.url.isEmpty()) Text("https://vt.tiktok.com/…", color = C.muted, fontSize = 14.sp, maxLines = 1)
                BasicTextField(
                    value = vm.url, onValueChange = { vm.url = it },
                    singleLine = true, interactionSource = src,
                    textStyle = TextStyle(color = C.text, fontSize = 14.sp),
                    cursorBrush = SolidColor(C.red),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { focus.clearFocus(); vm.startDownload() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (vm.url.isNotEmpty()) {
                IconButton(onClick = { vm.url = "" }, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Rounded.Close, "Xoá", tint = C.muted, modifier = Modifier.size(18.dp))
                }
            }
            SmallButton("Dán", Icons.Rounded.ContentPaste) { vm.pasteFromClipboard() }
        }

        AnimatedVisibility(vm.clipSuggestion != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            val s = vm.clipSuggestion.orEmpty()
            Row(
                Modifier.padding(top = 10.dp).fillMaxWidth().clip(Shape12).background(C.selected)
                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Phát hiện link: " + s.substringAfter("://"), color = Color(0xFFF1C9CA), fontSize = 12.5.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                PressScale(onClick = { focus.clearFocus(); vm.startDownload(s) }) { m ->
                    Text("Tải luôn", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp,
                        modifier = m.clip(RoundedCornerShape(8.dp)).background(C.red).padding(horizontal = 12.dp, vertical = 7.dp))
                }
                IconButton(onClick = { vm.dismissClip() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Close, "Bỏ qua", tint = C.muted, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        PressScale(onClick = { focus.clearFocus(); vm.startDownload() }) { m ->
            Row(
                m.fillMaxWidth().height(50.dp).clip(Shape14).background(C.red),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Download, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Tải về", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// ───────────────────────── Job ─────────────────────────

@Composable
private fun JobCard(job: JobState?, onRetry: (String) -> Unit) {
    AnimatedVisibility(job != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        val j = job ?: return@AnimatedVisibility
        val cover by rememberRemote(j.cover)
        val p by animateFloatAsState(j.progress.coerceIn(0f, 1f), tween(250), label = "p")
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(54.dp, 72.dp).clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(GRADS[0].first, GRADS[0].second)))) {
                    cover?.let { Image(it.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(j.title.ifBlank { "Video TikTok" }, color = C.text, fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(listOfNotNull(j.author.takeIf { it.isNotBlank() }?.let { "@$it" },
                        j.duration.takeIf { it > 0 }?.let(::fmt), "HD").joinToString(" · "),
                        color = C.muted, fontSize = 12.sp, maxLines = 1)
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(6.dp)).background(C.track)) {
                        val fill = when (j.phase) {
                            JobState.Phase.DONE -> 1f
                            JobState.Phase.ERROR -> 0f
                            else -> if (j.progress < 0) .3f else p
                        }
                        Box(Modifier.fillMaxWidth(fill).height(6.dp).clip(RoundedCornerShape(6.dp))
                            .background(if (j.phase == JobState.Phase.DONE) C.green else C.red))
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (txt, col) = when (j.phase) {
                            JobState.Phase.RESOLVING -> "Đang lấy thông tin…" to C.muted
                            JobState.Phase.DOWNLOADING -> "Đang tải…" to C.muted
                            JobState.Phase.DONE -> "✓ ${j.message}" to C.green
                            JobState.Phase.ERROR -> j.message to C.red
                        }
                        Text(txt, color = col, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 2)
                        if (j.phase == JobState.Phase.DOWNLOADING && j.progress >= 0)
                            Text("${(p * 100).toInt()}%", color = C.muted, fontSize = 12.sp)
                        if (j.queued > 0) Text("  +${j.queued} chờ", color = C.muted, fontSize = 12.sp)
                    }
                }
                if (j.phase == JobState.Phase.DONE || j.phase == JobState.Phase.ERROR) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { DownloadBus.job.value = null }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.Close, "Đóng", tint = C.muted, modifier = Modifier.size(18.dp))
                        }
                        if (j.phase == JobState.Phase.ERROR) IconButton(onClick = { onRetry(j.url) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.Refresh, "Thử lại", tint = C.red, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareTip() {
    Row(
        Modifier.fillMaxWidth().drawBehind {
            drawRoundRect(Color(0xFF4A4E57), style = Stroke(width = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                cornerRadius = CornerRadius(14.dp.toPx()))
        }.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Rounded.Share, null, tint = C.red, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(buildAnnotatedString {
            withStyle(SpanStyle(color = C.text, fontWeight = FontWeight.Bold)) { append("Nhanh hơn: ") }
            append("trong TikTok bấm ")
            withStyle(SpanStyle(color = C.text, fontWeight = FontWeight.Bold)) { append("Chia sẻ → TikGrab") }
            append(". Video tự tải ngầm, không cần mở app này.")
        }, color = C.muted, fontSize = 12.5.sp, lineHeight = 18.sp)
    }
}

// ───────────────────────── Library tile ─────────────────────────

@Composable
private fun MediaTile(item: SavedItem, modifier: Modifier, onOpen: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val thumb by rememberThumb(item.uri)
    val g = GRADS[(item.id.hashCode() and 0x7fffffff) % GRADS.size]
    Box(modifier) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(9f / 16f).clip(Shape12)
                .background(Brush.linearGradient(listOf(g.first, g.second)))
                .combinedClickable(onClick = onOpen, onLongClick = { menu = true })
        ) {
            thumb?.let { Image(it.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color(0x99000000))))
            val icon: ImageVector = when (item.kind) {
                Saver.Kind.VIDEO -> Icons.Rounded.PlayArrow
                Saver.Kind.AUDIO -> Icons.Rounded.MusicNote
                Saver.Kind.IMAGE -> Icons.Rounded.Image
            }
            Icon(icon, null, tint = Color.White.copy(alpha = .9f), modifier = Modifier.align(Alignment.Center).size(30.dp))
            if (item.kind == Saver.Kind.AUDIO) Tag("MP3", C.red, Modifier.align(Alignment.TopStart).padding(6.dp))
            if (item.author.isNotBlank()) Text("@${item.author}", color = Color.White, fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 6.dp, bottom = 6.dp, end = 44.dp))
            if (item.duration > 0) Tag(fmt(item.duration), Color(0x8C000000), Modifier.align(Alignment.BottomEnd).padding(6.dp))
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = C.panel) {
            DropdownMenuItem(text = { Text("Chia sẻ") }, leadingIcon = { Icon(Icons.Rounded.Share, null) },
                onClick = { menu = false; onShare() })
            DropdownMenuItem(text = { Text("Xoá", color = C.red) }, leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = C.red) },
                onClick = { menu = false; onDelete() })
        }
    }
}

@Composable
private fun Tag(text: String, bg: Color, modifier: Modifier) {
    Text(text, color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold,
        modifier = modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 6.dp, vertical = 1.dp))
}

// ───────────────────────── Shared bits ─────────────────────────

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth().clip(Shape16).background(C.panel).border(1.dp, C.line, Shape16).padding(14.dp)) { content() }
}

/** Nút lún nhẹ khi bấm (spring) — chuẩn animation Mondiro. */
@Composable
fun PressScale(onClick: () -> Unit, content: @Composable (Modifier) -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) .96f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "press")
    content(Modifier.scale(s).combinedClickable(interactionSource = src, indication = null, onClick = onClick))
}

@Composable
fun SmallButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    PressScale(onClick) { m ->
        Row(m.height(38.dp).clip(RoundedCornerShape(10.dp)).background(C.panel).border(1.dp, C.line, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = C.text, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, color = C.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val s by animateFloatAsState(if (selected) 1f else .97f, spring(Spring.DampingRatioMediumBouncy), label = "chip")
    PressScale(onClick) { m ->
        Text(text, color = if (selected) Color.White else C.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            modifier = m.scale(s).height(32.dp).clip(RoundedCornerShape(20.dp))
                .background(if (selected) C.selected else Color.Transparent)
                .border(1.dp, if (selected) C.red else C.line, RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp))
    }
}

private fun fmt(sec: Int) = "%d:%02d".format(sec / 60, sec % 60)

private fun mime(item: SavedItem) = when (item.kind) {
    Saver.Kind.VIDEO -> "video/*"; Saver.Kind.AUDIO -> "audio/*"; Saver.Kind.IMAGE -> "image/*"
}

private fun open(ctx: Context, item: SavedItem) {
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(item.uri, mime(item))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(ctx, "Không có app nào mở được file này", Toast.LENGTH_SHORT).show()
    }
}

private fun share(ctx: Context, item: SavedItem) {
    ctx.startActivity(Intent.createChooser(
        Intent(Intent.ACTION_SEND).setType(mime(item)).putExtra(Intent.EXTRA_STREAM, item.uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Chia sẻ"))
}
