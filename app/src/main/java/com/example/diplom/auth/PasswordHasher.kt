package com.example.diplom.auth

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    const val DEFAULT_ITERATIONS: Int = 120_000
    private const val KEY_LENGTH_BITS: Int = 256

    fun newSalt(sizeBytes: Int = 16): ByteArray {
        val out = ByteArray(sizeBytes)
        SecureRandom().nextBytes(out)
        return out
    }

    fun hash(password: CharArray, salt: ByteArray, iterations: Int = DEFAULT_ITERATIONS): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    fun verify(password: CharArray, salt: ByteArray, expectedHash: ByteArray, iterations: Int): Boolean {
        val actual = hash(password, salt, iterations)
        if (actual.size != expectedHash.size) return false
        var diff = 0
        for (i in actual.indices) diff = diff or (actual[i].toInt() xor expectedHash[i].toInt())
        return diff == 0
    }

    fun bytesToHex(b: ByteArray): String = b.joinToString("") { "%02x".format(it) }

    fun hexToBytes(s: String): ByteArray {
        val clean = s.trim()
        if (clean.length % 2 != 0) return ByteArray(0)
        val out = ByteArray(clean.length / 2)
        var i = 0
        while (i < clean.length) {
            out[i / 2] = ((clean[i].digitToInt(16) shl 4) + clean[i + 1].digitToInt(16)).toByte()
            i += 2
        }
        return out
    }
}
