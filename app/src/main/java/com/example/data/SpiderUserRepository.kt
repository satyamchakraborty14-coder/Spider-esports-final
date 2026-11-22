package com.example.data

import com.example.model.Dispute
import com.example.model.NotificationItem
import com.example.model.PaymentOrder
import com.example.model.PayoutRequest
import com.example.model.ReferralRecord
import com.example.model.SupportTicket
import com.example.model.TeamRoster
import com.example.model.UserProfile
import com.example.model.Wallet
import com.example.model.WalletConfig
import com.example.model.WalletTransaction
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale

class SpiderUserRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in.")

    suspend fun ensureUserProfile(user: FirebaseUser): Result<Unit> {
        val path = "users/${user.uid}"
        return try {
            val userRef = db.collection("users").document(user.uid)
            val snap = userRef.get().await()
            if (!snap.exists()) {
                val baseName = (user.displayName ?: user.email?.substringBefore("@") ?: "SpiderPlayer")
                    .replace(Regex("[^a-zA-Z0-9_ ]"), "")
                    .trim()
                    .ifBlank { "SpiderPlayer" }
                    .take(24)
                val baseCleanHandle = baseName.lowercase(Locale.US)
                    .replace(Regex("[^a-z0-9_]"), "_")
                    .take(14)
                    .ifBlank { "spider" } + "_" + user.uid.take(5).lowercase(Locale.US)
                var cleanHandle = baseCleanHandle
                val handleCheckSnap = db.collection("usernames").document(cleanHandle.lowercase(Locale.US)).get().await()
                if (handleCheckSnap.exists() && handleCheckSnap.getString("userId") != user.uid) {
                    cleanHandle = "sp_${user.uid.take(8).lowercase(Locale.US)}_${System.currentTimeMillis() % 1000}"
                }
                val finalHandleLower = cleanHandle.lowercase(Locale.US)
                val playerUid = "SE" + user.uid.take(6).uppercase(Locale.US)
                val refCode = "SPDR" + user.uid.take(6).uppercase(Locale.US)
                val normEmail = AccountValidationUtil.normalizeEmail(user.email ?: "")
                val emailHash = if (normEmail.isNotBlank()) AccountValidationUtil.sha256Hex(normEmail) else ""
                val phone = user.phoneNumber ?: ""
                val now = FieldValue.serverTimestamp()

                val emailClaimSnap = if (emailHash.isNotBlank()) {
                    db.collection("emailClaims").document(emailHash).get().await()
                } else null
                val refCodeSnap = db.collection("referralCodes").document(refCode).get().await()
                val usernameClaimSnap = db.collection("usernames").document(finalHandleLower).get().await()

                val batch = db.batch()
                batch.set(
                    userRef,
                    mapOf(
                        "userId" to user.uid,
                        "username" to cleanHandle,
                        "displayName" to baseName,
                        "email" to normEmail,
                        "photoUrl" to (user.photoUrl?.toString() ?: ""),
                        "playerUid" to playerUid,
                        "inGameName" to baseName,
                        "inGameUid" to "",
                        "phoneNumber" to phone,
                        "phoneVerified" to phone.isNotBlank(),
                        "emailVerified" to user.isEmailVerified,
                        "acceptedTerms" to false,
                        "onboardingCompleted" to false,
                        "referralCode" to refCode,
                        "referredBy" to "",
                        "language" to "EN",
                        "status" to "ACTIVE",
                        "matchesPlayed" to 0L,
                        "wins" to 0L,
                        "verifiedKills" to 0L,
                        "rankingPoints" to 0L,
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                )
                if (!usernameClaimSnap.exists()) {
                    batch.set(
                        db.collection("usernames").document(finalHandleLower),
                        mapOf("userId" to user.uid, "username" to cleanHandle, "createdAt" to now)
                    )
                }
                if (emailHash.isNotBlank() && emailClaimSnap?.exists() != true) {
                    batch.set(
                        db.collection("emailClaims").document(emailHash),
                        mapOf("userId" to user.uid, "createdAt" to now)
                    )
                }
                if (!refCodeSnap.exists()) {
                    batch.set(
                        db.collection("referralCodes").document(refCode),
                        mapOf("userId" to user.uid, "username" to cleanHandle, "referralCode" to refCode, "createdAt" to now)
                    )
                }
                batch.commit().await()
            } else if (user.isEmailVerified && snap.getBoolean("emailVerified") != true) {
                userRef.update(
                    mapOf("emailVerified" to true, "updatedAt" to FieldValue.serverTimestamp())
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun completeRegistrationProfile(
        user: FirebaseUser,
        gameName: String,
        gameUid: String,
        phoneE164: String,
        referralCodeInput: String,
        acceptedTerms: Boolean
    ): Result<Unit> {
        val path = "users/${user.uid}"
        return try {
            val uid = requireUserId()
            check(uid == user.uid) { "Authenticated user mismatch." }
            require(acceptedTerms) { "You must accept the Terms and Conditions and Privacy Policy." }
            AccountValidationUtil.validateUsername(gameName)?.let { throw IllegalArgumentException(it) }
            AccountValidationUtil.validateGameUid(gameUid)?.let { throw IllegalArgumentException(it) }
            require(AccountValidationUtil.isValidPhoneE164(phoneE164)) {
                "Enter a valid international mobile number (e.g. +919876543210)."
            }
            val trimmedGameName = gameName.trim()
            val newUsernameLower = AccountValidationUtil.normalizeUsernameKey(trimmedGameName)
            val trimmedGameUid = gameUid.trim()
            val gameUidHash = AccountValidationUtil.sha256Hex(AccountValidationUtil.normalizeGameUidKey(trimmedGameUid))
            val phoneHash = AccountValidationUtil.sha256Hex(phoneE164)
            val normEmail = AccountValidationUtil.normalizeEmail(user.email ?: "")
            val emailHash = if (normEmail.isNotBlank()) AccountValidationUtil.sha256Hex(normEmail) else ""
            val refCodeUpper = AccountValidationUtil.normalizeReferralCode(referralCodeInput)
            if (refCodeUpper.isNotBlank() && !AccountValidationUtil.isValidReferralCodeFormat(refCodeUpper)) {
                throw IllegalArgumentException("Referral code must be 4–24 letters, numbers, hyphens, or underscores.")
            }
            val ownRefCode = "SPDR" + uid.take(6).uppercase(Locale.US)
            val playerUid = "SE" + uid.take(6).uppercase(Locale.US)

            if (refCodeUpper.isNotBlank() && refCodeUpper == ownRefCode) {
                throw IllegalArgumentException("You cannot use your own referral code.")
            }

            db.runTransaction { tx ->
                val userRef = db.collection("users").document(uid)
                val userSnap = tx.get(userRef)

                val newUsernameRef = db.collection("usernames").document(newUsernameLower)
                val newUsernameSnap = tx.get(newUsernameRef)

                val newPhoneRef = db.collection("phoneClaims").document(phoneHash)
                val newPhoneSnap = tx.get(newPhoneRef)

                val newGameUidRef = db.collection("gameUidClaims").document(gameUidHash)
                val newGameUidSnap = tx.get(newGameUidRef)

                val emailRef = if (emailHash.isNotBlank()) db.collection("emailClaims").document(emailHash) else null
                val emailSnap = emailRef?.let { tx.get(it) }

                val ownCodeRef = db.collection("referralCodes").document(ownRefCode)
                val ownCodeSnap = tx.get(ownCodeRef)

                val refCodeLookupSnap = if (refCodeUpper.isNotBlank()) {
                    tx.get(db.collection("referralCodes").document(refCodeUpper))
                } else null
                val existingReferralDocSnap = if (refCodeUpper.isNotBlank()) {
                    tx.get(db.collection("referrals").document(uid))
                } else null

                if (newUsernameSnap.exists() && newUsernameSnap.getString("userId") != uid) {
                    throw IllegalStateException("Game Name '$trimmedGameName' is already taken (case-insensitive).")
                }
                if (newPhoneSnap.exists() && newPhoneSnap.getString("userId") != uid) {
                    throw IllegalStateException("Mobile number '$phoneE164' is already registered to another account.")
                }
                if (newGameUidSnap.exists() && newGameUidSnap.getString("userId") != uid) {
                    throw IllegalStateException("Game UID '$trimmedGameUid' is already registered to another account.")
                }
                if (emailSnap != null && emailSnap.exists() && emailSnap.getString("userId") != uid) {
                    throw IllegalStateException("Email '$normEmail' is already registered.")
                }

                var referrerUid = ""
                var referrerUsername = ""
                if (refCodeUpper.isNotBlank()) {
                    if (refCodeLookupSnap == null || !refCodeLookupSnap.exists()) {
                        throw IllegalStateException("Referral code '$refCodeUpper' does not exist.")
                    }
                    referrerUid = refCodeLookupSnap.getString("userId") ?: ""
                    referrerUsername = refCodeLookupSnap.getString("username") ?: "Player"
                    if (referrerUid.isBlank() || referrerUid == uid) {
                        throw IllegalStateException("Self-referral is not permitted.")
                    }
                }

                // Release old username/phone/gameUid claims ONLY if the docs exist and belong to this uid (read before write)
                val oldUsernameLower = userSnap.getString("username")?.let { AccountValidationUtil.normalizeUsernameKey(it) } ?: ""
                val oldPhone = userSnap.getString("phoneNumber") ?: ""
                val oldGameUid = userSnap.getString("inGameUid") ?: ""
                val existingReferredBy = userSnap.getString("referredBy") ?: ""
                val effectiveReferredBy = existingReferredBy.ifBlank { refCodeUpper }

                val oldUsernameRef = if (oldUsernameLower.isNotBlank() && oldUsernameLower != newUsernameLower) {
                    db.collection("usernames").document(oldUsernameLower)
                } else null
                val oldUsernameSnap = oldUsernameRef?.let { tx.get(it) }

                val oldPhoneRef = if (oldPhone.isNotBlank() && oldPhone != phoneE164) {
                    db.collection("phoneClaims").document(AccountValidationUtil.sha256Hex(oldPhone))
                } else null
                val oldPhoneSnap = oldPhoneRef?.let { tx.get(it) }

                val oldGameUidRef = if (oldGameUid.isNotBlank() && AccountValidationUtil.normalizeGameUidKey(oldGameUid) != AccountValidationUtil.normalizeGameUidKey(trimmedGameUid)) {
                    db.collection("gameUidClaims").document(AccountValidationUtil.sha256Hex(AccountValidationUtil.normalizeGameUidKey(oldGameUid)))
                } else null
                val oldGameUidSnap = oldGameUidRef?.let { tx.get(it) }

                if (oldUsernameRef != null && oldUsernameSnap?.exists() == true && oldUsernameSnap.getString("userId") == uid) {
                    tx.delete(oldUsernameRef)
                }
                if (oldPhoneRef != null && oldPhoneSnap?.exists() == true && oldPhoneSnap.getString("userId") == uid) {
                    tx.delete(oldPhoneRef)
                }
                if (oldGameUidRef != null && oldGameUidSnap?.exists() == true && oldGameUidSnap.getString("userId") == uid) {
                    tx.delete(oldGameUidRef)
                }

                val now = FieldValue.serverTimestamp()
                val phoneIsVerified = user.phoneNumber == phoneE164

                if (!userSnap.exists()) {
                    tx.set(
                        userRef,
                        mapOf(
                            "userId" to uid,
                            "username" to trimmedGameName,
                            "displayName" to (user.displayName?.takeIf { it.isNotBlank() } ?: trimmedGameName).take(60),
                            "email" to normEmail,
                            "photoUrl" to (user.photoUrl?.toString() ?: ""),
                            "playerUid" to playerUid,
                            "inGameName" to trimmedGameName,
                            "inGameUid" to trimmedGameUid,
                            "phoneNumber" to phoneE164,
                            "phoneVerified" to phoneIsVerified,
                            "emailVerified" to user.isEmailVerified,
                            "acceptedTerms" to true,
                            "onboardingCompleted" to true,
                            "referralCode" to ownRefCode,
                            "referredBy" to effectiveReferredBy,
                            "language" to "EN",
                            "status" to "ACTIVE",
                            "matchesPlayed" to 0L,
                            "wins" to 0L,
                            "verifiedKills" to 0L,
                            "rankingPoints" to 0L,
                            "createdAt" to now,
                            "updatedAt" to now
                        )
                    )
                } else {
                    val updates = mutableMapOf<String, Any>(
                        "username" to trimmedGameName,
                        "displayName" to (userSnap.getString("displayName")?.takeIf { it.isNotBlank() } ?: trimmedGameName).take(60),
                        "inGameName" to trimmedGameName,
                        "inGameUid" to trimmedGameUid,
                        "phoneNumber" to phoneE164,
                        "emailVerified" to user.isEmailVerified,
                        "acceptedTerms" to true,
                        "onboardingCompleted" to true,
                        "updatedAt" to now
                    )
                    if (phoneIsVerified) {
                        updates["phoneVerified"] = true
                    }
                    if (existingReferredBy.isBlank() && effectiveReferredBy.isNotBlank()) {
                        updates["referredBy"] = effectiveReferredBy
                    }
                    tx.update(userRef, updates)
                }

                if (!newUsernameSnap.exists()) {
                    tx.set(newUsernameRef, mapOf("userId" to uid, "username" to trimmedGameName, "createdAt" to now))
                }
                if (!newPhoneSnap.exists()) {
                    tx.set(newPhoneRef, mapOf("userId" to uid, "createdAt" to now))
                }
                if (!newGameUidSnap.exists()) {
                    tx.set(newGameUidRef, mapOf("userId" to uid, "createdAt" to now))
                }
                if (emailRef != null && emailSnap?.exists() != true) {
                    tx.set(emailRef, mapOf("userId" to uid, "createdAt" to now))
                }
                if (!ownCodeSnap.exists()) {
                    tx.set(
                        ownCodeRef,
                        mapOf(
                            "userId" to uid,
                            "username" to trimmedGameName,
                            "referralCode" to ownRefCode,
                            "createdAt" to now
                        )
                    )
                }
                if (refCodeUpper.isNotBlank() && existingReferralDocSnap?.exists() != true && existingReferredBy.isBlank()) {
                    tx.set(
                        db.collection("referrals").document(uid),
                        mapOf(
                            "referrerUid" to referrerUid,
                            "referrerUsername" to referrerUsername.take(32).ifBlank { "Player" },
                            "referredUid" to uid,
                            "referredUsername" to trimmedGameName.take(32).let { if (it.length < 2) "Player" else it },
                            "referralCode" to refCodeUpper,
                            "status" to "PENDING",
                            "bonusAmount" to 0.0,
                            "transactionId" to "",
                            "createdAt" to now,
                            "updatedAt" to now
                        )
                    )
                }
            }.await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun syncEmailVerified(user: FirebaseUser): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == user.uid)
        db.collection("users").document(uid).update(
            mapOf(
                "emailVerified" to user.isEmailVerified,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/${user.uid}")
        Result.failure(e)
    }

    suspend fun markPhoneVerified(phoneE164: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Must be signed in."))
        val path = "users/$uid"
        return try {
            require(AccountValidationUtil.isValidPhoneE164(phoneE164)) { "Invalid E.164 mobile number." }
            val newPhoneHash = AccountValidationUtil.sha256Hex(phoneE164)
            db.runTransaction { tx ->
                val userRef = db.collection("users").document(uid)
                val userSnap = tx.get(userRef)
                val phoneRef = db.collection("phoneClaims").document(newPhoneHash)
                val phoneSnap = tx.get(phoneRef)
                if (phoneSnap.exists() && phoneSnap.getString("userId") != uid) {
                    throw IllegalStateException("This mobile number is already claimed by another player.")
                }
                val oldPhone = userSnap.getString("phoneNumber") ?: ""
                val oldPhoneRef = if (oldPhone.isNotBlank() && oldPhone != phoneE164) {
                    db.collection("phoneClaims").document(AccountValidationUtil.sha256Hex(oldPhone))
                } else null
                val oldPhoneSnap = oldPhoneRef?.let { tx.get(it) }

                if (oldPhoneRef != null && oldPhoneSnap?.exists() == true && oldPhoneSnap.getString("userId") == uid) {
                    tx.delete(oldPhoneRef)
                }
                val now = FieldValue.serverTimestamp()
                if (!phoneSnap.exists()) {
                    tx.set(phoneRef, mapOf("userId" to uid, "createdAt" to now))
                }
                tx.update(
                    userRef,
                    mapOf(
                        "phoneNumber" to phoneE164,
                        "phoneVerified" to true,
                        "updatedAt" to now
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun applyReferralCodeLater(referralCodeInput: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Must be signed in."))
        val path = "referrals/$uid"
        return try {
            val codeUpper = AccountValidationUtil.normalizeReferralCode(referralCodeInput)
            require(codeUpper.isNotBlank() && AccountValidationUtil.isValidReferralCodeFormat(codeUpper)) {
                "Enter a valid referral code (4–24 letters, numbers, hyphens, or underscores)."
            }
            db.runTransaction { tx ->
                val userRef = db.collection("users").document(uid)
                val userSnap = tx.get(userRef)
                val referralRef = db.collection("referrals").document(uid)
                val referralSnap = tx.get(referralRef)
                val codeSnap = tx.get(db.collection("referralCodes").document(codeUpper))

                if (referralSnap.exists() || !userSnap.getString("referredBy").isNullOrBlank()) {
                    throw IllegalStateException("A referral code has already been applied to your account.")
                }
                if (!codeSnap.exists()) {
                    throw IllegalStateException("Referral code '$codeUpper' not found.")
                }
                val referrerUid = codeSnap.getString("userId") ?: ""
                val referrerUsername = codeSnap.getString("username") ?: "Player"
                if (referrerUid.isBlank() || referrerUid == uid) {
                    throw IllegalStateException("You cannot use your own referral code.")
                }
                val myUsername = (userSnap.getString("username") ?: "Player")
                    .trim()
                    .take(32)
                    .let { if (it.length < 2) "Player" else it }
                val now = FieldValue.serverTimestamp()
                tx.set(
                    referralRef,
                    mapOf(
                        "referrerUid" to referrerUid,
                        "referrerUsername" to referrerUsername.trim().take(32).let { if (it.length < 2) "Player" else it },
                        "referredUid" to uid,
                        "referredUsername" to myUsername,
                        "referralCode" to codeUpper,
                        "status" to "PENDING",
                        "bonusAmount" to 0.0,
                        "transactionId" to "",
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                )
                tx.update(userRef, mapOf("referredBy" to codeUpper, "updatedAt" to now))
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { snap -> snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                logStreamError(error, OperationType.GET, "users/$userId", userId)
                throw error
            }

    fun observeMyReferrals(userId: String): Flow<List<ReferralRecord>> =
        db.collection("referrals")
            .whereEqualTo("referrerUid", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(ReferralRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, "referrals", userId)
                throw error
            }

    fun observeMyReferralReceived(userId: String): Flow<ReferralRecord?> =
        db.collection("referrals").document(userId)
            .snapshots()
            .map { snap -> snap.toObject(ReferralRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                logStreamError(error, OperationType.GET, "referrals/$userId", userId)
                throw error
            }

    suspend fun updateUserProfile(
        userId: String,
        currentUsername: String,
        newUsername: String,
        displayName: String,
        inGameName: String,
        inGameUid: String,
        photoUrl: String,
        language: String,
        referredBy: String
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user profile update" }
            val trimmedUser = newUsername.trim()
            AccountValidationUtil.validateUsername(trimmedUser)?.let { throw IllegalArgumentException(it) }
            val newHandleLower = AccountValidationUtil.normalizeUsernameKey(trimmedUser)
            val oldHandleLower = AccountValidationUtil.normalizeUsernameKey(currentUsername)
            val trimmedGameUid = inGameUid.trim()
            if (trimmedGameUid.isNotEmpty()) {
                AccountValidationUtil.validateGameUid(trimmedGameUid)?.let { throw IllegalArgumentException(it) }
            }
            val newGameUidHash = if (trimmedGameUid.isNotEmpty()) {
                AccountValidationUtil.sha256Hex(AccountValidationUtil.normalizeGameUidKey(trimmedGameUid))
            } else ""

            val refCodeUpper = AccountValidationUtil.normalizeReferralCode(referredBy)
            if (refCodeUpper.isNotBlank() && !AccountValidationUtil.isValidReferralCodeFormat(refCodeUpper)) {
                throw IllegalArgumentException("Referral code must be 4–24 letters, numbers, hyphens, or underscores.")
            }

            db.runTransaction { tx ->
                val userRef = db.collection("users").document(uid)
                val userSnap = tx.get(userRef)
                val existingReferredBy = userSnap.getString("referredBy") ?: ""
                val ownRefCode = userSnap.getString("referralCode") ?: ""

                val newUsernameRef = db.collection("usernames").document(newHandleLower)
                val newUsernameSnap = if (newHandleLower != oldHandleLower) tx.get(newUsernameRef) else null

                val oldUsernameRef = if (newHandleLower != oldHandleLower && oldHandleLower.isNotBlank()) {
                    db.collection("usernames").document(oldHandleLower)
                } else null
                val oldUsernameSnap = oldUsernameRef?.let { tx.get(it) }

                val newGameUidRef = if (newGameUidHash.isNotEmpty()) db.collection("gameUidClaims").document(newGameUidHash) else null
                val newGameUidSnap = newGameUidRef?.let { tx.get(it) }

                val oldGameUid = userSnap.getString("inGameUid") ?: ""
                val oldGameUidHash = if (oldGameUid.isNotBlank()) {
                    AccountValidationUtil.sha256Hex(AccountValidationUtil.normalizeGameUidKey(oldGameUid))
                } else ""
                val oldGameUidRef = if (oldGameUidHash.isNotEmpty() && newGameUidHash != oldGameUidHash) {
                    db.collection("gameUidClaims").document(oldGameUidHash)
                } else null
                val oldGameUidSnap = oldGameUidRef?.let { tx.get(it) }

                val shouldApplyRef = existingReferredBy.isBlank() && refCodeUpper.isNotBlank()
                val refCodeSnap = if (shouldApplyRef) tx.get(db.collection("referralCodes").document(refCodeUpper)) else null
                val existingRefDocSnap = if (shouldApplyRef) tx.get(db.collection("referrals").document(uid)) else null

                if (newUsernameSnap != null && newUsernameSnap.exists() && newUsernameSnap.getString("userId") != uid) {
                    throw IllegalStateException("Username '$trimmedUser' is already taken (case-insensitive).")
                }
                if (newGameUidSnap != null && newGameUidSnap.exists() && newGameUidSnap.getString("userId") != uid) {
                    throw IllegalStateException("Game UID '$trimmedGameUid' is already registered to another player.")
                }

                var validReferrerUid = ""
                var validReferrerUsername = ""
                if (shouldApplyRef && existingRefDocSnap?.exists() != true) {
                    if (refCodeUpper == ownRefCode) {
                        throw IllegalArgumentException("You cannot use your own referral code.")
                    }
                    if (refCodeSnap == null || !refCodeSnap.exists()) {
                        throw IllegalStateException("Referral code '$refCodeUpper' does not exist.")
                    }
                    validReferrerUid = refCodeSnap.getString("userId") ?: ""
                    validReferrerUsername = refCodeSnap.getString("username") ?: "Player"
                    if (validReferrerUid.isBlank() || validReferrerUid == uid) {
                        throw IllegalStateException("You cannot use your own referral code.")
                    }
                }

                val now = FieldValue.serverTimestamp()
                if (newHandleLower != oldHandleLower) {
                    if (oldUsernameRef != null && oldUsernameSnap?.exists() == true && oldUsernameSnap.getString("userId") == uid) {
                        tx.delete(oldUsernameRef)
                    }
                    if (newUsernameSnap?.exists() != true) {
                        tx.set(newUsernameRef, mapOf("userId" to uid, "username" to trimmedUser, "createdAt" to now))
                    }
                }
                if (newGameUidHash != oldGameUidHash) {
                    if (oldGameUidRef != null && oldGameUidSnap?.exists() == true && oldGameUidSnap.getString("userId") == uid) {
                        tx.delete(oldGameUidRef)
                    }
                    if (newGameUidRef != null && newGameUidSnap?.exists() != true) {
                        tx.set(newGameUidRef, mapOf("userId" to uid, "createdAt" to now))
                    }
                }

                val updates = mutableMapOf<String, Any>(
                    "username" to trimmedUser,
                    "displayName" to displayName.trim().ifBlank { trimmedUser }.take(60),
                    "inGameName" to inGameName.trim().take(40),
                    "inGameUid" to trimmedGameUid.take(40),
                    "photoUrl" to photoUrl.take(200000),
                    "language" to language.take(16),
                    "updatedAt" to now
                )
                if (validReferrerUid.isNotBlank()) {
                    updates["referredBy"] = refCodeUpper.take(24)
                    tx.set(
                        db.collection("referrals").document(uid),
                        mapOf(
                            "referrerUid" to validReferrerUid,
                            "referrerUsername" to validReferrerUsername.take(32),
                            "referredUid" to uid,
                            "referredUsername" to trimmedUser.take(32),
                            "referralCode" to refCodeUpper.take(24),
                            "status" to "PENDING",
                            "bonusAmount" to 0.0,
                            "transactionId" to "",
                            "createdAt" to now,
                            "updatedAt" to now
                        )
                    )
                }
                tx.update(userRef, updates)
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeWallet(userId: String): Flow<Wallet> =
        db.collection("wallets").document(userId)
            .snapshots()
            .map { snap ->
                val raw = snap.toObject(Wallet::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: Wallet(id = userId, userId = userId)
                if (!snap.contains("bonusBalance") && raw.earningsBalance > 0.0) {
                    raw.copy(bonusBalance = raw.earningsBalance)
                } else {
                    raw
                }
            }
            .catch { error ->
                logStreamError(error, OperationType.GET, "wallets/$userId", userId)
                throw error
            }

    fun observeWalletConfig(userId: String): Flow<WalletConfig> =
        db.collection("appConfig").document("wallet")
            .snapshots()
            .map { snap ->
                snap.toObject(WalletConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: WalletConfig(id = "wallet")
            }
            .catch { error ->
                logStreamError(error, OperationType.GET, "appConfig/wallet", userId)
                throw error
            }

    fun observePaymentOrders(userId: String): Flow<List<PaymentOrder>> =
        db.collection("payments")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(PaymentOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, "payments", userId)
                throw error
            }

    fun observeTransactions(userId: String): Flow<List<WalletTransaction>> =
        db.collection("transactions")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(WalletTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, "transactions", userId)
                throw error
            }

    fun observePayoutRequests(userId: String): Flow<List<PayoutRequest>> =
        db.collection("payoutRequests")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(PayoutRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, "payoutRequests", userId)
                throw error
            }

    suspend fun submitDepositRequest(
        coinAmount: Double,
        referenceId: String
    ): Result<Unit> {
        val docRef = db.collection("payments").document()
        return try {
            val uid = requireUserId()
            require(coinAmount in 1.0..100000.0) { "Enter a valid amount between 1 and 100,000 Coins." }
            val cleanRef = referenceId.trim().take(80)
            require(cleanRef.length >= 4) { "Enter a valid UTR / Transaction Reference ID (min 4 characters)." }
            val userSnap = db.collection("users").document(uid).get().await()
            val status = userSnap.getString("status") ?: "ACTIVE"
            if (status != "ACTIVE") {
                throw IllegalStateException("Your account is $status and cannot submit deposit requests.")
            }
            val now = FieldValue.serverTimestamp()
            val payload = mapOf(
                "userId" to uid,
                "packageName" to "Custom ${coinAmount.toInt()} Coins (₹${coinAmount.toInt()})",
                "coinAmount" to coinAmount,
                "rupeeAmount" to coinAmount,
                "gateway" to "UPI_QR",
                "referenceId" to cleanRef,
                "reviewedBy" to "",
                "status" to "PENDING",
                "createdAt" to now,
                "updatedAt" to now
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun submitWithdrawalRequest(
        coinAmount: Double,
        destinationType: String,
        destinationDetails: String
    ): Result<Unit> {
        val docRef = db.collection("payoutRequests").document()
        return try {
            val uid = requireUserId()
            require(coinAmount >= 50.0) { "Minimum withdrawal is 50 Coins (₹50)." }
            val cleanDetails = destinationDetails.trim().take(120)
            require(cleanDetails.length >= 3) { "Enter your valid UPI ID or payout account details." }
            val userSnap = db.collection("users").document(uid).get().await()
            val status = userSnap.getString("status") ?: "ACTIVE"
            if (status != "ACTIVE") {
                throw IllegalStateException("Your account is $status and cannot withdraw coins.")
            }
            val walletSnap = db.collection("wallets").document(uid).get().await()
            val winningBal = walletSnap.getDouble("winningBalance") ?: 0.0
            if (winningBal < coinAmount) {
                throw IllegalStateException(
                    "Only Winning Prize balance can be redeemed. Available Winning: ${String.format(Locale.US, "%.2f", winningBal)} Coins."
                )
            }
            val now = FieldValue.serverTimestamp()
            val payload = mapOf(
                "userId" to uid,
                "amount" to coinAmount,
                "destinationType" to destinationType.trim().take(40).ifBlank { "UPI" },
                "destinationDetails" to cleanDetails,
                "status" to "PENDING",
                "reviewedBy" to "",
                "createdAt" to now,
                "updatedAt" to now
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeNotifications(userId: String): Flow<List<NotificationItem>> =
        db.collection("notifications")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(NotificationItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, "notifications", userId)
                throw error
            }

    suspend fun markNotificationRead(notifId: String): Result<Unit> =
        try {
            requireUserId()
            db.collection("notifications").document(notifId).update("read", true).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "notifications/$notifId")
            Result.failure(e)
        }

    fun observeSupportTickets(userId: String): Flow<List<SupportTicket>> =
        db.collection("supportTickets")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, "supportTickets", userId)
                throw error
            }

    suspend fun createSupportTicket(
        username: String,
        category: String,
        subject: String,
        description: String,
        matchId: String
    ): Result<Unit> {
        val docRef = db.collection("supportTickets").document()
        return try {
            val uid = requireUserId()
            require(subject.trim().length in 3..100) { "Subject must be 3-100 characters" }
            require(description.trim().length in 5..1000) { "Description must be 5-1000 characters" }
            val now = FieldValue.serverTimestamp()
            val payload = mapOf(
                "userId" to uid,
                "username" to username.take(32).ifBlank { "Player" },
                "category" to category,
                "subject" to subject.trim(),
                "description" to description.trim(),
                "matchId" to matchId.trim().take(64),
                "status" to "OPEN",
                "adminReply" to "",
                "createdAt" to now,
                "updatedAt" to now
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeDisputes(userId: String): Flow<List<Dispute>> =
        db.collection("disputes")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(Dispute::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, "disputes", userId)
                throw error
            }

    suspend fun createDispute(tournamentId: String, matchId: String, reason: String): Result<Unit> {
        val docRef = db.collection("disputes").document()
        return try {
            val uid = requireUserId()
            require(reason.trim().length in 5..1000) { "Reason must be 5-1000 characters" }
            val now = FieldValue.serverTimestamp()
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "tournamentId" to tournamentId,
                    "matchId" to matchId,
                    "reason" to reason.trim(),
                    "status" to "OPEN",
                    "resolutionNotes" to "",
                    "createdAt" to now,
                    "updatedAt" to now
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeTeams(userId: String): Flow<List<TeamRoster>> =
        db.collection("teams")
            .whereEqualTo("ownerId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(TeamRoster::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, "teams", userId)
                throw error
            }

    suspend fun createTeam(teamName: String, gameMode: String, membersSummary: String): Result<Unit> {
        val docRef = db.collection("teams").document()
        return try {
            val uid = requireUserId()
            require(teamName.trim().length in 2..40) { "Team name must be 2-40 characters" }
            val now = FieldValue.serverTimestamp()
            docRef.set(
                mapOf(
                    "ownerId" to uid,
                    "teamName" to teamName.trim(),
                    "gameMode" to gameMode,
                    "membersSummary" to membersSummary.trim().take(200),
                    "createdAt" to now,
                    "updatedAt" to now
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    private fun logStreamError(error: Throwable, op: OperationType, path: String, uid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val ex = firestoreError ?: (error as? Exception)
        if (ex != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(ex, op, path)
        }
    }
}
