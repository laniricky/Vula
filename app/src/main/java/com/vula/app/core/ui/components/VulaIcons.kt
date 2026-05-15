package com.vula.app.core.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Vula custom icon set — bespoke vector paths used in the PostCard stats pill
 * and other branded surfaces.
 *
 * IMPORTANT: fill/stroke must use SolidColor(Color.Black) so that the
 * Icon composable can tint them correctly via ColorFilter.
 */
object VulaIcons {

    // ── VulaPulse — filled heart (likes) ─────────────────────────────────────
    val VulaPulse: ImageVector by lazy {
        ImageVector.Builder(
            name           = "VulaPulse",
            defaultWidth   = 24.dp,
            defaultHeight  = 24.dp,
            viewportWidth  = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20.84f, 4.61f)
                curveTo(20.33f, 4.1f, 19.72f, 3.7f, 19.05f, 3.43f)
                curveTo(18.38f, 3.15f, 17.67f, 3.01f, 16.95f, 3.01f)
                curveTo(16.23f, 3.01f, 15.52f, 3.15f, 14.85f, 3.43f)
                curveTo(14.18f, 3.7f, 13.57f, 4.1f, 13.06f, 4.61f)
                lineTo(12f, 5.67f)
                lineTo(10.94f, 4.61f)
                curveTo(9.91f, 3.57f, 8.52f, 2.99f, 7.07f, 2.99f)
                curveTo(5.62f, 2.99f, 4.23f, 3.57f, 3.2f, 4.61f)
                curveTo(2.16f, 5.64f, 1.58f, 7.03f, 1.58f, 8.48f)
                curveTo(1.58f, 9.93f, 2.16f, 11.32f, 3.2f, 12.35f)
                lineTo(12f, 21.15f)
                lineTo(20.8f, 12.35f)
                curveTo(21.84f, 11.32f, 22.42f, 9.93f, 22.42f, 8.48f)
                curveTo(22.42f, 7.03f, 21.84f, 5.64f, 20.84f, 4.61f)
                close()
            }
        }.build()
    }

    // ── VulaWave — speech bubble with wave tail (comments) ───────────────────
    val VulaWave: ImageVector by lazy {
        ImageVector.Builder(
            name           = "VulaWave",
            defaultWidth   = 24.dp,
            defaultHeight  = 24.dp,
            viewportWidth  = 24f,
            viewportHeight = 24f
        ).apply {
            // Bubble body (filled)
            path(fill = SolidColor(Color.Black)) {
                moveTo(20f, 2f)
                lineTo(4f, 2f)
                curveTo(2.9f, 2f, 2f, 2.9f, 2f, 4f)
                lineTo(2f, 15f)
                curveTo(2f, 16.1f, 2.9f, 17f, 4f, 17f)
                lineTo(7.5f, 17f)
                curveTo(8f, 17f, 8.5f, 17.5f, 9.2f, 18.3f)
                curveTo(9.9f, 19.1f, 10.5f, 19.8f, 11f, 20.5f)
                curveTo(11.3f, 20.9f, 11.6f, 21.2f, 12f, 21.2f)
                curveTo(12.4f, 21.2f, 12.6f, 20.8f, 12.8f, 19.8f)
                curveTo(13f, 18.8f, 13.3f, 17.7f, 14f, 17f)
                lineTo(20f, 17f)
                curveTo(21.1f, 17f, 22f, 16.1f, 22f, 15f)
                lineTo(22f, 4f)
                curveTo(22f, 2.9f, 21.1f, 2f, 20f, 2f)
                close()
            }
            // 3 dots cut-out (white background colour so they punch through)
            path(fill = SolidColor(Color(0xFFFFFFFF))) {
                // left dot
                moveTo(7.5f, 10.5f)
                curveTo(7.5f, 11.05f, 7.05f, 11.5f, 6.5f, 11.5f)
                curveTo(5.95f, 11.5f, 5.5f, 11.05f, 5.5f, 10.5f)
                curveTo(5.5f, 9.95f, 5.95f, 9.5f, 6.5f, 9.5f)
                curveTo(7.05f, 9.5f, 7.5f, 9.95f, 7.5f, 10.5f)
                close()
                // middle dot
                moveTo(13f, 10.5f)
                curveTo(13f, 11.05f, 12.55f, 11.5f, 12f, 11.5f)
                curveTo(11.45f, 11.5f, 11f, 11.05f, 11f, 10.5f)
                curveTo(11f, 9.95f, 11.45f, 9.5f, 12f, 9.5f)
                curveTo(12.55f, 9.5f, 13f, 9.95f, 13f, 10.5f)
                close()
                // right dot
                moveTo(18.5f, 10.5f)
                curveTo(18.5f, 11.05f, 18.05f, 11.5f, 17.5f, 11.5f)
                curveTo(16.95f, 11.5f, 16.5f, 11.05f, 16.5f, 10.5f)
                curveTo(16.5f, 9.95f, 16.95f, 9.5f, 17.5f, 9.5f)
                curveTo(18.05f, 9.5f, 18.5f, 9.95f, 18.5f, 10.5f)
                close()
            }
        }.build()
    }

    // ── VulaCycle — two-arrow loop (reposts) ─────────────────────────────────
    val VulaCycle: ImageVector by lazy {
        ImageVector.Builder(
            name           = "VulaCycle",
            defaultWidth   = 24.dp,
            defaultHeight  = 24.dp,
            viewportWidth  = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill            = SolidColor(Color.Black),
                stroke          = null,
                strokeLineWidth = 0f
            ) {
                // Top-right arrow (pointing right → down)
                moveTo(17f, 1f)
                lineTo(13f, 5f)
                lineTo(15f, 5f)
                curveTo(15f, 8.31f, 12.31f, 11f, 9f, 11f)
                curveTo(7.99f, 11f, 7.03f, 10.75f, 6.2f, 10.3f)
                lineTo(4.73f, 11.77f)
                curveTo(5.97f, 12.57f, 7.43f, 13f, 9f, 13f)
                curveTo(13.42f, 13f, 17f, 9.42f, 17f, 5f)
                lineTo(19f, 5f)
                close()
                // Bottom-left arrow (pointing left → up)
                moveTo(7f, 23f)
                lineTo(11f, 19f)
                lineTo(9f, 19f)
                curveTo(9f, 15.69f, 11.69f, 13f, 15f, 13f)
                curveTo(16.01f, 13f, 16.97f, 13.25f, 17.8f, 13.7f)
                lineTo(19.27f, 12.23f)
                curveTo(18.03f, 11.43f, 16.57f, 11f, 15f, 11f)
                curveTo(10.58f, 11f, 7f, 14.58f, 7f, 19f)
                lineTo(5f, 19f)
                close()
            }
        }.build()
    }
}
