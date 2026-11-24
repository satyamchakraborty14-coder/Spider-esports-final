package com.example.data

import com.example.data.SpiderWalletUtil.toWriteMap
import com.example.model.AdminUserRecord
import com.example.model.Announcement
import com.example.model.AuditLog
import com.example.model.CustomTournamentType
import com.example.model.Dispute
import com.example.model.MapBanner
import com.example.model.MatchResult
import com.example.model.PaymentOrder
import com.example.model.PayoutRequest
import com.example.model.PromoBanner
import com.example.model.ReferralRecord
import com.example.model.Registration
import com.example.model.SupportTicket
import com.example.model.Tournament
import com.example.model.TournamentCategory
import com.example.model.UserProfile
import com.example.model.Wallet
import com.example.model.WalletConfig
import com.example.model.WalletTransaction
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale

class SpiderAdminRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireAdminIdentity(): Pair<String, String> {
        val user = auth.currentUser ?: throw IllegalStateException("Admin must be signed in.")
        if (!AuthHelper.isBootstrapAdmin(user)) {
            throw SecurityException("Admin access is restricted to ${AuthHelper.BOOTSTRAP_ADMIN_EMAIL}.")
        }
        return user.uid to (user.email ?: AuthHelper.BOOTSTRAP_ADMIN_EMAIL)
    }

    suspend fun checkIsAdmin(uid: String): Boolean {
        val current = auth.currentUser ?: return false
        return current.uid == uid && AuthHelper.isBootstrapAdmin(current)
    }

    suspend fun ensureLinkedAdminRecord(user: FirebaseUser): Result<Unit> {
        if (!AuthHelper.isBootstrapAdmin(user)) return Result.success(Unit)
        val path = "adminUsers/${user.uid}"
        return try {
            val adminRef = db.collection("adminUsers").document(user.uid)
            val snap = adminRef.get().await()
            if (!snap.exists()) {
                val now = FieldValue.serverTimestamp()
                val batch = db.batch()
                batch.set(
                    adminRef,
                    mapOf(
                        "userId" to user.uid,
                        "email" to AuthHelper.BOOTSTRAP_ADMIN_EMAIL,
                        "role" to "OWNER_ADMIN",
                        "grantedBy" to AuthHelper.BOOTSTRAP_ADMIN_EMAIL,
                        "createdAt" to now
                    )
                )
                appendAudit(
                    batch = batch,
                    uid = user.uid,
                    email = AuthHelper.BOOTSTRAP_ADMIN_EMAIL,
                    action = "LINK_OWNER_ADMIN",
                    collection = "adminUsers",
                    targetId = user.uid,
                    summary = "Linked verified owner Gmail ${AuthHelper.BOOTSTRAP_ADMIN_EMAIL} as sole Admin"
                )
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeAllUsers(adminUid: String): Flow<List<UserProfile>> =
        db.collection("users").snapshots()
            .map { it.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .retryTransientFirestoreErrors("SpiderAdminRepo") { auth.currentUser?.uid == adminUid }
            .catch { logAndRethrow(it, OperationType.LIST, "users", adminUid) }

    fun observeAllCategories(adminUid: String): Flow<List<TournamentCategory>> =
        db.collection("tournamentCategories").snapshots()
            .map { snap ->
                snap.toObjects(TournamentCategory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.displayOrder }
            }
            .retryTransientFirestoreErrors("SpiderAdminRepo") { auth.currentUser?.uid == adminUid }
            .catch { logAndRethrow(it, OperationType.LIST, "tournamentCategories", adminUid) }

    fun observeAllCustomTypes(adminUid: String): Flow<List<CustomTournamentType>> =
        db.collection("customTournamentTypes").snapshots()
            .map { snap ->
                snap.toObjects(CustomTournamentType::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.displayOrder }
            }
            .retryTransientFirestoreErrors("SpiderAdminRepo") { auth.currentUser?.uid == adminUid }
            .catch { logAndRethrow(it, OperationType.LIST, "customTournamentTypes", adminUid) }

    fun observeAllTournaments(adminUid: String): Flow<List<Tournament>> =
        db.collection("tournaments").snapshots()
            .map { snap ->
                snap.toObjects(Tournament::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .retryTransientFirestoreErrors("SpiderAdminRepo") { auth.currentUser?.uid == adminUid }
            .catch { logAndRethrow(it, OperationType.LIST, "tournaments", adminUid) }

    fun observeAllBanners(adminUid: String): Flow<List<PromoBanner>> =
        db.collection("banners").snapshots()
            .map { snap ->
                snap.toObjects(PromoBanner::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.displayOrder }
            }
            .retryTransientFirestoreErrors("SpiderAdminRepo") { auth.currentUser?.uid == adminUid }
            .catch { logAndRethrow(it, OperationType.LIST, "banners", adminUid) }

    fun observeAllMapBanners(adminUid: String): Flow<List<MapBanner>> =
        db.collection("mapBanners").snapshots()
            .map { snap ->
                snap.toObjects(MapBanner::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareBy<MapBanner> { it.displayOrder }.thenBy { it.mapName })
            }
            .retryTransientFirestoreErrors("SpiderAdminRepo") { auth.currentUser?.uid == adminUid }
            .catch { logAndRethrow(it, OperationType.LIST, "mapBanners", adminUid) }

    fun observeAllAnnouncements(adminUid: String): Flow<List<Announcement>> =
        db.collection("announcements").snapshots()
            .map { snap ->
                snap.toObjects(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.priority }
            }
            .retryTransientFirestoreErrors("SpiderAdminRepo") { auth.currentUser?.uid == adminUid }
            .catch { logAndRethrow(it, OperationType.LIST, "announcements", adminUid) }

    fun observeAllRegistrations(adminUid: String): Flow<List<Registration>> =
        db.collection("registrations").snapshots()
            .map { snap ->
                snap.toObjects(Registration::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .retryTransientFirestoreErrors("SpiderAdminRepo") { auth.currentUser?.uid == adminUid }
            .catch { logAndRethrow(it, OperationType.LIST, "registrations", adminUid) }

    fun observeAllWallets(adminUid: String): Flow<List<Wallet>> =
        db.collection("wallets").snapshots()
            .map { snap ->
                snap.documents.map { doc ->
                    SpiderWalletUtil.parseWalletSnapshot(doc, doc.id)
                }
            }
            .retryTransientFirestoreErrors("SpiderAdminRepo") { auth.currentUser?.uid == adminUid }
            .catch { logAndRethrow(it, OperationType.LIST, "wallets", adminUid) }

    suspend fun fetchAllWallets(): Result<List<Wallet>> = try {
        requireAdminIdentity()
        val snap = try {
            db.collection("wallets").get(Source.SERVER).await()
        } catch (_: Exception) {
            db.collection("wallets").get().await()
        }
        Result.success(snap.documents.map { doc -> SpiderWalletUtil.parseWalletSnapshot(doc, doc.id) })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.LIST, "wallets")
        Result.failure(e)
    }

    fun observeWalletConfig(adminUid: String): Flow<WalletConfig> =
        db.collection("appConfig").document("wallet").snapshots()
            .map { snap ->
                snap.toObject(WalletConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: WalletConfig(id = "wallet")
            }
            .retryTransientFirestoreErrors("SpiderAdminRepo") { auth.currentUser?.uid == adminUid }
            .catch { logAndRethrow(it, OperationType.GET, "appConfig/wallet", adminUid) }

    fun observeAllPayments(adminUid: String): Flow<List<PaymentOrder>> =
        db.collection("payments").snapshots()
            .map { snap ->
                snap.toObjects(PaymentOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .retryTransientFirestoreErrors("SpiderAdminRepo") { auth.currentUser?.uid == adminUid }
            .catch { logAndRethrow(it, OperationType.LIST, "payments", adminUid) }

    fun observeAllTransactions(adminUid: String): Flow<List<WalletTransaction>> =
        db.collection("transactions").snapshots()
            .map { snap ->
                snap.toObjects(WalletTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .retryTransientFirestoreErrors("SpiderAdminRepo") { auth.currentUser?.uid == adminUid }
            .catch { logAndRethrow(it, OperationType.LIST, "transactions", adminUid) }

    fun observeAllPayouts(adminUid: String): Flow<List<PayoutRequest>> =
        db.collection("payoutRequests").snapshots()
            .map { snap ->
                snap.toObjects(PayoutRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .retryTransientFirestoreErrors("SpiderAdminRepo") { auth.currentUser?.uid == adminUid }
            .catch { logAndRethrow(it, OperationType.LIST, "payoutRequests", adminUid) }

    fun observeAllTickets(adminUid: String): Flow<List<SupportTicket>> =
        db.collection("supportTickets").snapshots()
            .map { snap ->
                snap.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .retryTransientFirestoreErrors("SpiderAdminRepo") { auth.currentUser?.uid == adminUid }
            .catch { logAndRethrow(it, OperationType.LIST, "supportTickets", adminUid) }

    fun observeAllDisputes(adminUid: String): Flow<List<Dispute>> =
        db.collection("disputes").snapshots()
            .map { snap ->
                snap.toObjects(Dispute::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .retryTransientFirestoreErrors("SpiderAdminRepo") { auth.currentUser?.uid == adminUid }
            .catch { logAndRethrow(it, OperationType.LIST, "disputes", adminUid) }

    fun observeAuditLogs(adminUid: String): Flow<List<AuditLog>> =
        db.collection("auditLogs").snapshots()
            .map { snap ->
                snap.toObjects(AuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .retryTransientFirestoreErrors("SpiderAdminRepo") { auth.currentUser?.uid == adminUid }
            .catch { logAndRethrow(it, OperationType.LIST, "auditLogs", adminUid) }

    fun observeAllReferrals(adminUid: String): Flow<List<ReferralRecord>> =
        db.collection("referrals").snapshots()
            .map { snap ->
                snap.toObjects(ReferralRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .retryTransientFirestoreErrors("SpiderAdminRepo") { auth.currentUser?.uid == adminUid }
            .catch { logAndRethrow(it, OperationType.LIST, "referrals", adminUid) }

    fun observeAllAdminUsers(adminUid: String): Flow<List<AdminUserRecord>> =
        db.collection("adminUsers").snapshots()
            .map { snap ->
                snap.toObjects(AdminUserRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .retryTransientFirestoreErrors("SpiderAdminRepo") { auth.currentUser?.uid == adminUid }
            .catch { logAndRethrow(it, OperationType.LIST, "adminUsers", adminUid) }

    fun observeAllResults(adminUid: String): Flow<List<MatchResult>> =
        db.collection("results").snapshots()
            .map { snap ->
                snap.toObjects(MatchResult::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareBy<MatchResult> { it.tournamentId }.thenBy { it.placement })
            }
            .retryTransientFirestoreErrors("SpiderAdminRepo") { auth.currentUser?.uid == adminUid }
            .catch { logAndRethrow(it, OperationType.LIST, "results", adminUid) }

    suspend fun saveCategory(
        existingId: String?,
        name: String,
        imageUrl: String,
        categoryType: String,
        presetKey: String,
        enabled: Boolean,
        displayOrder: Long
    ): Result<Unit> = runAdminMutation("tournamentCategories") { uid, email ->
        val catId = existingId?.takeIf { it.isNotBlank() }
            ?: "cat_${name.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "_").take(24)}_${System.currentTimeMillis() % 10000}"
        val ref = db.collection("tournamentCategories").document(catId)
        val now = FieldValue.serverTimestamp()
        val batch = db.batch()
        if (existingId.isNullOrBlank()) {
            batch.set(
                ref,
                mapOf(
                    "categoryName" to name.trim().take(50),
                    "categoryImageUrl" to imageUrl.take(200000),
                    "categoryType" to categoryType,
                    "presetKey" to presetKey,
                    "enabled" to enabled,
                    "randomTournament" to false,
                    "displayOrder" to displayOrder,
                    "createdAt" to now,
                    "updatedAt" to now
                )
            )
        } else {
            batch.update(
                ref,
                mapOf(
                    "categoryName" to name.trim().take(50),
                    "categoryImageUrl" to imageUrl.take(200000),
                    "categoryType" to categoryType,
                    "presetKey" to presetKey,
                    "enabled" to enabled,
                    "displayOrder" to displayOrder,
                    "updatedAt" to now
                )
            )
        }
        appendAudit(batch, uid, email, if (existingId == null) "CREATE_CATEGORY" else "UPDATE_CATEGORY", "tournamentCategories", catId, "Saved category $name")
        batch.commit().await()
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> = runAdminMutation("tournamentCategories/$categoryId") { uid, email ->
        val batch = db.batch()
        batch.delete(db.collection("tournamentCategories").document(categoryId))
        appendAudit(batch, uid, email, "DELETE_CATEGORY", "tournamentCategories", categoryId, "Deleted category $categoryId")
        batch.commit().await()
    }

    suspend fun saveRandomTournament(
        existingTypeId: String?,
        existingCategoryId: String?,
        displayName: String,
        bannerUrl: String,
        shortDescription: String,
        matchDetails: String,
        gameMode: String,
        mapName: String,
        entryFee: Double,
        prizePool: Double,
        perKillReward: Double,
        maxParticipants: Long,
        matchDate: String,
        matchTime: String,
        registrationDeadline: String,
        rules: String,
        visibility: String,
        status: String,
        displayOrder: Long,
        enabled: Boolean,
        archived: Boolean
    ): Result<Unit> = runAdminMutation("customTournamentTypes") { uid, email ->
        val suffix = System.currentTimeMillis().toString()
        val typeId = existingTypeId?.takeIf { it.isNotBlank() } ?: "rand_$suffix"
        val categoryId = existingCategoryId?.takeIf { it.isNotBlank() } ?: "cat_rand_$suffix"
        val tourId = "tour_$typeId"
        val now = FieldValue.serverTimestamp()
        val batch = db.batch()
        val cleanDetails = matchDetails.trim().take(4000)

        val typeRef = db.collection("customTournamentTypes").document(typeId)
        val typePayload = mutableMapOf<String, Any>(
            "displayName" to displayName.trim().take(60),
            "bannerUrl" to bannerUrl.take(200000),
            "shortDescription" to shortDescription.trim().take(300),
            "matchDetails" to cleanDetails,
            "gameMode" to gameMode.trim().take(40),
            "mapName" to mapName.trim().take(40),
            "entryFee" to entryFee.coerceAtLeast(0.0),
            "prizePool" to prizePool.coerceAtLeast(0.0),
            "perKillReward" to perKillReward.coerceAtLeast(0.0),
            "maxParticipants" to maxParticipants.coerceIn(2L, 500L),
            "matchDate" to matchDate.trim().take(40),
            "matchTime" to matchTime.trim().take(40),
            "registrationDeadline" to registrationDeadline.trim().take(60),
            "rules" to rules.trim().take(4000),
            "visibility" to visibility,
            "status" to status,
            "categoryId" to categoryId,
            "displayOrder" to displayOrder,
            "enabled" to enabled,
            "archived" to archived,
            "updatedAt" to now
        )
        if (existingTypeId.isNullOrBlank()) {
            typePayload["createdAt"] = now
            batch.set(typeRef, typePayload)
        } else {
            batch.update(typeRef, typePayload)
        }

        val catRef = db.collection("tournamentCategories").document(categoryId)
        val catEnabled = enabled && !archived && visibility == "PUBLIC"
        if (existingCategoryId.isNullOrBlank()) {
            batch.set(
                catRef,
                mapOf(
                    "categoryName" to displayName.trim().take(50),
                    "categoryImageUrl" to bannerUrl.take(200000),
                    "categoryType" to "CUSTOM",
                    "presetKey" to "championship",
                    "enabled" to catEnabled,
                    "randomTournament" to true,
                    "displayOrder" to displayOrder,
                    "createdAt" to now,
                    "updatedAt" to now
                )
            )
        } else {
            batch.update(
                catRef,
                mapOf(
                    "categoryName" to displayName.trim().take(50),
                    "categoryImageUrl" to bannerUrl.take(200000),
                    "enabled" to catEnabled,
                    "displayOrder" to displayOrder,
                    "updatedAt" to now
                )
            )
        }

        val tourRef = db.collection("tournaments").document(tourId)
        val tourStatus = if (archived) "ARCHIVED" else status
        val tourPayload = mutableMapOf<String, Any>(
            "categoryId" to categoryId,
            "title" to displayName.trim().take(80),
            "shortDescription" to shortDescription.trim().take(300),
            "matchDetails" to cleanDetails,
            "gameMode" to gameMode.trim().take(40),
            "mapName" to mapName.trim().take(40),
            "entryFee" to entryFee.coerceAtLeast(0.0),
            "prizePool" to prizePool.coerceAtLeast(0.0),
            "perKillReward" to perKillReward.coerceAtLeast(0.0),
            "maxParticipants" to maxParticipants.coerceIn(2L, 500L),
            "matchDate" to matchDate.trim().take(40),
            "matchTime" to matchTime.trim().take(40),
            "registrationDeadline" to registrationDeadline.trim().take(60),
            "rules" to rules.trim().take(4000),
            "bannerUrl" to bannerUrl.take(200000),
            "presetKey" to "championship",
            "visibility" to visibility,
            "status" to tourStatus,
            "soloFormat" to gameMode.contains("SOLO", ignoreCase = true),
            "randomTournament" to true,
            "updatedAt" to now
        )
        if (existingTypeId.isNullOrBlank()) {
            tourPayload["registeredCount"] = 0L
            tourPayload["roomReleased"] = false
            tourPayload["createdAt"] = now
            batch.set(tourRef, tourPayload)
        } else {
            batch.update(tourRef, tourPayload)
        }

        appendAudit(batch, uid, email, if (existingTypeId == null) "CREATE_RANDOM_TOURNAMENT" else "UPDATE_RANDOM_TOURNAMENT", "customTournamentTypes", typeId, "Saved Random Tournament: $displayName")
        batch.commit().await()
    }

    suspend fun deleteRandomTournament(type: CustomTournamentType): Result<Unit> =
        runAdminMutation("customTournamentTypes/${type.id}") { uid, email ->
            val batch = db.batch()
            batch.delete(db.collection("customTournamentTypes").document(type.id))
            if (type.categoryId.isNotBlank()) {
                batch.delete(db.collection("tournamentCategories").document(type.categoryId))
            }
            batch.delete(db.collection("tournaments").document("tour_${type.id}"))
            appendAudit(batch, uid, email, "DELETE_RANDOM_TOURNAMENT", "customTournamentTypes", type.id, "Deleted Random Tournament ${type.displayName}")
            batch.commit().await()
        }

    suspend fun saveBanner(
        existingId: String?,
        title: String,
        subtitle: String,
        imageUrl: String,
        presetKey: String,
        targetAction: String,
        displayOrder: Long,
        enabled: Boolean
    ): Result<Unit> = runAdminMutation("banners") { uid, email ->
        val id = existingId?.takeIf { it.isNotBlank() } ?: "banner_${System.currentTimeMillis()}"
        val now = FieldValue.serverTimestamp()
        val ref = db.collection("banners").document(id)
        val batch = db.batch()
        val data = mutableMapOf<String, Any>(
            "title" to title.trim().take(80),
            "subtitle" to subtitle.trim().take(140),
            "imageUrl" to imageUrl.take(200000),
            "presetKey" to presetKey,
            "targetAction" to targetAction,
            "targetId" to "",
            "displayOrder" to displayOrder,
            "enabled" to enabled,
            "updatedAt" to now
        )
        if (existingId.isNullOrBlank()) {
            data["createdAt"] = now
            batch.set(ref, data)
        } else {
            batch.update(ref, data)
        }
        appendAudit(batch, uid, email, "SAVE_BANNER", "banners", id, "Saved banner $title")
        batch.commit().await()
    }

    suspend fun deleteBanner(bannerId: String): Result<Unit> = runAdminMutation("banners/$bannerId") { uid, email ->
        val batch = db.batch()
        batch.delete(db.collection("banners").document(bannerId))
        appendAudit(batch, uid, email, "DELETE_BANNER", "banners", bannerId, "Deleted banner $bannerId")
        batch.commit().await()
    }

    suspend fun saveMapBanner(
        existingId: String?,
        mapName: String,
        bannerUrl: String,
        presetKey: String,
        displayOrder: Long,
        enabled: Boolean
    ): Result<Unit> = runAdminMutation("mapBanners") { uid, email ->
        val cleanName = mapName.trim().take(40)
        require(cleanName.length >= 2) { "Map name must be at least 2 characters." }
        val slug = cleanName.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "custom" }
        val docId = existingId?.takeIf { it.isNotBlank() } ?: "map_${slug.take(40)}"
        val ref = db.collection("mapBanners").document(docId)
        val existingSnap = ref.get().await()
        val now = FieldValue.serverTimestamp()
        val batch = db.batch()
        val safePreset = presetKey.trim().take(64).ifBlank { "battle_royale" }
        val data = mutableMapOf<String, Any>(
            "mapName" to cleanName,
            "bannerUrl" to bannerUrl.take(200000),
            "presetKey" to safePreset,
            "displayOrder" to displayOrder.coerceIn(0L, 10000L),
            "enabled" to enabled,
            "updatedAt" to now
        )
        if (!existingSnap.exists()) {
            data["createdAt"] = now
            batch.set(ref, data)
        } else {
            batch.update(ref, data)
        }
        appendAudit(batch, uid, email, "SAVE_MAP_BANNER", "mapBanners", docId, "Saved map banner for $cleanName")
        batch.commit().await()
    }

    suspend fun deleteMapBanner(mapId: String): Result<Unit> = runAdminMutation("mapBanners/$mapId") { uid, email ->
        val batch = db.batch()
        batch.delete(db.collection("mapBanners").document(mapId))
        appendAudit(batch, uid, email, "DELETE_MAP_BANNER", "mapBanners", mapId, "Deleted map banner $mapId")
        batch.commit().await()
    }

    suspend fun saveTournament(
        existingId: String?,
        categoryId: String,
        title: String,
        shortDescription: String,
        matchDetails: String,
        gameMode: String,
        mapName: String,
        entryFee: Double,
        prizePool: Double,
        perKillReward: Double,
        maxParticipants: Long,
        matchDate: String,
        matchTime: String,
        registrationDeadline: String,
        rules: String,
        bannerUrl: String,
        presetKey: String,
        visibility: String,
        status: String,
        soloFormat: Boolean
    ): Result<Unit> = runAdminMutation("tournaments") { uid, email ->
        val id = existingId?.takeIf { it.isNotBlank() } ?: "tour_${System.currentTimeMillis()}"
        val now = FieldValue.serverTimestamp()
        val ref = db.collection("tournaments").document(id)
        val batch = db.batch()
        val safeMode = gameMode.trim().take(40).ifBlank { "BATTLE ROYALE" }
        val safeMap = mapName.trim().take(40).ifBlank { "Bermuda" }
        val safeDesc = shortDescription.trim().take(300).ifBlank { "$safeMode tournament on $safeMap" }
        val safeDetails = matchDetails.trim().take(4000)
        val safeDeadline = registrationDeadline.trim().take(60).ifBlank { "$matchDate $matchTime".trim() }
        val safeRules = rules.trim().take(4000).ifBlank { "Fair-play rules enforced. Zero hacks or teaming." }
        val safeVisibility = if (visibility in listOf("PUBLIC", "HIDDEN")) visibility else "PUBLIC"
        val safeStatus = if (status in listOf("UPCOMING", "LIVE", "FULL", "COMPLETED", "CANCELLED", "ARCHIVED")) status else "UPCOMING"
        val data = mutableMapOf<String, Any>(
            "categoryId" to categoryId,
            "title" to title.trim().take(80),
            "shortDescription" to safeDesc,
            "matchDetails" to safeDetails,
            "gameMode" to safeMode,
            "mapName" to safeMap,
            "entryFee" to entryFee.coerceAtLeast(0.0),
            "prizePool" to prizePool.coerceAtLeast(0.0),
            "perKillReward" to perKillReward.coerceAtLeast(0.0),
            "maxParticipants" to maxParticipants.coerceIn(2L, 500L),
            "matchDate" to matchDate.trim().take(40),
            "matchTime" to matchTime.trim().take(40),
            "registrationDeadline" to safeDeadline,
            "rules" to safeRules,
            "bannerUrl" to bannerUrl.take(200000),
            "presetKey" to presetKey.ifBlank { "battle_royale" },
            "visibility" to safeVisibility,
            "status" to safeStatus,
            "soloFormat" to soloFormat,
            "updatedAt" to now
        )
        if (existingId.isNullOrBlank()) {
            data["registeredCount"] = 0L
            data["randomTournament"] = false
            data["roomReleased"] = false
            data["createdAt"] = now
            batch.set(ref, data)
        } else {
            batch.update(ref, data)
        }
        appendAudit(
            batch,
            uid,
            email,
            if (existingId.isNullOrBlank()) "CREATE_TOURNAMENT" else "UPDATE_TOURNAMENT",
            "tournaments",
            id,
            "Saved tournament $title (Per Kill=${perKillReward.toInt()}, Prize=${prizePool.toInt()})"
        )
        batch.commit().await()
    }

    suspend fun deleteTournament(tournament: Tournament): Result<Unit> =
        runAdminMutation("tournaments/${tournament.id}") { uid, email ->
            val batch = db.batch()
            batch.delete(db.collection("tournaments").document(tournament.id))
            appendAudit(batch, uid, email, "DELETE_TOURNAMENT", "tournaments", tournament.id, "Deleted tournament ${tournament.title}")
            batch.commit().await()
        }

    suspend fun publishRoomCredentials(
        tournament: Tournament,
        roomId: String,
        roomPassword: String
    ): Result<Unit> = runAdminMutation("tournaments/${tournament.id}/credentials/room") { uid, email ->
        val now = FieldValue.serverTimestamp()
        val regs = db.collection("registrations")
            .whereEqualTo("tournamentId", tournament.id)
            .get()
            .await()
            .documents
            .mapNotNull { it.getString("userId") }
            .distinct()

        val batch = db.batch()
        batch.set(
            db.collection("tournaments").document(tournament.id)
                .collection("credentials").document("room"),
            mapOf(
                "tournamentId" to tournament.id,
                "roomId" to roomId.trim(),
                "roomPassword" to roomPassword.trim(),
                "released" to true,
                "releaseNotice" to "Released by Admin",
                "updatedAt" to now
            )
        )
        batch.update(
            db.collection("tournaments").document(tournament.id),
            mapOf("roomReleased" to true, "status" to "LIVE", "updatedAt" to now)
        )
        batch.set(
            db.collection("matches").document("match_${tournament.id}"),
            mapOf(
                "tournamentId" to tournament.id,
                "title" to tournament.title,
                "categoryId" to tournament.categoryId,
                "gameMode" to tournament.gameMode,
                "mapName" to tournament.mapName,
                "status" to "ONGOING",
                "matchDate" to tournament.matchDate,
                "matchTime" to tournament.matchTime,
                "roomReleased" to true,
                "participantUids" to regs,
                "createdAt" to now,
                "updatedAt" to now
            )
        )
        appendAudit(batch, uid, email, "PUBLISH_ROOM_CREDENTIALS", "tournaments", tournament.id, "Released room credentials for ${tournament.title}")
        batch.commit().await()
    }

    suspend fun setUserStatus(targetUid: String, newStatus: String): Result<Unit> =
        runAdminMutation("users/$targetUid") { uid, email ->
            require(newStatus in listOf("ACTIVE", "SUSPENDED", "BANNED")) { "Unsupported status: $newStatus" }
            val userRef = db.collection("users").document(targetUid)
            val snap = userRef.get().await()
            val targetEmail = snap.getString("email") ?: ""
            if (targetEmail.equals(AuthHelper.BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true) && newStatus != "ACTIVE") {
                throw IllegalStateException("Cannot suspend or ban the Owner Admin account.")
            }
            val batch = db.batch()
            batch.update(
                userRef,
                mapOf("status" to newStatus, "updatedAt" to FieldValue.serverTimestamp())
            )
            appendAudit(batch, uid, email, "USER_STATUS_$newStatus", "users", targetUid, "Changed user $targetUid status to $newStatus")
            batch.commit().await()
        }

    suspend fun saveWalletQrConfig(
        qrImageUrl: String,
        upiId: String,
        instructions: String,
        minWithdraw: Double = 50.0
    ): Result<Unit> = runAdminMutation("appConfig/wallet") { uid, email ->
        val safeMin = minWithdraw.coerceAtLeast(50.0)
        val batch = db.batch()
        batch.set(
            db.collection("appConfig").document("wallet"),
            mapOf(
                "qrImageUrl" to qrImageUrl.take(200000),
                "upiId" to upiId.trim().take(120),
                "instructions" to instructions.trim().take(500).ifBlank { "Scan QR to pay, enter UTR/Reference ID below. 50 Coins = ₹50." },
                "minWithdraw" to safeMin,
                "coinsPerRupee" to 1.0,
                "winningOnlyRedeem" to true,
                "updatedBy" to email,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        appendAudit(batch, uid, email, "UPDATE_WALLET_QR_CONFIG", "appConfig", "wallet", "Updated Add Coin QR & withdrawal config (minWithdraw=$safeMin)")
        batch.commit().await()
    }

    suspend fun adjustUserWallet(
        targetUid: String,
        amountDelta: Double,
        direction: String,
        bucket: String,
        reason: String
    ): Result<Unit> = runAdminMutation("wallets/$targetUid") { uid, email ->
        val cleanAmount = SpiderWalletUtil.roundCoins(amountDelta)
        require(cleanAmount > 0.0) { "Amount must be greater than 0" }
        val normalizedDir = direction.uppercase(Locale.US)
        val normalizedBucket = bucket.uppercase(Locale.US).let {
            if (it in listOf("WINNING", "BONUS", "DEPOSIT")) it else "BONUS"
        }
        val walletRef = db.collection("wallets").document(targetUid)
        val txRef = db.collection("transactions").document()
        val notifRef = db.collection("notifications").document()
        val auditRef = db.collection("auditLogs").document()

        db.runTransaction { tx ->
            val snap = tx.get(walletRef)
            val currentWallet = SpiderWalletUtil.fromDocumentOrZero(snap)
            val nextWallet = SpiderWalletUtil.computeAdminAdjustment(
                current = currentWallet,
                amountDelta = cleanAmount,
                direction = normalizedDir,
                bucket = normalizedBucket
            )
            val now = FieldValue.serverTimestamp()

            val walletData = nextWallet.toWriteMap(
                userId = targetUid,
                now = now,
                includeCreatedAt = !snap.exists()
            )
            if (!snap.exists()) {
                tx.set(walletRef, walletData)
            } else {
                tx.update(walletRef, walletData)
            }

            val txType = when {
                normalizedDir == "DEBIT" -> "ADMIN_ADJUSTMENT"
                normalizedBucket == "WINNING" -> "PRIZE"
                normalizedBucket == "BONUS" -> "BONUS"
                else -> "DEPOSIT"
            }
            val descPrefix = "$normalizedBucket ${normalizedDir.lowercase(Locale.US)}"
            tx.set(
                txRef,
                mapOf(
                    "userId" to targetUid,
                    "type" to txType,
                    "direction" to normalizedDir,
                    "description" to "${reason.trim().ifBlank { "Admin $descPrefix" }} [$normalizedBucket]".take(200),
                    "matchId" to "",
                    "amount" to cleanAmount,
                    "status" to "COMPLETED",
                    "resultingBalance" to nextWallet.total,
                    "createdAt" to now
                )
            )
            tx.set(
                notifRef,
                mapOf(
                    "userId" to targetUid,
                    "title" to "Wallet Updated ($normalizedBucket)",
                    "message" to "${if (normalizedDir == "CREDIT") "+" else "-"}${String.format(Locale.US, "%.0f", cleanAmount)} Coins in your $normalizedBucket balance. ${reason.trim()}".take(400),
                    "type" to txType,
                    "read" to false,
                    "createdAt" to now
                )
            )
            tx.set(
                auditRef,
                mapOf(
                    "actorId" to uid,
                    "actorEmail" to email,
                    "action" to "WALLET_${normalizedBucket}_$normalizedDir",
                    "targetCollection" to "wallets",
                    "targetId" to targetUid,
                    "summary" to "Adjusted $targetUid [$normalizedBucket] $normalizedDir $cleanAmount ($reason)".take(400),
                    "createdAt" to now
                )
            )
        }.await()
    }

    suspend fun reviewDepositRequest(
        payment: PaymentOrder,
        approve: Boolean,
        note: String = ""
    ): Result<Unit> = runAdminMutation("payments/${payment.id}") { adminUid, adminEmail ->
        val paymentRef = db.collection("payments").document(payment.id)
        val walletRef = db.collection("wallets").document(payment.userId)
        val txRef = db.collection("transactions").document("tx_dep_${payment.id}")
        val notifRef = db.collection("notifications").document("notif_dep_${payment.id}")
        val auditRef = db.collection("auditLogs").document()

        db.runTransaction { tx ->
            val paySnap = tx.get(paymentRef)
            if (!paySnap.exists()) throw IllegalStateException("Deposit request not found.")
            if (paySnap.getString("status") != "PENDING") {
                throw IllegalStateException("Deposit request already processed.")
            }
            val walletSnap = tx.get(walletRef)
            val now = FieldValue.serverTimestamp()

            if (approve) {
                val cleanAmount = SpiderWalletUtil.roundCoins(payment.coinAmount)
                val currentWallet = SpiderWalletUtil.fromDocumentOrZero(walletSnap)
                val nextWallet = SpiderWalletUtil.computeDepositApproval(currentWallet, cleanAmount)
                val walletData = nextWallet.toWriteMap(
                    userId = payment.userId,
                    now = now,
                    includeCreatedAt = !walletSnap.exists()
                )
                if (!walletSnap.exists()) {
                    tx.set(walletRef, walletData)
                } else {
                    tx.update(walletRef, walletData)
                }
                tx.update(
                    paymentRef,
                    mapOf("status" to "APPROVED", "reviewedBy" to adminEmail, "updatedAt" to now)
                )
                tx.set(
                    txRef,
                    mapOf(
                        "userId" to payment.userId,
                        "type" to "DEPOSIT",
                        "direction" to "CREDIT",
                        "description" to "Deposit approved (Ref: ${payment.referenceId.ifBlank { payment.id }})".take(200),
                        "matchId" to "",
                        "amount" to cleanAmount,
                        "status" to "COMPLETED",
                        "resultingBalance" to nextWallet.total,
                        "createdAt" to now
                    )
                )
                tx.set(
                    notifRef,
                    mapOf(
                        "userId" to payment.userId,
                        "title" to "Deposit Approved",
                        "message" to "+${cleanAmount.toInt()} Coins credited to your Deposit balance.",
                        "type" to "DEPOSIT",
                        "read" to false,
                        "createdAt" to now
                    )
                )
            } else {
                tx.update(
                    paymentRef,
                    mapOf("status" to "REJECTED", "reviewedBy" to adminEmail, "updatedAt" to now)
                )
                tx.set(
                    notifRef,
                    mapOf(
                        "userId" to payment.userId,
                        "title" to "Deposit Request Rejected",
                        "message" to "Your deposit of ${payment.coinAmount.toInt()} Coins was rejected. ${note.trim()}".take(400),
                        "type" to "DEPOSIT",
                        "read" to false,
                        "createdAt" to now
                    )
                )
            }
            tx.set(
                auditRef,
                mapOf(
                    "actorId" to adminUid,
                    "actorEmail" to adminEmail,
                    "action" to if (approve) "APPROVE_DEPOSIT" else "REJECT_DEPOSIT",
                    "targetCollection" to "payments",
                    "targetId" to payment.id,
                    "summary" to "${if (approve) "Approved" else "Rejected"} deposit ${payment.coinAmount.toInt()} Coins for ${payment.userId}".take(400),
                    "createdAt" to now
                )
            )
        }.await()
    }

    suspend fun reviewPayoutRequest(
        payout: PayoutRequest,
        approve: Boolean,
        note: String = ""
    ): Result<Unit> = runAdminMutation("payoutRequests/${payout.id}") { adminUid, adminEmail ->
        val payoutRef = db.collection("payoutRequests").document(payout.id)
        val walletRef = db.collection("wallets").document(payout.userId)
        val txRef = db.collection("transactions").document("tx_out_${payout.id}")
        val revTxRef = db.collection("transactions").document("tx_out_rev_${payout.id}")
        val notifRef = db.collection("notifications").document("notif_out_${payout.id}")
        val auditRef = db.collection("auditLogs").document()

        db.runTransaction { tx ->
            // All reads MUST precede all writes in Firestore transactions
            val payoutSnap = tx.get(payoutRef)
            if (!payoutSnap.exists()) throw IllegalStateException("Withdrawal request not found.")
            if (payoutSnap.getString("status") != "PENDING") {
                throw IllegalStateException("Withdrawal request already processed.")
            }
            val walletSnap = tx.get(walletRef)
            val existingTxSnap = tx.get(txRef)
            val now = FieldValue.serverTimestamp()

            val wasHeldOnSubmit = existingTxSnap.exists() && existingTxSnap.getString("status") == "PENDING"
            val cleanAmount = SpiderWalletUtil.roundCoins(payout.amount)
            val currentWallet = SpiderWalletUtil.fromDocumentOrZero(walletSnap)
            val nextWallet = SpiderWalletUtil.computePayoutReview(
                current = currentWallet,
                coinAmount = cleanAmount,
                approve = approve,
                wasHeldOnSubmit = wasHeldOnSubmit
            )

            if (approve) {
                val walletData = nextWallet.toWriteMap(
                    userId = payout.userId,
                    now = now,
                    includeCreatedAt = !walletSnap.exists()
                )
                if (!walletSnap.exists()) {
                    tx.set(walletRef, walletData)
                } else {
                    tx.update(walletRef, walletData)
                }
                tx.update(
                    payoutRef,
                    mapOf("status" to "PAID", "reviewedBy" to adminEmail, "updatedAt" to now)
                )
                val paidDesc = "Winning payout paid via ${payout.destinationType} (${payout.destinationDetails})".take(200)
                if (existingTxSnap.exists()) {
                    tx.update(
                        txRef,
                        mapOf(
                            "description" to paidDesc,
                            "status" to "COMPLETED",
                            "resultingBalance" to nextWallet.total
                        )
                    )
                } else {
                    tx.set(
                        txRef,
                        mapOf(
                            "userId" to payout.userId,
                            "type" to "PAYOUT",
                            "direction" to "DEBIT",
                            "description" to paidDesc,
                            "matchId" to "",
                            "amount" to cleanAmount,
                            "status" to "COMPLETED",
                            "resultingBalance" to nextWallet.total,
                            "createdAt" to now
                        )
                    )
                }
                tx.set(
                    notifRef,
                    mapOf(
                        "userId" to payout.userId,
                        "title" to "Withdrawal Paid",
                        "message" to "₹${cleanAmount.toInt()} (${cleanAmount.toInt()} Winning Coins) sent to ${payout.destinationDetails.ifBlank { payout.destinationType }}.",
                        "type" to "PAYOUT",
                        "read" to false,
                        "createdAt" to now
                    )
                )
            } else {
                if (wasHeldOnSubmit) {
                    val walletData = nextWallet.toWriteMap(
                        userId = payout.userId,
                        now = now,
                        includeCreatedAt = !walletSnap.exists()
                    )
                    if (!walletSnap.exists()) {
                        tx.set(walletRef, walletData)
                    } else {
                        tx.update(walletRef, walletData)
                    }
                    tx.update(
                        txRef,
                        mapOf("status" to "REVERSED")
                    )
                    tx.set(
                        revTxRef,
                        mapOf(
                            "userId" to payout.userId,
                            "type" to "REFUND",
                            "direction" to "CREDIT",
                            "description" to "Withdrawal rejected refund (${payout.destinationType})".take(200),
                            "matchId" to "",
                            "amount" to cleanAmount,
                            "status" to "COMPLETED",
                            "resultingBalance" to nextWallet.total,
                            "createdAt" to now
                        )
                    )
                }
                tx.update(
                    payoutRef,
                    mapOf("status" to "REJECTED", "reviewedBy" to adminEmail, "updatedAt" to now)
                )
                val rejectMsg = if (wasHeldOnSubmit) {
                    "Your ${cleanAmount.toInt()} Coins withdrawal was rejected and refunded to Winning balance. ${note.trim()}"
                } else {
                    "Your ${cleanAmount.toInt()} Coins withdrawal was rejected. ${note.trim()}"
                }
                tx.set(
                    notifRef,
                    mapOf(
                        "userId" to payout.userId,
                        "title" to if (wasHeldOnSubmit) "Withdrawal Rejected & Refunded" else "Withdrawal Rejected",
                        "message" to rejectMsg.take(400),
                        "type" to if (wasHeldOnSubmit) "REFUND" else "PAYOUT",
                        "read" to false,
                        "createdAt" to now
                    )
                )
            }
            tx.set(
                auditRef,
                mapOf(
                    "actorId" to adminUid,
                    "actorEmail" to adminEmail,
                    "action" to if (approve) "APPROVE_PAYOUT" else "REJECT_PAYOUT",
                    "targetCollection" to "payoutRequests",
                    "targetId" to payout.id,
                    "summary" to "${if (approve) "Paid" else "Rejected"} withdrawal ${cleanAmount.toInt()} Coins for ${payout.userId}".take(400),
                    "createdAt" to now
                )
            )
        }.await()
    }

    private fun placementPoints(placement: Long): Long = when (placement) {
        1L -> 100L
        2L -> 60L
        3L -> 40L
        in 4L..10L -> 25L
        else -> 15L
    }

    suspend fun verifyMatchResultAndRank(
        tournamentId: String,
        tournamentTitle: String,
        playerName: String,
        targetUser: UserProfile?,
        placement: Long,
        kills: Long,
        rewardCoins: Double,
        leaderboardMode: String
    ): Result<Unit> = runAdminMutation("results") { uid, email ->
        val cleanName = playerName.trim().take(40)
        require(cleanName.isNotEmpty()) { "Player name is required." }
        val cleanPlacement = placement.coerceIn(1L, 500L)
        val cleanKills = kills.coerceIn(0L, 200L)
        val cleanReward = SpiderWalletUtil.roundCoins(rewardCoins.coerceAtLeast(0.0))
        val cleanTitle = tournamentTitle.trim().take(80).let { if (it.length < 2) "Match" else it }
        val allowedModes = setOf("OVERALL", "WEEKLY", "MONTHLY", "SOLO", "DUO", "LONE_WOLF", "CLASH_SQUAD")
        val normalizedMode = if (leaderboardMode in allowedModes) leaderboardMode else "SOLO"

        val resId = "${tournamentId}_pos_${cleanPlacement}"
        val resRef = db.collection("results").document(resId)
        val auditRef = db.collection("auditLogs").document()

        db.runTransaction { tx ->
            // All reads MUST precede all writes in Firestore transactions
            val existingRes = tx.get(resRef)
            val linkedUid = targetUser?.userId?.takeIf { it.isNotBlank() }
            val userRef = linkedUid?.let { db.collection("users").document(it) }
            val walletRef = linkedUid?.let { db.collection("wallets").document(it) }
            val txRef = linkedUid?.let { db.collection("transactions").document("tx_prize_${tournamentId}_$it") }
            val notifRef = linkedUid?.let { db.collection("notifications").document("notif_prize_${tournamentId}_$it") }

            val userSnap = userRef?.let { tx.get(it) }
            val walletSnap = walletRef?.let { tx.get(it) }
            val existingPrizeTx = txRef?.let { tx.get(it) }

            val now = FieldValue.serverTimestamp()
            val pointsEarned = (cleanKills * 10L) + placementPoints(cleanPlacement)

            tx.set(
                resRef,
                mapOf(
                    "tournamentId" to tournamentId,
                    "tournamentTitle" to cleanTitle,
                    "userId" to (linkedUid ?: ""),
                    "username" to cleanName,
                    "placement" to cleanPlacement,
                    "kills" to cleanKills,
                    "rewardCoins" to cleanReward,
                    "verified" to true,
                    "declaredBy" to email,
                    "createdAt" to (existingRes.getTimestamp("createdAt") ?: now),
                    "updatedAt" to now
                )
            )

            if (targetUser != null && linkedUid != null && userSnap != null && userSnap.exists() && walletRef != null && walletSnap != null && txRef != null && existingPrizeTx != null && notifRef != null) {
                val curMatches = userSnap.getLong("matchesPlayed") ?: targetUser.matchesPlayed
                val curWins = userSnap.getLong("wins") ?: targetUser.wins
                val curKills = userSnap.getLong("verifiedKills") ?: targetUser.verifiedKills
                val curPoints = userSnap.getLong("rankingPoints") ?: targetUser.rankingPoints

                val wasSameUser = existingRes.exists() && existingRes.getString("userId") == linkedUid
                val oldPlacement = if (wasSameUser) (existingRes.getLong("placement") ?: cleanPlacement) else 0L
                val oldKills = if (wasSameUser) (existingRes.getLong("kills") ?: 0L) else 0L
                val oldPoints = if (wasSameUser) (oldKills * 10L) + placementPoints(oldPlacement) else 0L

                val newMatches = if (wasSameUser) curMatches else curMatches + 1L
                val newWins = (curWins - (if (wasSameUser && oldPlacement == 1L) 1L else 0L) + (if (cleanPlacement == 1L) 1L else 0L)).coerceAtLeast(0L)
                val newKills = (curKills - oldKills + cleanKills).coerceAtLeast(0L)
                val newPoints = (curPoints - oldPoints + pointsEarned).coerceAtLeast(0L)

                tx.update(
                    userRef,
                    mapOf(
                        "matchesPlayed" to newMatches,
                        "wins" to newWins,
                        "verifiedKills" to newKills,
                        "rankingPoints" to newPoints,
                        "updatedAt" to now
                    )
                )

                val lbName = (userSnap.getString("username") ?: targetUser.username).trim().take(32).let { if (it.length < 2) cleanName.take(32).padEnd(2, '_') else it }
                val lbPhoto = userSnap.getString("photoUrl") ?: targetUser.photoUrl
                listOf("OVERALL", normalizedMode).distinct().forEach { modeKey ->
                    tx.set(
                        db.collection("leaderboards").document("${modeKey}_$linkedUid"),
                        mapOf(
                            "userId" to linkedUid,
                            "username" to lbName,
                            "photoUrl" to lbPhoto,
                            "mode" to modeKey,
                            "matches" to newMatches,
                            "wins" to newWins,
                            "verifiedKills" to newKills,
                            "rankingPoints" to newPoints,
                            "verified" to true,
                            "updatedAt" to now
                        )
                    )
                }

                val alreadyCredited = existingPrizeTx.exists()
                val prevAmount = if (alreadyCredited) (existingPrizeTx.getDouble("amount") ?: 0.0) else 0.0
                val delta = SpiderWalletUtil.roundCoins(cleanReward - prevAmount)

                if ((!alreadyCredited && cleanReward > 0.0) || (alreadyCredited && kotlin.math.abs(delta) >= 0.01)) {
                    val currentWallet = SpiderWalletUtil.fromDocumentOrZero(walletSnap)
                    val nextWallet = SpiderWalletUtil.computeWinningPrizeUpdate(currentWallet, delta)
                    val walletData = nextWallet.toWriteMap(
                        userId = linkedUid,
                        now = now,
                        includeCreatedAt = !walletSnap.exists()
                    )
                    if (!walletSnap.exists()) {
                        tx.set(walletRef, walletData)
                    } else {
                        tx.update(walletRef, walletData)
                    }
                    if (!alreadyCredited) {
                        tx.set(
                            txRef,
                            mapOf(
                                "userId" to linkedUid,
                                "type" to "PRIZE",
                                "direction" to "CREDIT",
                                "description" to "Prize for #$cleanPlacement in $cleanTitle ($cleanKills kills)".take(200),
                                "matchId" to tournamentId,
                                "amount" to cleanReward,
                                "status" to "COMPLETED",
                                "resultingBalance" to nextWallet.total,
                                "createdAt" to now
                            )
                        )
                        tx.set(
                            notifRef,
                            mapOf(
                                "userId" to linkedUid,
                                "title" to "Winning Prize Credited!",
                                "message" to "+${cleanReward.toInt()} Winning Coins added for #$cleanPlacement in $cleanTitle.",
                                "type" to "PRIZE",
                                "read" to false,
                                "createdAt" to now
                            )
                        )
                    } else {
                        tx.update(
                            txRef,
                            mapOf(
                                "description" to "Prize for #$cleanPlacement in $cleanTitle ($cleanKills kills)".take(200),
                                "amount" to cleanReward,
                                "resultingBalance" to nextWallet.total
                            )
                        )
                    }
                }
            }

            tx.set(
                auditRef,
                mapOf(
                    "actorId" to uid,
                    "actorEmail" to email,
                    "action" to "VERIFY_RESULT",
                    "targetCollection" to "results",
                    "targetId" to resId,
                    "summary" to "Position #$cleanPlacement: $cleanName ($cleanKills kills, $cleanReward Coins) in $cleanTitle".take(400),
                    "createdAt" to now
                )
            )
        }.await()
    }

    suspend fun clearTournamentPositionResult(
        tournamentId: String,
        placement: Long,
        existingResultId: String
    ): Result<Unit> = runAdminMutation("results") { uid, email ->
        val docId = existingResultId.ifBlank { "${tournamentId}_pos_${placement}" }
        val batch = db.batch()
        batch.delete(db.collection("results").document(docId))
        appendAudit(
            batch, uid, email, "CLEAR_RESULT_POS", "results", docId,
            "Cleared position #$placement result for tournament $tournamentId"
        )
        batch.commit().await()
    }

    suspend fun markTournamentCompleted(tournament: Tournament): Result<Unit> =
        runAdminMutation("tournaments/${tournament.id}") { uid, email ->
            val batch = db.batch()
            batch.update(
                db.collection("tournaments").document(tournament.id),
                mapOf(
                    "status" to "COMPLETED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            appendAudit(
                batch, uid, email, "COMPLETE_TOURNAMENT", "tournaments", tournament.id,
                "Marked '${tournament.title}' as COMPLETED"
            )
            batch.commit().await()
        }

    suspend fun saveAnnouncementOrSupportLinks(
        id: String,
        title: String,
        body: String,
        category: String,
        telegramUrl: String,
        whatsappUrl: String,
        whatsappBannerUrl: String = ""
    ): Result<Unit> = runAdminMutation("announcements/$id") { uid, email ->
        val now = FieldValue.serverTimestamp()
        val batch = db.batch()
        batch.set(
            db.collection("announcements").document(id),
            mapOf(
                "title" to title.trim().take(100),
                "body" to body.trim().take(4000),
                "category" to category,
                "telegramUrl" to telegramUrl.trim().take(300),
                "whatsappUrl" to whatsappUrl.trim().take(300),
                "whatsappBannerUrl" to whatsappBannerUrl.trim().take(200000),
                "priority" to 1L,
                "enabled" to true,
                "createdAt" to now,
                "updatedAt" to now
            )
        )
        appendAudit(batch, uid, email, "UPDATE_ANNOUNCEMENT", "announcements", id, "Updated announcement/config $title")
        batch.commit().await()
    }

    suspend fun replySupportTicket(ticketId: String, status: String, reply: String): Result<Unit> =
        runAdminMutation("supportTickets/$ticketId") { uid, email ->
            val batch = db.batch()
            batch.update(
                db.collection("supportTickets").document(ticketId),
                mapOf(
                    "status" to status,
                    "adminReply" to reply.trim().take(1000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            appendAudit(batch, uid, email, "REPLY_TICKET", "supportTickets", ticketId, "Ticket $ticketId marked $status")
            batch.commit().await()
        }

    suspend fun resolveDispute(disputeId: String, status: String, notes: String): Result<Unit> =
        runAdminMutation("disputes/$disputeId") { uid, email ->
            val batch = db.batch()
            batch.update(
                db.collection("disputes").document(disputeId),
                mapOf(
                    "status" to status,
                    "resolutionNotes" to notes.trim().take(1000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            appendAudit(batch, uid, email, "RESOLVE_DISPUTE", "disputes", disputeId, "Dispute $disputeId marked $status")
            batch.commit().await()
        }

    suspend fun approveAndCreditReferralBonus(
        referral: ReferralRecord,
        bonusCoins: Double
    ): Result<Unit> = runAdminMutation("referrals/${referral.referredUid}") { adminUid, adminEmail ->
        require(bonusCoins > 0.0 && bonusCoins <= 1000.0) { "Referral bonus must be between 1 and 1000 Coins." }
        val refDocId = referral.referredUid.ifBlank { referral.id }
        val referralRef = db.collection("referrals").document(refDocId)
        val walletRef = db.collection("wallets").document(referral.referrerUid)
        val txRef = db.collection("transactions").document("tx_ref_$refDocId")
        val notifRef = db.collection("notifications").document("notif_ref_$refDocId")
        val auditRef = db.collection("auditLogs").document()

        db.runTransaction { tx ->
            val refSnap = tx.get(referralRef)
            val walletSnap = tx.get(walletRef)
            if (!refSnap.exists()) {
                throw IllegalStateException("Referral record not found.")
            }
            if (refSnap.getString("status") != "PENDING") {
                throw IllegalStateException("Referral has already been processed (${refSnap.getString("status")}).")
            }

            val cleanBonus = SpiderWalletUtil.roundCoins(bonusCoins)
            val currentWallet = SpiderWalletUtil.fromDocumentOrZero(walletSnap)
            val nextWallet = SpiderWalletUtil.computeBonusCredit(currentWallet, cleanBonus)
            val now = FieldValue.serverTimestamp()

            val walletData = nextWallet.toWriteMap(
                userId = referral.referrerUid,
                now = now,
                includeCreatedAt = !walletSnap.exists()
            )
            if (!walletSnap.exists()) {
                tx.set(walletRef, walletData)
            } else {
                tx.update(walletRef, walletData)
            }

            tx.set(
                txRef,
                mapOf(
                    "userId" to referral.referrerUid,
                    "type" to "REFERRAL_BONUS",
                    "direction" to "CREDIT",
                    "description" to "Referral bonus for inviting ${referral.referredUsername}".take(200),
                    "matchId" to "",
                    "amount" to cleanBonus,
                    "status" to "COMPLETED",
                    "resultingBalance" to nextWallet.total,
                    "createdAt" to now
                )
            )

            tx.update(
                referralRef,
                mapOf(
                    "status" to "CREDITED",
                    "bonusAmount" to cleanBonus,
                    "transactionId" to txRef.id,
                    "updatedAt" to now
                )
            )

            tx.set(
                notifRef,
                mapOf(
                    "userId" to referral.referrerUid,
                    "title" to "Referral Bonus Credited",
                    "message" to "+${String.format(Locale.US, "%.0f", cleanBonus)} Coins credited for referring ${referral.referredUsername}.",
                    "type" to "REFERRAL_BONUS",
                    "read" to false,
                    "createdAt" to now
                )
            )

            tx.set(
                auditRef,
                mapOf(
                    "actorId" to adminUid,
                    "actorEmail" to adminEmail,
                    "action" to "APPROVE_REFERRAL_BONUS",
                    "targetCollection" to "referrals",
                    "targetId" to refDocId,
                    "summary" to "Credited $cleanBonus Coins to ${referral.referrerUsername} for referring ${referral.referredUsername}".take(400),
                    "createdAt" to now
                )
            )
        }.await()
    }

    suspend fun rejectReferral(referral: ReferralRecord, reason: String): Result<Unit> =
        runAdminMutation("referrals/${referral.referredUid}") { uid, email ->
            val refDocId = referral.referredUid.ifBlank { referral.id }
            val batch = db.batch()
            batch.update(
                db.collection("referrals").document(refDocId),
                mapOf(
                    "status" to "REJECTED",
                    "bonusAmount" to 0.0,
                    "transactionId" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            appendAudit(
                batch, uid, email, "REJECT_REFERRAL", "referrals", refDocId,
                "Rejected referral for ${referral.referredUsername}: ${reason.ifBlank { "Ineligible" }}".take(400)
            )
            batch.commit().await()
        }

    suspend fun grantOrRevokeAdminAccess(
        targetUser: UserProfile,
        grant: Boolean,
        role: String = "OWNER_ADMIN"
    ): Result<Unit> = runAdminMutation("adminUsers/${targetUser.userId}") { uid, email ->
        if (!targetUser.email.equals(AuthHelper.BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true)) {
            throw SecurityException("Admin access cannot be granted to other accounts.")
        }
        if (!grant) {
            throw IllegalStateException("Cannot revoke primary administrator account.")
        }
        require(targetUser.emailVerified) { "Target account must have a verified email." }
        val batch = db.batch()
        val adminRef = db.collection("adminUsers").document(targetUser.userId)
        batch.set(
            adminRef,
            mapOf(
                "userId" to targetUser.userId,
                "email" to AuthHelper.BOOTSTRAP_ADMIN_EMAIL,
                "role" to role.take(32),
                "grantedBy" to email,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        appendAudit(batch, uid, email, "LINK_OWNER_ADMIN", "adminUsers", targetUser.userId, "Verified owner admin ${targetUser.email}")
        batch.commit().await()
    }

    private fun appendAudit(
        batch: com.google.firebase.firestore.WriteBatch,
        uid: String,
        email: String,
        action: String,
        collection: String,
        targetId: String,
        summary: String
    ) {
        batch.set(
            db.collection("auditLogs").document(),
            mapOf(
                "actorId" to uid,
                "actorEmail" to email,
                "action" to action,
                "targetCollection" to collection,
                "targetId" to targetId,
                "summary" to summary.take(400),
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
    }

    private suspend fun runAdminMutation(
        path: String,
        block: suspend (uid: String, email: String) -> Unit
    ): Result<Unit> = try {
        val (uid, email) = requireAdminIdentity()
        block(uid, email)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, path)
        Result.failure(e)
    }

    private fun logAndRethrow(error: Throwable, op: OperationType, path: String, uid: String): Nothing {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val ex = firestoreError ?: (error as? Exception)
        if (ex != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(ex, op, path)
        }
        throw error
    }
}
