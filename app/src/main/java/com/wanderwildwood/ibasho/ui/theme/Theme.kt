package com.wanderwildwood.ibasho.ui.theme

import androidx.compose.runtime.Composable
import com.mudita.mmd.ThemeMMD

/**
 * Black on white, from MMD — for the Compose half of this app.
 *
 * What stood here was six colour schemes: light, dark, and medium- and high-contrast
 * variants of each, plus Android's dynamic colour pulled from the wallpaper. All of it
 * resolved, at runtime, to tints that a panel with sixteen greys renders as near-identical
 * mud. This screen has one appearance, and MMD is it.
 *
 * `AppTheme` keeps its name so the screens that wrap themselves in it still compile. It
 * has lost its `darkTheme` parameter, which no caller was passing. The scheme itself is in
 * [monochrome], the same file every app of this shop carries.
 *
 * ⚠ This app is half Compose and half Android views. Only the Compose half comes through
 * here — the access-control screens, About, the shared top bar and dialogs. The
 * thirty-five XML layouts are still themed by `AppTheme` in res/values/themes.xml, which
 * is Material3.Light, and by `FmdActivity.applyDynamicColors()`. So the dynamic-colours
 * switch in Appearance still does something over there and nothing here. That is a real
 * seam, and it closes when the XML half is rebuilt, not before.
 */
@Composable
fun AppTheme(content: @Composable () -> Unit) =
    ThemeMMD(colorScheme = monochrome, content = content)
