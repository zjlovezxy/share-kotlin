package io.github.zjlovezxy.sharekt.core.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import io.github.zjlovezxy.sharekt.core.common.AppLog
import io.github.zjlovezxy.sharekt.core.network.Session
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.serialization.json.Json

/**
 * 登录态落盘。用 AndroidKeyStore 的 AES-GCM 加密，
 * 明文 SUB 绝不进 SharedPreferences（备份/root 都能直接读）。
 */
class AuthStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun save(session: Session) {
        runCatching {
            val raw = json.encodeToString(Session.serializer(), session)
            prefs.edit().putString(KEY_SESSION, encrypt(raw)).apply()
        }.onFailure { AppLog.e(TAG, "保存登录态失败", it) }
    }

    fun load(): Session {
        val stored = prefs.getString(KEY_SESSION, null) ?: return Session()
        val raw = runCatching { decrypt(stored) }.getOrElse {
            AppLog.w(TAG, "登录态解密失败，按未登录处理", it)
            return Session()
        }
        return runCatching { json.decodeFromString(Session.serializer(), raw) }
            .getOrDefault(Session())
    }

    fun clear() {
        prefs.edit().remove(KEY_SESSION).apply()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val payload = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val out = ByteArray(1 + iv.size + payload.size)
        out[0] = iv.size.toByte()
        iv.copyInto(out, 1)
        payload.copyInto(out, 1 + iv.size)
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val data = Base64.decode(encoded, Base64.NO_WRAP)
        if (data.size < 2) error("cipher text too short")
        val ivSize = data[0].toInt()
        val iv = data.copyOfRange(1, 1 + ivSize)
        val payload = data.copyOfRange(1 + ivSize, data.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(payload), Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val TAG = "AuthStore"
        const val PREFS = "sharekt_auth"
        const val KEY_SESSION = "session"
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "sharekt_auth_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
