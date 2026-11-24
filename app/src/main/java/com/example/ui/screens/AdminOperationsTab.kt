package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.AuthHelper
import com.example.data.ImagePickerUtil
import com.example.model.AdminUserRecord
import com.example.model.Announcement
import com.example.model.Dispute
import com.example.model.MapBanner
import com.example.model.MatchResult
import com.example.model.PaymentOrder
import com.example.model.PayoutRequest
import com.example.model.ReferralRecord
import com.example.model.Registration
import com.example.model.SupportTicket
import com.example.model.Tournament
import com.example.model.TournamentCategory
import com.example.model.UserProfile
import com.example.model.Wallet
import com.example.model.WalletConfig
import com.example.model.WalletTransaction
import com.example.ui.components.SpiderArtworkImage
import com.example.ui.components.SpiderSectionTitle
import com.example.ui.components.StatusPill
import com.example.ui.components.statusColor
import com.example.ui.theme.SpiderAmber
import com.example.ui.theme.SpiderCardBorder
import com.example.ui.theme.SpiderCardDark
import com.example.ui.theme.SpiderEmerald
import com.example.ui.theme.SpiderGold
import com.example.ui.theme.SpiderRed
import com.example.ui.theme.SpiderRedBright
import com.example.ui.theme.SpiderSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.SpiderAdminViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun AdminTournamentsSection(
    tournaments: List<Tournament>,
    categories: List<TournamentCategory>,
    mapBanners: List<MapBanner>,
    users: List<UserProfile>,
    registrations: List<Registration>,
    results: List<MatchResult>,
    adminViewModel: SpiderAdminViewModel
) {
    var showCreate by remember { mutableStateOf(false) }
    var editingTournament by remember { mutableStateOf<Tournament?>(null) }
    var deleteTarget by remember { mutableStateOf<Tournament?>(null) }
    var roomTarget by remember { mutableStateOf<Tournament?>(null) }
    var resultTarget by remember { mutableStateOf<Tournament?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(
                onClick = { showCreate = true },
                colors = ButtonDefaults.buttonColors(containerColor = SpiderRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_create_tournament")
            ) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("CREATE TOURNAMENT", style = MaterialTheme.typography.labelLarge)
            }
        }
        items(tournaments, key = { it.id }) { tour ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SpiderCardDark,
                border = BorderStroke(1.dp, SpiderCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(tour.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(
                                "${tour.gameMode} • ${tour.mapName} • Slots ${tour.registeredCount}/${tour.maxParticipants}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                "Entry: ${if (tour.entryFee <= 0.0) "FREE" else "${tour.entryFee.toInt()} Coins"} • Prize: ${tour.prizePool.toInt()} Coins • Per Kill: ${tour.perKillReward.toInt()} Coins",
                                style = MaterialTheme.typography.labelSmall,
                                color = SpiderGold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusPill(tour.status, statusColor(tour.status))
                            IconButton(
                                onClick = { editingTournament = tour },
                                modifier = Modifier.testTag("edit_tournament_${tour.id}")
                            ) {
                                Icon(Icons.Filled.Edit, "Edit tournament", tint = TextPrimary)
                            }
                            IconButton(
                                onClick = { deleteTarget = tour },
                                modifier = Modifier.testTag("delete_tournament_${tour.id}")
                            ) {
                                Icon(Icons.Filled.Delete, "Delete tournament", tint = SpiderRedBright)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { roomTarget = tour },
                            border = BorderStroke(1.dp, SpiderCardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Key, null, tint = SpiderEmerald, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Room Creds", color = TextPrimary, style = MaterialTheme.typography.labelSmall)
                        }
                        val rankedCount = results.count { it.tournamentId == tour.id }
                        OutlinedButton(
                            onClick = { resultTarget = tour },
                            border = BorderStroke(1.dp, if (rankedCount > 0) SpiderGold.copy(alpha = 0.65f) else SpiderCardBorder),
                            modifier = Modifier.weight(1f).testTag("admin_verify_results_${tour.id}")
                        ) {
                            Icon(Icons.Filled.EmojiEvents, null, tint = SpiderGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (rankedCount > 0) "Results ($rankedCount/48)" else "Results (1–48)",
                                color = TextPrimary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreate || editingTournament != null) {
        TournamentEditorDialog(
            initial = editingTournament,
            categories = categories,
            mapBanners = mapBanners,
            onSaveMapBanner = { mName, bUrl, pKey ->
                adminViewModel.saveMapBanner(null, mName, bUrl, pKey)
            },
            onDismiss = {
                showCreate = false
                editingTournament = null
            },
            onSave = { categoryId, title, shortDesc, matchDetails, gameMode, mapName, entryFee, prizePool, perKillReward, maxP, matchDate, matchTime, deadline, rules, bannerUrl, presetKey, visibility, status, solo ->
                adminViewModel.saveTournament(
                    existingId = editingTournament?.id,
                    categoryId = categoryId,
                    title = title,
                    shortDescription = shortDesc,
                    matchDetails = matchDetails,
                    gameMode = gameMode,
                    mapName = mapName,
                    entryFee = entryFee,
                    prizePool = prizePool,
                    perKillReward = perKillReward,
                    maxParticipants = maxP,
                    matchDate = matchDate,
                    matchTime = matchTime,
                    registrationDeadline = deadline,
                    rules = rules,
                    bannerUrl = bannerUrl,
                    presetKey = presetKey,
                    visibility = visibility,
                    status = status,
                    soloFormat = solo
                )
                showCreate = false
                editingTournament = null
            }
        )
    }

    deleteTarget?.let { tour ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = SpiderCardDark,
            title = { Text("Delete Tournament?", color = TextPrimary) },
            text = {
                Text(
                    "Remove '${tour.title}' (${tour.gameMode}) from Spider Esports? This cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.deleteTournament(tour)
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpiderRedBright)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    roomTarget?.let { tour ->
        var roomId by remember { mutableStateOf("") }
        var roomPass by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { roomTarget = null },
            containerColor = SpiderCardDark,
            title = { Text("Publish Room: ${tour.title}", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val fc = adminFieldColors()
                    OutlinedTextField(value = roomId, onValueChange = { roomId = it }, label = { Text("Room ID") }, singleLine = true, colors = fc, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = roomPass, onValueChange = { roomPass = it }, label = { Text("Room Password") }, singleLine = true, colors = fc, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.publishRoomCredentials(tour, roomId, roomPass)
                        roomTarget = null
                    },
                    enabled = roomId.isNotBlank() && roomPass.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = SpiderRed)
                ) { Text("Release Now") }
            },
            dismissButton = { TextButton(onClick = { roomTarget = null }) { Text("Cancel", color = TextSecondary) } }
        )
    }

    resultTarget?.let { tour ->
        val latestTour = tournaments.firstOrNull { it.id == tour.id } ?: tour
        AdminTournamentResultsDialog(
            tournament = latestTour,
            users = users,
            registrations = registrations,
            results = results,
            onDismiss = { resultTarget = null },
            onSavePosition = { playerName, linkedUser, placement, kills, rewardCoins, mode, onDone ->
                adminViewModel.verifyResult(
                    tournamentId = latestTour.id,
                    tournamentTitle = latestTour.title,
                    playerName = playerName,
                    user = linkedUser,
                    placement = placement,
                    kills = kills,
                    rewardCoins = rewardCoins,
                    leaderboardMode = mode,
                    onDone = onDone
                )
            },
            onClearPosition = { placement, existingId ->
                adminViewModel.clearResultPosition(latestTour.id, placement, existingId)
            },
            onMarkCompleted = {
                adminViewModel.markTournamentCompleted(latestTour)
            }
        )
    }
}

@Composable
fun AdminUsersSection(
    users: List<UserProfile>,
    adminUsers: List<AdminUserRecord>,
    adminViewModel: SpiderAdminViewModel
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(users, search) {
        if (search.isBlank()) users
        else users.filter {
            it.username.contains(search, true) ||
                it.email.contains(search, true) ||
                it.playerUid.contains(search, true) ||
                it.phoneNumber.contains(search, true)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Search username, email, phone, or UID") },
                singleLine = true,
                colors = adminFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }
        items(filtered, key = { it.id }) { user ->
            val isBootstrapOwner = user.email.equals(AuthHelper.BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true) &&
                user.emailVerified

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SpiderCardDark,
                border = BorderStroke(1.dp, if (isBootstrapOwner) SpiderGold.copy(alpha = 0.45f) else SpiderCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${user.username} (${user.playerUid})", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                if (isBootstrapOwner) {
                                    StatusPill("ADMIN", SpiderGold)
                                }
                            }
                            Text(user.email, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            if (user.phoneNumber.isNotBlank()) {
                                Text("Mobile: ${user.phoneNumber}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            Text(
                                "Ref: ${user.referralCode.ifBlank { "-" }} • Pts: ${user.rankingPoints} • Kills: ${user.verifiedKills}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusPill(
                                    text = if (user.emailVerified) "EMAIL VERIFIED" else "EMAIL UNVERIFIED",
                                    color = if (user.emailVerified) SpiderEmerald else SpiderRedBright
                                )
                            }
                        }
                        StatusPill(user.status, statusColor(user.status))
                    }

                    if (!isBootstrapOwner) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val isSuspended = user.status == "SUSPENDED"
                            val isBanned = user.status == "BANNED"
                            OutlinedButton(
                                onClick = {
                                    adminViewModel.setUserStatus(
                                        user.userId,
                                        if (isSuspended) "ACTIVE" else "SUSPENDED"
                                    )
                                },
                                border = BorderStroke(1.dp, if (isSuspended) SpiderEmerald else SpiderAmber),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    if (isSuspended) "Unsuspend" else "Suspend",
                                    color = if (isSuspended) SpiderEmerald else SpiderAmber,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    adminViewModel.setUserStatus(
                                        user.userId,
                                        if (isBanned) "ACTIVE" else "BANNED"
                                    )
                                },
                                border = BorderStroke(1.dp, if (isBanned) SpiderEmerald else SpiderRedBright),
                                modifier = Modifier.weight(1f).testTag("ban_user_${user.userId}")
                            ) {
                                Text(
                                    if (isBanned) "Unban Player" else "Ban Player",
                                    color = if (isBanned) SpiderEmerald else SpiderRedBright,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminWalletsSection(
    users: List<UserProfile>,
    wallets: List<Wallet>,
    walletConfig: WalletConfig,
    payments: List<PaymentOrder>,
    payouts: List<PayoutRequest>,
    transactions: List<WalletTransaction>,
    referrals: List<ReferralRecord>,
    adminViewModel: SpiderAdminViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var targetUser by remember { mutableStateOf<UserProfile?>(null) }
    var approveReferralTarget by remember { mutableStateOf<ReferralRecord?>(null) }

    var qrImageUrl by remember(walletConfig.qrImageUrl) { mutableStateOf(walletConfig.qrImageUrl) }
    var upiId by remember(walletConfig.upiId) { mutableStateOf(walletConfig.upiId) }
    var instructions by remember(walletConfig.instructions) {
        mutableStateOf(walletConfig.instructions.ifBlank { "Scan QR to pay, enter UTR/Reference ID below. 50 Coins = ₹50." })
    }
    var minWithdrawText by remember(walletConfig.minWithdraw) {
        mutableStateOf(walletConfig.minWithdraw.coerceAtLeast(50.0).toInt().toString())
    }

    val qrPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                ImagePickerUtil.encodeUriToDataUri(context, uri).onSuccess { qrImageUrl = it }
            }
        }
    }

    val sortedPayments = remember(payments) {
        payments.sortedWith(compareBy<PaymentOrder> { it.status != "PENDING" }.thenByDescending { it.createdAt?.seconds ?: 0L })
    }
    val pendingDepositCount = payments.count { it.status == "PENDING" }

    val sortedPayouts = remember(payouts) {
        payouts.sortedWith(compareBy<PayoutRequest> { it.status != "PENDING" }.thenByDescending { it.createdAt?.seconds ?: 0L })
    }
    val pendingPayoutCount = payouts.count { it.status == "PENDING" }

    val sortedReferrals = remember(referrals) {
        referrals.sortedWith(compareBy<ReferralRecord> { it.status != "PENDING" }.thenByDescending { it.createdAt?.seconds ?: 0L })
    }
    val pendingRefCount = referrals.count { it.status == "PENDING" }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Admin QR Code & Withdrawal Rules Config
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SpiderCardDark,
                border = BorderStroke(1.dp, SpiderGold.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SpiderSectionTitle(
                        title = "ADD COIN QR & WITHDRAW SETTINGS",
                        subtitle = "50 Coins = ₹50 • Only Winning Prize redeemable"
                    )
                    if (qrImageUrl.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            modifier = Modifier.size(150.dp).align(Alignment.CenterHorizontally)
                        ) {
                            SpiderArtworkImage(
                                imageUrl = qrImageUrl,
                                presetKey = "logo",
                                contentDescription = "Admin QR Preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize().padding(8.dp)
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.QrCode2, null, tint = SpiderGold, modifier = Modifier.size(28.dp))
                            Text("No QR uploaded yet. Upload your UPI QR below.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { qrPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            border = BorderStroke(1.dp, SpiderCardBorder),
                            modifier = Modifier.weight(1f).testTag("admin_upload_qr_button")
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Upload QR Image", color = TextPrimary, style = MaterialTheme.typography.labelSmall)
                        }
                        if (qrImageUrl.isNotBlank()) {
                            OutlinedButton(
                                onClick = { qrImageUrl = "" },
                                border = BorderStroke(1.dp, SpiderRedBright)
                            ) {
                                Text("Clear", color = SpiderRedBright, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    val fc = adminFieldColors()
                    OutlinedTextField(
                        value = qrImageUrl,
                        onValueChange = { qrImageUrl = it },
                        label = { Text("QR Image URL or Uploaded Data") },
                        singleLine = true,
                        colors = fc,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it },
                        label = { Text("Admin UPI ID (e.g. name@upi)") },
                        singleLine = true,
                        colors = fc,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        label = { Text("Add Coin Payment Instructions") },
                        minLines = 2,
                        colors = fc,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = minWithdrawText,
                        onValueChange = { minWithdrawText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Minimum Withdraw Coins (Enforced >= 50)") },
                        singleLine = true,
                        colors = fc,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val minW = (minWithdrawText.toDoubleOrNull() ?: 50.0).coerceAtLeast(50.0)
                            minWithdrawText = minW.toInt().toString()
                            adminViewModel.saveWalletQrConfig(qrImageUrl, upiId, instructions, minW)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SpiderRed),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_save_qr_config_button")
                    ) {
                        Text("SAVE QR & WALLET RULES", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // 2. Deposit Requests Queue (Add Coins)
        item {
            SpiderSectionTitle(
                title = "ADD COIN DEPOSIT QUEUE ($pendingDepositCount Pending)",
                subtitle = "Verify UTR & credit user's Deposit balance"
            )
        }
        if (sortedPayments.isEmpty()) {
            item { Text("No deposit requests submitted yet.", style = MaterialTheme.typography.bodySmall, color = TextMuted) }
        } else {
            items(sortedPayments, key = { it.id }) { pay ->
                val userName = users.find { it.userId == pay.userId }?.username ?: pay.userId.take(10)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SpiderCardDark,
                    border = BorderStroke(1.dp, if (pay.status == "PENDING") SpiderGold.copy(alpha = 0.55f) else SpiderCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("$userName • +${pay.coinAmount.toInt()} Coins (₹${pay.coinAmount.toInt()})", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("UTR / Ref: ${pay.referenceId.ifBlank { "-" }} • ${pay.gateway}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            StatusPill(pay.status, statusColor(pay.status))
                        }
                        if (pay.status == "PENDING") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { adminViewModel.reviewDeposit(pay, true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SpiderEmerald, contentColor = Color.Black),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Approve Deposit", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { adminViewModel.reviewDeposit(pay, false, "UTR not matched") },
                                    border = BorderStroke(1.dp, SpiderRedBright),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Reject", color = SpiderRedBright, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Winning Withdrawal Queue
        item {
            SpiderSectionTitle(
                title = "WINNING WITHDRAWAL QUEUE ($pendingPayoutCount Pending)",
                subtitle = "Min 50 Coins • Deducts from Winning Prize balance only"
            )
        }
        if (sortedPayouts.isEmpty()) {
            item { Text("No withdrawal requests submitted yet.", style = MaterialTheme.typography.bodySmall, color = TextMuted) }
        } else {
            items(sortedPayouts, key = { it.id }) { payout ->
                val userName = users.find { it.userId == payout.userId }?.username ?: payout.userId.take(10)
                val userWin = wallets.find { it.userId == payout.userId }?.winningBalance ?: 0.0
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SpiderCardDark,
                    border = BorderStroke(1.dp, if (payout.status == "PENDING") SpiderGold.copy(alpha = 0.55f) else SpiderCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("$userName • ₹${payout.amount.toInt()} (${payout.amount.toInt()} Winning Coins)", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text(
                                    "${payout.destinationType}: ${payout.destinationDetails.ifBlank { "-" }} • Win Bal: ${userWin.toInt()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            StatusPill(payout.status, statusColor(payout.status))
                        }
                        if (payout.status == "PENDING") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { adminViewModel.reviewPayout(payout, true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SpiderRed, contentColor = Color.White),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Approve & Mark Paid", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { adminViewModel.reviewPayout(payout, false, "Payout verification failed") },
                                    border = BorderStroke(1.dp, SpiderRedBright),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Reject", color = SpiderRedBright, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Referral Bonus Queue
        item {
            SpiderSectionTitle(
                title = "REFERRAL BONUS QUEUE ($pendingRefCount Pending)",
                subtitle = "Verify & credit Bonus balance for invited players"
            )
        }
        if (sortedReferrals.isEmpty()) {
            item {
                Text("No referral claims recorded yet.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        } else {
            items(sortedReferrals, key = { it.id }) { ref ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SpiderCardDark,
                    border = BorderStroke(1.dp, if (ref.status == "PENDING") SpiderGold.copy(alpha = 0.55f) else SpiderCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Referrer: ${ref.referrerUsername} -> Invited: ${ref.referredUsername}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                Text(
                                    "Code: ${ref.referralCode}" + if (ref.bonusAmount > 0.0) " • Credited +${ref.bonusAmount.toInt()} Bonus Coins" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            StatusPill(ref.status, statusColor(ref.status))
                        }
                        if (ref.status == "PENDING") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { approveReferralTarget = ref },
                                    colors = ButtonDefaults.buttonColors(containerColor = SpiderRed, contentColor = Color.White),
                                    modifier = Modifier.weight(1f).testTag("approve_referral_${ref.referredUid}")
                                ) {
                                    Text("Approve & Credit", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { adminViewModel.rejectReferral(ref, "Ineligible referral claim") },
                                    border = BorderStroke(1.dp, SpiderRedBright),
                                    modifier = Modifier.weight(1f).testTag("reject_referral_${ref.referredUid}")
                                ) {
                                    Text("Reject", color = SpiderRedBright, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Player Wallets (Manage Deposit / Winning / Bonus per player)
        item {
            SpiderSectionTitle(
                title = "PLAYER WALLETS (${users.size})",
                subtitle = "Credit or debit Deposit, Winning, or Bonus coins"
            )
        }
        items(users, key = { it.id }) { user ->
            val w = wallets.find { it.userId == user.userId }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SpiderCardDark,
                border = BorderStroke(1.dp, SpiderCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("${user.username} (${user.status})", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text(
                            "Total: ${String.format(Locale.US, "%.0f", w?.totalBalance ?: 0.0)} • Dep: ${(w?.depositedBalance ?: 0.0).toInt()} • Win: ${(w?.winningBalance ?: 0.0).toInt()} • Bonus: ${(w?.bonusBalance ?: 0.0).toInt()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SpiderGold
                        )
                    }
                    OutlinedButton(onClick = { targetUser = user }, border = BorderStroke(1.dp, SpiderCardBorder)) {
                        Text("Manage", color = TextPrimary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        item { SpiderSectionTitle(title = "RECENT TRANSACTIONS (${transactions.size})") }
        items(transactions.take(15), key = { it.id }) { tx ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SpiderCardDark,
                border = BorderStroke(1.dp, SpiderCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${tx.direction} • ${tx.amount.toInt()} Coins (${tx.type})", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text(tx.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    StatusPill(tx.status, statusColor(tx.status))
                }
            }
        }
    }

    approveReferralTarget?.let { ref ->
        var bonusAmountText by remember(ref.id) { mutableStateOf("10") }
        val parsedBonus = bonusAmountText.toDoubleOrNull() ?: 0.0
        AlertDialog(
            onDismissRequest = { approveReferralTarget = null },
            containerColor = SpiderCardDark,
            title = { Text("Credit Referral Bonus", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Credit Bonus coins to ${ref.referrerUsername} for inviting ${ref.referredUsername} (Code: ${ref.referralCode}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = bonusAmountText,
                        onValueChange = { bonusAmountText = it },
                        label = { Text("Bonus Coins (1–1000)") },
                        singleLine = true,
                        colors = adminFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.approveReferralBonus(ref, parsedBonus)
                        approveReferralTarget = null
                    },
                    enabled = parsedBonus in 1.0..1000.0,
                    colors = ButtonDefaults.buttonColors(containerColor = SpiderRed)
                ) { Text("Credit Bonus") }
            },
            dismissButton = {
                TextButton(onClick = { approveReferralTarget = null }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }

    targetUser?.let { u ->
        var amount by remember { mutableStateOf("50") }
        var direction by remember { mutableStateOf("CREDIT") }
        var bucket by remember { mutableStateOf("WINNING") }
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { targetUser = null },
            containerColor = SpiderCardDark,
            title = { Text("Manage Wallet: ${u.username}", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Wallet Bucket", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("WINNING", "BONUS", "DEPOSIT").forEach { b ->
                            FilterChip(
                                selected = bucket == b,
                                onClick = { bucket = b },
                                label = { Text(b, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SpiderGold,
                                    selectedLabelColor = Color.Black,
                                    containerColor = SpiderSurfaceElevated,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("CREDIT", "DEBIT").forEach { dir ->
                            FilterChip(
                                selected = direction == dir,
                                onClick = { direction = dir },
                                label = { Text(dir) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SpiderRed,
                                    selectedLabelColor = Color.White,
                                    containerColor = SpiderSurfaceElevated,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                    val fc = adminFieldColors()
                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Coins (50 Coins = ₹50)") }, singleLine = true, colors = fc, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Audit Reason") }, singleLine = true, colors = fc, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.adjustWallet(u.userId, amount.toDoubleOrNull() ?: 0.0, direction, bucket, reason)
                        targetUser = null
                    },
                    enabled = (amount.toDoubleOrNull() ?: 0.0) > 0.0 && reason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = SpiderRed)
                ) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { targetUser = null }) { Text("Cancel", color = TextSecondary) } }
        )
    }
}

@Composable
fun AdminSupportAndConfigSection(
    announcements: List<Announcement>,
    tickets: List<SupportTicket>,
    disputes: List<Dispute>,
    adminViewModel: SpiderAdminViewModel
) {
    val supportCfg = announcements.firstOrNull { it.category == "SUPPORT_CONFIG" }
    val rulesCfg = announcements.firstOrNull { it.category == "RULES" }

    var telegramUrl by remember(supportCfg) { mutableStateOf(supportCfg?.telegramUrl ?: "https://t.me/spideresports_official") }
    var whatsappUrl by remember(supportCfg) { mutableStateOf(supportCfg?.whatsappUrl ?: "https://wa.me/0000000000") }
    var rulesBody by remember(rulesCfg) { mutableStateOf(rulesCfg?.body ?: "") }
    var alertTitle by remember { mutableStateOf("") }
    var alertBody by remember { mutableStateOf("") }
    var replyTicket by remember { mutableStateOf<SupportTicket?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(shape = RoundedCornerShape(14.dp), color = SpiderCardDark, border = BorderStroke(1.dp, SpiderCardBorder), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpiderSectionTitle(title = "SUPPORT LINKS & RULES")
                    val fc = adminFieldColors()
                    OutlinedTextField(value = telegramUrl, onValueChange = { telegramUrl = it }, label = { Text("Telegram Support URL") }, singleLine = true, colors = fc, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = whatsappUrl, onValueChange = { whatsappUrl = it }, label = { Text("WhatsApp Support URL") }, singleLine = true, colors = fc, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            adminViewModel.saveAnnouncementConfig("ann_support_links", "Official Support Channels", "Reach tournament marshals via Telegram or WhatsApp.", "SUPPORT_CONFIG", telegramUrl, whatsappUrl)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SpiderRed),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Update Support Links") }

                    OutlinedTextField(value = rulesBody, onValueChange = { rulesBody = it }, label = { Text("Spider Esports Rules Text") }, minLines = 3, colors = fc, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            adminViewModel.saveAnnouncementConfig("ann_rules_master", "SPIDER ESPORTS OFFICIAL RULES", rulesBody, "RULES", telegramUrl, whatsappUrl)
                        },
                        enabled = rulesBody.trim().length >= 5,
                        colors = ButtonDefaults.buttonColors(containerColor = SpiderRed),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save Official Rules") }
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(14.dp), color = SpiderCardDark, border = BorderStroke(1.dp, SpiderCardBorder), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpiderSectionTitle(title = "PUBLISH IMPORTANT ALERT")
                    val fc = adminFieldColors()
                    OutlinedTextField(value = alertTitle, onValueChange = { alertTitle = it }, label = { Text("Alert Title") }, singleLine = true, colors = fc, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = alertBody, onValueChange = { alertBody = it }, label = { Text("Alert Message") }, minLines = 2, colors = fc, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            adminViewModel.saveAnnouncementConfig("ann_alert_${System.currentTimeMillis()}", alertTitle, alertBody, "IMPORTANT", telegramUrl, whatsappUrl)
                            alertTitle = ""
                            alertBody = ""
                        },
                        enabled = alertTitle.trim().length >= 2 && alertBody.trim().length >= 2,
                        colors = ButtonDefaults.buttonColors(containerColor = SpiderRed),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Publish Alert") }
                }
            }
        }

        item { SpiderSectionTitle(title = "SUPPORT TICKETS (${tickets.size})") }
        items(tickets, key = { it.id }) { t ->
            Surface(shape = RoundedCornerShape(12.dp), color = SpiderCardDark, border = BorderStroke(1.dp, SpiderCardBorder), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${t.subject} (${t.username})", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        StatusPill(t.status, statusColor(t.status))
                    }
                    Text(t.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    TextButton(onClick = { replyTicket = t }) { Text("Reply / Resolve", color = SpiderRedBright) }
                }
            }
        }

        item { SpiderSectionTitle(title = "DISPUTES (${disputes.size})") }
        items(disputes, key = { it.id }) { d ->
            Surface(shape = RoundedCornerShape(12.dp), color = SpiderCardDark, border = BorderStroke(1.dp, SpiderCardBorder), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tournament: ${d.tournamentId}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text(d.reason, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    TextButton(onClick = { adminViewModel.resolveDispute(d.id, "RESOLVED", "Reviewed by Marshal") }) {
                        Text("Resolve", color = SpiderEmerald)
                    }
                }
            }
        }
    }

    replyTicket?.let { ticket ->
        var reply by remember { mutableStateOf(ticket.adminReply) }
        AlertDialog(
            onDismissRequest = { replyTicket = null },
            containerColor = SpiderCardDark,
            title = { Text("Reply: ${ticket.subject}", color = TextPrimary) },
            text = {
                OutlinedTextField(value = reply, onValueChange = { reply = it }, label = { Text("Admin Reply") }, minLines = 3, colors = adminFieldColors(), modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.replyTicket(ticket.id, "RESOLVED", reply)
                        replyTicket = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpiderRed)
                ) { Text("Mark Resolved") }
            },
            dismissButton = { TextButton(onClick = { replyTicket = null }) { Text("Cancel", color = TextSecondary) } }
        )
    }
}
