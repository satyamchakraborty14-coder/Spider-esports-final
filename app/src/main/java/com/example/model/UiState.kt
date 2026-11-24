package com.example.model

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class BottomNavTab(val route: String, val label: String) {
    EARN("earn", "EARN"),
    RANKING("ranking", "RANKING"),
    PLAY("play", "PLAY"),
    WALLET("wallet", "WALLET"),
    ACCOUNT("account", "ACCOUNT")
}

object AppRoutes {
    const val PLAY = "play"
    const val EARN = "earn"
    const val RANKING = "ranking"
    const val WALLET = "wallet"
    const val ACCOUNT = "account"
    const val TOURNAMENTS_BY_CATEGORY = "tournaments/{categoryId}"
    const val MATCH_DETAILS = "match_details/{tournamentId}"
    const val MY_MATCHES = "my_matches/{initialTab}"
    const val RULES = "rules"
    const val SUPPORT = "support"
    const val NOTIFICATIONS = "notifications"
    const val TEAMS = "teams"
    const val ADMIN_DASHBOARD = "admin_dashboard"

    fun tournamentsByCategory(categoryId: String) = "tournaments/$categoryId"
    fun matchDetails(tournamentId: String) = "match_details/$tournamentId"
    fun myMatches(initialTab: String = "UPCOMING") = "my_matches/$initialTab"
}

enum class TournamentFilter(val label: String) {
    ALL("All"),
    UPCOMING("Upcoming"),
    LIVE("Live"),
    FULL("Full"),
    COMPLETED("Completed")
}

enum class TournamentSort(val label: String) {
    STARTING_SOON("Starting Soon"),
    LOWEST_ENTRY("Lowest Entry Fee"),
    HIGHEST_PRIZE("Highest Prize Pool"),
    RECENTLY_ADDED("Recently Added")
}

enum class PlayGameTab {
    TOURNAMENT,
    SOLO
}
