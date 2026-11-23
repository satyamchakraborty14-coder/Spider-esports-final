package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.AuthHelper
import com.example.data.SpiderAdminRepository
import com.example.data.SpiderCatalogRepository
import com.example.data.SpiderUserRepository
import com.example.model.AppRoutes
import com.example.model.BottomNavTab
import com.example.model.UiState
import com.example.ui.components.SpiderBottomNavBar
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CompleteProfileGateScreen
import com.example.ui.screens.EarnScreen
import com.example.ui.screens.EmailVerificationGateScreen
import com.example.ui.screens.MyMatchesScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PlayHomeScreen
import com.example.ui.screens.RankingScreen
import com.example.ui.screens.RulesScreen
import com.example.ui.screens.SupportScreen
import com.example.ui.screens.TeamsScreen
import com.example.ui.screens.TournamentListScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.SpiderBlack
import com.example.ui.theme.SpiderCardDark
import com.example.ui.theme.SpiderEmerald
import com.example.ui.theme.SpiderEsportsTheme
import com.example.ui.theme.SpiderRed
import com.example.ui.theme.SpiderRedBright
import com.example.ui.theme.SpiderSurface
import com.example.ui.theme.TextMuted
import com.example.viewmodel.SpiderAdminViewModel
import com.example.viewmodel.SpiderMainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpiderEsportsTheme {
                AppNavigationRoot()
            }
        }
    }
}

@Composable
private fun AppNavigationRoot() {
    val auth = remember { Firebase.auth }
    val currentUser by AuthHelper.authStateFlow(auth)
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var verificationRevision by remember { mutableIntStateOf(0) }
    var signupInProgress by remember { mutableStateOf(false) }

    val user = currentUser
    val isEmailVerified = remember(user, verificationRevision) {
        user?.isEmailVerified == true
    }

    if (user == null || signupInProgress) {
        AuthScreen(
            onAuthSuccess = { verificationRevision++ },
            onRegistrationBusyChange = { busy -> signupInProgress = busy }
        )
    } else if (!isEmailVerified) {
        EmailVerificationGateScreen(
            user = user,
            onVerified = { verificationRevision++ },
            onSignedOut = { verificationRevision++ }
        )
    } else {
        key(user.uid) {
            SpiderAppShell(currentUserId = user.uid)
        }
    }
}

