package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FlutterDash
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DiscoveryCategory
import com.example.data.ExplorerProfile
import com.example.data.QuestProgress
import com.example.data.UiState
import com.example.data.WorldBiome
import com.example.data.WorldCatalog
import com.example.ui.theme.BlossomPink
import com.example.ui.theme.HoneyGold
import com.example.ui.theme.MeadowMint

@Composable
fun QuestsScreen(
    questsState: UiState<List<QuestProgress>>,
    onGoToBiome: (WorldBiome) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFF4D6), Color(0xFFFFF8FB))
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
                Surface(shape = CircleShape, color = Color.White, shadowElevation = 4.dp) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("quests_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to World")
                    }
                }
                Column {
                    Text(
                        text = "Woodland Quests",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color(0xFFB37400)
                    )
                    Text(
                        text = "Complete joyful adventures to earn Golden Stars!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when (questsState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Loading quests...", style = MaterialTheme.typography.titleLarge)
                    }
                }
                is UiState.Error -> {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = questsState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                is UiState.Success -> {
                    val quests = questsState.data
                    if (quests.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("All woodland quests ready soon!", style = MaterialTheme.typography.titleLarge)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(quests, key = { it.questId }) { quest ->
                                val seed = WorldCatalog.starterQuests.firstOrNull { it.questId == quest.questId }
                                val hintBiome = seed?.hintBiome ?: WorldBiome.MEADOW
                                val progress = (quest.currentCount.toFloat() / quest.targetCount.coerceAtLeast(1L).toFloat())
                                    .coerceIn(0f, 1f)

                                Card(
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (quest.completed) Color(0xFFE7FFF4) else Color.White
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = if (quest.completed) Icons.Rounded.CheckCircle else Icons.Rounded.Star,
                                                    contentDescription = null,
                                                    tint = if (quest.completed) MeadowMint else HoneyGold,
                                                    modifier = Modifier.size(30.dp)
                                                )
                                                Text(
                                                    text = quest.title,
                                                    style = MaterialTheme.typography.titleLarge,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = Color(0xFFFFF0C2)
                                            ) {
                                                Text(
                                                    text = "+${quest.rewardStars} ★",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = Color(0xFF8A5800),
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                )
                                            }
                                        }

                                        LinearProgressIndicator(
                                            progress = { progress },
                                            color = if (quest.completed) MeadowMint else BlossomPink,
                                            trackColor = Color(0xFFFFE5F1),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (quest.completed) "Completed! Hooray!" else "${quest.currentCount} of ${quest.targetCount} done",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (quest.completed) MeadowMint else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (!quest.completed) {
                                                Button(
                                                    onClick = { onGoToBiome(hintBiome) },
                                                    shape = RoundedCornerShape(18.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = BlossomPink)
                                                ) {
                                                    Icon(Icons.Rounded.Explore, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Visit ${hintBiome.title.substringBefore(" ")}")
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
        }
    }
}

@Composable
fun FloraAlmanacScreen(
    profile: ExplorerProfile?,
    onVisitEntryBiome: (WorldBiome) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var selectedCategory by remember { mutableStateOf<DiscoveryCategory?>(null) }
    val unlockedSet = profile?.discoveredIds?.toSet() ?: emptySet()

    val filtered = remember(selectedCategory) {
        if (selectedCategory == null) WorldCatalog.discoveries
        else WorldCatalog.discoveries.filter { it.category == selectedCategory }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFE7FFF4), Color(0xFFFFF8FB))
                )
            )
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(shape = CircleShape, color = Color.White, shadowElevation = 4.dp) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("almanac_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to World")
                    }
                }
                Column {
                    Text(
                        text = "Nature Discovery Book",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MeadowMint
                    )
                    Text(
                        text = "${unlockedSet.size} of ${WorldCatalog.discoveries.size} ecosystem wonders found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_flora_card_1795682629591),
                    contentDescription = "Magical woodland flora and butterflies sanctuary",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("All (${WorldCatalog.discoveries.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BlossomPink,
                        selectedLabelColor = Color.White
                    )
                )
                DiscoveryCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MeadowMint,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filtered, key = { it.id }) { entry ->
                    val unlocked = entry.id in unlockedSet
                    val icon = when (entry.category) {
                        DiscoveryCategory.BUTTERFLY -> Icons.Rounded.FlutterDash
                        DiscoveryCategory.FLORA -> Icons.Rounded.LocalFlorist
                        DiscoveryCategory.FAUNA -> Icons.Rounded.Pets
                    }
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (unlocked) Color.White else Color(0xFFF3EDF2)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (unlocked) 4.dp else 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { onVisitEntryBiome(entry.biome) }
                            .testTag("almanac_${entry.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(entry.accentColorHex).copy(alpha = if (unlocked) 0.22f else 0.08f),
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (unlocked) icon else Icons.Rounded.Lock,
                                        contentDescription = entry.name,
                                        tint = if (unlocked) Color(entry.accentColorHex) else Color.Gray,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (unlocked) entry.funFact else "Explore ${entry.biome.title} to discover!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (unlocked) Color(0xFFD9F7E6) else Color(0xFFFFE5F1)
                            ) {
                                Text(
                                    text = if (unlocked) "Found!" else "+${entry.starReward} ★",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (unlocked) Color(0xFF157A52) else BlossomPink,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
