@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalTextApi::class)

package dev.podlink.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.podlink.R

private val GoogleSansFlex = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(
            FontWeight.Normal,
            FontStyle.Normal,
            FontVariation.Setting("ROND", 60f),
        ),
    ),
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontWeight.Medium,
            FontStyle.Normal,
            FontVariation.Setting("ROND", 75f),
        ),
    ),
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontWeight.SemiBold,
            FontStyle.Normal,
            FontVariation.Setting("ROND", 90f),
        ),
    ),
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontWeight.Bold,
            FontStyle.Normal,
            FontVariation.Setting("ROND", 100f),
        ),
    ),
)

private val GoogleSansFlexDisplay = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontWeight(550),
            FontStyle.Normal,
            FontVariation.Setting("ROND", 100f),
            FontVariation.opticalSizing(36.sp),
        ),
    ),
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontWeight(620),
            FontStyle.Normal,
            FontVariation.Setting("ROND", 100f),
            FontVariation.opticalSizing(36.sp),
        ),
    ),
    Font(
        resId = R.font.google_sans_flex,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontWeight.Bold,
            FontStyle.Normal,
            FontVariation.Setting("ROND", 100f),
            FontVariation.opticalSizing(36.sp),
        ),
    ),
)

private val BaseTypography = Typography()

val PodTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = GoogleSansFlexDisplay, letterSpacing = (-0.5).sp),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = GoogleSansFlexDisplay, letterSpacing = (-0.3).sp),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = GoogleSansFlexDisplay),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = GoogleSansFlexDisplay, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = GoogleSansFlexDisplay, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = GoogleSansFlexDisplay, fontWeight = FontWeight.SemiBold),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = GoogleSansFlex, fontWeight = FontWeight.SemiBold),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = GoogleSansFlex),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = GoogleSansFlex),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = GoogleSansFlex),
    labelLarge = BaseTypography.labelLarge.copy(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium),
    displayLargeEmphasized = BaseTypography.displayLargeEmphasized.copy(fontFamily = GoogleSansFlexDisplay, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    displayMediumEmphasized = BaseTypography.displayMediumEmphasized.copy(fontFamily = GoogleSansFlexDisplay, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    displaySmallEmphasized = BaseTypography.displaySmallEmphasized.copy(fontFamily = GoogleSansFlexDisplay, fontWeight = FontWeight.SemiBold),
    headlineLargeEmphasized = BaseTypography.headlineLargeEmphasized.copy(fontFamily = GoogleSansFlexDisplay, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    headlineMediumEmphasized = BaseTypography.headlineMediumEmphasized.copy(fontFamily = GoogleSansFlexDisplay, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    headlineSmallEmphasized = BaseTypography.headlineSmallEmphasized.copy(fontFamily = GoogleSansFlexDisplay, fontWeight = FontWeight.SemiBold),
    titleLargeEmphasized = BaseTypography.titleLargeEmphasized.copy(fontFamily = GoogleSansFlex, fontWeight = FontWeight.SemiBold),
    titleMediumEmphasized = BaseTypography.titleMediumEmphasized.copy(fontFamily = GoogleSansFlex, fontWeight = FontWeight.SemiBold),
    titleSmallEmphasized = BaseTypography.titleSmallEmphasized.copy(fontFamily = GoogleSansFlex, fontWeight = FontWeight.SemiBold),
    bodyLargeEmphasized = BaseTypography.bodyLargeEmphasized.copy(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium),
    bodyMediumEmphasized = BaseTypography.bodyMediumEmphasized.copy(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium),
    bodySmallEmphasized = BaseTypography.bodySmallEmphasized.copy(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium),
    labelLargeEmphasized = BaseTypography.labelLargeEmphasized.copy(fontFamily = GoogleSansFlex, fontWeight = FontWeight.SemiBold),
    labelMediumEmphasized = BaseTypography.labelMediumEmphasized.copy(fontFamily = GoogleSansFlex, fontWeight = FontWeight.SemiBold),
    labelSmallEmphasized = BaseTypography.labelSmallEmphasized.copy(fontFamily = GoogleSansFlex, fontWeight = FontWeight.SemiBold),
)

val PodShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
    largeIncreased = RoundedCornerShape(24.dp),
    extraLargeIncreased = RoundedCornerShape(24.dp),
    extraExtraLarge = RoundedCornerShape(24.dp),
)

private val FallbackLightColors = lightColorScheme(
    primary = Color(0xFF0062A1),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D35),
    secondary = Color(0xFF535F70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD6E3F7),
    onSecondaryContainer = Color(0xFF101C2B),
    tertiary = Color(0xFF6B5778),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF3DAFF),
    onTertiaryContainer = Color(0xFF251432),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C6CF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F3FA),
    surfaceContainer = Color(0xFFECEEF4),
    surfaceContainerHigh = Color(0xFFE6E8EE),
    surfaceContainerHighest = Color(0xFFE1E2E9),
    surfaceBright = Color(0xFFFDFDFF),
)

