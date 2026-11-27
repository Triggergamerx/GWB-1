package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AvatarStyle
import com.example.data.ExplorerProfile
import com.example.data.FloraFaunaEntry
import com.example.data.GwbRepository
import com.example.data.MeadowRoom
import com.example.data.MiniGamePortal
import com.example.data.QuestProgress
import com.example.data.RoomPlayer
import com.example.data.SafeEmote
import com.example.data.UiState
import com.example.data.WorldBiome
import com.example.engine3d.GwbWorldSimulation
import com.example.engine3d.WorldHudSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class GwbScreenRoute {
    WORLD,
    MAP,
    QUESTS,
    ALMANAC,
    MINI_GAMES,
    FRIENDS,
    WARDROBE
}

@OptIn(ExperimentalCoroutinesApi::class)
class GwbViewModel(
    private val repository: GwbRepository,
    val currentUserId: String,
    private val defaultDisplayName: String
) : ViewModel() {

    val simulation = GwbWorldSimulation()

    private val _currentRoute = MutableStateFlow(GwbScreenRoute.WORLD)
    val currentRoute: StateFlow<GwbScreenRoute> = _currentRoute.asStateFlow()

    private val _activeMiniGameId = MutableStateFlow<String?>(null)
    val activeMiniGameId: StateFlow<String?> = _activeMiniGameId.asStateFlow()

    private val _hudSnapshot = MutableStateFlow(WorldHudSnapshot())
    val hudSnapshot: StateFlow<WorldHudSnapshot> = _hudSnapshot.asStateFlow()

    private val _celebrationBanner = MutableStateFlow<String?>(null)
    val celebrationBanner: StateFlow<String?> = _celebrationBanner.asStateFlow()

    private val _activeRoomCode = MutableStateFlow<String?>(null)
    val activeRoomCode: StateFlow<String?> = _activeRoomCode.asStateFlow()

    private val _activeEmote = MutableStateFlow("")
    val activeEmote: StateFlow<String> = _activeEmote.asStateFlow()

    private val _multiplayerBusy = MutableStateFlow(false)
    val multiplayerBusy: StateFlow<Boolean> = _multiplayerBusy.asStateFlow()

    private val _multiplayerError = MutableStateFlow<String?>(null)
    val multiplayerError: StateFlow<String?> = _multiplayerError.asStateFlow()

    val profileState: StateFlow<UiState<ExplorerProfile?>> = repository.observeProfile(currentUserId)
        .map<ExplorerProfile?, UiState<ExplorerProfile?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing profile", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load explorer profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val questsState: StateFlow<UiState<List<QuestProgress>>> = repository.observeQuests(currentUserId)
        .map<List<QuestProgress>, UiState<List<QuestProgress>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing quests", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load quests"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val openRoomsState: StateFlow<UiState<List<MeadowRoom>>> = repository.observeOpenMeadowRooms()
        .map<List<MeadowRoom>, UiState<List<MeadowRoom>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing open rooms", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load open meadows"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val activeRoomState: StateFlow<MeadowRoom?> = _activeRoomCode
        .flatMapLatest { code ->
            if (code.isNullOrBlank()) {
                flowOf(null)
            } else {
                repository.observeRoom(code).catch { error ->
                    val firestoreError = unwrapFirestoreError(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing active room", firestoreError ?: error)
                    emit(null)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    val roomPlayersState: StateFlow<List<RoomPlayer>> = activeRoomState
        .flatMapLatest { room ->
            if (room == null) {
                flowOf(emptyList())
            } else {
                repository.observeRoomPlayers(
                    roomCode = room.roomCode,
                    openForFriends = room.openForFriends,
                    userId = currentUserId
                ).catch { error ->
                    val firestoreError = unwrapFirestoreError(error)
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing room players", firestoreError ?: error)
                    emit(emptyList())
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            repository.ensureInitialProfileAndQuests(currentUserId, defaultDisplayName)
        }

        // Sync local avatar style into 3D simulation
        viewModelScope.launch {
            profileState.collect { state ->
                if (state is UiState.Success && state.data != null) {
                    simulation.localAvatar = AvatarStyle.fromId(state.data.avatarId)
                }
            }
        }

        // Sync remote multiplayer friends into 3D simulation
        viewModelScope.launch {
            roomPlayersState.collect { players ->
                simulation.updateRemoteFriends(players, currentUserId)
            }
        }

        // Wire 3D simulation events safely to main ViewModel scope
        simulation.onButterflyCaught = {
            viewModelScope.launch {
                handleButterflyCaught()
            }
        }
        simulation.onBiomeVisited = { biome ->
            viewModelScope.launch {
                if (biome == WorldBiome.TREEHOUSE) {
                    val questBonus = repository.advanceQuest(currentUserId, "quest_treehouse", 1L)
                        .getOrDefault(0L)
                    if (questBonus > 0L) {
                        showCelebration("Treehouse Plaza Explored! +$questBonus Stars!")
                    } else {
                        showCelebration("Entered ${biome.title}!")
                    }
                } else {
                    showCelebration("Entered ${biome.title}!")
                }
            }
        }
        simulation.onPortalEntered = { portal ->
            viewModelScope.launch {
                if (_currentRoute.value == GwbScreenRoute.WORLD && _activeMiniGameId.value == null) {
                    openMiniGame(portal.id)
                }
            }
        }

        // Smooth HUD polling + throttled multiplayer position sync
        viewModelScope.launch {
            var tickCount = 0
            while (true) {
                delay(150L)
                val snap = simulation.snapshotHud()
                _hudSnapshot.value = snap
                tickCount++
                if (tickCount % 3 == 0) {
                    syncActiveRoomPoseIfJoined(snap)
                }
            }
        }
    }

    fun navigateTo(route: GwbScreenRoute) {
        _currentRoute.value = route
    }

    fun returnToWorld() {
        _activeMiniGameId.value = null
        _currentRoute.value = GwbScreenRoute.WORLD
    }

    fun openMiniGame(gameId: String) {
        _activeMiniGameId.value = gameId
        _currentRoute.value = GwbScreenRoute.MINI_GAMES
    }

    fun closeMiniGame() {
        _activeMiniGameId.value = null
    }

    fun fastTravelToBiome(biome: WorldBiome) {
        simulation.teleportTo(biome.centerX, biome.centerZ)
        simulation.snapCameraBehindPlayer()
        _currentRoute.value = GwbScreenRoute.WORLD
        showCelebration("Whoosh! Arrived at ${biome.title}!")
    }

    fun fastTravelToPortal(portal: MiniGamePortal) {
        simulation.teleportTo(portal.worldX + 2.8f, portal.worldZ + 2.8f)
        simulation.snapCameraBehindPlayer()
        _currentRoute.value = GwbScreenRoute.WORLD
    }

    fun triggerJump() = simulation.triggerJump()

    fun triggerBearBoost() {
        simulation.triggerBearBoost()
        showCelebration("Barnaby Bear Piggyback Boost!")
    }

    fun triggerSparkleNet() {
        val caught = simulation.triggerSparkleNet()
        if (caught.isEmpty()) {
            // Check if a flora/fauna entry is nearby to discover with the sparkle wave
            _hudSnapshot.value.nearbyDiscovery?.let { entry ->
                discoverFloraFauna(entry)
            }
        }
    }

    fun discoverFloraFauna(entry: FloraFaunaEntry) {
        viewModelScope.launch {
            val profile = (profileState.value as? UiState.Success)?.data
            val alreadyFound = profile?.discoveredIds?.contains(entry.id) == true
            if (alreadyFound) {
                showCelebration("${entry.name}: ${entry.funFact}")
                return@launch
            }
            val result = repository.recordProgressAndDiscovery(
                userId = currentUserId,
                starsDelta = entry.starReward.toLong(),
                butterfliesDelta = if (entry.category.name == "BUTTERFLY") 1L else 0L,
                newDiscoveryId = entry.id
            )
            if (result.isSuccess) {
                val questBonus = repository.advanceQuest(currentUserId, "quest_discoveries", 1L)
                    .getOrDefault(0L)
                val bonusText = if (questBonus > 0L) " +$questBonus Quest Bonus!" else ""
                showCelebration("Discovered ${entry.name}! +${entry.starReward} Stars!$bonusText")
            }
        }
    }

    fun completeMiniGame(gameTitle: String, starsEarned: Long, butterfliesEarned: Long) {
        viewModelScope.launch {
            repository.recordProgressAndDiscovery(
                userId = currentUserId,
                starsDelta = starsEarned,
                butterfliesDelta = butterfliesEarned
            )
            val questBonus = repository.advanceQuest(currentUserId, "quest_minigames", 1L)
                .getOrDefault(0L)
            if (butterfliesEarned > 0L) {
                repository.advanceQuest(currentUserId, "quest_butterflies", butterfliesEarned)
            }
            val extra = if (questBonus > 0L) " +$questBonus Quest Stars!" else ""
            showCelebration("$gameTitle Complete! +$starsEarned Stars!$extra")
        }
    }

    fun updateProfileWardrobe(displayName: String, avatarId: String) {
        viewModelScope.launch {
            val res = repository.updateAvatarAndName(currentUserId, displayName, avatarId)
            if (res.isSuccess) {
                showCelebration("Outfit Updated! Looking sparkly!")
                _currentRoute.value = GwbScreenRoute.WORLD
            }
        }
    }

    fun createFriendRoom(roomName: String, biome: WorldBiome, openForFriends: Boolean) {
        viewModelScope.launch {
            _multiplayerBusy.value = true
            _multiplayerError.value = null
            val code = generateFriendRoomCode()
            val res = repository.createRoom(
                userId = currentUserId,
                roomCode = code,
                roomName = roomName,
                biomeTheme = biome.id,
                openForFriends = openForFriends
            )
            _multiplayerBusy.value = false
            res.onSuccess { room ->
                _activeRoomCode.value = room.roomCode
                simulation.teleportTo(biome.centerX, biome.centerZ)
                showCelebration("Meadow Room ${room.roomCode} Ready!")
                _currentRoute.value = GwbScreenRoute.WORLD
            }.onFailure { err ->
                _multiplayerError.value = err.localizedMessage ?: "Could not create meadow room"
            }
        }
    }

    fun joinFriendRoom(roomCode: String) {
        viewModelScope.launch {
            _multiplayerBusy.value = true
            _multiplayerError.value = null
            val res = repository.joinRoom(currentUserId, roomCode)
            _multiplayerBusy.value = false
            res.onSuccess { room ->
                _activeRoomCode.value = room.roomCode
                val biome = WorldBiome.fromId(room.biomeTheme)
                simulation.teleportTo(biome.centerX + 2f, biome.centerZ + 2f)
                showCelebration("Joined ${room.roomName}!")
                _currentRoute.value = GwbScreenRoute.WORLD
            }.onFailure { err ->
                _multiplayerError.value = err.localizedMessage ?: "Could not find room $roomCode"
            }
        }
    }

    fun leaveFriendRoom() {
        val code = _activeRoomCode.value ?: return
        viewModelScope.launch {
            repository.leaveRoom(currentUserId, code)
            _activeRoomCode.value = null
            _activeEmote.value = ""
            showCelebration("Left multiplayer meadow")
        }
    }

    fun sendSafeEmote(emote: SafeEmote) {
        _activeEmote.value = emote.id
        showCelebration("Sent \"${emote.label}\" to friends!")
        viewModelScope.launch {
            syncActiveRoomPoseIfJoined(_hudSnapshot.value)
            repository.advanceQuest(currentUserId, "quest_friends", 1L)
            delay(4000L)
            if (_activeEmote.value == emote.id) {
                _activeEmote.value = ""
            }
        }
    }

    fun clearMultiplayerError() {
        _multiplayerError.value = null
    }

    fun dismissCelebration() {
        _celebrationBanner.value = null
    }

    private suspend fun handleButterflyCaught() {
        repository.recordProgressAndDiscovery(
            userId = currentUserId,
            starsDelta = 5L,
            butterfliesDelta = 1L
        )
        val questBonus = repository.advanceQuest(currentUserId, "quest_butterflies", 1L)
            .getOrDefault(0L)
        if (questBonus > 0L) {
            showCelebration("Quest Complete! +$questBonus Bonus Stars!")
        } else {
            showCelebration("Befriended a Butterfly! +5 Stars!")
        }
    }

    private suspend fun syncActiveRoomPoseIfJoined(snap: WorldHudSnapshot) {
        val room = activeRoomState.value ?: return
        val profile = (profileState.value as? UiState.Success)?.data
        repository.syncPlayerPose(
            roomCode = room.roomCode,
            userId = currentUserId,
            displayName = profile?.displayName ?: defaultDisplayName,
            avatarId = profile?.avatarId ?: AvatarStyle.MAYA.id,
            posX = snap.playerX,
            posZ = snap.playerZ,
            yaw = snap.playerYaw,
            emote = _activeEmote.value,
            roomOpen = room.openForFriends,
            memberUids = room.memberUids
        )
    }

    private fun showCelebration(message: String) {
        _celebrationBanner.value = message
        viewModelScope.launch {
            delay(3200L)
            if (_celebrationBanner.value == message) {
                _celebrationBanner.value = null
            }
        }
    }

    private fun generateFriendRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    private companion object {
        const val TAG = "GwbViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
