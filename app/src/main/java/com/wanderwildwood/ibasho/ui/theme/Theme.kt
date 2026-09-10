package com.wanderwildwood.ibasho.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.mudita.mmd.ThemeMMD
import androidx.compose.material3.lightColorScheme

/**
 * Black on white, from MMD — for the Compose half of this app.
 *
 * What stood here was six colour schemes: light, dark, and medium- and high-contrast
 * variants of each, plus Android's dynamic colour pulled from the wallpaper. All of it
 * resolved, at runtime, to tints that a panel with sixteen greys renders as near-identical
 * mud. This screen has one appearance, and MMD is it.
 *
 * `AppTheme` keeps its name so the screens that wrap themselves in it still compile. It
 * has lost its `darkTheme` parameter, which no caller was passing.
 */

/**
 * Monochrome, built the safe way.
 *
 * ⚠ **Not `eInkColorScheme.copy(...)`.** MMD constructs its scheme by calling the raw
 * `ColorScheme(...)` constructor as it stood in Material 3 1.3.1, which is what MMD was
 * compiled against. This app resolves a newer Material 3, and every colour role added
 * since — the surfaceContainer tiers among them — is left at whatever that constructor
 * defaults to. Six are `Color.Unspecified` in MMD's own source, and more arrive unset
 * from the version gap. `copy()` cannot fill what was never set.
 *
 * An unspecified colour is not a missing colour; it is an invalid one. Handed to a paint
 * it throws `IllegalArgumentException: Invalid ID: 63` from deep inside ColorSpace, with
 * nothing in the message to say which colour or which composable. Before that it simply
 * painted the access-control screen solid black, silently.
 *
 * `lightColorScheme()` fills every role the *resolved* Material 3 has, so starting there
 * and overriding is version-proof. The overrides are MMD's own values. This is also,
 * incidentally, how cycle and Audio Reading always built their schemes, which is why
 * neither ever hit this.
 */
private val monochrome = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = Color.Black,
    onPrimaryContainer = Color.White,
    inversePrimary = Color.White,
    secondary = Color.White,
    onSecondary = Color.Black,
    secondaryContainer = Color.Black,
    onSecondaryContainer = Color.White,
    tertiary = Color.White,
    onTertiary = Color.Black,
    tertiaryContainer = Color.Black,
    onTertiaryContainer = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color.White,
    onSurfaceVariant = Color.Black,
    surfaceTint = Color.White,
    inverseSurface = Color.White,
    inverseOnSurface = Color.Black,
    error = Color.Black,
    onError = Color.White,
    errorContainer = Color.White,
    onErrorContainer = Color.Black,
    outline = Color.Black,
    outlineVariant = Color.Black,
    scrim = Color.Black,
    surfaceBright = Color.White,
    surfaceDim = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainerLowest = Color.White,
)

/**
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
