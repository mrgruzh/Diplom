package com.example.diplom.sec

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

object SecureStorage {
    private const val PREFS = "sec_prefs"
    private const val KEY_DB_PASSPHRASE = "db_pass"

    private fun prefs(ctx: Context) = EncryptedSharedPreferences.create(
        ctx,
        PREFS,
        MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /** Получаем/создаём секрет для SQLCipher (байты) */
    fun getOrCreateDbPassphrase(ctx: Context): ByteArray {
        val p = prefs(ctx)
        val hex = p.getString(KEY_DB_PASSPHRASE, null)
        if (hex != null) return hexToBytes(hex)
        // генерим 32 байта случайного ключа
        val buf = ByteArray(32)
        SecureRandom().nextBytes(buf)
        p.edit().putString(KEY_DB_PASSPHRASE, bytesToHex(buf)).apply()
        return buf
    }

    private fun bytesToHex(b: ByteArray) = b.joinToString("") { "%02x".format(it) }
    private fun hexToBytes(s: String): ByteArray {
        val out = ByteArray(s.length / 2)
        var i = 0
        while (i < s.length) {
            out[i / 2] = ((s[i].digitToInt(16) shl 4) + s[i + 1].digitToInt(16)).toByte()
            i += 2
        }
        return out
    }
}
