package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val playerUid: String = "",
    val inGameName: String = "",
    val inGameUid: String = "",
    val phoneNumber: String = "",
    val phoneVerified: Boolean = false,
    val emailVerified: Boolean = false,
    val acceptedTerms: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val referralCode: String = "",
    val referredBy: String = "",
    val language: String = "EN",
    val status: String = "ACTIVE",
    val matchesPlayed: Long = 0L,
    val wins: Long = 0L,
    val verifiedKills: Long = 0L,
    val rankingPoints: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TournamentCategory(
    @DocumentId val id: String = "",
    val categoryName: String = "",
    val categoryImageUrl: String = "",
    val categoryType: String = "BATTLE_ROYALE",
    val presetKey: String = "battle_royale",
    val enabled: Boolean = true,
    val randomTournament: Boolean = false,
    val displayOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CustomTournamentType(
    @DocumentId val id: String = "",
    val displayName: String = "",
    val bannerUrl: String = "",
    val shortDescription: String = "",
    val matchDetails: String = "",
    val gameMode: String = "",
    val mapName: String = "",
    val entryFee: Double = 0.0,
    val prizePool: Double = 0.0,
    val perKillReward: Double = 0.0,
    val maxParticipants: Long = 48L,
    val matchDate: String = "",
    val matchTime: String = "",
    val registrationDeadline: String = "",
    val rules: String = "",
    val visibility: String = "PUBLIC",
    val status: String = "UPCOMING",
    val categoryId: String = "",
    val displayOrder: Long = 0L,
    val enabled: Boolean = true,
    val archived: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Tournament(
    @DocumentId val id: String = "",
    val categoryId: String = "",
    val title: String = "",
    val shortDescription: String = "",
    val matchDetails: String = "",
    val gameMode: String = "",
    val mapName: String = "",
    val entryFee: Double = 0.0,
    val prizePool: Double = 0.0,
    val perKillReward: Double = 0.0,
    val maxParticipants: Long = 48L,
    val registeredCount: Long = 0L,
    val matchDate: String = "",
    val matchTime: String = "",
    val registrationDeadline: String = "",
    val rules: String = "",
    val bannerUrl: String = "",
    val presetKey: String = "battle_royale",
    val visibility: String = "PUBLIC",
    val status: String = "UPCOMING",
    val soloFormat: Boolean = false,
    val randomTournament: Boolean = false,
    val roomReleased: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoomCredential(
    @DocumentId val id: String = "",
    val tournamentId: String = "",
    val roomId: String = "",
    val roomPassword: String = "",
    val released: Boolean = false,
    val releaseNotice: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Registration(
    @DocumentId val id: String = "",
    val tournamentId: String = "",
    val tournamentTitle: String = "",
    val categoryId: String = "",
    val userId: String = "",
    val username: String = "",
    val inGameName: String = "",
    val inGameUid: String = "",
    val entryFeePaid: Double = 0.0,
    val status: String = "CONFIRMED",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TournamentParticipant(
    @DocumentId val id: String = "",
    val tournamentId: String = "",
    val userId: String = "",
    val playerName: String = "",
    val username: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MapBanner(
    @DocumentId val id: String = "",
    val mapName: String = "",
    val bannerUrl: String = "",
    val presetKey: String = "battle_royale",
    val displayOrder: Long = 0L,
    val enabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MatchRecord(
    @DocumentId val id: String = "",
    val tournamentId: String = "",
    val title: String = "",
    val categoryId: String = "",
    val gameMode: String = "",
    val mapName: String = "",
    val status: String = "UPCOMING",
    val matchDate: String = "",
    val matchTime: String = "",
    val roomReleased: Boolean = false,
    val participantUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MatchResult(
    @DocumentId val id: String = "",
    val tournamentId: String = "",
    val tournamentTitle: String = "",
    val userId: String = "",
    val username: String = "",
    val placement: Long = 1L,
    val kills: Long = 0L,
    val rewardCoins: Double = 0.0,
    val verified: Boolean = true,
    val declaredBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Wallet(
    @DocumentId val id: String = "",
    val userId: String = "",
    val totalBalance: Double = 0.0,
    val depositedBalance: Double = 0.0,
    val winningBalance: Double = 0.0,
    val bonusBalance: Double = 0.0,
    val earningsBalance: Double = 0.0,
    val payoutsTotal: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WalletTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "ADMIN_ADJUSTMENT",
    val direction: String = "CREDIT",
    val description: String = "",
    val matchId: String = "",
    val amount: Double = 0.0,
    val status: String = "COMPLETED",
    val resultingBalance: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PayoutRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val destinationType: String = "UPI",
    val destinationDetails: String = "",
    val status: String = "PENDING",
    val reviewedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PaymentOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val packageName: String = "",
    val coinAmount: Double = 0.0,
    val rupeeAmount: Double = 0.0,
    val gateway: String = "UPI_QR",
    val referenceId: String = "",
    val reviewedBy: String = "",
    val status: String = "PENDING",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WalletConfig(
    @DocumentId val id: String = "",
    val qrImageUrl: String = "",
    val upiId: String = "",
    val instructions: String = "Scan QR to pay, enter UTR/Transaction Ref ID below. 50 Coins = ₹50.",
    val minWithdraw: Double = 50.0,
    val coinsPerRupee: Double = 1.0,
    val winningOnlyRedeem: Boolean = true,
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RefundRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val tournamentId: String = "",
    val amount: Double = 0.0,
    val reason: String = "",
    val status: String = "PENDING",
    val processedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PromoBanner(
    @DocumentId val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val imageUrl: String = "",
    val presetKey: String = "championship",
    val targetAction: String = "TOURNAMENTS",
    val targetId: String = "",
    val displayOrder: Long = 0L,
    val enabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = "IMPORTANT",
    val telegramUrl: String = "https://t.me/spideresports_official",
    val whatsappUrl: String = "",
    val whatsappBannerUrl: String = "",
    val priority: Long = 0L,
    val enabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val photoUrl: String = "",
    val mode: String = "OVERALL",
    val matches: Long = 0L,
    val wins: Long = 0L,
    val verifiedKills: Long = 0L,
    val rankingPoints: Long = 0L,
    val verified: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SupportTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val category: String = "GENERAL",
    val subject: String = "",
    val description: String = "",
    val matchId: String = "",
    val status: String = "OPEN",
    val adminReply: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Dispute(
    @DocumentId val id: String = "",
    val userId: String = "",
    val tournamentId: String = "",
    val matchId: String = "",
    val reason: String = "",
    val status: String = "OPEN",
    val resolutionNotes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class NotificationItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "INFO",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TeamRoster(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val teamName: String = "",
    val gameMode: String = "DUO",
    val membersSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val actorId: String = "",
    val actorEmail: String = "",
    val action: String = "",
    val targetCollection: String = "",
    val targetId: String = "",
    val summary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ReferralRecord(
    @DocumentId val id: String = "",
    val referrerUid: String = "",
    val referrerUsername: String = "",
    val referredUid: String = "",
    val referredUsername: String = "",
    val referralCode: String = "",
    val status: String = "PENDING",
    val bonusAmount: Double = 0.0,
    val transactionId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminUserRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val role: String = "ADMIN",
    val grantedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class CoinPackage(
    val id: String,
    val title: String,
    val coins: Int,
    val displayPrice: String,
    val badge: String = ""
)
