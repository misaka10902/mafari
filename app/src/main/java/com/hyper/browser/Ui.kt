package com.hyper.browser

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme



/** 跟随 MIUIX 的 color mode 判断深色，而不是直接读系统深色。 */
@Composable
fun isAppDarkTheme(): Boolean = when (MiuixTheme.colorSchemeMode) {
    ColorSchemeMode.Light, ColorSchemeMode.MonetLight -> false
    ColorSchemeMode.Dark, ColorSchemeMode.MonetDark -> true
    else -> isSystemInDarkTheme()
}
