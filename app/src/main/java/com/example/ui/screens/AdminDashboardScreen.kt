package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.example.data.AuthHelper
import com.example.ui.components.SpiderBackTopBar
import com.example.ui.components.SpiderSectionTitle
import com.example.ui.components.StatusPill
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
import com.example.viewmodel.SpiderAdminViewModel
import java.util.Locale

private enum class AdminTab(val label: String) {
    OVERVIEW("Overview"),
    CATEGORIES("Categories"),
    RANDOM("Random Tourneys"),
    BANNERS("Banners"),
    TOURNAMENTS("Tournaments"),
    USERS("Users"),
    WALLETS("Wallets"),
    SUPPORT("Support & Rules")
}

@Composable
fun AdminDashboardScreen(
    adminViewModel: SpiderAdminViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(AdminTab.OVERVIEW) }
    val statusMsg by adminViewModel.statusMessage.collectAsStateWithLifecycle()

    val users by adminViewModel.users.collectAsStateWithLifecycle()
    val categories by adminViewModel.categories.collectAsStateWithLifecycle()
    val customTypes by adminViewModel.customTypes.collectAsStateWithLifecycle()
    val tournaments by adminViewModel.tournaments.collectAsStateWithLifecycle()
    val banners by adminViewModel.banners.collectAsStateWithLifecycle()
    val announcements by adminViewModel.announcements.collectAsStateWithLifecycle()
    val registrations by adminViewModel.registrations.collectAsStateWithLifecycle()
    val wallets by adminViewModel.wallets.collectAsStateWithLifecycle()
    val walletConfig by adminViewModel.walletConfig.collectAsStateWithLifecycle()
    val payments by adminViewModel.payments.collectAsStateWithLifecycle()
    val payouts by adminViewModel.payouts.collectAsStateWithLifecycle()
    val transactions by adminViewModel.transactions.collectAsStateWithLifecycle()
    val tickets by adminViewModel.tickets.collectAsStateWithLifecycle()
    val disputes by adminViewModel.disputes.collectAsStateWithLifecycle()
    val auditLogs by adminViewModel.auditLogs.collectAsStateWithLifecycle()
    val referrals by adminViewModel.referrals.collectAsStateWithLifecycle()
    val adminUsers by adminViewModel.adminUsers.collectAsStateWithLifecycle()
    val results by adminViewModel.results.collectAsStateWithLifecycle()

    val currentAuthUser = Firebase.auth.currentUser
    LaunchedEffect(currentAuthUser?.uid, currentAuthUser?.email, currentAuthUser?.isEmailVerified) {
        if (!AuthHelper.isBootstrapAdmin(currentAuthUser)) {
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpiderBlack)
    ) {
        SpiderBackTopBar(title = "Admin Command Center", onBack = onBack)

        Surface(
            color = SpiderEmerald.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, SpiderEmerald.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Verified Admin: ${currentAuthUser?.email ?: "Authorized"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SpiderEmerald
                )
                StatusPill("EMAIL VERIFIED", SpiderEmerald)
            }
        }

        statusMsg?.let { (ok, msg) ->
            Surface(
                onClick = { adminViewModel.clearStatus() },
                color = if (ok) SpiderEmerald.copy(alpha = 0.18f) else SpiderRed.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, if (ok) SpiderEmerald else SpiderRedBright),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "$msg (Tap to dismiss)",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (ok) SpiderEmerald else SpiderRedBright,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                FilterChip(
                    selected = selected,
                    onClick = { selectedTab = tab },
                    label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
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
                    ),
                    modifier = Modifier.testTag("admin_tab_${tab.name}")
                )
            }
        }

        when (selectedTab) {
            AdminTab.OVERVIEW -> {
                val activeUsers = users.count { it.status == "ACTIVE" }
                val liveTours = tournaments.count { it.status == "LIVE" }
                val completedTours = tournaments.count { it.status == "COMPLETED" }
                val totalWalletCoins = wallets.sumOf { it.totalBalance }
                val totalPrizes = tournaments.sumOf { it.prizePool }

                LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Button(
                            onClick = { adminViewModel.seedDefaultCatalog() },
                            colors = ButtonDefaults.buttonColors(containerColor = SpiderSurfaceElevated, contentColor = TextPrimary),
                            border = BorderStroke(1.dp, SpiderRed.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Text("SYNC / SEED DEFAULT CATALOG & BANNERS", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                AdminKpiCard("TOTAL USERS", "${users.size}", SpiderRedBright, Modifier.weight(1f))
                                AdminKpiCard("ACTIVE USERS", "$activeUsers", SpiderEmerald, Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                AdminKpiCard("TOURNAMENTS", "${tournaments.size}", SpiderGold, Modifier.weight(1f))
                                AdminKpiCard("LIVE / DONE", "$liveTours / $completedTours", SpiderEmerald, Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                AdminKpiCard("REGISTRATIONS", "${registrations.size}", TextPrimary, Modifier.weight(1f))
                                AdminKpiCard("WALLET COINS", String.format(Locale.US, "%.0f", totalWalletCoins), SpiderGold, Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                AdminKpiCard("CATEGORIES", "${categories.size}", TextPrimary, Modifier.weight(1f))
                                AdminKpiCard("PRIZE POOLS", String.format(Locale.US, "%.0f", totalPrizes), SpiderRedBright, Modifier.weight(1f))
                            }
                        }
                    }

                    item { SpiderSectionTitle(title = "AUDIT LOGS (${auditLogs.size})") }
                    items(auditLogs.take(20), key = { it.id }) { log ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SpiderCardDark,
                            border = BorderStroke(1.dp, SpiderCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(log.action, style = MaterialTheme.typography.labelSmall, color = SpiderGold)
                                    StatusPill(log.targetCollection, TextMuted)
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(log.summary, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                Text("By ${log.actorEmail}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                        }
                    }
                }
            }
            AdminTab.CATEGORIES -> AdminCategoriesSection(categories, adminViewModel)
            AdminTab.RANDOM -> AdminRandomTournamentsSection(customTypes, adminViewModel)
            AdminTab.BANNERS -> AdminBannersSection(banners, adminViewModel)
            AdminTab.TOURNAMENTS -> AdminTournamentsSection(tournaments, categories, users, registrations, results, adminViewModel)
            AdminTab.USERS -> AdminUsersSection(users, adminUsers, adminViewModel)
            AdminTab.WALLETS -> AdminWalletsSection(users, wallets, walletConfig, payments, payouts, transactions, referrals, adminViewModel)
            AdminTab.SUPPORT -> AdminSupportAndConfigSection(announcements, tickets, disputes, adminViewModel)
        }
    }
}

@Composable
private fun AdminKpiCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SpiderCardDark,
        border = BorderStroke(1.dp, SpiderCardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, color = accent, fontWeight = FontWeight.Bold)
        }
    }
}
