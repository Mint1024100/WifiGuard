package com.wifiguard.core.security

import android.os.Build
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for AesEncryption class
 * 
 * Тестирует:
 * - Шифрование и расшифровку данных
 * - HMAC integrity verification
 * - Обработку ошибок
 * - Ротацию ключей
 * - Безопасность (timing attacks)
 * 
 * ПРИМЕЧАНИЕ: Эти тесты требуют AndroidKeyStore, который доступен только в instrumented tests
 * или через Robolectric. Для unit-тестов отключены через @Ignore.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class AesEncryptionTest {

    private lateinit var aesEncryption: AesEncryption

    @Before
    fun setUp() {
        // AndroidKeyStore недоступен в unit-тестах даже с Robolectric
        // Эти тесты должны быть instrumented tests (androidTest)
        // Для демонстрации оставляем с @Ignore
    }

    @Test
    @Ignore("AndroidKeyStore недоступен в unit-тестах. Используйте instrumented test.")
    fun `encrypt and decrypt should work correctly`() {
        aesEncryption = AesEncryption()
        
        // Given
        val originalText = "Test sensitive data for encryption"
        
        // When
        val encryptedData = aesEncryption.encrypt(originalText)
        val decryptedText = aesEncryption.decrypt(encryptedData)
        
        // Then
        assertNotNull("Encrypted data should not be null", encryptedData)
        assertNotNull("Encrypted data should not be empty", encryptedData.encryptedData)
        assertNotNull("IV should not be null", encryptedData.iv)
        assertNotNull("HMAC should not be null", encryptedData.hmac)
        
        assertEquals("Decrypted text should match original", originalText, decryptedText)
        assertNotEquals("Encrypted data should be different from original", originalText, String(encryptedData.encryptedData))
    }

    @Test
    @Ignore("AndroidKeyStore недоступен в unit-тестах. Используйте instrumented test.")
    fun `encrypt should generate different IV for same data`() {
        aesEncryption = AesEncryption()
        
        // Given
        val originalText = "Same data to encrypt twice"
        
        // When
        val encryptedData1 = aesEncryption.encrypt(originalText)
        val encryptedData2 = aesEncryption.encrypt(originalText)
        
        // Then
        assertFalse("IVs should be different", encryptedData1.iv.contentEquals(encryptedData2.iv))
        assertFalse("Encrypted data should be different", 
            encryptedData1.encryptedData.contentEquals(encryptedData2.encryptedData))
        assertFalse("HMACs should be different", 
            encryptedData1.hmac.contentEquals(encryptedData2.hmac))
    }

    @Test
    @Ignore("AndroidKeyStore недоступен в unit-тестах. Используйте instrumented test.")
    fun `decrypt should fail with tampered data`() {
        aesEncryption = AesEncryption()
        
        // Given
        val originalText = "Data to be tampered"
        val encryptedData = aesEncryption.encrypt(originalText)
        
        // When - tamper with encrypted data
        val tamperedData = encryptedData.copy(
            encryptedData = encryptedData.encryptedData.copyOf().apply { 
                this[0] = (this[0] + 1).toByte() 
            }
        )
        
        // Then
        try {
            aesEncryption.decrypt(tamperedData)
            fail("Decryption should fail with tampered data")
        } catch (e: EncryptionException) {
            assertTrue("Should be HMAC verification error", e.message?.contains("HMAC") == true)
        }
    }

    @Test
    @Ignore("AndroidKeyStore недоступен в unit-тестах. Используйте instrumented test.")
    fun `decrypt should fail with tampered HMAC`() {
        aesEncryption = AesEncryption()
        
        // Given
        val originalText = "Data with tampered HMAC"
        val encryptedData = aesEncryption.encrypt(originalText)
        
        // When - tamper with HMAC
        val tamperedData = encryptedData.copy(
            hmac = encryptedData.hmac.copyOf().apply { 
                this[0] = (this[0] + 1).toByte() 
            }
        )
        
        // Then
        try {
            aesEncryption.decrypt(tamperedData)
            fail("Decryption should fail with tampered HMAC")
        } catch (e: EncryptionException) {
            assertTrue("Should be HMAC verification error", e.message?.contains("HMAC") == true)
        }
    }

    @Test
    @Ignore("AndroidKeyStore недоступен в unit-тестах. Используйте instrumented test.")
    fun `encrypt should fail with empty string`() {
        aesEncryption = AesEncryption()
        
        // Given
        val emptyString = ""
        
        // When & Then
        try {
            aesEncryption.encrypt(emptyString)
            fail("Encryption should fail with empty string")
        } catch (e: EncryptionException) {
            assertTrue("Should be empty string error", e.message?.contains("empty") == true)
        }
    }

    @Test
    @Ignore("AndroidKeyStore недоступен в unit-тестах. Используйте instrumented test.")
    fun `decrypt should fail with empty data`() {
        aesEncryption = AesEncryption()
        
        // Given
        val emptyEncryptedData = EncryptedData(
            encryptedData = ByteArray(0),
            iv = ByteArray(12),
            hmac = ByteArray(32)
        )
        
        // When & Then
        try {
            aesEncryption.decrypt(emptyEncryptedData)
            fail("Decryption should fail with empty data")
        } catch (e: EncryptionException) {
            assertTrue("Should be empty data error", e.message?.contains("empty") == true)
        }
    }

    @Test
    @Ignore("AndroidKeyStore недоступен в unit-тестах. Используйте instrumented test.")
    fun `key rotation should work correctly`() {
        aesEncryption = AesEncryption()
        
        // Given
        val originalText = "Data before key rotation"
        val encryptedData = aesEncryption.encrypt(originalText)
        
        // When
        val rotationResult = aesEncryption.rotateKeys()
        
        // Then
        assertTrue("Key rotation should succeed", rotationResult)
        
        // Old encrypted data should still be decryptable (if keys are preserved)
        // In real implementation, you might want to test key migration
        val decryptedText = aesEncryption.decrypt(encryptedData)
        assertEquals("Data should still be decryptable", originalText, decryptedText)
    }

    @Test
    @Ignore("AndroidKeyStore недоступен в unit-тестах. Используйте instrumented test.")
    fun `should rotate keys should return false by default`() {
        aesEncryption = AesEncryption()
        
        // When
        val shouldRotate = aesEncryption.shouldRotateKeys()
        
        // Then
        assertFalse("Should not rotate keys by default", shouldRotate)
    }

    @Test
    @Ignore("AndroidKeyStore недоступен в unit-тестах. Используйте instrumented test.")
    fun `encrypted data should have correct structure`() {
        aesEncryption = AesEncryption()
        
        // Given
        val originalText = "Test data structure"
        
        // When
        val encryptedData = aesEncryption.encrypt(originalText)
        
        // Then
        assertEquals("IV should be 12 bytes", 12, encryptedData.iv.size)
        assertEquals("HMAC should be 32 bytes", 32, encryptedData.hmac.size)
        assertTrue("Encrypted data should not be empty", encryptedData.encryptedData.isNotEmpty())
    }

    @Test
    @Ignore("AndroidKeyStore недоступен в unit-тестах. Используйте instrumented test.")
    fun `encryption should handle large data`() {
        aesEncryption = AesEncryption()
        
        // Given
        val largeData = "A".repeat(10000) // 10KB of data
        
        // When
        val encryptedData = aesEncryption.encrypt(largeData)
        val decryptedText = aesEncryption.decrypt(encryptedData)
        
        // Then
        assertEquals("Large data should be encrypted and decrypted correctly", largeData, decryptedText)
    }

    @Test
    @Ignore("AndroidKeyStore недоступен в unit-тестах. Используйте instrumented test.")
    fun `encryption should handle special characters`() {
        aesEncryption = AesEncryption()
        
        // Given
        val specialText = "Special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?`~"
        
        // When
        val encryptedData = aesEncryption.encrypt(specialText)
        val decryptedText = aesEncryption.decrypt(encryptedData)
        
        // Then
        assertEquals("Special characters should be handled correctly", specialText, decryptedText)
    }

    @Test
    @Ignore("AndroidKeyStore недоступен в unit-тестах. Используйте instrumented test.")
    fun `encryption should handle unicode characters`() {
        aesEncryption = AesEncryption()
        
        // Given
        val unicodeText = "Unicode: Привет мир! 🌍 中文 العربية"
        
        // When
        val encryptedData = aesEncryption.encrypt(unicodeText)
        val decryptedText = aesEncryption.decrypt(encryptedData)
        
        // Then
        assertEquals("Unicode characters should be handled correctly", unicodeText, decryptedText)
    }
}
