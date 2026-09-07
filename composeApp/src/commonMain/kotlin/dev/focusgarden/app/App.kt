package dev.focusgarden.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

internal val ForestInk = Color(0xFF19382C)
internal val WarmCream = Color(0xFFF6F0E3)

@Composable
fun FocusGardenApp() {
    var page by remember { mutableIntStateOf(0) }
    var records by remember { mutableStateOf(GardenDataStore.loadRecords()) }
    var settings by remember { mutableStateOf(GardenDataStore.loadSettings()) }

    fun updateSettings(value: GardenSettings) {
        settings = value
        GardenDataStore.saveSettings(value)
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = ForestInk, background = WarmCream)) {
        Box(Modifier.fillMaxSize()) {
            AnimatedContent(page, label = "page") { current ->
                when (current) {
                    0 -> ImmersiveFocusScreen(settings, ::updateSettings) { duration, biome ->
                        records = GardenDataStore.addRecord(duration, biome)
                        GardenRuntime.platform.completionFeedback(settings.soundEnabled)
                    }
                    1 -> ForestCollection(records)
                    else -> SettingsScreen(settings, ::updateSettings)
                }
            }
            AppNavigation(page, { page = it }, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun ImmersiveFocusScreen(
    settings: GardenSettings,
    onSettings: (GardenSettings) -> Unit,
    onComplete: (Int, Biome) -> Unit,
) {
    var session by remember { mutableStateOf(FocusSession(settings.durationMinutes)) }
    var biome by remember { mutableStateOf(Biome.SERENE) }
    var showBiomes by remember { mutableStateOf(false) }
    var completionSaved by remember { mutableStateOf(false) }
    val growth by animateFloatAsState(session.progress, tween(900), label = "forest-growth")

    LaunchedEffect(session.status) {
        while (session.status == SessionStatus.Running) {
            delay(1_000L)
            session = session.tick()
        }
    }
    LaunchedEffect(session.status) {
        if (session.status == SessionStatus.Completed && !completionSaved) {
            completionSaved = true
            onComplete(session.durationMinutes, biome)
        }
    }

    Box(Modifier.fillMaxSize()) {
        LivingForest(biome, maxOf(growth, .04f), seed = 42)
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopBar(biome) { showBiomes = !showBiomes }
            AnimatedVisibility(showBiomes) {
                BiomeSelector(biome) { biome = it; showBiomes = false }
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth().padding(bottom = 70.dp), horizontalArrangement = Arrangement.End) {
                TimerGlassCard(
                    session = session,
                    onDuration = {
                        session = session.withDuration(it)
                        onSettings(settings.copy(durationMinutes = it))
                    },
                ) {
                    session = when (session.status) {
                        SessionStatus.Ready, SessionStatus.Paused -> session.start()
                        SessionStatus.Running -> session.pause()
                        SessionStatus.Completed -> {
                            completionSaved = false
                            if (settings.randomBiome) {
                                val index = (GardenRuntime.platform.nowMillis() % Biome.entries.size).toInt()
                                biome = Biome.entries[index]
                            }
                            FocusSession(settings.durationMinutes)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(biome: Biome, onBiomeClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("FOCUS GARDEN", color = Color.White.copy(.72f), fontSize = 10.sp, letterSpacing = 2.sp)
            Text("Cultive o silêncio", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        }
        Surface(Modifier.clickable(onClick = onBiomeClick), color = Color.White.copy(.18f), shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.padding(horizontal = 13.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                BiomeIcon(biome, Modifier.size(20.dp)); Spacer(Modifier.width(7.dp))
                Text(biome.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun BiomeSelector(selected: Biome, onSelect: (Biome) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 14.dp).background(Color(0xCC18352B), RoundedCornerShape(24.dp)).padding(9.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Biome.entries.forEach { biome ->
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                    .background(if (biome == selected) Color.White.copy(.18f) else Color.Transparent)
                    .clickable { onSelect(biome) }.padding(vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BiomeIcon(biome, Modifier.size(24.dp))
                Text(biome.shortTitle, color = Color.White, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun TimerGlassCard(
    session: FocusSession,
    onDuration: (Int) -> Unit,
    onAction: () -> Unit,
) {
    Surface(color = Color(0xDDFBF8EE), shape = RoundedCornerShape(30.dp), shadowElevation = 12.dp, modifier = Modifier.width(292.dp)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                (listOf(15, 25, 45, 60) + session.durationMinutes).distinct().sorted().take(5).forEach { minutes ->
                    Surface(
                        color = if (minutes == session.durationMinutes) ForestInk else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable(session.status == SessionStatus.Ready) { onDuration(minutes) },
                    ) {
                        Text("$minutes", color = if (minutes == session.durationMinutes) Color.White else ForestInk.copy(.55f),
                            fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            val min = session.remainingSeconds / 60
            val sec = session.remainingSeconds % 60
            Text("${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}", color = ForestInk, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Text(statusMessage(session), color = ForestInk.copy(.58f), fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            Button(onAction, Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(containerColor = ForestInk)) {
                FocusActionIcon(session.status)
                Spacer(Modifier.width(9.dp))
                Text(when (session.status) {
                    SessionStatus.Ready -> "Começar a cultivar"
                    SessionStatus.Running -> "Pausar"
                    SessionStatus.Paused -> "Continuar"
                    SessionStatus.Completed -> "Guardar e recomeçar"
                }, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun statusMessage(session: FocusSession) = when (session.status) {
    SessionStatus.Ready -> "O bosque espera por você"
    SessionStatus.Paused -> "Seu bosque está descansando"
    SessionStatus.Completed -> "Seu bosque floresceu e foi guardado!"
    SessionStatus.Running -> when {
        session.progress < .2f -> "A terra começa a despertar"
        session.progress < .4f -> "A grama está cobrindo o campo"
        session.progress < .65f -> "Uma árvore encontra a luz"
        session.progress < .85f -> "As primeiras flores se abrem"
        else -> "O bosque está ganhando vida"
    }
}

@Composable
private fun ForestCollection(records: List<GardenRecord>) {
    val totalMinutes = records.sumOf { it.durationMinutes }
    val backgroundBiome = records.firstOrNull()?.biome ?: Biome.SERENE
    Box(Modifier.fillMaxSize()) {
        LivingForest(backgroundBiome, 1f, seed = 91)
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp).padding(bottom = 80.dp)) {
            Text("Seu mundo", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text(if (records.isEmpty()) "Conclua sua primeira sessão" else "${currentStreak(records)} dias cultivando presença",
                color = Color.White.copy(.72f), fontSize = 14.sp)
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatBubble("${records.size}", "bosques", Modifier.weight(1f))
                StatBubble(formatMinutes(totalMinutes), "de foco", Modifier.weight(1f))
                StatBubble("${currentStreak(records)}", "sequência", Modifier.weight(1f))
            }
            Spacer(Modifier.height(18.dp))
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (records.isEmpty()) EmptyCollectionCard()
                records.take(20).forEachIndexed { index, record -> RecordCard(record, records.size - index) }
            }
        }
    }
}

@Composable private fun EmptyCollectionCard() {
    Surface(color = Color(0xDDFBF8EE), shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.fillMaxWidth().padding(22.dp)) {
            Text("Seu primeiro bosque começa no foco", color = ForestInk, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("Quando o cronômetro terminar, ele aparecerá aqui.", color = ForestInk.copy(.56f), fontSize = 12.sp)
        }
    }
}

@Composable private fun RecordCard(record: GardenRecord, number: Int) {
    Surface(color = Color(0xDDFBF8EE), shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
            BiomeIcon(record.biome, Modifier.size(24.dp), record.biome.accent)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${record.biome.title} #$number", color = ForestInk, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${record.dateKey} · ${record.durationMinutes} minutos", color = ForestInk.copy(.56f), fontSize = 11.sp)
            }
            Text("✓", color = Color(0xFF5E895B), fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatMinutes(minutes: Int): String = if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${minutes % 60}"

@Composable
private fun BiomeIcon(biome: Biome, modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier) {
        val strokeWidth = size.minDimension * .09f
        val stroke = Stroke(strokeWidth, cap = StrokeCap.Round)
        when (biome) {
            Biome.SERENE -> {
                val tree = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * .5f, size.height * .12f)
                    lineTo(size.width * .18f, size.height * .62f)
                    lineTo(size.width * .38f, size.height * .62f)
                    lineTo(size.width * .25f, size.height * .82f)
                    lineTo(size.width * .75f, size.height * .82f)
                    lineTo(size.width * .62f, size.height * .62f)
                    lineTo(size.width * .82f, size.height * .62f)
                    close()
                }
                drawPath(tree, color)
                drawLine(color, center.copy(y = size.height * .8f), center.copy(y = size.height * .94f), strokeWidth, StrokeCap.Round)
            }
            Biome.SAKURA -> {
                val petalRadius = size.minDimension * .18f
                listOf(
                    .5f to .27f, .7f to .43f, .62f to .68f, .38f to .68f, .3f to .43f,
                ).forEach { (x, y) -> drawCircle(color, petalRadius, center.copy(x = size.width * x, y = size.height * y)) }
                drawCircle(Color(0xFFFFE0E3), size.minDimension * .105f)
            }
            Biome.NIGHT -> {
                drawArc(color, startAngle = 70f, sweepAngle = 235f, useCenter = false, topLeft = center.copy(x = size.width * .14f, y = size.height * .14f), size = androidx.compose.ui.geometry.Size(size.width * .62f, size.height * .62f), style = stroke)
                drawLine(color, center.copy(x = size.width * .26f, y = size.height * .2f), center.copy(x = size.width * .65f, y = size.height * .69f), strokeWidth, StrokeCap.Round)
                drawCircle(color, size.minDimension * .055f, center.copy(x = size.width * .79f, y = size.height * .25f))
            }
            Biome.AUTUMN -> {
                val leaf = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * .15f, size.height * .54f)
                    cubicTo(size.width * .28f, size.height * .12f, size.width * .75f, size.height * .12f, size.width * .84f, size.height * .2f)
                    cubicTo(size.width * .89f, size.height * .62f, size.width * .57f, size.height * .86f, size.width * .25f, size.height * .77f)
                    close()
                }
                drawPath(leaf, color)
                drawLine(color, center.copy(x = size.width * .22f, y = size.height * .83f), center.copy(x = size.width * .73f, y = size.height * .3f), strokeWidth, StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun SettingsScreen(settings: GardenSettings, onChange: (GardenSettings) -> Unit) {
    Box(Modifier.fillMaxSize().background(WarmCream)) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(24.dp).padding(bottom = 85.dp).verticalScroll(rememberScrollState()),
        ) {
            Text("Ajustes", color = ForestInk, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text("Deixe o jardim no seu ritmo", color = ForestInk.copy(.58f), fontSize = 14.sp)
            Spacer(Modifier.height(22.dp))
            SettingsCard {
                SettingSwitch("Bioma aleatório", "Sorteia um novo cenário após cada sessão", settings.randomBiome) {
                    onChange(settings.copy(randomBiome = it))
                }
                HorizontalDivider(color = ForestInk.copy(.1f))
                SettingSwitch("Som ao concluir", "Toque suave quando o bosque florescer", settings.soundEnabled) {
                    onChange(settings.copy(soundEnabled = it))
                }
            }
            Spacer(Modifier.height(14.dp))
            SettingsCard {
                Text("Duração padrão", color = ForestInk, fontWeight = FontWeight.Bold)
                Text("${settings.durationMinutes} minutos", color = ForestInk.copy(.58f), fontSize = 13.sp)
                Slider(
                    value = settings.durationMinutes.toFloat(),
                    onValueChange = { value ->
                        val rounded = ((value / 5).toInt() * 5).coerceAtLeast(5)
                        onChange(settings.copy(durationMinutes = rounded))
                    },
                    valueRange = 5f..120f,
                    steps = 22,
                    colors = SliderDefaults.colors(thumbColor = ForestInk, activeTrackColor = ForestInk),
                )
                Text("A nova duração vale para a próxima sessão.", color = ForestInk.copy(.48f), fontSize = 11.sp)
            }
        }
    }
}

@Composable private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Color.White.copy(.82f), shape = RoundedCornerShape(24.dp), shadowElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable private fun SettingSwitch(title: String, description: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = ForestInk, fontWeight = FontWeight.SemiBold)
            Text(description, color = ForestInk.copy(.52f), fontSize = 11.sp)
        }
        Switch(checked, onChecked, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ForestInk))
    }
}

@Composable private fun StatBubble(value: String, label: String, modifier: Modifier) {
    Column(modifier.background(Color.White.copy(.17f), RoundedCornerShape(20.dp)).padding(13.dp)) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = Color.White.copy(.65f), fontSize = 10.sp)
    }
}

@Composable
private fun AppNavigation(page: Int, onPage: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.navigationBarsPadding().padding(bottom = 12.dp).background(Color(0xDD142D25), RoundedCornerShape(28.dp)).padding(5.dp)) {
        listOf("Foco", "Mundo", "Ajustes").forEachIndexed { index, label ->
            Row(
                Modifier.clip(RoundedCornerShape(23.dp))
                    .background(if (page == index) Color.White.copy(.18f) else Color.Transparent)
                    .clickable { onPage(index) }.padding(horizontal = 13.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavigationIcon(index)
                if (page == index) { Spacer(Modifier.width(6.dp)); Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium) }
            }
        }
    }
}

@Composable
private fun NavigationIcon(index: Int) {
    Canvas(Modifier.size(20.dp)) {
        val line = size.minDimension * .105f
        val stroke = Stroke(width = line, cap = StrokeCap.Round)
        when (index) {
            0 -> {
                drawCircle(Color.White, radius = size.minDimension * .36f, style = stroke)
                drawCircle(Color.White, radius = size.minDimension * .11f)
            }
            1 -> {
                drawLine(Color.White, center.copy(y = size.height * .45f), center.copy(y = size.height * .84f), line, StrokeCap.Round)
                drawLine(Color.White, center.copy(y = size.height * .7f), center.copy(x = size.width * .34f, y = size.height * .82f), line, StrokeCap.Round)
                drawLine(Color.White, center.copy(y = size.height * .7f), center.copy(x = size.width * .66f, y = size.height * .82f), line, StrokeCap.Round)
                drawCircle(Color.White, center = center.copy(y = size.height * .34f), radius = size.minDimension * .25f, style = stroke)
            }
            else -> {
                val ys = listOf(.27f, .5f, .73f)
                val knobs = listOf(.35f, .66f, .44f)
                ys.forEachIndexed { i, y ->
                    drawLine(Color.White, center.copy(x = size.width * .17f, y = size.height * y), center.copy(x = size.width * .83f, y = size.height * y), line, StrokeCap.Round)
                    drawCircle(Color(0xFF18352B), center = center.copy(x = size.width * knobs[i], y = size.height * y), radius = line * 1.15f)
                    drawCircle(Color.White, center = center.copy(x = size.width * knobs[i], y = size.height * y), radius = line * .72f)
                }
            }
        }
    }
}

@Composable
private fun FocusActionIcon(status: SessionStatus) {
    Canvas(Modifier.size(18.dp)) {
        when (status) {
            SessionStatus.Running -> {
                val barWidth = size.width * .2f
                drawRoundRect(Color.White, topLeft = center.copy(x = size.width * .22f, y = size.height * .19f), size = androidx.compose.ui.geometry.Size(barWidth, size.height * .62f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth * .35f))
                drawRoundRect(Color.White, topLeft = center.copy(x = size.width * .58f, y = size.height * .19f), size = androidx.compose.ui.geometry.Size(barWidth, size.height * .62f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth * .35f))
            }
            SessionStatus.Completed -> {
                drawArc(Color.White, startAngle = -55f, sweepAngle = 285f, useCenter = false, style = Stroke(size.width * .12f, cap = StrokeCap.Round), topLeft = center.copy(x = size.width * .16f, y = size.height * .16f), size = androidx.compose.ui.geometry.Size(size.width * .68f, size.height * .68f))
                drawLine(Color.White, center.copy(x = size.width * .2f, y = size.height * .16f), center.copy(x = size.width * .2f, y = size.height * .4f), size.width * .12f, StrokeCap.Round)
                drawLine(Color.White, center.copy(x = size.width * .2f, y = size.height * .16f), center.copy(x = size.width * .43f, y = size.height * .17f), size.width * .12f, StrokeCap.Round)
            }
            else -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * .32f, size.height * .2f)
                    lineTo(size.width * .8f, size.height * .5f)
                    lineTo(size.width * .32f, size.height * .8f)
                    close()
                }
                drawPath(path, Color.White)
            }
        }
    }
}
