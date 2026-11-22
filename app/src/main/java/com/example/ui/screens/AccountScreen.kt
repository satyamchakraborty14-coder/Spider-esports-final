package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.credentials.CredentialManager
import com.example.data.AuthHelper
import com.example.data.ImagePickerUtil
import com.example.model.UserProfile
import com.example.ui.components.SpiderArtworkImage
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AccountScreen(
    profile: UserProfile?,
    isAdmin: Boolean,
    unreadNotificationsCount: Int,
    telegramUrl: String,
    onEditProfile: (String, String, String, String, String, String, (Boolean) -> Unit) -> Unit,
    onOpenMyMatches: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenTeams: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenAdminDashboard: () -> Unit,
    onPhoneVerified: (String) -> Unit = {},
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var showEditDialog by remember { mutableStateOf(false) }
    var showPhoneOtpDialog by remember { mutableStateOf(false) }
    var legalDialogTitle by remember { mutableStateOf<String?>(null) }

    val regDate = remember(profile?.createdAt) {
        profile?.createdAt?.toDate()?.let {
            SimpleDateFormat("dd MMM yyyy", Locale.US).format(it)
        } ?: "Joined Today"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpiderBlack)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Text(
            text = "ACCOUNT",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        )

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Profile Card
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SpiderCardDark,
                    border = BorderStroke(1.dp, SpiderCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, SpiderRed, CircleShape)
                                    .background(SpiderSurfaceElevated)
                            ) {
                                SpiderArtworkImage(
                                    imageUrl = profile?.photoUrl ?: "",
                                    presetKey = "logo",
                                    contentDescription = "Profile photo",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = profile?.username ?: "SpiderPlayer",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary
                                    )
                                    if (profile?.emailVerified == true) {
                                        Spacer(Modifier.width(6.dp))
                                        Icon(Icons.Filled.Verified, "Email Verified", tint = SpiderEmerald, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Text(
                                    text = "UID: ${profile?.playerUid ?: "---"} • Joined $regDate",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = profile?.email ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                                if (profile != null && profile.phoneNumber.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = profile.phoneNumber,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                        StatusPill(
                                            text = if (profile.phoneVerified) "PHONE VERIFIED" else "UNVERIFIED",
                                            color = if (profile.phoneVerified) SpiderEmerald else TextMuted
                                        )
                                    }
                                }
                            }
                        }

                        if (profile != null && !profile.phoneVerified) {
                            OutlinedButton(
                                onClick = { showPhoneOtpDialog = true },
                                border = BorderStroke(1.dp, SpiderCardBorder),
                                modifier = Modifier.fillMaxWidth().testTag("account_verify_phone_button")
                            ) {
                                Text("Verify Mobile Number (Firebase OTP)", color = TextPrimary, style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatPill("MATCHES", "${profile?.matchesPlayed ?: 0}")
                            StatPill("WINS", "${profile?.wins ?: 0}")
                            StatPill("KILLS", "${profile?.verifiedKills ?: 0}")
                            StatPill("POINTS", "${profile?.rankingPoints ?: 0}")
                        }
                    }
                }
            }

            // Admin Dashboard Shortcut (strictly visible ONLY to verified owner Gmail satyamchakraborty14@gmail.com)
            val isVerifiedOwnerAdmin = isAdmin &&
                profile?.emailVerified == true &&
                profile.email.equals(AuthHelper.BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true)
            if (isVerifiedOwnerAdmin) {
                item {
                    Surface(
                        onClick = onOpenAdminDashboard,
                        shape = RoundedCornerShape(14.dp),
                        color = SpiderRed.copy(alpha = 0.16f),
                        border = BorderStroke(1.5.dp, SpiderRed),
                        modifier = Modifier.fillMaxWidth().testTag("open_admin_dashboard")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.AdminPanelSettings, null, tint = SpiderRedBright, modifier = Modifier.size(26.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ADMIN COMMAND CENTER", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Manage Categories, Random Tournaments, Banners, Users & Wallets", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            StatusPill("ADMIN", SpiderGold)
                        }
                    }
                }
            }

            // Account Options
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SpiderCardDark,
                    border = BorderStroke(1.dp, SpiderCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        AccountOptionRow(Icons.Filled.Edit, "Edit Profile", "Update username, avatar & In-Game UID", onClick = { showEditDialog = true }, testTag = "account_edit_profile")
                        AccountOptionRow(Icons.Filled.SportsEsports, "My Matches", "Ongoing, Upcoming & Completed matches", onClick = onOpenMyMatches)
                        AccountOptionRow(Icons.Filled.AccountBalanceWallet, "My Wallet", "Coins breakdown & redemption status", onClick = onOpenWallet)
                        AccountOptionRow(Icons.AutoMirrored.Filled.ReceiptLong, "Transaction History", "View verified wallet ledger", onClick = onOpenWallet)
                        AccountOptionRow(Icons.Filled.Groups, "My Teams", "Create & manage Duo/Squad rosters", onClick = onOpenTeams)
                        AccountOptionRow(
                            Icons.Filled.Notifications,
                            "Notifications",
                            if (unreadNotificationsCount > 0) "$unreadNotificationsCount unread alerts" else "All caught up",
                            onClick = onOpenNotifications
                        )
                        AccountOptionRow(Icons.Filled.HeadsetMic, "Customer Support", "Tickets, disputes & WhatsApp/Telegram", onClick = onOpenSupport)
                        AccountOptionRow(Icons.AutoMirrored.Filled.Send, "Telegram Support", "Join official Spider Esports Telegram", onClick = {
                            val cleanUrl = telegramUrl.trim()
                            if (cleanUrl.startsWith("https://", true) || cleanUrl.startsWith("http://", true)) {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, cleanUrl.toUri()))
                                }
                            }
                        })
                        AccountOptionRow(Icons.Filled.Policy, "Privacy Policy", "How Spider Esports protects your data", onClick = {
                            legalDialogTitle = "Privacy Policy"
                        })
                        AccountOptionRow(Icons.Filled.Description, "Terms and Conditions", "Fair-play, age & tournament compliance", onClick = {
                            legalDialogTitle = "Terms and Conditions"
                        })
                    }
                }
            }

            // Logout
            item {
                Button(
                    onClick = {
                        AuthHelper.signOut(credentialManager, scope, onSignedOut)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpiderSurfaceElevated, contentColor = SpiderRedBright),
                    border = BorderStroke(1.dp, SpiderRed.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("logout_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("LOGOUT", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    if (showEditDialog && profile != null) {
        EditProfileDialog(
            profile = profile,
            onDismiss = { showEditDialog = false },
            onSave = { username, display, ign, igUid, photoUrl, refBy, done ->
                onEditProfile(username, display, ign, igUid, photoUrl, refBy) { ok ->
                    done(ok)
                    if (ok) showEditDialog = false
                }
            }
        )
    }

    if (showPhoneOtpDialog && profile != null) {
        PhoneOtpVerificationDialog(
            initialPhoneE164 = profile.phoneNumber,
            onDismiss = { showPhoneOtpDialog = false },
            onVerified = { verifiedPhone ->
                onPhoneVerified(verifiedPhone)
                showPhoneOtpDialog = false
            }
        )
    }

    legalDialogTitle?.let { title ->
        AlertDialog(
            onDismissRequest = { legalDialogTitle = null },
            containerColor = SpiderCardDark,
            title = { Text(title, color = TextPrimary, style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    text = if (title == "Privacy Policy") {
                        "Spider Esports stores your authenticated Google profile, gamer handle, match registrations, and verified statistics securely in Cloud Firestore. PII (email, account status) is isolated to your account and authorized tournament administrators. No ad networks or ad tracking SDKs are integrated."
                    } else {
                        "1. Fair Play: Cheating, hacking, or score manipulation results in permanent suspension.\n2. Eligibility: Paid-entry tournaments and real-money redemptions remain disabled until adult operator legal, age, and payment-gateway approvals are in place.\n3. Rankings: Only verified match results affect leaderboards and awards."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { legalDialogTitle = null }) { Text("Close", color = SpiderRedBright) }
            }
        )
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SpiderSurfaceElevated,
        border = BorderStroke(1.dp, SpiderCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleSmall, color = SpiderRedBright, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}

@Composable
private fun AccountOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp)
            .let { if (testTag.isNotEmpty()) it.testTag(testTag) else it },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = SpiderRedBright, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextMuted)
    }
}

@Composable
private fun EditProfileDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, (Boolean) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf(profile.username) }
    var displayName by remember { mutableStateOf(profile.displayName) }
    var ign by remember { mutableStateOf(profile.inGameName) }
    var igUid by remember { mutableStateOf(profile.inGameUid) }
    var photoUrl by remember { mutableStateOf(profile.photoUrl) }
    var referredBy by remember { mutableStateOf(profile.referredBy) }
    var saving by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                ImagePickerUtil.encodeUriToDataUri(context, uri).onSuccess { dataUri ->
                    photoUrl = dataUri
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpiderCardDark,
        title = { Text("Edit Profile", color = TextPrimary, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape).border(1.5.dp, SpiderRed, CircleShape)
                    ) {
                        SpiderArtworkImage(imageUrl = photoUrl, presetKey = "logo", contentDescription = "Avatar preview", modifier = Modifier.fillMaxSize())
                    }
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        border = BorderStroke(1.dp, SpiderCardBorder)
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pick Avatar", color = TextPrimary)
                    }
                }

                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpiderRed,
                    unfocusedBorderColor = SpiderCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = displayName, onValueChange = { displayName = it }, label = { Text("Display Name") }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = ign, onValueChange = { ign = it }, label = { Text("In-Game Name") }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = igUid, onValueChange = { igUid = it }, label = { Text("In-Game UID") }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = referredBy,
                    onValueChange = { if (profile.referredBy.isBlank()) referredBy = it.uppercase() },
                    enabled = profile.referredBy.isBlank(),
                    label = { Text(if (profile.referredBy.isBlank()) "Referral Code (optional)" else "Referred By (Locked)") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    saving = true
                    onSave(username, displayName, ign, igUid, photoUrl, referredBy) { saving = false }
                },
                enabled = !saving && username.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = SpiderRed)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}
