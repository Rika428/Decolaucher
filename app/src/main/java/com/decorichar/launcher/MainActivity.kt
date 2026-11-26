package com.decorichar.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val IDLE_MS = 60_000L
val NAVY = Color(0xFF050B2E)
val GOLD = Color(0xFFF6B800)

class AppItem(val name: String, val pkgs: List<String>, val c1: Color, val c2: Color, val txt: Color = Color.White)
class Row(val title: String, val apps: List<AppItem>)

val ROWS = listOf(
    Row("Películas", listOf(
        AppItem("YouTube", listOf("com.google.android.youtube.tv", "com.google.android.youtube"), Color(0xFFE81818), Color(0xFFA00000)),
        AppItem("NETFLIX", listOf("com.netflix.ninja", "com.netflix.mediaclient"), Color(0xFF0A0A0A), Color(0xFF0A0A0A), Color(0xFFE50914)),
        AppItem("prime video", listOf("com.amazon.amazonvideo.livingroom", "com.amazon.avod.thirdpartyclient"), Color(0xFF1A98FF), Color(0xFF0B62C4)),
        AppItem("HBO max", listOf("com.wbd.stream", "com.hbo.hbomax"), Color(0xFF6D2CF0), Color(0xFF2B0F78)),
        AppItem("Disney+", listOf("com.disney.disneyplus"), Color(0xFF0F2A7A), Color(0xFF1F5BD6)))),
    Row("Series", listOf(
        AppItem("Disney+", listOf("com.disney.disneyplus"), Color(0xFF0F2A7A), Color(0xFF1F5BD6)),
        AppItem("max", listOf("com.wbd.stream"), Color(0xFF2B1FD6), Color(0xFF1A0F8A)),
        AppItem("Paramount+", listOf("com.cbs.ott", "com.cbs.app"), Color(0xFF0A50E6), Color(0xFF1F78FF)),
        AppItem("STAR+", listOf(), Color(0xFF0A0A14), Color(0xFF0A0A14)),
        AppItem("flow", listOf(), Color(0xFF0A1A1A), Color(0xFF0A1A1A), Color(0xFF2EE6B0)))),
    Row("Aplicaciones", listOf(
        AppItem("Play Store", listOf("com.android.vending"), Color(0xFF1A2030), Color(0xFF10151F)),
        AppItem("Aptoide TV", listOf("cm.aptoide.tv", "cm.aptoide.pt"), Color(0xFF1A2030), Color(0xFF10151F), Color(0xFFFF6A4A)),
        AppItem("YouTube Kids", listOf("com.google.android.youtube.tvkids", "com.google.android.apps.youtube.kids"), Color(0xFFF2F2F2), Color(0xFFE0E0E0), Color(0xFFD00000)),
        AppItem("Spotify", listOf("com.spotify.tv.android", "com.spotify.music"), Color(0xFF0B1A10), Color(0xFF0B1A10), Color(0xFF1ED760)),
        AppItem("TikTok", listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"), Color(0xFF050505), Color(0xFF050505)))))
val ALL = ROWS.flatMap { it.apps }.distinctBy { it.name }

class MainActivity : ComponentActivity() {
    var idle by mutableStateOf(false)
    var last = System.currentTimeMillis()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Root(this) }
    }
    override fun onResume() { super.onResume(); last = System.currentTimeMillis(); idle = false }
    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        last = System.currentTimeMillis()
        if (idle) { if (e.action == KeyEvent.ACTION_UP) idle = false; return true }
        return super.dispatchKeyEvent(e)
    }
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        last = System.currentTimeMillis()
        if (idle) { if (ev.action == MotionEvent.ACTION_UP) idle = false; return true }
        return super.dispatchTouchEvent(ev)
    }
}

fun launch(ctx: Context, name: String, pkgs: List<String>) {
    val pm = ctx.packageManager
    for (p in pkgs) {
        val i = pm.getLeanbackLaunchIntentForPackage(p) ?: pm.getLaunchIntentForPackage(p)
        if (i != null) { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return }
    }
    val key = name.lowercase().replace("+", "").trim()
    val hit = installed(ctx).firstOrNull { it.label.lowercase().replace("+", "").contains(key) }
    if (hit != null) {
        val i = pm.getLeanbackLaunchIntentForPackage(hit.pkg) ?: pm.getLaunchIntentForPackage(hit.pkg)
        if (i != null) { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return }
    }
    Toast.makeText(ctx, "$name no está instalada", Toast.LENGTH_SHORT).show()
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=" + Uri.encode(name))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {}
}

class Inst(val label: String, val pkg: String, val icon: ImageBitmap)

fun installed(ctx: Context): List<Inst> {
    val pm = ctx.packageManager
    val a = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0)
    val b = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
    return (a + b).distinctBy { it.activityInfo.packageName }
        .filter { it.activityInfo.packageName != ctx.packageName }
        .map { Inst(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.loadIcon(pm).toBitmap(96, 96).asImageBitmap()) }
        .sortedBy { it.label.lowercase() }
}

@Composable
fun rememberNow(): State<Date> = produceState(Date()) { while (true) { value = Date(); delay(10_000) } }

