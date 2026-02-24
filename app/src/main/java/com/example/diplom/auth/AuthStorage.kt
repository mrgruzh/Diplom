package com.example.diplom.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object AuthStorage {
    private const val PREFS = "auth_prefs"
    private const val KEY_CURRENT_LOGIN = "current_login"

    private fun prefs(ctx: Context) = EncryptedSharedPreferences.create(
        ctx,
        PREFS,
        MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun userKey(login: String) = "user:$login"

    fun currentUser(ctx: Context): UserProfile? {
        val p = prefs(ctx)
        val login = p.getString(KEY_CURRENT_LOGIN, null) ?: return null
        val json = p.getString(userKey(login), null) ?: return null
        return UserProfile.fromJsonString(json)
    }

    fun logout(ctx: Context) {
        prefs(ctx).edit().remove(KEY_CURRENT_LOGIN).apply()
    }

    fun register(
        ctx: Context,
        login: String,
        password: String,
        fio: String,
        callsign: String,
        role: UserRole,
        evacPoint: String?,
        hospital: String?
    ): Boolean {
        val l = login.trim()
        if (l.isBlank()) return false
        if (password.isBlank()) return false

        val p = prefs(ctx)
        if (p.contains(userKey(l))) return false

        val salt = PasswordHasher.newSalt()
        val iterations = PasswordHasher.DEFAULT_ITERATIONS
        val hash = PasswordHasher.hash(password.toCharArray(), salt, iterations)

        val profile = UserProfile(
            login = l,
            fio = fio.trim(),
            callsign = callsign.trim(),
            role = role,
            evacPoint = evacPoint?.trim()?.takeIf { it.isNotBlank() },
            hospital = hospital?.trim()?.takeIf { it.isNotBlank() },
            passwordSaltHex = PasswordHasher.bytesToHex(salt),
            passwordHashHex = PasswordHasher.bytesToHex(hash),
            passwordIterations = iterations
        )

        p.edit()
            .putString(userKey(l), profile.toJsonString())
            .putString(KEY_CURRENT_LOGIN, l)
            .apply()

        return true
    }

    fun login(ctx: Context, login: String, password: String): Boolean {
        val l = login.trim()
        val p = prefs(ctx)
        val json = p.getString(userKey(l), null) ?: return false
        val profile = UserProfile.fromJsonString(json) ?: return false

        val salt = PasswordHasher.hexToBytes(profile.passwordSaltHex)
        val expected = PasswordHasher.hexToBytes(profile.passwordHashHex)
        val ok = PasswordHasher.verify(password.toCharArray(), salt, expected, profile.passwordIterations)
        if (!ok) return false

        p.edit().putString(KEY_CURRENT_LOGIN, l).apply()
        return true
    }

    fun allUsers(ctx: Context): List<UserProfile> {
        val p = prefs(ctx)
        val out = ArrayList<UserProfile>()

        for ((k, v) in p.all) {
            if (!k.startsWith("user:")) continue
            val json = v as? String ?: continue
            val profile = UserProfile.fromJsonString(json) ?: continue
            out.add(profile)
        }

        return out
    }
}
