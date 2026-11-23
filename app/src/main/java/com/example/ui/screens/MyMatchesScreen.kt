package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.MatchRecord
import com.example.model.MatchResult
import com.example.model.Registration
import com.example.model.RoomCredential
import com.example.model.Tournament
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
import com.example.ui.theme.SpiderSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MyMatchesScreen(
    initialTab: String,
    currentUserId: String,
    registrations: List<Registration>,
    tournaments: List<Tournament>,
    matches: List<MatchRecord>,
    results: List<MatchResult>,
    roomCredentialsCache: Map<String, RoomCredential>,
    onBack: () -> Unit,
    onRequestRoomCredential: (String) -> Unit,
    onSubmitDispute: (String, String, String, (Boolean) -> Unit) -> Unit
) {
    val tabs = listOf("ONGOING", "UPCOMING", "COMPLETED")
    val startIdx = tabs.indexOf(initialTab.uppercase()).let { if (it >= 0) it else 1 }
    var selectedTabIndex by remember { mutableIntStateOf(startIdx) }
    var disputeTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var activeResultsTournament by remember { mutableStateOf<Tournament?>(null) }

    val regMap = remember(registrations) { registrations.associateBy { it.tournamentId } }
    val registeredTournaments = remember(tournaments, regMap) {
        tournaments.filter { it.id in regMap.keys }
    }
    val tourIdsWithResults = remember(results) { results.map { it.tournamentId }.toSet() }
    val completedList = remember(tournaments, results, regMap, currentUserId, tourIdsWithResults) {
        val existingCompleted = tournaments.filter { it.status == "COMPLETED" || it.id in tourIdsWithResults }
        val knownIds = existingCompleted.map { it.id }.toSet()
        val synthetic = results
            .filter { it.tournamentId !in knownIds }
            .groupBy { it.tournamentId }
            .map { (tid, list) ->
                Tournament(
                    id = tid,
                    title = list.firstOrNull()?.tournamentTitle ?: "Completed Match",
                    gameMode = "MATCH",
                    mapName = "Arena",
                    status = "COMPLETED",
                    maxParticipants = 48L
                )
            }
        (existingCompleted + synthetic).sortedByDescending { t ->
            val isUserIn = (t.id in regMap) || results.any { it.tournamentId == t.id && it.userId == currentUserId }
            if (isUserIn) 1 else 0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpiderBlack)
    ) {
        SpiderBackTopBar(title = "My Matches", onBack = onBack)

        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = SpiderCardDark,
            contentColor = TextPrimary,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = SpiderRed
                )
            }
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTabIndex == idx,
                    onClick = { selectedTabIndex = idx },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selectedTabIndex == idx) SpiderRedBright else TextSecondary
                        )
                    },
                    modifier = Modifier.testTag("matches_tab_$title")
                )
            }
        }

        val currentTab = tabs[selectedTabIndex]

        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (currentTab) {
                "ONGOING" -> {
                    val liveTours = registeredTournaments.filter { it.status == "LIVE" || it.roomReleased }
                    val liveMatches = matches.filter { it.status == "ONGOING" }
                    if (liveTours.isEmpty() && liveMatches.isEmpty()) {
                        item {
                            SpiderEmptyState(
                                title = "No Ongoing Matches",
                                subtitle = "Registered matches go live here when room credentials are released (5 minutes before start)."
                            )
                        }
                    } else {
                        items(liveTours, key = { it.id }) { tour ->
                            LaunchedEffect(tour.id, tour.roomReleased) {
                                if (tour.roomReleased) onRequestRoomCredential(tour.id)
                            }
                            RegisteredMatchCard(
                                tournament = tour,
                                registration = regMap[tour.id],
                                credential = roomCredentialsCache[tour.id],
                                onFetchRoom = { onRequestRoomCredential(tour.id) },
                                onRaiseDispute = { disputeTarget = tour.id to tour.title }
                            )
                        }
                    }
                }
                "UPCOMING" -> {
                    val upcomingTours = registeredTournaments.filter { it.status == "UPCOMING" && !it.roomReleased }
                    if (upcomingTours.isEmpty()) {
                        item {
                            SpiderEmptyState(
                                title = "No Upcoming Registrations",
                                subtitle = "Browse TOURNAMENT or SOLO modes on the PLAY tab to register for upcoming matches."
                            )
                        }
                    } else {
                        items(upcomingTours, key = { it.id }) { tour ->
                            RegisteredMatchCard(
                                tournament = tour,
                                registration = regMap[tour.id],
                                credential = roomCredentialsCache[tour.id],
                                onFetchRoom = { onRequestRoomCredential(tour.id) },
                                onRaiseDispute = { disputeTarget = tour.id to tour.title }
                            )
                        }
                    }
                }
                "COMPLETED" -> {
                    if (completedList.isEmpty()) {
                        item {
                            SpiderEmptyState(
                                title = "No Completed Matches Yet",
                                subtitle = "Completed match banners and full 1–48 player results appear here once verified by Admin."
                            )
                        }
                    } else {
                        items(completedList, key = { "comp_${it.id}" }) { tour ->
                            val tourResults = remember(results, tour.id) { results.filter { it.tournamentId == tour.id } }
                            val myRes = remember(tourResults, currentUserId) {
                                tourResults.firstOrNull { it.userId == currentUserId }
                            }
                            val isJoined = tour.id in regMap
                            CompletedBannerCard(
                                tournament = tour,
                                rankedCount = tourResults.size,
                                myResult = myRes,
                                isJoined = isJoined,
                                onOpenResults = { activeResultsTournament = tour },
                                onRaiseDispute = if (isJoined || myRes != null) {
                                    { disputeTarget = tour.id to tour.title }
                                } else null
                            )
                        }
                    }
                }
            }
        }
    }

    activeResultsTournament?.let { tour ->
        val isJoinedOrPlaced = (tour.id in regMap) || results.any { it.tournamentId == tour.id && it.userId == currentUserId }
        TournamentResultsSheetDialog(
            tournament = tour,
            results = results,
            currentUserId = currentUserId,
            onDismiss = { activeResultsTournament = null },
            onRaiseDispute = if (isJoinedOrPlaced) {
                {
                    activeResultsTournament = null
                    disputeTarget = tour.id to tour.title
                }
            } else null
        )
    }

    disputeTarget?.let { (tourId, title) ->
        var reason by remember { mutableStateOf("") }
        var sending by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { disputeTarget = null },
            containerColor = SpiderCardDark,
            title = { Text("Raise Dispute: $title", color = TextPrimary, style = MaterialTheme.typography.titleMedium) },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Describe issue (kills, rank, fair-play)") },
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SpiderRed,
                        unfocusedBorderColor = SpiderCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        sending = true
                        onSubmitDispute(tourId, tourId, reason) { ok ->
                            sending = false
                            if (ok) disputeTarget = null
                        }
                    },
                    enabled = !sending && reason.trim().length >= 5,
                    colors = ButtonDefaults.buttonColors(containerColor = SpiderRed)
                ) { Text("Submit Dispute") }
            },
            dismissButton = {
                TextButton(onClick = { disputeTarget = null }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }
}

