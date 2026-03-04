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
 * ## Encryption (Encrypt-then-MAC)
 * The shared secret is expanded into a 256-bit AES key via HKDF-SHA256.
 * `encrypt()`/`decrypt()` use AES-256-GCM (12-byte IV + ciphertext + 128-bit tag).
 * Wired into P2PManager's `forwardPacket()` (send) and `onPayloadReceived()` (receive)
 * using Encrypt-then-MAC: encrypt payload → HMAC the ciphertext → send.
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
    fun getPublicKeyBase64(): String =
        Base64.encodeToString(localKeyPair.public.encoded, Base64.NO_WRAP)

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
            val aesKeyBytes = hkdfExpand(sharedSecret, HKDF_INFO_AES.toByteArray(), AES_KEY_SIZE / 8)
            aesKeys[peerId] = SecretKeySpec(aesKeyBytes, "AES")

            // Derive HMAC key (HKDF-SHA256 with "samizdat-hmac" info)
            val hmacKeyBytes = hkdfExpand(sharedSecret, HKDF_INFO_HMAC.toByteArray(), AES_KEY_SIZE / 8)
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
     * Compute a **safety number** for verifying the key exchange with [peerId].
     *
     * Both peers compute the same number because the hash input is the
     * **sorted** concatenation of both public keys — order-independent,
     * just like Signal's safety numbers.
     *
     * Users compare this number out-of-band (e.g. reading digits aloud, or
     * showing a QR code) to confirm that no MITM tampered with the ECDH exchange.
     *
     * Output: 12-digit numeric string (e.g. "384 791 256 013") derived from
     * SHA-256(sortedKeys), formatted in groups of 3 for readability.
     *
     * @return The safety number string, or null if no key is exchanged with [peerId].
     */
    fun computeSafetyNumber(peerId: String): String? {
        val peerPubKey = peerPublicKeys[peerId] ?: return null
        val localPubKey = localKeyPair.public

        // Sort by encoded bytes to ensure both sides compute the same hash
        val localBytes = localPubKey.encoded
        val peerBytes = peerPubKey.encoded
        val (first, second) = if (compareByteArrays(localBytes, peerBytes) < 0) {
            localBytes to peerBytes
        } else {
            peerBytes to localBytes
        }

        val digest = java.security.MessageDigest.getInstance("SHA-256")
        digest.update(first)
        digest.update(second)
        val hash = digest.digest()

        // Convert first 6 bytes of hash to 12 decimal digits (2 digits per byte)
        val sb = StringBuilder()
        for (i in 0 until 6) {
            val value = hash[i].toInt() and 0xFF
            sb.append(String.format(java.util.Locale.US, "%02d", value % 100))
        }
        // Format as "XXX XXX XXX XXX" for readability
        return sb.toString().chunked(3).joinToString(" ")
    }

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

    /** Lexicographic comparison of two byte arrays (for deterministic key ordering). */
    private fun compareByteArrays(a: ByteArray, b: ByteArray): Int {
        for (i in 0 until minOf(a.size, b.size)) {
            val cmp = (a[i].toInt() and 0xFF) - (b[i].toInt() and 0xFF)
            if (cmp != 0) return cmp
        }
        return a.size - b.size
    }
}
