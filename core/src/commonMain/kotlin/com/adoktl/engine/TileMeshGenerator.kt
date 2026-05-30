package com.adoktl.engine

import com.adoktl.math.AdoktlColor
import kotlin.math.*

data class MeshData(
    val vertices: FloatArray,
    val indices: IntArray,
    val colors: FloatArray
)

class TileMeshGenerator {

    companion object {
        private const val TILE_WIDTH = 0.275
        private const val TILE_LENGTH = 0.5
        private const val OUTLINE = 0.025

        fun generateTileMesh(
            previousAngle: Double,
            currentAngle: Double,
            is999: Boolean,
            trackStyle: String = "Standard",
            color: AdoktlColor = AdoktlColor.WHITE,
            bgColor: AdoktlColor = AdoktlColor(0.3f, 0.3f, 0.3f)
        ): MeshData {
            return when (trackStyle) {
                "Gems" -> generateGemsMesh(previousAngle, currentAngle, TILE_LENGTH, TILE_WIDTH, OUTLINE)
                else -> generateStandardMesh(previousAngle, currentAngle, is999, color, bgColor)
            }
        }

        private fun generateStandardMesh(
            ang1: Double,
            ang2: Double,
            is999: Boolean,
            color: AdoktlColor,
            bgColor: AdoktlColor
        ): MeshData {
            val vertices = mutableListOf<Float>()
            val indices = mutableListOf<Int>()
            val colors = mutableListOf<Float>()

            val points = calculatePoints(-ang1, -ang2, TILE_WIDTH, TILE_LENGTH, if (is999) 1.0 else 0.0)

            if (points.size < 3) return MeshData(floatArrayOf(), intArrayOf(), floatArrayOf())

            val startIdx = 0
            for ((x, y) in points) {
                vertices.add(x.toFloat())
                vertices.add((-y).toFloat())
                vertices.add(0f)
                colors.add(color.r)
                colors.add(color.g)
                colors.add(color.b)
            }

            for (i in 1 until points.size - 1) {
                indices.add(startIdx)
                indices.add(startIdx + i)
                indices.add(startIdx + i + 1)
            }

            return MeshData(
                vertices.toFloatArray(),
                indices.toIntArray(),
                colors.toFloatArray()
            )
        }

        fun calculatePoints(
            ang1: Double,
            ang2: Double,
            wid: Double,
            len: Double,
            mr: Double
        ): List<Pair<Double, Double>> {
            val a1 = -ang1
            val a2 = -ang2
            val alpha = min(fmod(a1 - a2, 360.0), fmod(a2 - a1, 360.0))

            val a = mutableListOf<Double>()
            if (fmod(a1 - a2, 360.0) > fmod(a2 - a1, 360.0)) {
                a.add(fmod(a1, 360.0))
                a.add(fmod(a2, 360.0))
            } else {
                a.add(fmod(a2, 360.0))
                a.add(fmod(a1, 360.0))
            }

            val m = a[0] + alpha / 2.0
            val x0 = -f(alpha, wid) * cos(d2r(m))
            val y0 = -f(alpha, wid) * sin(d2r(m))
            val r0 = abs(f(alpha, wid) * sin(d2r(alpha / 2.0)) - wid)

            val pts = mutableListOf<Pair<Double, Double>>()

            pts.add(Pair(-r0 * sin(d2r(a[1])) + x0, r0 * cos(d2r(a[1])) + y0))
            pts.add(Pair(-wid * sin(d2r(a[1])) + len * cos(d2r(a[1])), wid * cos(d2r(a[1])) + len * sin(d2r(a[1]))))

            if (mr == 0.0) {
                if (2.0 * atan(wid / len) < d2r(alpha)) {
                    pts.add(Pair(wid * sin(d2r(a[1])) + len * cos(d2r(a[1])), -wid * cos(d2r(a[1])) + len * sin(d2r(a[1]))))
                    pts.add(Pair((wid / sin(d2r(alpha / 2.0))) * cos(d2r(m)), (wid / sin(d2r(alpha / 2.0))) * sin(d2r(m))))
                    pts.add(Pair(-wid * sin(d2r(a[0])) + len * cos(d2r(a[0])), wid * cos(d2r(a[0])) + len * sin(d2r(a[0]))))
                } else {
                    pts.add(Pair(
                        (len * cos(d2r(a[0])) - wid * sin(d2r(a[1]))) / cos(d2r(alpha)),
                        (len * sin(d2r(a[0])) + wid * cos(d2r(a[1]))) / cos(d2r(alpha))
                    ))
                    pts.add(Pair(-wid * sin(d2r(a[0])) + len * cos(d2r(a[0])), wid * cos(d2r(a[0])) + len * sin(d2r(a[0]))))
                    pts.add(Pair(
                        (len * cos(d2r(m))) / cos(d2r(alpha / 2.0)),
                        (len * sin(d2r(m))) / cos(d2r(alpha / 2.0))
                    ))
                    pts.add(Pair(wid * sin(d2r(a[1])) + len * cos(d2r(a[1])), -wid * cos(d2r(a[1])) + len * sin(d2r(a[1]))))
                    pts.add(Pair(
                        (len * cos(d2r(a[1])) + wid * sin(d2r(a[0]))) / cos(d2r(alpha)),
                        (len * sin(d2r(a[1])) - wid * cos(d2r(a[0]))) / cos(d2r(alpha))
                    ))
                }
                pts.add(Pair(wid * sin(d2r(a[0])) + len * cos(d2r(a[0])), -wid * cos(d2r(a[0])) + len * sin(d2r(a[0]))))
                pts.add(Pair(r0 * sin(d2r(a[0])) + x0, -r0 * cos(d2r(a[0])) + y0))

                return generateArcPoints(pts, x0, y0, r0, alpha, a1, a2, ang1, ang2)
            } else {
                pts.add(Pair(wid * sin(d2r(a[1])) + len * cos(d2r(a[1])), -wid * cos(d2r(a[1])) + len * sin(d2r(a[1]))))
                pts.add(Pair(r0 * sin(d2r(a[0])) + x0, -r0 * cos(d2r(a[0])) + y0))
                pts.add(Pair(x0 + r0 * cos(d2r(a1 + 180.0)), y0 + r0 * sin(d2r(a1 + 180.0))))
                return pts
            }
        }

        private fun generateArcPoints(
            pts: MutableList<Pair<Double, Double>>,
            x0: Double, y0: Double, r0: Double,
            alpha: Double, a1: Double, a2: Double,
            ang1: Double, ang2: Double
        ): List<Pair<Double, Double>> {
            val lastPt = pts.last()
            val startDist = sqrt(
                (x0 + r0 * cos(d2r(ang1)) - lastPt.first).pow(2) +
                (y0 + r0 * sin(d2r(ang1)) - lastPt.second).pow(2)
            )

            val dir: Double
            val startAngle: Double
            if (r0 < startDist) {
                dir = if (fmod(a1, 360.0) - fmod(a2, 360.0) > 180.0) 1.0 else -1.0
                startAngle = if (fmod(a1, 360.0) - fmod(a2, 360.0) > 180.0) a2 + 90.0 else a2 - 90.0
            } else {
                dir = if (fmod(a2, 360.0) - fmod(a1, 360.0) > 180.0) 1.0 else -1.0
                startAngle = if (fmod(a2, 360.0) - fmod(a1, 360.0) > 180.0) a1 + 90.0 else a1 - 90.0
            }

            val arcAngle = 180.0 - alpha
            val acc = 6.0
            val steps = floor(arcAngle / acc).toInt()

            var angle = startAngle
            for (i in 0 until steps - 1) {
                pts.add(Pair(x0 + r0 * cos(d2r(angle)), y0 + r0 * sin(d2r(angle))))
                angle += dir * acc
            }
            pts.add(Pair(x0 + r0 * cos(d2r(startAngle + dir * arcAngle)), y0 + r0 * sin(d2r(startAngle + dir * arcAngle))))

            return pts
        }

        private fun generateGemsMesh(
            startAngle: Double,
            endAngle: Double,
            length: Double,
            width: Double,
            outline: Double
        ): MeshData {
            val vertices = mutableListOf<Float>()
            val indices = mutableListOf<Int>()
            val colors = mutableListOf<Float>()

            val m11 = cos(d2r(startAngle))
            val m12 = sin(d2r(startAngle))
            val m21 = cos(d2r(endAngle))
            val m22 = sin(d2r(endAngle))

            val hexRadius = width
            val hexOutlineRadius = hexRadius + outline

            val outerHexStart = vertices.size / 3
            for (i in 0 until 6) {
                val a = (PI / 3.0) * i
                vertices.add((cos(a) * hexOutlineRadius).toFloat())
                vertices.add((sin(a) * hexOutlineRadius).toFloat())
                vertices.add(0f)
                colors.addAll(listOf(0f, 0f, 0f))
            }
            for (i in 0 until 6) {
                val next = (i + 1) % 6
                indices.addAll(listOf(outerHexStart, outerHexStart + i, outerHexStart + next))
            }

            val innerHexStart = vertices.size / 3
            val hexInnerRadius = hexRadius - outline
            for (i in 0 until 6) {
                val a = (PI / 3.0) * i
                vertices.add((cos(a) * hexInnerRadius).toFloat())
                vertices.add((sin(a) * hexInnerRadius).toFloat())
                vertices.add(0f)
                colors.addAll(listOf(1f, 1f, 1f))
            }
            for (i in 0 until 6) {
                val next = (i + 1) % 6
                indices.addAll(listOf(innerHexStart, innerHexStart + i, innerHexStart + next))
            }

            val capStart = vertices.size / 3
            vertices.addAll(listOf(
                (length * m11 + width * m12).toFloat(), (length * m12 - width * m11).toFloat(), 0f,
                (length * m11 - width * m12).toFloat(), (length * m12 + width * m11).toFloat(), 0f,
                (-width * m12).toFloat(), (width * m11).toFloat(), 0f,
                (width * m12).toFloat(), (-width * m11).toFloat(), 0f,
                (length * m21 + width * m22).toFloat(), (length * m22 - width * m21).toFloat(), 0f,
                (length * m21 - width * m22).toFloat(), (length * m22 + width * m21).toFloat(), 0f,
                (-width * m22).toFloat(), (width * m21).toFloat(), 0f,
                (width * m22).toFloat(), (-width * m21).toFloat(), 0f
            ))
            repeat(8) { colors.addAll(listOf(0f, 0f, 0f)) }

            indices.addAll(listOf(
                capStart, capStart + 1, capStart + 2,
                capStart + 2, capStart + 3, capStart,
                capStart + 4, capStart + 5, capStart + 6,
                capStart + 6, capStart + 7, capStart + 4
            ))

            return MeshData(vertices.toFloatArray(), indices.toIntArray(), colors.toFloatArray())
        }

        private fun f(x: Double, w: Double): Double {
            if (x <= 5.0) return 0.0
            return -1.0 * (lerp(0.0, w, q(x)) - w) / sin(d2r(x / 2.0))
        }

        private fun q(x: Double): Double {
            return when {
                x >= 0 && x <= 5 -> 1.0
                x > 5 && x <= 30 -> lerp(1.0, 0.83, sqrt((x - 5.0) / 25.0))
                x > 30 && x <= 45 -> lerp(0.83, 0.77, (x - 30.0) / 15.0)
                x > 45 && x <= 90 -> lerp(0.77, 0.15, ((x - 45.0) / 45.0).pow(0.7))
                x > 90 && x <= 120 -> lerp(0.15, 0.0, sqrt((x - 90.0) / 45.0))
                else -> 0.0
            }
        }

        private fun lerp(a: Double, b: Double, t: Double): Double = a + (b - a) * t
        private fun d2r(deg: Double): Double = deg * PI / 180.0
        private fun fmod(a: Double, b: Double): Double = a - b * floor(a / b)

        private infix fun Double.pow(exp: Double): Double = when {
            exp == 2.0 -> this * this
            else -> kotlin.math.exp(exp * kotlin.math.ln(this))
        }
    }
}