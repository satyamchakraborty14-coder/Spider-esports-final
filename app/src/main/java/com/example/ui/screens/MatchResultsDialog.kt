package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.MatchResult
import com.example.model.Registration
import com.example.model.Tournament
import com.example.model.UserProfile
import com.example.ui.components.SpiderArtworkImage
import com.example.ui.components.StatusPill
import com.example.ui.components.statusColor
import com.example.ui.theme.SpiderBlack
import com.example.ui.theme.SpiderBronze
import com.example.ui.theme.SpiderCardBorder
import com.example.ui.theme.SpiderCardDark
import com.example.ui.theme.SpiderEmerald
import com.example.ui.theme.SpiderGold
import com.example.ui.theme.SpiderRed
import com.example.ui.theme.SpiderRedBright
import com.example.ui.theme.SpiderSilver
import com.example.ui.theme.SpiderSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

const val TOTAL_RESULT_POSITIONS = 48

internal fun buildPositionedResults(
    resultsForTournament: List<MatchResult>,
    totalPositions: Int = TOTAL_RESULT_POSITIONS
): Map<Int, MatchResult> {
    val map = LinkedHashMap<Int, MatchResult>()
    resultsForTournament
        .sortedBy { it.placement }
        .forEach { res ->
            val pos = res.placement.toInt().coerceAtLeast(1)
            if (!map.containsKey(pos) || res.id.contains("_pos_")) {
                map[pos] = res
            }
        }
    return map
}

private fun formatCoinsShort(coins: Double): String {
    if (coins <= 0.0) return "0 Coins"
    return if (coins % 1.0 == 0.0) "${coins.toInt()} Coins" else String.format(java.util.Locale.US, "%.1f Coins", coins)
}

