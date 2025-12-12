package com.wifiguard.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import com.wifiguard.core.common.Constants
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Улучшенный класс для AES шифрования с HMAC-SHA256 integrity verification
 * и дополнительными мерами безопасности
 */
@Singleton
class AesEncryption @Inject constructor() {
    
    companion object {
        private const val TAG = "AesEncryption"
        private const val HMAC_ALGORITHM = "HmacSHA256"
        private const val HMAC_KEY_LENGTH = 32 // 256 bits
        private const val GCM_TAG_LENGTH = 128
        private const val IV_LENGTH = 12 // 96 bits for GCM
        private const val MAX_KEY_AGE_DAYS = 90L
    }
    
    private val keyStore: KeyStore = KeyStore.getInstance(Constants.KEYSTORE_PROVIDER)
    private val secureRandom = SecureRandom()
    
    init {
        keyStore.load(null)
        generateKeysIfNotExists()
    }
    
    /**
     * Генерирует ключи шифрования и HMAC если они не существуют
     */
    private fun generateKeysIfNotExists() {
        try {
            if (!keyStore.containsAlias(Constants.AES_KEY_ALIAS)) {
                generateAESKey()
            }
            if (!keyStore.containsAlias(Constants.HMAC_KEY_ALIAS)) {
                generateHMACKey()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating keys: ${e.message}", e)
            throw EncryptionException("Failed to generate encryption keys", e)
        }
    }
    
    /**
     * Генерирует AES ключ с улучшенными параметрами безопасности
     */
    private fun generateAESKey() {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, Constants.KEYSTORE_PROVIDER)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            Constants.AES_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(256)
        .setUserAuthenticationRequired(false) // Для фоновых операций
        .setRandomizedEncryptionRequired(true)
        // setUserAuthenticationValidityDurationSeconds устарел в новых версиях Android
        // При setUserAuthenticationRequired(false) этот параметр не требуется
        .build()
        
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
        Log.d(TAG, "AES key generated successfully")
    }
    
    /**
     * Генерирует HMAC ключ для integrity verification
     */
    private fun generateHMACKey() {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, Constants.KEYSTORE_PROVIDER)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            Constants.HMAC_KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
        .setKeySize(HMAC_KEY_LENGTH * 8) // 256 bits
        .setUserAuthenticationRequired(false)
        .build()
        
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
        Log.d(TAG, "HMAC key generated successfully")
    }
    
    /**
     * Получает AES ключ из KeyStore
     */
    private fun getAESKey(): SecretKey {
        return try {
            keyStore.getKey(Constants.AES_KEY_ALIAS, null) as SecretKey
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving AES key: ${e.message}", e)
            throw EncryptionException("Failed to retrieve AES key", e)
        }
    }
    
    /**
     * Получает HMAC ключ из KeyStore
     */
    private fun getHMACKey(): SecretKey {
        return try {
            keyStore.getKey(Constants.HMAC_KEY_ALIAS, null) as SecretKey
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving HMAC key: ${e.message}", e)
            throw EncryptionException("Failed to retrieve HMAC key", e)
        }
    }
    
    /**
     * Шифрует данные с HMAC integrity verification
     */
    fun encrypt(plainText: String): EncryptedData {
        return try {
            if (plainText.isEmpty()) {
                throw EncryptionException("Cannot encrypt empty string")
            }
            
            // Генерируем криптографически стойкий IV
            val iv = ByteArray(IV_LENGTH)
            secureRandom.nextBytes(iv)
            
            // Шифруем данные
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, getAESKey(), gcmParameterSpec)
            
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            
            // Создаем HMAC для integrity verification
            val hmac = createHMAC(encryptedBytes + iv)
            
            Log.d(TAG, "Data encrypted successfully, size: ${encryptedBytes.size} bytes")
            
            EncryptedData(
                encryptedData = encryptedBytes,
                iv = iv,
                hmac = hmac
            )
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed: ${e.message}", e)
            throw EncryptionException("Encryption failed", e)
        }
    }
    
    /**
     * Расшифровывает данные с проверкой HMAC integrity
     */
    fun decrypt(encryptedData: EncryptedData): String {
        return try {
            if (encryptedData.encryptedData.isEmpty()) {
                throw EncryptionException("Cannot decrypt empty data")
            }
            
            // Проверяем HMAC integrity
            val expectedHmac = createHMAC(encryptedData.encryptedData + encryptedData.iv)
            if (!secureEquals(encryptedData.hmac, expectedHmac)) {
                throw EncryptionException("HMAC verification failed - data may be tampered")
            }
            
            // Расшифровываем данные
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, encryptedData.iv)
            cipher.init(Cipher.DECRYPT_MODE, getAESKey(), gcmParameterSpec)
            
            val decryptedBytes = cipher.doFinal(encryptedData.encryptedData)
            val result = String(decryptedBytes, Charsets.UTF_8)
            
            Log.d(TAG, "Data decrypted successfully, size: ${decryptedBytes.size} bytes")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed: ${e.message}", e)
            throw EncryptionException("Decryption failed", e)
        }
    }
    
    /**
     * Создает HMAC для данных
     */
    private fun createHMAC(data: ByteArray): ByteArray {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(getHMACKey())
        return mac.doFinal(data)
    }
    
    /**
     * Безопасное сравнение массивов байтов (защита от timing attacks)
     */
    private fun secureEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }
    
    /**
     * Ротация ключей (вызывается периодически)
     */
    fun rotateKeys(): Boolean {
        return try {
            Log.i(TAG, "Starting key rotation")
            
            // Удаляем старые ключи
            if (keyStore.containsAlias(Constants.AES_KEY_ALIAS)) {
                keyStore.deleteEntry(Constants.AES_KEY_ALIAS)
            }
            if (keyStore.containsAlias(Constants.HMAC_KEY_ALIAS)) {
                keyStore.deleteEntry(Constants.HMAC_KEY_ALIAS)
            }
            
            // Генерируем новые ключи
            generateKeysIfNotExists()
            
            Log.i(TAG, "Key rotation completed successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Key rotation failed: ${e.message}", e)
            false
        }
    }
    
    /**
     * Проверяет, нужно ли выполнить ротацию ключей
     */
    fun shouldRotateKeys(): Boolean {
        // В реальном приложении здесь должна быть проверка времени создания ключей
        // Для демонстрации возвращаем false
        return false
    }
}

/**
 * Класс для хранения зашифрованных данных с HMAC
 */
data class EncryptedData(
    val encryptedData: ByteArray,
    val iv: ByteArray,
    val hmac: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as EncryptedData
        
        if (!encryptedData.contentEquals(other.encryptedData)) return false
        if (!iv.contentEquals(other.iv)) return false
        if (!hmac.contentEquals(other.hmac)) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = encryptedData.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + hmac.contentHashCode()
        return result
    }
}

/**
 * Исключение для ошибок шифрования
 */
class EncryptionException(message: String, cause: Throwable? = null) : Exception(message, cause)