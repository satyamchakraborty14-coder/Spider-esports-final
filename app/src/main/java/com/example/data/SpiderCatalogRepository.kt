package com.example.data

import com.example.model.Announcement
import com.example.model.LeaderboardEntry
import com.example.model.MatchRecord
import com.example.model.MatchResult
import com.example.model.PromoBanner
import com.example.model.Registration
import com.example.model.RoomCredential
import com.example.model.Tournament
import com.example.model.TournamentCategory
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SpiderCatalogRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in with Google.")

    fun observeEnabledCategories(userId: String): Flow<List<TournamentCategory>> =
        db.collection("tournamentCategories")
            .whereEqualTo("enabled", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(TournamentCategory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.displayOrder }
            }
            .catch { logAndRethrow(it, OperationType.LIST, "tournamentCategories", userId) }

    fun observeEnabledBanners(userId: String): Flow<List<PromoBanner>> =
        db.collection("banners")
            .whereEqualTo("enabled", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(PromoBanner::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.displayOrder }
            }
            .catch { logAndRethrow(it, OperationType.LIST, "banners", userId) }

    fun observeEnabledAnnouncements(userId: String): Flow<List<Announcement>> =
        db.collection("announcements")
            .whereEqualTo("enabled", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.priority }
            }
            .catch { logAndRethrow(it, OperationType.LIST, "announcements", userId) }

    fun observePublicTournaments(userId: String): Flow<List<Tournament>> =
        db.collection("tournaments")
            .whereEqualTo("visibility", "PUBLIC")
            .snapshots()
            .map { snap ->
                snap.toObjects(Tournament::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { it.status != "ARCHIVED" }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { logAndRethrow(it, OperationType.LIST, "tournaments", userId) }

    fun observeUserRegistrations(userId: String): Flow<List<Registration>> =
        db.collection("registrations")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(Registration::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { logAndRethrow(it, OperationType.LIST, "registrations", userId) }

    fun observeUserMatches(userId: String): Flow<List<MatchRecord>> =
        db.collection("matches")
            .whereArrayContains("participantUids", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(MatchRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt?.seconds ?: 0L }
            }
            .catch { logAndRethrow(it, OperationType.LIST, "matches", userId) }

    fun observeVerifiedResults(userId: String): Flow<List<MatchResult>> =
        db.collection("results")
            .whereEqualTo("verified", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(MatchResult::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { logAndRethrow(it, OperationType.LIST, "results", userId) }

    fun observeVerifiedLeaderboards(userId: String): Flow<List<LeaderboardEntry>> =
        db.collection("leaderboards")
            .whereEqualTo("verified", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(LeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.rankingPoints }
            }
            .catch { logAndRethrow(it, OperationType.LIST, "leaderboards", userId) }

    suspend fun fetchRoomCredential(tournamentId: String): Result<RoomCredential?> {
        val path = "tournaments/$tournamentId/credentials/room"
        return try {
            requireUserId()
            val snap = db.collection("tournaments").document(tournamentId)
                .collection("credentials").document("room").get().await()
            val cred = snap.toObject(RoomCredential::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            Result.success(cred)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun registerForFreeTournament(
        tournament: Tournament,
        username: String,
        inGameName: String,
        inGameUid: String
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val regId = "${tournament.id}_$uid"
        val regPath = "registrations/$regId"
        return try {
            if (tournament.entryFee > 0.0) {
                return Result.failure(
                    IllegalStateException("Paid-entry tournaments remain disabled until legal, age, and payment gateway approvals are completed.")
                )
            }
            if (tournament.status !in listOf("UPCOMING", "LIVE")) {
                return Result.failure(IllegalStateException("Tournament registration is closed."))
            }
            if (tournament.registeredCount >= tournament.maxParticipants) {
                return Result.failure(IllegalStateException("Tournament slots are full."))
            }
            require(inGameName.trim().isNotEmpty() && inGameUid.trim().isNotEmpty()) {
                "In-game Name and In-game UID are required."
            }

            val regRef = db.collection("registrations").document(regId)
            val tourRef = db.collection("tournaments").document(tournament.id)
            db.runTransaction { tx ->
                val existingReg = tx.get(regRef)
                val tourSnap = tx.get(tourRef)
                if (existingReg.exists()) {
                    throw IllegalStateException("You are already registered for this match.")
                }
                val liveStatus = tourSnap.getString("status") ?: tournament.status
                val liveFee = tourSnap.getDouble("entryFee") ?: tournament.entryFee
                if (liveFee > 0.0) {
                    throw IllegalStateException("Paid-entry tournaments remain disabled until legal, age, and payment gateway approvals are completed.")
                }
                if (liveStatus !in listOf("UPCOMING", "LIVE")) {
                    throw IllegalStateException("Tournament registration is closed.")
                }
                val liveCount = tourSnap.getLong("registeredCount") ?: 0L
                val maxCap = tourSnap.getLong("maxParticipants") ?: tournament.maxParticipants
                if (liveCount >= maxCap) {
                    throw IllegalStateException("Tournament just filled up.")
                }

                val now = FieldValue.serverTimestamp()
                tx.set(
                    regRef,
                    mapOf(
                        "tournamentId" to tournament.id,
                        "tournamentTitle" to tournament.title.take(80),
                        "categoryId" to tournament.categoryId,
                        "userId" to uid,
                        "username" to username.trim().take(32).let { if (it.length < 2) "Player" else it },
                        "inGameName" to inGameName.trim().take(40),
                        "inGameUid" to inGameUid.trim().take(40),
                        "entryFeePaid" to 0.0,
                        "status" to "CONFIRMED",
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                )
                tx.update(
                    tourRef,
                    mapOf(
                        "registeredCount" to liveCount + 1L,
                        "updatedAt" to now
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, regPath)
            Result.failure(e)
        }
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
