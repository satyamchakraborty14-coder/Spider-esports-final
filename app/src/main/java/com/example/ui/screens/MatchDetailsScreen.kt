package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.MatchResult
import com.example.model.Registration
import com.example.model.Tournament
import com.example.model.TournamentParticipant
import com.example.model.UserProfile
import com.example.model.Wallet
import com.example.ui.components.SpiderArtworkImage
import com.example.ui.components.SpiderBackTopBar
import com.example.ui.components.SpiderEmptyState
import com.example.ui.components.StatusPill
import com.example.ui.components.statusColor
import com.example.ui.theme.SpiderBlack
import com.example.ui.theme.SpiderCardBorder
import com.example.ui.theme.SpiderCardDark
import com.example.ui.theme.SpiderEmerald
import com.example.ui.theme.SpiderGold
import com.example.ui.theme.SpiderRed
import com.example.ui.theme.SpiderRedBright
import com.example.ui.theme.SpiderSurface
import com.example.ui.theme.SpiderSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private enum class MatchDetailsTab {
    DESCRIPTION,
    JOINED_MEMBERS
}

@Composable
fun MatchDetailsScreen(
    tournament: Tournament?,
    participants: List<TournamentParticipant>,
    currentUserRegistration: Registration?,
    currentUserId: String,
    profile: UserProfile?,
    wallet: Wallet,
    results: List<MatchResult>,
    onBack: () -> Unit,
    onJoinTournament: (Tournament, String, String, (Boolean) -> Unit) -> Unit
) {
    var selectedTab by remember { mutableStateOf(MatchDetailsTab.DESCRIPTION) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showResultsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpiderBlack)
    ) {
        SpiderBackTopBar(
            title = "Match Details",
            onBack = onBack
        )

        val activeTournament = tournament
        if (activeTournament == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                SpiderEmptyState(
                    title = "Match Not Found",
                    subtitle = "This tournament may have been removed or archived by Admin."
                )
            }
            return
        }

        val isRegistered = currentUserRegistration != null || participants.any { it.userId == currentUserId }
        val isCompleted = tournament.status == "COMPLETED"
        val maxP = tournament.maxParticipants.coerceAtLeast(1L)
        val slotsLeft = (tournament.maxParticipants - tournament.registeredCount).coerceAtLeast(0L)
        val progress = (tournament.registeredCount.toFloat() / maxP.toFloat()).coerceIn(0f, 1f)
        val tourResultCount = remember(results, tournament.id) {
            results.count { it.tournamentId == tournament.id }
        }

        // Order joined members one-by-one: if the current user has joined, start with their name first!
        val orderedMembers = remember(participants, currentUserRegistration, currentUserId, tournament.id) {
            orderTournamentParticipants(
                tournamentId = tournament.id,
                participants = participants,
                currentUserRegistration = currentUserRegistration,
                currentUserId = currentUserId
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 820.dp)
                    .align(Alignment.TopCenter),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Same Match Banner & Key Info Header
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SpiderCardDark,
                        border = BorderStroke(1.dp, if (isCompleted) SpiderGold.copy(alpha = 0.55f) else SpiderCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(176.dp)
                                    .testTag("match_details_banner_${tournament.id}")
                            ) {
                                SpiderArtworkImage(
                                    imageUrl = tournament.bannerUrl,
                                    presetKey = tournament.presetKey,
                                    contentDescription = tournament.title,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, SpiderBlack.copy(alpha = 0.92f))
                                            )
                                        )
                                )
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    StatusPill(text = tournament.gameMode, color = SpiderRedBright)
                                    StatusPill(text = tournament.mapName, color = TextPrimary)
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp)
                                ) {
                                    StatusPill(
                                        text = tournament.status,
                                        color = if (isCompleted) SpiderGold else statusColor(tournament.status)
                                    )
                                }
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = tournament.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (tournament.shortDescription.isNotBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = tournament.shortDescription,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    DetailMetric(
                                        label = "ENTRY FEE",
                                        value = if (tournament.entryFee <= 0.0) "FREE" else "${tournament.entryFee.toInt()} Coins",
                                        accent = if (tournament.entryFee <= 0.0) SpiderEmerald else SpiderGold
                                    )
                                    DetailMetric(
                                        label = "PRIZE POOL",
                                        value = if (tournament.prizePool <= 0.0) "Points" else "${tournament.prizePool.toInt()} Coins",
                                        accent = TextPrimary
                                    )
                                    DetailMetric(
                                        label = "PER KILL",
                                        value = "${tournament.perKillReward.toInt()} Coins",
                                        accent = SpiderGold
                                    )
                                    DetailMetric(
                                        label = "SLOTS",
                                        value = "${tournament.registeredCount}/${tournament.maxParticipants}",
                                        accent = if (slotsLeft > 0L) TextPrimary else SpiderRedBright
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { progress },
                                    color = if (isCompleted) SpiderGold else SpiderRed,
                                    trackColor = SpiderSurfaceElevated,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Schedule, null, tint = TextMuted, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "${tournament.matchDate} • ${tournament.matchTime} (Reg ends ${tournament.registrationDeadline})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Two Options Under Banner: DESCRIPTION & JOINED MEMBER
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SpiderCardDark)
                            .border(1.dp, SpiderCardBorder, RoundedCornerShape(14.dp))
                            .padding(5.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val descSelected = selectedTab == MatchDetailsTab.DESCRIPTION
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (descSelected) SpiderRed else SpiderSurfaceElevated)
                                .clickable { selectedTab = MatchDetailsTab.DESCRIPTION }
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                                .testTag("details_tab_description"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Description,
                                    contentDescription = "Description tab",
                                    tint = if (descSelected) Color.White else TextSecondary,
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = "DESCRIPTION",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (descSelected) Color.White else TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }

                        val membersSelected = selectedTab == MatchDetailsTab.JOINED_MEMBERS
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (membersSelected) SpiderRed else SpiderSurfaceElevated)
                                .clickable { selectedTab = MatchDetailsTab.JOINED_MEMBERS }
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                                .testTag("details_tab_joined_members"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Group,
                                    contentDescription = "Joined Member tab",
                                    tint = if (membersSelected) Color.White else TextSecondary,
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = "JOINED MEMBER (${orderedMembers.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (membersSelected) Color.White else TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // 3. Selected Tab Content
                if (selectedTab == MatchDetailsTab.DESCRIPTION) {
                    item {
                        val descriptionBody = tournament.matchDetails.ifBlank {
                            tournament.shortDescription.ifBlank {
                                "No additional match description was provided by the organizer."
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SpiderCardDark,
                            border = BorderStroke(1.dp, SpiderCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Info, null, tint = SpiderGold, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Match Description",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = descriptionBody,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SpiderCardDark,
                            border = BorderStroke(1.dp, SpiderCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.EmojiEvents, null, tint = SpiderGold, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Match Overview",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                OverviewLine("Game Mode", tournament.gameMode)
                                OverviewLine("Map", tournament.mapName)
                                OverviewLine("Match Schedule", "${tournament.matchDate} at ${tournament.matchTime}")
                                OverviewLine("Registration Ends", tournament.registrationDeadline)
                                OverviewLine(
                                    "Entry Fee",
                                    if (tournament.entryFee <= 0.0) "FREE" else "${tournament.entryFee.toInt()} Coins"
                                )
                                OverviewLine(
                                    "Prize Pool / Per Kill",
                                    "${tournament.prizePool.toInt()} Coins / ${tournament.perKillReward.toInt()} Coins per kill"
                                )
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SpiderCardDark,
                            border = BorderStroke(1.dp, SpiderCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Gavel, null, tint = SpiderRedBright, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Tournament Rules",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = tournament.rules.ifBlank { "Fair-play rules enforced. Zero hacks or teaming." },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    // JOINED MEMBER TAB
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SpiderSurfaceElevated,
                            border = BorderStroke(1.dp, SpiderCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isRegistered) {
                                        "Your joined name starts at #1 followed by other joined players"
                                    } else {
                                        "Players joined in this match (${orderedMembers.size}/${tournament.maxParticipants})"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isRegistered) SpiderEmerald else TextSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                StatusPill(
                                    text = "${orderedMembers.size} JOINED",
                                    color = SpiderGold
                                )
                            }
                        }
                    }

                    if (orderedMembers.isEmpty()) {
                        item {
                            SpiderEmptyState(
                                title = "No Joined Members Yet",
                                subtitle = "Be the first player to join this match! Your name will appear at #1 once you join."
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = orderedMembers,
                            key = { idx, item -> "${item.userId}_$idx" }
                        ) { index, member ->
                            val isCurrentUser = member.userId == currentUserId
                            val displayPlayerName = member.playerName.ifBlank {
                                member.username.ifBlank { "Player #${index + 1}" }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCurrentUser) SpiderEmerald.copy(alpha = 0.12f) else SpiderCardDark,
                                border = BorderStroke(
                                    1.dp,
                                    if (isCurrentUser) SpiderEmerald.copy(alpha = 0.65f) else SpiderCardBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("joined_member_row_${index + 1}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isCurrentUser) SpiderEmerald else SpiderSurfaceElevated
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrentUser) Color.White else SpiderGold
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = displayPlayerName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (member.username.isNotBlank() &&
                                            !member.username.equals(displayPlayerName, ignoreCase = true)
                                        ) {
                                            Text(
                                                text = "@${member.username}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    if (isCurrentUser) {
                                        StatusPill(text = "YOU", color = SpiderEmerald)
                                    } else {
                                        StatusPill(text = "JOINED", color = SpiderGold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Persistent Bottom CTA Bar
        Surface(
            color = SpiderSurface,
            border = BorderStroke(1.dp, SpiderCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                when {
                    isCompleted -> {
                        Button(
                            onClick = { showResultsDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SpiderGold,
                                contentColor = SpiderBlack
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("match_details_results_button")
                        ) {
                            Icon(Icons.Filled.EmojiEvents, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (tourResultCount > 0) {
                                    "VIEW RESULTS & RANKS ($tourResultCount/48)"
                                } else {
                                    "VIEW RESULTS & RANKS (1–48)"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    isRegistered -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SpiderEmerald.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, SpiderEmerald.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("match_details_joined_badge")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.CheckCircle, null, tint = SpiderEmerald, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                val joinedName = currentUserRegistration?.inGameName?.ifBlank {
                                    currentUserRegistration.username
                                } ?: orderedMembers.firstOrNull { it.userId == currentUserId }?.playerName ?: "Player"
                                Text(
                                    text = "JOINED AS $joinedName",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SpiderEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    else -> {
                        Button(
                            onClick = { showJoinDialog = true },
                            enabled = slotsLeft > 0L,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SpiderRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("match_details_join_button")
                        ) {
                            Text(
                                text = when {
                                    slotsLeft == 0L -> "TOURNAMENT FULL"
                                    tournament.entryFee > 0.0 -> "JOIN TOURNAMENT (${tournament.entryFee.toInt()} COINS)"
                                    else -> "JOIN FREE TOURNAMENT"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    val activeTour = tournament ?: return

    if (showJoinDialog) {
        JoinTournamentDialog(
            tournament = activeTour,
            profile = profile,
            walletBalance = wallet.totalBalance,
            isAlreadyRegistered = currentUserRegistration != null,
            onDismiss = { showJoinDialog = false },
            onConfirmJoin = { ign, igUid, onDone ->
                onJoinTournament(activeTour, ign, igUid) { ok ->
                    onDone(ok)
                    if (ok) {
                        showJoinDialog = false
                        selectedTab = MatchDetailsTab.JOINED_MEMBERS
                    }
                }
            }
        )
    }

    if (showResultsDialog) {
        TournamentResultsSheetDialog(
            tournament = activeTour,
            results = results,
            currentUserId = currentUserId,
            onDismiss = { showResultsDialog = false }
        )
    }
}

@Composable
private fun DetailMetric(label: String, value: String, accent: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(value, style = MaterialTheme.typography.titleSmall, color = accent, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun OverviewLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

internal fun orderTournamentParticipants(
    tournamentId: String,
    participants: List<TournamentParticipant>,
    currentUserRegistration: Registration?,
    currentUserId: String
): List<TournamentParticipant> {
    val baseList = participants.sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }.toMutableList()
    if (currentUserRegistration != null && baseList.none { it.userId == currentUserId }) {
        baseList.add(
            TournamentParticipant(
                id = currentUserId,
                tournamentId = tournamentId,
                userId = currentUserId,
                playerName = currentUserRegistration.inGameName.ifBlank {
                    currentUserRegistration.username.ifBlank { "Player" }
                },
                username = currentUserRegistration.username,
                createdAt = currentUserRegistration.createdAt
            )
        )
    }
    val currentUserEntry = baseList.firstOrNull { it.userId == currentUserId }
    return if (currentUserEntry != null) {
        listOf(currentUserEntry) + baseList.filter { it.userId != currentUserId }
    } else {
        baseList
    }
}
