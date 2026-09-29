package com.example.gametracker.utils

import java.security.MessageDigest
import java.security.SecureRandom

object PasswordUtils {
    fun gerarSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    fun hash(salt: ByteArray, senha: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return toHex(digest.digest(senha.toByteArray(Charsets.UTF_8)))
    }

    fun compararHashes(hashA: String, hashB: String): Boolean =
        MessageDigest.isEqual(
            hashA.toByteArray(Charsets.UTF_8),
            hashB.toByteArray(Charsets.UTF_8)
        )

    fun toHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    fun hexToBytes(value: String): ByteArray =
        value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
