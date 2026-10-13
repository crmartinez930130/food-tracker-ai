package com.foodtracker.app.ui.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star

/**
 * The expressive shapes the app uses, in the spirit of Material 3 Expressive's MaterialShapes
 * (internal in Compose Multiplatform's stable material3, so built here on graphics-shapes).
 * All are centered on (0, 0) with radius 1; [toComposePath] fits them into any size.
 */
object ExpressiveShapes {
    /** Scalloped "cookie", like Greenwave's score badge. */
    val cookie9: RoundedPolygon = RoundedPolygon.star(
        numVerticesPerRadius = 9,
        innerRadius = 0.82f,
        rounding = CornerRounding(radius = 0.28f),
        innerRounding = CornerRounding(radius = 0.28f),
    )

    /** Soft eight-pointed sun. */
    val sunny: RoundedPolygon = RoundedPolygon.star(
        numVerticesPerRadius = 8,
        innerRadius = 0.78f,
        rounding = CornerRounding(radius = 0.15f),
    )

    /** Ten-pointed burst with soft tips. */
    val softBurst: RoundedPolygon = RoundedPolygon.star(
        numVerticesPerRadius = 10,
        innerRadius = 0.7f,
        rounding = CornerRounding(radius = 0.1f),
        innerRounding = CornerRounding(radius = 0.1f),
    )

    /** Rounded pentagon. */
    val pentagon: RoundedPolygon = RoundedPolygon(numVertices = 5, rounding = CornerRounding(radius = 0.3f))

    val circle: RoundedPolygon = RoundedPolygon.circle(numVertices = 12)

    /** The loading indicator cycles through these, in order. */
    val loadingSequence: List<RoundedPolygon> = listOf(softBurst, cookie9, pentagon, sunny)
}

/**
 * Bounds of [cubics] as (left, top, right, bottom). Uses every control point, which is a safe
 * (slightly generous) box for cubic curves.
 */
internal fun cubicsBounds(cubics: List<Cubic>): FloatArray {
    var left = Float.POSITIVE_INFINITY
    var top = Float.POSITIVE_INFINITY
    var right = Float.NEGATIVE_INFINITY
    var bottom = Float.NEGATIVE_INFINITY
    for (cubic in cubics) {
        val xs = floatArrayOf(cubic.anchor0X, cubic.control0X, cubic.control1X, cubic.anchor1X)
        val ys = floatArrayOf(cubic.anchor0Y, cubic.control0Y, cubic.control1Y, cubic.anchor1Y)
        xs.forEach { left = minOf(left, it); right = maxOf(right, it) }
        ys.forEach { top = minOf(top, it); bottom = maxOf(bottom, it) }
    }
    return floatArrayOf(left, top, right, bottom)
}

/** Uniform scale plus offset that maps shape coordinates into a target size. */
data class FitTransform(val scale: Float, val offsetX: Float, val offsetY: Float) {
    fun x(value: Float) = value * scale + offsetX
    fun y(value: Float) = value * scale + offsetY
}

/**
 * Scales [bounds] (left, top, right, bottom) uniformly - keeping proportions - to fit [size], and
 * centers it. Every point inside the bounds ends inside 0..width × 0..height.
 */
fun fitTransform(bounds: FloatArray, size: Size): FitTransform {
    val (left, top, right, bottom) = listOf(bounds[0], bounds[1], bounds[2], bounds[3])
    val scale = minOf(size.width / (right - left), size.height / (bottom - top))
    return FitTransform(
        scale = scale,
        offsetX = (size.width - (right - left) * scale) / 2f - left * scale,
        offsetY = (size.height - (bottom - top) * scale) / 2f - top * scale,
    )
}

/** Draws [cubics] fitted into [size] (see [fitTransform]). */
fun cubicsToPath(cubics: List<Cubic>, size: Size, path: Path = Path()): Path {
    path.reset()
    if (cubics.isEmpty() || size.width <= 0f || size.height <= 0f) return path
    val fit = fitTransform(cubicsBounds(cubics), size)
    path.moveTo(fit.x(cubics.first().anchor0X), fit.y(cubics.first().anchor0Y))
    for (c in cubics) {
        path.cubicTo(fit.x(c.control0X), fit.y(c.control0Y), fit.x(c.control1X), fit.y(c.control1Y), fit.x(c.anchor1X), fit.y(c.anchor1Y))
    }
    path.close()
    return path
}

fun RoundedPolygon.toComposePath(size: Size, path: Path = Path()): Path = cubicsToPath(cubics, size, path)

/** A [Shape] (for clip, background, border) from a polygon, fitted to the component's size. */
class PolygonShape(private val polygon: RoundedPolygon) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(polygon.toComposePath(size))
}
