package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.model.Announcement
import com.example.model.NotificationItem
import com.example.model.SupportTicket
import com.example.model.TeamRoster
import com.example.ui.components.SpiderBackTopBar
import com.example.ui.components.SpiderEmptyState
import com.example.ui.components.SpiderSectionTitle
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
fun RulesScreen(
    announcements: List<Announcement>,
    onBack: () -> Unit
) {
    val ruleDocs = announcements.filter { it.category == "RULES" }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpiderBlack)
    ) {
        SpiderBackTopBar(title = "Spider Esports Rules", onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (ruleDocs.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SpiderCardDark,
                        border = BorderStroke(1.dp, SpiderCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Gavel, null, tint = SpiderRedBright)
                                Spacer(Modifier.width(8.dp))
                                Text("FAIR PLAY & ARENA CODE", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            }
                            Text(
                                "1. Zero tolerance for hacks, scripts, teaming, or unauthorized emulators.\n" +
                                    "2. Room ID & Password unlock 5 minutes before match start for registered players only.\n" +
                                    "3. Only verified match results affect rankings and rewards.\n" +
                                    "4. Coin-entry matches deduct the entry fee from your Wallet upon registration; only one account per mobile number is permitted.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(ruleDocs, key = { it.id }) { doc ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SpiderCardDark,
                        border = BorderStroke(1.dp, SpiderRed.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(doc.title, style = MaterialTheme.typography.titleMedium, color = SpiderRedBright)
                            Text(doc.body, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SupportScreen(
    tickets: List<SupportTicket>,
    telegramUrl: String,
    onBack: () -> Unit,
    onSubmitTicket: (String, String, String, String, (Boolean) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val categories = listOf("PAYMENT", "TOURNAMENT", "ACCOUNT", "GENERAL")
    var selectedCategory by remember { mutableStateOf("TOURNAMENT") }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var matchId by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpiderBlack)
    ) {
        SpiderBackTopBar(title = "Customer Support", onBack = onBack)

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Button(
                    onClick = {
                        val cleanUrl = telegramUrl.trim()
                        if (cleanUrl.startsWith("https://", true) || cleanUrl.startsWith("http://", true)) {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, cleanUrl.toUri())) }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpiderSurfaceElevated, contentColor = TextPrimary),
                    border = BorderStroke(1.dp, SpiderCardBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, tint = SpiderRedBright, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open Telegram Support", style = MaterialTheme.typography.labelLarge)
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SpiderCardDark,
                    border = BorderStroke(1.dp, SpiderCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SpiderSectionTitle(title = "SUBMIT SUPPORT TICKET")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            categories.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SpiderRed,
                                        selectedLabelColor = Color.White,
                                        containerColor = SpiderSurfaceElevated,
                                        labelColor = TextSecondary
                                    )
                                )
                            }
                        }
                        val fieldColors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SpiderRed,
                            unfocusedBorderColor = SpiderCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                        OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = matchId, onValueChange = { matchId = it }, label = { Text("Match ID (optional)") }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Describe your issue") }, minLines = 3, colors = fieldColors, modifier = Modifier.fillMaxWidth())

                        Button(
                            onClick = {
                                submitting = true
                                onSubmitTicket(selectedCategory, subject, description, matchId) { ok ->
                                    submitting = false
                                    if (ok) {
                                        subject = ""
                                        description = ""
                                        matchId = ""
                                    }
                                }
                            },
                            enabled = !submitting && subject.trim().length >= 3 && description.trim().length >= 5,
                            colors = ButtonDefaults.buttonColors(containerColor = SpiderRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("SUBMIT TICKET", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            item { SpiderSectionTitle(title = "MY TICKETS") }
            if (tickets.isEmpty()) {
                item { SpiderEmptyState(title = "No Support Tickets", subtitle = "Your payment, tournament, and account complaints appear here.") }
            } else {
                items(tickets, key = { it.id }) { ticket ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SpiderCardDark,
                        border = BorderStroke(1.dp, SpiderCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(ticket.subject, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                StatusPill(ticket.status, statusColor(ticket.status))
                            }
                            Text("${ticket.category} • ${ticket.description}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            if (ticket.adminReply.isNotBlank()) {
                                Text("Admin Reply: ${ticket.adminReply}", style = MaterialTheme.typography.bodySmall, color = SpiderGold)
                            }
                        }
                    }
                }
            }

            item {
                SpiderSectionTitle(title = "FAQ")
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SpiderCardDark,
                    border = BorderStroke(1.dp, SpiderCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Q: When do Room ID & Password appear?", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("A: 5 minutes before match start in My Matches (registered players only).", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text("Q: How do I join coin tournaments & redeem winnings?", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("A: Add coins in Wallet (or use bonus/winning coins) to join any open coin tournament until slots are full. Winning Prize coins can be withdrawn via UPI (min 50 Coins).", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(
    notifications: List<NotificationItem>,
    onBack: () -> Unit,
    onMarkRead: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpiderBlack)
    ) {
        SpiderBackTopBar(title = "Notifications", onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (notifications.isEmpty()) {
                item { SpiderEmptyState(title = "No Notifications", subtitle = "Match reminders and tournament updates will appear here.") }
            } else {
                items(notifications, key = { it.id }) { notif ->
                    Surface(
                        onClick = { if (!notif.read) onMarkRead(notif.id) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (notif.read) SpiderCardDark else SpiderSurfaceElevated,
                        border = BorderStroke(1.dp, if (notif.read) SpiderCardBorder else SpiderRed.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(notif.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                if (!notif.read) StatusPill("NEW", SpiderRedBright)
                            }
                            Text(notif.message, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeamsScreen(
    teams: List<TeamRoster>,
    onBack: () -> Unit,
    onCreateTeam: (String, String, String, (Boolean) -> Unit) -> Unit
) {
    var teamName by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("DUO") }
    var members by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpiderBlack)
    ) {
        SpiderBackTopBar(title = "My Teams", onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SpiderCardDark,
                    border = BorderStroke(1.dp, SpiderCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SpiderSectionTitle(title = "CREATE TEAM ROSTER")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("DUO", "SQUAD", "CLASH_SQUAD").forEach { m ->
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
                        val fieldColors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SpiderRed,
                            unfocusedBorderColor = SpiderCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                        OutlinedTextField(value = teamName, onValueChange = { teamName = it }, label = { Text("Team Name") }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = members, onValueChange = { members = it }, label = { Text("Teammate IGNs / UIDs") }, minLines = 2, colors = fieldColors, modifier = Modifier.fillMaxWidth())
                        Button(
                            onClick = {
                                saving = true
                                onCreateTeam(teamName, mode, members) { ok ->
                                    saving = false
                                    if (ok) {
                                        teamName = ""
                                        members = ""
                                    }
                                }
                            },
                            enabled = !saving && teamName.trim().length >= 2,
                            colors = ButtonDefaults.buttonColors(containerColor = SpiderRed),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Filled.Groups, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("SAVE TEAM", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            if (teams.isEmpty()) {
                item { SpiderEmptyState(title = "No Teams Created", subtitle = "Save your Duo or Clash Squad teammate UIDs here.") }
            } else {
                items(teams, key = { it.id }) { team ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SpiderCardDark,
                        border = BorderStroke(1.dp, SpiderCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(team.teamName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                StatusPill(team.gameMode, SpiderGold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(team.membersSummary.ifBlank { "No members listed" }, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}
