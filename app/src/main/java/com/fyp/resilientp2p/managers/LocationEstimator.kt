package com.fyp.resilientp2p.managers

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * RTT-based trilateration engine for approximate 2D positioning of mesh peers.
 *
 * ## Algorithm
 * Uses PING/PONG round-trip-time (RTT) measurements to estimate distances.
 * When 3+ peers have known positions (anchors), trilateration computes
 * the local device's (x, y) coordinate on an arbitrary 2D plane.
 *
 * ## Distance Model
 * WiFi Direct / BLE: $d = \frac{RTT \times c}{2}$ where $c = 3 \times 10^8$ m/s.
 * Since Nearby Connections runs over WiFi Direct with ~1–5 ms RTT, raw distances
 * are noisy. We apply an exponential moving average (EMA) to smooth RTT before
 * distance conversion.
 *
 * ## Coordinate System
 * The first anchor is placed at (0, 0). The second anchor is placed on the X-axis
 * at (estimatedDistance, 0). Subsequent peers trilaterate relative to these.
 *
 * @property speedOfLight Speed of light in m/s used for distance calculation.
 * @property emaAlpha Smoothing factor for the exponential moving average (0–1).
 */
class LocationEstimator(
    private val speedOfLight: Double = 3e8,
    private val emaAlpha: Double = 0.3
) {
    companion object {
        private const val TAG = "LocationEstimator"
        /** Minimum number of reference points needed for trilateration. */
        const val MIN_ANCHORS = 3
    }

    /** Smoothed RTT in milliseconds per peer. */
    private val smoothedRtt = ConcurrentHashMap<String, Double>()

    /** Known 2D positions of peers (anchor nodes or previously located). */
    private val peerPositions = ConcurrentHashMap<String, Position>()

    /** The estimated local position (null until trilateration succeeds). */
    @Volatile
    var localPosition: Position? = null
        private set

    /**
     * 2D position on the mesh coordinate plane.
     * @property x X coordinate in metres.
     * @property y Y coordinate in metres.
     */
    data class Position(val x: Double, val y: Double) {
        /** Euclidean distance to another position. */
        fun distanceTo(other: Position): Double =
            sqrt((x - other.x).pow(2) + (y - other.y).pow(2))
    }

    /**
     * Record an RTT measurement from a PING/PONG exchange.
     *
     * @param peerId The peer name.
     * @param rttMs Round-trip time in milliseconds.
     */
    fun recordRtt(peerId: String, rttMs: Double) {
        val prev = smoothedRtt[peerId]
        val smoothed = if (prev == null) rttMs else emaAlpha * rttMs + (1 - emaAlpha) * prev
        smoothedRtt[peerId] = smoothed
    }

    /**
     * Convert current smoothed RTT to estimated distance in metres.
     * Formula: distance = (RTT_ms / 1000) * c / 2
     */
    fun estimateDistance(peerId: String): Double? {
        val rtt = smoothedRtt[peerId] ?: return null
        return (rtt / 1000.0) * speedOfLight / 2.0
    }

    /**
     * Set a peer's known position (e.g. the first two anchors are manually placed,
     * or a previously trilaterated peer becomes an anchor for others).
     */
    fun setAnchor(peerId: String, position: Position) {
        peerPositions[peerId] = position
    }

    /** Get a peer's position if known. */
    fun getPeerPosition(peerId: String): Position? = peerPositions[peerId]

    /** Get all known peer positions. */
    fun getAllPositions(): Map<String, Position> = HashMap(peerPositions)

    /** Get smoothed RTT for a peer (for UI display). */
    fun getSmoothedRtt(peerId: String): Double? = smoothedRtt[peerId]

    /** Get all peer RTTs. */
    fun getAllRtts(): Map<String, Double> = HashMap(smoothedRtt)

    /**
     * Auto-assign anchor positions. The first peer with RTT becomes (0,0),
     * the second is placed on the X-axis at the estimated distance.
     */
    fun autoAssignAnchors() {
        val peers = smoothedRtt.keys.toList()
        if (peers.isEmpty()) return

        // First anchor at origin
        if (!peerPositions.containsKey(peers[0])) {
            peerPositions[peers[0]] = Position(0.0, 0.0)
        }

        // Second anchor on X-axis
        if (peers.size >= 2 && !peerPositions.containsKey(peers[1])) {
            val d = estimateDistance(peers[1]) ?: 1.0
            peerPositions[peers[1]] = Position(d, 0.0)
        }
    }

    /**
     * Attempt trilateration using 3+ anchor peers with known positions
     * and RTT-derived distances.
     *
     * Uses a least-squares linearized approach:
     * Given anchors A1=(x1,y1), A2=(x2,y2), A3=(x3,y3) with distances d1, d2, d3,
     * solve the system of linear equations obtained by subtracting the circle equations.
     *
     * @return The estimated local position, or null if insufficient data.
     */
    fun trilaterate(): Position? {
        autoAssignAnchors()

        // Collect anchors for which we have both position and distance
        data class Anchor(val name: String, val pos: Position, val dist: Double)

        val anchors = peerPositions.mapNotNull { (peer, pos) ->
            val dist = estimateDistance(peer)
            if (dist != null) Anchor(peer, pos, dist) else null
        }

        if (anchors.size < MIN_ANCHORS) {
            Log.d(TAG, "trilaterate: need $MIN_ANCHORS anchors, have ${anchors.size}")
            return null
        }

        // Use first anchor as reference; form linear system from differences
        val ref = anchors[0]
        val n = anchors.size - 1

        // Build matrices A and b: for each i=1..n:
        //   2*(xi - x0)*x + 2*(yi - y0)*y = di0^2 - di^2 + xi^2 - x0^2 + yi^2 - y0^2
        val aMatrix = Array(n) { DoubleArray(2) }
        val bVector = DoubleArray(n)

        for (i in 1..n.coerceAtMost(anchors.size - 1)) {
            val a = anchors[i]
            aMatrix[i - 1][0] = 2.0 * (a.pos.x - ref.pos.x)
            aMatrix[i - 1][1] = 2.0 * (a.pos.y - ref.pos.y)
            bVector[i - 1] = ref.dist.pow(2) - a.dist.pow(2) +
                    a.pos.x.pow(2) - ref.pos.x.pow(2) +
                    a.pos.y.pow(2) - ref.pos.y.pow(2)
        }

        // Solve via least squares: (A^T A)^{-1} A^T b
        val result = leastSquares2x2(aMatrix, bVector) ?: return null
        val pos = Position(result[0], result[1])
        localPosition = pos
        Log.d(TAG, "trilaterate: position=(${pos.x}, ${pos.y}) from ${anchors.size} anchors")
        return pos
    }

    /**
     * Solve over-determined 2-variable least squares system.
     * Returns [x, y] or null if singular.
     */
    private fun leastSquares2x2(a: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
        val n = a.size
        // Compute A^T * A (2x2)
        var ata00 = 0.0; var ata01 = 0.0; var ata11 = 0.0
        var atb0 = 0.0; var atb1 = 0.0

        for (i in 0 until n) {
            ata00 += a[i][0] * a[i][0]
            ata01 += a[i][0] * a[i][1]
            ata11 += a[i][1] * a[i][1]
            atb0 += a[i][0] * b[i]
            atb1 += a[i][1] * b[i]
        }

        val det = ata00 * ata11 - ata01 * ata01
        if (kotlin.math.abs(det) < 1e-10) return null // Singular

        val x = (ata11 * atb0 - ata01 * atb1) / det
        val y = (ata00 * atb1 - ata01 * atb0) / det
        return doubleArrayOf(x, y)
    }

    /** Clear all data (e.g. on mesh restart). */
    fun reset() {
        smoothedRtt.clear()
        peerPositions.clear()
        localPosition = null
    }
}
