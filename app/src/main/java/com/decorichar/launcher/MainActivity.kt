package com.decorichar.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.input.key.onPreviewKeyEvent
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
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val IDLE_MS = 60_000L
// Clima: cambiá estos 3 valores por tu ciudad
const val CIUDAD = "Buenos Aires"
const val LAT = -34.61
const val LON = -58.38

val NAVY = Color(0xFF050B2E)
val GOLD = Color(0xFFF6B800)
val TITLES = listOf("Películas", "Series", "Aplicaciones")

var weatherText by mutableStateOf("")

class AppItem(val name: String, val pkgs: List<String>, val c1: Color, val c2: Color, val txt: Color = Color.White)
class Row(val title: String, val apps: List<AppItem>)

// Apps que se cargan la primera vez (si están instaladas). Después las elegís vos.
val ROWS = listOf(
    Row("Películas", listOf(
        AppItem("YouTube", listOf("com.google.android.youtube.tv", "com.google.android.youtube"), Color(0xFFE81818), Color(0xFFA00000)),
        AppItem("NETFLIX", listOf("com.netflix.ninja", "com.netflix.mediaclient"), Color(0xFF0A0A0A), Color(0xFF0A0A0A), Color(0xFFE50914)),
        AppItem("prime video", listOf("com.amazon.amazonvideo.livingroom", "com.amazon.avod.thirdpartyclient"), Color(0xFF1A98FF), Color(0xFF0B62C4)),
        AppItem("HBO max", listOf("com.wbd.stream", "com.hbo.hbomax"), Color(0xFF6D2CF0), Color(0xFF2B0F78)),
        AppItem("Disney+", listOf("com.disney.disneyplus"), Color(0xFF0F2A7A), Color(0xFF1F5BD6)))),
    Row("Series", listOf(
        AppItem("Paramount+", listOf("com.cbs.ott", "com.cbs.app"), Color(0xFF0A50E6), Color(0xFF1F78FF)),
        AppItem("STAR+", listOf(), Color(0xFF0A0A14), Color(0xFF0A0A14)),
        AppItem("flow", listOf(), Color(0xFF0A1A1A), Color(0xFF0A1A1A), Color(0xFF2EE6B0)))),
    Row("Aplicaciones", listOf(
        AppItem("Play Store", listOf("com.android.vending"), Color(0xFF1A2030), Color(0xFF10151F)),
        AppItem("Aptoide TV", listOf("cm.aptoide.tv", "cm.aptoide.pt"), Color(0xFF1A2030), Color(0xFF10151F), Color(0xFFFF6A4A)),
        AppItem("YouTube Kids", listOf("com.google.android.youtube.tvkids", "com.google.android.apps.youtube.kids"), Color(0xFFF2F2F2), Color(0xFFE0E0E0), Color(0xFFD00000)),
        AppItem("Spotify", listOf("com.spotify.tv.android", "com.spotify.music"), Color(0xFF0B1A10), Color(0xFF0B1A10), Color(0xFF1ED760)),
        AppItem("TikTok", listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"), Color(0xFF050505), Color(0xFF050505)))))
val KNOWN: Map<String, AppItem> = ROWS.flatMap { it.apps }.flatMap { a -> a.pkgs.map { it to a } }.toMap()

val PRESETS = listOf(
    "Azul y oro" to listOf(Color(0xFF1B3FD6), Color(0xFF0A1A6E), NAVY),
    "Noche" to listOf(Color(0xFF16213E), Color(0xFF0B1020), Color.Black),
    "Montaña" to listOf(Color(0xFFC9C3F0), Color(0xFF6F7FD8), Color(0xFF23306B)),
    "Dorado" to listOf(Color(0xFF4A3500), Color(0xFF0A1A6E), NAVY)
)

class Inst(val label: String, val pkg: String)

fun norm(s: String) = s.lowercase().replace("+", "").trim()

fun installedBasic(ctx: Context): List<Inst> {
    val pm = ctx.packageManager
    val a = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0)
    val b = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
    return (a + b).distinctBy { it.activityInfo.packageName }
        .filter { it.activityInfo.packageName != ctx.packageName }
        .map { Inst(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
        .sortedBy { it.label.lowercase() }
}

fun resolvePkg(ctx: Context, a: AppItem): String? {
    val pm = ctx.packageManager
    for (p in a.pkgs) if (pm.getLeanbackLaunchIntentForPackage(p) != null || pm.getLaunchIntentForPackage(p) != null) return p
    val key = norm(a.name)
    return installedBasic(ctx).firstOrNull { norm(it.label).contains(key) }?.pkg
}

fun labelOf(pm: PackageManager, pkg: String): String =
    try { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() } catch (e: Exception) { pkg }

fun launchPkg(ctx: Context, pkg: String) {
    val pm = ctx.packageManager
    val i = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
    if (i != null) ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    else Toast.makeText(ctx, "Esa app ya no está instalada", Toast.LENGTH_SHORT).show()
}

fun loadBg(ctx: Context): ImageBitmap? {
    val f = File(ctx.filesDir, "fondo.jpg")
    if (!f.exists()) return null
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(f.path, o)
    var s = 1
    while (o.outWidth / s > 2000) s *= 2
    val o2 = BitmapFactory.Options().apply { inSampleSize = s }
    return BitmapFactory.decodeFile(f.path, o2)?.asImageBitmap()
}

fun wmo(c: Int): String = when (c) {
    0 -> "Despejado"
    1, 2 -> "Parcialmente nublado"
    3 -> "Nublado"
    45, 48 -> "Niebla"
    in 51..57 -> "Llovizna"
    in 61..67 -> "Lluvia"
    in 71..77 -> "Nieve"
    in 80..82 -> "Chubascos"
    in 95..99 -> "Tormenta"
    else -> ""
}

suspend fun fetchWeather(): String = withContext(Dispatchers.IO) {
    try {
        val u = URL("https://api.open-meteo.com/v1/forecast?latitude=$LAT&longitude=$LON&current=temperature_2m,weather_code&timezone=auto")
        val c = u.openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 8000
        val j = JSONObject(c.inputStream.bufferedReader().readText()).getJSONObject("current")
        val t = Math.round(j.getDouble("temperature_2m")).toInt()
        "$t° " + wmo(j.getInt("weather_code"))
    } catch (e: Exception) { "" }
}

class Store(val ctx: Context) {
    private val p = ctx.getSharedPreferences("deco2", Context.MODE_PRIVATE)
    val rows = List(3) { mutableStateListOf<String>() }
    var fav by mutableStateOf(p.getString("fav", "") ?: "")
    var bg by mutableStateOf(p.getString("bg", "p0") ?: "p0")
    var bgVersion by mutableStateOf(0)

    init {
        if (p.getBoolean("seeded", false)) {
            rows.forEachIndexed { i, l ->
                l.addAll((p.getString("row$i", "") ?: "").split("|").filter { it.isNotEmpty() })
            }
        } else {
            val used = HashSet<String>()
            ROWS.forEachIndexed { i, r ->
                r.apps.forEach { a ->
                    val k = resolvePkg(ctx, a)
                    if (k != null && used.add(k)) rows[i].add(k)
                }
            }
            p.edit().putBoolean("seeded", true).apply()
            save()
        }
    }

    fun save() {
        val e = p.edit()
        rows.forEachIndexed { i, l -> e.putString("row$i", l.joinToString("|")) }
        e.putString("fav", fav).putString("bg", bg).apply()
    }
}

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

@Composable
fun rememberNow(): State<Date> = produceState(Date()) { while (true) { value = Date(); delay(10_000) } }

@Composable
fun Root(act: MainActivity) {
    val ctx = LocalContext.current
    val store = remember { Store(ctx) }
    var screen by remember { mutableStateOf("home") }
    var addRow by remember { mutableStateOf(-1) }
    LaunchedEffect(Unit) {
        while (true) { delay(1000); if (!act.idle && System.currentTimeMillis() - act.last > IDLE_MS) act.idle = true }
    }
    LaunchedEffect(Unit) {
        while (true) {
            val w = fetchWeather()
            if (w.isNotEmpty()) weatherText = w
            delay(if (w.isEmpty()) 60_000L else 30 * 60_000L)
        }
    }
    Box(Modifier.fillMaxSize()) {
        when {
            addRow >= 0 -> AppGrid("Agregar a " + TITLES[addRow], { addRow = -1 }) { a ->
                if (!store.rows[addRow].contains(a.pkg)) { store.rows[addRow].add(a.pkg); store.save() }
                Toast.makeText(ctx, a.label + " agregada", Toast.LENGTH_SHORT).show()
                addRow = -1
            }
            screen == "all" -> AppGrid("Todas las apps", { screen = "home" }) { a -> launchPkg(ctx, a.pkg) }
            else -> Home(store, { screen = "all" }, { addRow = it })
        }
        if (act.idle) Idle(store)
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
    val fired = remember { booleanArrayOf(false) }
    val sc by animateFloatAsState(if (focused) 1.06f else 1f, label = "scale")
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier.scale(sc)
            .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
            .border(if (focused) 3.dp else 1.dp, if (focused) Color.White else Color(0x33FFFFFF), shape)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(c1, c2)))
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { e ->
                val ok = e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter
                if (onLong != null && e.key == Key.Menu && e.type == KeyEventType.KeyUp) {
                    onLong(); true
                } else if (onLong != null && ok && e.type == KeyEventType.KeyDown && e.nativeKeyEvent.repeatCount >= 1) {
                    if (!fired[0]) { fired[0] = true; onLong() }
                    true
                } else if (ok && e.type == KeyEventType.KeyUp && fired[0]) {
                    fired[0] = false; true
                } else false
            }
            .combinedClickable(onClick = onClick, onLongClick = onLong),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
fun TextTile(
    modifier: Modifier, name: String, c1: Color, c2: Color, txt: Color = Color.White, size: TextUnit = 18.sp,
    requester: FocusRequester? = null, onLong: (() -> Unit)? = null, onClick: () -> Unit
) {
    Tile(modifier, c1, c2, requester, onLong, onClick) {
        Text(name, color = txt, fontSize = size, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
fun AppTile(
    modifier: Modifier, pkg: String, big: Boolean = false,
    requester: FocusRequester? = null, onLong: (() -> Unit)? = null, onClick: () -> Unit
) {
    val pm = LocalContext.current.packageManager
    val known = KNOWN[pkg]
    val banner = remember(pkg) { try { pm.getApplicationBanner(pkg)?.toBitmap(320, 180)?.asImageBitmap() } catch (e: Exception) { null } }
    val icon = remember(pkg) { try { pm.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap() } catch (e: Exception) { null } }
    val label = remember(pkg) { labelOf(pm, pkg) }
    Tile(modifier, known?.c1 ?: Color(0xFF1B2552), known?.c2 ?: Color(0xFF10163A), requester, onLong, onClick) {
        if (banner != null) {
            Image(banner, label, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else if (known != null) {
            Text(known.name, color = known.txt, fontSize = if (big) 28.sp else 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2)
        } else {
            if (icon != null) Image(icon, null, Modifier.size(if (big) 56.dp else 30.dp))
            Text(label, color = Color.White, fontSize = if (big) 20.sp else 12.sp, maxLines = 1, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun OptionsDialog(title: String, options: List<Pair<String, () -> Unit>>, onClose: () -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(150); try { first.requestFocus() } catch (_: Exception) {} }
    Dialog(onDismissRequest = onClose) {
        Column(
            Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF0B1230)).padding(20.dp).width(300.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            options.forEachIndexed { i, o ->
                TextTile(Modifier.fillMaxWidth().height(44.dp), o.first, Color(0xFF1B2552), Color(0xFF10163A), size = 15.sp,
                    requester = if (i == 0) first else null, onClick = { o.second(); onClose() })
            }
        }
    }
}

@Composable
fun Wallpaper(store: Store) {
    val ctx = LocalContext.current
    val img = remember(store.bg, store.bgVersion) { if (store.bg == "img") loadBg(ctx) else null }
    Box(Modifier.fillMaxSize()) {
        if (img != null) {
            Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            val i = (store.bg.removePrefix("p").toIntOrNull() ?: 0).coerceIn(0, PRESETS.lastIndex)
            val cols = PRESETS[i].second
            Box(Modifier.fillMaxSize().background(if (i == 0) Brush.radialGradient(cols) else Brush.verticalGradient(cols)))
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x33000000), Color(0xB3000000)))))
    }
}

@Composable
fun Home(store: Store, onAll: () -> Unit, onAdd: (Int) -> Unit) {
    val ctx = LocalContext.current
    val pm = ctx.packageManager
    val now by rememberNow()
    val first = remember { FocusRequester() }
    var menu by remember { mutableStateOf<Triple<Int, Int, String>?>(null) }
    var bgDlg by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf(false) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                ctx.contentResolver.openInputStream(uri)?.use { inp ->
                    File(ctx.filesDir, "fondo.jpg").outputStream().use { out -> inp.copyTo(out) }
                }
                store.bg = "img"; store.bgVersion++; store.save()
            } catch (e: Exception) {
                Toast.makeText(ctx, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val favPkg = store.fav.ifEmpty { store.rows.firstOrNull { it.isNotEmpty() }?.first() ?: "" }
    LaunchedEffect(Unit) { delay(300); try { first.requestFocus() } catch (_: Exception) {} }
    BackHandler(enabled = true) { }

    Box(Modifier.fillMaxSize()) {
        Wallpaper(store)
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth().height(140.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(230.dp)) {
                    Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(now), color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black)
                    Text(SimpleDateFormat("EEE, d 'de' MMM.", Locale("es", "AR")).format(now), color = Color.White, fontSize = 15.sp)
                    Text(if (weatherText.isNotEmpty()) "$weatherText · $CIUDAD" else CIUDAD, color = Color(0xCCFFFFFF), fontSize = 13.sp, maxLines = 2)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DECO RICHAR", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
                    Text("LAUNCHER", color = GOLD, fontSize = 40.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
                    Text("TU TV BOX, TU MUNDO", color = Color.White, fontSize = 12.sp, letterSpacing = 3.sp)
                }
                if (favPkg.isNotEmpty()) {
                    Box(Modifier.width(230.dp).height(118.dp)) {
                        AppTile(Modifier.fillMaxSize(), favPkg, big = true, onClick = { launchPkg(ctx, favPkg) })
                        Text("FAVORITA", color = GOLD, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                                .background(Color(0xAA000000), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.weight(1f)) {
                Column(Modifier.width(88.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val side = Modifier.fillMaxWidth().height(50.dp)
                    val navy1 = Color(0xFF1B2552);
