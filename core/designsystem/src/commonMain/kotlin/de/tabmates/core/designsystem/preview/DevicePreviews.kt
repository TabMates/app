package de.tabmates.core.designsystem.preview

import androidx.compose.ui.tooling.preview.Preview

private const val UI_MODE_NIGHT_NO = 0x10
private const val UI_MODE_NIGHT_YES = 0x20

/**
 * Multi-preview annotation that generates **Light** and **Dark** theme previews.
 *
 * Usage:
 * ```
 * @PreviewThemes
 * @Composable
 * fun MyScreenPreview() { TabMatesTheme { MyScreen() } }
 * ```
 */
@Preview(name = "Light Mode", uiMode = UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "Dark Mode", uiMode = UI_MODE_NIGHT_YES, showBackground = true)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
annotation class PreviewThemes

/**
 * Multi-preview annotation for **phone** form factors (portrait & landscape).
 */
@Preview(name = "Phone Portrait", widthDp = 360, heightDp = 640, showBackground = true)
@Preview(name = "Phone Landscape", widthDp = 640, heightDp = 360, showBackground = true)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
annotation class PreviewPhones

/**
 * Multi-preview annotation covering all major screen-size categories:
 * - Phone (portrait & landscape)
 * - Foldable (unfolded, portrait)
 * - Tablet (portrait & landscape)
 * - Desktop
 * - Web (common 1 366 × 768 viewport)
 */
@Preview(name = "Phone Portrait", widthDp = 360, heightDp = 640, showBackground = true)
@Preview(name = "Phone Landscape", widthDp = 640, heightDp = 360, showBackground = true)
@Preview(name = "Foldable", widthDp = 673, heightDp = 841, showBackground = true)
@Preview(name = "Tablet Portrait", widthDp = 800, heightDp = 1280, showBackground = true)
@Preview(name = "Tablet Landscape", widthDp = 1280, heightDp = 800, showBackground = true)
@Preview(name = "Desktop", widthDp = 1920, heightDp = 1080, showBackground = true)
@Preview(name = "Web", widthDp = 1366, heightDp = 768, showBackground = true)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
annotation class PreviewScreenSizes

/**
 * Comprehensive multi-preview that generates a preview for **every combination**
 * of theme (Light / Dark) and screen-size category.
 *
 * This produces **14 previews** — use it sparingly on heavyweight screens.
 */
@Preview(
    name = "Phone Portrait – Light",
    widthDp = 360,
    heightDp = 640,
    uiMode = UI_MODE_NIGHT_NO,
    showBackground = true,
)
@Preview(
    name = "Phone Portrait – Dark",
    widthDp = 360,
    heightDp = 640,
    uiMode = UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Preview(
    name = "Phone Landscape – Light",
    widthDp = 640,
    heightDp = 360,
    uiMode = UI_MODE_NIGHT_NO,
    showBackground = true,
)
@Preview(
    name = "Phone Landscape – Dark",
    widthDp = 640,
    heightDp = 360,
    uiMode = UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Preview(
    name = "Foldable – Light",
    widthDp = 673,
    heightDp = 841,
    uiMode = UI_MODE_NIGHT_NO,
    showBackground = true,
)
@Preview(
    name = "Foldable – Dark",
    widthDp = 673,
    heightDp = 841,
    uiMode = UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Preview(
    name = "Tablet Portrait – Light",
    widthDp = 800,
    heightDp = 1280,
    uiMode = UI_MODE_NIGHT_NO,
    showBackground = true,
)
@Preview(
    name = "Tablet Portrait – Dark",
    widthDp = 800,
    heightDp = 1280,
    uiMode = UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Preview(
    name = "Tablet Landscape – Light",
    widthDp = 1280,
    heightDp = 800,
    uiMode = UI_MODE_NIGHT_NO,
    showBackground = true,
)
@Preview(
    name = "Tablet Landscape – Dark",
    widthDp = 1280,
    heightDp = 800,
    uiMode = UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Preview(
    name = "Desktop – Light",
    widthDp = 1920,
    heightDp = 1080,
    uiMode = UI_MODE_NIGHT_NO,
    showBackground = true,
)
@Preview(
    name = "Desktop – Dark",
    widthDp = 1920,
    heightDp = 1080,
    uiMode = UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Preview(name = "Web – Light", widthDp = 1366, heightDp = 768, uiMode = UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "Web – Dark", widthDp = 1366, heightDp = 768, uiMode = UI_MODE_NIGHT_YES, showBackground = true)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
annotation class PreviewAll
