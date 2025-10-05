package com.wifiguard.core.security

import android.util.Base64
import com.wifiguard.core.common.Constants
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Класс для шифрования и дешифрования данных с использованием AES-256-GCM.
 * Обеспечивает высокий уровень безопасности для хранимых данных.
 * 
 * Особенности:
 * - Использует AES-256 в режиме GCM для максимальной безопасности
 * - Обеспечивает аутентичность и целостность данных
 * - Использует криптографически стойкие генераторы случайных чисел
 */
class AesEncryption {
    
    companion object {
        private const val ALGORITHM = "AES"
        private val secureRandom = SecureRandom()
        
        /**
         * Генерирует новый 256-битный AES ключ.
         * 
         * @return Новый секретный ключ в виде Base64 строки
         * @throws SecurityException Если не удалось сгенерировать ключ
         */
        fun generateKey(): String {
            return try {
                val keyGen = KeyGenerator.getInstance(ALGORITHM)
                keyGen.init(Constants.AES_KEY_SIZE, secureRandom)
                val secretKey = keyGen.generateKey()
                Base64.encodeToString(secretKey.encoded, Base64.NO_WRAP)
            } catch (e: Exception) {
                throw SecurityException("Не удалось сгенерировать ключ шифрования", e)
            }
        }
        
        /**
         * Генерирует криптографически стойкий вектор инициализации.
         * 
         * @return Массив байт с IV для GCM режима
         */
        private fun generateIv(): ByteArray {
            val iv = ByteArray(Constants.AES_GCM_IV_LENGTH)
            secureRandom.nextBytes(iv)
            return iv
        }
    }
    
    /**
     * Шифрует данные с использованием указанного ключа.
     * 
     * @param data Данные для шифрования
     * @param keyBase64 Ключ шифрования в формате Base64
     * @return Шифрованные данные с IV в формате Base64
     * @throws SecurityException При ошибках шифрования
     */
    @Throws(SecurityException::class)
    fun encrypt(data: String, keyBase64: String): String {
        if (data.isEmpty() || keyBase64.isEmpty()) {
            throw SecurityException("Данные или ключ не могут быть пустыми")
        }
        
        return try {
            // Получаем ключ из Base64
            val keyBytes = Base64.decode(keyBase64, Base64.NO_WRAP)
            val secretKey: SecretKey = SecretKeySpec(keyBytes, ALGORITHM)
            
            // Генерируем IV
            val iv = generateIv()
            
            // Настраиваем шифр
            val cipher = Cipher.getInstance(Constants.AES_TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(Constants.AES_GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
            
            // Шифруем
            val encryptedData = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            
            // Объединяем IV и зашифрованные данные
            val encryptedWithIv = ByteArray(iv.size + encryptedData.size)
            System.arraycopy(iv, 0, encryptedWithIv, 0, iv.size)
            System.arraycopy(encryptedData, 0, encryptedWithIv, iv.size, encryptedData.size)
            
            Base64.encodeToString(encryptedWithIv, Base64.NO_WRAP)
            
        } catch (e: Exception) {
            throw SecurityException("Ошибка при шифровании данных", e)
        }
    }
    
    /**
     * Дешифрует данные с использованием указанного ключа.
     * 
     * @param encryptedDataBase64 Зашифрованные данные с IV в формате Base64
     * @param keyBase64 Ключ шифрования в формате Base64
     * @return Расшифрованные данные
     * @throws SecurityException При ошибках дешифрования
     */
    @Throws(SecurityException::class)
    fun decrypt(encryptedDataBase64: String, keyBase64: String): String {
        if (encryptedDataBase64.isEmpty() || keyBase64.isEmpty()) {
            throw SecurityException("Данные или ключ не могут быть пустыми")
        }
        
        return try {
            // Получаем ключ из Base64
            val keyBytes = Base64.decode(keyBase64, Base64.NO_WRAP)
            val secretKey: SecretKey = SecretKeySpec(keyBytes, ALGORITHM)
            
            // Получаем зашифрованные данные
            val encryptedWithIv = Base64.decode(encryptedDataBase64, Base64.NO_WRAP)
            
            if (encryptedWithIv.size < Constants.AES_GCM_IV_LENGTH) {
                throw SecurityException("Некорректные зашифрованные данные")
            }
            
            // Извлекаем IV и данные
            val iv = ByteArray(Constants.AES_GCM_IV_LENGTH)
            val encryptedData = ByteArray(encryptedWithIv.size - Constants.AES_GCM_IV_LENGTH)
            System.arraycopy(encryptedWithIv, 0, iv, 0, Constants.AES_GCM_IV_LENGTH)
            System.arraycopy(encryptedWithIv, Constants.AES_GCM_IV_LENGTH, encryptedData, 0, encryptedData.size)
            
            // Настраиваем шифр
            val cipher = Cipher.getInstance(Constants.AES_TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(Constants.AES_GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            
            // Дешифруем
            val decryptedData = cipher.doFinal(encryptedData)
            String(decryptedData, Charsets.UTF_8)
            
        } catch (e: Exception) {
            throw SecurityException("Ошибка при дешифровании данных", e)
        }
    }
    
    /**
     * Проверяет валидность ключа шифрования.
     * 
     * @param keyBase64 Ключ в формате Base64
     * @return true, если ключ валиден
     */
    fun isValidKey(keyBase64: String?): Boolean {
        if (keyBase64.isNullOrEmpty()) return false
        
        return try {
            val keyBytes = Base64.decode(keyBase64, Base64.NO_WRAP)
            keyBytes.size == (Constants.AES_KEY_SIZE / 8) // 256 бит = 32 байта
        } catch (e: Exception) {
            false
        }
    }
}