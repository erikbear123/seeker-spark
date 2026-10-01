package com.seekerspark.game

import org.bouncycastle.math.ec.rfc8032.Ed25519
import java.math.BigInteger

object ScoreProof {
    fun message(roundId: String, score: Int, hits: Int, address: String, completedAt: String): String =
        "Seeker Spark score proof\nVersion: 1\nRound: $roundId\nScore: $score\nSparks: $hits\nWallet: $address\nCompleted: $completedAt\nPurpose: certify this local game result. No transaction or payment."

    fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean =
        publicKey.size == 32 && signature.size == 64 &&
            Ed25519.verify(signature, 0, publicKey, 0, message, 0, message.size)

    fun base58(bytes: ByteArray): String {
        val alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
        var value = BigInteger(1, bytes)
        val result = StringBuilder()
        val base = BigInteger.valueOf(58)
        while (value > BigInteger.ZERO) {
            val parts = value.divideAndRemainder(base)
            result.append(alphabet[parts[1].toInt()]); value = parts[0]
        }
        bytes.takeWhile { it == 0.toByte() }.forEach { _ -> result.append('1') }
        return result.reverse().toString()
    }
}
