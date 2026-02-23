package com.example.diplom.auth

import org.json.JSONObject

data class UserProfile(
    val login: String,
    val fio: String,
    val callsign: String,
    val role: UserRole,
    val evacPoint: String?,
    val hospital: String?,
    val passwordSaltHex: String,
    val passwordHashHex: String,
    val passwordIterations: Int
) {
    fun toJsonString(): String = JSONObject()
        .put("login", login)
        .put("fio", fio)
        .put("callsign", callsign)
        .put("role", role.name)
        .put("evacPoint", evacPoint)
        .put("hospital", hospital)
        .put("passwordSaltHex", passwordSaltHex)
        .put("passwordHashHex", passwordHashHex)
        .put("passwordIterations", passwordIterations)
        .toString()

    companion object {
        fun fromJsonString(value: String): UserProfile? {
            return try {
                val o = JSONObject(value)
                val role = UserRole.fromName(o.optString("role", null)) ?: return null
                UserProfile(
                    login = o.getString("login"),
                    fio = o.optString("fio", ""),
                    callsign = o.optString("callsign", ""),
                    role = role,
                    evacPoint = o.optString("evacPoint", null),
                    hospital = o.optString("hospital", null),
                    passwordSaltHex = o.getString("passwordSaltHex"),
                    passwordHashHex = o.getString("passwordHashHex"),
                    passwordIterations = o.optInt("passwordIterations", PasswordHasher.DEFAULT_ITERATIONS)
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
