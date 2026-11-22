package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.AccountValidationUtil
import com.example.data.AuthHelper
import com.example.model.UserProfile
import com.example.ui.theme.SpiderAmber
import com.example.ui.theme.SpiderBlack
import com.example.ui.theme.SpiderCardBorder
import com.example.ui.theme.SpiderCardDark
import com.example.ui.theme.SpiderEmerald
import com.example.ui.theme.SpiderRed
import com.example.ui.theme.SpiderRedBright
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.launch

@Composable
fun EmailVerificationGateScreen(
    user: FirebaseUser,
    onVerified: (FirebaseUser) -> Unit,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var busy by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpiderBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SpiderCardDark,
            border = BorderStroke(1.dp, SpiderCardBorder),
            modifier = Modifier.widthIn(max = 500.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Filled.MarkEmailRead, null, tint = SpiderRedBright, modifier = Modifier.size(48.dp))
                Text("VERIFY YOUR EMAIL", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                Text(
                    "We sent a verification link to ${user.email ?: "your email"}. Verify your email address to activate your Spider Esports account.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                statusMsg?.let { (ok, msg) ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (ok) SpiderEmerald else SpiderRedBright,
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = {
                        busy = true
                        statusMsg = null
                        scope.launch {
                            AuthHelper.reloadCurrentUser()
                                .onSuccess { refreshed ->
                                    busy = false
                                    if (refreshed != null && refreshed.isEmailVerified) {
                                        onVerified(refreshed)
                                    } else {
                                        statusMsg = false to "Email is not verified yet. Please check your inbox/spam folder."
                                    }
                                }
                                .onFailure { err ->
                                    busy = false
                                    statusMsg = false to (err.localizedMessage ?: "Verification check failed")
                                }
                        }
                    },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = SpiderRed, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("refresh_email_verification_button")
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), Color.White, strokeWidth = 2.dp)
                    else Text("I HAVE VERIFIED — CONTINUE", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        busy = true
                        scope.launch {
                            AuthHelper.resendEmailVerification()
                                .onSuccess {
                                    busy = false
                                    statusMsg = true to "Verification email resent!"
                                }
                                .onFailure { err ->
                                    busy = false
                                    statusMsg = false to (err.localizedMessage ?: "Failed to resend email")
                                }
                        }
                    },
                    enabled = !busy,
                    border = BorderStroke(1.dp, SpiderCardBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("resend_verification_button")
                ) {
                    Text("Resend Verification Email", color = TextPrimary)
                }

                TextButton(
                    onClick = { AuthHelper.signOut(credentialManager, scope, onSignedOut) }
                ) {
                    Text("Use Different Account / Sign Out", color = TextMuted)
                }
            }
        }
    }
}

