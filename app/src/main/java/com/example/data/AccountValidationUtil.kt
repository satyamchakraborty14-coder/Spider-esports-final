package com.example.data

import java.security.MessageDigest
import java.util.Locale

object AccountValidationUtil {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val USERNAME_REGEX = Regex("^[A-Za-z0-9_]{3,20}$")
    private val GAME_UID_REGEX = Regex("^[A-Za-z0-9_\\-]{5,20}$")
    private val E164_REGEX = Regex("^\\+[1-9]\\d{7,14}$")
    private val REFERRAL_REGEX = Regex("^[A-Z0-9_\\-]{4,24}$")

    fun normalizeEmail(raw: String): String = raw.trim().lowercase(Locale.US)

    fun isValidEmail(raw: String): Boolean {
        val norm = normalizeEmail(raw)
        return norm.length <= 160 && EMAIL_REGEX.matches(norm)
    }

    fun normalizeUsernameKey(raw: String): String = raw.trim().lowercase(Locale.US)

    fun validateUsername(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.length < 3 || trimmed.length > 20) {
            return "Game Name (Username) must be 3–20 characters."
        }
        if (!USERNAME_REGEX.matches(trimmed)) {
            return "Game Name may only contain letters, numbers, and underscores."
        }
        return null
    }

    fun normalizeGameUidKey(raw: String): String = raw.trim().lowercase(Locale.US)

    fun validateGameUid(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.length < 5 || trimmed.length > 20) {
            return "Game UID must be 5–20 characters."
        }
        if (!GAME_UID_REGEX.matches(trimmed)) {
            return "Game UID may only contain letters, numbers, hyphens, or underscores."
        }
        return null
    }

    fun normalizePhoneE164(countryCodeInput: String, rawPhone: String): String {
        val cleaned = rawPhone.trim().replace(Regex("[\\s\\-().]"), "")
        if (cleaned.isEmpty()) return ""
        if (cleaned.startsWith("+")) {
            val digits = cleaned.drop(1).filter { it.isDigit() }
            return if (digits.isNotEmpty()) "+$digits" else ""
        }
        val ccDigits = countryCodeInput.filter { it.isDigit() }.ifBlank { "91" }
        val localDigits = cleaned.filter { it.isDigit() }.trimStart('0')
        return if (localDigits.isNotEmpty()) "+$ccDigits$localDigits" else ""
    }

    fun isValidPhoneE164(phoneE164: String): Boolean = E164_REGEX.matches(phoneE164)

    fun normalizeReferralCode(raw: String): String = raw.trim().uppercase(Locale.US)

    fun isValidReferralCodeFormat(raw: String): Boolean {
        val norm = normalizeReferralCode(raw)
        return norm.isEmpty() || REFERRAL_REGEX.matches(norm)
    }

    fun validatePassword(password: String): String? {
        if (password.length < 8) return "Password must be at least 8 characters."
        if (!password.any { it.isLetter() } || !password.any { it.isDigit() }) {
            return "Password must include at least 1 letter and 1 number."
        }
        return null
    }

    fun sha256Hex(normalizedInput: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(normalizedInput.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun isPersonalWhatsAppNumberUrl(raw: String): Boolean {
        val trimmed = raw.trim().lowercase(Locale.US)
        if (trimmed.isEmpty()) return false
        return trimmed.contains("wa.me/") ||
            trimmed.contains("api.whatsapp.com/send") ||
            trimmed.startsWith("+") ||
            trimmed.all { it.isDigit() || it == ' ' || it == '-' }
    }

    fun sanitizeWhatsAppChannelUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.contains("0000000000")) return ""
        if (isPersonalWhatsAppNumberUrl(trimmed)) return ""
        return if (trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("http://", ignoreCase = true)
        ) {
            trimmed
        } else {
            ""
        }
    }
}
