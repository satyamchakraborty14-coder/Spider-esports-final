package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.example.data.ImagePickerUtil
import com.example.model.CustomTournamentType
import com.example.model.PromoBanner
import com.example.model.TournamentCategory
import com.example.ui.components.SpiderArtworkImage
import com.example.ui.components.StatusPill
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
import kotlinx.coroutines.launch

@Composable
fun AdminCategoriesSection(
    categories: List<TournamentCategory>,
    adminViewModel: SpiderAdminViewModel
) {
    var editingCategory by remember { mutableStateOf<TournamentCategory?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = SpiderRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_add_category")
            ) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("ADD / UPLOAD CATEGORY ARTWORK", style = MaterialTheme.typography.labelLarge)
            }
        }
        items(categories, key = { it.id }) { cat ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SpiderCardDark,
                border = BorderStroke(1.dp, SpiderCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(10.dp))
                    ) {
                        SpiderArtworkImage(
                            imageUrl = cat.categoryImageUrl,
                            presetKey = cat.presetKey,
                            contentDescription = cat.categoryName,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(cat.categoryName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("${cat.categoryType} • Order #${cat.displayOrder}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Spacer(Modifier.height(4.dp))
                        StatusPill(if (cat.enabled) "ENABLED" else "DISABLED", if (cat.enabled) SpiderEmerald else SpiderRedBright)
                    }
                    IconButton(onClick = { editingCategory = cat }) {
                        Icon(Icons.Filled.Edit, "Edit category", tint = TextPrimary)
                    }
                    IconButton(onClick = { adminViewModel.deleteCategory(cat.id) }) {
                        Icon(Icons.Filled.Delete, "Delete category", tint = SpiderRedBright)
                    }
                }
            }
        }
    }

    if (showCreateDialog || editingCategory != null) {
        CategoryEditorDialog(
            initial = editingCategory,
            nextOrder = (categories.maxOfOrNull { it.displayOrder } ?: 0L) + 1L,
            onDismiss = {
                showCreateDialog = false
                editingCategory = null
            },
            onSave = { name, imgUrl, catType, preset, enabled, order ->
                adminViewModel.saveCategory(editingCategory?.id, name, imgUrl, catType, preset, enabled, order)
                showCreateDialog = false
                editingCategory = null
            }
        )
    }
}