@Composable
fun TournamentResultsSheetDialog(
    tournament: Tournament,
    results: List<MatchResult>,
    currentUserId: String,
    onDismiss: () -> Unit,
    onRaiseDispute: (() -> Unit)? = null
) {
    val tourResults = remember(results, tournament.id) {
        results.filter { it.tournamentId == tournament.id }
    }
    val positionMap = remember(tourResults) { buildPositionedResults(tourResults) }
    val totalSlots = remember(tournament.maxParticipants, positionMap) {
        maxOf(TOTAL_RESULT_POSITIONS, tournament.maxParticipants.toInt().coerceAtMost(500), positionMap.keys.maxOrNull() ?: 48)
    }
    var showFilledOnly by remember { mutableStateOf(false) }

    val positionsToShow = remember(totalSlots, showFilledOnly, positionMap) {
        val all = (1..totalSlots).toList()
        if (showFilledOnly) all.filter { positionMap.containsKey(it) } else all
    }
    val rankedCount = positionMap.size
    val totalCoinsAwarded = remember(positionMap) { positionMap.values.sumOf { it.rewardCoins } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SpiderCardDark,
            border = BorderStroke(1.dp, SpiderCardBorder),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("tournament_results_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Banner Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(126.dp)
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
                                Brush.verticalGradient(listOf(SpiderBlack.copy(alpha = 0.35f), SpiderBlack.copy(alpha = 0.95f)))
                            )
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .testTag("close_results_dialog_button")
                    ) {
                        Icon(Icons.Filled.Close, "Close results", tint = Color.White)
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusPill(text = tournament.status, color = statusColor(tournament.status))
                            StatusPill(text = "${tournament.gameMode} • ${tournament.mapName}", color = SpiderGold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = tournament.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Prize Pool: ${formatCoinsShort(tournament.prizePool)} • Per Kill: ${formatCoinsShort(tournament.perKillReward)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SpiderGold
                        )
                    }
                }

                // Stats & Filter Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MATCH RESULTS & RANKS (1–$totalSlots)",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "$rankedCount/$totalSlots Positions Filled • ${formatCoinsShort(totalCoinsAwarded)} Won",
                            style = MaterialTheme.typography.labelSmall,
                            color = SpiderEmerald
                        )
                    }
                    FilterChip(
                        selected = showFilledOnly,
                        onClick = { showFilledOnly = !showFilledOnly },
                        label = { Text(if (showFilledOnly) "Ranked ($rankedCount)" else "All $totalSlots", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SpiderRed,
                            selectedLabelColor = Color.White,
                            containerColor = SpiderSurfaceElevated,
                            labelColor = TextSecondary
                        )
                    )
                }

                // Table Column Header
                Surface(
                    color = SpiderSurfaceElevated,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.width(38.dp))
                        Text("PLAYER NAME", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.weight(1f))
                        Text("KILLS", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
                        Text("PRIZE WON", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.width(86.dp), textAlign = TextAlign.End)
                    }
                }

                // 1..48 Single-Line Result Rows
                if (positionsToShow.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No player results published yet. Check back soon after admin verification.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(positionsToShow, key = { it }) { pos ->
                            val entry = positionMap[pos]
                            val isSelf = entry != null && entry.userId.isNotBlank() && entry.userId == currentUserId
                            PositionResultLineRow(
                                position = pos,
                                entry = entry,
                                isSelf = isSelf,
                                isSelected = false
                            )
                        }
                    }
                }

                HorizontalDivider(color = SpiderCardBorder)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onRaiseDispute != null) {
                        OutlinedButton(
                            onClick = onRaiseDispute,
                            border = BorderStroke(1.dp, SpiderCardBorder),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.ReportProblem, null, tint = SpiderRedBright, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Raise Dispute", color = TextPrimary, style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = SpiderRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PositionResultLineRow(
    position: Int,
    entry: MatchResult?,
    isSelf: Boolean = false,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val rankColor = when (position) {
        1 -> SpiderGold
        2 -> SpiderSilver
        3 -> SpiderBronze
        else -> TextMuted
    }
    val rowBg = when {
        isSelected -> SpiderRed.copy(alpha = 0.22f)
        isSelf -> SpiderEmerald.copy(alpha = 0.14f)
        entry != null -> SpiderBlack.copy(alpha = 0.55f)
        else -> SpiderSurfaceElevated.copy(alpha = 0.32f)
    }
    val borderColor = when {
        isSelected -> SpiderRedBright
        isSelf -> SpiderEmerald
        position <= 3 && entry != null -> rankColor.copy(alpha = 0.45f)
        else -> SpiderCardBorder.copy(alpha = 0.5f)
    }

    val rowModifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(rowBg)
        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
        .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }
        .padding(horizontal = 10.dp, vertical = 9.dp)
        .testTag("result_position_row_$position")

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank badge (#1..#48)
        Box(
            modifier = Modifier
                .width(34.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(rankColor.copy(alpha = if (entry != null || position <= 3) 0.18f else 0.08f))
                .padding(vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#$position",
                style = MaterialTheme.typography.labelMedium,
                color = if (entry != null || position <= 3) rankColor else TextMuted,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(6.dp))

        // Player Name (single line)
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = entry?.username?.ifBlank { "Player #$position" } ?: if (onClick != null) "Tap to add #$position" else "—",
                style = MaterialTheme.typography.bodySmall,
                color = if (entry != null) TextPrimary else TextMuted,
                fontWeight = if (entry != null && position <= 3) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (isSelf) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = SpiderEmerald
                ) {
                    Text(
                        text = "YOU",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = SpiderBlack,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }

        // Kills
        Text(
            text = if (entry != null) "${entry.kills}" else "—",
            style = MaterialTheme.typography.bodySmall,
            color = if (entry != null && entry.kills > 0) TextPrimary else TextMuted,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.width(48.dp)
        )

        // Prize Won (Coins)
        Text(
            text = if (entry != null) formatCoinsShort(entry.rewardCoins) else "—",
            style = MaterialTheme.typography.labelMedium,
            color = when {
                entry == null -> TextMuted
                entry.rewardCoins > 0.0 -> SpiderGold
                else -> TextSecondary
            },
            fontWeight = if (entry != null && entry.rewardCoins > 0.0) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(86.dp)
        )
    }
}

@Composable
fun AdminTournamentResultsDialog(
    tournament: Tournament,
    users: List<UserProfile>,
    registrations: List<Registration>,
    results: List<MatchResult>,
    onDismiss: () -> Unit,
    onSavePosition: (
        playerName: String,
        linkedUser: UserProfile?,
        placement: Long,
        kills: Long,
        rewardCoins: Double,
        leaderboardMode: String,
        onDone: (Boolean, String?) -> Unit
    ) -> Unit,
    onClearPosition: (placement: Long, existingResultId: String) -> Unit,
    onMarkCompleted: () -> Unit
) {
    val tourResults = remember(results, tournament.id) {
        results.filter { it.tournamentId == tournament.id }
    }
    val positionMap = remember(tourResults) { buildPositionedResults(tourResults) }
    val totalSlots = remember(tournament.maxParticipants, positionMap) {
        maxOf(TOTAL_RESULT_POSITIONS, tournament.maxParticipants.toInt().coerceAtMost(500), positionMap.keys.maxOrNull() ?: 48)
    }
    val tourRegistrations = remember(registrations, tournament.id) {
        registrations.filter { it.tournamentId == tournament.id }
    }

    val firstEmptySlot = remember(positionMap, totalSlots) {
        (1..totalSlots).firstOrNull { !positionMap.containsKey(it) } ?: 1
    }

    var selectedPosition by remember(tournament.id) { mutableIntStateOf(firstEmptySlot) }
    val existingForSlot = positionMap[selectedPosition]

    var playerName by remember(selectedPosition, existingForSlot?.id) {
        mutableStateOf(existingForSlot?.username ?: "")
    }
    var selectedUser by remember(selectedPosition, existingForSlot?.id, users) {
        mutableStateOf(
            existingForSlot?.userId?.takeIf { it.isNotBlank() }?.let { uid ->
                users.firstOrNull { it.userId == uid }
            }
        )
    }
    var killsText by remember(selectedPosition, existingForSlot?.id) {
        mutableStateOf(existingForSlot?.kills?.toString() ?: "0")
    }
    var rewardText by remember(selectedPosition, existingForSlot?.id) {
        mutableStateOf(
            existingForSlot?.rewardCoins?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
            } ?: "0"
        )
    }
    var mode by remember(tournament.id) {
        mutableStateOf(
            when {
                tournament.gameMode.contains("LONE", ignoreCase = true) || tournament.gameMode.contains("LW", ignoreCase = true) -> "LONE_WOLF"
                tournament.gameMode.contains("CLASH", ignoreCase = true) || tournament.gameMode.contains("CS", ignoreCase = true) -> "CLASH_SQUAD"
                tournament.gameMode.contains("DUO", ignoreCase = true) -> "DUO"
                else -> "SOLO"
            }
        )
    }
    var saving by remember { mutableStateOf(false) }
    var inlineFeedback by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    fun loadPosition(pos: Int) {
        val clamped = pos.coerceIn(1, totalSlots)
        selectedPosition = clamped
        val existing = positionMap[clamped]
        playerName = existing?.username ?: ""
        selectedUser = existing?.userId?.takeIf { it.isNotBlank() }?.let { uid -> users.firstOrNull { it.userId == uid } }
        killsText = existing?.kills?.toString() ?: "0"
        rewardText = existing?.rewardCoins?.let {
            if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
        } ?: "0"
        inlineFeedback = null
    }

    // Auto-match user if typed name matches an app user or registered IGN
    val autoMatchedUser = remember(selectedUser, playerName, users, tourRegistrations) {
        selectedUser ?: run {
            val q = playerName.trim()
            if (q.isBlank()) null
            else {
                val regMatch = tourRegistrations.firstOrNull {
                    it.inGameName.equals(q, ignoreCase = true) || it.username.equals(q, ignoreCase = true)
                }
                if (regMatch != null) users.firstOrNull { it.userId == regMatch.userId }
                else users.firstOrNull {
                    it.inGameName.equals(q, ignoreCase = true) || it.username.equals(q, ignoreCase = true)
                }
            }
        }
    }

    // Build quick-pick candidates: registered players first, then filtered users
    val quickPickUsers = remember(tourRegistrations, users, playerName) {
        val regUids = tourRegistrations.map { it.userId }.toSet()
        val regEntries = tourRegistrations.mapNotNull { reg ->
            val u = users.firstOrNull { it.userId == reg.userId }
            val display = reg.inGameName.ifBlank { reg.username }
            Triple(display, u, true)
        }
        val otherUsers = users.filter { it.userId !in regUids }
            .filter {
                playerName.isBlank() ||
                    it.username.contains(playerName, ignoreCase = true) ||
                    it.inGameName.contains(playerName, ignoreCase = true)
            }
            .take(15)
            .map { u -> Triple(u.inGameName.ifBlank { u.username }, u, false) }
        (regEntries + otherUsers).distinctBy { it.second?.userId ?: it.first }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SpiderCardDark,
            border = BorderStroke(1.dp, SpiderCardBorder),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .testTag("admin_results_editor_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SpiderSurfaceElevated)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "RESULTS & RANKS (1–$totalSlots)",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${tournament.title} • ${positionMap.size}/$totalSlots Added",
                            style = MaterialTheme.typography.bodySmall,
                            color = SpiderGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (tournament.status != "COMPLETED") {
                        OutlinedButton(
                            onClick = onMarkCompleted,
                            border = BorderStroke(1.dp, SpiderEmerald),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, tint = SpiderEmerald, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Mark Done", color = SpiderEmerald, style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        StatusPill("COMPLETED", SpiderEmerald)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, "Close", tint = TextPrimary)
                    }
                }

                // One-by-One Entry Editor Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SpiderBlack.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, SpiderRed.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Position Stepper (#1 to #48)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    onClick = { loadPosition(selectedPosition - 1) },
                                    enabled = selectedPosition > 1,
                                    shape = CircleShape,
                                    color = SpiderSurfaceElevated,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous position", tint = if (selectedPosition > 1) TextPrimary else TextMuted)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SpiderRed.copy(alpha = 0.18f),
                                    border = BorderStroke(1.dp, SpiderRedBright)
                                ) {
                                    Text(
                                        text = "POSITION #$selectedPosition",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = SpiderGold,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                                Surface(
                                    onClick = { loadPosition(selectedPosition + 1) },
                                    enabled = selectedPosition < totalSlots,
                                    shape = CircleShape,
                                    color = SpiderSurfaceElevated,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next position", tint = if (selectedPosition < totalSlots) TextPrimary else TextMuted)
                                    }
                                }
                            }

                            if (existingForSlot != null) {
                                TextButton(
                                    onClick = {
                                        onClearPosition(selectedPosition.toLong(), existingForSlot.id)
                                        playerName = ""
                                        selectedUser = null
                                        killsText = "0"
                                        rewardText = "0"
                                    }
                                ) {
                                    Icon(Icons.Filled.DeleteOutline, null, tint = SpiderRedBright, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text("Clear #$selectedPosition", color = SpiderRedBright, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        // Quick-pick registered players / app users
                        if (quickPickUsers.isNotEmpty()) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Quick Pick:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                quickPickUsers.forEach { (display, userObj, isReg) ->
                                    val isPicked = (userObj != null && selectedUser?.userId == userObj.userId) ||
                                        playerName.equals(display, ignoreCase = true)
                                    FilterChip(
                                        selected = isPicked,
                                        onClick = {
                                            playerName = display
                                            selectedUser = userObj
                                        },
                                        leadingIcon = if (isReg) {
                                            { Icon(Icons.Filled.Person, null, modifier = Modifier.size(13.dp)) }
                                        } else null,
                                        label = {
                                            Text(
                                                text = if (isReg) "$display (Joined)" else display,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SpiderRed,
                                            selectedLabelColor = Color.White,
                                            containerColor = SpiderSurfaceElevated,
                                            labelColor = if (isReg) SpiderEmerald else TextSecondary
                                        )
                                    )
                                }
                            }
                        }

                        val fc = adminFieldColors()
                        OutlinedTextField(
                            value = playerName,
                            onValueChange = {
                                playerName = it
                                if (selectedUser != null &&
                                    !it.equals(selectedUser?.username, true) &&
                                    !it.equals(selectedUser?.inGameName, true)
                                ) {
                                    selectedUser = null
                                }
                            },
                            label = { Text("Player Name for #$selectedPosition*") },
                            supportingText = {
                                Text(
                                    text = if (autoMatchedUser != null) {
                                        "Linked to account @${autoMatchedUser.username} (Winning coins auto-credited)"
                                    } else {
                                        "Manual name (shown on 1–$totalSlots list; pick a joined player above to auto-credit wallet)"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (autoMatchedUser != null) SpiderEmerald else TextMuted
                                )
                            },
                            singleLine = true,
                            colors = fc,
                            modifier = Modifier.fillMaxWidth().testTag("admin_result_player_name_input")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = killsText,
                                onValueChange = { killsText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Kills") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = fc,
                                modifier = Modifier.weight(1f).testTag("admin_result_kills_input")
                            )
                            OutlinedTextField(
                                value = rewardText,
                                onValueChange = { rewardText = it.filterDecimalInput() },
                                label = { Text("Prize Won (Coins)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = fc,
                                modifier = Modifier.weight(1.3f).testTag("admin_result_reward_input")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (tournament.perKillReward > 0.0) {
                                val k = killsText.toLongOrNull() ?: 0L
                                val killBonus = k * tournament.perKillReward
                                OutlinedButton(
                                    onClick = {
                                        rewardText = if (killBonus % 1.0 == 0.0) killBonus.toInt().toString() else killBonus.toString()
                                    },
                                    border = BorderStroke(1.dp, SpiderGold.copy(alpha = 0.6f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Kill Coins (${killBonus.toInt()})", color = SpiderGold, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Row(
                                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("SOLO", "DUO", "LONE_WOLF", "CLASH_SQUAD").forEach { m ->
                                    FilterChip(
                                        selected = mode == m,
                                        onClick = { mode = m },
                                        label = { Text(m, style = MaterialTheme.typography.labelSmall) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SpiderRed,
                                            selectedLabelColor = Color.White,
                                            containerColor = SpiderSurfaceElevated,
                                            labelColor = TextSecondary
                                        )
                                    )
                                }
                            }
                        }

                        inlineFeedback?.let { (ok, msg) ->
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (ok) SpiderEmerald else SpiderRedBright
                            )
                        }

                        val nextPos = (selectedPosition + 1).coerceAtMost(totalSlots)
                        Button(
                            onClick = {
                                val cleanName = playerName.trim()
                                if (cleanName.isNotEmpty() && !saving) {
                                    saving = true
                                    inlineFeedback = null
                                    val savedPos = selectedPosition
                                    onSavePosition(
                                        cleanName,
                                        autoMatchedUser,
                                        savedPos.toLong(),
                                        killsText.toLongOrNull() ?: 0L,
                                        rewardText.toDoubleOrNull() ?: 0.0,
                                        mode
                                    ) { ok, msg ->
                                        saving = false
                                        if (ok) {
                                            inlineFeedback = true to "Saved #$savedPos ($cleanName)!"
                                            if (savedPos < totalSlots) {
                                                loadPosition(savedPos + 1)
                                            }
                                        } else {
                                            inlineFeedback = false to (msg ?: "Failed to save #$savedPos")
                                        }
                                    }
                                }
                            },
                            enabled = !saving && playerName.trim().isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = SpiderRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp).testTag("admin_save_result_position_button")
                        ) {
                            if (saving) {
                                CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.EmojiEvents, null, tint = SpiderGold, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (selectedPosition < totalSlots) {
                                        "SAVE #$selectedPosition & NEXT (#$nextPos)"
                                    } else {
                                        "SAVE POSITION #$selectedPosition"
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 1..48 Full Position List Header
                Surface(
                    color = SpiderSurfaceElevated,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.width(38.dp))
                        Text("PLAYER NAME (TAP ROW TO EDIT)", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.weight(1f))
                        Text("KILLS", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
                        Text("PRIZE WON", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.width(86.dp), textAlign = TextAlign.End)
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items((1..totalSlots).toList(), key = { it }) { pos ->
                        PositionResultLineRow(
                            position = pos,
                            entry = positionMap[pos],
                            isSelected = selectedPosition == pos,
                            onClick = { loadPosition(pos) }
                        )
                    }
                }
            }
        }
    }
}
