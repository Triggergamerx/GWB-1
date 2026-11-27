package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FlightTakeoff
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.MiniGamePortal
import com.example.data.WorldBiome
import com.example.data.WorldCatalog
import com.example.engine3d.GwbWorldSimulation
import com.example.engine3d.WorldHudSnapshot
import com.example.ui.theme.BerryPlum
import com.example.ui.theme.BlossomPink
import com.example.ui.theme.MeadowMint

@Composable
fun WorldMapScreen(
    hudSnapshot: WorldHudSnapshot,
    onFastTravelBiome: (WorldBiome) -> Unit,
    onFastTravelPortal: (MiniGamePortal) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFEDF5), Color(0xFFE9FFF4))
                )
            )
            .safeDrawingPadding()
    ) {
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
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("map_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back to 3D World"
                        )
                    }
                }
                Column {
                    Text(
                        text = "GWB-1 Open World Map",
                        style = MaterialTheme.typography.headlineMedium,
                        color = BlossomPink
                    )
                    Text(
                        text = "Tap any magical biome to fly there with Maya & Barnaby!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Interactive Vector Map Canvas
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFDFF9EC)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val scaleX = w / 120f
                    val scaleZ = h / 110f
                    val cx = w / 2f
                    val cy = h / 2f

                    // Meadow blossom corner glow
                    drawCircle(
                        color = Color(0xFFFFD6E8),
                        radius = w * 0.25f,
                        center = Offset(cx + 24f * scaleX, cy - 16f * scaleZ)
                    )
                    // Starlight corner glow
                    drawCircle(
                        color = Color(0xFFE9D8FD),
                        radius = w * 0.22f,
                        center = Offset(cx + 28f * scaleX, cy + 32f * scaleZ)
                    )

                    // Crystal River
                    val riverPath = Path()
                    for (zStep in -55..55 step 5) {
                        val x = GwbWorldSimulation.riverCenterX(zStep.toFloat())
                        val px = cx + x * scaleX
                        val py = cy + zStep * scaleZ
                        if (zStep == -55) riverPath.moveTo(px, py) else riverPath.lineTo(px, py)
                    }
                    drawPath(
                        path = riverPath,
                        color = Color(0xFF4CC9F0),
                        style = Stroke(width = 18f, cap = StrokeCap.Round)
                    )

                    // Mini-game portals (gold diamonds/circles)
                    WorldCatalog.portals.forEach { portal ->
                        drawCircle(
                            color = Color(portal.colorHex),
                            radius = 12f,
                            center = Offset(cx + portal.worldX * scaleX, cy + portal.worldZ * scaleZ)
                        )
                    }

                    // Biomes
                    WorldBiome.entries.forEach { biome ->
                        val bx = cx + biome.centerX * scaleX
                        val by = cy + biome.centerZ * scaleZ
                        drawCircle(
                            color = Color.White,
                            radius = 18f,
                            center = Offset(bx, by)
                        )
                        drawCircle(
                            color = Color(biome.colorHex),
                            radius = 13f,
                            center = Offset(bx, by)
                        )
                    }

                    // Friends
                    hudSnapshot.friendPositions.forEach { (fx, fz) ->
                        drawCircle(
                            color = BerryPlum,
                            radius = 11f,
                            center = Offset(cx + fx * scaleX, cy + fz * scaleZ)
                        )
                    }

                    // Maya & Bear current marker
                    val px = (cx + hudSnapshot.playerX * scaleX).coerceIn(18f, w - 18f)
                    val py = (cy + hudSnapshot.playerZ * scaleZ).coerceIn(18f, h - 18f)
                    drawCircle(color = Color.White, radius = 16f, center = Offset(px, py))
                    drawCircle(color = BlossomPink, radius = 11f, center = Offset(px, py))
                }
            }

            // Fast Travel Biome Cards
            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(WorldBiome.entries) { biome ->
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { onFastTravelBiome(biome) }
                            .testTag("fast_travel_${biome.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(biome.colorHex).copy(alpha = 0.2f),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Explore,
                                            contentDescription = null,
                                            tint = Color(biome.colorHex)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = biome.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = biome.subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Button(
                                onClick = { onFastTravelBiome(biome) },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MeadowMint)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FlightTakeoff,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Go!")
                            }
                        }
                    }
                }
            }
        }
    }
}
