package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.example.model.Registration
import com.example.model.Tournament
import com.example.model.TournamentCategory
import com.example.model.TournamentFilter
import com.example.model.TournamentSort
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.components.SpiderArtworkImage
import com.example.ui.components.SpiderBackTopBar
import com.example.ui.components.SpiderEmptyState
import com.example.ui.components.StatusPill
import com.example.ui.components.statusColor
import com.example.ui.theme.SpiderAmber
import com.example.ui.theme.SpiderBlack
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

@Composable
fun TournamentListScreen(
    category: TournamentCategory?,
    tournamentsState: UiState<List<Tournament>>,
    registrations: List<Registration>,
    profile: UserProfile?,
    onBack: () -> Unit,
    onJoinTournament: (Tournament, String, String, (Boolean) -> Unit) -> Unit
) {
    var selectedFilter by remember { mutableStateOf(TournamentFilter.ALL) }
    var selectedSort by remember { mutableStateOf(TournamentSort.STARTING_SOON) }
    var sortExpanded by remember { mutableStateOf(false) }
    var activeJoinTournament by remember { mutableStateOf<Tournament?>(null) }

    val registeredIds = remember(registrations) { registrations.map { it.tournamentId }.toSet() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpiderBlack)
    ) {
        SpiderBackTopBar(
            title = category?.categoryName ?: "Tournaments",
            onBack = onBack,
            trailing = {
                Box {
                    OutlinedButton(
                        onClick = { sortExpanded = true },
                        border = BorderStroke(1.dp, SpiderCardBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.FilterList, null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(selectedSort.label, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                    }
                    DropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false },
                        containerColor = SpiderSurfaceElevated
                    ) {
                        TournamentSort.entries.forEach { sort ->
                            DropdownMenuItem(
                                text = { Text(sort.label, color = TextPrimary) },
                                onClick = {
                                    selectedSort = sort
                                    sortExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TournamentFilter.entries.forEach { filter ->
                val selected = selectedFilter == filter
                FilterChip(
                    selected = selected,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter.label, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SpiderRed,
                        selectedLabelColor = Color.White,
                        containerColor = SpiderCardDark,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = SpiderCardBorder,
                        selectedBorderColor = SpiderRed
                    )
                )
            }
        }

        when (tournamentsState) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SpiderRed)
            }
            is UiState.Error -> Box(Modifier.padding(16.dp)) {
                SpiderEmptyState(title = "Failed to load tournaments", subtitle = tournamentsState.message)
            }
            is UiState.Success -> {
                val filtered = remember(tournamentsState.data, category?.id, selectedFilter, selectedSort) {
                    tournamentsState.data
                        .filter { category == null || it.categoryId == category.id || it.gameMode.equals(category.categoryName, ignoreCase = true) }
                        .filter { t ->
                            when (selectedFilter) {
                                TournamentFilter.ALL -> true
                                TournamentFilter.UPCOMING -> t.status == "UPCOMING"
                                TournamentFilter.LIVE -> t.status == "LIVE"
                                TournamentFilter.FULL -> t.status == "FULL" || t.registeredCount >= t.maxParticipants
                                TournamentFilter.COMPLETED -> t.status == "COMPLETED"
                            }
                        }
                        .let { list ->
                            when (selectedSort) {
                                TournamentSort.LOWEST_ENTRY -> list.sortedBy { it.entryFee }
                                TournamentSort.HIGHEST_PRIZE -> list.sortedByDescending { it.prizePool }
                                TournamentSort.RECENTLY_ADDED -> list.sortedByDescending { it.createdAt?.seconds ?: 0L }
                                TournamentSort.STARTING_SOON -> list.sortedBy { it.matchTime }
                            }
                        }
                }

                if (filtered.isEmpty()) {
                    Box(Modifier.padding(16.dp)) {
                        SpiderEmptyState(
                            title = "No Matches Found",
                            subtitle = "No tournaments match this category and filter yet. Admin can publish new matches anytime."
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(filtered, key = { it.id }) { tour ->
                            TournamentDetailCard(
                                tournament = tour,
                                isRegistered = tour.id in registeredIds,
                                onJoinClick = { activeJoinTournament = tour }
                            )
                        }
                    }
                }
            }
        }
    }

    activeJoinTournament?.let { tour ->
        JoinTournamentDialog(
            tournament = tour,
            profile = profile,
            isAlreadyRegistered = tour.id in registeredIds,
            onDismiss = { activeJoinTournament = null },
            onConfirmJoin = { ign, igUid, onDone ->
                onJoinTournament(tour, ign, igUid) { ok ->
                    onDone(ok)
                    if (ok) activeJoinTournament = null
                }
            }
        )
    }
}

@Composable
private fun TournamentDetailCard(
    tournament: Tournament,
    isRegistered: Boolean,
    onJoinClick: () -> Unit
) {
    val maxP = tournament.maxParticipants.coerceAtLeast(1L)
    val progress = (tournament.registeredCount.toFloat() / maxP.toFloat()).coerceIn(0f, 1f)
    val slotsLeft = (tournament.maxParticipants - tournament.registeredCount).coerceAtLeast(0L)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SpiderCardDark,
        border = BorderStroke(1.dp, SpiderCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp)
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
                            Brush.verticalGradient(listOf(Color.Transparent, SpiderBlack.copy(alpha = 0.92f)))
                        )
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusPill(text = tournament.gameMode, color = SpiderRedBright)
                    StatusPill(text = tournament.mapName, color = TextPrimary)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    StatusPill(text = tournament.status, color = statusColor(tournament.status))
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(tournament.title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    if (tournament.shortDescription.isNotBlank()) {
                        Text(tournament.shortDescription, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
                    }
                }
            }

            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem("ENTRY FEE", if (tournament.entryFee <= 0.0) "FREE" else "${tournament.entryFee.toInt()} Coins")
                    MetricItem("PRIZE POOL", if (tournament.prizePool <= 0.0) "Points" else "${tournament.prizePool.toInt()} Coins")
                    MetricItem("PER KILL", if (tournament.perKillReward <= 0.0) "0 Coins" else "${tournament.perKillReward.toInt()} Coins")
                    MetricItem("SLOTS", "${tournament.registeredCount}/${tournament.maxParticipants}")
                }

                LinearProgressIndicator(
                    progress = { progress },
                    color = SpiderRed,
                    trackColor = SpiderSurfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Schedule, null, tint = TextMuted, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "${tournament.matchDate} • ${tournament.matchTime} (Reg ends ${tournament.registrationDeadline})",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Button(
                    onClick = onJoinClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRegistered) SpiderEmerald else SpiderRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("join_tournament_${tournament.id}")
                ) {
                    Text(
                        text = when {
                            isRegistered -> "ALREADY REGISTERED • VIEW DETAILS"
                            slotsLeft == 0L -> "TOURNAMENT FULL"
                            else -> "JOIN TOURNAMENT"
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(value, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
    }
}

@Composable
fun JoinTournamentDialog(
    tournament: Tournament,
    profile: UserProfile?,
    isAlreadyRegistered: Boolean,
    onDismiss: () -> Unit,
    onConfirmJoin: (String, String, (Boolean) -> Unit) -> Unit
) {
    var ign by remember(profile) { mutableStateOf(profile?.inGameName?.ifBlank { profile.displayName } ?: "") }
    var igUid by remember(profile) { mutableStateOf(profile?.inGameUid ?: "") }
    var submitting by remember { mutableStateOf(false) }

    val isPaid = tournament.entryFee > 0.0
    val isSuspended = profile?.status == "SUSPENDED"
    val isFull = tournament.registeredCount >= tournament.maxParticipants

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpiderCardDark,
        title = {
            Text("Join ${tournament.title}", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Verification checklist
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SpiderSurfaceElevated,
                    border = BorderStroke(1.dp, SpiderCardBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.VerifiedUser, null, tint = if (isSuspended) SpiderRedBright else SpiderEmerald, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (isSuspended) "Account Suspended" else "Account Verified (${profile?.username ?: "Player"})",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                        }
                        Text("Mode: ${tournament.gameMode} • Map: ${tournament.mapName}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            "Entry Fee: ${if (isPaid) "${tournament.entryFee.toInt()} Coins" else "FREE"} • Slots: ${tournament.registeredCount}/${tournament.maxParticipants}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SpiderGold
                        )
                        Text(
                            "Prize Pool: ${if (tournament.prizePool <= 0.0) "Rank Pts" else "${tournament.prizePool.toInt()} Coins"} • Per Kill: ${tournament.perKillReward.toInt()} Coins",
                            style = MaterialTheme.typography.bodySmall,
                            color = SpiderGold
                        )
                    }
                }

                // Rules box
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Gavel, null, tint = SpiderRedBright, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Tournament Rules", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                }
                Text(tournament.rules, style = MaterialTheme.typography.bodySmall, color = TextSecondary)

                if (isPaid) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SpiderAmber.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, SpiderAmber.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Filled.Lock, null, tint = SpiderAmber, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Paid-entry and real-money prize operations remain disabled until required legal, age, and verified payment-gateway approvals are completed. No coins will be deducted.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SpiderAmber
                            )
                        }
                    }
                } else if (!isAlreadyRegistered && !isFull && !isSuspended) {
                    OutlinedTextField(
                        value = ign,
                        onValueChange = { ign = it },
                        label = { Text("In-Game Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SpiderRed,
                            unfocusedBorderColor = SpiderCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = igUid,
                        onValueChange = { igUid = it },
                        label = { Text("In-Game UID") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SpiderRed,
                            unfocusedBorderColor = SpiderCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (!isPaid && !isAlreadyRegistered && !isFull && !isSuspended) {
                Button(
                    onClick = {
                        submitting = true
                        onConfirmJoin(ign, igUid) { submitting = false }
                    },
                    enabled = !submitting && ign.isNotBlank() && igUid.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = SpiderRed)
                ) {
                    if (submitting) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Confirm Free Entry", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        }
    )
}
