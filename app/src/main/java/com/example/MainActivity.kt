package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.GwbRepository
import com.example.data.UiState
import com.example.ui.AuthScreen
import com.example.ui.FloraAlmanacScreen
import com.example.ui.MiniGamesHubAndPlayScreen
import com.example.ui.MultiplayerFriendsScreen
import com.example.ui.QuestsScreen
import com.example.ui.WardrobeScreen
import com.example.ui.WorldHudScreen
import com.example.ui.WorldMapScreen
import com.example.ui.signOutFromMeadow
import com.example.ui.theme.GwbTheme
import com.example.viewmodel.GwbScreenRoute
import com.example.viewmodel.GwbViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GwbTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val uid = user.uid
        val defaultName = user.displayName?.takeIf { it.isNotBlank() } ?: "Little Maya"
        key(uid) {
            GwbWorldApp(
                currentUserId = uid,
                defaultDisplayName = defaultName
            )
        }
    }
}

@Composable
private fun GwbWorldApp(
    currentUserId: String,
    defaultDisplayName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: GwbViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                GwbViewModel(
                    repository = GwbRepository(db),
                    currentUserId = currentUserId,
                    defaultDisplayName = defaultDisplayName
                )
            }
        }
    )

    val route by viewModel.currentRoute.collectAsStateWithLifecycle()
    val activeMiniGameId by viewModel.activeMiniGameId.collectAsStateWithLifecycle()
    val hudSnapshot by viewModel.hudSnapshot.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val questsState by viewModel.questsState.collectAsStateWithLifecycle()
    val openRoomsState by viewModel.openRoomsState.collectAsStateWithLifecycle()
    val activeRoom by viewModel.activeRoomState.collectAsStateWithLifecycle()
    val roomPlayers by viewModel.roomPlayersState.collectAsStateWithLifecycle()
    val celebrationBanner by viewModel.celebrationBanner.collectAsStateWithLifecycle()
    val multiplayerBusy by viewModel.multiplayerBusy.collectAsStateWithLifecycle()
    val multiplayerError by viewModel.multiplayerError.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val quests = (questsState as? UiState.Success)?.data ?: emptyList()

    when (route) {
        GwbScreenRoute.WORLD -> WorldHudScreen(
            simulation = viewModel.simulation,
            hudSnapshot = hudSnapshot,
            profile = profile,
            quests = quests,
            activeRoom = activeRoom,
            friendCount = roomPlayers.size,
            celebrationBanner = celebrationBanner,
            onNavigate = viewModel::navigateTo,
            onJump = viewModel::triggerJump,
            onSparkleNet = viewModel::triggerSparkleNet,
            onBearBoost = viewModel::triggerBearBoost,
            onDiscoverNearby = viewModel::discoverFloraFauna,
            onEnterPortal = viewModel::openMiniGame,
            onSendEmote = viewModel::sendSafeEmote,
            onDismissCelebration = viewModel::dismissCelebration
        )
        GwbScreenRoute.MAP -> WorldMapScreen(
            hudSnapshot = hudSnapshot,
            onFastTravelBiome = viewModel::fastTravelToBiome,
            onFastTravelPortal = viewModel::fastTravelToPortal,
            onBack = viewModel::returnToWorld
        )
        GwbScreenRoute.QUESTS -> QuestsScreen(
            questsState = questsState,
            onGoToBiome = viewModel::fastTravelToBiome,
            onBack = viewModel::returnToWorld
        )
        GwbScreenRoute.ALMANAC -> FloraAlmanacScreen(
            profile = profile,
            onVisitEntryBiome = viewModel::fastTravelToBiome,
            onBack = viewModel::returnToWorld
        )
        GwbScreenRoute.MINI_GAMES -> MiniGamesHubAndPlayScreen(
            activeMiniGameId = activeMiniGameId,
            onSelectMiniGame = viewModel::openMiniGame,
            onCloseMiniGame = viewModel::closeMiniGame,
            onCompleteMiniGame = viewModel::completeMiniGame,
            onBackToWorld = viewModel::returnToWorld
        )
        GwbScreenRoute.FRIENDS -> MultiplayerFriendsScreen(
            activeRoom = activeRoom,
            roomPlayers = roomPlayers,
            openRoomsState = openRoomsState,
            busy = multiplayerBusy,
            errorText = multiplayerError,
            onCreateRoom = viewModel::createFriendRoom,
            onJoinRoom = viewModel::joinFriendRoom,
            onLeaveRoom = viewModel::leaveFriendRoom,
            onSendEmote = viewModel::sendSafeEmote,
            onClearError = viewModel::clearMultiplayerError,
            onBack = viewModel::returnToWorld
        )
        GwbScreenRoute.WARDROBE -> WardrobeScreen(
            profile = profile,
            onSaveWardrobe = viewModel::updateProfileWardrobe,
            onSignOut = {
                viewModel.leaveFriendRoom()
                signOutFromMeadow(credentialManager = credentialManager, scope = scope)
            },
            onBack = viewModel::returnToWorld
        )
    }
}
