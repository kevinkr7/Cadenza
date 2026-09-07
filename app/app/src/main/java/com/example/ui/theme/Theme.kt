package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Cadenza primarily uses a light, studio-hardware aesthetic. 
// We will force a unified "physical" look across both modes, but define it via Material 3.
private val CadenzaColorScheme = lightColorScheme(
    primary = CadenzaHyperMagenta,
    onPrimary = CadenzaWhite,
    secondary = CadenzaDeepSkyBlue,
    onSecondary = CadenzaWhite,
    background = CadenzaOffWhite,
    onBackground = CadenzaTextPrimary,
    surface = CadenzaPanelLight,
    onSurface = CadenzaTextPrimary,
    surfaceVariant = CadenzaRecessedBase,
    onSurfaceVariant = CadenzaTextSecondary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, 
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CadenzaColorScheme,
        typography = Typography,
        content = content
    )
}
