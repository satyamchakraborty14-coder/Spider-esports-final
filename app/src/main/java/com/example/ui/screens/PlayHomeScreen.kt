package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Announcement
import com.example.model.MatchRecord
import com.example.model.PlayGameTab
import com.example.model.PromoBanner
import com.example.model.Registration
import com.example.model.Tournament
import com.example.model.TournamentCategory
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.model.Wallet
import com.example.ui.components.SpiderArtworkImage
import com.example.ui.components.SpiderEmptyState
import com.example.ui.components.SpiderSectionTitle
import com.example.ui.components.SpiderTopHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.statusColor
import com.example.ui.theme.SpiderBlack
import com.example.ui.theme.SpiderCardBorder
import com.example.ui.theme.SpiderCardDark
import com.example.ui.theme.SpiderEmerald
import com.example.ui.theme.SpiderGold
import com.example.ui.theme.SpiderRed
import com.example.ui.theme.SpiderRedBright
import com.example.ui.theme.SpiderRedDark
import com.example.ui.theme.SpiderSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun PlayHomeScreen(
    profileState: UiState<UserProfile?>,
    wallet: Wallet,
    selectedLanguage: String,
    selectedGameTab: PlayGameTab,
    categoriesState: UiState<List<TournamentCategory>>,
    tournamentsState: UiState<List<Tournament>>,
    banners: List<PromoBanner>,
    announcements: List<Announcement>,
    matches: List<MatchRecord>,
    registrations: List<Registration>,
    onLanguageChange: (String) -> Unit,
    onSupportClick: () -> Unit,
    onWalletClick: () -> Unit,
    onAccountClick: () -> Unit,
    onRulesClick: () -> Unit,
    onBannerAction: (PromoBanner) -> Unit,
    onMyMatchesClick: (String) -> Unit,
    onSelectGameTab: (PlayGameTab) -> Unit,
    onCategoryClick: (TournamentCategory) -> Unit,
    onJoinSoloTournament: (Tournament, String, String, (Boolean) -> Unit) -> Unit
) {
    val profile = (profileState as? UiState.Success)?.data
    val tournaments = (tournamentsState as? UiState.Success)?.data.orEmpty()
    val registeredIds = remember(registrations) { registrations.map { it.tournamentId }.toSet() }
    var joinModalTournament by remember { mutableStateOf<Tournament?>(null) }

    val ongoingCount = matches.count { it.status == "ONGOING" } +
        tournaments.count { it.id in registeredIds && it.status == "LIVE" }
    val upcomingCount = matches.count { it.status == "UPCOMING" } +
        tournaments.count { it.id in registeredIds && it.status == "UPCOMING" }
    val completedCount = matches.count { it.status == "COMPLETED" } +
        tournaments.count { it.id in registeredIds && it.status == "COMPLETED" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpiderBlack)
    ) {
        SpiderTopHeader(
            username = profile?.username ?: profile?.displayName ?: "SpiderPlayer",
            photoUrl = profile?.photoUrl ?: "",
            walletBalance = wallet.totalBalance,
            selectedLanguage = selectedLanguage,
            onLanguageChange = onLanguageChange,
            onSupportClick = onSupportClick,
            onWalletClick = onWalletClick,
            onAvatarClick = onAccountClick
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 600.dp
            val gridColumns = if (isWide) 4 else 3

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 880.dp)
                    .align(Alignment.TopCenter),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Megaphone Announcement Bar
                item {
                    Surface(
                        onClick = onRulesClick,
                        shape = RoundedCornerShape(12.dp),
                        color = SpiderCardDark,
                        border = BorderStroke(1.dp, SpiderRed.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rules_announcement_bar")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SpiderRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Campaign,
                                    contentDescription = "Rules announcement",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "SPIDER ESPORTS RULES — CLICK HERE",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = SpiderRedBright
                            )
                        }
                    }
                }

                // 2. IMPORTANT / Updates and Alerts
                val importantItems = announcements.filter { it.category == "IMPORTANT" || it.category == "ALERT" }
                if (importantItems.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SpiderSectionTitle(
                                title = "IMPORTANT",
                                subtitle = "Updates and Alerts"
                            )
                            importantItems.take(2).forEach { ann ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SpiderSurfaceElevated,
                                    border = BorderStroke(1.dp, SpiderGold.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.NotificationsActive,
                                            contentDescription = null,
                                            tint = SpiderGold,
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = ann.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = ann.body,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Auto-Sliding Promotional Banner Carousel
                if (banners.isNotEmpty()) {
                    item {
                        val pagerState = rememberPagerState(pageCount = { banners.size })
                        LaunchedEffect(banners.size) {
                            while (banners.size > 1) {
                                delay(4000)
                                val next = (pagerState.currentPage + 1) % banners.size
                                pagerState.animateScrollToPage(next)
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxWidth()
                            ) { page ->
                                val banner = banners[page]
                                Surface(
                                    onClick = { onBannerAction(banner) },
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, SpiderCardBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(152.dp)
                                        .testTag("promo_banner_$page")
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        SpiderArtworkImage(
                                            imageUrl = banner.imageUrl,
                                            presetKey = banner.presetKey,
                                            contentDescription = banner.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(
                                                            SpiderBlack.copy(alpha = 0.88f),
                                                            SpiderBlack.copy(alpha = 0.45f),
                                                            Color.Transparent
                                                        )
                                                    )
                                                )
                                        )
                                        Column(
                                            modifier = Modifier
                                                .align(Alignment.CenterStart)
                                                .padding(16.dp)
                                                .widthIn(max = 260.dp)
                                        ) {
                                            StatusPill(text = banner.targetAction, color = SpiderRedBright)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = banner.title,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = TextPrimary,
                                                maxLines = 2
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = banner.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            if (banners.size > 1) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    repeat(banners.size) { idx ->
                                        val active = pagerState.currentPage == idx
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 3.dp)
                                                .height(6.dp)
                                                .width(if (active) 20.dp else 6.dp)
                                                .clip(CircleShape)
                                                .background(if (active) SpiderRed else TextMuted)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. MY MATCHES (ONGOING, UPCOMING, COMPLETED)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SpiderSectionTitle(title = "MY MATCHES")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MyMatchStatusCard(
                                label = "ONGOING",
                                count = ongoingCount,
                                accent = SpiderEmerald,
                                icon = Icons.Filled.PlayCircle,
                                onClick = { onMyMatchesClick("ONGOING") },
                                modifier = Modifier.weight(1f).testTag("my_matches_ongoing")
                            )
                            MyMatchStatusCard(
                                label = "UPCOMING",
                                count = upcomingCount,
                                accent = SpiderGold,
                                icon = Icons.Filled.Schedule,
                                onClick = { onMyMatchesClick("UPCOMING") },
                                modifier = Modifier.weight(1f).testTag("my_matches_upcoming")
                            )
                            MyMatchStatusCard(
                                label = "COMPLETED",
                                count = completedCount,
                                accent = SpiderRedBright,
                                icon = Icons.Filled.CheckCircle,
                                onClick = { onMyMatchesClick("COMPLETED") },
                                modifier = Modifier.weight(1f).testTag("my_matches_completed")
                            )
                        }
                    }
                }

                // 5. ESPORTS GAME — Big Winnings For All + TOURNAMENT / SOLO Tabs
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SpiderSectionTitle(
                            title = "ESPORTS GAME",
                            subtitle = "Big Winnings For All"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SpiderCardDark)
                                .border(1.dp, SpiderCardBorder, RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val tourSelected = selectedGameTab == PlayGameTab.TOURNAMENT
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (tourSelected) SpiderRed else SpiderSurfaceElevated)
                                    .clickable { onSelectGameTab(PlayGameTab.TOURNAMENT) }
                                    .padding(vertical = 11.dp)
                                    .testTag("tab_tournament"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "TOURNAMENT",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (tourSelected) Color.White else TextSecondary
                                )
                            }

                            val soloSelected = selectedGameTab == PlayGameTab.SOLO
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (soloSelected) SpiderRed else SpiderSurfaceElevated)
                                    .clickable { onSelectGameTab(PlayGameTab.SOLO) }
                                    .padding(vertical = 11.dp)
                                    .testTag("tab_solo"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "SOLO",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (soloSelected) Color.White else TextSecondary
                                )
                            }
                        }
                    }
                }

                // 6. Content for TOURNAMENT or SOLO tab
                if (selectedGameTab == PlayGameTab.TOURNAMENT) {
                    when (categoriesState) {
                        is UiState.Loading -> item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = SpiderRed)
                            }
                        }
                        is UiState.Error -> item {
                            SpiderEmptyState(
                                title = "Unable to Load Categories",
                                subtitle = categoriesState.message
                            )
                        }
                        is UiState.Success -> {
                            val allCats = categoriesState.data
                            val nonSoloCats = allCats.filter { it.categoryType != "SOLO" }
                            if (nonSoloCats.isEmpty()) {
                                item {
                                    SpiderEmptyState(
                                        title = "No Tournament Categories Yet",
                                        subtitle = "Admin can add Battle Royale, Lone Wolf, Clash Squad, or Random Tournament categories in the Admin Dashboard."
                                    )
                                }
                            } else {
                                val groups = listOf(
                                    "BATTLE_ROYALE" to "BATTLE ROYALE",
                                    "LONE_WOLF" to "LONE WOLF",
                                    "CLASH_SQUAD" to "CLASH SQUAD",
                                    "ADDITIONAL" to "ADDITIONAL",
                                    "CUSTOM" to "RANDOM TOURNAMENTS"
                                )
                                groups.forEach { (typeKey, groupTitle) ->
                                    val groupCats = nonSoloCats.filter {
                                        if (typeKey == "CUSTOM") it.randomTournament || it.categoryType == "CUSTOM"
                                        else !it.randomTournament && it.categoryType == typeKey
                                    }
                                    if (groupCats.isNotEmpty()) {
                                        item(key = "group_$typeKey") {
                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Text(
                                                    text = groupTitle,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = if (typeKey == "CUSTOM") SpiderGold else TextSecondary,
                                                    letterSpacing = 1.sp
                                                )
                                                val rows = groupCats.chunked(gridColumns)
                                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    rows.forEach { rowItems ->
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                        ) {
                                                            rowItems.forEach { cat ->
                                                                CategoryGridCard(
                                                                    category = cat,
                                                                    onClick = { onCategoryClick(cat) },
                                                                    modifier = Modifier.weight(1f)
                                                                )
                                                            }
                                                            repeat(gridColumns - rowItems.size) {
                                                                Spacer(modifier = Modifier.weight(1f))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // SOLO TAB: Solo formats + Admin-created Solo tournaments
                    val soloCategories = (categoriesState as? UiState.Success)?.data
                        ?.filter { it.categoryType == "SOLO" || it.categoryName.contains("SOLO", ignoreCase = true) }
                        .orEmpty()
                    val soloTournaments = tournaments.filter {
                        it.soloFormat || it.gameMode.contains("SOLO", ignoreCase = true)
                    }

                    if (soloCategories.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "SOLO FORMATS",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextSecondary,
                                    letterSpacing = 1.sp
                                )
                                val rows = soloCategories.chunked(gridColumns)
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    rows.forEach { rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            rowItems.forEach { cat ->
                                                CategoryGridCard(
                                                    category = cat,
                                                    onClick = { onCategoryClick(cat) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            repeat(gridColumns - rowItems.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (soloTournaments.isEmpty()) {
                        item {
                            SpiderEmptyState(
                                title = "No Solo Matches Active",
                                subtitle = "Check back soon or ask an Admin to publish a Solo match."
                            )
                        }
                    } else {
                        items(soloTournaments.size, key = { soloTournaments[it].id }) { idx ->
                            val tour = soloTournaments[idx]
                            SoloTournamentCard(
                                tournament = tour,
                                isRegistered = tour.id in registeredIds,
                                onJoinClick = { joinModalTournament = tour }
                            )
                        }
                    }
                }
            }
        }
    }

    joinModalTournament?.let { tour ->
        JoinTournamentDialog(
            tournament = tour,
            profile = profile,
            walletBalance = wallet.totalBalance,
            isAlreadyRegistered = tour.id in registeredIds,
            onDismiss = { joinModalTournament = null },
            onConfirmJoin = { ign, igUid, done ->
                onJoinSoloTournament(tour, ign, igUid) { ok ->
                    done(ok)
                    if (ok) joinModalTournament = null
                }
            }
        )
    }
}

@Composable
private fun MyMatchStatusCard(
    label: String,
    count: Int,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = SpiderCardDark,
        border = BorderStroke(1.dp, SpiderCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
                maxLines = 1
            )
            Text(
                text = "$count Matches",
                style = MaterialTheme.typography.bodySmall,
                color = accent,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun CategoryGridCard(
    category: TournamentCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = SpiderCardDark,
        border = BorderStroke(
            1.dp,
            if (category.randomTournament) SpiderGold.copy(alpha = 0.65f) else SpiderCardBorder
        ),
        modifier = modifier.testTag("category_card_${category.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
            ) {
                SpiderArtworkImage(
                    imageUrl = category.categoryImageUrl,
                    presetKey = category.presetKey,
                    contentDescription = category.categoryName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (category.randomTournament) {
                    Surface(
                        color = SpiderGold,
                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "EVENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = SpiderBlack,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(SpiderRed, SpiderRedDark))
                    )
                    .padding(horizontal = 6.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.categoryName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SoloTournamentCard(
    tournament: Tournament,
    isRegistered: Boolean,
    onJoinClick: () -> Unit
) {
    val availableSlots = (tournament.maxParticipants - tournament.registeredCount).coerceAtLeast(0)
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
                    .height(120.dp)
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
                            Brush.verticalGradient(listOf(Color.Transparent, SpiderBlack.copy(alpha = 0.9f)))
                        )
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusPill(text = tournament.status, color = statusColor(tournament.status))
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(tournament.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(
                        "${tournament.gameMode} • ${tournament.mapName} • ${tournament.matchDate} ${tournament.matchTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("ENTRY", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(
                            if (tournament.entryFee <= 0.0) "FREE" else "${tournament.entryFee.toInt()} Coins",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (tournament.entryFee <= 0.0) SpiderEmerald else SpiderGold
                        )
                    }
                    Column {
                        Text("PRIZE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.EmojiEvents, null, tint = SpiderGold, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(
                                if (tournament.prizePool <= 0.0) "Rank Pts" else "${tournament.prizePool.toInt()} Coins",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                        }
                    }
                    Column {
                        Text("PER KILL", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(
                            if (tournament.perKillReward <= 0.0) "0 Coins" else "${tournament.perKillReward.toInt()} Coins",
                            style = MaterialTheme.typography.titleSmall,
                            color = SpiderGold
                        )
                    }
                    Column {
                        Text("SLOTS", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(
                            "$availableSlots / ${tournament.maxParticipants}",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                    }
                }
                Button(
                    onClick = onJoinClick,
                    enabled = !isRegistered && availableSlots > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRegistered) SpiderEmerald else SpiderRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("solo_join_${tournament.id}")
                ) {
                    Text(if (isRegistered) "JOINED" else "JOIN TOURNAMENT", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