private val FallbackDarkColors = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF003257),
    primaryContainer = Color(0xFF00497B),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3B4858),
    onSecondaryContainer = Color(0xFFD6E3F7),
    tertiary = Color(0xFFD7BEE4),
    onTertiary = Color(0xFF3B2947),
    tertiaryContainer = Color(0xFF53405F),
    onTertiaryContainer = Color(0xFFF3DAFF),
    background = Color(0xFF111418),
    onBackground = Color(0xFFE1E2E9),
    surface = Color(0xFF111418),
    onSurface = Color(0xFFE1E2E9),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C6CF),
    outline = Color(0xFF8D9199),
    outlineVariant = Color(0xFF43474E),
    surfaceContainerLowest = Color(0xFF0C0E13),
    surfaceContainerLow = Color(0xFF191C20),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerHigh = Color(0xFF272A2F),
    surfaceContainerHighest = Color(0xFF32353A),
    surfaceBright = Color(0xFF37393E),
)

@Immutable
data class PodSemanticColors(
    val pageContainer: Color,
    val cardContainer: Color,
    val cardInsetContainer: Color,
    val rowContainer: Color,
    val rowDisabledContainer: Color,
    val primaryDisabledInk: Color,
    val secondaryDisabledInk: Color,
)

private val DefaultPodSemanticColors = PodSemanticColors(
    pageContainer = FallbackDarkColors.surfaceContainer,
    cardContainer = FallbackDarkColors.surfaceContainerLow,
    cardInsetContainer = FallbackDarkColors.surfaceContainerHigh,
    rowContainer = FallbackDarkColors.surfaceContainerHigh,
    rowDisabledContainer = FallbackDarkColors.surfaceContainerHigh.copy(alpha = 0.55f),
    primaryDisabledInk = FallbackDarkColors.onSurface.copy(alpha = 0.38f),
    secondaryDisabledInk = FallbackDarkColors.onSurface.copy(alpha = 0.34f),
)

private val LocalPodColors = staticCompositionLocalOf { DefaultPodSemanticColors }

@Immutable
object PodThemeTokens {
    val pageContainer: Color
        @Composable @ReadOnlyComposable
        get() = LocalPodColors.current.pageContainer

    val cardContainer: Color
        @Composable @ReadOnlyComposable
        get() = LocalPodColors.current.cardContainer

    val cardInsetContainer: Color
        @Composable @ReadOnlyComposable
        get() = LocalPodColors.current.cardInsetContainer

    val rowContainer: Color
        @Composable @ReadOnlyComposable
        get() = LocalPodColors.current.rowContainer

    val rowDisabledContainer: Color
        @Composable @ReadOnlyComposable
        get() = LocalPodColors.current.rowDisabledContainer

    val primaryDisabledInk: Color
        @Composable @ReadOnlyComposable
        get() = LocalPodColors.current.primaryDisabledInk

    val secondaryDisabledInk: Color
        @Composable @ReadOnlyComposable
        get() = LocalPodColors.current.secondaryDisabledInk

    val outerRadius = 24.dp
    val innerRadius = 6.dp
    val segmentGap = 2.dp
    val sectionSpacing = 14.dp
}

val TextStyle.tabular: TextStyle
    get() = copy(fontFeatureSettings = "tnum")

@Composable
fun PodTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scheme: ColorScheme = remember(context, darkTheme, dynamicColor) {
        when {
            dynamicColor -> if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            darkTheme -> FallbackDarkColors
            else -> FallbackLightColors
        }
    }
    val semanticColors = remember(scheme, darkTheme) {
        val row = if (darkTheme) scheme.surfaceContainerHigh else scheme.surfaceContainerLowest
        PodSemanticColors(
            pageContainer = scheme.surfaceContainer,
            cardContainer = if (darkTheme) scheme.surfaceContainerLow else scheme.surfaceBright,
            cardInsetContainer = if (darkTheme) scheme.surfaceContainerHigh else scheme.surfaceContainerLow,
            rowContainer = row,
            rowDisabledContainer = row.copy(alpha = 0.55f),
            primaryDisabledInk = scheme.onSurface.copy(alpha = 0.38f),
            secondaryDisabledInk = scheme.onSurface.copy(alpha = 0.34f),
        )
    }

    CompositionLocalProvider(LocalPodColors provides semanticColors) {
        MaterialExpressiveTheme(
            colorScheme = scheme,
            motionScheme = MotionScheme.expressive(),
            shapes = PodShapes,
            typography = PodTypography,
            content = content,
        )
    }
}