@Composable
private fun RegisteredMatchCard(
    tournament: Tournament,
    registration: Registration?,
    credential: RoomCredential?,
    onFetchRoom: () -> Unit,
    onRaiseDispute: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SpiderCardDark,
        border = BorderStroke(1.dp, SpiderCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tournament.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                StatusPill(text = tournament.status, color = statusColor(tournament.status))
            }
            Text(
                "${tournament.gameMode} • ${tournament.mapName} • ${tournament.matchDate} ${tournament.matchTime}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            if (registration != null) {
                Text(
                    "Registered IGN: ${registration.inGameName} (UID: ${registration.inGameUid})",
                    style = MaterialTheme.typography.labelSmall,
                    color = SpiderEmerald
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SpiderSurfaceElevated,
                border = BorderStroke(1.dp, SpiderCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (credential != null && credential.released) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Key, null, tint = SpiderEmerald, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("ROOM CREDENTIALS", style = MaterialTheme.typography.labelLarge, color = SpiderEmerald)
                        }
                        Text("Room ID: ${credential.roomId}", style = MaterialTheme.typography.labelLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Password: ${credential.roomPassword}", style = MaterialTheme.typography.labelLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Lock, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Room ID & Password unlock 5 mins before start (registered players only).",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        if (tournament.roomReleased) {
                            TextButton(onClick = onFetchRoom) {
                                Text("Reveal", color = SpiderRedBright)
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onRaiseDispute) {
                    Text("Report Issue / Dispute", color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun CompletedBannerCard(
    tournament: Tournament,
    rankedCount: Int,
    myResult: MatchResult? = null,
    isJoined: Boolean = false,
    onOpenResults: () -> Unit,
    onRaiseDispute: (() -> Unit)? = null
) {
    Surface(
        onClick = onOpenResults,
        shape = RoundedCornerShape(16.dp),
        color = SpiderCardDark,
        border = BorderStroke(1.dp, SpiderGold.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("completed_banner_${tournament.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(124.dp)
                    .clickable(onClick = onOpenResults)
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
                            Brush.verticalGradient(listOf(Color.Transparent, SpiderBlack.copy(alpha = 0.94f)))
                        )
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusPill(text = tournament.gameMode, color = SpiderRedBright)
                    if (tournament.mapName.isNotBlank()) {
                        StatusPill(text = tournament.mapName, color = TextPrimary)
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    StatusPill(text = "TAP FOR 1–48 RESULTS", color = SpiderGold)
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = tournament.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${tournament.matchDate} ${tournament.matchTime} • Prize: ${tournament.prizePool.toInt()} Coins • Per Kill: ${tournament.perKillReward.toInt()} Coins",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (myResult != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SpiderEmerald.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, SpiderEmerald.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Your Rank #${myResult.placement} • ${myResult.kills} Kills",
                                style = MaterialTheme.typography.labelLarge,
                                color = SpiderEmerald
                            )
                            Text(
                                text = "+${myResult.rewardCoins.toInt()} Coins Won",
                                style = MaterialTheme.typography.labelLarge,
                                color = SpiderGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (isJoined) {
                    Text(
                        text = "You joined this match • Tap banner to inspect all 48 positions",
                        style = MaterialTheme.typography.labelSmall,
                        color = SpiderEmerald
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onOpenResults,
                        colors = ButtonDefaults.buttonColors(containerColor = SpiderGold, contentColor = SpiderBlack),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Filled.EmojiEvents, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (rankedCount > 0) "VIEW FULL LIST ($rankedCount/48)" else "VIEW FULL LIST (1–48)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (onRaiseDispute != null) {
                        OutlinedButton(
                            onClick = onRaiseDispute,
                            border = BorderStroke(1.dp, SpiderCardBorder),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(Icons.Filled.ReportProblem, "Dispute", tint = SpiderRedBright, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

