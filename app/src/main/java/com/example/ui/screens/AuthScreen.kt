package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.AccountValidationUtil
import com.example.data.AuthHelper
import com.example.data.SpiderUserRepository
import com.example.ui.theme.SpiderBlack
import com.example.ui.theme.SpiderCardBorder
import com.example.ui.theme.SpiderCardDark
import com.example.ui.theme.SpiderEmerald
import com.example.ui.theme.SpiderRed
import com.example.ui.theme.SpiderRedBright
import com.example.ui.theme.SpiderSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private enum class AuthMode { SIGN_IN, SIGN_UP }

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onRegistrationBusyChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var authMode by remember { mutableStateOf(AuthMode.SIGN_IN) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var legalTitle by remember { mutableStateOf<String?>(null) }

    // Sign Up fields (unique account registration paired with Google Auth)
    var gameName by remember { mutableStateOf("") }
    var gameUid by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("+91") }
    var mobileNumber by remember { mutableStateOf("") }
    var referralCode by remember { mutableStateOf("") }
    var acceptedTerms by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AuthHelper.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = SpiderRed,
        unfocusedBorderColor = SpiderCardBorder,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedLabelColor = SpiderRedBright,
        unfocusedLabelColor = TextSecondary
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpiderBlack)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_banner_championship_1795273772511),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.35f
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            SpiderBlack.copy(alpha = 0.55f),
                            SpiderBlack.copy(alpha = 0.88f),
                            SpiderBlack
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .widthIn(max = 520.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .border(2.5.dp, SpiderRed, CircleShape)
                        .background(SpiderCardDark),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_spider_logo),
                        contentDescription = "Spider Esports Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "SPIDER ESPORTS",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "PLAY FAIR. WIN RESPECT AND MONEY.",
                    style = MaterialTheme.typography.labelLarge,
                    color = SpiderRedBright,
                    textAlign = TextAlign.Center
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SpiderCardDark.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, SpiderCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SpiderSurfaceElevated)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(AuthMode.SIGN_IN to "SIGN IN", AuthMode.SIGN_UP to "SIGN UP").forEach { (mode, label) ->
                            val selected = authMode == mode
                            Surface(
                                onClick = {
                                    authMode = mode
                                    errorMessage = null
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) SpiderRed else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("auth_tab_${mode.name}")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) Color.White else TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = SpiderEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (authMode == AuthMode.SIGN_IN) "Verified Tournament Arena" else "Create Unique Spider Account",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SpiderRed.copy(alpha = 0.18f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = SpiderRedBright,
                                modifier = Modifier.padding(10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (authMode == AuthMode.SIGN_IN) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SpiderSurfaceElevated,
                            border = BorderStroke(1.dp, SpiderCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Filled.VerifiedUser, null, tint = SpiderEmerald, modifier = Modifier.size(22.dp))
                                Text(
                                    text = "Sign in securely with your verified Google account. Returning players open directly; new players complete their unique Spider Esports profile.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = {
                                errorMessage = null
                                isLoading = true
                                AuthHelper.onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        errorMessage = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = {
                                        isLoading = false
                                        val sha1 = AuthHelper.getAppSigningSha1(context)
                                        errorMessage = if (sha1.isNotBlank()) {
                                            "Google Sign-In was cancelled or closed. If no Google account picker appeared, register this APK's SHA-1 in Firebase Console -> Project Settings:\nSHA-1: $sha1"
                                        } else {
                                            "Google Sign-In was cancelled. Please select a Google account to continue."
                                        }
                                    }
                                )
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = SpiderRed, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("google_signin_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(Modifier.size(20.dp), Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.Security, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Sign in with Google", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text(
                            text = "Your Google account verifies your unique email & credentials securely (no plaintext passwords stored). Enter your unique gamer profile details below:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        OutlinedTextField(
                            value = gameName,
                            onValueChange = { gameName = it },
                            label = { Text("Game Name (Username) *") },
                            supportingText = { Text("Case-insensitive unique (SpiderX = spiderx)", color = TextMuted) },
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth().testTag("signup_username_input")
                        )
                        OutlinedTextField(
                            value = gameUid,
                            onValueChange = { gameUid = it },
                            label = { Text("Game UID *") },
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth().testTag("signup_gameuid_input")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = countryCode,
                                onValueChange = { countryCode = it },
                                label = { Text("Code") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = fieldColors,
                                modifier = Modifier.width(86.dp)
                            )
                            OutlinedTextField(
                                value = mobileNumber,
                                onValueChange = { mobileNumber = it },
                                label = { Text("Mobile Number *") },
                                supportingText = { Text("One account per mobile number (no OTP required)", color = TextMuted) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = fieldColors,
                                modifier = Modifier.weight(1f).testTag("signup_phone_input")
                            )
                        }
                        OutlinedTextField(
                            value = referralCode,
                            onValueChange = { referralCode = it.uppercase() },
                            label = { Text("Referral Code (Optional)") },
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth().testTag("signup_referral_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = acceptedTerms,
                                onCheckedChange = { acceptedTerms = it },
                                colors = CheckboxDefaults.colors(checkedColor = SpiderRed, uncheckedColor = TextSecondary),
                                modifier = Modifier.testTag("signup_terms_checkbox")
                            )
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("I accept ", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text(
                                    "Terms",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SpiderRedBright,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier.clickable { legalTitle = "Terms and Conditions" }
                                )
                                Text("&", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text(
                                    "Privacy Policy",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SpiderRedBright,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier.clickable { legalTitle = "Privacy Policy" }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                errorMessage = null
                                AccountValidationUtil.validateUsername(gameName)?.let {
                                    errorMessage = it
                                    return@Button
                                }
                                AccountValidationUtil.validateGameUid(gameUid)?.let {
                                    errorMessage = it
                                    return@Button
                                }
                                val normalizedPhone = AccountValidationUtil.normalizePhoneE164(countryCode, mobileNumber)
                                if (!AccountValidationUtil.isValidPhoneE164(normalizedPhone)) {
                                    errorMessage = "Enter a valid mobile number with country code (e.g. +91 9876543210)."
                                    return@Button
                                }
                                if (!AccountValidationUtil.isValidReferralCodeFormat(referralCode)) {
                                    errorMessage = "Referral code must be 4–24 letters/numbers."
                                    return@Button
                                }
                                if (!acceptedTerms) {
                                    errorMessage = "Please accept the Terms and Conditions and Privacy Policy."
                                    return@Button
                                }

                                isLoading = true
                                onRegistrationBusyChange(true)
                                AuthHelper.onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        val signedInUser = Firebase.auth.currentUser
                                        if (signedInUser == null) {
                                            isLoading = false
                                            onRegistrationBusyChange(false)
                                            errorMessage = "Google authentication failed."
                                            return@onGoogleSignInClicked
                                        }
                                        scope.launch {
                                            val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
                                            val existingDoc = runCatching {
                                                db.collection("users").document(signedInUser.uid).get().await()
                                            }.getOrNull()
                                            if (existingDoc?.exists() == true && existingDoc.getBoolean("onboardingCompleted") == true) {
                                                isLoading = false
                                                onRegistrationBusyChange(false)
                                                onAuthSuccess()
                                                return@launch
                                            }
                                            val userRepo = SpiderUserRepository(db)
                                            val claimRes = userRepo.completeRegistrationProfile(
                                                user = signedInUser,
                                                gameName = gameName,
                                                gameUid = gameUid,
                                                phoneE164 = normalizedPhone,
                                                referralCodeInput = referralCode,
                                                acceptedTerms = acceptedTerms
                                            )
                                            claimRes.onSuccess {
                                                isLoading = false
                                                onRegistrationBusyChange(false)
                                                onAuthSuccess()
                                            }.onFailure { err ->
                                                val errText = err.localizedMessage ?: "Account validation failed."
                                                if (existingDoc?.exists() != true) {
                                                    AuthHelper.signOut(credentialManager, scope) {
                                                        isLoading = false
                                                        onRegistrationBusyChange(false)
                                                        errorMessage = errText
                                                    }
                                                } else {
                                                    isLoading = false
                                                    onRegistrationBusyChange(false)
                                                    errorMessage = errText
                                                }
                                            }
                                        }
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        onRegistrationBusyChange(false)
                                        errorMessage = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = {
                                        isLoading = false
                                        onRegistrationBusyChange(false)
                                        val sha1 = AuthHelper.getAppSigningSha1(context)
                                        errorMessage = if (sha1.isNotBlank()) {
                                            "Google Sign-In was cancelled or closed. If no Google account picker appeared, register this APK's SHA-1 in Firebase Console -> Project Settings:\nSHA-1: $sha1"
                                        } else {
                                            "Google Sign-In was cancelled. Please select a Google account to continue."
                                        }
                                    }
                                )
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = SpiderRed, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("create_account_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(Modifier.size(20.dp), Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.Security, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("CONTINUE WITH GOOGLE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    legalTitle?.let { title ->
        AlertDialog(
            onDismissRequest = { legalTitle = null },
            containerColor = SpiderCardDark,
            title = { Text(title, color = TextPrimary) },
            text = {
                Text(
                    text = if (title == "Privacy Policy") {
                        "Spider Esports stores your authenticated account details, unique gamer handle, Game UID, normalized mobile number, match registrations, and verified statistics securely in Cloud Firestore. Passwords and credentials are handled exclusively by Firebase Authentication and Google Identity."
                    } else {
                        "1. Fair Play: Cheating, multi-accounting, or score manipulation results in permanent suspension.\n2. Unique Accounts: Email, Game Name, Game UID, and Mobile Number are strictly one per player.\n3. Referrals: Referral bonuses are reviewed and credited server-side after verification."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { legalTitle = null }) { Text("Close", color = SpiderRedBright) }
            }
        )
    }
}