@Composable
fun CompleteProfileGateScreen(
    user: FirebaseUser,
    profile: UserProfile,
    onSubmitProfile: (
        gameName: String,
        gameUid: String,
        phoneE164: String,
        referralCodeInput: String,
        acceptedTerms: Boolean,
        onDone: (Boolean, String?) -> Unit
    ) -> Unit,
    onPhoneVerified: (String) -> Unit,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var gameName by remember(profile.id) { mutableStateOf(profile.inGameName.ifBlank { profile.username }) }
    var gameUid by remember(profile.id) { mutableStateOf(profile.inGameUid) }
    var countryCode by remember { mutableStateOf("+91") }
    var mobileNumber by remember(profile.id) { mutableStateOf(profile.phoneNumber) }
    var referralCode by remember(profile.id) { mutableStateOf(profile.referredBy) }
    var acceptedTerms by remember { mutableStateOf(profile.acceptedTerms) }
    var busy by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showPhoneOtpDialog by remember { mutableStateOf(false) }

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
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SpiderCardDark,
            border = BorderStroke(1.dp, SpiderCardBorder),
            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.VerifiedUser, null, tint = SpiderRedBright, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("COMPLETE PLAYER PROFILE", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                }
                Text(
                    "Set your unique Game Name, Game UID, and international mobile number to enter tournaments.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                if (!errorMsg.isNullOrBlank()) {
                    Text(errorMsg!!, style = MaterialTheme.typography.bodySmall, color = SpiderRedBright)
                }

                OutlinedTextField(
                    value = gameName,
                    onValueChange = { gameName = it },
                    label = { Text("Game Name (Username) *") },
                    supportingText = { Text("Case-insensitive unique (SpiderX = spiderx)", color = TextMuted) },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_username_input")
                )
                OutlinedTextField(
                    value = gameUid,
                    onValueChange = { gameUid = it },
                    label = { Text("Game UID *") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_gameuid_input")
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
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = fieldColors,
                        modifier = Modifier.weight(1f).testTag("onboarding_phone_input")
                    )
                }

                if (profile.phoneVerified) {
                    Text("Mobile Verified via Firebase Phone Auth", style = MaterialTheme.typography.labelSmall, color = SpiderEmerald)
                } else {
                    OutlinedButton(
                        onClick = {
                            val norm = AccountValidationUtil.normalizePhoneE164(countryCode, mobileNumber)
                            if (!AccountValidationUtil.isValidPhoneE164(norm)) {
                                errorMsg = "Enter a valid international number first (e.g. +919876543210)."
                            } else {
                                errorMsg = null
                                showPhoneOtpDialog = true
                            }
                        },
                        border = BorderStroke(1.dp, SpiderCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.PhoneAndroid, null, tint = SpiderAmber, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Verify Mobile via OTP (Firebase)", color = TextPrimary, style = MaterialTheme.typography.labelMedium)
                    }
                }

                OutlinedTextField(
                    value = referralCode,
                    onValueChange = { referralCode = it.uppercase() },
                    label = { Text("Referral Code (Optional)") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_referral_input")
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = acceptedTerms,
                        onCheckedChange = { acceptedTerms = it },
                        colors = CheckboxDefaults.colors(checkedColor = SpiderRed, uncheckedColor = TextSecondary)
                    )
                    Text("I accept the Terms and Conditions & Privacy Policy", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }

                Button(
                    onClick = {
                        errorMsg = null
                        AccountValidationUtil.validateUsername(gameName)?.let { errorMsg = it; return@Button }
                        AccountValidationUtil.validateGameUid(gameUid)?.let { errorMsg = it; return@Button }
                        val normPhone = AccountValidationUtil.normalizePhoneE164(countryCode, mobileNumber)
                        if (!AccountValidationUtil.isValidPhoneE164(normPhone)) {
                            errorMsg = "Enter a valid international mobile number (e.g. +919876543210)."
                            return@Button
                        }
                        if (!AccountValidationUtil.isValidReferralCodeFormat(referralCode)) {
                            errorMsg = "Referral code must be 4–24 characters."
                            return@Button
                        }
                        if (!acceptedTerms) {
                            errorMsg = "Please accept the Terms and Conditions and Privacy Policy."
                            return@Button
                        }
                        busy = true
                        onSubmitProfile(gameName, gameUid, normPhone, referralCode, acceptedTerms) { ok, err ->
                            busy = false
                            if (!ok) errorMsg = err
                        }
                    },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = SpiderRed, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("complete_profile_button")
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), Color.White, strokeWidth = 2.dp)
                    else Text("ACTIVATE ACCOUNT", fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = { AuthHelper.signOut(credentialManager, scope, onSignedOut) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Sign Out", color = TextMuted)
                }
            }
        }
    }

    if (showPhoneOtpDialog) {
        val normPhone = AccountValidationUtil.normalizePhoneE164(countryCode, mobileNumber)
        PhoneOtpVerificationDialog(
            initialPhoneE164 = normPhone,
            onDismiss = { showPhoneOtpDialog = false },
            onVerified = { verifiedE164 ->
                mobileNumber = verifiedE164
                onPhoneVerified(verifiedE164)
                showPhoneOtpDialog = false
            }
        )
    }
}

