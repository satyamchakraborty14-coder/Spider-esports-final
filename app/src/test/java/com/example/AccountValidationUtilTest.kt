package com.example

import com.example.data.AccountValidationUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountValidationUtilTest {

    @Test
    fun normalizeEmail_trimsAndLowercases() {
        assertEquals("spider.user@example.com", AccountValidationUtil.normalizeEmail("  Spider.User@Example.COM  "))
    }

    @Test
    fun isValidEmail_acceptsStandardAndRejectsInvalid() {
        assertTrue(AccountValidationUtil.isValidEmail("player_1@spider.gg"))
        assertFalse(AccountValidationUtil.isValidEmail("not-an-email"))
        assertFalse(AccountValidationUtil.isValidEmail("missing@domain"))
    }

    @Test
    fun normalizeUsernameKey_isCaseInsensitive() {
        assertEquals(
            AccountValidationUtil.normalizeUsernameKey("SpiderX"),
            AccountValidationUtil.normalizeUsernameKey("  spiderx ")
        )
    }

    @Test
    fun validateUsername_enforcesLengthAndCharacters() {
        assertNull(AccountValidationUtil.validateUsername("Spider_X99"))
        assertNotNull(AccountValidationUtil.validateUsername("ab"))
        assertNotNull(AccountValidationUtil.validateUsername("bad name!"))
    }

    @Test
    fun validateGameUid_enforcesFormat() {
        assertNull(AccountValidationUtil.validateGameUid("5123456789"))
        assertNotNull(AccountValidationUtil.validateGameUid("123"))
        assertNotNull(AccountValidationUtil.validateGameUid("bad uid@"))
    }

    @Test
    fun normalizePhoneE164_formatsConsistentInternationalNumber() {
        assertEquals("+919876543210", AccountValidationUtil.normalizePhoneE164("+91", "98765-43210"))
        assertEquals("+14155552671", AccountValidationUtil.normalizePhoneE164("+91", "+1 (415) 555-2671"))
        assertTrue(AccountValidationUtil.isValidPhoneE164("+919876543210"))
        assertFalse(AccountValidationUtil.isValidPhoneE164("12345"))
    }

    @Test
    fun validatePassword_requiresLengthLettersAndDigits() {
        assertNull(AccountValidationUtil.validatePassword("Spider2026"))
        assertNotNull(AccountValidationUtil.validatePassword("short1"))
        assertNotNull(AccountValidationUtil.validatePassword("onlyletters"))
        assertNotNull(AccountValidationUtil.validatePassword("12345678"))
    }

    @Test
    fun sha256Hex_producesDeterministic64CharDigest() {
        val hash1 = AccountValidationUtil.sha256Hex("+919876543210")
        val hash2 = AccountValidationUtil.sha256Hex("+919876543210")
        assertEquals(64, hash1.length)
        assertEquals(hash1, hash2)
    }
}
