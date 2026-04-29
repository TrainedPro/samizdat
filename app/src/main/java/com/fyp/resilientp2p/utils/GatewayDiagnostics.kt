package com.fyp.resilientp2p.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.fyp.resilientp2p.BuildConfig
import com.fyp.resilientp2p.managers.P2PManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Diagnostic utilities for debugging Internet Gateway issues.
 * Helps identify Firebase configuration problems and network connectivity issues.
 */
object GatewayDiagnostics {

    /**
     * Run comprehensive diagnostics for Internet Gateway functionality.
     * Call this when gateway is not working as expected.
     */
    suspend fun runDiagnostics(context: Context, p2pManager: P2PManager): DiagnosticResult {
        val results = mutableListOf<DiagnosticCheck>()

        // 1. Check Firebase Configuration
        results.add(checkFirebaseConfig())

        // 2. Check Network Connectivity
        results.add(checkNetworkConnectivity(context))

        // 3. Check Internet Validation
        results.add(checkInternetValidation(context))

        // 4. Test Firestore Connectivity
        results.add(testFirestoreConnectivity())

        // 5. Check Gateway State
        results.add(checkGatewayState(p2pManager))

        val overallStatus = if (results.all { it.passed }) {
            DiagnosticStatus.PASS
        } else if (results.any { it.severity == DiagnosticSeverity.CRITICAL && !it.passed }) {
            DiagnosticStatus.CRITICAL_FAILURE
        } else {
            DiagnosticStatus.WARNINGS
        }

        return DiagnosticResult(overallStatus, results)
    }

    private fun checkFirebaseConfig(): DiagnosticCheck {
        val projectId = BuildConfig.FIREBASE_PROJECT_ID
        val apiKey = BuildConfig.FIREBASE_API_KEY

        return when {
            projectId.isBlank() && apiKey.isBlank() -> DiagnosticCheck(
                name = "Firebase Configuration",
                passed = false,
                severity = DiagnosticSeverity.CRITICAL,
                message = "FIREBASE_PROJECT_ID and FIREBASE_API_KEY are both empty in local.properties",
                solution = "Add FIREBASE_PROJECT_ID=your-project-id and FIREBASE_API_KEY=your-api-key to local.properties file"
            )
            projectId.isBlank() -> DiagnosticCheck(
                name = "Firebase Project ID",
                passed = false,
                severity = DiagnosticSeverity.CRITICAL,
                message = "FIREBASE_PROJECT_ID is empty in local.properties",
                solution = "Add FIREBASE_PROJECT_ID=your-project-id to local.properties file"
            )
            apiKey.isBlank() -> DiagnosticCheck(
                name = "Firebase API Key",
                passed = false,
                severity = DiagnosticSeverity.CRITICAL,
                message = "FIREBASE_API_KEY is empty in local.properties",
                solution = "Add FIREBASE_API_KEY=your-api-key to local.properties file"
            )
            else -> DiagnosticCheck(
                name = "Firebase Configuration",
                passed = true,
                severity = DiagnosticSeverity.INFO,
                message = "Firebase project ID and API key are configured " +
                    "(projectId=${projectId.take(10)}...)",
                solution = null
            )
        }
    }

    private fun checkNetworkConnectivity(context: Context): DiagnosticCheck {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork

        return if (network == null) {
            DiagnosticCheck(
                name = "Network Connectivity",
                passed = false,
                severity = DiagnosticSeverity.CRITICAL,
                message = "No active network connection",
                solution = "Connect to WiFi or enable mobile data"
            )
        } else {
            DiagnosticCheck(
                name = "Network Connectivity",
                passed = true,
                severity = DiagnosticSeverity.INFO,
                message = "Active network connection found",
                solution = null
            )
        }
    }

    private fun checkInternetValidation(context: Context): DiagnosticCheck {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }

