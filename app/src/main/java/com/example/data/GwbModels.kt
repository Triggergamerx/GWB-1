package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class ExplorerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Little Maya",
    val avatarId: String = "maya",
    val stars: Long = 0L,
    val butterfliesCaught: Long = 0L,
    val discoveredIds: List<String> = emptyList(),
    val activeQuestId: String = "quest_butterflies",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuestProgress(
    @DocumentId val id: String = "",
    val questId: String = "",
    val title: String = "",
    val targetCount: Long = 1L,
    val currentCount: Long = 0L,
    val completed: Boolean = false,
    val rewardStars: Long = 10L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MeadowRoom(
    @DocumentId val id: String = "",
    val roomCode: String = "",
    val roomName: String = "",
    val hostUid: String = "",
    val biomeTheme: String = "meadow",
    val openForFriends: Boolean = true,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoomPlayer(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val playerUid: String = "",
    val displayName: String = "Friend",
    val avatarId: String = "pinky",
    val posX: Double = 0.0,
    val posZ: Double = 0.0,
    val yaw: Double = 0.0,
    val emote: String = "",
    val roomOpen: Boolean = true,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class AvatarStyle(
    val id: String,
    val title: String,
    val subtitle: String,
    val primaryHex: Long,
    val accentHex: Long
) {
    MAYA("maya", "Maya Explorer", "Pink hooded woodland explorer", 0xFFFF4D94, 0xFFFFD166),
    PINKY("pinky", "Pinky Fairy", "Iridescent butterfly wings & sparkles", 0xFFFF7EB6, 0xFF8338EC),
    BARNABY("barnaby", "Barnaby Cub", "Cozy knitted scarf & honey paws", 0xFF8D5524, 0xFFFFB703),
    BLOSSOM("blossom", "Blossom Star", "Starlight lavender crown & cape", 0xFFA855F7, 0xFF2EC486);

    companion object {
        fun fromId(raw: String): AvatarStyle =
            entries.firstOrNull { it.id == raw } ?: MAYA
    }
}

enum class WorldBiome(
    val id: String,
    val title: String,
    val subtitle: String,
    val centerX: Float,
    val centerZ: Float,
    val colorHex: Long
) {
    COTTAGE("cottage", "Bear's Cozy Cottage", "Warm cabin & honey picnic", -24f, -18f, 0xFFFFB703),
    MEADOW("meadow", "Pinky Blossom Meadow", "Sakura trees & fluttering butterflies", 24f, -16f, 0xFFFF4D94),
    RIVER("river", "Crystal River Bridge", "Sparkling water & lilypad hops", 0f, 4f, 0xFF3A86FF),
    TREEHOUSE("treehouse", "Treehouse Picnic Plaza", "Giant oak hangout for friends", -26f, 34f, 0xFF2EC486),
    STARLIGHT("starlight", "Starlight Rainbow Hills", "Glowing mushrooms & rare monarchs", 28f, 36f, 0xFF8338EC);

    companion object {
        fun fromId(raw: String): WorldBiome =
            entries.firstOrNull { it.id == raw } ?: MEADOW
    }
}

enum class SafeEmote(
    val id: String,
    val label: String,
    val badge: String
) {
    WAVE("wave", "Hello Friend!", "Wave"),
    BUTTERFLY("butterfly", "Look, a Butterfly!", "Flutter"),
    HEART("heart", "Bear Hug!", "Hug"),
    PICNIC("picnic", "Picnic Time!", "Berry"),
    DANCE("dance", "Happy Twirl!", "Twirl")
}

enum class DiscoveryCategory(val label: String) {
    BUTTERFLY("Butterflies"),
    FLORA("Magical Flora"),
    FAUNA("Woodland Friends")
}

data class FloraFaunaEntry(
    val id: String,
    val name: String,
    val category: DiscoveryCategory,
    val biome: WorldBiome,
    val worldX: Float,
    val worldZ: Float,
    val funFact: String,
    val starReward: Int,
    val accentColorHex: Long
)

data class MiniGamePortal(
    val id: String,
    val title: String,
    val subtitle: String,
    val biome: WorldBiome,
    val worldX: Float,
    val worldZ: Float,
    val colorHex: Long
)

data class QuestSeed(
    val questId: String,
    val title: String,
    val targetCount: Long,
    val rewardStars: Long,
    val hintBiome: WorldBiome
)

object WorldCatalog {
    val discoveries = listOf(
        FloraFaunaEntry(
            id = "pinky_monarch",
            name = "Pinky Monarch",
            category = DiscoveryCategory.BUTTERFLY,
            biome = WorldBiome.MEADOW,
            worldX = 18f,
            worldZ = -12f,
            funFact = "Leaves tiny pink sparkles whenever it dances over blossoms.",
            starReward = 15,
            accentColorHex = 0xFFFF4D94
        ),
        FloraFaunaEntry(
            id = "sapphire_morpho",
            name = "Sapphire Morpho",
            category = DiscoveryCategory.BUTTERFLY,
            biome = WorldBiome.RIVER,
            worldX = 6f,
            worldZ = 10f,
            funFact = "Loves gliding right above the shimmering crystal river waves.",
            starReward = 15,
            accentColorHex = 0xFF3A86FF
        ),
        FloraFaunaEntry(
            id = "sunbeam_swallowtail",
            name = "Sunbeam Swallowtail",
            category = DiscoveryCategory.BUTTERFLY,
            biome = WorldBiome.COTTAGE,
            worldX = -16f,
            worldZ = -10f,
            funFact = "Visits Barnaby Bear's porch every morning for sweet honey aroma.",
            starReward = 15,
            accentColorHex = 0xFFFFB703
        ),
        FloraFaunaEntry(
            id = "starlight_luna",
            name = "Starlight Luna",
            category = DiscoveryCategory.BUTTERFLY,
            biome = WorldBiome.STARLIGHT,
            worldX = 24f,
            worldZ = 32f,
            funFact = "Its pastel lavender wings glow softly on the rainbow ridge.",
            starReward = 20,
            accentColorHex = 0xFF8338EC
        ),
        FloraFaunaEntry(
            id = "pink_lotus",
            name = "Glowing Pink Lotus",
            category = DiscoveryCategory.FLORA,
            biome = WorldBiome.MEADOW,
            worldX = 28f,
            worldZ = -22f,
            funFact = "Opens its warm petals whenever a happy child giggles nearby.",
            starReward = 10,
            accentColorHex = 0xFFFF7EB6
        ),
        FloraFaunaEntry(
            id = "honeyberry_bush",
            name = "Honeyberry Bush",
            category = DiscoveryCategory.FLORA,
            biome = WorldBiome.COTTAGE,
            worldX = -30f,
            worldZ = -14f,
            funFact = "Grows sweet pink and blue berries for Bear's favorite jam.",
            starReward = 10,
            accentColorHex = 0xFFE63946
        ),
        FloraFaunaEntry(
            id = "rainbow_toadstool",
            name = "Rainbow Toadstool",
            category = DiscoveryCategory.FLORA,
            biome = WorldBiome.STARLIGHT,
            worldX = 32f,
            worldZ = 40f,
            funFact = "Bouncy mushroom caps that play soft marimba notes in the wind.",
            starReward = 15,
            accentColorHex = 0xFFA855F7
        ),
        FloraFaunaEntry(
            id = "oak_lantern_vine",
            name = "Lantern Blossom Vine",
            category = DiscoveryCategory.FLORA,
            biome = WorldBiome.TREEHOUSE,
            worldX = -22f,
            worldZ = 28f,
            funFact = "Wraps the Treehouse Plaza in cozy golden-pink fairy flowers.",
            starReward = 10,
            accentColorHex = 0xFF2EC486
        ),
        FloraFaunaEntry(
            id = "barnaby_bear",
            name = "Barnaby Bear",
            category = DiscoveryCategory.FAUNA,
            biome = WorldBiome.COTTAGE,
            worldX = -20f,
            worldZ = -20f,
            funFact = "Gentle giant bear who loves scarves, picnics, and giving piggyback rides.",
            starReward = 15,
            accentColorHex = 0xFF8D5524
        ),
        FloraFaunaEntry(
            id = "ladybug_pip",
            name = "Ladybug Pip",
            category = DiscoveryCategory.FAUNA,
            biome = WorldBiome.MEADOW,
            worldX = 20f,
            worldZ = -20f,
            funFact = "Counts dewdrops on fern leaves with five lucky spots.",
            starReward = 10,
            accentColorHex = 0xFFEF4444
        ),
        FloraFaunaEntry(
            id = "river_frog_boba",
            name = "River Frog Boba",
            category = DiscoveryCategory.FAUNA,
            biome = WorldBiome.RIVER,
            worldX = -4f,
            worldZ = 14f,
            funFact = "Cheering coach for the Rainbow Bridge lilypad hopping game.",
            starReward = 15,
            accentColorHex = 0xFF10B981
        ),
        FloraFaunaEntry(
            id = "blossom_fawn",
            name = "Blossom Fawn",
            category = DiscoveryCategory.FAUNA,
            biome = WorldBiome.TREEHOUSE,
            worldX = -32f,
            worldZ = 38f,
            funFact = "Shy woodland fawn wearing a garland of daisies near the treehouse.",
            starReward = 20,
            accentColorHex = 0xFFF59E0B
        )
    )

    val portals = listOf(
        MiniGamePortal(
            id = "butterfly_catch",
            title = "Butterfly Dance",
            subtitle = "Tap fluttering butterflies in Pinky Meadow",
            biome = WorldBiome.MEADOW,
            worldX = 22f,
            worldZ = -14f,
            colorHex = 0xFFFF4D94
        ),
        MiniGamePortal(
            id = "berry_picnic",
            title = "Bear's Berry Picnic",
            subtitle = "Sort honey jars & sweet berries with Barnaby",
            biome = WorldBiome.COTTAGE,
            worldX = -22f,
            worldZ = -12f,
            colorHex = 0xFFFFB703
        ),
        MiniGamePortal(
            id = "blossom_memory",
            title = "Pinky's Flower Chime",
            subtitle = "Tap glowing blossoms in musical order",
            biome = WorldBiome.STARLIGHT,
            worldX = 26f,
            worldZ = 34f,
            colorHex = 0xFF8338EC
        ),
        MiniGamePortal(
            id = "rainbow_hop",
            title = "Rainbow Bridge Hop",
            subtitle = "Bounce across sparkling river lilypads",
            biome = WorldBiome.RIVER,
            worldX = 2f,
            worldZ = 8f,
            colorHex = 0xFF2EC486
        )
    )

    val starterQuests = listOf(
        QuestSeed(
            questId = "quest_butterflies",
            title = "Befriend 3 Meadow Butterflies",
            targetCount = 3L,
            rewardStars = 25L,
            hintBiome = WorldBiome.MEADOW
        ),
        QuestSeed(
            questId = "quest_discoveries",
            title = "Discover 4 Flora or Fauna Wonders",
            targetCount = 4L,
            rewardStars = 30L,
            hintBiome = WorldBiome.STARLIGHT
        ),
        QuestSeed(
            questId = "quest_minigames",
            title = "Play 2 Fun Mini-Games",
            targetCount = 2L,
            rewardStars = 30L,
            hintBiome = WorldBiome.COTTAGE
        ),
        QuestSeed(
            questId = "quest_treehouse",
            title = "Explore Treehouse Picnic Plaza",
            targetCount = 1L,
            rewardStars = 20L,
            hintBiome = WorldBiome.TREEHOUSE
        ),
        QuestSeed(
            questId = "quest_friends",
            title = "Wave in a Friends Meadow Room",
            targetCount = 1L,
            rewardStars = 25L,
            hintBiome = WorldBiome.TREEHOUSE
        )
    )
}