@Composable
fun PhoneOtpVerificationDialog(
    initialPhoneE164: String,
    onDismiss: () -> Unit,
    onVerified: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var phoneInput by remember { mutableStateOf(initialPhoneE164) }
    var otpCode by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    var verifying by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = SpiderRed,
        unfocusedBorderColor = SpiderCardBorder,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpiderCardDark,
        title = { Text("Verify Mobile Number", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Enter your mobile number in international E.164 format (e.g. +919876543210) to receive a Firebase SMS OTP.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("Mobile Number (+CountryCode...)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                if (verificationId != null) {
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { otpCode = it.filter { ch -> ch.isDigit() }.take(6) },
                        label = { Text("6-Digit SMS OTP") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth().testTag("phone_otp_input")
                    )
                }
                statusMsg?.let { (ok, msg) ->
                    Text(msg, style = MaterialTheme.typography.bodySmall, color = if (ok) SpiderEmerald else SpiderRedBright)
                }
            }
        },
        confirmButton = {
            if (verificationId == null) {
                Button(
                    onClick = {
                        val norm = AccountValidationUtil.normalizePhoneE164("+91", phoneInput)
                        if (!AccountValidationUtil.isValidPhoneE164(norm)) {
                            statusMsg = false to "Invalid E.164 format. Include country code like +919876543210."
                            return@Button
                        }
                        val activity = context as? Activity
                        if (activity == null) {
                            statusMsg = false to "Activity context unavailable."
                            return@Button
                        }
                        phoneInput = norm
                        sending = true
                        statusMsg = null
                        AuthHelper.startPhoneVerification(
                            activity = activity,
                            phoneNumberE164 = norm,
                            callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                    sending = false
                                    verifying = true
                                    scope.launch {
                                        AuthHelper.linkOrUpdatePhoneCredential(credential)
                                            .onSuccess { verified ->
                                                verifying = false
                                                onVerified(verified.ifBlank { norm })
                                            }
                                            .onFailure { err ->
                                                verifying = false
                                                statusMsg = false to (err.localizedMessage ?: "Auto verification failed")
                                            }
                                    }
                                }

                                override fun onVerificationFailed(e: FirebaseException) {
                                    sending = false
                                    val raw = e.localizedMessage ?: ""
                                    statusMsg = false to if (raw.contains("not allowed", ignoreCase = true)) {
                                        "Phone OTP requires enabling the Phone provider in Firebase Console (Authentication -> Sign-in method). Your mobile number uniqueness is still enforced."
                                    } else {
                                        raw.ifBlank { "Firebase Phone Auth failed. Enable Phone sign-in & Play Integrity in Firebase Console." }
                                    }
                                }

                                override fun onCodeSent(verId: String, token: PhoneAuthProvider.ForceResendingToken) {
                                    sending = false
                                    verificationId = verId
                                    statusMsg = true to "OTP code sent to $norm."
                                }
                            }
                        )
                    },
                    enabled = !sending,
                    colors = ButtonDefaults.buttonColors(containerColor = SpiderRed)
                ) {
                    Text(if (sending) "Sending..." else "Send OTP")
                }
            } else {
                Button(
                    onClick = {
                        val verId = verificationId ?: return@Button
                        if (otpCode.length < 6) {
                            statusMsg = false to "Enter the 6-digit OTP code."
                            return@Button
                        }
                        verifying = true
                        scope.launch {
                            val cred = PhoneAuthProvider.getCredential(verId, otpCode)
                            AuthHelper.linkOrUpdatePhoneCredential(cred)
                                .onSuccess { verified ->
                                    verifying = false
                                    onVerified(verified.ifBlank { phoneInput })
                                }
                                .onFailure { err ->
                                    verifying = false
                                    statusMsg = false to (err.localizedMessage ?: "Invalid OTP code")
                                }
                        }
                    },
                    enabled = !verifying,
                    colors = ButtonDefaults.buttonColors(containerColor = SpiderRed)
                ) {
                    Text(if (verifying) "Verifying..." else "Verify OTP")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}
