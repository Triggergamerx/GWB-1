package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.example.data.AvatarStyle
import com.example.data.ExplorerProfile
import com.example.data.MeadowRoom
import com.example.data.RoomPlayer
import com.example.data.SafeEmote
import com.example.data.UiState
import com.example.data.WorldBiome
import com.example.ui.theme.BerryPlum
import com.example.ui.theme.BlossomPink
import com.example.ui.theme.MeadowMint

@Composable
fun MultiplayerFriendsScreen(
    activeRoom: MeadowRoom?,
    roomPlayers: List<RoomPlayer>,
    openRoomsState: UiState<List<MeadowRoom>>,
    busy: Boolean,
    errorText: String?,
    onCreateRoom: (roomName: String, biome: WorldBiome, openForFriends: Boolean) -> Unit,
    onJoinRoom: (roomCode: String) -> Unit,
    onLeaveRoom: () -> Unit,
    onSendEmote: (SafeEmote) -> Unit,
    onClearError: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var roomNameInput by remember { mutableStateOf("Pinky Blossom Picnic") }
    var joinCodeInput by remember { mutableStateOf("") }
    var selectedBiome by remember { mutableStateOf(WorldBiome.TREEHOUSE) }
    var openForFriends by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF3E8FF), Color(0xFFFFF8FB))
                )
            )
            .safeDrawingPadding()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(shape = CircleShape, color = Color.White, shadowElevation = 4.dp) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("friends_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to World")
                        }
                    }
                    Column {
                        Text(
                            text = "Safe Friends Meadow",
                            style = MaterialTheme.typography.headlineMedium,
                            color = BerryPlum
                        )
                        Text(
                            text = "Explore together with preset friendly emotes — zero unsafe chat!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(155.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_multiplayer_meadow_1795682630451),
                        contentDescription = "Multiplayer treehouse picnic meadow",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (errorText != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClearError() }
                    ) {
                        Text(
                            text = errorText,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            if (activeRoom != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE7FFF4)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
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
                                Column {
                                    Text(
                                        text = activeRoom.roomName,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MeadowMint
                                    )
                                    Text(
                                        text = "Share Room Code: ${activeRoom.roomCode}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                OutlinedButton(
                                    onClick = onLeaveRoom,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.testTag("leave_room_button")
                                ) {
                                    Text("Leave")
                                }
                            }

                            Text(
                                text = "Friends Exploring (${roomPlayers.size}/8):",
                                style = MaterialTheme.typography.labelLarge
                            )
                            if (roomPlayers.isEmpty()) {
                                Text("Walking into the meadow...", style = MaterialTheme.typography.bodyMedium)
                            } else {
                                roomPlayers.forEach { player ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val style = AvatarStyle.fromId(player.avatarId)
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(style.primaryHex),
                                            modifier = Modifier.size(14.dp)
                                        ) {}
                                        Text(
                                            text = "${player.displayName} (${style.title})",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (player.emote.isNotEmpty()) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color.White
                                            ) {
                                                Text(
                                                    text = player.emote,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = BerryPlum,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Text("Send a Safe Emote:", style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SafeEmote.entries.forEach { emote ->
                                    Button(
                                        onClick = { onSendEmote(emote) },
                                        shape = RoundedCornerShape(18.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BerryPlum)
                                    ) {
                                        Text(emote.label)
                                    }
                                }
                            }

                            Button(
                                onClick = onBack,
                                shape = RoundedCornerShape(22.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BlossomPink),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Return to 3D World Together!")
                            }
                        }
                    }
                }
            }

            // Create Room Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Create a Friend Meadow", style = MaterialTheme.typography.titleLarge, color = BlossomPink)
                        OutlinedTextField(
                            value = roomNameInput,
                            onValueChange = { roomNameInput = it.take(36) },
                            label = { Text("Meadow Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WorldBiome.entries.forEach { b ->
                                FilterChip(
                                    selected = selectedBiome == b,
                                    onClick = { selectedBiome = b },
                                    label = { Text(b.title.substringBefore(" ")) }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (openForFriends) Icons.Rounded.Public else Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = if (openForFriends) MeadowMint else BerryPlum
                                )
                                Text(
                                    text = if (openForFriends) "Open Cozy Meadow" else "Private Code Only",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Switch(checked = openForFriends, onCheckedChange = { openForFriends = it })
                        }
                        Button(
                            onClick = { onCreateRoom(roomNameInput, selectedBiome, openForFriends) },
                            enabled = !busy,
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BlossomPink),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("create_room_button")
                        ) {
                            if (busy) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Icon(Icons.Rounded.AddCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open Meadow Room")
                            }
                        }
                    }
                }
            }

            // Join by Room Code Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Join a Friend's Code", style = MaterialTheme.typography.titleLarge, color = BerryPlum)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = joinCodeInput,
                                onValueChange = { joinCodeInput = it.uppercase().take(12) },
                                label = { Text("6-Letter Code") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("join_code_input")
                            )
                            Button(
                                onClick = { if (joinCodeInput.isNotBlank()) onJoinRoom(joinCodeInput) },
                                enabled = !busy && joinCodeInput.isNotBlank(),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BerryPlum),
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("join_code_button")
                            ) {
                                Text("Join")
                            }
                        }
                    }
                }
            }

            // Open Cozy Meadows List
            item {
                Text(
                    text = "Open Cozy Meadows",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            when (openRoomsState) {
                is UiState.Loading -> {
                    item { Text("Looking for cozy friend meadows...") }
                }
                is UiState.Error -> {
                    item { Text(openRoomsState.message, color = MaterialTheme.colorScheme.error) }
                }
                is UiState.Success -> {
                    val rooms = openRoomsState.data
                    if (rooms.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No open meadows right now — tap Open Meadow Room above to invite friends!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(rooms, key = { it.roomCode }) { room ->
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Groups, contentDescription = null, tint = MeadowMint)
                                        Column {
                                            Text(room.roomName, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                text = "Code: ${room.roomCode} • ${room.memberUids.size}/8 friends",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { onJoinRoom(room.roomCode) },
                                        enabled = !busy,
                                        shape = RoundedCornerShape(18.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MeadowMint)
                                    ) {
                                        Text("Play")
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
fun WardrobeScreen(
    profile: ExplorerProfile?,
    onSaveWardrobe: (displayName: String, avatarId: String) -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var nickname by remember(profile?.displayName) {
        mutableStateOf(profile?.displayName ?: "Little Maya")
    }
    var selectedAvatar by remember(profile?.avatarId) {
        mutableStateOf(profile?.avatarId ?: AvatarStyle.MAYA.id)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFE5F1), Color(0xFFFFF8FB))
                )
            )
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                                .testTag("wardrobe_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to World")
                        }
                    }
                    Column {
                        Text(
                            text = "Dress Up & Profile",
                            style = MaterialTheme.typography.headlineMedium,
                            color = BlossomPink
                        )
                        Text(
                            text = "Pick your 3D woodland style!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedButton(
                    onClick = onSignOut,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Sign Out", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sign Out")
                }
            }

            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it.take(28) },
                label = { Text("Explorer Nickname") },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("nickname_input")
            )

            AvatarStyle.entries.forEach { style ->
                val isSelected = selectedAvatar == style.id
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFFFFEFF5) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = if (isSelected) BlossomPink else Color.Transparent,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { selectedAvatar = style.id }
                        .testTag("avatar_choice_${style.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(style.primaryHex),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Face,
                                    contentDescription = style.title,
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(style.title, style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = style.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Selected",
                                tint = BlossomPink,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { onSaveWardrobe(nickname, selectedAvatar) },
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MeadowMint),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("save_wardrobe_button")
            ) {
                Text("Save Look & Play!", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