@Composable
private fun CategoryEditorDialog(
    initial: TournamentCategory?,
    nextOrder: Long,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Boolean, Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initial?.categoryName ?: "") }
    var imageUrl by remember { mutableStateOf(initial?.categoryImageUrl ?: "") }
    var categoryType by remember { mutableStateOf(initial?.categoryType ?: "BATTLE_ROYALE") }
    var presetKey by remember { mutableStateOf(initial?.presetKey ?: "battle_royale") }
    var enabled by remember { mutableStateOf(initial?.enabled ?: true) }
    var orderText by remember { mutableStateOf((initial?.displayOrder ?: nextOrder).toString()) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                ImagePickerUtil.encodeUriToDataUri(context, uri).onSuccess { imageUrl = it }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpiderCardDark,
        title = { Text(if (initial == null) "Create Category" else "Edit ${initial.categoryName}", color = TextPrimary) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(12.dp))) {
                    SpiderArtworkImage(imageUrl = imageUrl, presetKey = presetKey, contentDescription = "Preview", modifier = Modifier.fillMaxSize())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        border = BorderStroke(1.dp, SpiderCardBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Upload Image", color = TextPrimary)
                    }
                    OutlinedButton(
                        onClick = { imageUrl = "" },
                        border = BorderStroke(1.dp, SpiderRed.copy(alpha = 0.5f))
                    ) {
                        Text("Clear", color = SpiderRedBright)
                    }
                }
                val fc = adminFieldColors()
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Category Name") }, singleLine = true, colors = fc, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Or Paste Image URL (https://...)") }, singleLine = true, colors = fc, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = orderText, onValueChange = { orderText = it.filter { c -> c.isDigit() } }, label = { Text("Display Order") }, singleLine = true, colors = fc, modifier = Modifier.fillMaxWidth())

                Text("Category Group", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("BATTLE_ROYALE", "LONE_WOLF", "CLASH_SQUAD", "ADDITIONAL", "SOLO", "CUSTOM").forEach { t ->
                        FilterChip(
                            selected = categoryType == t,
                            onClick = { categoryType = t },
                            label = { Text(t, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SpiderRed, selectedLabelColor = Color.White, containerColor = SpiderSurfaceElevated, labelColor = TextSecondary)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Enabled in User App", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = enabled, onCheckedChange = { enabled = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SpiderRed))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, imageUrl, categoryType, presetKey, enabled, orderText.toLongOrNull() ?: 1L) },
                enabled = name.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = SpiderRed)
            ) { Text("Save Changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
    )
}

@Composable
fun AdminRandomTournamentsSection(
    customTypes: List<CustomTournamentType>,
    adminViewModel: SpiderAdminViewModel
) {
    var editingType by remember { mutableStateOf<CustomTournamentType?>(null) }
    var showCreate by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(
                onClick = { showCreate = true },
                colors = ButtonDefaults.buttonColors(containerColor = SpiderRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_create_random_tournament")
            ) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("CREATE RANDOM TOURNAMENT", style = MaterialTheme.typography.labelLarge)
            }
        }
        items(customTypes, key = { it.id }) { item ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SpiderCardDark,
                border = BorderStroke(1.dp, SpiderGold.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp))) {
                        SpiderArtworkImage(imageUrl = item.bannerUrl, presetKey = "championship", contentDescription = item.displayName, modifier = Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.displayName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("${item.gameMode} • ${item.mapName} • Max ${item.maxParticipants}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                            StatusPill(item.status, SpiderGold)
                            StatusPill(if (item.enabled && !item.archived) "LIVE" else "HIDDEN", if (item.enabled && !item.archived) SpiderEmerald else SpiderRedBright)
                        }
                    }
                    IconButton(onClick = { editingType = item }) { Icon(Icons.Filled.Edit, "Edit", tint = TextPrimary) }
                    IconButton(onClick = { adminViewModel.deleteRandomTournament(item) }) { Icon(Icons.Filled.Delete, "Delete", tint = SpiderRedBright) }
                }
            }
        }
    }

    if (showCreate || editingType != null) {
        RandomTournamentEditorDialog(
            initial = editingType,
            nextOrder = (customTypes.maxOfOrNull { it.displayOrder } ?: 20L) + 1L,
            onDismiss = {
                showCreate = false
                editingType = null
            },
            onSave = { name, banner, desc, mode, map, entry, prize, perKill, maxP, date, time, deadline, rules, vis, status, order, enabled, archived ->
                adminViewModel.saveRandomTournament(
                    editingType?.id, editingType?.categoryId,
                    name, banner, desc, mode, map, entry, prize, perKill, maxP,
                    date, time, deadline, rules, vis, status, order, enabled, archived
                )
                showCreate = false
                editingType = null
            }
        )
    }
}