@Composable
fun Root(act: MainActivity) {
    var screen by remember { mutableStateOf("home") }
    LaunchedEffect(Unit) {
        while (true) { delay(1000); if (!act.idle && System.currentTimeMillis() - act.last > IDLE_MS) act.idle = true }
    }
    Box(Modifier.fillMaxSize()) {
        if (screen == "home") Home(onAll = { screen = "all" }) else AllApps(onBack = { screen = "home" })
        if (act.idle) Idle()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Tile(
    modifier: Modifier, c1: Color, c2: Color,
    requester: FocusRequester? = null, onLong: (() -> Unit)? = null, onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val sc by animateFloatAsState(if (focused) 1.06f else 1f, label = "scale")
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier.scale(sc)
            .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
            .border(if (focused) 3.dp else 1.dp, if (focused) Color.White else Color(0x33FFFFFF), shape)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(c1, c2)))
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { if (it.key == Key.Menu && it.type == KeyEventType.KeyUp && onLong != null) { onLong(); true } else false }
            .combinedClickable(onClick = onClick, onLongClick = onLong),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
fun TextTile(
    modifier: Modifier, name: String, c1: Color, c2: Color, txt: Color = Color.White, size: TextUnit = 18.sp,
    sub: String? = null, requester: FocusRequester? = null, onLong: (() -> Unit)? = null, onClick: () -> Unit
) {
    Tile(modifier, c1, c2, requester, onLong, onClick) {
        if (sub != null) Text(sub, color = GOLD, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(name, color = txt, fontSize = size, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
fun Home(onAll: () -> Unit) {
    val ctx = LocalContext.current
    val now by rememberNow()
    val prefs = remember { ctx.getSharedPreferences("deco", Context.MODE_PRIVATE) }
    var fav by remember { mutableStateOf(prefs.getString("fav", "YouTube") ?: "YouTube") }
    val favApp = ALL.firstOrNull { it.name == fav } ?: ALL[0]
    val first = remember { FocusRequester() }
    val setFav: (String) -> Unit = { n ->
        fav = n; prefs.edit().putString("fav", n).apply()
        Toast.makeText(ctx, "$n es tu favorita", Toast.LENGTH_SHORT).show()
    }
    LaunchedEffect(Unit) { try { first.requestFocus() } catch (_: Exception) {} }
    BackHandler(enabled = true) { }

    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF1B3FD6), Color(0xFF0A1A6E), NAVY)))) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth().height(140.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(210.dp)) {
                    Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(now), color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black)
                    Text(SimpleDateFormat("EEE, d 'de' MMM.", Locale("es", "AR")).format(now), color = Color.White, fontSize = 15.sp)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DECO RICHAR", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
                    Text("LAUNCHER", color = GOLD, fontSize = 40.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
                    Text("TU TV BOX, TU MUNDO", color = Color.White, fontSize = 12.sp, letterSpacing = 3.sp)
                }
                TextTile(Modifier.width(230.dp).height(118.dp), favApp.name, favApp.c1, favApp.c2, favApp.txt, 28.sp,
                    sub = "FAVORITA", onClick = { launch(ctx, favApp.name, favApp.pkgs) })
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.weight(1f)) {
                Column(Modifier.width(88.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val side = Modifier.fillMaxWidth().height(56.dp)
                    TextTile(side, "Inicio", Color(0xFF1D58FF), Color(0xFF0B2F9E), size = 14.sp, onClick = { try { first.requestFocus() } catch (_: Exception) {} })
                    TextTile(side, "Apps", Color(0xFF1B2552), Color(0xFF10163A), size = 14.sp, onClick = onAll)
                    TextTile(side, "Ajustes", Color(0xFF1B2552), Color(0xFF10163A), size = 14.sp,
                        onClick = { ctx.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) })
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    ROWS.forEachIndexed { ri, row ->
                        Column {
                            Text(row.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.apps.forEachIndexed { i, a ->
                                    TextTile(Modifier.weight(1f).height(64.dp), a.name, a.c1, a.c2, a.txt,
                                        requester = if (ri == 0 && i == 0) first else null,
                                        onLong = { setFav(a.name) }, onClick = { launch(ctx, a.name, a.pkgs) })
                                }
                                TextTile(Modifier.weight(0.8f).height(64.dp), "+ Más Apps", Color(0xFF1B2552), Color(0xFF10163A), size = 14.sp, onClick = onAll)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AllApps(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val ctx = LocalContext.current
    val apps = remember { installed(ctx) }
    Column(Modifier.fillMaxSize().background(NAVY).padding(24.dp)) {
        Text("Todas las apps", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
        LazyVerticalGrid(GridCells.Adaptive(120.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(apps) { a ->
                Tile(Modifier.fillMaxWidth().height(96.dp), Color(0xFF1B2552), Color(0xFF10163A), onClick = { launch(ctx, a.label, listOf(a.pkg)) }) {
                    Image(a.icon, null, Modifier.size(44.dp))
                    Text(a.label, color = Color.White, fontSize = 12.sp, maxLines = 1, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun Idle() {
    val ctx = LocalContext.current
    val now by rememberNow()
    val ids = remember { (1..8).map { ctx.resources.getIdentifier("reposo$it", "drawable", ctx.packageName) }.filter { it != 0 } }
    var n by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(6000); n++ } }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Crossfade(targetState = n, animationSpec = tween(1400), label = "idle") { k ->
            if (ids.isNotEmpty()) {
                Image(painterResource(ids[k % ids.size]), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                val cols = if (k % 2 == 0) listOf(Color(0xFFC9C3F0), Color(0xFF6F7FD8), Color(0xFF23306B))
                           else listOf(Color(0xFF1B3FD6), Color(0xFF0A1A6E), NAVY)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(cols)))
            }
        }
        Column(Modifier.align(Alignment.BottomStart).padding(start = 56.dp, bottom = 64.dp)) {
            Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(now), color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.ExtraLight)
            Text(SimpleDateFormat("EEEE d MMMM", Locale("es", "AR")).format(now), color = Color.White, fontSize = 18.sp)
        }
    }
}
