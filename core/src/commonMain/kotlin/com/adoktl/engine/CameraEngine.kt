package com.adoktl.engine

import com.adoktl.math.Vector2
import com.adoktl.math.EasingFunctions

data class CameraState(
    var relativeTo: String = "Player",
    var anchorTileIndex: Int = 0,
    var position: Vector2 = Vector2.ZERO,
    var zoom: Double = 100.0,
    var rotation: Double = 0.0,
    var angleOffset: Double = 0.0,
    var lastEventRelativePosition: Vector2 = Vector2.ZERO,
    var lastUsedMovementType: String = "Player",
    var lastTileCamFloor: Int = -1,
    var followMode: Boolean = true
)

data class PropertyTransition<T>(
    var active: Boolean = false,
    var startTime: Double = 0.0,
    var duration: Double = 0.0,
    var startValue: T? = null,
    var endValue: T? = null,
    var ease: String = "Linear"
)

data class CameraTimelineEntry(
    val time: Double,
    val event: Map<String, Any?>,
    val floor: Int
)

data class CameraInterpolated(
    val x: Double,
    val y: Double,
    val zoom: Double,
    val rotation: Double
)

class CameraEngine(
    private val levelData: com.adoktl.level.LevelData
) {
    private val state = CameraState()
    private var posXTween = PropertyTransition<Double>()
    private var posYTween = PropertyTransition<Double>()
    private var rotTween = PropertyTransition<Double>()
    private var zoomTween = PropertyTransition<Double>()
    private var timeline = mutableListOf<CameraTimelineEntry>()
    private var lastTimelineIndex = -1

    fun resetState() {
        val s = levelData.settings
        val rt = EventUtils.getMovementType(s.relativeTo)
        state.apply {
            relativeTo = rt
            position = s.position
            zoom = s.zoom.toDouble()
            rotation = s.rotation
            angleOffset = 0.0
            followMode = rt == "Player"
            lastUsedMovementType = rt
            lastTileCamFloor = -1
        }
        resetTransitions()
    }

    private fun resetTransitions() {
        posXTween = PropertyTransition()
        posYTween = PropertyTransition()
        rotTween = PropertyTransition()
        zoomTween = PropertyTransition()
    }

    fun buildTimeline(tileCameraEvents: Map<Int, List<Map<String, Any?>>>, tileStartTimes: DoubleArray) {
        timeline.clear()
        val entries = mutableListOf<CameraTimelineEntry>()

        for ((floor, events) in tileCameraEvents) {
            val tileStart = tileStartTimes.getOrElse(floor) { 0.0 }
            val sorted = events
                .filter { EventUtils.isEventActive(it) }
                .sortedBy { (it["id"] as? Number)?.toInt() ?: Int.MAX_VALUE }

            val zeroOffsetEvents = sorted.filter { (it["angleOffset"] as? Number)?.toDouble() ?: 0.0 == 0.0 }

            for (event in sorted) {
                val ao = (event["angleOffset"] as? Number)?.toDouble() ?: 0.0
                var offset = (ao / 180.0)
                if (ao == 0.0 && zeroOffsetEvents.size > 1) {
                    val order = zeroOffsetEvents.indexOf(event)
                    offset += order * 0.0001
                }
                entries.add(CameraTimelineEntry(tileStart + offset, event, floor))
            }
        }

        entries.sortWith(compareBy({ it.time }, { (it.event["id"] as? Number)?.toInt() ?: Int.MAX_VALUE }))
        timeline.addAll(entries)
    }

    fun update(elapsedTime: Double, pivot: Vector2 = Vector2.ZERO) {
        var idx = lastTimelineIndex
        while (idx + 1 < timeline.size && timeline[idx + 1].time <= elapsedTime) {
            idx++
            val entry = timeline[idx]
            processCameraEvent(entry.event, entry.floor, entry.time)
        }
        lastTimelineIndex = idx
    }

    fun getInterpolated(elapsedTime: Double): CameraInterpolated {
        val t = elapsedTime
        var x = state.position.x
        var y = state.position.y
        var zoom = state.zoom
        var rotation = state.rotation

        fun apply(tr: PropertyTransition<Double>, set: (Double) -> Unit) {
            if (!tr.active) return
            val sv = tr.startValue ?: return
            val ev = tr.endValue ?: return
            val p = ((t - tr.startTime) / tr.duration).coerceIn(0.0, 1.0)
            if (p >= 1.0) {
                tr.active = false
                set(ev)
                return
            }
            val ease = EasingFunctions.get(tr.ease)
            set(sv + (ev - sv) * ease(p))
        }

        apply(posXTween) { x = it }
        apply(posYTween) { y = it }
        apply(rotTween) { rotation = it }
        apply(zoomTween) { zoom = it }

        return CameraInterpolated(x, y, zoom, rotation)
    }

    fun calculateTargetPosition(
        pivot: Vector2,
        interpolated: CameraInterpolated
    ): Vector2 {
        return when (state.relativeTo) {
            "Tile" -> pivot // simplified
            "Global" -> Vector2(interpolated.x, interpolated.y)
            else -> pivot + Vector2(interpolated.x, interpolated.y)
        }
    }

    fun getState() = state

    private fun processCameraEvent(event: Map<String, Any?>, floorIndex: Int, eventTime: Double) {
        if (!EventUtils.isEventActive(event)) return

        val duration = (event["duration"] as? Number)?.toDouble() ?: 0.0
        val ease = (event["ease"] as? String) ?: "Linear"
        val pos = event["position"] as? List<*>
        val zoom = (event["zoom"] as? Number)?.toDouble()
        val rotation = (event["rotation"] as? Number)?.toDouble()
        val relativeTo = EventUtils.getMovementType(event["relativeTo"])
        val angleOffset = (event["angleOffset"] as? Number)?.toDouble() ?: 0.0

        if (relativeTo != "Player") {
            state.relativeTo = relativeTo
            state.followMode = false
        }
        state.lastUsedMovementType = relativeTo
        state.lastTileCamFloor = floorIndex

        val disabled = event["disabled"] as? Map<String, Any?>

        if (pos != null && pos.size >= 2 && disabled?.get("position") != true) {
            val px = (pos[0] as? Number)?.toDouble() ?: 0.0
            val py = (pos[1] as? Number)?.toDouble() ?: 0.0
            if (duration > 0.0) {
                startTween(posXTween, state.position.x, px, eventTime, duration, ease)
                startTween(posYTween, state.position.y, py, eventTime, duration, ease)
            } else {
                state.position = Vector2(px, py)
            }
        }

        if (zoom != null && disabled?.get("zoom") != true) {
            if (duration > 0.0) {
                startTween(zoomTween, state.zoom, zoom, eventTime, duration, ease)
            } else {
                state.zoom = zoom
            }
        }

        if (rotation != null && disabled?.get("rotation") != true) {
            if (duration > 0.0) {
                startTween(rotTween, state.rotation, rotation, eventTime, duration, ease)
            } else {
                state.rotation = rotation
            }
        }

        state.angleOffset = angleOffset
    }

    private fun startTween(tween: PropertyTransition<Double>, from: Double, to: Double, startTime: Double, duration: Double, ease: String) {
        tween.active = true
        tween.startTime = startTime
        tween.duration = duration
        tween.startValue = from
        tween.endValue = to
        tween.ease = ease
    }
}