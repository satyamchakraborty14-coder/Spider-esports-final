package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthHelper
import com.example.data.SpiderAdminRepository
import com.example.data.SpiderCatalogSeeder
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
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SpiderAdminViewModel(
    private val db: FirebaseFirestore,
    private val adminUid: String,
    private val adminRepo: SpiderAdminRepository
) : ViewModel() {

    private val _statusMessage = MutableStateFlow<Pair<Boolean, String>?>(null)
    val statusMessage: StateFlow<Pair<Boolean, String>?> = _statusMessage.asStateFlow()

    val users: StateFlow<List<UserProfile>> = adminRepo.observeAllUsers(adminUid).safeList("users")
    val categories: StateFlow<List<TournamentCategory>> = adminRepo.observeAllCategories(adminUid).safeList("categories")
    val customTypes: StateFlow<List<CustomTournamentType>> = adminRepo.observeAllCustomTypes(adminUid).safeList("customTypes")
    val tournaments: StateFlow<List<Tournament>> = adminRepo.observeAllTournaments(adminUid).safeList("tournaments")
    val banners: StateFlow<List<PromoBanner>> = adminRepo.observeAllBanners(adminUid).safeList("banners")
    val announcements: StateFlow<List<Announcement>> = adminRepo.observeAllAnnouncements(adminUid).safeList("announcements")
    val registrations: StateFlow<List<Registration>> = adminRepo.observeAllRegistrations(adminUid).safeList("registrations")
    val wallets: StateFlow<List<Wallet>> = adminRepo.observeAllWallets(adminUid).safeList("wallets")
    val walletConfig: StateFlow<WalletConfig> = adminRepo.observeWalletConfig(adminUid)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w("SpiderAdminVM", "Error observing walletConfig", firestoreError ?: error)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), WalletConfig(id = "wallet"))
    val payments: StateFlow<List<PaymentOrder>> = adminRepo.observeAllPayments(adminUid).safeList("payments")
    val transactions: StateFlow<List<WalletTransaction>> = adminRepo.observeAllTransactions(adminUid).safeList("transactions")
    val payouts: StateFlow<List<PayoutRequest>> = adminRepo.observeAllPayouts(adminUid).safeList("payouts")
    val tickets: StateFlow<List<SupportTicket>> = adminRepo.observeAllTickets(adminUid).safeList("tickets")
    val disputes: StateFlow<List<Dispute>> = adminRepo.observeAllDisputes(adminUid).safeList("disputes")
    val auditLogs: StateFlow<List<AuditLog>> = adminRepo.observeAuditLogs(adminUid).safeList("auditLogs")
    val referrals: StateFlow<List<ReferralRecord>> = adminRepo.observeAllReferrals(adminUid).safeList("referrals")
    val adminUsers: StateFlow<List<AdminUserRecord>> = adminRepo.observeAllAdminUsers(adminUid).safeList("adminUsers")

    fun clearStatus() {
        _statusMessage.value = null
    }

    fun seedDefaultCatalog() {
        viewModelScope.launch {
            val email = Firebase.auth.currentUser?.email ?: AuthHelper.BOOTSTRAP_ADMIN_EMAIL
            SpiderCatalogSeeder.seedDefaultCatalogIfEmpty(db, adminUid, email, forceCheck = true)
                .onSuccess { _statusMessage.value = true to "Default categories, banners, rules & starter matches synced." }
                .onFailure { _statusMessage.value = false to (it.localizedMessage ?: "Seed failed") }
        }
    }

    fun saveCategory(
        existingId: String?,
        name: String,
        imageUrl: String,
        categoryType: String,
        presetKey: String,
        enabled: Boolean,
        displayOrder: Long
    ) {
        viewModelScope.launch {
            adminRepo.saveCategory(existingId, name, imageUrl, categoryType, presetKey, enabled, displayOrder)
                .onSuccess { _statusMessage.value = true to "Category '$name' saved & synced live." }
                .onFailure { _statusMessage.value = false to (it.localizedMessage ?: "Failed to save category") }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            adminRepo.deleteCategory(categoryId)
                .onSuccess { _statusMessage.value = true to "Category deleted." }
                .onFailure { _statusMessage.value = false to (it.localizedMessage ?: "Delete failed") }
        }
    }

    fun saveRandomTournament(
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
    ) {
        viewModelScope.launch {
            adminRepo.saveRandomTournament(
                existingTypeId, existingCategoryId, displayName, bannerUrl, shortDescription,
                gameMode, mapName, entryFee, prizePool, perKillReward, maxParticipants,
                matchDate, matchTime, registrationDeadline, rules, visibility, status,
                displayOrder, enabled, archived
            ).onSuccess {
                _statusMessage.value = true to "Random Tournament '$displayName' published dynamically!"
            }.onFailure {
                _statusMessage.value = false to (it.localizedMessage ?: "Failed to save Random Tournament")
            }
        }
    }

    fun deleteRandomTournament(type: CustomTournamentType) {
        viewModelScope.launch {
            adminRepo.deleteRandomTournament(type)
                .onSuccess { _statusMessage.value = true to "Random Tournament archived/deleted." }
                .onFailure { _statusMessage.value = false to (it.localizedMessage ?: "Delete failed") }
        }
    }

    fun saveBanner(
        existingId: String?,
        title: String,
        subtitle: String,
        imageUrl: String,
        presetKey: String,
        targetAction: String,
        displayOrder: Long,
        enabled: Boolean
    ) {
        viewModelScope.launch {
            adminRepo.saveBanner(existingId, title, subtitle, imageUrl, presetKey, targetAction, displayOrder, enabled)
                .onSuccess { _statusMessage.value = true to "Banner '$title' saved." }
                .onFailure { _statusMessage.value = false to (it.localizedMessage ?: "Banner save failed") }
        }
    }

    fun deleteBanner(bannerId: String) {
        viewModelScope.launch {
            adminRepo.deleteBanner(bannerId)
                .onSuccess { _statusMessage.value = true to "Banner deleted." }
                .onFailure { _statusMessage.value = false to (it.localizedMessage ?: "Banner delete failed") }
        }
    }

    fun saveTournament(
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
    ) {
        viewModelScope.launch {
            adminRepo.saveTournament(
                existingId, categoryId, title, shortDescription, gameMode, mapName,
                entryFee, prizePool, perKillReward, maxParticipants,
                matchDate, matchTime, registrationDeadline, rules,
                bannerUrl, presetKey, visibility, status, soloFormat
            ).onSuccess { _statusMessage.value = true to "Tournament '$title' saved." }
                .onFailure { _statusMessage.value = false to (it.localizedMessage ?: "Failed to save tournament") }
        }
    }

    fun deleteTournament(tournament: Tournament) {
        viewModelScope.launch {
            adminRepo.deleteTournament(tournament)
                .onSuccess { _statusMessage.value = true to "Tournament '${tournament.title}' deleted." }
                .onFailure { _statusMessage.value = false to (it.localizedMessage ?: "Failed to delete tournament") }
        }
    }

    fun publishRoomCredentials(tournament: Tournament, roomId: String, roomPassword: String) {
        viewModelScope.launch {
            adminRepo.publishRoomCredentials(tournament, roomId, roomPassword)
                .onSuccess { _statusMessage.value = true to "Room credentials released to registered players." }
                .onFailure { _statusMessage.value = false to (it.localizedMessage ?: "Failed to release credentials") }
        }
    }

    fun setUserStatus(targetUid: String, newStatus: String) {
        viewModelScope.launch {
            adminRepo.setUserStatus(targetUid, newStatus)
                .onSuccess { _statusMessage.value = true to "User status set to $newStatus." }
                .onFailure { _statusMessage.value = false to (it.localizedMessage ?: "Failed to update user") }
        }
    }

    fun saveWalletQrConfig(qrImageUrl: String, upiId: String, instructions: String, minWithdraw: Double = 50.0) {
        viewModelScope.launch {
            adminRepo.saveWalletQrConfig(qrImageUrl, upiId, instructions, minWithdraw)
                .onSuccess { _statusMessage.value = true to "Payment QR & withdrawal settings saved." }
                .onFailure { _statusMessage.value = false to (it.localizedMessage ?: "Failed to save QR settings") }
        }
    }

    fun adjustWallet(targetUid: String, amount: Double, direction: String, bucket: String, reason: String) {
        viewModelScope.launch {
            adminRepo.adjustUserWallet(targetUid, amount, direction, bucket, reason)
                .onSuccess { _statusMessage.value = true to "Wallet [$bucket] $direction of ${amount.toInt()} Coins logged." }
                .onFailure { _statusMessage.value = false to (it.localizedMessage ?: "Wallet adjustment failed") }
        }
    }

    fun reviewDeposit(payment: PaymentOrder, approve: Boolean, note: String = "") {
        viewModelScope.launch {
            adminRepo.reviewDepositRequest(payment, approve, note)
                .onSuccess {
                    _statusMessage.value = true to if (approve) {
                        "Approved deposit +${payment.coinAmount.toInt()} Coins to Deposit balance."
                    } else {
                        "Deposit request marked REJECTED."
                    }
                }
                .onFailure { _statusMessage.value = false to (it.localizedMessage ?: "Deposit review failed") }
        }
    }

    fun reviewPayout(payout: PayoutRequest, approve: Boolean, note: String = "") {
        viewModelScope.launch {
            adminRepo.reviewPayoutRequest(payout, approve, note)
                .onSuccess {
                    _statusMessage.value = true to if (approve) {
                        "Withdrawal ₹${payout.amount.toInt()} marked PAID from Winning balance."
                    } else {
                        "Withdrawal request marked REJECTED."
                    }
                }
                .onFailure { _statusMessage.value = false to (it.localizedMessage ?: "Withdrawal review failed") }
        }
    }

    fun verifyResult(
        tournamentId: String,
        tournamentTitle: String,
        user: UserProfile,
        placement: Long,
        kills: Long,
        rewardCoins: Double,
        leaderboardMode: String
    ) {
        viewModelScope.launch {
            adminRepo.verifyMatchResultAndRank(
                tournamentId, tournamentTitle, user, placement, kills, rewardCoins, leaderboardMode
            ).onSuccess {
                _statusMessage.value = true to "Verified result & ranking points for ${user.username}."
            }.onFailure {
                _statusMessage.value = false to (it.localizedMessage ?: "Verification failed")
            }
        }
    }

    fun saveAnnouncementConfig(
        id: String,
        title: String,
        body: String,
        category: String,
        telegramUrl: String,
        whatsappUrl: String
    ) {
        viewModelScope.launch {
            adminRepo.saveAnnouncementOrSupportLinks(id, title, body, category, telegramUrl, whatsappUrl)
                .onSuccess { _statusMessage.value = true to "Announcement/config updated." }
                .onFailure { _statusMessage.value = false to (it.localizedMessage ?: "Update failed") }
        }
    }

    fun replyTicket(ticketId: String, status: String, reply: String) {
        viewModelScope.launch {
            adminRepo.replySupportTicket(ticketId, status, reply)
                .onSuccess { _statusMessage.value = true to "Ticket updated." }
                .onFailure { _statusMessage.value = false to (it.localizedMessage ?: "Failed to reply") }
        }
    }

    fun resolveDispute(disputeId: String, status: String, notes: String) {
        viewModelScope.launch {
            adminRepo.resolveDispute(disputeId, status, notes)
                .onSuccess { _statusMessage.value = true to "Dispute $status." }
                .onFailure { _statusMessage.value = false to (it.localizedMessage ?: "Failed to resolve dispute") }
        }
    }

    fun approveReferralBonus(referral: ReferralRecord, bonusCoins: Double) {
        viewModelScope.launch {
            adminRepo.approveAndCreditReferralBonus(referral, bonusCoins)
                .onSuccess {
                    _statusMessage.value = true to "Credited ${bonusCoins.toInt()} Coins referral bonus to ${referral.referrerUsername}."
                }
                .onFailure {
                    _statusMessage.value = false to (it.localizedMessage ?: "Referral bonus approval failed")
                }
        }
    }

    fun rejectReferral(referral: ReferralRecord, reason: String) {
        viewModelScope.launch {
            adminRepo.rejectReferral(referral, reason)
                .onSuccess { _statusMessage.value = true to "Referral marked REJECTED." }
                .onFailure { _statusMessage.value = false to (it.localizedMessage ?: "Failed to reject referral") }
        }
    }

    fun setAdminAccess(targetUser: UserProfile, grant: Boolean) {
        viewModelScope.launch {
            adminRepo.grantOrRevokeAdminAccess(targetUser, grant)
                .onSuccess {
                    _statusMessage.value = true to if (grant) {
                        "Granted Admin role to ${targetUser.username}."
                    } else {
                        "Revoked Admin role from ${targetUser.username}."
                    }
                }
                .onFailure {
                    _statusMessage.value = false to (it.localizedMessage ?: "Admin access update failed")
                }
        }
    }

    private fun <T> Flow<List<T>>.safeList(label: String): StateFlow<List<T>> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w("SpiderAdminVM", "Error observing $label", firestoreError ?: error)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
}
