package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.Announcement
import com.example.model.LeaderboardEntry
import com.example.model.MatchResult
import com.example.model.ReferralRecord
import com.example.model.Tournament
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.WalletTransaction
import com.example.ui.components.SpiderArtworkImage
import com.example.ui.components.SpiderEmptyState
import com.example.ui.components.SpiderSectionTitle
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
import com.example.ui.theme.WalletPurpleEnd
import com.example.ui.theme.WalletPurpleStart
import java.util.Locale

@Composable
fun EarnScreen(
    profile: UserProfile?,
    announcements: List<Announcement>,
    myReferrals: List<ReferralRecord> = emptyList(),
    myReferralReceived: ReferralRecord? = null,
    referralTransactions: List<WalletTransaction> = emptyList(),
    onApplyReferralCode: (String, (Boolean, String?) -> Unit) -> Unit = { _, done -> done(false, null) }
) {
    val context = LocalContext.current
    val referralCode = profile?.referralCode?.ifBlank { "SPIDER2026" } ?: "SPIDER2026"
    var copiedNotice by remember { mutableStateOf(false) }
    var applyCodeInput by remember { mutableStateOf("") }
    var applyStatus by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var applying by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpiderBlack)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Text(
            text = "EARN & REFERRALS",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        )

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, SpiderRed.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(listOf(WalletPurpleStart, WalletPurpleEnd)))
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CardGiftcard, null, tint = SpiderGold, modifier = Modifier.size(26.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("Invite Squadmates to Spider Esports", style = MaterialTheme.typography.titleLarge, color = Color.White)
                            }
                            Text(
                                "Share your unique referral code. Any promotional credits are strictly authorized and verified server-side — never generated automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SpiderBlack.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("YOUR REFERRAL CODE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                        Text(
                                            referralCode,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = SpiderGold,
                                            modifier = Modifier.testTag("referral_code_text")
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cm.setPrimaryClip(ClipData.newPlainText("Spider Referral", referralCode))
                                            copiedNotice = true
                                        },
                                        modifier = Modifier.testTag("copy_referral_button")
                                    ) {
                                        Icon(Icons.Filled.ContentCopy, "Copy code", tint = Color.White)
                                    }
                                }
                            }

                            if (copiedNotice) {
                                Text("Copied referral code to clipboard!", style = MaterialTheme.typography.labelSmall, color = SpiderEmerald)
                            }

                            Button(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Join me on SPIDER ESPORTS! Play fair and compete in tournaments. Use my referral code: $referralCode"
                                        )
                                    }
                                    runCatching {
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Spider Esports"))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SpiderRed, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("share_app_button")
                            ) {
                                Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("SHARE APPLICATION", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            item {
                val totalBonusCoins = myReferrals.filter { it.status == "CREDITED" }.sumOf { it.bonusAmount }
                SpiderSectionTitle(title = "REFERRAL TRACKING", subtitle = "Verified attribution & bonus transactions")
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SpiderCardDark,
                    border = BorderStroke(1.dp, SpiderCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Referred Friends: ${myReferrals.size}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(
                                "Earned: +${String.format(Locale.US, "%.0f", totalBonusCoins)} Coins",
                                style = MaterialTheme.typography.titleSmall,
                                color = SpiderGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "Referred By: ${profile?.referredBy?.ifBlank { "Direct Join" } ?: "Direct Join"}${myReferralReceived?.let { " (${it.status})" } ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        if (profile != null && profile.referredBy.isBlank() && myReferralReceived == null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = applyCodeInput,
                                    onValueChange = { applyCodeInput = it.uppercase() },
                                    label = { Text("Have a referral code?") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SpiderRed,
                                        unfocusedBorderColor = SpiderCardBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier.weight(1f).testTag("earn_apply_referral_input")
                                )
                                Button(
                                    onClick = {
                                        applying = true
                                        applyStatus = null
                                        onApplyReferralCode(applyCodeInput) { ok, err ->
                                            applying = false
                                            applyStatus = if (ok) {
                                                applyCodeInput = ""
                                                true to "Referral linked! Pending verification."
                                            } else {
                                                false to (err ?: "Invalid referral code")
                                            }
                                        }
                                    },
                                    enabled = !applying && applyCodeInput.trim().length >= 4,
                                    colors = ButtonDefaults.buttonColors(containerColor = SpiderRed),
                                    modifier = Modifier.height(52.dp).testTag("earn_apply_referral_button")
                                ) { Text("Apply") }
                            }
                            applyStatus?.let { (ok, msg) ->
                                Text(msg, style = MaterialTheme.typography.labelSmall, color = if (ok) SpiderEmerald else SpiderRedBright)
                            }
                        }
                        Text(
                            "Fair-Play Policy: Spider Esports does not offer daily login rewards, daily free coin claims, or automated free coin generation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }

            if (myReferrals.isNotEmpty()) {
                item { SpiderSectionTitle(title = "MY INVITED SQUAD (${myReferrals.size})") }
                itemsIndexed(myReferrals) { _, ref ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SpiderCardDark,
                        border = BorderStroke(1.dp, SpiderCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(ref.referredUsername, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("Code: ${ref.referralCode}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                StatusPill(ref.status, statusColor(ref.status))
                                if (ref.bonusAmount > 0.0) {
                                    Text("+${ref.bonusAmount.toInt()} Coins", style = MaterialTheme.typography.labelSmall, color = SpiderGold)
                                }
                            }
                        }
                    }
                }
            }

            if (referralTransactions.isNotEmpty()) {
                item { SpiderSectionTitle(title = "REFERRAL BONUS TRANSACTIONS") }
                itemsIndexed(referralTransactions) { _, tx ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SpiderCardDark,
                        border = BorderStroke(1.dp, SpiderCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tx.description, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("Balance after credit: ${String.format(Locale.US, "%.2f", tx.resultingBalance)} Coins", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            Text("+${tx.amount.toInt()} Coins", style = MaterialTheme.typography.titleSmall, color = SpiderEmerald, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            val promos = announcements.filter { it.category == "IMPORTANT" }
            if (promos.isNotEmpty()) {
                item { SpiderSectionTitle(title = "ELIGIBLE PROMOTIONAL ACTIVITIES") }
                itemsIndexed(promos) { _, ann ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SpiderCardDark,
                        border = BorderStroke(1.dp, SpiderCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(ann.title, style = MaterialTheme.typography.titleSmall, color = SpiderGold)
                            Text(ann.body, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RankingScreen(
    leaderboardsState: UiState<List<LeaderboardEntry>>,
    tournamentsState: UiState<List<Tournament>> = UiState.Success(emptyList()),
    results: List<MatchResult> = emptyList(),
    currentUserId: String = ""
) {
    val modes = listOf(
        "OVERALL" to "Overall",
        "WEEKLY" to "Weekly",
        "MONTHLY" to "Monthly",
        "SOLO" to "Solo",
        "DUO" to "Duo",
        "LONE_WOLF" to "Lone Wolf",
        "CLASH_SQUAD" to "Clash Squad"
    )
    var selectedSection by remember { mutableIntStateOf(0) } // 0 = Player Ranking, 1 = Match Results (1-48)
    var selectedMode by remember { mutableStateOf("OVERALL") }
    var activeResultsTournament by remember { mutableStateOf<Tournament?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpiderBlack)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("RANKING & RESULTS", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Text("Verified leaderboards & full 1–48 match results", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Icon(Icons.Filled.EmojiEvents, null, tint = SpiderGold, modifier = Modifier.size(28.dp))
        }

        // Section switcher: Player Ranking vs Match Results (1-48)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SpiderCardDark)
                .border(1.dp, SpiderCardBorder, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("PLAYER RANKING", "MATCH RESULTS (1–48)").forEachIndexed { idx, label ->
                val selected = selectedSection == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (selected) SpiderRed else SpiderSurfaceElevated)
                        .clickable { selectedSection = idx }
                        .padding(vertical = 10.dp)
                        .testTag(if (idx == 0) "ranking_tab_players" else "ranking_tab_match_results"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) Color.White else TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (selectedSection == 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                modes.forEach { (key, label) ->
                    val active = selectedMode == key
                    FilterChip(
                        selected = active,
                        onClick = { selectedMode = key },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SpiderRed,
                            selectedLabelColor = Color.White,
                            containerColor = SpiderCardDark,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = active,
                            borderColor = SpiderCardBorder,
                            selectedBorderColor = SpiderRed
                        )
                    )
                }
            }

            when (leaderboardsState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SpiderRed)
                }
                is UiState.Error -> Box(Modifier.padding(16.dp)) {
                    SpiderEmptyState(title = "Unable to load rankings", subtitle = leaderboardsState.message)
                }
                is UiState.Success -> {
                    val filtered = remember(leaderboardsState.data, selectedMode) {
                        leaderboardsState.data
                            .filter { it.mode == selectedMode && it.verified }
                            .sortedByDescending { it.rankingPoints }
                    }
                    if (filtered.isEmpty()) {
                        Box(Modifier.padding(16.dp)) {
                            SpiderEmptyState(
                                title = "No Verified Rankings Yet",
                                subtitle = "Rankings populate automatically as Admins verify tournament match results."
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(filtered, key = { idx, item -> "${item.id}_$idx" }) { index, entry ->
                                val rank = index + 1
                                val rankColor = when (rank) {
                                    1 -> SpiderGold
                                    2 -> SpiderSilver
                                    3 -> SpiderBronze
                                    else -> TextMuted
                                }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = SpiderCardDark,
                                    border = BorderStroke(1.dp, if (rank <= 3) rankColor.copy(alpha = 0.55f) else SpiderCardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(rankColor.copy(alpha = 0.18f))
                                                .border(1.dp, rankColor, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("#$rank", style = MaterialTheme.typography.labelLarge, color = rankColor)
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(SpiderSurfaceElevated)
                                        ) {
                                            SpiderArtworkImage(
                                                imageUrl = entry.photoUrl,
                                                presetKey = "logo",
                                                contentDescription = entry.username,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(entry.username, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                                Spacer(Modifier.width(4.dp))
                                                Icon(Icons.Filled.Verified, null, tint = SpiderEmerald, modifier = Modifier.size(14.dp))
                                            }
                                            Text(
                                                "Matches: ${entry.matches} • Wins: ${entry.wins} • Kills: ${entry.verifiedKills}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("${entry.rankingPoints}", style = MaterialTheme.typography.titleMedium, color = SpiderRedBright, fontWeight = FontWeight.Bold)
                                            Text("PTS", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val allTours = (tournamentsState as? UiState.Success)?.data.orEmpty()
            val tourIdsWithResults = remember(results) { results.map { it.tournamentId }.toSet() }
            val completedTours = remember(allTours, results, tourIdsWithResults) {
                val existing = allTours.filter { it.status == "COMPLETED" || it.id in tourIdsWithResults }
                val knownIds = existing.map { it.id }.toSet()
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
                existing + synthetic
            }

            if (completedTours.isEmpty()) {
                Box(Modifier.padding(16.dp)) {
                    SpiderEmptyState(
                        title = "No Match Results Published Yet",
                        subtitle = "Tap any completed match banner here to see all 1–48 player names, kills, and prize coins won."
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(completedTours, key = { "rank_res_${it.id}" }) { tour ->
                        val tourResults = remember(results, tour.id) { results.filter { it.tournamentId == tour.id } }
                        val myRes = remember(tourResults, currentUserId) {
                            tourResults.firstOrNull { it.userId == currentUserId }
                        }
                        CompletedBannerCard(
                            tournament = tour,
                            rankedCount = tourResults.size,
                            myResult = myRes,
                            isJoined = myRes != null,
                            onOpenResults = { activeResultsTournament = tour }
                        )
                    }
                }
            }
        }
    }

    activeResultsTournament?.let { tour ->
        TournamentResultsSheetDialog(
            tournament = tour,
            results = results,
            currentUserId = currentUserId,
            onDismiss = { activeResultsTournament = null }
        )
    }
}
