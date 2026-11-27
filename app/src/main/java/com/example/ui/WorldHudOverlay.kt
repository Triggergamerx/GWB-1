package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FlutterDash
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.VideogameAsset
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ExplorerProfile
import com.example.data.MeadowRoom
import com.example.data.QuestProgress
import com.example.data.SafeEmote
import com.example.data.WorldBiome
import com.example.data.WorldCatalog
import com.example.engine3d.GwbWorldGlView
import com.example.engine3d.GwbWorldSimulation
import com.example.engine3d.WorldHudSnapshot
import com.example.ui.theme.BerryPlum
import com.example.ui.theme.BlossomPink
import com.example.ui.theme.HoneyGold
import com.example.ui.theme.MeadowMint
import com.example.ui.theme.SkyAzure
import com.example.viewmodel.GwbScreenRoute
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun WorldHudScreen(
    simulation: GwbWorldSimulation,
    hudSnapshot: WorldHudSnapshot,
    profile: ExplorerProfile?,
    quests: List<QuestProgress>,
    activeRoom: MeadowRoom?,
    friendCount: Int,
    celebrationBanner: String?,
    onNavigate: (GwbScreenRoute) -> Unit,
    onJump: () -> Unit,
    onSparkleNet: () -> Unit,
    onBearBoost: () -> Unit,
    onDiscoverNearby: (com.example.data.FloraFaunaEntry) -> Unit,
    onEnterPortal: (String) -> Unit,
    onSendEmote: (SafeEmote) -> Unit,
    onDismissCelebration: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 700.dp

        // Full-bleed OpenGL ES 3.0 3D Open World
        GwbWorldGlView(
            simulation = simulation,
            modifier = Modifier.fillMaxSize()
        )

        // Top HUD: Mini-map radar + Counters + Quest Pill
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // GTA-style circular Mini-Map Radar (tap for full World Map)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    MiniMapRadar(
                        hudSnapshot = hudSnapshot,
                        onClick = { onNavigate(GwbScreenRoute.MAP) }
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Biome Pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.90f),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Explore,
                                    contentDescription = "Current biome",
                                    tint = Color(hudSnapshot.currentBiome.colorHex),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = hudSnapshot.currentBiome.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Active Quest Tracker Pill
                        val activeQuest = quests.firstOrNull { !it.completed }
                        if (activeQuest != null) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFFFFF5D6).copy(alpha = 0.94f),
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .clickable { onNavigate(GwbScreenRoute.QUESTS) }
                                    .testTag("active_quest_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.TaskAlt,
                                        contentDescription = "Active quest",
                                        tint = Color(0xFFB37400),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${activeQuest.title} (${activeQuest.currentCount}/${activeQuest.targetCount})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF4A3200),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Top-Right Stats & Camera Snap
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HudStatPill(
                            icon = Icons.Rounded.Star,
                            value = (profile?.stars ?: 0L).toString(),
                            tint = HoneyGold,
                            contentDesc = stringResource(R.string.world_hud_stars)
                        )
                        HudStatPill(
                            icon = Icons.Rounded.FlutterDash,
                            value = (profile?.butterfliesCaught ?: 0L).toString(),
                            tint = BlossomPink,
                            contentDesc = stringResource(R.string.world_hud_butterflies)
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.90f),
                            shadowElevation = 4.dp
                        ) {
                            IconButton(
                                onClick = { simulation.snapCameraBehindPlayer() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("snap_camera_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MyLocation,
                                    contentDescription = "Center camera behind Maya",
                                    tint = SkyAzure
                                )
                            }
                        }
                    }

                    if (activeRoom != null) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MeadowMint.copy(alpha = 0.95f),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .clickable { onNavigate(GwbScreenRoute.FRIENDS) }
                                .testTag("active_room_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Groups,
                                    contentDescription = "Multiplayer room active",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Room ${activeRoom.roomCode} • $friendCount",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Celebration Toast Banner
            AnimatedVisibility(
                visible = celebrationBanner != null,
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 }
            ) {
                if (celebrationBanner != null) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = BlossomPink,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clickable { onDismissCelebration() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Text(
                                text = celebrationBanner,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Bottom Controls & Contextual Discovery / Portal Prompt
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Contextual 1-Tap Nearby Discovery or Mini-Game Portal Prompt
            val nearbyPortal = hudSnapshot.nearbyPortal
            val nearbyDiscovery = hudSnapshot.nearbyDiscovery
            if (nearbyPortal != null) {
                Button(
                    onClick = { onEnterPortal(nearbyPortal.id) },
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(nearbyPortal.colorHex),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("nearby_portal_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SportsEsports,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Play ${nearbyPortal.title}!",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else if (nearbyDiscovery != null) {
                val alreadyFound = profile?.discoveredIds?.contains(nearbyDiscovery.id) == true
                Button(
                    onClick = { onDiscoverNearby(nearbyDiscovery) },
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (alreadyFound) MeadowMint else BlossomPink,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("nearby_discovery_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFlorist,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (alreadyFound) "Say Hi to ${nearbyDiscovery.name}" else "Discover ${nearbyDiscovery.name}!",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // Safe Emote Strip when in a Multiplayer Room
            if (activeRoom != null) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SafeEmote.entries.forEach { emote ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.92f),
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .clickable { onSendEmote(emote) }
                                .testTag("emote_${emote.id}")
                        ) {
                            Text(
                                text = emote.badge,
                                style = MaterialTheme.typography.labelLarge,
                                color = BerryPlum,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Virtual Thumbstick (Left) + Big Action Buttons (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                VirtualThumbstick(
                    onMove = { jx, jy ->
                        simulation.joyX = jx
                        simulation.joyY = jy
                    }
                )

                if (isWide) {
                    QuickNavDock(
                        onNavigate = onNavigate,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Right Toddler Action Cluster
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Bear Boost
                    ActionCircleButton(
                        icon = Icons.Rounded.Pets,
                        label = stringResource(R.string.btn_bear_boost),
                        color = if (hudSnapshot.bearBoostActive) BlossomPink else HoneyGold,
                        sizeDp = 58,
                        enabled = hudSnapshot.bearBoostCooldown <= 0f,
                        onClick = onBearBoost,
                        tag = "bear_boost_button"
                    )
                    // Jump & Twirl
                    ActionCircleButton(
                        icon = Icons.Rounded.ArrowUpward,
                        label = stringResource(R.string.btn_jump),
                        color = MeadowMint,
                        sizeDp = 64,
                        enabled = true,
                        onClick = onJump,
                        tag = "jump_button"
                    )
                    // Sparkle Net
                    ActionCircleButton(
                        icon = Icons.Rounded.FlutterDash,
                        label = stringResource(R.string.btn_catch),
                        color = BlossomPink,
                        sizeDp = 72,
                        enabled = true,
                        onClick = onSparkleNet,
                        tag = "sparkle_net_button"
                    )
                }
            }

            if (!isWide) {
                QuickNavDock(onNavigate = onNavigate)
            }
        }
    }
}

@Composable
private fun MiniMapRadar(
    hudSnapshot: WorldHudSnapshot,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = Color(0xFFFFF8FB).copy(alpha = 0.92f),
        shadowElevation = 6.dp,
        modifier = Modifier
            .size(92.dp)
            .border(3.dp, BlossomPink, CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag("minimap_radar")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val scale = w / 150f
            val cx = w / 2f
            val cy = h / 2f

            // Meadow background
            drawCircle(color = Color(0xFFD9F7E6), radius = w * 0.48f)

            // Winding river curve
            val riverPath = Path()
            for (step in -70..70 step 10) {
                val z = step.toFloat()
                val x = GwbWorldSimulation.riverCenterX(z)
                val px = cx + x * scale
                val py = cy + z * scale
                if (step == -70) riverPath.moveTo(px, py) else riverPath.lineTo(px, py)
            }
            drawPath(
                path = riverPath,
                color = Color(0xFF4CC9F0),
                style = Stroke(width = 7f, cap = StrokeCap.Round)
            )

            // Biome dots
            WorldBiome.entries.forEach { biome ->
                drawCircle(
                    color = Color(biome.colorHex),
                    radius = 5.5f,
                    center = Offset(cx + biome.centerX * scale, cy + biome.centerZ * scale)
                )
            }

            // Friend dots
            hudSnapshot.friendPositions.forEach { (fx, fz) ->
                drawCircle(
                    color = BerryPlum,
                    radius = 5f,
                    center = Offset(cx + fx * scale, cy + fz * scale)
                )
            }

            // Player dot
            val px = (cx + hudSnapshot.playerX * scale).coerceIn(10f, w - 10f)
            val py = (cy + hudSnapshot.playerZ * scale).coerceIn(10f, h - 10f)
            drawCircle(color = Color.White, radius = 8f, center = Offset(px, py))
            drawCircle(color = BlossomPink, radius = 5.5f, center = Offset(px, py))
        }
    }
}

@Composable
private fun HudStatPill(
    icon: ImageVector,
    value: String,
    tint: Color,
    contentDesc: String
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.92f),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDesc,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun VirtualThumbstick(
    onMove: (Float, Float) -> Unit
) {
    var knobX by remember { mutableFloatStateOf(0f) }
    var knobY by remember { mutableFloatStateOf(0f) }
    val maxRadiusPx = 105f

    Box(
        modifier = Modifier
            .size(132.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.42f),
                        Color(0xFFFFD8E8).copy(alpha = 0.30f)
                    )
                )
            )
            .border(2.5.dp, Color.White.copy(alpha = 0.78f), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        knobX = 0f
                        knobY = 0f
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        knobX = 0f
                        knobY = 0f
                        onMove(0f, 0f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val nx = knobX + dragAmount.x
                        val ny = knobY + dragAmount.y
                        val dist = sqrt(nx * nx + ny * ny)
                        if (dist > maxRadiusPx) {
                            knobX = (nx / dist) * maxRadiusPx
                            knobY = (ny / dist) * maxRadiusPx
                        } else {
                            knobX = nx
                            knobY = ny
                        }
                        onMove(knobX / maxRadiusPx, knobY / maxRadiusPx)
                    }
                )
            }
            .testTag("virtual_thumbstick"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = BlossomPink.copy(alpha = 0.90f),
            shadowElevation = 6.dp,
            modifier = Modifier
                .offset { IntOffset(knobX.roundToInt(), knobY.roundToInt()) }
                .size(54.dp)
                .border(2.dp, Color.White, CircleShape)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.FlutterDash,
                    contentDescription = "Move Maya",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun ActionCircleButton(
    icon: ImageVector,
    label: String,
    color: Color,
    sizeDp: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = if (enabled) color else Color.Gray.copy(alpha = 0.55f),
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(sizeDp.dp)
                .border(2.5.dp, Color.White, CircleShape)
                .clip(CircleShape)
                .clickable(enabled = enabled, onClick = onClick)
                .testTag(tag)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size((sizeDp * 0.48f).dp)
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.Black.copy(alpha = 0.45f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun QuickNavDock(
    onNavigate: (GwbScreenRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.94f),
        shadowElevation = 6.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockItem(
                icon = Icons.Rounded.Map,
                label = stringResource(R.string.btn_map),
                tint = SkyAzure,
                onClick = { onNavigate(GwbScreenRoute.MAP) },
                tag = "nav_map"
            )
            DockItem(
                icon = Icons.Rounded.TaskAlt,
                label = stringResource(R.string.btn_quests),
                tint = HoneyGold,
                onClick = { onNavigate(GwbScreenRoute.QUESTS) },
                tag = "nav_quests"
            )
            DockItem(
                icon = Icons.Rounded.LocalFlorist,
                label = stringResource(R.string.btn_almanac),
                tint = MeadowMint,
                onClick = { onNavigate(GwbScreenRoute.ALMANAC) },
                tag = "nav_almanac"
            )
            DockItem(
                icon = Icons.Rounded.VideogameAsset,
                label = stringResource(R.string.btn_games),
                tint = BlossomPink,
                onClick = { onNavigate(GwbScreenRoute.MINI_GAMES) },
                tag = "nav_games"
            )
            DockItem(
                icon = Icons.Rounded.Groups,
                label = stringResource(R.string.btn_friends),
                tint = BerryPlum,
                onClick = { onNavigate(GwbScreenRoute.FRIENDS) },
                tag = "nav_friends"
            )
            DockItem(
                icon = Icons.Rounded.Checkroom,
                label = stringResource(R.string.btn_wardrobe),
                tint = BlossomPink,
                onClick = { onNavigate(GwbScreenRoute.WARDROBE) },
                tag = "nav_wardrobe"
            )
        }
    }
}

@Composable
private fun DockItem(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    tag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(tag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
