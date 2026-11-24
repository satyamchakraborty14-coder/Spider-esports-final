package com.example.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

object SpiderCatalogSeeder {

    data class DefaultCategorySpec(
        val id: String,
        val name: String,
        val type: String,
        val presetKey: String,
        val order: Int
    )

    val defaultCategories = listOf(
        // Battle Royale
        DefaultCategorySpec("cat_full_map", "FULL MAP", "BATTLE_ROYALE", "battle_royale", 1),
        DefaultCategorySpec("cat_full_map_2", "FULL MAP 2", "BATTLE_ROYALE", "battle_royale", 2),
        DefaultCategorySpec("cat_solo_per_kill", "SOLO PER KILL", "BATTLE_ROYALE", "lone_wolf", 3),
        DefaultCategorySpec("cat_duo", "DUO", "BATTLE_ROYALE", "battle_royale", 4),
        DefaultCategorySpec("cat_survival", "SURVIVAL", "BATTLE_ROYALE", "clash_squad", 5),
        // Lone Wolf
        DefaultCategorySpec("cat_lw_1v1", "LW 1VS1", "LONE_WOLF", "lone_wolf", 6),
        DefaultCategorySpec("cat_lw_2v2", "LW 2VS2", "LONE_WOLF", "lone_wolf", 7),
        DefaultCategorySpec("cat_lw_head", "LW HEAD", "LONE_WOLF", "lone_wolf", 8),
        DefaultCategorySpec("cat_lw_loss", "LW LOSS", "LONE_WOLF", "lone_wolf", 9),
        // Clash Squad
        DefaultCategorySpec("cat_cs_head", "CS HEAD", "CLASH_SQUAD", "clash_squad", 10),
        DefaultCategorySpec("cat_cs_1v1", "CS 1VS1", "CLASH_SQUAD", "clash_squad", 11),
        DefaultCategorySpec("cat_cs_2v2", "CS 2VS2", "CLASH_SQUAD", "clash_squad", 12),
        // Additional
        DefaultCategorySpec("cat_night_mode", "NIGHT MODE", "ADDITIONAL", "championship", 13),
        DefaultCategorySpec("cat_free_match", "FREE MATCH", "ADDITIONAL", "battle_royale", 14),
        // Solo Formats
        DefaultCategorySpec("cat_standard_solo", "Standard Solo", "SOLO", "lone_wolf", 15),
        DefaultCategorySpec("cat_solo_kill_fmt", "Solo Per Kill", "SOLO", "lone_wolf", 16),
        DefaultCategorySpec("cat_full_map_solo", "Full Map Solo", "SOLO", "battle_royale", 17)
    )

