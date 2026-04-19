package com.fyp.resilientp2p

import com.fyp.resilientp2p.managers.LocationEstimator
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LocationEstimatorTest {

    @Test
    fun trilaterate_returnsNull_whenInsufficientAnchors() {
        val estimator = LocationEstimator()
        estimator.logFn = { _, _ -> }

        estimator.setAnchor("A", LocationEstimator.Position(0.0, 0.0))
        estimator.setAnchor("B", LocationEstimator.Position(10.0, 0.0))

        estimator.recordRtt("A", toRttMs(5.0))
        estimator.recordRtt("B", toRttMs(8.0))

        val pos = estimator.trilaterate()
        assertNull(pos)
    }

    @Test
    fun trilaterate_estimatesPosition_fromThreeAnchors() {
        val estimator = LocationEstimator()
        estimator.logFn = { _, _ -> }

        // True local point to recover.
        val expectedX = 3.0
        val expectedY = 4.0

        estimator.setAnchor("A", LocationEstimator.Position(0.0, 0.0))
        estimator.setAnchor("B", LocationEstimator.Position(10.0, 0.0))
        estimator.setAnchor("C", LocationEstimator.Position(0.0, 10.0))

        val dA = sqrt((expectedX - 0.0) * (expectedX - 0.0) + (expectedY - 0.0) * (expectedY - 0.0))
        val dB = sqrt((expectedX - 10.0) * (expectedX - 10.0) + (expectedY - 0.0) * (expectedY - 0.0))
        val dC = sqrt((expectedX - 0.0) * (expectedX - 0.0) + (expectedY - 10.0) * (expectedY - 10.0))

        estimator.recordRtt("A", toRttMs(dA))
        estimator.recordRtt("B", toRttMs(dB))
        estimator.recordRtt("C", toRttMs(dC))

        val pos = estimator.trilaterate()

        assertNotNull(pos)
        assertEquals(expectedX, pos!!.x, 0.2)
        assertEquals(expectedY, pos.y, 0.2)
    }

    @Test
    fun reset_clearsState() {
        val estimator = LocationEstimator()
        estimator.logFn = { _, _ -> }

        estimator.setAnchor("A", LocationEstimator.Position(0.0, 0.0))
        estimator.recordRtt("A", toRttMs(5.0))
        estimator.reset()

        assertNull(estimator.getPeerPosition("A"))
        assertNull(estimator.getSmoothedRtt("A"))
        assertNull(estimator.localPosition)
    }

    private fun toRttMs(distanceMeters: Double): Double =
        // Inverse of estimator formula: d = (rttMs / 1000) * c / 2
        distanceMeters * 2000.0 / 3e8
}
