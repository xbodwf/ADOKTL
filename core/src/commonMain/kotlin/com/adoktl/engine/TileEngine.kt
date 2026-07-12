package com.adoktl.engine

import com.adoktl.math.Vector2
import kotlin.math.*

class TileEngine(private val levelData: com.adoktl.level.LevelData) {

    var tileCount: Int = levelData.angleData.size
    var tilePositions: Array<Vector2> = emptyArray()
    var tileAngles: DoubleArray = doubleArrayOf()
    var tileDurations: DoubleArray = doubleArrayOf()
    var tileStartTimes: DoubleArray = doubleArrayOf()
    var tileBPMs: DoubleArray = doubleArrayOf()
    var tileIsCW: BooleanArray = booleanArrayOf()
    var tileExtraRotations: DoubleArray = doubleArrayOf()
    var tileStickToFloors: BooleanArray = booleanArrayOf()

    private val TILE_SIZE = 1.0

    fun initialize() {
        val n = tileCount
        tilePositions = Array(n) { Vector2.ZERO }
        tileAngles = DoubleArray(n) { 0.0 }
        tileDurations = DoubleArray(if (n > 0) n - 1 else 0) { 0.0 }
        tileStartTimes = DoubleArray(n) { 0.0 }
        tileBPMs = DoubleArray(n) { levelData.settings.bpm }
        tileIsCW = BooleanArray(n) { true }
        tileExtraRotations = DoubleArray(n) { 0.0 }
        tileStickToFloors = BooleanArray(n) { levelData.settings.stickToFloors }

        calculatePositions()
        calculateTiming()
    }

    private fun calculatePositions() {
        val angles = resolveAngles()
        if (angles.isEmpty()) return
        var pos = Vector2.ZERO
        tilePositions[0] = pos

        for (i in 0 until tileCount - 1) {
            val rad = angles[i] * PI / 180.0
            pos = Vector2(pos.x + cos(rad) * TILE_SIZE, pos.y + sin(rad) * TILE_SIZE)
            tilePositions[i + 1] = pos
        }
    }

    fun resolveAngles(): DoubleArray {
        val n = tileCount
        val result = DoubleArray(n) { 0.0 }
        var prev = 0.0
        for (i in 0 until n) {
            val raw = levelData.angleData.getOrNull(i) ?: (prev + 180.0)
            result[i] = if (raw == 999.0) prev + 180.0 else raw
            prev = result[i]
        }
        return result
    }

    private fun calculateTiming() {
        if (tileCount < 2) return
        var totalTime = 0.0
        var currentBPM = levelData.settings.bpm

        for (i in 0 until tileCount - 1) {
            tileBPMs[i] = currentBPM
            val duration = 60.0 / currentBPM
            tileDurations[i] = duration
            tileStartTimes[i + 1] = totalTime + duration
            totalTime += duration
        }

        if (tileCount > 1) {
            val shift = tileStartTimes[1]
            for (i in 0 until tileCount) {
                tileStartTimes[i] -= shift
            }
        }
    }

    fun getTilePosition(index: Int): Vector2 {
        if (index < 0 || index >= tileCount) return Vector2.ZERO
        return tilePositions[index]
    }

    fun getCurrentTileIndex(elapsedTime: Double): Int {
        for (i in tileStartTimes.indices.reversed()) {
            if (elapsedTime >= tileStartTimes[i]) return i
        }
        return 0
    }

    fun getTileProgress(elapsedTime: Double, tileIndex: Int): Double {
        if (tileIndex < 0 || tileIndex >= tileCount - 1) return 0.0
        val start = tileStartTimes[tileIndex]
        val end = tileStartTimes[tileIndex + 1]
        val duration = end - start
        if (duration <= 0.0) return 0.0
        return ((elapsedTime - start) / duration).coerceIn(0.0, 1.0)
    }

    fun calculatePlanetPosition(elapsedTime: Double): Vector2 {
        if (tileCount == 0) return Vector2.ZERO

        val tileIndex = getCurrentTileIndex(elapsedTime)
        val progress = getTileProgress(elapsedTime, tileIndex)

        if (tileIndex >= tileCount - 1) {
            return tilePositions[tileCount - 1]
        }

        val start = tilePositions[tileIndex]
        val end = tilePositions[tileIndex + 1]

        return Vector2(
            start.x + (end.x - start.x) * progress,
            start.y + (end.y - start.y) * progress
        )
    }
}