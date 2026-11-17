package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.CosmeticCategory
import com.example.data.model.CosmeticItem
import com.example.data.model.CurrencyType
import com.example.data.model.GraphicsConfig
import com.example.data.model.MatchRecord
import com.example.data.model.PlayerProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PlayerRepository(
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

    fun observePlayerProfile(userId: String): Flow<PlayerProfile?> = flow {
        val uid = requireUserId()
        require(uid == userId) { "Authenticated user mismatch." }
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            PlayerProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeMatches(userId: String): Flow<List<MatchRecord>> = flow {
        val uid = requireUserId()
        require(uid == userId) { "Authenticated user mismatch." }
        val colRef = db.collection("users").document(userId).collection("matches")
        emitAll(
            colRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(20)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MatchRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureProfileExists(displayName: String?): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(
            IllegalStateException("Not authenticated")
        )
        val docRef = db.collection("users").document(uid)
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeCallsign = (displayName?.trim()?.takeIf { it.length >= 2 } ?: "Striker")
                    .take(24)
                val payload = mapOf(
                    "userId" to uid,
                    "callsign" to safeCallsign,
                    "diamonds" to 600L,
                    "goldCoins" to 2500L,
                    "booyahs" to 0L,
                    "eliminations" to 0L,
                    "matchesPlayed" to 0L,
                    "equippedOutfitId" to "outfit_recruit",
                    "equippedWeaponSkinId" to "weapon_factory",
                    "equippedParachuteId" to "chute_standard",
                    "ownedCosmeticIds" to listOf(
                        "outfit_recruit",
                        "weapon_factory",
                        "chute_standard"
                    ),
                    "graphicsPreset" to "BALANCED",
                    "targetFps" to 45L,
                    "resolutionScale" to 0.85,
                    "shadowsEnabled" to true,
                    "particlesEnabled" to true,
                    "aimAssistEnabled" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun equipCosmetic(item: CosmeticItem): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = db.collection("users").document(uid)
        return try {
            requireUserId()
            val fieldName = when (item.category) {
                CosmeticCategory.OUTFIT -> "equippedOutfitId"
                CosmeticCategory.WEAPON_SKIN -> "equippedWeaponSkinId"
                CosmeticCategory.PARACHUTE -> "equippedParachuteId"
            }
            docRef.update(
                mapOf(
                    fieldName to item.id,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun purchaseAndEquipCosmetic(
        profile: PlayerProfile,
        item: CosmeticItem
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = db.collection("users").document(uid)
        return try {
            requireUserId()
            if (profile.ownedCosmeticIds.contains(item.id)) {
                return equipCosmetic(item)
            }
            val updatedOwned = (profile.ownedCosmeticIds + item.id).distinct().take(40)
            val newDiamonds: Long
            val newGold: Long
            when (item.currency) {
                CurrencyType.DIAMONDS -> {
                    require(profile.diamonds >= item.price) { "Insufficient Diamonds" }
                    newDiamonds = profile.diamonds - item.price
                    newGold = profile.goldCoins
                }
                CurrencyType.GOLD -> {
                    require(profile.goldCoins >= item.price) { "Insufficient Gold Coins" }
                    newDiamonds = profile.diamonds
                    newGold = profile.goldCoins - item.price
                }
            }
            val equipField = when (item.category) {
                CosmeticCategory.OUTFIT -> "equippedOutfitId"
                CosmeticCategory.WEAPON_SKIN -> "equippedWeaponSkinId"
                CosmeticCategory.PARACHUTE -> "equippedParachuteId"
            }
            docRef.update(
                mapOf(
                    "diamonds" to newDiamonds,
                    "goldCoins" to newGold,
                    "ownedCosmeticIds" to updatedOwned,
                    equipField to item.id,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateGraphicsAndControls(
        config: GraphicsConfig,
        callsign: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = db.collection("users").document(uid)
        return try {
            requireUserId()
            val cleanCallsign = callsign.trim().ifBlank { "Striker" }.take(24)
            require(cleanCallsign.length >= 2) { "Callsign must be at least 2 characters" }
            val validFps = when (config.targetFps) {
                30, 45, 60 -> config.targetFps.toLong()
                else -> 45L
            }
            docRef.update(
                mapOf(
                    "graphicsPreset" to config.preset.name,
                    "targetFps" to validFps,
                    "resolutionScale" to config.resolutionScale.toDouble().coerceIn(0.6, 1.0),
                    "shadowsEnabled" to config.shadowsEnabled,
                    "particlesEnabled" to config.particlesEnabled,
                    "aimAssistEnabled" to config.aimAssistEnabled,
                    "callsign" to cleanCallsign,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordMatchResult(
        profile: PlayerProfile,
        placement: Int,
        eliminations: Int,
        damageDealt: Int,
        survivalSeconds: Int,
        goldEarned: Int,
        diamondsEarned: Int,
        modeId: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val userDoc = db.collection("users").document(uid)
        val matchDoc = userDoc.collection("matches").document()
        return try {
            requireUserId()
            val safePlacement = placement.coerceIn(1, 30).toLong()
            val safeKills = eliminations.coerceIn(0, 30).toLong()
            val safeDamage = damageDealt.coerceIn(0, 50000).toLong()
            val safeSurvival = survivalSeconds.coerceIn(0, 3600).toLong()
            val safeGold = goldEarned.coerceIn(0, 5000).toLong()
            val safeDiamonds = diamondsEarned.coerceIn(0, 500).toLong()
            val safeMode = if (modeId == "CLASH_DROP") "CLASH_DROP" else "BERMUDA_BLITZ"
            val isWin = safePlacement == 1L

            val batch = db.batch()
            batch.set(
                matchDoc,
                mapOf(
                    "userId" to uid,
                    "placement" to safePlacement,
                    "eliminations" to safeKills,
                    "damageDealt" to safeDamage,
                    "survivalSeconds" to safeSurvival,
                    "goldEarned" to safeGold,
                    "diamondsEarned" to safeDiamonds,
                    "mode" to safeMode,
                    "booyah" to isWin,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                userDoc,
                mapOf(
                    "diamonds" to (profile.diamonds + safeDiamonds),
                    "goldCoins" to (profile.goldCoins + safeGold),
                    "booyahs" to (profile.booyahs + if (isWin) 1L else 0L),
                    "eliminations" to (profile.eliminations + safeKills),
                    "matchesPlayed" to (profile.matchesPlayed + 1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, matchDoc.path)
            Result.failure(e)
        }
    }
}