    suspend fun seedDefaultCatalogIfEmpty(
        db: FirebaseFirestore,
        adminUid: String,
        adminEmail: String,
        forceCheck: Boolean = false
    ): Result<Boolean> {
        return try {
            val existingCats = db.collection("tournamentCategories").limit(1).get().await()
            if (!existingCats.isEmpty && !forceCheck) {
                return Result.success(false)
            }

            val batch = db.batch()
            val now = FieldValue.serverTimestamp()

            if (existingCats.isEmpty) {
                defaultCategories.forEach { cat ->
                    val ref = db.collection("tournamentCategories").document(cat.id)
                    batch.set(
                        ref,
                        mapOf(
                            "categoryName" to cat.name,
                            "categoryImageUrl" to "",
                            "categoryType" to cat.type,
                            "presetKey" to cat.presetKey,
                            "enabled" to true,
                            "randomTournament" to false,
                            "displayOrder" to cat.order,
                            "createdAt" to now,
                            "updatedAt" to now
                        )
                    )
                }
            }

            val existingBanners = db.collection("banners").limit(1).get().await()
            if (existingBanners.isEmpty) {
                val banners = listOf(
                    Triple("banner_championship", "SPIDER ARENA OPEN", "Free-entry community scrims & fair-play qualifiers") to ("championship" to "TOURNAMENTS"),
                    Triple("banner_rules", "PLAY FAIR. WIN RESPECT.", "Read anti-cheat, emulator & room timing rules") to ("lone_wolf" to "RULES"),
                    Triple("banner_support", "24/7 PLAYER SUPPORT", "Tap to connect directly on official Telegram Support") to ("clash_squad" to "SUPPORT")
                )
                banners.forEachIndexed { idx, pair ->
                    val (id, title, sub) = pair.first
                    val (preset, action) = pair.second
                    batch.set(
                        db.collection("banners").document(id),
                        mapOf(
                            "title" to title,
                            "subtitle" to sub,
                            "imageUrl" to "",
                            "presetKey" to preset,
                            "targetAction" to action,
                            "targetId" to "",
                            "displayOrder" to idx + 1,
                            "enabled" to true,
                            "createdAt" to now,
                            "updatedAt" to now
                        )
                    )
                }
            }

            val existingAnnouncements = db.collection("announcements").limit(1).get().await()
            if (existingAnnouncements.isEmpty) {
                batch.set(
                    db.collection("announcements").document("ann_important_1"),
                    mapOf(
                        "title" to "Spider Fair-Play Season Open",
                        "body" to "Free-entry and coin-entry tournaments are live across Battle Royale, Lone Wolf & Clash Squad. Add coins in Wallet or join open matches anytime.",
                        "category" to "IMPORTANT",
                        "telegramUrl" to "https://t.me/spideresports_official",
                        "whatsappUrl" to "",
                        "priority" to 1,
                        "enabled" to true,
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                )
                batch.set(
                    db.collection("announcements").document("ann_rules_master"),
                    mapOf(
                        "title" to "SPIDER ESPORTS OFFICIAL RULES",
                        "body" to "1. Zero tolerance for hacks, scripts, teaming, or unauthorized emulators.\n2. Room ID & Password unlock 5 minutes before match start for registered players only.\n3. Join your assigned slot promptly; late entries forfeit slots.\n4. Only verified match results affect rankings and rewards.\n5. Raise any score dispute within 30 minutes of match completion.",
                        "category" to "RULES",
                        "telegramUrl" to "https://t.me/spideresports_official",
                        "whatsappUrl" to "",
                        "priority" to 1,
                        "enabled" to true,
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                )
                batch.set(
                    db.collection("announcements").document("ann_support_links"),
                    mapOf(
                        "title" to "Official Support Channels",
                        "body" to "Reach our tournament marshals via Telegram Support or join our official WhatsApp Channel.",
                        "category" to "SUPPORT_CONFIG",
                        "telegramUrl" to "https://t.me/spideresports_official",
                        "whatsappUrl" to "",
                        "priority" to 1,
                        "enabled" to true,
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                )
            }

            val existingTournaments = db.collection("tournaments").limit(1).get().await()
            if (existingTournaments.isEmpty) {
                val starterTournaments = listOf(
                    mapOf(
                        "id" to "tour_full_map_1",
                        "categoryId" to "cat_full_map",
                        "title" to "Full Map Bermuda Clash",
                        "shortDescription" to "Full map survival with per-kill coin rewards.",
                        "gameMode" to "FULL MAP",
                        "mapName" to "Bermuda",
                        "entryFee" to 0.0,
                        "prizePool" to 200.0,
                        "perKillReward" to 10.0,
                        "maxParticipants" to 48,
                        "registeredCount" to 0,
                        "matchDate" to "Today",
                        "matchTime" to "19:00",
                        "registrationDeadline" to "18:50 Today",
                        "rules" to "Mobile only. Every verified kill earns per-kill reward + placement prize.",
                        "bannerUrl" to "",
                        "presetKey" to "battle_royale",
                        "visibility" to "PUBLIC",
                        "status" to "UPCOMING",
                        "soloFormat" to false,
                        "randomTournament" to false,
                        "roomReleased" to false
                    ),
                    mapOf(
                        "id" to "tour_full_map_2_1",
                        "categoryId" to "cat_full_map_2",
                        "title" to "Full Map 2 Purgatory Drop",
                        "shortDescription" to "High-intensity Full Map 2 match with per-kill bonus.",
                        "gameMode" to "FULL MAP 2",
                        "mapName" to "Purgatory",
                        "entryFee" to 0.0,
                        "prizePool" to 300.0,
                        "perKillReward" to 15.0,
                        "maxParticipants" to 48,
                        "registeredCount" to 0,
                        "matchDate" to "Today",
                        "matchTime" to "21:30",
                        "registrationDeadline" to "21:20 Today",
                        "rules" to "No hacks, scripts, or teaming. Verified kills credited after match.",
                        "bannerUrl" to "",
                        "presetKey" to "battle_royale",
                        "visibility" to "PUBLIC",
                        "status" to "UPCOMING",
                        "soloFormat" to false,
                        "randomTournament" to false,
                        "roomReleased" to false
                    ),
                    mapOf(
                        "id" to "tour_free_br_1",
                        "categoryId" to "cat_free_match",
                        "title" to "Spider Community Scrim #1",
                        "shortDescription" to "Open free-entry Battle Royale showcase match.",
                        "gameMode" to "FREE MATCH",
                        "mapName" to "Bermuda",
                        "entryFee" to 0.0,
                        "prizePool" to 0.0,
                        "perKillReward" to 0.0,
                        "maxParticipants" to 48,
                        "registeredCount" to 0,
                        "matchDate" to "Today",
                        "matchTime" to "20:00",
                        "registrationDeadline" to "19:50 Today",
                        "rules" to "Standard mobile-only fair play rules. No teaming.",
                        "bannerUrl" to "",
                        "presetKey" to "battle_royale",
                        "visibility" to "PUBLIC",
                        "status" to "UPCOMING",
                        "soloFormat" to true,
                        "randomTournament" to false,
                        "roomReleased" to false
                    ),
                    mapOf(
                        "id" to "tour_solo_std_1",
                        "categoryId" to "cat_standard_solo",
                        "title" to "Standard Solo Qualifier",
                        "shortDescription" to "Free-entry solo qualifier for Spider Ranking Points.",
                        "gameMode" to "Standard Solo",
                        "mapName" to "Purgatory",
                        "entryFee" to 0.0,
                        "prizePool" to 0.0,
                        "perKillReward" to 0.0,
                        "maxParticipants" to 48,
                        "registeredCount" to 0,
                        "matchDate" to "Today",
                        "matchTime" to "21:00",
                        "registrationDeadline" to "20:50 Today",
                        "rules" to "Solo only. Screen recording recommended for top 3.",
                        "bannerUrl" to "",
                        "presetKey" to "lone_wolf",
                        "visibility" to "PUBLIC",
                        "status" to "UPCOMING",
                        "soloFormat" to true,
                        "randomTournament" to false,
                        "roomReleased" to false
                    ),
                    mapOf(
                        "id" to "tour_lw_1v1_1",
                        "categoryId" to "cat_lw_1v1",
                        "title" to "Lone Wolf 1v1 Showdown",
                        "shortDescription" to "Free-entry 1v1 sniper & AR duel bracket.",
                        "gameMode" to "LW 1VS1",
                        "mapName" to "Iron Cage",
                        "entryFee" to 0.0,
                        "prizePool" to 0.0,
                        "perKillReward" to 0.0,
                        "maxParticipants" to 16,
                        "registeredCount" to 0,
                        "matchDate" to "Tomorrow",
                        "matchTime" to "18:30",
                        "registrationDeadline" to "18:20 Tomorrow",
                        "rules" to "No grenades, head-to-head fair duel.",
                        "bannerUrl" to "",
                        "presetKey" to "lone_wolf",
                        "visibility" to "PUBLIC",
                        "status" to "UPCOMING",
                        "soloFormat" to true,
                        "randomTournament" to false,
                        "roomReleased" to false
                    ),
                    mapOf(
                        "id" to "tour_cs_1v1_1",
                        "categoryId" to "cat_cs_1v1",
                        "title" to "Clash Squad Rush",
                        "shortDescription" to "Tactical Clash Squad showcase.",
                        "gameMode" to "CS 1VS1",
                        "mapName" to "Kalahari",
                        "entryFee" to 0.0,
                        "prizePool" to 0.0,
                        "perKillReward" to 0.0,
                        "maxParticipants" to 8,
                        "registeredCount" to 0,
                        "matchDate" to "Tomorrow",
                        "matchTime" to "19:30",
                        "registrationDeadline" to "19:20 Tomorrow",
                        "rules" to "Best of 7 rounds. Standard Clash Squad rules.",
                        "bannerUrl" to "",
                        "presetKey" to "clash_squad",
                        "visibility" to "PUBLIC",
                        "status" to "UPCOMING",
                        "soloFormat" to false,
                        "randomTournament" to false,
                        "roomReleased" to false
                    ),
                    mapOf(
                        "id" to "tour_night_mode_1",
                        "categoryId" to "cat_night_mode",
                        "title" to "Spider Midnight Invitational",
                        "shortDescription" to "Coin-entry night invitational with 1000 Coins prize pool.",
                        "gameMode" to "NIGHT MODE",
                        "mapName" to "Alpine",
                        "entryFee" to 25.0,
                        "prizePool" to 1000.0,
                        "perKillReward" to 10.0,
                        "maxParticipants" to 48,
                        "registeredCount" to 0,
                        "matchDate" to "Tonight",
                        "matchTime" to "23:00",
                        "registrationDeadline" to "22:45",
                        "rules" to "Entry fee: 25 Coins from Wallet. Fair-play rules apply; zero hacks or teaming.",
                        "bannerUrl" to "",
                        "presetKey" to "championship",
                        "visibility" to "PUBLIC",
                        "status" to "UPCOMING",
                        "soloFormat" to false,
                        "randomTournament" to false,
                        "roomReleased" to false
                    )
                )

                starterTournaments.forEach { t ->
                    val tid = t["id"] as String
                    val payload = HashMap<String, Any>(t).apply {
                        remove("id")
                        put("createdAt", now)
                        put("updatedAt", now)
                    }
                    batch.set(db.collection("tournaments").document(tid), payload)
                }
            }

            batch.set(
                db.collection("auditLogs").document("seed_${System.currentTimeMillis()}"),
                mapOf(
                    "actorId" to adminUid,
                    "actorEmail" to adminEmail,
                    "action" to "SEED_DEFAULT_CATALOG",
                    "targetCollection" to "tournamentCategories",
                    "targetId" to "default_catalog",
                    "summary" to "Initialized default Spider Esports categories, banners, rules, and starter matches.",
                    "createdAt" to now
                )
            )

            batch.commit().await()
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "tournamentCategories")
            Result.failure(e)
        }
    }
}
