package com.fyp.resilientp2p.testing

import org.json.JSONArray
import org.json.JSONObject

/**
 * Result of an individual test within the test suite.
 */
data class TestResult(
    val testName: String,
    val passed: Boolean,
    val durationMs: Long,
    val details: Map<String, Any> = emptyMap(),
    val error: String? = null,
    val warnings: List<String> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("testName", testName)
        put("passed", passed)
        put("durationMs", durationMs)
        error?.let { put("error", it) }
        if (warnings.isNotEmpty()) put("warnings", JSONArray(warnings))
        details.forEach { (k, v) -> put(k, v) }
    }
}

/**
 * Aggregate result of a full test run.
 */
data class TestRunResult(
    val runTimestamp: String,
    val deviceName: String,
    val peers: List<String>,
    val results: List<TestResult>,
    val totalDurationMs: Long
) {
    val passedCount get() = results.count { it.passed }
    val failedCount get() = results.count { !it.passed }
    val allPassed get() = results.all { it.passed }

    fun toJson(): JSONObject = JSONObject().apply {
        put("testRun", runTimestamp)
        put("device", deviceName)
        put("peers", JSONArray(peers))
        put("totalDurationMs", totalDurationMs)
        put("passedCount", passedCount)
        put("failedCount", failedCount)
        put("allPassed", allPassed)
        put("results", JSONArray().apply {
            results.forEach { put(it.toJson()) }
        })
    }

    fun toCsv(): String {
        val sb = StringBuilder()
        sb.appendLine("# ResilientP2P Test Results")
        sb.appendLine("# Device: $deviceName")
        sb.appendLine("# Run: $runTimestamp")
        sb.appendLine("# Peers: ${peers.joinToString(", ")}")
        sb.appendLine("# Total Duration: ${totalDurationMs}ms")
        sb.appendLine("# Passed: $passedCount / ${results.size}")
        sb.appendLine("#")
        sb.appendLine("TestName,Passed,DurationMs,Error,Warnings,Details")
        results.forEach { r ->
            val detailStr = r.details.entries.joinToString("; ") { "${it.key}=${it.value}" }
            val warnStr = r.warnings.joinToString("; ")
            sb.appendLine("${r.testName},${r.passed},${r.durationMs},${r.error ?: ""},\"$warnStr\",\"$detailStr\"")
        }
        return sb.toString()
    }
}

/**
 * Current state of the test runner, exposed to UI.
 */
data class TestState(
    val isRunning: Boolean = false,
    val currentTest: String = "",
    val progress: Float = 0f,          // 0.0 to 1.0
    val completedTests: Int = 0,
    val totalTests: Int = 0,
    val results: List<TestResult> = emptyList(),
    val finalResult: TestRunResult? = null,
    val statusMessage: String = "Idle",
    val waitingForPeers: Boolean = false,
    val logMessages: List<String> = emptyList()  // Recent test log messages
)
