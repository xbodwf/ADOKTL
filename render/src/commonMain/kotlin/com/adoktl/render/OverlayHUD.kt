package com.adoktl.render

import com.adoktl.math.AdoktlColor
import kotlin.math.pow

/**
 * 2D overlay HUD for debugging/stats display.
 * Port of OverlayHUD.ts from Re_ADOJAS.
 */
class OverlayHUD {
    private var _visible: Boolean = true

    private var fps: Double = 0.0
    private var currentTime: Double = 0.0
    private var tileIndex: Int = 0
    private var totalTiles: Int = 0
    private var tileBPM: DoubleArray = doubleArrayOf()
    private var tileStartTimes: DoubleArray = doubleArrayOf()

    var visible: Boolean
        get() = _visible
        set(value) { _visible = value }

    fun update(stats: HUDSData) {
        fps = stats.fps
        currentTime = stats.time
        tileIndex = stats.tileIndex
        totalTiles = stats.totalTiles
        tileBPM = stats.tileBPM
        tileStartTimes = stats.tileStartTimes
    }

    fun render(backend: RenderBackendApi, width: Int, height: Int) {
        if (!_visible) return

        val textLines = buildTextLines()
        val fpsLine = "FPS  ${fmt(fps, 2)}"

        val allLines = listOf(fpsLine) + textLines
        val lineHeight = 18f
        val padding = 8f
        val fontSize = 13f
        val boxWidth = 200f
        val fpsBoxHeight = lineHeight + padding * 2
        val panelBoxHeight = allLines.size * lineHeight + padding * 2
        val panelY = 64f

        val fpsVerts = buildTextBackgroundQuad(
            x = 16f, y = panelY,
            w = boxWidth, h = fpsBoxHeight,
            color = AdoktlColor(0f, 0f, 0f, 0.5f)
        )
        backend.drawMesh(fpsVerts)

        val panelX = width - boxWidth - 16f
        val panelVerts = buildTextBackgroundQuad(
            x = panelX, y = panelY,
            w = boxWidth, h = panelBoxHeight,
            color = AdoktlColor(0f, 0f, 0f, 0.5f)
        )
        backend.drawMesh(panelVerts)
    }

    private fun buildTextLines(): List<String> {
        val tbpm = tileBPM.getOrElse(tileIndex) { 0.0 }
        var cbpm = tbpm
        if (tileIndex in 0 until totalTiles - 1 && tileStartTimes.size > tileIndex + 1) {
            val tCurrent = tileStartTimes.getOrElse(tileIndex) { 0.0 }
            val tNext = tileStartTimes.getOrElse(tileIndex + 1) { 0.0 }
            val dt = tNext - tCurrent
            if (dt > 0.0) cbpm = 60.0 / dt
        }

        val safeTile = (tileIndex + 1).coerceAtMost(totalTiles)
        val pct = if (totalTiles > 0) (safeTile.toDouble() / totalTiles) * 100.0 else 0.0

        return listOf(
            "TBPM | ${fmt(tbpm, 2)}",
            "CBPM | ${fmt(cbpm, 2)}",
            "Map Time | ${formatTime(currentTime)}",
            "Tiles | $safeTile / $totalTiles (${fmt(pct, 1)}%)"
        )
    }

    private fun buildTextBackgroundQuad(
        x: Float, y: Float,
        w: Float, h: Float,
        color: AdoktlColor
    ): Mesh {
        val verts = floatArrayOf(
            x, y, 0f, color.r, color.g, color.b, color.a, 0f, 0f,
            x + w, y, 0f, color.r, color.g, color.b, color.a, 1f, 0f,
            x, y + h, 0f, color.r, color.g, color.b, color.a, 0f, 1f,
            x + w, y + h, 0f, color.r, color.g, color.b, color.a, 1f, 1f
        )
        val idx = intArrayOf(0, 1, 2, 1, 3, 2)
        return Mesh(verts, idx)
    }

    private fun formatTime(seconds: Double): String {
        if (seconds.isNaN() || seconds < 0) return "0:00.0"
        val m = (seconds / 60).toInt()
        val sFloat = seconds % 60
        val s = sFloat.toInt()
        val d = ((sFloat - s) * 10).toInt()
        return "$m:${s.toString().padStart(2, '0')}.$d"
    }

    private fun fmt(value: Double, decimals: Int): String {
        val factor = 10.0.pow(decimals)
        val whole = value.toInt()
        val frac = ((value - whole) * factor).toInt()
        return "$whole.${frac.toString().padStart(decimals, '0')}"
    }
}

data class HUDSData(
    val fps: Double,
    val time: Double,
    val tileIndex: Int,
    val tileBPM: DoubleArray,
    val tileStartTimes: DoubleArray,
    val totalTiles: Int
)