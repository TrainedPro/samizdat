package com.fyp.resilientp2p.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme =
        darkColorScheme(
                primary = NeonBlue,
                secondary = NeonGreen,
                tertiary = NeonRed,
                background = DarkBackground,
                surface = DarkSurface,
                onPrimary = Color.White,
                onSecondary = Color.Black,
                onBackground = TextWhite,
                onSurface = TextWhite
        )

private val LightColorScheme =
        lightColorScheme(
                primary = NeonBlueDark,
                secondary = NeonGreenDark,
                tertiary = NeonRedDark,
                background = Color(0xFFF5F5F5),
                surface = Color.White,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color.Black,
                onSurface = Color.Black
        )

@Composable
fun ResilientP2PTestbedTheme(
        darkTheme: Boolean = isSystemInDarkTheme(),
        // Dynamic color is available on Android 12+
        dynamicColor: Boolean = false, // Disabled dynamic color to enforce our custom UI branding
        content: @Composable () -> Unit
) {
        val colorScheme =
                when {
                        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                                val context = LocalContext.current
                                if (darkTheme) dynamicDarkColorScheme(context)
                                else dynamicLightColorScheme(context)
                        }
                        darkTheme -> DarkColorScheme
                        else -> LightColorScheme
                }

        val view = LocalView.current
        if (!view.isInEditMode) {
                SideEffect {
                        val window = (view.context as Activity).window
                        if (Build.VERSION.SDK_INT < 35) {
                                try {
                                        val method =
                                                window.javaClass.getMethod(
                                                        "setStatusBarColor",
                                                        Int::class.javaPrimitiveType
                                                )
                                        method.invoke(window, colorScheme.background.toArgb())
                                } catch (e: Exception) {
                                        // Ignore reflection errors
                                }
                        }
                        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                                !darkTheme
                }
        }

        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