        return when {
            caps == null -> DiagnosticCheck(
                name = "Internet Validation",
                passed = false,
                severity = DiagnosticSeverity.CRITICAL,
                message = "Cannot get network capabilities",
                solution = "Check network connection and try again"
            )
            !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> DiagnosticCheck(
                name = "Internet Capability",
                passed = false,
                severity = DiagnosticSeverity.CRITICAL,
                message = "Network does not have internet capability",
                solution = "Switch to a network with internet access"
            )
            !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> DiagnosticCheck(
                name = "Internet Validation",
                passed = false,
                severity = DiagnosticSeverity.WARNING,
                message = "Network internet access is not validated (may take 30-60 seconds)",
                solution = "Wait for Android to validate internet connectivity, or try opening a web browser"
            )
            else -> DiagnosticCheck(
                name = "Internet Validation",
                passed = true,
                severity = DiagnosticSeverity.INFO,
                message = "Network has validated internet access",
                solution = null
            )
        }
    }

    private suspend fun testFirestoreConnectivity(): DiagnosticCheck = withContext(Dispatchers.IO) {
        val projectId = BuildConfig.FIREBASE_PROJECT_ID
        val apiKey = BuildConfig.FIREBASE_API_KEY

        if (projectId.isBlank() || apiKey.isBlank()) {
            return@withContext DiagnosticCheck(
                name = "Firestore Connectivity",
                passed = false,
                severity = DiagnosticSeverity.CRITICAL,
                message = "Cannot test Firestore - Firebase not configured",
                solution = "Configure Firebase first"
            )
        }

        return@withContext try {
            val url = "https://firestore.googleapis.com/v1/projects/$projectId/" +
                "databases/(default)/documents/diagnostic_test?key=$apiKey"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
            }

            val responseCode = conn.responseCode
            conn.disconnect()

            when (responseCode) {
                200, 404 -> DiagnosticCheck(
                    name = "Firestore Connectivity",
                    passed = true,
                    severity = DiagnosticSeverity.INFO,
                    message = "Successfully connected to Firestore (HTTP $responseCode)",
                    solution = null
                )
                401, 403 -> DiagnosticCheck(
                    name = "Firestore Authentication",
                    passed = false,
                    severity = DiagnosticSeverity.CRITICAL,
                    message = "Firestore authentication failed (HTTP $responseCode)",
                    solution = "Check your FIREBASE_API_KEY in local.properties - " +
                        "it may be invalid or expired"
                )
                else -> DiagnosticCheck(
                    name = "Firestore Connectivity",
                    passed = false,
                    severity = DiagnosticSeverity.WARNING,
                    message = "Firestore returned HTTP $responseCode",
                    solution = "Check Firebase project status and API key permissions"
                )
            }
        } catch (e: Exception) {
            DiagnosticCheck(
                name = "Firestore Connectivity",
                passed = false,
                severity = DiagnosticSeverity.CRITICAL,
                message = "Failed to connect to Firestore: ${e.message}",
                solution = "Check internet connection and Firebase configuration"
            )
        }
    }

    private fun checkGatewayState(p2pManager: P2PManager): DiagnosticCheck {
        val gateway = p2pManager.internetGatewayManager
        return if (gateway == null) {
            DiagnosticCheck(
                name = "Gateway Manager",
                passed = false,
                severity = DiagnosticSeverity.CRITICAL,
                message = "InternetGatewayManager is not initialized",
                solution = "This is a code bug - gateway manager should be initialized in P2PApplication"
            )
        } else {
            val hasInternet = gateway.hasInternet.value
            val isEnabled = gateway.gatewayEnabled.value
            val isGateway = gateway.isGateway.value

            DiagnosticCheck(
                name = "Gateway State",
                passed = isGateway,
                severity = if (isGateway) DiagnosticSeverity.INFO else DiagnosticSeverity.WARNING,
                message = "hasInternet=$hasInternet, enabled=$isEnabled, isGateway=$isGateway",
                solution = if (!isGateway) {
                    "Gateway should activate automatically when internet is validated"
                } else {
                    null
                }
            )
        }
    }

    /**
     * Quick check to see if gateway should be working
     */
    fun quickCheck(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }
        val projectId = BuildConfig.FIREBASE_PROJECT_ID
        val apiKey = BuildConfig.FIREBASE_API_KEY

        return buildString {
            appendLine("=== GATEWAY QUICK CHECK ===")
            appendLine("Firebase Project ID: ${if (projectId.isBlank()) "NOT SET" else "${projectId.take(10)}..."}")
            @Suppress("MaxLineLength") // Log formatting
            appendLine("Firebase API Key: ${if (apiKey.isBlank()) "NOT SET" else "${apiKey.take(10)}..."}")
            appendLine("Active Network: ${if (network == null) "NONE" else "CONNECTED"}")
            @Suppress("MaxLineLength") // Log formatting
            appendLine("Internet Capability: ${if (caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) "YES" else "NO"}")
            @Suppress("MaxLineLength") // Log formatting
            appendLine("Internet Validated: ${if (caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true) "YES" else "NO (may take 30-60s)"}")
            appendLine("===============================")
        }
    }
}

data class DiagnosticResult(
    val status: DiagnosticStatus,
    val checks: List<DiagnosticCheck>
) {
    fun toLogString(): String = buildString {
        appendLine("=== GATEWAY DIAGNOSTICS ===")
        appendLine("Overall Status: $status")
        appendLine()
        checks.forEach { check ->
            val icon = if (check.passed) "[PASS]" else when (check.severity) {
                DiagnosticSeverity.CRITICAL -> "[FAIL]"
                DiagnosticSeverity.WARNING -> "[WARN]"
                DiagnosticSeverity.INFO -> "[INFO]"
            }
            appendLine("$icon ${check.name}: ${check.message}")
            if (check.solution != null) {
                appendLine("   Solution: ${check.solution}")
            }
        }
        appendLine("============================")
    }
}

data class DiagnosticCheck(
    val name: String,
    val passed: Boolean,
    val severity: DiagnosticSeverity,
    val message: String,
    val solution: String?
)

enum class DiagnosticStatus {
    PASS,
    WARNINGS,
    CRITICAL_FAILURE
}

enum class DiagnosticSeverity {
    INFO,
    WARNING,
    CRITICAL
}
