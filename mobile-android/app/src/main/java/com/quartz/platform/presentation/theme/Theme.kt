package com.quartz.platform.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme

private val QuartzLightColors = lightColorScheme(
    primary = QuartzBlue,
    onPrimary = QuartzSurface,
    secondary = QuartzOrange,
    background = QuartzBackground,
    surface = QuartzSurface,
    onSurface = QuartzOnSurface,
    error = QuartzError
)

private val QuartzDarkColors = darkColorScheme(
    primary = QuartzBlueContainer,
    secondary = QuartzOrange,
    background = QuartzOnSurface,
    surface = QuartzOnSurface
)

@Composable
fun QuartzTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) QuartzDarkColors else QuartzLightColors
    MaterialTheme(
        colorScheme = colors,
        typography = QuartzTypography,
        content = content
    )
}
