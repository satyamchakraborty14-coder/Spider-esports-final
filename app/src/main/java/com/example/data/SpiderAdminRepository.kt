package com.example.data

import com.example.model.AdminUserRecord
import com.example.model.Announcement
import com.example.model.AuditLog
import com.example.model.CustomTournamentType
import com.example.model.Dispute
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
            .catch { logAndRethrow(it, OperationType.LIST, "users", adminUid) }

    fun observeAllCategories(adminUid: String): Flow<List<TournamentCategory>> =
        db.collection("tournamentCategories").snapshots()
            .map { snap ->
                snap.toObjects(TournamentCategory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.displayOrder }
            }
            .catch { logAndRethrow(it, OperationType.LIST, "tournamentCategories", adminUid) }

    fun observeAllCustomTypes(adminUid: String): Flow<List<CustomTournamentType>> =
        db.collection("customTournamentTypes").snapshots()
            .map { snap ->
                snap.toObjects(CustomTournamentType::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.displayOrder }
            }
            .catch { logAndRethrow(it, OperationType.LIST, "customTournamentTypes", adminUid) }

    fun observeAllTournaments(adminUid: String): Flow<List<Tournament>> =
        db.collection("tournaments").snapshots()
            .map { snap ->
                snap.toObjects(Tournament::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { logAndRethrow(it, OperationType.LIST, "tournaments", adminUid) }

    fun observeAllBanners(adminUid: String): Flow<List<PromoBanner>> =
        db.collection("banners").snapshots()
            .map { snap ->
                snap.toObjects(PromoBanner::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.displayOrder }
            }
            .catch { logAndRethrow(it, OperationType.LIST, "banners", adminUid) }

    fun observeAllAnnouncements(adminUid: String): Flow<List<Announcement>> =
        db.collection("announcements").snapshots()
            .map { snap ->
                snap.toObjects(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.priority }
            }
            .catch { logAndRethrow(it, OperationType.LIST, "announcements", adminUid) }

    fun observeAllRegistrations(adminUid: String): Flow<List<Registration>> =
        db.collection("registrations").snapshots()
            .map { snap ->
                snap.toObjects(Registration::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { logAndRethrow(it, OperationType.LIST, "registrations", adminUid) }

    fun observeAllWallets(adminUid: String): Flow<List<Wallet>> =
        db.collection("wallets").snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    val w = doc.toObject(Wallet::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (w != null && !doc.contains("bonusBalance") && w.earningsBalance > 0.0) {
                        w.copy(bonusBalance = w.earningsBalance)
                    } else {
                        w
                    }
                }
            }
            .catch { logAndRethrow(it, OperationType.LIST, "wallets", adminUid) }

    fun observeWalletConfig(adminUid: String): Flow<WalletConfig> =
        db.collection("appConfig").document("wallet").snapshots()
            .map { snap ->
                snap.toObject(WalletConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: WalletConfig(id = "wallet")
            }
            .catch { logAndRethrow(it, OperationType.GET, "appConfig/wallet", adminUid) }

    fun observeAllPayments(adminUid: String): Flow<List<PaymentOrder>> =
        db.collection("payments").snapshots()
            .map { snap ->
                snap.toObjects(PaymentOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { logAndRethrow(it, OperationType.LIST, "payments", adminUid) }

    fun observeAllTransactions(adminUid: String): Flow<List<WalletTransaction>> =
        db.collection("transactions").snapshots()
            .map { snap ->
                snap.toObjects(WalletTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { logAndRethrow(it, OperationType.LIST, "transactions", adminUid) }

    fun observeAllPayouts(adminUid: String): Flow<List<PayoutRequest>> =
        db.collection("payoutRequests").snapshots()
            .map { snap ->
                snap.toObjects(PayoutRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { logAndRethrow(it, OperationType.LIST, "payoutRequests", adminUid) }

    fun observeAllTickets(adminUid: String): Flow<List<SupportTicket>> =
        db.collection("supportTickets").snapshots()
            .map { snap ->
                snap.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { logAndRethrow(it, OperationType.LIST, "supportTickets", adminUid) }

    fun observeAllDisputes(adminUid: String): Flow<List<Dispute>> =
        db.collection("disputes").snapshots()
            .map { snap ->
                snap.toObjects(Dispute::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { logAndRethrow(it, OperationType.LIST, "disputes", adminUid) }

    fun observeAuditLogs(adminUid: String): Flow<List<AuditLog>> =
        db.collection("auditLogs").snapshots()
            .map { snap ->
                snap.toObjects(AuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { logAndRethrow(it, OperationType.LIST, "auditLogs", adminUid) }

    fun observeAllReferrals(adminUid: String): Flow<List<ReferralRecord>> =
        db.collection("referrals").snapshots()
            .map { snap ->
                snap.toObjects(ReferralRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { logAndRethrow(it, OperationType.LIST, "referrals", adminUid) }

    fun observeAllAdminUsers(adminUid: String): Flow<List<AdminUserRecord>> =
        db.collection("adminUsers").snapshots()
            .map { snap ->
                snap.toObjects(AdminUserRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { logAndRethrow(it, OperationType.LIST, "adminUsers", adminUid) }

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

        val typeRef = db.collection("customTournamentTypes").document(typeId)
        val typePayload = mutableMapOf<String, Any>(
            "displayName" to displayName.trim().take(60),
            "bannerUrl" to bannerUrl.take(200000),
            "shortDescription" to shortDescription.trim().take(300),
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

    suspend fun saveTournament(
        existingId: String?,
        categoryId: String,
        title: String,
        shortDescription: String,
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
        val safeDeadline = registrationDeadline.trim().take(60).ifBlank { "$matchDate $matchTime".trim() }
        val safeRules = rules.trim().take(4000).ifBlank { "Fair-play rules enforced. Zero hacks or teaming." }
        val safeVisibility = if (visibility in listOf("PUBLIC", "HIDDEN")) visibility else "PUBLIC"
        val safeStatus = if (status in listOf("UPCOMING", "LIVE", "FULL", "COMPLETED", "CANCELLED", "ARCHIVED")) status else "UPCOMING"
        val data = mutableMapOf<String, Any>(
            "categoryId" to categoryId,
            "title" to title.trim().take(80),
            "shortDescription" to safeDesc,
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
        require(amountDelta > 0.0) { "Amount must be greater than 0" }
        val normalizedBucket = bucket.uppercase(Locale.US).let {
            if (it in listOf("WINNING", "BONUS", "DEPOSIT")) it else "BONUS"
        }
        val walletRef = db.collection("wallets").document(targetUid)
        val txRef = db.collection("transactions").document()
        val notifRef = db.collection("notifications").document()
        val auditRef = db.collection("auditLogs").document()

        db.runTransaction { tx ->
            val snap = tx.get(walletRef)
            val currentDep = snap.getDouble("depositedBalance") ?: 0.0
            val currentWin = snap.getDouble("winningBalance") ?: 0.0
            val currentBonus = if (snap.contains("bonusBalance")) {
                snap.getDouble("bonusBalance") ?: 0.0
            } else {
                snap.getDouble("earningsBalance") ?: 0.0
            }
            val currentPayout = snap.getDouble("payoutsTotal") ?: 0.0

            val signedDelta = if (direction == "CREDIT") amountDelta else -amountDelta
            val nextDep = if (normalizedBucket == "DEPOSIT") (currentDep + signedDelta).coerceAtLeast(0.0) else currentDep
            val nextWin = if (normalizedBucket == "WINNING") (currentWin + signedDelta).coerceAtLeast(0.0) else currentWin
            val nextBonus = if (normalizedBucket == "BONUS") (currentBonus + signedDelta).coerceAtLeast(0.0) else currentBonus
            val nextTotal = nextDep + nextWin + nextBonus
            val now = FieldValue.serverTimestamp()

            if (!snap.exists()) {
                tx.set(
                    walletRef,
                    mapOf(
                        "userId" to targetUid,
                        "totalBalance" to nextTotal,
                        "depositedBalance" to nextDep,
                        "winningBalance" to nextWin,
                        "bonusBalance" to nextBonus,
                        "earningsBalance" to nextBonus,
                        "payoutsTotal" to currentPayout,
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                )
            } else {
                tx.update(
                    walletRef,
                    mapOf(
                        "totalBalance" to nextTotal,
                        "depositedBalance" to nextDep,
                        "winningBalance" to nextWin,
                        "bonusBalance" to nextBonus,
                        "earningsBalance" to nextBonus,
                        "updatedAt" to now
                    )
                )
            }

            val txType = when {
                direction == "DEBIT" -> "ADMIN_ADJUSTMENT"
                normalizedBucket == "WINNING" -> "PRIZE"
                normalizedBucket == "BONUS" -> "BONUS"
                else -> "DEPOSIT"
            }
            val descPrefix = "$normalizedBucket ${direction.lowercase(Locale.US)}"
            tx.set(
                txRef,
                mapOf(
                    "userId" to targetUid,
                    "type" to txType,
                    "direction" to direction,
                    "description" to "${reason.trim().ifBlank { "Admin $descPrefix" }} [$normalizedBucket]".take(200),
                    "matchId" to "",
                    "amount" to amountDelta,
                    "status" to "COMPLETED",
                    "resultingBalance" to nextTotal,
                    "createdAt" to now
                )
            )
            tx.set(
                notifRef,
                mapOf(
                    "userId" to targetUid,
                    "title" to "Wallet Updated ($normalizedBucket)",
                    "message" to "${if (direction == "CREDIT") "+" else "-"}${String.format(Locale.US, "%.0f", amountDelta)} Coins in your $normalizedBucket balance. ${reason.trim()}".take(400),
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
                    "action" to "WALLET_${normalizedBucket}_$direction",
                    "targetCollection" to "wallets",
                    "targetId" to targetUid,
                    "summary" to "Adjusted $targetUid [$normalizedBucket] $direction $amountDelta ($reason)".take(400),
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
                val currentDep = walletSnap.getDouble("depositedBalance") ?: 0.0
                val currentWin = walletSnap.getDouble("winningBalance") ?: 0.0
                val currentBonus = if (walletSnap.contains("bonusBalance")) {
                    walletSnap.getDouble("bonusBalance") ?: 0.0
                } else {
                    walletSnap.getDouble("earningsBalance") ?: 0.0
                }
                val currentPayout = walletSnap.getDouble("payoutsTotal") ?: 0.0
                val nextDep = currentDep + payment.coinAmount
                val nextTotal = nextDep + currentWin + currentBonus

                if (!walletSnap.exists()) {
                    tx.set(
                        walletRef,
                        mapOf(
                            "userId" to payment.userId,
                            "totalBalance" to nextTotal,
                            "depositedBalance" to nextDep,
                            "winningBalance" to currentWin,
                            "bonusBalance" to currentBonus,
                            "earningsBalance" to currentBonus,
                            "payoutsTotal" to currentPayout,
                            "createdAt" to now,
                            "updatedAt" to now
                        )
                    )
                } else {
                    tx.update(
                        walletRef,
                        mapOf(
                            "totalBalance" to nextTotal,
                            "depositedBalance" to nextDep,
                            "bonusBalance" to currentBonus,
                            "updatedAt" to now
                        )
                    )
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
                        "amount" to payment.coinAmount,
                        "status" to "COMPLETED",
                        "resultingBalance" to nextTotal,
                        "createdAt" to now
                    )
                )
                tx.set(
                    notifRef,
                    mapOf(
                        "userId" to payment.userId,
                        "title" to "Deposit Approved",
                        "message" to "+${payment.coinAmount.toInt()} Coins credited to your Deposit balance.",
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
        val notifRef = db.collection("notifications").document("notif_out_${payout.id}")
        val auditRef = db.collection("auditLogs").document()

        db.runTransaction { tx ->
            val payoutSnap = tx.get(payoutRef)
            if (!payoutSnap.exists()) throw IllegalStateException("Withdrawal request not found.")
            if (payoutSnap.getString("status") != "PENDING") {
                throw IllegalStateException("Withdrawal request already processed.")
            }
            val walletSnap = tx.get(walletRef)
            val now = FieldValue.serverTimestamp()

            if (approve) {
                require(payout.amount >= 50.0) { "Minimum withdrawal is 50 Coins." }
                val currentDep = walletSnap.getDouble("depositedBalance") ?: 0.0
                val currentWin = walletSnap.getDouble("winningBalance") ?: 0.0
                val currentBonus = if (walletSnap.contains("bonusBalance")) {
                    walletSnap.getDouble("bonusBalance") ?: 0.0
                } else {
                    walletSnap.getDouble("earningsBalance") ?: 0.0
                }
                val currentPayout = walletSnap.getDouble("payoutsTotal") ?: 0.0
                if (currentWin < payout.amount) {
                    throw IllegalStateException("Insufficient Winning balance (${currentWin.toInt()} Coins) for ${payout.amount.toInt()} Coins payout.")
                }
                val nextWin = currentWin - payout.amount
                val nextPayout = currentPayout + payout.amount
                val nextTotal = currentDep + nextWin + currentBonus

                tx.update(
                    walletRef,
                    mapOf(
                        "totalBalance" to nextTotal,
                        "winningBalance" to nextWin,
                        "payoutsTotal" to nextPayout,
                        "updatedAt" to now
                    )
                )
                tx.update(
                    payoutRef,
                    mapOf("status" to "PAID", "reviewedBy" to adminEmail, "updatedAt" to now)
                )
                tx.set(
                    txRef,
                    mapOf(
                        "userId" to payout.userId,
                        "type" to "PAYOUT",
                        "direction" to "DEBIT",
                        "description" to "Winning payout paid via ${payout.destinationType} (${payout.destinationDetails})".take(200),
                        "matchId" to "",
                        "amount" to payout.amount,
                        "status" to "COMPLETED",
                        "resultingBalance" to nextTotal,
                        "createdAt" to now
                    )
                )
                tx.set(
                    notifRef,
                    mapOf(
                        "userId" to payout.userId,
                        "title" to "Withdrawal Paid",
                        "message" to "₹${payout.amount.toInt()} (${payout.amount.toInt()} Winning Coins) sent to ${payout.destinationDetails.ifBlank { payout.destinationType }}.",
                        "type" to "PAYOUT",
                        "read" to false,
                        "createdAt" to now
                    )
                )
            } else {
                tx.update(
                    payoutRef,
                    mapOf("status" to "REJECTED", "reviewedBy" to adminEmail, "updatedAt" to now)
                )
                tx.set(
                    notifRef,
                    mapOf(
                        "userId" to payout.userId,
                        "title" to "Withdrawal Rejected",
                        "message" to "Your ${payout.amount.toInt()} Coins withdrawal was rejected. ${note.trim()}".take(400),
                        "type" to "PAYOUT",
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
                    "summary" to "${if (approve) "Paid" else "Rejected"} withdrawal ${payout.amount.toInt()} Coins for ${payout.userId}".take(400),
                    "createdAt" to now
                )
            )
        }.await()
    }

    suspend fun verifyMatchResultAndRank(
        tournamentId: String,
        tournamentTitle: String,
        targetUser: UserProfile,
        placement: Long,
        kills: Long,
        rewardCoins: Double,
        leaderboardMode: String
    ): Result<Unit> = runAdminMutation("results") { uid, email ->
        val resId = "${tournamentId}_${targetUser.userId}"
        val resRef = db.collection("results").document(resId)
        val userRef = db.collection("users").document(targetUser.userId)
        val walletRef = db.collection("wallets").document(targetUser.userId)
        val txRef = db.collection("transactions").document("tx_prize_$resId")
        val notifRef = db.collection("notifications").document("notif_prize_$resId")
        val auditRef = db.collection("auditLogs").document()

        db.runTransaction { tx ->
            val existingRes = tx.get(resRef)
            val walletSnap = tx.get(walletRef)
            val now = FieldValue.serverTimestamp()
            val pointsEarned = (kills * 10L) + when (placement) {
                1L -> 100L
                2L -> 60L
                3L -> 40L
                else -> 15L
            }

            tx.set(
                resRef,
                mapOf(
                    "tournamentId" to tournamentId,
                    "tournamentTitle" to tournamentTitle,
                    "userId" to targetUser.userId,
                    "username" to targetUser.username,
                    "placement" to placement,
                    "kills" to kills,
                    "rewardCoins" to rewardCoins,
                    "verified" to true,
                    "declaredBy" to email,
                    "createdAt" to (existingRes.getTimestamp("createdAt") ?: now),
                    "updatedAt" to now
                )
            )
            val newMatches = targetUser.matchesPlayed + 1L
            val newWins = targetUser.wins + if (placement == 1L) 1L else 0L
            val newKills = targetUser.verifiedKills + kills
            val newPoints = targetUser.rankingPoints + pointsEarned

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

            listOf("OVERALL", leaderboardMode).distinct().forEach { modeKey ->
                tx.set(
                    db.collection("leaderboards").document("${modeKey}_${targetUser.userId}"),
                    mapOf(
                        "userId" to targetUser.userId,
                        "username" to targetUser.username,
                        "photoUrl" to targetUser.photoUrl,
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

            if (rewardCoins > 0.0 && !existingRes.exists()) {
                val currentDep = walletSnap.getDouble("depositedBalance") ?: 0.0
                val currentWin = walletSnap.getDouble("winningBalance") ?: 0.0
                val currentBonus = if (walletSnap.contains("bonusBalance")) {
                    walletSnap.getDouble("bonusBalance") ?: 0.0
                } else {
                    walletSnap.getDouble("earningsBalance") ?: 0.0
                }
                val currentPayout = walletSnap.getDouble("payoutsTotal") ?: 0.0
                val nextWin = currentWin + rewardCoins
                val nextTotal = currentDep + nextWin + currentBonus

                if (!walletSnap.exists()) {
                    tx.set(
                        walletRef,
                        mapOf(
                            "userId" to targetUser.userId,
                            "totalBalance" to nextTotal,
                            "depositedBalance" to currentDep,
                            "winningBalance" to nextWin,
                            "bonusBalance" to currentBonus,
                            "earningsBalance" to currentBonus,
                            "payoutsTotal" to currentPayout,
                            "createdAt" to now,
                            "updatedAt" to now
                        )
                    )
                } else {
                    tx.update(
                        walletRef,
                        mapOf(
                            "totalBalance" to nextTotal,
                            "winningBalance" to nextWin,
                            "bonusBalance" to currentBonus,
                            "updatedAt" to now
                        )
                    )
                }
                tx.set(
                    txRef,
                    mapOf(
                        "userId" to targetUser.userId,
                        "type" to "PRIZE",
                        "direction" to "CREDIT",
                        "description" to "Prize for #$placement in $tournamentTitle ($kills kills)".take(200),
                        "matchId" to tournamentId,
                        "amount" to rewardCoins,
                        "status" to "COMPLETED",
                        "resultingBalance" to nextTotal,
                        "createdAt" to now
                    )
                )
                tx.set(
                    notifRef,
                    mapOf(
                        "userId" to targetUser.userId,
                        "title" to "Winning Prize Credited!",
                        "message" to "+${rewardCoins.toInt()} Winning Coins added for #$placement in $tournamentTitle.",
                        "type" to "PRIZE",
                        "read" to false,
                        "createdAt" to now
                    )
                )
            }

            tx.set(
                auditRef,
                mapOf(
                    "actorId" to uid,
                    "actorEmail" to email,
                    "action" to "VERIFY_RESULT",
                    "targetCollection" to "results",
                    "targetId" to resId,
                    "summary" to "Verified #$placement ($kills kills, +$rewardCoins Winning Coins) for ${targetUser.username}".take(400),
                    "createdAt" to now
                )
            )
        }.await()
    }

    suspend fun saveAnnouncementOrSupportLinks(
        id: String,
        title: String,
        body: String,
        category: String,
        telegramUrl: String,
        whatsappUrl: String
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

            val currentDep = walletSnap.getDouble("depositedBalance") ?: 0.0
            val currentWin = walletSnap.getDouble("winningBalance") ?: 0.0
            val currentBonus = if (walletSnap.contains("bonusBalance")) {
                walletSnap.getDouble("bonusBalance") ?: 0.0
            } else {
                walletSnap.getDouble("earningsBalance") ?: 0.0
            }
            val currentPayout = walletSnap.getDouble("payoutsTotal") ?: 0.0
            val nextBonus = currentBonus + bonusCoins
            val nextTotal = currentDep + currentWin + nextBonus
            val now = FieldValue.serverTimestamp()

            if (!walletSnap.exists()) {
                tx.set(
                    walletRef,
                    mapOf(
                        "userId" to referral.referrerUid,
                        "totalBalance" to nextTotal,
                        "depositedBalance" to currentDep,
                        "winningBalance" to currentWin,
                        "bonusBalance" to nextBonus,
                        "earningsBalance" to nextBonus,
                        "payoutsTotal" to currentPayout,
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                )
            } else {
                tx.update(
                    walletRef,
                    mapOf(
                        "totalBalance" to nextTotal,
                        "bonusBalance" to nextBonus,
                        "earningsBalance" to nextBonus,
                        "updatedAt" to now
                    )
                )
            }

            tx.set(
                txRef,
                mapOf(
                    "userId" to referral.referrerUid,
                    "type" to "REFERRAL_BONUS",
                    "direction" to "CREDIT",
                    "description" to "Referral bonus for inviting ${referral.referredUsername}".take(200),
                    "matchId" to "",
                    "amount" to bonusCoins,
                    "status" to "COMPLETED",
                    "resultingBalance" to nextTotal,
                    "createdAt" to now
                )
            )

            tx.update(
                referralRef,
                mapOf(
                    "status" to "CREDITED",
                    "bonusAmount" to bonusCoins,
                    "transactionId" to txRef.id,
                    "updatedAt" to now
                )
            )

            tx.set(
                notifRef,
                mapOf(
                    "userId" to referral.referrerUid,
                    "title" to "Referral Bonus Credited",
                    "message" to "+${String.format(Locale.US, "%.0f", bonusCoins)} Coins credited for referring ${referral.referredUsername}.",
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
                    "summary" to "Credited $bonusCoins Coins to ${referral.referrerUsername} for referring ${referral.referredUsername}".take(400),
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
