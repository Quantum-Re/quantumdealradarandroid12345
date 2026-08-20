package com.example.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.max
import kotlin.math.min

/**
 * D3-compatible Color Scales and Interpolation Engine for Geographic Overlays in Android Jetpack Compose.
 * Implements popular D3 chromatic continuous color maps (Turbo, Inferno, Viridis, Plasma, Spectral, YlOrRd, CoolWarm)
 * with precise piecewise RGB interpolation and multi-metric normalizers.
 */
enum class D3Palette(val displayName: String, val description: String, val category: String) {
    TURBO("D3 Turbo", "Spettro percettivo ad alto contrasto", "Rainbow / Multi-Hue"),
    INFERNO("D3 Inferno", "Nero -> Viola -> Rosso -> Arancio -> Giallo", "Sequential"),
    VIRIDIS("D3 Viridis", "Viola -> Blu -> Teal -> Verde -> Giallo", "Sequential (Standard)"),
    PLASMA("D3 Plasma", "Blu scuro -> Magenta -> Arancio -> Giallo", "Perceptual Sequential"),
    SPECTRAL("D3 Spectral", "Rosso -> Arancio -> Giallo -> Verde -> Blu", "Diverging"),
    YL_OR_RD("D3 YlOrRd", "Giallo -> Arancio -> Rosso Fuoco", "Heatmap / Risk"),
    COOL_WARM("D3 Cool-Warm", "Blu Ciano -> Neutro -> Rosso Acceso", "Diverging Polar")
}

enum class D3OverlayMetric(val displayName: String, val unit: String, val icon: String, val description: String) {
    PROPERTY_DENSITY("Densità Asset", "immobili", "🏢", "Concentrazione spaziale di immobili per km²"),
    DISTRESS_SEVERITY("Livello Distress", "indice", "🚨", "Gravità delle procedure esecutive, Aste e sofferenze NPL"),
    AVERAGE_YIELD("Rendimento Medio", "%", "📈", "Densità del rendimento lordo da locazione e Cap Rate per regione"),
    DISCOUNT_PERCENT("Sconto Medio", "%", "🏷️", "Percentuale media di sconto rispetto al valore OMI / perizia"),
    OPPORTUNITY_ALPHA("Alpha Opportunità", "score", "💎", "Punteggio composito: Alta concentrazione + Forte sconto + Resa")
}

object D3ColorScale {

    // Piecewise stops for D3 Turbo (Google Turbo colormap)
    private val TURBO_STOPS = listOf(
        0.00f to Color(0xFF30123B),
        0.10f to Color(0xFF4145AB),
        0.20f to Color(0xFF4675ED),
        0.30f to Color(0xFF39A2FC),
        0.40f to Color(0xFF1BCECE),
        0.50f to Color(0xFF24EC85),
        0.60f to Color(0xFF6DEB35),
        0.70f to Color(0xFFB1DD2F),
        0.80f to Color(0xFFE9B32B),
        0.90f to Color(0xFFF76B15),
        1.00f to Color(0xFFD9280B)
    )

    // Piecewise stops for D3 Inferno
    private val INFERNO_STOPS = listOf(
        0.00f to Color(0xFF000004),
        0.12f to Color(0xFF1B0C41),
        0.25f to Color(0xFF4A0C6B),
        0.38f to Color(0xFF781C6D),
        0.50f to Color(0xFFA52C60),
        0.62f to Color(0xFFCF4446),
        0.75f to Color(0xFFED6925),
        0.88f to Color(0xFFFB9B06),
        1.00f to Color(0xFFFCFFA4)
    )

    // Piecewise stops for D3 Viridis
    private val VIRIDIS_STOPS = listOf(
        0.00f to Color(0xFF440154),
        0.15f to Color(0xFF482878),
        0.30f to Color(0xFF3E4A89),
        0.45f to Color(0xFF31688E),
        0.60f to Color(0xFF26828E),
        0.75f to Color(0xFF1F9E89),
        0.88f to Color(0xFF6CCE59),
        1.00f to Color(0xFFFDE725)
    )

    // Piecewise stops for D3 Plasma
    private val PLASMA_STOPS = listOf(
        0.00f to Color(0xFF0D0887),
        0.15f to Color(0xFF46039F),
        0.30f to Color(0xFF7201A8),
        0.45f to Color(0xFF9C179E),
        0.60f to Color(0xFFBD3786),
        0.75f to Color(0xFFD8576B),
        0.88f to Color(0xFFED7953),
        1.00f to Color(0xFFF0F921)
    )

