package com.fyp.resilientp2p.security

import android.util.Base64
import android.util.Log
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * End-to-end encryption and packet integrity for the Samizdat mesh network.
 *
 * ## Key Exchange
 * Uses ECDH (Elliptic-Curve Diffie-Hellman) on the prime256v1 (P-256) curve to
 * derive a shared secret during the IDENTITY handshake. Each peer generates an
 * ephemeral EC key pair; public keys are exchanged via IDENTITY packet payloads.
 *
 * ## Encryption
 * The shared secret is expanded into a 256-bit AES key via HKDF-SHA256.
 * Payloads are encrypted with AES-256-GCM (128-bit auth tag, 12-byte random IV).
 * The IV is prepended to the ciphertext.
 *
 * ## Packet Integrity
 * Every outgoing packet has an HMAC-SHA256 appended (32 bytes). The HMAC key is
 * derived from the shared secret (different derivation path than the AES key).
 * Packets with invalid HMACs are silently dropped.
 *
 * ## Limitations
 * - Key pairs are ephemeral (not persisted across app restarts).
 * - No certificate pinning or PKI — this is a research prototype.
 * - Forward secrecy is achieved by the ephemeral nature of each session.
 *
 * @see RateLimiter
 * @see PeerBlacklist
 */
class SecurityManager {

    companion object {
        private const val TAG = "SecurityManager"

        /** EC curve for ECDH key exchange. P-256 is widely supported on Android. */
        private const val EC_CURVE = "prime256v1"
        /** AES key size in bits derived from ECDH shared secret. */
        private const val AES_KEY_SIZE = 256
        /** GCM authentication tag length in bits. */
        private const val GCM_TAG_LENGTH = 128
        /** GCM initialization vector length in bytes. */
        private const val GCM_IV_LENGTH = 12
        /** HMAC algorithm. */
        private const val HMAC_ALGO = "HmacSHA256"
        /** HMAC output size in bytes. */
        const val HMAC_SIZE = 32
        /** HKDF info string for AES key derivation. */
        private const val HKDF_INFO_AES = "samizdat-aes"
        /** HKDF info string for HMAC key derivation. */
        private const val HKDF_INFO_HMAC = "samizdat-hmac"
    }

    /** This device's ephemeral ECDH key pair, generated once per session. */
    private val localKeyPair: KeyPair = generateECKeyPair()

    /** Peer public keys exchanged during IDENTITY handshake. peerId → PublicKey */
    private val peerPublicKeys = ConcurrentHashMap<String, PublicKey>()

    /** Derived AES-256 keys per peer. peerId → SecretKey */
    private val aesKeys = ConcurrentHashMap<String, SecretKey>()

    /** Derived HMAC keys per peer. peerId → SecretKey */
    private val hmacKeys = ConcurrentHashMap<String, SecretKey>()

    /** Secure random for IV generation. */
    private val secureRandom = SecureRandom()

    /**
     * Returns this device's ECDH public key as a Base64-encoded string.
     * Sent in IDENTITY packet payloads during the handshake.
     */
    fun getPublicKeyBase64(): String {
        return Base64.encodeToString(localKeyPair.public.encoded, Base64.NO_WRAP)
    }