@Composable
private fun SpiderAppShell(currentUserId: String) {
    val mainViewModel: SpiderMainViewModel = viewModel(
        key = "main_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                SpiderMainViewModel(
                    db = db,
                    currentUserId = currentUserId,
                    userRepo = SpiderUserRepository(db),
                    catalogRepo = SpiderCatalogRepository(db),
                    adminRepo = SpiderAdminRepository(db)
                )
            }
        }
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppRoutes.PLAY

    val isAdmin by mainViewModel.isAdmin.collectAsStateWithLifecycle()
    val profileState by mainViewModel.userProfile.collectAsStateWithLifecycle()
    val wallet by mainViewModel.wallet.collectAsStateWithLifecycle()
    val walletConfig by mainViewModel.walletConfig.collectAsStateWithLifecycle()
    val paymentOrders by mainViewModel.paymentOrders.collectAsStateWithLifecycle()
    val transactionsState by mainViewModel.transactions.collectAsStateWithLifecycle()
    val payoutRequests by mainViewModel.payoutRequests.collectAsStateWithLifecycle()
    val categoriesState by mainViewModel.categories.collectAsStateWithLifecycle()
    val tournamentsState by mainViewModel.tournaments.collectAsStateWithLifecycle()
    val banners by mainViewModel.banners.collectAsStateWithLifecycle()
    val announcements by mainViewModel.announcements.collectAsStateWithLifecycle()
    val registrations by mainViewModel.registrations.collectAsStateWithLifecycle()
    val matches by mainViewModel.matches.collectAsStateWithLifecycle()
    val results by mainViewModel.results.collectAsStateWithLifecycle()
    val leaderboardsState by mainViewModel.leaderboards.collectAsStateWithLifecycle()
    val notifications by mainViewModel.notifications.collectAsStateWithLifecycle()
    val tickets by mainViewModel.supportTickets.collectAsStateWithLifecycle()
    val teams by mainViewModel.teams.collectAsStateWithLifecycle()
    val selectedGameTab by mainViewModel.selectedGameTab.collectAsStateWithLifecycle()
    val selectedLanguage by mainViewModel.selectedLanguage.collectAsStateWithLifecycle()
    val actionBanner by mainViewModel.actionBanner.collectAsStateWithLifecycle()
    val roomCache by mainViewModel.roomCredentialsCache.collectAsStateWithLifecycle()
    val myReferrals by mainViewModel.myReferrals.collectAsStateWithLifecycle()
    val myReferralReceived by mainViewModel.myReferralReceived.collectAsStateWithLifecycle()
    val profileInitError by mainViewModel.profileInitError.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentAuthUser = Firebase.auth.currentUser
    val isVerifiedOwnerAdmin = isAdmin && AuthHelper.isBootstrapAdmin(currentAuthUser)
    val profile = (profileState as? UiState.Success)?.data
    LaunchedEffect(profile?.emailVerified, currentAuthUser?.isEmailVerified) {
        if (currentAuthUser != null && currentAuthUser.isEmailVerified && profile != null && !profile.emailVerified) {
            mainViewModel.syncEmailVerification(currentAuthUser)
        }
    }

    val errorMessage = (profileState as? UiState.Error)?.message ?: if (profile == null) profileInitError else null
    if (errorMessage != null) {
        Box(
            modifier = Modifier.fillMaxSize().background(SpiderBlack).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SpiderCardDark,
                border = BorderStroke(1.dp, SpiderRedBright),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Unable to Load Profile", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text(errorMessage, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    Button(
                        onClick = { mainViewModel.initializeSession() },
                        colors = ButtonDefaults.buttonColors(containerColor = SpiderRed, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Retry")
                    }
                    OutlinedButton(
                        onClick = { AuthHelper.signOut(credentialManager, scope) },
                        border = BorderStroke(1.dp, SpiderRedBright),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign Out", color = SpiderRedBright)
                    }
                }
            }
        }
        return
    }

    if (profileState is UiState.Loading || (profileState is UiState.Success && profile == null)) {
        Box(
            modifier = Modifier.fillMaxSize().background(SpiderBlack),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = SpiderRed)
        }
        return
    }

    if (profile != null && profile.status == "BANNED" && !isVerifiedOwnerAdmin) {
        Box(
            modifier = Modifier.fillMaxSize().background(SpiderBlack).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SpiderCardDark,
                border = BorderStroke(1.5.dp, SpiderRedBright),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ACCOUNT BANNED", style = MaterialTheme.typography.titleLarge, color = SpiderRedBright)
                    Text(
                        "Your Spider Esports account (${profile.username}) has been banned by the Administrator. You cannot join matches, deposit, or withdraw.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    OutlinedButton(
                        onClick = { AuthHelper.signOut(credentialManager, scope) },
                        border = BorderStroke(1.dp, SpiderRedBright),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign Out", color = SpiderRedBright)
                    }
                }
            }
        }
        return
    }

    if (profile != null && !profile.onboardingCompleted && currentAuthUser != null) {
        CompleteProfileGateScreen(
            user = currentAuthUser,
            profile = profile,
            onSubmitProfile = { gameName, gameUid, phoneE164, refCode, acceptedTerms, onDone ->
                mainViewModel.completeOnboarding(
                    user = currentAuthUser,
                    gameName = gameName,
                    gameUid = gameUid,
                    phoneE164 = phoneE164,
                    referralCodeInput = refCode,
                    acceptedTerms = acceptedTerms,
                    onDone = onDone
                )
            },
            onSignedOut = {}
        )
        return
    }

    val supportConfig = remember(announcements) {
        announcements.firstOrNull { it.category == "SUPPORT_CONFIG" }
    }
    val telegramUrl = supportConfig?.telegramUrl ?: "https://t.me/spideresports_official"
    val whatsappUrl = supportConfig?.whatsappUrl ?: "https://wa.me/0000000000"

    val bottomRoutes = setOf(
        AppRoutes.EARN,
        AppRoutes.RANKING,
        AppRoutes.PLAY,
        AppRoutes.WALLET,
        AppRoutes.ACCOUNT
    )
    val showBottomOrRailNav = currentRoute in bottomRoutes

    fun navigateTopTab(tab: BottomNavTab) {
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(actionBanner) {
        if (actionBanner != null) {
            delay(3500)
            mainViewModel.clearBanner()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(SpiderBlack)
    ) {
        val useNavRail = maxWidth >= 720.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavRail && showBottomOrRailNav) {
                NavigationRail(
                    containerColor = SpiderSurface,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(Modifier.weight(1f))
                    BottomNavTab.entries.forEach { tab ->
                        val selected = currentRoute == tab.route
                        val icon = when (tab) {
                            BottomNavTab.EARN -> Icons.Filled.CardGiftcard
                            BottomNavTab.RANKING -> Icons.Filled.Leaderboard
                            BottomNavTab.PLAY -> Icons.Filled.SportsEsports
                            BottomNavTab.WALLET -> Icons.Filled.AccountBalanceWallet
                            BottomNavTab.ACCOUNT -> Icons.Filled.Person
                        }
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateTopTab(tab) },
                            icon = { Icon(icon, tab.label) },
                            label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = SpiderRedBright,
                                selectedTextColor = SpiderRedBright,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
                                indicatorColor = SpiderRed.copy(alpha = 0.2f)
                            )
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
            }

            Scaffold(
                containerColor = SpiderBlack,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (!useNavRail && showBottomOrRailNav) {
                        val activeTab = BottomNavTab.entries.firstOrNull { it.route == currentRoute } ?: BottomNavTab.PLAY
                        SpiderBottomNavBar(
                            selectedTab = activeTab,
                            onTabSelected = ::navigateTopTab
                        )
                    }
                },
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = AppRoutes.PLAY,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(AppRoutes.PLAY) {
                            PlayHomeScreen(
                                profileState = profileState,
                                wallet = wallet,
                                selectedLanguage = selectedLanguage,
                                selectedGameTab = selectedGameTab,
                                categoriesState = categoriesState,
                                tournamentsState = tournamentsState,
                                banners = banners,
                                announcements = announcements,
                                matches = matches,
                                registrations = registrations,
                                onLanguageChange = mainViewModel::setLanguage,
                                onSupportClick = { navController.navigate(AppRoutes.SUPPORT) },
                                onWalletClick = { navigateTopTab(BottomNavTab.WALLET) },
                                onAccountClick = { navigateTopTab(BottomNavTab.ACCOUNT) },
                                onRulesClick = { navController.navigate(AppRoutes.RULES) },
                                onBannerAction = { banner ->
                                    when (banner.targetAction) {
                                        "RULES" -> navController.navigate(AppRoutes.RULES)
                                        "SUPPORT" -> navController.navigate(AppRoutes.SUPPORT)
                                        "RANKING" -> navigateTopTab(BottomNavTab.RANKING)
                                        else -> {
                                            val firstCat = (categoriesState as? UiState.Success)?.data?.firstOrNull()
                                            if (firstCat != null) {
                                                navController.navigate(AppRoutes.tournamentsByCategory(firstCat.id))
                                            }
                                        }
                                    }
                                },
                                onMyMatchesClick = { initialTab ->
                                    navController.navigate(AppRoutes.myMatches(initialTab))
                                },
                                onSelectGameTab = mainViewModel::selectGameTab,
                                onCategoryClick = { cat ->
                                    navController.navigate(AppRoutes.tournamentsByCategory(cat.id))
                                },
                                onJoinSoloTournament = mainViewModel::registerForTournament
                            )
                        }

                        composable(AppRoutes.EARN) {
                            val referralTxs = remember(transactionsState) {
                                (transactionsState as? UiState.Success)?.data
                                    ?.filter { it.type == "REFERRAL_BONUS" }
                                    .orEmpty()
                            }
                            EarnScreen(
                                profile = profile,
                                announcements = announcements,
                                myReferrals = myReferrals,
                                myReferralReceived = myReferralReceived,
                                referralTransactions = referralTxs,
                                onApplyReferralCode = mainViewModel::applyReferralCode
                            )
                        }

                        composable(AppRoutes.RANKING) {
                            RankingScreen(leaderboardsState = leaderboardsState)
                        }

                        composable(AppRoutes.WALLET) {
                            WalletScreen(
                                wallet = wallet,
                                walletConfig = walletConfig,
                                transactionsState = transactionsState,
                                paymentOrders = paymentOrders,
                                payoutRequests = payoutRequests,
                                onSubmitDeposit = mainViewModel::submitDepositRequest,
                                onSubmitWithdrawal = mainViewModel::submitWithdrawalRequest,
                                onBack = { navigateTopTab(BottomNavTab.PLAY) }
                            )
                        }

                        composable(AppRoutes.ACCOUNT) {
                            AccountScreen(
                                profile = profile,
                                isAdmin = isVerifiedOwnerAdmin,
                                unreadNotificationsCount = notifications.count { !it.read },
                                telegramUrl = telegramUrl,
                                onEditProfile = mainViewModel::updateProfile,
                                onOpenMyMatches = { navController.navigate(AppRoutes.myMatches("UPCOMING")) },
                                onOpenWallet = { navigateTopTab(BottomNavTab.WALLET) },
                                onOpenNotifications = { navController.navigate(AppRoutes.NOTIFICATIONS) },
                                onOpenTeams = { navController.navigate(AppRoutes.TEAMS) },
                                onOpenSupport = { navController.navigate(AppRoutes.SUPPORT) },
                                onOpenAdminDashboard = { navController.navigate(AppRoutes.ADMIN_DASHBOARD) },
                                onSignedOut = {}
                            )
                        }

                        composable(
                            route = AppRoutes.TOURNAMENTS_BY_CATEGORY,
                            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
                        ) { entry ->
                            val categoryId = entry.arguments?.getString("categoryId") ?: ""
                            val cat = (categoriesState as? UiState.Success)?.data?.firstOrNull { it.id == categoryId }
                            TournamentListScreen(
                                category = cat,
                                tournamentsState = tournamentsState,
                                registrations = registrations,
                                profile = profile,
                                wallet = wallet,
                                onBack = { navController.popBackStack() },
                                onJoinTournament = mainViewModel::registerForTournament
                            )
                        }

                        composable(
                            route = AppRoutes.MY_MATCHES,
                            arguments = listOf(navArgument("initialTab") { type = NavType.StringType })
                        ) { entry ->
                            val initialTab = entry.arguments?.getString("initialTab") ?: "UPCOMING"
                            val tours = (tournamentsState as? UiState.Success)?.data.orEmpty()
                            MyMatchesScreen(
                                initialTab = initialTab,
                                currentUserId = currentUserId,
                                registrations = registrations,
                                tournaments = tours,
                                matches = matches,
                                results = results,
                                roomCredentialsCache = roomCache,
                                onBack = { navController.popBackStack() },
                                onRequestRoomCredential = mainViewModel::loadRoomCredential,
                                onSubmitDispute = mainViewModel::submitDispute
                            )
                        }

                        composable(AppRoutes.RULES) {
                            RulesScreen(
                                announcements = announcements,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(AppRoutes.SUPPORT) {
                            SupportScreen(
                                tickets = tickets,
                                telegramUrl = telegramUrl,
                                whatsappUrl = whatsappUrl,
                                onBack = { navController.popBackStack() },
                                onSubmitTicket = mainViewModel::submitSupportTicket
                            )
                        }

                        composable(AppRoutes.NOTIFICATIONS) {
                            NotificationsScreen(
                                notifications = notifications,
                                onBack = { navController.popBackStack() },
                                onMarkRead = mainViewModel::markNotificationRead
                            )
                        }

                        composable(AppRoutes.TEAMS) {
                            TeamsScreen(
                                teams = teams,
                                onBack = { navController.popBackStack() },
                                onCreateTeam = mainViewModel::createTeam
                            )
                        }

                        composable(AppRoutes.ADMIN_DASHBOARD) {
                            if (isVerifiedOwnerAdmin) {
                                val adminViewModel: SpiderAdminViewModel = viewModel(
                                    key = "admin_$currentUserId",
                                    factory = viewModelFactory {
                                        initializer {
                                            val app = checkNotNull(this[APPLICATION_KEY])
                                            val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                                            SpiderAdminViewModel(
                                                db = db,
                                                adminUid = currentUserId,
                                                adminRepo = SpiderAdminRepository(db)
                                            )
                                        }
                                    }
                                )
                                AdminDashboardScreen(
                                    adminViewModel = adminViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            } else {
                                LaunchedEffect(Unit) { navController.popBackStack() }
                            }
                        }
                    }

                    actionBanner?.let { (ok, message) ->
                        Surface(
                            onClick = { mainViewModel.clearBanner() },
                            shape = RoundedCornerShape(12.dp),
                            color = SpiderCardDark,
                            border = BorderStroke(1.5.dp, if (ok) SpiderEmerald else SpiderRedBright),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (ok) SpiderEmerald else Color.White,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