    // Piecewise stops for D3 Spectral (Diverging)
    private val SPECTRAL_STOPS = listOf(
        0.00f to Color(0xFF9E0142),
        0.17f to Color(0xFFD53E4F),
        0.33f to Color(0xFFFEE08B),
        0.50f to Color(0xFFFFFFBF),
        0.67f to Color(0xFFE6F598),
        0.83f to Color(0xFF66C2A5),
        1.00f to Color(0xFF5E4FA2)
    )

    // Piecewise stops for D3 YlOrRd
    private val YL_OR_RD_STOPS = listOf(
        0.00f to Color(0xFFFFFFCC),
        0.20f to Color(0xFFFFEDA0),
        0.40f to Color(0xFFFED976),
        0.60f to Color(0xFFFEB24C),
        0.75f to Color(0xFFFD8D3C),
        0.90f to Color(0xFFF03B20),
        1.00f to Color(0xFFBD0026)
    )

    // Piecewise stops for D3 Cool-Warm
    private val COOL_WARM_STOPS = listOf(
        0.00f to Color(0xFF3B4CC0),
        0.25f to Color(0xFF8CBDFF),
        0.50f to Color(0xFFE2E2E2),
        0.75f to Color(0xFFF49A7B),
        1.00f to Color(0xFFB40426)
    )

    /**
     * Interpolates color along a normalized scale [0.0f, 1.0f] for a specified D3 palette.
     */
    fun interpolate(t: Float, palette: D3Palette, alpha: Float = 1.0f): Color {
        val clampedT = t.coerceIn(0.0f, 1.0f)
        val stops = when (palette) {
            D3Palette.TURBO -> TURBO_STOPS
            D3Palette.INFERNO -> INFERNO_STOPS
            D3Palette.VIRIDIS -> VIRIDIS_STOPS
            D3Palette.PLASMA -> PLASMA_STOPS
            D3Palette.SPECTRAL -> SPECTRAL_STOPS
            D3Palette.YL_OR_RD -> YL_OR_RD_STOPS
            D3Palette.COOL_WARM -> COOL_WARM_STOPS
        }

        val baseColor = interpolateStops(clampedT, stops)
        return if (alpha < 1.0f) baseColor.copy(alpha = alpha.coerceIn(0.0f, 1.0f)) else baseColor
    }

    /**
     * Scales an arbitrary continuous value to [0.0, 1.0] and returns the corresponding D3 color.
     */
    fun scaleLinear(
        value: Float,
        domainMin: Float,
        domainMax: Float,
        palette: D3Palette,
        alpha: Float = 1.0f
    ): Color {
        val range = domainMax - domainMin
        val normalized = if (range <= 0.0001f) 0.5f else ((value - domainMin) / range).coerceIn(0.0f, 1.0f)
        return interpolate(normalized, palette, alpha)
    }

    /**
     * Generates a sample list of colors to draw continuous gradient previews in Compose.
     */
    fun getGradientColors(palette: D3Palette, steps: Int = 10): List<Color> {
        val list = mutableListOf<Color>()
        val count = max(2, steps)
        for (i in 0 until count) {
            val t = i.toFloat() / (count - 1).toFloat()
            list.add(interpolate(t, palette))
        }
        return list
    }

    private fun interpolateStops(t: Float, stops: List<Pair<Float, Color>>): Color {
        if (stops.isEmpty()) return Color.White
        if (t <= stops.first().first) return stops.first().second
        if (t >= stops.last().first) return stops.last().second

        for (i in 0 until stops.size - 1) {
            val (t0, c0) = stops[i]
            val (t1, c1) = stops[i + 1]
            if (t in t0..t1) {
                val localT = ((t - t0) / (t1 - t0)).coerceIn(0.0f, 1.0f)
                return lerpColor(c0, c1, localT)
            }
        }
        return stops.last().second
    }

    private fun lerpColor(c0: Color, c1: Color, t: Float): Color {
        val r = c0.red + (c1.red - c0.red) * t
        val g = c0.green + (c1.green - c0.green) * t
        val b = c0.blue + (c1.blue - c0.blue) * t
        val a = c0.alpha + (c1.alpha - c0.alpha) * t
        return Color(r, g, b, a)
    }
}
