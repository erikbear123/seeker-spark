package com.seekerspark.game

import org.bouncycastle.math.ec.rfc8032.Ed25519
import org.junit.Assert.*
import org.junit.Test

class ScoreProofTest {
    @Test fun verifiesRealEd25519SignatureAndRejectsAlteredScore() {
        val privateKey = ByteArray(32) { (it + 1).toByte() }
        val publicKey = ByteArray(32); Ed25519.generatePublicKey(privateKey, 0, publicKey, 0)
        val message = ScoreProof.message("test-round", 120, 10, ScoreProof.base58(publicKey), "2026-10-02T00:00:00Z").toByteArray()
        val signature = ByteArray(64); Ed25519.sign(privateKey, 0, message, 0, message.size, signature, 0)
        assertTrue(ScoreProof.verify(publicKey, message, signature))
        assertFalse(ScoreProof.verify(publicKey, message + 0, signature))
        assertFalse(ScoreProof.verify(publicKey, message, ByteArray(64)))
        assertFalse(ScoreProof.verify(ByteArray(31), message, signature))
    }
    @Test fun base58PreservesLeadingZeroBytes() {
        assertEquals("111", ScoreProof.base58(byteArrayOf(0, 0, 0)))
        assertEquals("12", ScoreProof.base58(byteArrayOf(0, 1)))
    }
}
