package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.ImagePickerUtil
import com.example.model.MapBanner
import com.example.model.Tournament
import com.example.model.TournamentCategory
import com.example.ui.components.SpiderArtworkImage
import com.example.ui.theme.SpiderCardBorder
import com.example.ui.theme.SpiderCardDark
import com.example.ui.theme.SpiderEmerald
import com.example.ui.theme.SpiderGold
import com.example.ui.theme.SpiderRed
import com.example.ui.theme.SpiderRedBright
import com.example.ui.theme.SpiderSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

internal val DEFAULT_MAP_SUGGESTIONS = listOf(
    "Bermuda",
    "Purgatory",
    "Kalahari",
    "Alpine",
    "Nexterra",
    "Iron Cage",
    "Solara"
)

internal fun findSavedMapBanner(mapName: String, mapBanners: List<MapBanner>): MapBanner? {
    val clean = mapName.trim()
    if (clean.isEmpty()) return null
    return mapBanners.firstOrNull { it.enabled && it.mapName.trim().equals(clean, ignoreCase = true) }
}

@Composable
fun TournamentEditorDialog(
    initial: Tournament?,
    categories: List<TournamentCategory>,
    mapBanners: List<MapBanner> = emptyList(),
    onSaveMapBanner: (mapName: String, bannerUrl: String, presetKey: String) -> Unit = { _, _, _ -> },
    onDismiss: () -> Unit,
    onSave: (
        categoryId: String,
        title: String,
        shortDescription: String,
        matchDetails: String,
        gameMode: String,
        mapName: String,
        entryFee: Double,
        prizePool: Double,
        perKillReward: Double,
        maxParticipants: Long,
        matchDate: String,
        matchTime: String,
        registrationDeadline: String,
        rules: String,
        bannerUrl: String,
        presetKey: String,
        visibility: String,
        status: String,
        soloFormat: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val initialCat = remember(initial, categories) {
        if (initial != null) {
            categories.firstOrNull { it.id == initial.categoryId } ?: categories.firstOrNull()
        } else {
            categories.firstOrNull()
        }
    }

    var title by remember(initial) { mutableStateOf(initial?.title ?: "") }
    var selectedCat by remember(initial, categories) { mutableStateOf(initialCat) }
    var shortDesc by remember(initial) { mutableStateOf(initial?.shortDescription ?: "") }
    var matchDetails by remember(initial) { mutableStateOf(initial?.matchDetails ?: "") }
    var gameMode by remember(initial) {
        mutableStateOf(initial?.gameMode ?: initialCat?.categoryName ?: "FULL MAP")
    }
    var mapName by remember(initial) { mutableStateOf(initial?.mapName ?: "Bermuda") }
    var entryFee by remember(initial) {
        mutableStateOf(
            if (initial == null || initial.entryFee == 0.0) "0"
            else if (initial.entryFee % 1.0 == 0.0) initial.entryFee.toInt().toString()
            else initial.entryFee.toString()
        )
    }
    var prizePool by remember(initial) {
        mutableStateOf(
            if (initial == null || initial.prizePool == 0.0) "0"
            else if (initial.prizePool % 1.0 == 0.0) initial.prizePool.toInt().toString()
            else initial.prizePool.toString()
        )
    }
    var perKill by remember(initial) {
        mutableStateOf(
            if (initial == null || initial.perKillReward == 0.0) "0"
            else if (initial.perKillReward % 1.0 == 0.0) initial.perKillReward.toInt().toString()
            else initial.perKillReward.toString()
        )
    }
    var maxP by remember(initial) { mutableStateOf((initial?.maxParticipants ?: 48L).toString()) }
    var matchDate by remember(initial) { mutableStateOf(initial?.matchDate ?: "Today") }
    var matchTime by remember(initial) { mutableStateOf(initial?.matchTime ?: "20:00") }
    var deadline by remember(initial) { mutableStateOf(initial?.registrationDeadline ?: "19:45 Today") }
    var rules by remember(initial) {
        mutableStateOf(initial?.rules ?: "Fair-play rules enforced. Zero hacks or teaming.")
    }
    var bannerUrl by remember(initial) {
        mutableStateOf(
            initial?.bannerUrl?.takeIf { it.isNotBlank() }
                ?: findSavedMapBanner(initial?.mapName ?: "Bermuda", mapBanners)?.bannerUrl
                ?: ""
        )
    }
    var lastAutoBannerUrl by remember(initial) {
        mutableStateOf(
            if (initial?.bannerUrl.isNullOrBlank()) {
                findSavedMapBanner(initial?.mapName ?: "Bermuda", mapBanners)?.bannerUrl ?: ""
            } else ""
        )
    }
    var presetKey by remember(initial) {
        mutableStateOf(initial?.presetKey ?: initialCat?.presetKey ?: "battle_royale")
    }
    var visibility by remember(initial) { mutableStateOf(initial?.visibility ?: "PUBLIC") }
    var status by remember(initial) { mutableStateOf(initial?.status ?: "UPCOMING") }
    var solo by remember(initial) {
        mutableStateOf(initial?.soloFormat ?: (initialCat?.categoryType == "SOLO"))
    }

    // Auto-load saved map banner when mapBanners arrive on create (or when banner is still empty)
    LaunchedEffect(mapBanners, mapName) {
        val saved = findSavedMapBanner(mapName, mapBanners)
        if (saved != null && saved.bannerUrl.isNotBlank() && (bannerUrl.isBlank() || bannerUrl == lastAutoBannerUrl)) {
            bannerUrl = saved.bannerUrl
            presetKey = saved.presetKey
            lastAutoBannerUrl = saved.bannerUrl
        }
    }

    fun applyMapSelection(newMap: String, forceApplySaved: Boolean) {
        mapName = newMap
        val saved = findSavedMapBanner(newMap, mapBanners)
        if (saved != null && saved.bannerUrl.isNotBlank()) {
            if (forceApplySaved || bannerUrl.isBlank() || bannerUrl == lastAutoBannerUrl) {
                bannerUrl = saved.bannerUrl
                presetKey = saved.presetKey
                lastAutoBannerUrl = saved.bannerUrl
            }
        } else if (forceApplySaved && bannerUrl == lastAutoBannerUrl) {
            bannerUrl = ""
            lastAutoBannerUrl = ""
        }
    }

    val mapChoices = remember(mapBanners) {
        (mapBanners.filter { it.enabled }.map { it.mapName } + DEFAULT_MAP_SUGGESTIONS)
            .distinctBy { it.trim().lowercase() }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                ImagePickerUtil.encodeUriToDataUri(context, uri).onSuccess {
                    bannerUrl = it
                    lastAutoBannerUrl = ""
                }
            }
        }
    }

    val matchedSavedBanner = remember(mapName, mapBanners) { findSavedMapBanner(mapName, mapBanners) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpiderCardDark,
        title = {
            Text(
                if (initial == null) "Create Tournament" else "Edit ${initial.title}",
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    SpiderArtworkImage(
                        imageUrl = bannerUrl,
                        presetKey = presetKey,
                        contentDescription = "Tournament banner preview",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (matchedSavedBanner != null && matchedSavedBanner.bannerUrl.isNotBlank()) {
                    Text(
                        text = "Saved map banner loaded for ${matchedSavedBanner.mapName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SpiderEmerald
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        border = BorderStroke(1.dp, SpiderCardBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Banner Image", color = TextPrimary, style = MaterialTheme.typography.labelSmall)
                    }
                    if (bannerUrl.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                bannerUrl = ""
                                lastAutoBannerUrl = ""
                            },
                            border = BorderStroke(1.dp, SpiderRed.copy(alpha = 0.5f))
                        ) {
                            Text("Clear", color = SpiderRedBright, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                if (bannerUrl.isNotBlank() && mapName.trim().length >= 2) {
                    val alreadySaved = matchedSavedBanner?.bannerUrl == bannerUrl
                    OutlinedButton(
                        onClick = {
                            onSaveMapBanner(mapName.trim(), bannerUrl, presetKey)
                            lastAutoBannerUrl = bannerUrl
                        },
                        enabled = !alreadySaved,
                        border = BorderStroke(1.dp, SpiderGold.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth().testTag("save_map_banner_inline")
                    ) {
                        Icon(Icons.Filled.BookmarkAdd, null, tint = SpiderGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (alreadySaved) "Banner Saved for $mapName" else "Save Banner for Map '$mapName'",
                            color = SpiderGold,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                val fc = adminFieldColors()
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tournament Title*") },
                    singleLine = true,
                    colors = fc,
                    modifier = Modifier.fillMaxWidth().testTag("tournament_title_input")
                )

                Text(
                    "Category: ${selectedCat?.categoryName ?: "Select Category"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SpiderGold
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCat?.id == cat.id,
                            onClick = {
                                selectedCat = cat
                                presetKey = cat.presetKey
                                if (initial == null || gameMode.isBlank()) {
                                    gameMode = cat.categoryName
                                }
                                if (cat.categoryType == "SOLO" || cat.categoryName.contains("SOLO", ignoreCase = true)) {
                                    solo = true
                                }
                            },
                            label = { Text(cat.categoryName, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SpiderRed,
                                selectedLabelColor = Color.White,
                                containerColor = SpiderSurfaceElevated,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = shortDesc,
                    onValueChange = { shortDesc = it },
                    label = { Text("Short Summary (Card Subtitle)") },
                    singleLine = true,
                    colors = fc,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = matchDetails,
                    onValueChange = { matchDetails = it },
                    label = { Text("Match Details (Shows in Details -> Description)") },
                    minLines = 3,
                    maxLines = 6,
                    colors = fc,
                    modifier = Modifier.fillMaxWidth().testTag("tournament_match_details_input")
                )

                Text("Select Map (Auto-fills saved map banner)", style = MaterialTheme.typography.labelSmall, color = SpiderGold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    mapChoices.forEach { m ->
                        val hasSaved = findSavedMapBanner(m, mapBanners)?.bannerUrl?.isNotBlank() == true
                        FilterChip(
                            selected = mapName.trim().equals(m, ignoreCase = true),
                            onClick = { applyMapSelection(m, forceApplySaved = true) },
                            label = {
                                Text(
                                    if (hasSaved) "$m ★" else m,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SpiderRed,
                                selectedLabelColor = Color.White,
                                containerColor = SpiderSurfaceElevated,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = gameMode,
                        onValueChange = { gameMode = it },
                        label = { Text("Game Mode") },
                        singleLine = true,
                        colors = fc,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = mapName,
                        onValueChange = { applyMapSelection(it, forceApplySaved = false) },
                        label = { Text("Map") },
                        singleLine = true,
                        colors = fc,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = entryFee,
                        onValueChange = { entryFee = it.filterDecimalInput() },
                        label = { Text("Entry (Coins)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = fc,
                        modifier = Modifier.weight(1f).testTag("tournament_entry_input")
                    )
                    OutlinedTextField(
                        value = prizePool,
                        onValueChange = { prizePool = it.filterDecimalInput() },
                        label = { Text("Prize Pool") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = fc,
                        modifier = Modifier.weight(1f).testTag("tournament_prize_input")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = perKill,
                        onValueChange = { perKill = it.filterDecimalInput() },
                        label = { Text("Per Kill (Coins)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = fc,
                        modifier = Modifier.weight(1f).testTag("tournament_per_kill_input")
                    )
                    OutlinedTextField(
                        value = maxP,
                        onValueChange = { maxP = it.filter { c -> c.isDigit() } },
                        label = { Text("Max Slots") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fc,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = matchDate,
                        onValueChange = { matchDate = it },
                        label = { Text("Date") },
                        singleLine = true,
                        colors = fc,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = matchTime,
                        onValueChange = { matchTime = it },
                        label = { Text("Time") },
                        singleLine = true,
                        colors = fc,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    label = { Text("Registration Deadline") },
                    singleLine = true,
                    colors = fc,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bannerUrl,
                    onValueChange = {
                        bannerUrl = it
                        lastAutoBannerUrl = ""
                    },
                    label = { Text("Banner Image URL (optional)") },
                    singleLine = true,
                    colors = fc,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rules,
                    onValueChange = { rules = it },
                    label = { Text("Tournament Rules") },
                    minLines = 2,
                    colors = fc,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Status", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("UPCOMING", "LIVE", "FULL", "COMPLETED", "CANCELLED", "ARCHIVED").forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SpiderRed,
                                selectedLabelColor = Color.White,
                                containerColor = SpiderSurfaceElevated,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Publicly Visible", color = TextPrimary)
                    Switch(
                        checked = visibility == "PUBLIC",
                        onCheckedChange = { visibility = if (it) "PUBLIC" else "HIDDEN" },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SpiderRed)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Solo Tab Format", color = TextPrimary)
                    Switch(
                        checked = solo,
                        onCheckedChange = { solo = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SpiderRed)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cat = selectedCat ?: return@Button
                    onSave(
                        cat.id,
                        title,
                        shortDesc,
                        matchDetails,
                        gameMode.ifBlank { cat.categoryName },
                        mapName,
                        entryFee.toDoubleOrNull() ?: 0.0,
                        prizePool.toDoubleOrNull() ?: 0.0,
                        perKill.toDoubleOrNull() ?: 0.0,
                        maxP.toLongOrNull() ?: 48L,
                        matchDate,
                        matchTime,
                        deadline,
                        rules,
                        bannerUrl,
                        presetKey,
                        visibility,
                        status,
                        solo
                    )
                },
                enabled = title.trim().length >= 2 && selectedCat != null,
                colors = ButtonDefaults.buttonColors(containerColor = SpiderRed),
                modifier = Modifier.testTag("tournament_save_button")
            ) {
                Text(if (initial == null) "Publish" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

internal fun String.filterDecimalInput(): String {
    var dotSeen = false
    val sb = StringBuilder()
    for (ch in this) {
        if (ch.isDigit()) {
            sb.append(ch)
        } else if (ch == '.' && !dotSeen) {
            dotSeen = true
            sb.append(ch)
        }
    }
    return sb.toString()
}