    /**
     * Registers a peer's ECDH public key and derives the shared AES + HMAC keys.
     * Called when an IDENTITY packet is received containing the peer's public key.
     *
     * @param peerId The peer's device name.
     * @param publicKeyBase64 The peer's public key as a Base64-encoded string.
     * @return true if key exchange succeeded, false on error.
     */
    fun registerPeerKey(peerId: String, publicKeyBase64: String): Boolean {
        return try {
            val keyFactory = KeyFactory.getInstance("EC")
            val keyBytes = Base64.decode(publicKeyBase64, Base64.NO_WRAP)
            val peerPublicKey = keyFactory.generatePublic(X509EncodedKeySpec(keyBytes))
            peerPublicKeys[peerId] = peerPublicKey

            // Derive shared secret via ECDH
            val keyAgreement = KeyAgreement.getInstance("ECDH")
            keyAgreement.init(localKeyPair.private)
            keyAgreement.doPhase(peerPublicKey, true)
            val sharedSecret = keyAgreement.generateSecret()

            // Derive AES key (HKDF-SHA256 with "samizdat-aes" info)
            val aesKeyBytes = hkdfExpand(sharedSecret, HKDF_INFO_AES.toByteArray(), 32)
            aesKeys[peerId] = SecretKeySpec(aesKeyBytes, "AES")

            // Derive HMAC key (HKDF-SHA256 with "samizdat-hmac" info)
            val hmacKeyBytes = hkdfExpand(sharedSecret, HKDF_INFO_HMAC.toByteArray(), 32)
            hmacKeys[peerId] = SecretKeySpec(hmacKeyBytes, HMAC_ALGO)

            Log.i(TAG, "KEY_EXCHANGE_OK peer=$peerId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "KEY_EXCHANGE_FAILED peer=$peerId error=${e.message}")
            false
        }
    }

    /**
     * Encrypts payload bytes for a specific peer using AES-256-GCM.
     *
     * Output format: `[12-byte IV][ciphertext+GCM tag]`
     *
     * @param peerId Target peer.
     * @param plaintext Raw payload bytes.
     * @return Encrypted bytes (IV + ciphertext), or null if no key exists for this peer.
     */
    fun encrypt(peerId: String, plaintext: ByteArray): ByteArray? {
        val key = aesKeys[peerId] ?: return null
        return try {
            val iv = ByteArray(GCM_IV_LENGTH)
            secureRandom.nextBytes(iv)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val ciphertext = cipher.doFinal(plaintext)

            // Prepend IV
            iv + ciphertext
        } catch (e: Exception) {
            Log.e(TAG, "ENCRYPT_FAILED peer=$peerId error=${e.message}")
            null
        }
    }

    /**
     * Decrypts payload bytes from a specific peer.
     *
     * @param peerId Source peer.
     * @param ciphertext Encrypted bytes (IV + ciphertext as produced by [encrypt]).
     * @return Decrypted plaintext, or null on failure (wrong key, tampered data).
     */
    fun decrypt(peerId: String, ciphertext: ByteArray): ByteArray? {
        val key = aesKeys[peerId] ?: return null
        if (ciphertext.size < GCM_IV_LENGTH + 16) return null // Too short (IV + min tag)
        return try {
            val iv = ciphertext.copyOfRange(0, GCM_IV_LENGTH)
            val encrypted = ciphertext.copyOfRange(GCM_IV_LENGTH, ciphertext.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            cipher.doFinal(encrypted)
        } catch (e: Exception) {
            Log.w(TAG, "DECRYPT_FAILED peer=$peerId error=${e.message}")
            null
        }
    }

    /**
     * Computes HMAC-SHA256 over the given data for a specific peer.
     *
     * @param peerId The peer whose HMAC key to use.
     * @param data The data to authenticate.
     * @return 32-byte HMAC, or null if no HMAC key exists for this peer.
     */
    fun computeHmac(peerId: String, data: ByteArray): ByteArray? {
        val key = hmacKeys[peerId] ?: return null
        return try {
            val mac = Mac.getInstance(HMAC_ALGO)
            mac.init(key)
            mac.doFinal(data)
        } catch (e: Exception) {
            Log.e(TAG, "HMAC_COMPUTE_FAILED peer=$peerId error=${e.message}")
            null
        }
    }

    /**
     * Verifies HMAC-SHA256 of the given data against the expected value.
     *
     * @param peerId The peer whose HMAC key to use.
     * @param data The data that was authenticated.
     * @param expectedHmac The HMAC to verify against (32 bytes).
     * @return true if the HMAC is valid, false otherwise.
     */
    fun verifyHmac(peerId: String, data: ByteArray, expectedHmac: ByteArray): Boolean {
        val computed = computeHmac(peerId, data) ?: return false
        return MessageDigest.isEqual(computed, expectedHmac)
    }

    /**
     * Returns true if we have completed key exchange with this peer.
     */
    fun hasKeyForPeer(peerId: String): Boolean = aesKeys.containsKey(peerId)

    /**
     * Removes all key material for a peer (on disconnect).
     */
    fun removePeerKeys(peerId: String) {
        peerPublicKeys.remove(peerId)
        aesKeys.remove(peerId)
        hmacKeys.remove(peerId)
        Log.d(TAG, "KEYS_REMOVED peer=$peerId")
    }

    /**
     * Clears all key material (on shutdown).
     */
    fun destroy() {
        peerPublicKeys.clear()
        aesKeys.clear()
        hmacKeys.clear()
        Log.i(TAG, "SecurityManager destroyed — all keys cleared")
    }

    // --- Key Generation ---

    private fun generateECKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        keyPairGenerator.initialize(ECGenParameterSpec(EC_CURVE))
        return keyPairGenerator.generateKeyPair()
    }

    // --- HKDF-SHA256 (simplified — extract+expand) ---

    /**
     * Simplified HKDF-expand using HMAC-SHA256.
     * Input keying material (IKM) is used directly as the PRK (no extract step needed
     * since ECDH output already has sufficient entropy).
     */
    private fun hkdfExpand(ikm: ByteArray, info: ByteArray, length: Int): ByteArray {
        val mac = Mac.getInstance(HMAC_ALGO)
        mac.init(SecretKeySpec(ikm, HMAC_ALGO))

        val result = ByteArray(length)
        var t = ByteArray(0)
        var offset = 0
        var counter: Byte = 1

        while (offset < length) {
            mac.reset()
            mac.update(t)
            mac.update(info)
            mac.update(counter)
            t = mac.doFinal()
            val toCopy = minOf(t.size, length - offset)
            System.arraycopy(t, 0, result, offset, toCopy)
            offset += toCopy
            counter++
        }

        return result
    }
}
