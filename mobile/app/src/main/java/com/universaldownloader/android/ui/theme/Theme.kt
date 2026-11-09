package com.universaldownloader.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = FunimatedRed,
    secondary = FunimatedYellow,
    tertiary = FunimatedGreen,
    background = FunimatedBg,
    surface = FunimatedSurface,
    onPrimary = FunimatedBorder,
    onBackground = FunimatedBorder,
    onSurface = FunimatedBorder
)

@Composable
fun UniversalDownloaderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