@Composable
private fun RandomTournamentEditorDialog(
    initial: CustomTournamentType?,
    nextOrder: Long,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, Double, Double, Double, Long, String, String, String, String, String, String, Long, Boolean, Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var displayName by remember { mutableStateOf(initial?.displayName ?: "") }
    var bannerUrl by remember { mutableStateOf(initial?.bannerUrl ?: "") }
    var shortDesc by remember { mutableStateOf(initial?.shortDescription ?: "") }
    var gameMode by remember { mutableStateOf(initial?.gameMode ?: "CUSTOM ARENA") }
    var mapName by remember { mutableStateOf(initial?.mapName ?: "Bermuda") }
    var entryFee by remember { mutableStateOf((initial?.entryFee ?: 0.0).toString()) }
    var prizePool by remember { mutableStateOf((initial?.prizePool ?: 0.0).toString()) }
    var perKill by remember { mutableStateOf((initial?.perKillReward ?: 0.0).toString()) }
    var maxParticipants by remember { mutableStateOf((initial?.maxParticipants ?: 48L).toString()) }
    var matchDate by remember { mutableStateOf(initial?.matchDate ?: "Today") }
    var matchTime by remember { mutableStateOf(initial?.matchTime ?: "20:00") }
    var deadline by remember { mutableStateOf(initial?.registrationDeadline ?: "19:45 Today") }
    var rules by remember { mutableStateOf(initial?.rules ?: "Fair play required. Zero hacks or teaming.") }
    var visibility by remember { mutableStateOf(initial?.visibility ?: "PUBLIC") }
    var status by remember { mutableStateOf(initial?.status ?: "UPCOMING") }
    var displayOrder by remember { mutableStateOf((initial?.displayOrder ?: nextOrder).toString()) }
    var enabled by remember { mutableStateOf(initial?.enabled ?: true) }
    var archived by remember { mutableStateOf(initial?.archived ?: false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch { ImagePickerUtil.encodeUriToDataUri(context, uri).onSuccess { bannerUrl = it } }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpiderCardDark,
        title = { Text(if (initial == null) "Create Random Tournament" else "Edit ${initial.displayName}", color = TextPrimary) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(10.dp))) {
                    SpiderArtworkImage(imageUrl = bannerUrl, presetKey = "championship", contentDescription = "Banner preview", modifier = Modifier.fillMaxSize())
                }
                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    border = BorderStroke(1.dp, SpiderCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PhotoLibrary, null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Upload Custom Banner Image", color = TextPrimary)
                }
                val fc = adminFieldColors()
                OutlinedTextField(value = displayName, onValueChange = { displayName = it }, label = { Text("Tournament Display Name*") }, singleLine = true, colors = fc, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bannerUrl, onValueChange = { bannerUrl = it }, label = { Text("Banner Image URL (optional)") }, singleLine = true, colors = fc, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = shortDesc, onValueChange = { shortDesc = it }, label = { Text("Short Description") }, colors = fc, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = gameMode, onValueChange = { gameMode = it }, label = { Text("Game Mode") }, singleLine = true, colors = fc, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = mapName, onValueChange = { mapName = it }, label = { Text("Map") }, singleLine = true, colors = fc, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = entryFee, onValueChange = { entryFee = it }, label = { Text("Entry Fee") }, singleLine = true, colors = fc, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = prizePool, onValueChange = { prizePool = it }, label = { Text("Prize Pool") }, singleLine = true, colors = fc, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = perKill, onValueChange = { perKill = it }, label = { Text("Per Kill") }, singleLine = true, colors = fc, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = maxParticipants, onValueChange = { maxParticipants = it.filter { c -> c.isDigit() } }, label = { Text("Max Players") }, singleLine = true, colors = fc, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = matchDate, onValueChange = { matchDate = it }, label = { Text("Match Date") }, singleLine = true, colors = fc, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = matchTime, onValueChange = { matchTime = it }, label = { Text("Match Time") }, singleLine = true, colors = fc, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = deadline, onValueChange = { deadline = it }, label = { Text("Registration Deadline") }, singleLine = true, colors = fc, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = rules, onValueChange = { rules = it }, label = { Text("Tournament Rules") }, minLines = 2, colors = fc, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = displayOrder, onValueChange = { displayOrder = it.filter { c -> c.isDigit() } }, label = { Text("Display Position Order") }, singleLine = true, colors = fc, modifier = Modifier.fillMaxWidth())

                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("UPCOMING", "LIVE", "COMPLETED", "ARCHIVED").forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SpiderRed, selectedLabelColor = Color.White, containerColor = SpiderSurfaceElevated, labelColor = TextSecondary)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Visible & Enabled", color = TextPrimary)
                    Switch(
                        checked = enabled && visibility == "PUBLIC" && !archived,
                        onCheckedChange = {
                            enabled = it
                            visibility = if (it) "PUBLIC" else "HIDDEN"
                            archived = !it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SpiderRed)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        displayName, bannerUrl, shortDesc, gameMode, mapName,
                        entryFee.toDoubleOrNull() ?: 0.0,
                        prizePool.toDoubleOrNull() ?: 0.0,
                        perKill.toDoubleOrNull() ?: 0.0,
                        maxParticipants.toLongOrNull() ?: 48L,
                        matchDate, matchTime, deadline, rules,
                        visibility, status,
                        displayOrder.toLongOrNull() ?: nextOrder,
                        enabled, archived
                    )
                },
                enabled = displayName.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = SpiderRed)
            ) { Text("Save & Publish") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
    )
}

