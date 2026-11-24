package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthHelper
import com.example.data.SpiderAdminRepository
import com.example.data.SpiderCatalogRepository
import com.example.data.SpiderCatalogSeeder
import com.example.data.SpiderUserRepository
import com.example.model.Announcement
import com.example.model.CoinPackage
import com.example.model.Dispute
import com.example.model.LeaderboardEntry
import com.example.model.MatchRecord
import com.example.model.MatchResult
import com.example.model.NotificationItem
import com.example.model.PaymentOrder
import com.example.model.PayoutRequest
import com.example.model.PlayGameTab
import com.example.model.PromoBanner
import com.example.model.ReferralRecord
import com.example.model.Registration
import com.example.model.RoomCredential
import com.example.model.SupportTicket
import com.example.model.TeamRoster
import com.example.model.Tournament
import com.example.model.TournamentCategory
import com.example.model.TournamentParticipant
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.Wallet
import com.example.model.WalletConfig
import com.example.model.WalletTransaction
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SpiderMainViewModel(
    private val db: FirebaseFirestore,
    private val currentUserId: String,
    private val userRepo: SpiderUserRepository,
    private val catalogRepo: SpiderCatalogRepository,
    private val adminRepo: SpiderAdminRepository
) : ViewModel() {

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _selectedGameTab = MutableStateFlow(PlayGameTab.TOURNAMENT)
    val selectedGameTab: StateFlow<PlayGameTab> = _selectedGameTab.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("EN")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _actionBanner = MutableStateFlow<Pair<Boolean, String>?>(null)
    val actionBanner: StateFlow<Pair<Boolean, String>?> = _actionBanner.asStateFlow()

    private val _roomCredentialsCache = MutableStateFlow<Map<String, RoomCredential>>(emptyMap())
    val roomCredentialsCache: StateFlow<Map<String, RoomCredential>> = _roomCredentialsCache.asStateFlow()

    private val _profileInitError = MutableStateFlow<String?>(null)
    val profileInitError: StateFlow<String?> = _profileInitError.asStateFlow()

    val coinPackages = listOf(
        CoinPackage("pkg_100", "Starter Pack", 100, "₹100", "POPULAR"),
        CoinPackage("pkg_250", "Arena Pack", 250, "₹240", "SAVE 4%"),
        CoinPackage("pkg_500", "Pro Squad Pack", 500, "₹475", "BEST VALUE"),
        CoinPackage("pkg_1000", "Spider Elite Pack", 1000, "₹920", "PRO")
    )

    private val walletRefreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val userProfile: StateFlow<UiState<UserProfile?>> =
        userRepo.observeUserProfile(currentUserId).toUiState("profile", SharingStarted.Eagerly)

    val wallet: StateFlow<Wallet> =
        channelFlow {
            launch {
                userRepo.observeWallet(currentUserId).collect { send(it) }
            }
            launch {
                walletRefreshTrigger.collect {
                    userRepo.fetchWallet(currentUserId).onSuccess { send(it) }
                }
            }
        }
            .catch { error ->
                val fe = extractFirestoreErrorOrRethrow(error)
                Log.w(TAG, "Wallet stream error", fe)
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, Wallet(id = currentUserId, userId = currentUserId))

    val walletConfig: StateFlow<WalletConfig> =
        userRepo.observeWalletConfig(currentUserId)
            .catch { error ->
                val fe = extractFirestoreErrorOrRethrow(error)
                Log.w(TAG, "Wallet config stream error", fe)
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, WalletConfig(id = "wallet"))

    val paymentOrders: StateFlow<List<PaymentOrder>> =
        userRepo.observePaymentOrders(currentUserId)
            .catch { error ->
                val fe = extractFirestoreErrorOrRethrow(error)
                Log.w(TAG, "Payment orders stream error", fe)
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val transactions: StateFlow<UiState<List<WalletTransaction>>> =
        userRepo.observeTransactions(currentUserId).toUiState("transactions", SharingStarted.Eagerly)

    val payoutRequests: StateFlow<List<PayoutRequest>> =
        userRepo.observePayoutRequests(currentUserId)
            .catch { error ->
                val fe = extractFirestoreErrorOrRethrow(error)
                Log.w(TAG, "Payout requests stream error", fe)
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val categories: StateFlow<UiState<List<TournamentCategory>>> =
        catalogRepo.observeEnabledCategories(currentUserId).toUiState("categories")

    val banners: StateFlow<List<PromoBanner>> =
        catalogRepo.observeEnabledBanners(currentUserId)
            .catch { error ->
                val fe = extractFirestoreErrorOrRethrow(error)
                Log.w(TAG, "Banner stream error", fe)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val announcements: StateFlow<List<Announcement>> =
        catalogRepo.observeEnabledAnnouncements(currentUserId)
            .catch { error ->
                val fe = extractFirestoreErrorOrRethrow(error)
                Log.w(TAG, "Announcement stream error", fe)
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val tournaments: StateFlow<UiState<List<Tournament>>> =
        catalogRepo.observePublicTournaments(currentUserId).toUiState("tournaments")

    val registrations: StateFlow<List<Registration>> =
        catalogRepo.observeUserRegistrations(currentUserId)
            .catch { error ->
                val fe = extractFirestoreErrorOrRethrow(error)
                Log.w(TAG, "Registration stream error", fe)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val matches: StateFlow<List<MatchRecord>> =
        catalogRepo.observeUserMatches(currentUserId)
            .catch { error ->
                val fe = extractFirestoreErrorOrRethrow(error)
                Log.w(TAG, "Matches stream error", fe)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val results: StateFlow<List<MatchResult>> =
        catalogRepo.observeVerifiedResults(currentUserId)
            .catch { error ->
                val fe = extractFirestoreErrorOrRethrow(error)
                Log.w(TAG, "Results stream error", fe)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val leaderboards: StateFlow<UiState<List<LeaderboardEntry>>> =
        catalogRepo.observeVerifiedLeaderboards(currentUserId).toUiState("leaderboards")

    val notifications: StateFlow<List<NotificationItem>> =
        userRepo.observeNotifications(currentUserId)
            .catch { error ->
                val fe = extractFirestoreErrorOrRethrow(error)
                Log.w(TAG, "Notifications stream error", fe)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val supportTickets: StateFlow<List<SupportTicket>> =
        userRepo.observeSupportTickets(currentUserId)
            .catch { error ->
                val fe = extractFirestoreErrorOrRethrow(error)
                Log.w(TAG, "Support tickets error", fe)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val disputes: StateFlow<List<Dispute>> =
        userRepo.observeDisputes(currentUserId)
            .catch { error ->
                val fe = extractFirestoreErrorOrRethrow(error)
                Log.w(TAG, "Disputes error", fe)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val teams: StateFlow<List<TeamRoster>> =
        userRepo.observeTeams(currentUserId)
            .catch { error ->
                val fe = extractFirestoreErrorOrRethrow(error)
                Log.w(TAG, "Teams error", fe)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myReferrals: StateFlow<List<ReferralRecord>> =
        userRepo.observeMyReferrals(currentUserId)
            .catch { error ->
                val fe = extractFirestoreErrorOrRethrow(error)
                Log.w(TAG, "My referrals error", fe)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myReferralReceived: StateFlow<ReferralRecord?> =
        userRepo.observeMyReferralReceived(currentUserId)
            .catch { error ->
                val fe = extractFirestoreErrorOrRethrow(error)
                Log.w(TAG, "Referral received error", fe)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    init {
        initializeSession()
    }

    fun initializeSession() {
        viewModelScope.launch {
            _profileInitError.value = null
            val currentUser = Firebase.auth.currentUser
            if (currentUser != null && currentUser.uid == currentUserId) {
                userRepo.ensureUserProfile(currentUser)
                    .onFailure { err ->
                        _profileInitError.value = err.localizedMessage ?: "Failed to initialize player profile."
                    }
                val admin = AuthHelper.isBootstrapAdmin(currentUser) && adminRepo.checkIsAdmin(currentUserId)
                _isAdmin.value = admin
                if (admin) {
                    adminRepo.ensureLinkedAdminRecord(currentUser)
                    SpiderCatalogSeeder.seedDefaultCatalogIfEmpty(
                        db = db,
                        adminUid = currentUser.uid,
                        adminEmail = currentUser.email ?: AuthHelper.BOOTSTRAP_ADMIN_EMAIL
                    )
                }
                refreshWallet()
            }
        }
    }

    fun refreshWallet() {
        walletRefreshTrigger.tryEmit(Unit)
    }

    fun selectGameTab(tab: PlayGameTab) {
        _selectedGameTab.value = tab
    }

    fun setLanguage(lang: String) {
        _selectedLanguage.value = lang
        val profile = (userProfile.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            userRepo.updateUserProfile(
                userId = currentUserId,
                currentUsername = profile.username,
                newUsername = profile.username,
                displayName = profile.displayName,
                inGameName = profile.inGameName,
                inGameUid = profile.inGameUid,
                phoneE164 = profile.phoneNumber,
                photoUrl = profile.photoUrl,
                language = lang,
                referredBy = profile.referredBy
            )
        }
    }

    fun showBanner(success: Boolean, message: String) {
        _actionBanner.value = success to message
    }

    fun clearBanner() {
        _actionBanner.value = null
    }

    fun registerForTournament(
        tournament: Tournament,
        inGameName: String,
        inGameUid: String,
        onComplete: (Boolean) -> Unit
    ) {
        val profile = (userProfile.value as? UiState.Success)?.data
        if (profile?.status == "SUSPENDED" || profile?.status == "BANNED") {
            _actionBanner.value = false to "Your account is currently ${profile.status.lowercase()}. Contact customer support."
            onComplete(false)
            return
        }
        viewModelScope.launch {
            val res = catalogRepo.registerForTournament(
                tournament = tournament,
                username = profile?.username ?: "Player",
                inGameName = inGameName,
                inGameUid = inGameUid
            )
            res.onSuccess {
                refreshWallet()
                val feeNote = if (tournament.entryFee > 0.0) " (${tournament.entryFee.toInt()} Coins deducted)" else ""
                _actionBanner.value = true to "Registered for ${tournament.title}!$feeNote"
                onComplete(true)
            }.onFailure { err ->
                _actionBanner.value = false to (err.localizedMessage ?: "Registration failed")
                onComplete(false)
            }
        }
    }

    fun observeTournamentParticipants(tournamentId: String): Flow<List<TournamentParticipant>> =
        catalogRepo.observeTournamentParticipants(tournamentId, currentUserId)
            .catch { error ->
                val fe = extractFirestoreErrorOrRethrow(error)
                Log.w(TAG, "Participants stream error for $tournamentId", fe)
            }

    fun loadRoomCredential(tournamentId: String) {
        viewModelScope.launch {
            catalogRepo.fetchRoomCredential(tournamentId).onSuccess { cred ->
                if (cred != null) {
                    _roomCredentialsCache.value = _roomCredentialsCache.value + (tournamentId to cred)
                }
            }
        }
    }

    fun updateProfile(
        newUsername: String,
        displayName: String,
        inGameName: String,
        inGameUid: String,
        phoneE164: String,
        photoUrl: String,
        referredBy: String,
        onDone: (Boolean) -> Unit
    ) {
        val existing = (userProfile.value as? UiState.Success)?.data
        viewModelScope.launch {
            val res = userRepo.updateUserProfile(
                userId = currentUserId,
                currentUsername = existing?.username ?: "",
                newUsername = newUsername,
                displayName = displayName,
                inGameName = inGameName,
                inGameUid = inGameUid,
                phoneE164 = phoneE164,
                photoUrl = photoUrl,
                language = _selectedLanguage.value,
                referredBy = referredBy
            )
            res.onSuccess {
                _actionBanner.value = true to "Profile updated successfully"
                onDone(true)
            }.onFailure { err ->
                _actionBanner.value = false to (err.localizedMessage ?: "Failed to update profile")
                onDone(false)
            }
        }
    }

    fun submitSupportTicket(
        category: String,
        subject: String,
        description: String,
        matchId: String,
        onDone: (Boolean) -> Unit
    ) {
        val username = (userProfile.value as? UiState.Success)?.data?.username ?: "Player"
        viewModelScope.launch {
            val res = userRepo.createSupportTicket(username, category, subject, description, matchId)
            res.onSuccess {
                _actionBanner.value = true to "Support ticket submitted."
                onDone(true)
            }.onFailure { err ->
                _actionBanner.value = false to (err.localizedMessage ?: "Ticket submission failed")
                onDone(false)
            }
        }
    }

    fun submitDispute(tournamentId: String, matchId: String, reason: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = userRepo.createDispute(tournamentId, matchId, reason)
            res.onSuccess {
                _actionBanner.value = true to "Dispute submitted for marshal review."
                onDone(true)
            }.onFailure { err ->
                _actionBanner.value = false to (err.localizedMessage ?: "Failed to submit dispute")
                onDone(false)
            }
        }
    }

    fun markNotificationRead(notifId: String) {
        viewModelScope.launch { userRepo.markNotificationRead(notifId) }
    }

    fun createTeam(teamName: String, gameMode: String, membersSummary: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = userRepo.createTeam(teamName, gameMode, membersSummary)
            res.onSuccess {
                _actionBanner.value = true to "Team '$teamName' saved."
                onDone(true)
            }.onFailure { err ->
                _actionBanner.value = false to (err.localizedMessage ?: "Failed to create team")
                onDone(false)
            }
        }
    }

    fun completeOnboarding(
        user: FirebaseUser,
        gameName: String,
        gameUid: String,
        phoneE164: String,
        referralCodeInput: String,
        acceptedTerms: Boolean,
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val res = userRepo.completeRegistrationProfile(
                user = user,
                gameName = gameName,
                gameUid = gameUid,
                phoneE164 = phoneE164,
                referralCodeInput = referralCodeInput,
                acceptedTerms = acceptedTerms
            )
            res.onSuccess {
                _actionBanner.value = true to "Spider Esports profile verified & activated!"
                onDone(true, null)
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Failed to complete profile setup"
                _actionBanner.value = false to msg
                onDone(false, msg)
            }
        }
    }

    fun applyReferralCode(code: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = userRepo.applyReferralCodeLater(code)
            res.onSuccess {
                _actionBanner.value = true to "Referral code applied! Pending admin verification."
                onDone(true, null)
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Could not apply referral code"
                _actionBanner.value = false to msg
                onDone(false, msg)
            }
        }
    }

    fun syncEmailVerification(user: FirebaseUser) {
        viewModelScope.launch {
            userRepo.syncEmailVerified(user)
        }
    }

    fun submitDepositRequest(
        coinAmount: Double,
        referenceId: String,
        onDone: (Boolean, String?) -> Unit
    ) {
        val profile = (userProfile.value as? UiState.Success)?.data
        if (profile?.status == "SUSPENDED" || profile?.status == "BANNED") {
            val msg = "Account is ${profile.status.lowercase()}. Deposits are disabled."
            _actionBanner.value = false to msg
            onDone(false, msg)
            return
        }
        viewModelScope.launch {
            val res = userRepo.submitDepositRequest(coinAmount, referenceId)
            res.onSuccess {
                refreshWallet()
                _actionBanner.value = true to "Deposit request (${coinAmount.toInt()} Coins = ₹${coinAmount.toInt()}) submitted for Admin verification."
                onDone(true, null)
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Deposit submission failed"
                _actionBanner.value = false to msg
                onDone(false, msg)
            }
        }
    }

    fun submitWithdrawalRequest(
        coinAmount: Double,
        destinationType: String,
        destinationDetails: String,
        onDone: (Boolean, String?) -> Unit
    ) {
        val profile = (userProfile.value as? UiState.Success)?.data
        if (profile?.status == "SUSPENDED" || profile?.status == "BANNED") {
            val msg = "Account is ${profile.status.lowercase()}. Withdrawals are disabled."
            _actionBanner.value = false to msg
            onDone(false, msg)
            return
        }
        viewModelScope.launch {
            val res = userRepo.submitWithdrawalRequest(coinAmount, destinationType, destinationDetails)
            res.onSuccess {
                refreshWallet()
                _actionBanner.value = true to "Winning withdrawal of ${coinAmount.toInt()} Coins (₹${coinAmount.toInt()}) submitted! Balance held pending review."
                onDone(true, null)
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Withdrawal request failed"
                _actionBanner.value = false to msg
                onDone(false, msg)
            }
        }
    }

    private fun <T> Flow<T>.toUiState(
        label: String,
        started: SharingStarted = SharingStarted.WhileSubscribed(5000L)
    ): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val root = extractFirestoreErrorOrRethrow(error)
                Log.w(TAG, "Error observing $label", root)
                emit(UiState.Error(root.message ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = started,
                initialValue = UiState.Loading
            )

    private fun extractFirestoreErrorOrRethrow(error: Throwable): Throwable {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        return firestoreError ?: error
    }

    private companion object {
        const val TAG = "SpiderMainVM"
    }
}
