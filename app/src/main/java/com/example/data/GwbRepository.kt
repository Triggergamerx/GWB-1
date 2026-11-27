package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class GwbRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    fun observeProfile(userId: String): Flow<ExplorerProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user profile listener." }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        ExplorerProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observeQuests(userId: String): Flow<List<QuestProgress>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user quest listener." }
        val path = "users/$uid/quests"
        emitAll(
            db.collection("users").document(uid).collection("quests")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        QuestProgress::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun ensureInitialProfileAndQuests(userId: String, preferredName: String): Result<Unit> {
        val profilePath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user initialization." }

            val userRef = db.collection("users").document(uid)
            val existingProfile = userRef.get().await()
            val cleanName = preferredName.trim().ifBlank { "Little Maya" }.take(30)

            if (!existingProfile.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "avatarId" to AvatarStyle.MAYA.id,
                    "stars" to 10L,
                    "butterfliesCaught" to 0L,
                    "discoveredIds" to emptyList<String>(),
                    "activeQuestId" to WorldCatalog.starterQuests.first().questId,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(payload).await()
            }

            val questsRef = userRef.collection("quests")
            val existingQuests = questsRef.get().await().documents.map { it.id }.toSet()
            for (seed in WorldCatalog.starterQuests) {
                if (seed.questId !in existingQuests) {
                    val questPayload = mapOf(
                        "questId" to seed.questId,
                        "title" to seed.title,
                        "targetCount" to seed.targetCount,
                        "currentCount" to 0L,
                        "completed" to false,
                        "rewardStars" to seed.rewardStars,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    questsRef.document(seed.questId).set(questPayload).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, profilePath)
            Result.failure(e)
        }
    }

    suspend fun updateAvatarAndName(
        userId: String,
        displayName: String,
        avatarId: String
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user update." }
            val cleanName = displayName.trim().ifBlank { "Little Maya" }.take(30)
            val safeAvatar = AvatarStyle.fromId(avatarId).id
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "displayName" to cleanName,
                        "avatarId" to safeAvatar,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordProgressAndDiscovery(
        userId: String,
        starsDelta: Long,
        butterfliesDelta: Long,
        newDiscoveryId: String? = null
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user progress write." }
            val userRef = db.collection("users").document(uid)
            val snapshot = userRef.get().await()
            val profile = snapshot.toObject(
                ExplorerProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return Result.failure(IllegalStateException("Profile not initialized yet"))

            val updatedDiscoveries = if (!newDiscoveryId.isNullOrBlank() &&
                newDiscoveryId !in profile.discoveredIds &&
                profile.discoveredIds.size < 30
            ) {
                profile.discoveredIds + newDiscoveryId
            } else {
                profile.discoveredIds
            }

            val nextStars = (profile.stars + starsDelta).coerceIn(0L, 1_000_000L)
            val nextButterflies = (profile.butterfliesCaught + butterfliesDelta).coerceIn(0L, 100_000L)

            userRef.update(
                mapOf(
                    "stars" to nextStars,
                    "butterfliesCaught" to nextButterflies,
                    "discoveredIds" to updatedDiscoveries,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun advanceQuest(userId: String, questId: String, incrementBy: Long = 1L): Result<Long> {
        val path = "users/$userId/quests/$questId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user quest update." }
            val questRef = db.collection("users").document(uid).collection("quests").document(questId)
            val snap = questRef.get().await()
            val quest = snap.toObject(
                QuestProgress::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return Result.success(0L)

            if (quest.completed) {
                return Result.success(0L)
            }

            val newCount = (quest.currentCount + incrementBy).coerceAtMost(quest.targetCount)
            val nowCompleted = newCount >= quest.targetCount

            questRef.update(
                mapOf(
                    "currentCount" to newCount,
                    "completed" to nowCompleted,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            var awardedStars = 0L
            if (nowCompleted) {
                awardedStars = quest.rewardStars
                recordProgressAndDiscovery(
                    userId = uid,
                    starsDelta = awardedStars,
                    butterfliesDelta = 0L
                )
            }
            Result.success(awardedStars)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeOpenMeadowRooms(): Flow<List<MeadowRoom>> = flow {
        val uid = requireUserId()
        val path = "meadowRooms"
        emitAll(
            db.collection("meadowRooms")
                .whereEqualTo("openForFriends", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MeadowRoom::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeRoom(roomCode: String): Flow<MeadowRoom?> = flow {
        val uid = requireUserId()
        val cleanCode = sanitizeRoomCode(roomCode)
        val path = "meadowRooms/$cleanCode"
        emitAll(
            db.collection("meadowRooms").document(cleanCode)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        MeadowRoom::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observeRoomPlayers(
        roomCode: String,
        openForFriends: Boolean,
        userId: String
    ): Flow<List<RoomPlayer>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val cleanCode = sanitizeRoomCode(roomCode)
        val path = "meadowRooms/$cleanCode/players"
        val baseCollection = db.collection("meadowRooms").document(cleanCode).collection("players")
        val query = if (openForFriends) {
            baseCollection.whereEqualTo("roomOpen", true)
        } else {
            baseCollection.whereArrayContains("memberUids", uid)
        }
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        RoomPlayer::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun createRoom(
        userId: String,
        roomCode: String,
        roomName: String,
        biomeTheme: String,
        openForFriends: Boolean
    ): Result<MeadowRoom> {
        val cleanCode = sanitizeRoomCode(roomCode)
        val path = "meadowRooms/$cleanCode"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user room create." }
            val cleanRoomName = roomName.trim().ifBlank { "Pinky Blossom Picnic" }.take(40)
            val safeBiome = WorldBiome.fromId(biomeTheme).id
            val roomRef = db.collection("meadowRooms").document(cleanCode)
            val payload = mapOf(
                "roomCode" to cleanCode,
                "roomName" to cleanRoomName,
                "hostUid" to uid,
                "biomeTheme" to safeBiome,
                "openForFriends" to openForFriends,
                "memberUids" to listOf(uid),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            roomRef.set(payload).await()
            val created = roomRef.get().await().toObject(
                MeadowRoom::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: MeadowRoom(
                id = cleanCode,
                roomCode = cleanCode,
                roomName = cleanRoomName,
                hostUid = uid,
                biomeTheme = safeBiome,
                openForFriends = openForFriends,
                memberUids = listOf(uid)
            )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinRoom(userId: String, roomCode: String): Result<MeadowRoom> {
        val cleanCode = sanitizeRoomCode(roomCode)
        val path = "meadowRooms/$cleanCode"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user join." }
            val roomRef = db.collection("meadowRooms").document(cleanCode)
            val snap = roomRef.get().await()
            val room = snap.toObject(
                MeadowRoom::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return Result.failure(IllegalStateException("Room $cleanCode was not found"))

            if (uid !in room.memberUids) {
                if (room.memberUids.size >= 8) {
                    return Result.failure(IllegalStateException("This cozy meadow already has 8 friends!"))
                }
                val nextMembers = room.memberUids + uid
                roomRef.update(
                    mapOf(
                        "memberUids" to nextMembers,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(room.copy(memberUids = nextMembers))
            } else {
                Result.success(room)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun leaveRoom(userId: String, roomCode: String): Result<Unit> {
        val cleanCode = sanitizeRoomCode(roomCode)
        val path = "meadowRooms/$cleanCode"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user leave." }
            val playerRef = db.collection("meadowRooms").document(cleanCode)
                .collection("players").document(uid)
            try {
                playerRef.delete().await()
            } catch (_: Exception) {
                // Ignore if player doc was already cleaned up
            }

            val roomRef = db.collection("meadowRooms").document(cleanCode)
            val snap = roomRef.get().await()
            val room = snap.toObject(
                MeadowRoom::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            if (room != null && uid in room.memberUids) {
                val remaining = room.memberUids.filter { it != uid }
                if (remaining.isNotEmpty()) {
                    roomRef.update(
                        mapOf(
                            "memberUids" to remaining,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun syncPlayerPose(
        roomCode: String,
        userId: String,
        displayName: String,
        avatarId: String,
        posX: Float,
        posZ: Float,
        yaw: Float,
        emote: String,
        roomOpen: Boolean,
        memberUids: List<String>
    ): Result<Unit> {
        val cleanCode = sanitizeRoomCode(roomCode)
        val path = "meadowRooms/$cleanCode/players/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user pose sync." }
            val playerRef = db.collection("meadowRooms").document(cleanCode)
                .collection("players").document(uid)
            val safeName = displayName.trim().ifBlank { "Friend" }.take(30)
            val safeAvatar = AvatarStyle.fromId(avatarId).id
            val safeX = posX.toDouble().coerceIn(-195.0, 195.0)
            val safeZ = posZ.toDouble().coerceIn(-195.0, 195.0)
            val safeYaw = (((yaw % 360f) + 360f) % 360f).toDouble()
            val safeEmote = if (emote in listOf("", "wave", "butterfly", "heart", "picnic", "dance")) emote else ""
            val safeMembers = (memberUids + uid).distinct().take(8)

            val existing = playerRef.get().await()
            if (!existing.exists()) {
                playerRef.set(
                    mapOf(
                        "roomId" to cleanCode,
                        "playerUid" to uid,
                        "displayName" to safeName,
                        "avatarId" to safeAvatar,
                        "posX" to safeX,
                        "posZ" to safeZ,
                        "yaw" to safeYaw,
                        "emote" to safeEmote,
                        "roomOpen" to roomOpen,
                        "memberUids" to safeMembers,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                playerRef.update(
                    mapOf(
                        "displayName" to safeName,
                        "avatarId" to safeAvatar,
                        "posX" to safeX,
                        "posZ" to safeZ,
                        "yaw" to safeYaw,
                        "emote" to safeEmote,
                        "roomOpen" to roomOpen,
                        "memberUids" to safeMembers,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private fun sanitizeRoomCode(raw: String): String {
        val cleaned = raw.trim().uppercase().replace(Regex("[^A-Z0-9_\\-]"), "")
        return cleaned.ifBlank { "PINKY1" }.take(24)
    }

    private fun handleFlowError(error: Throwable, operationType: OperationType, path: String, uid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
