package com.wifiguard.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.wifiguard.core.common.Constants
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AesEncryption @Inject constructor() {
    
    private val keyStore: KeyStore = KeyStore.getInstance(Constants.KEYSTORE_PROVIDER)
    
    init {
        keyStore.load(null)
        generateKeyIfNotExists()
    }
    
    private fun generateKeyIfNotExists() {
        if (!keyStore.containsAlias(Constants.AES_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, Constants.KEYSTORE_PROVIDER)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                Constants.AES_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
            
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }
    
    private fun getSecretKey(): SecretKey {
        return keyStore.getKey(Constants.AES_KEY_ALIAS, null) as SecretKey
    }
    
    fun encrypt(plainText: String): EncryptedData {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        
        return EncryptedData(
            encryptedData = encryptedBytes,
            iv = iv
        )
    }
    
    fun decrypt(encryptedData: EncryptedData): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmParameterSpec = GCMParameterSpec(128, encryptedData.iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), gcmParameterSpec)
        
        val decryptedBytes = cipher.doFinal(encryptedData.encryptedData)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}

data class EncryptedData(
    val encryptedData: ByteArray,
    val iv: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as EncryptedData
        
        if (!encryptedData.contentEquals(other.encryptedData)) return false
        if (!iv.contentEquals(other.iv)) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = encryptedData.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        return result
    }
}