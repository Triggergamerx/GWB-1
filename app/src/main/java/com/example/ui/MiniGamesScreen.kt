package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FlutterDash
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.WorldCatalog
import com.example.ui.theme.BerryPlum
import com.example.ui.theme.BlossomPink
import com.example.ui.theme.HoneyGold
import com.example.ui.theme.MeadowMint
import com.example.ui.theme.SkyAzure
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun MiniGamesHubAndPlayScreen(
    activeMiniGameId: String?,
    onSelectMiniGame: (String) -> Unit,
    onCloseMiniGame: () -> Unit,
    onCompleteMiniGame: (title: String, stars: Long, butterflies: Long) -> Unit,
    onBackToWorld: () -> Unit
) {
    BackHandler {
        if (activeMiniGameId != null) onCloseMiniGame() else onBackToWorld()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFE7F3), Color(0xFFE9F4FF))
                )
            )
            .safeDrawingPadding()
    ) {
        when (activeMiniGameId) {
            "butterfly_catch" -> ButterflyCatchMiniGame(
                onFinish = { score ->
                    val stars = (score * 4L).coerceAtLeast(10L)
                    onCompleteMiniGame("Butterfly Dance", stars, score.toLong().coerceAtLeast(1L))
                    onCloseMiniGame()
                },
                onBack = onCloseMiniGame
            )
            "berry_picnic" -> BerryPicnicMiniGame(
                onFinish = { score ->
                    val stars = (score * 5L).coerceAtLeast(12L)
                    onCompleteMiniGame("Bear's Berry Picnic", stars, 0L)
                    onCloseMiniGame()
                },
                onBack = onCloseMiniGame
            )
            "blossom_memory" -> BlossomMemoryMiniGame(
                onFinish = { rounds ->
                    val stars = (rounds * 8L).coerceAtLeast(12L)
                    onCompleteMiniGame("Pinky's Flower Chime", stars, 1L)
                    onCloseMiniGame()
                },
                onBack = onCloseMiniGame
            )
            "rainbow_hop" -> RainbowBridgeHopMiniGame(
                onFinish = { hops ->
                    val stars = (hops * 3L).coerceAtLeast(12L)
                    onCompleteMiniGame("Rainbow Bridge Hop", stars, 0L)
                    onCloseMiniGame()
                },
                onBack = onCloseMiniGame
            )
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(shape = CircleShape, color = Color.White, shadowElevation = 4.dp) {
                            IconButton(
                                onClick = onBackToWorld,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("minigames_back_button")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to World")
                            }
                        }
                        Column {
                            Text(
                                text = "Playful Mini-Games",
                                style = MaterialTheme.typography.headlineMedium,
                                color = BlossomPink
                            )
                            Text(
                                text = "Big friendly games for Maya, Barnaby & Pinky!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(WorldCatalog.portals, key = { it.id }) { portal ->
                            Card(
                                shape = RoundedCornerShape(26.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(26.dp))
                                    .clickable { onSelectMiniGame(portal.id) }
                                    .testTag("portal_card_${portal.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(portal.colorHex).copy(alpha = 0.18f),
                                            modifier = Modifier.size(56.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Rounded.SportsEsports,
                                                    contentDescription = portal.title,
                                                    tint = Color(portal.colorHex),
                                                    modifier = Modifier.size(30.dp)
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = portal.title,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = portal.subtitle,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { onSelectMiniGame(portal.id) },
                                        shape = RoundedCornerShape(22.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(portal.colorHex))
                                    ) {
                                        Text("Play!")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class MeadowTarget(
    val id: Int,
    var xNorm: Float,
    var yNorm: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    var caught: Boolean = false
)

@Composable
private fun ButterflyCatchMiniGame(
    onFinish: (Int) -> Unit,
    onBack: () -> Unit
) {
    var score by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableIntStateOf(18) }
    var tick by remember { mutableIntStateOf(0) }

    val targets = remember {
        mutableStateListOf(
            MeadowTarget(0, 0.25f, 0.35f, 0.008f, 0.005f, BlossomPink),
            MeadowTarget(1, 0.72f, 0.45f, -0.007f, 0.006f, SkyAzure),
            MeadowTarget(2, 0.48f, 0.65f, 0.006f, -0.007f, HoneyGold),
            MeadowTarget(3, 0.30f, 0.72f, 0.009f, -0.005f, BerryPlum),
            MeadowTarget(4, 0.68f, 0.28f, -0.008f, 0.004f, MeadowMint)
        )
    }

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(40L)
            tick++
            for (i in targets.indices) {
                val t = targets[i]
                var nx = t.xNorm + t.vx
                var ny = t.yNorm + t.vy
                var nvx = t.vx
                var nvy = t.vy
                if (nx < 0.12f || nx > 0.88f) { nvx = -nvx; nx = nx.coerceIn(0.12f, 0.88f) }
                if (ny < 0.16f || ny > 0.84f) { nvy = -nvy; ny = ny.coerceIn(0.16f, 0.84f) }
                targets[i] = t.copy(xNorm = nx, yNorm = ny, vx = nvx, vy = nvy)
            }
            if (tick % 25 == 0) {
                timeLeft--
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MiniGameHeader("Butterfly Dance", "Tap the fluttering butterflies!", score, timeLeft, onBack)

        if (timeLeft <= 0) {
            MiniGameCompleteCard(
                title = "Sparkling Butterfly Catch!",
                summary = "You befriended $score butterflies with Pinky!",
                onClaim = { onFinish(score) }
            )
        } else {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE7FFF4)),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                for (i in targets.indices) {
                                    val bf = targets[i]
                                    val bx = bf.xNorm * w
                                    val by = bf.yNorm * h
                                    val dist = sqrt((offset.x - bx) * (offset.x - bx) + (offset.y - by) * (offset.y - by))
                                    if (dist < 110f) {
                                        score++
                                        targets[i] = bf.copy(
                                            xNorm = (0.2f + ((i * 37 + tick) % 60) / 100f),
                                            yNorm = (0.2f + ((i * 53 + tick) % 60) / 100f)
                                        )
                                        break
                                    }
                                }
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    // Meadow flowers in background
                    for (f in 0..8) {
                        val fx = (f * 0.11f + 0.08f) * w
                        val fy = (0.78f + (f % 3) * 0.06f) * h
                        drawCircle(color = Color(0xFFFFB3D1), radius = 20f, center = Offset(fx, fy))
                        drawCircle(color = HoneyGold, radius = 8f, center = Offset(fx, fy))
                    }
                    // Butterflies
                    for (bf in targets) {
                        val cx = bf.xNorm * w
                        val cy = bf.yNorm * h
                        val flap = 0.55f + 0.45f * sin(tick * 0.35f + bf.id)
                        drawCircle(
                            color = bf.color.copy(alpha = 0.88f),
                            radius = 36f * flap,
                            center = Offset(cx - 26f * flap, cy - 10f)
                        )
                        drawCircle(
                            color = bf.color.copy(alpha = 0.88f),
                            radius = 36f * flap,
                            center = Offset(cx + 26f * flap, cy - 10f)
                        )
                        drawCircle(
                            color = bf.color.copy(alpha = 0.72f),
                            radius = 24f * flap,
                            center = Offset(cx - 18f * flap, cy + 20f)
                        )
                        drawCircle(
                            color = bf.color.copy(alpha = 0.72f),
                            radius = 24f * flap,
                            center = Offset(cx + 18f * flap, cy + 20f)
                        )
                        drawCircle(color = Color(0xFF3E1F47), radius = 10f, center = Offset(cx, cy))
                    }
                }
            }
        }
    }
}

@Composable
private fun BerryPicnicMiniGame(
    onFinish: (Int) -> Unit,
    onBack: () -> Unit
) {
    val treats = listOf(
        Triple("Pink Berry", BlossomPink, "Sweet pink strawberries!"),
        Triple("Honey Jar", HoneyGold, "Barnaby's golden honey!"),
        Triple("Blueberry", SkyAzure, "Juicy river blueberries!")
    )
    var targetIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var round by remember { mutableIntStateOf(0) }
    val totalRounds = 6

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        MiniGameHeader("Bear's Berry Picnic", "Give Barnaby the treat he asks for!", score, totalRounds - round, onBack)

        if (round >= totalRounds) {
            MiniGameCompleteCard(
                title = "Yummy Bear Picnic!",
                summary = "Barnaby Bear loved all $score treats you sorted!",
                onClaim = { onFinish(score) }
            )
        } else {
            val wanted = treats[targetIndex]
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Pets,
                        contentDescription = "Barnaby Bear",
                        tint = Color(0xFF8D5524),
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "Barnaby asks for: ${wanted.first}!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = wanted.second,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = wanted.third,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                treats.forEachIndexed { idx, treat ->
                    Card(
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = treat.second),
                        modifier = Modifier
                            .weight(1f)
                            .height(170.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .clickable {
                                if (idx == targetIndex) {
                                    score++
                                }
                                round++
                                targetIndex = (targetIndex + 1 + (round % 2)) % treats.size
                            }
                            .testTag("berry_basket_$idx")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocalFlorist,
                                contentDescription = treat.first,
                                tint = Color.White,
                                modifier = Modifier.size(46.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = treat.first,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlossomMemoryMiniGame(
    onFinish: (Int) -> Unit,
    onBack: () -> Unit
) {
    val petalColors = listOf(BlossomPink, HoneyGold, MeadowMint, SkyAzure)
    val petalNames = listOf("Pink Lotus", "Sun Petal", "Mint Leaf", "Sky Bell")

    var round by remember { mutableIntStateOf(1) }
    var sequence by remember { mutableStateOf(listOf(0, 1)) }
    var showingIndex by remember { mutableIntStateOf(-1) }
    var isShowing by remember { mutableStateOf(true) }
    var userStep by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(round, isShowing) {
        if (isShowing && !finished) {
            showingIndex = -1
            delay(350L)
            for (idx in sequence) {
                showingIndex = idx
                delay(550L)
                showingIndex = -1
                delay(220L)
            }
            isShowing = false
            userStep = 0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        MiniGameHeader(
            title = "Pinky's Flower Chime",
            subtitle = if (isShowing) "Watch the glowing petals!" else "Now tap the matching flowers!",
            score = round - 1,
            timeLeft = (4 - round).coerceAtLeast(0),
            onBack = onBack
        )

        if (finished) {
            MiniGameCompleteCard(
                title = "Musical Blossom Master!",
                summary = "Pinky danced to every glowing petal melody!",
                onClaim = { onFinish(3) }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (rowIdx in 0..1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (colIdx in 0..1) {
                            val idx = rowIdx * 2 + colIdx
                            val lit = showingIndex == idx
                            val bg by animateColorAsState(
                                targetValue = if (lit) petalColors[idx] else petalColors[idx].copy(alpha = 0.35f),
                                label = "petalGlow"
                            )
                            Surface(
                                shape = RoundedCornerShape(32.dp),
                                color = bg,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .border(
                                        width = if (lit) 5.dp else 2.dp,
                                        color = Color.White,
                                        shape = RoundedCornerShape(32.dp)
                                    )
                                    .clip(RoundedCornerShape(32.dp))
                                    .clickable(enabled = !isShowing) {
                                        if (sequence[userStep] == idx) {
                                            userStep++
                                            if (userStep >= sequence.size) {
                                                if (round >= 3) {
                                                    finished = true
                                                } else {
                                                    round++
                                                    sequence = sequence + ((round * 2 + 1) % 4)
                                                    isShowing = true
                                                }
                                            }
                                        } else {
                                            // Friendly retry for toddlers: replay gently!
                                            isShowing = true
                                        }
                                    }
                                    .testTag("memory_petal_$idx")
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocalFlorist,
                                        contentDescription = petalNames[idx],
                                        tint = Color.White,
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = petalNames[idx],
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RainbowBridgeHopMiniGame(
    onFinish: (Int) -> Unit,
    onBack: () -> Unit
) {
    var rowProgress by remember { mutableIntStateOf(0) }
    val goalRows = 6
    val safeLanes = remember { listOf(1, 0, 2, 1, 2, 0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        MiniGameHeader(
            title = "Rainbow Bridge Hop",
            subtitle = "Tap the glowing golden lilypad to hop across!",
            score = rowProgress,
            timeLeft = (goalRows - rowProgress).coerceAtLeast(0),
            onBack = onBack
        )

        if (rowProgress >= goalRows) {
            MiniGameCompleteCard(
                title = "Crossed the Crystal River!",
                summary = "Maya, Barnaby & Frog Boba cheered every hop!",
                onClaim = { onFinish(goalRows) }
            )
        } else {
            val targetLane = safeLanes[rowProgress]
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD6F4FF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Hop ${rowProgress + 1} of $goalRows",
                        style = MaterialTheme.typography.headlineMedium,
                        color = SkyAzure
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        for (lane in 0..2) {
                            val isGoldPad = lane == targetLane
                            Surface(
                                shape = CircleShape,
                                color = if (isGoldPad) HoneyGold else MeadowMint.copy(alpha = 0.55f),
                                shadowElevation = if (isGoldPad) 8.dp else 2.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(140.dp)
                                    .border(4.dp, Color.White, CircleShape)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (isGoldPad) {
                                            rowProgress++
                                        }
                                    }
                                    .testTag("lilypad_lane_$lane")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isGoldPad) Icons.Rounded.Star else Icons.Rounded.FlutterDash,
                                        contentDescription = if (isGoldPad) "Hop here" else "Lilypad",
                                        tint = Color.White,
                                        modifier = Modifier.size(46.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Tap the bright Golden Star lilypad!",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF0A4D7A)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniGameHeader(
    title: String,
    subtitle: String,
    score: Int,
    timeLeft: Int,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Surface(shape = CircleShape, color = Color.White, shadowElevation = 4.dp) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, color = BlossomPink)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Surface(shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 3.dp) {
            Text(
                text = "★ $score • $timeLeft",
                style = MaterialTheme.typography.titleMedium,
                color = BerryPlum,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun MiniGameCompleteCard(
    title: String,
    summary: String,
    onClaim: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = HoneyGold,
                modifier = Modifier.size(64.dp)
            )
            Text(title, style = MaterialTheme.typography.headlineMedium, color = BlossomPink, textAlign = TextAlign.Center)
            Text(summary, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Button(
                onClick = onClaim,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MeadowMint),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("claim_minigame_stars")
            ) {
                Text("Collect Golden Stars!", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