@Composable
fun AdminBannersSection(
    banners: List<PromoBanner>,
    adminViewModel: SpiderAdminViewModel
) {
    var editingBanner by remember { mutableStateOf<PromoBanner?>(null) }
    var showCreate by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(
                onClick = { showCreate = true },
                colors = ButtonDefaults.buttonColors(containerColor = SpiderRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("ADD PROMO BANNER", style = MaterialTheme.typography.labelLarge)
            }
        }
        items(banners, key = { it.id }) { banner ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SpiderCardDark,
                border = BorderStroke(1.dp, SpiderCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(86.dp).height(54.dp).clip(RoundedCornerShape(8.dp))) {
                        SpiderArtworkImage(imageUrl = banner.imageUrl, presetKey = banner.presetKey, contentDescription = banner.title, modifier = Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(banner.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("Pos #${banner.displayOrder} • Action: ${banner.targetAction}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    IconButton(onClick = { editingBanner = banner }) { Icon(Icons.Filled.Edit, "Edit", tint = TextPrimary) }
                    IconButton(onClick = { adminViewModel.deleteBanner(banner.id) }) { Icon(Icons.Filled.Delete, "Delete", tint = SpiderRedBright) }
                }
            }
        }
    }

    if (showCreate || editingBanner != null) {
        BannerEditorDialog(
            initial = editingBanner,
            nextOrder = (banners.maxOfOrNull { it.displayOrder } ?: 0L) + 1L,
            onDismiss = { showCreate = false; editingBanner = null },
            onSave = { title, sub, img, action, order, enabled ->
                adminViewModel.saveBanner(editingBanner?.id, title, sub, img, editingBanner?.presetKey ?: "championship", action, order, enabled)
                showCreate = false
                editingBanner = null
            }
        )
    }
}

@Composable
private fun BannerEditorDialog(
    initial: PromoBanner?,
    nextOrder: Long,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Long, Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var subtitle by remember { mutableStateOf(initial?.subtitle ?: "") }
    var imageUrl by remember { mutableStateOf(initial?.imageUrl ?: "") }
    var targetAction by remember { mutableStateOf(initial?.targetAction ?: "TOURNAMENTS") }
    var orderText by remember { mutableStateOf((initial?.displayOrder ?: nextOrder).toString()) }
    var enabled by remember { mutableStateOf(initial?.enabled ?: true) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { ImagePickerUtil.encodeUriToDataUri(context, uri).onSuccess { imageUrl = it } }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpiderCardDark,
        title = { Text(if (initial == null) "Add Promo Banner" else "Edit Banner", color = TextPrimary) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(10.dp))) {
                    SpiderArtworkImage(imageUrl = imageUrl, presetKey = initial?.presetKey ?: "championship", contentDescription = "Preview", modifier = Modifier.fillMaxSize())
                }
                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    border = BorderStroke(1.dp, SpiderCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PhotoLibrary, null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pick Banner Image", color = TextPrimary)
                }
                val fc = adminFieldColors()
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Banner Title") }, singleLine = true, colors = fc, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = subtitle, onValueChange = { subtitle = it }, label = { Text("Subtitle") }, colors = fc, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Or Image URL") }, singleLine = true, colors = fc, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = orderText, onValueChange = { orderText = it.filter { c -> c.isDigit() } }, label = { Text("Order") }, singleLine = true, colors = fc, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("TOURNAMENTS", "RULES", "SUPPORT", "RANKING").forEach { act ->
                        FilterChip(
                            selected = targetAction == act,
                            onClick = { targetAction = act },
                            label = { Text(act, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SpiderRed, selectedLabelColor = Color.White, containerColor = SpiderSurfaceElevated, labelColor = TextSecondary)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Enabled", color = TextPrimary)
                    Switch(checked = enabled, onCheckedChange = { enabled = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SpiderRed))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, subtitle, imageUrl, targetAction, orderText.toLongOrNull() ?: nextOrder, enabled) },
                enabled = title.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = SpiderRed)
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
    )
}

@Composable
fun adminFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SpiderRed,
    unfocusedBorderColor = SpiderCardBorder,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = TextSecondary,
    unfocusedLabelColor = TextMuted
)
