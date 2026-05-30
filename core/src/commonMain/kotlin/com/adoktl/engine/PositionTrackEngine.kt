package com.adoktl.engine

import com.adoktl.level.LevelData
import com.adoktl.math.Vector2
import com.adoktl.math.Vector3
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

data class TileTransform(
    val position: Vector3 = Vector3.ZERO,
    val rotation: Double = 0.0,
    val scale: Vector3 = Vector3(1.0, 1.0, 1.0),
    val opacity: Double = 1.0,
    val stickToFloors: Boolean = true
)

class PositionTrackEngine(private val levelData: LevelData) {

    private val events = mutableMapOf<Int, List<Map<String, Any?>>>()
    private val transforms = mutableMapOf<Int, TileTransform>()
    private val basePositions = mutableMapOf<Int, Vector2>()

    private val TILE_SIZE = 1.0

    fun parseEvents() {
        events.clear()
        for (action in levelData.actions) {
            if (action.eventType != "PositionTrack") continue
            val floor = action.floor
            events.getOrPut(floor) { mutableListOf() } + action.rawData
        }
    }

    private fun normalizeVec2(v: Any?): Vector2 {
        return when (v) {
            is List<*> -> Vector2(
                (v.getOrNull(0) as? Number)?.toDouble() ?: 0.0,
                (v.getOrNull(1) as? Number)?.toDouble() ?: 0.0
            )
            else -> Vector2.ZERO
        }
    }

    private fun idFromTile(relativeTo: List<*>, thisTileId: Int): Int {
        val offset = (relativeTo.getOrNull(0) as? Number)?.toInt() ?: 0
        val type = (relativeTo.getOrNull(1) as? String) ?: "ThisTile"
        val totalTiles = levelData.angleData.size
        return when (type) {
            "Start", "1" -> offset.coerceIn(0, totalTiles - 1)
            "End", "2" -> (totalTiles - 1 + offset).coerceIn(0, totalTiles - 1)
            else -> (thisTileId + offset).coerceIn(0, totalTiles - 1)
        }
    }

    fun calculateAllTransforms(isEditorMode: Boolean = false): Map<Int, TileTransform> {
        val n = levelData.angleData.size
        if (n == 0) return emptyMap()

        val positions = Array(n) { Vector2.ZERO }
        val rotations = DoubleArray(n) { 0.0 }
        val scales = DoubleArray(n) { 1.0 }
        val opacities = DoubleArray(n) { 1.0 }
        val sticks = BooleanArray(n) { levelData.settings.stickToFloors }

        val resolved = resolveAngles()
        var pos = Vector2.ZERO
        positions[0] = pos
        for (i in 0 until n - 1) {
            val rad = resolved[i] * PI / 180.0
            pos = Vector2(pos.x + cos(rad) * TILE_SIZE, pos.y + sin(rad) * TILE_SIZE)
            positions[i + 1] = pos
        }

        for (i in 0 until n) {
            basePositions[i] = positions[i]
        }

        var vector = Vector2.ZERO

        for (floor in 0 until n) {
            val floorEvents = events[floor] ?: continue
            for (event in floorEvents) {
                if (event["editorOnly"] == true && !isEditorMode) continue

                val disabled = event["disabled"] as? Map<String, Any?>
                val justThisTile = event["justThisTile"] == true

                if (disabled?.get("positionOffset") != true) {
                    var changeX = 0.0
                    var changeY = 0.0
                    var targetTileId = floor

                    val relativeTo = event["relativeTo"] as? List<*>
                    if (relativeTo != null) {
                        targetTileId = idFromTile(relativeTo, floor)
                    }

                    val positionOffset = event["positionOffset"]
                    if (positionOffset != null) {
                        val p = normalizeVec2(positionOffset)
                        changeX += p.x * TILE_SIZE
                        changeY += p.y * TILE_SIZE
                    }

                    if (targetTileId != floor && targetTileId < n) {
                        val basePos = basePositions[floor] ?: Vector2.ZERO
                        changeX += positions[targetTileId].x - (basePos.x + vector.x)
                        changeY += positions[targetTileId].y - (basePos.y + vector.y)
                    }

                    if (justThisTile) {
                        positions[floor] = Vector2(positions[floor].x + changeX, positions[floor].y + changeY)
                    } else {
                        for (j in floor until n) {
                            positions[j] = Vector2(positions[j].x + changeX, positions[j].y + changeY)
                        }
                        val basePos = basePositions[floor] ?: Vector2.ZERO
                        vector = Vector2(positions[floor].x - basePos.x, positions[floor].y - basePos.y)
                    }
                }

                val scale = event["scale"] as? Number
                if (scale != null && disabled?.get("scale") != true) {
                    val s = scale.toDouble() / 100.0
                    if (justThisTile) scales[floor] = s
                    else for (j in floor until n) scales[j] = s
                }

                val rotation = event["rotation"] as? Number
                if (rotation != null && disabled?.get("rotation") != true) {
                    val r = rotation.toDouble()
                    if (justThisTile) rotations[floor] = r
                    else for (j in floor until n) rotations[j] = r
                }

                val opacity = event["opacity"] as? Number
                if (opacity != null && disabled?.get("opacity") != true) {
                    val o = opacity.toDouble() / 100.0
                    if (justThisTile) opacities[floor] = o
                    else for (j in floor until n) opacities[j] = o
                }

                val stick = event["stickToFloors"]
                if (stick != null && disabled?.get("stickToFloors") != true) {
                    val s = when (stick) {
                        is Boolean -> stick
                        is String -> stick == "Enabled"
                        else -> levelData.settings.stickToFloors
                    }
                    if (justThisTile) sticks[floor] = s
                    else for (j in floor until n) sticks[j] = s
                }
            }
        }

        for (i in 0 until n) {
            transforms[i] = TileTransform(
                position = Vector3(positions[i].x, positions[i].y, 0.0),
                rotation = rotations[i],
                scale = Vector3(scales[i], scales[i], 1.0),
                opacity = opacities[i],
                stickToFloors = sticks[i]
            )
        }

        return transforms
    }

    private fun resolveAngles(): DoubleArray {
        val n = levelData.angleData.size
        val result = DoubleArray(n) { 0.0 }
        var prev = 0.0
        for (i in 0 until n) {
            val raw = levelData.angleData.getOrNull(i) ?: (prev + 180.0)
            result[i] = if (raw == 999.0) prev + 180.0 else raw
            prev = result[i]
        }
        return result
    }

    fun getTileTransform(index: Int): TileTransform? = transforms[index]
}