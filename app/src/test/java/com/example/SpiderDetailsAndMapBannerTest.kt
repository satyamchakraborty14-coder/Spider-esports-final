package com.example

import com.example.model.MapBanner
import com.example.model.Registration
import com.example.model.TournamentParticipant
import com.example.ui.screens.findSavedMapBanner
import com.example.ui.screens.orderTournamentParticipants
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpiderDetailsAndMapBannerTest {

    @Test
    fun `joined members start with current user first then other joined players chronologically`() {
        val participants = listOf(
            TournamentParticipant(id = "u1", tournamentId = "t1", userId = "u1", playerName = "AlphaFF", username = "alpha", createdAt = Timestamp(100L, 0)),
            TournamentParticipant(id = "u2", tournamentId = "t1", userId = "u2", playerName = "BravoFF", username = "bravo", createdAt = Timestamp(200L, 0)),
            TournamentParticipant(id = "me", tournamentId = "t1", userId = "me", playerName = "SatyamFF", username = "satyam", createdAt = Timestamp(300L, 0))
        )

        val ordered = orderTournamentParticipants(
            tournamentId = "t1",
            participants = participants,
            currentUserRegistration = null,
            currentUserId = "me"
        )

        assertEquals(listOf("SatyamFF", "AlphaFF", "BravoFF"), ordered.map { it.playerName })
    }

    @Test
    fun `joined members show legacy current user registration first even if subcollection lacks doc`() {
        val participants = listOf(
            TournamentParticipant(id = "u1", tournamentId = "t1", userId = "u1", playerName = "AlphaFF", username = "alpha", createdAt = Timestamp(100L, 0))
        )
        val reg = Registration(
            id = "t1_me",
            tournamentId = "t1",
            userId = "me",
            username = "satyam",
            inGameName = "SatyamPro",
            inGameUid = "999888777"
        )

        val ordered = orderTournamentParticipants(
            tournamentId = "t1",
            participants = participants,
            currentUserRegistration = reg,
            currentUserId = "me"
        )

        assertEquals(listOf("SatyamPro", "AlphaFF"), ordered.map { it.playerName })
    }

    @Test
    fun `when user has not joined yet, other joined players appear chronologically`() {
        val participants = listOf(
            TournamentParticipant(id = "u2", tournamentId = "t1", userId = "u2", playerName = "SecondPlayer", username = "p2", createdAt = Timestamp(200L, 0)),
            TournamentParticipant(id = "u1", tournamentId = "t1", userId = "u1", playerName = "FirstPlayer", username = "p1", createdAt = Timestamp(100L, 0))
        )

        val ordered = orderTournamentParticipants(
            tournamentId = "t1",
            participants = participants,
            currentUserRegistration = null,
            currentUserId = "viewer_not_joined"
        )

        assertEquals(listOf("FirstPlayer", "SecondPlayer"), ordered.map { it.playerName })
    }

    @Test
    fun `findSavedMapBanner matches case insensitively and skips disabled banners`() {
        val banners = listOf(
            MapBanner(id = "map_bermuda", mapName = "Bermuda", bannerUrl = "https://img/bermuda.png", enabled = true),
            MapBanner(id = "map_kalahari", mapName = "Kalahari", bannerUrl = "https://img/kalahari.png", enabled = false)
        )

        assertEquals("https://img/bermuda.png", findSavedMapBanner(" bermuda ", banners)?.bannerUrl)
        assertNull(findSavedMapBanner("Kalahari", banners))
        assertNull(findSavedMapBanner("Alpine", banners))
    }
}
