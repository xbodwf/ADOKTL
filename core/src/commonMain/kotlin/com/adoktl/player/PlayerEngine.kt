package com.adoktl.player

import com.adoktl.level.LevelData
import com.adoktl.level.LevelParser
import com.adoktl.engine.*
import com.adoktl.audio.AudioEngine
import com.adoktl.audio.HitsoundEngine
import com.adoktl.math.Vector2
import com.adoktl.util.currentTimeMillis
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class PlayerState {
    IDLE, PLAYING, PAUSED, STOPPED, FINISHED
}

data class PlayerFrameData(
    val state: PlayerState,
    val currentTime: Double,
    val currentTileIndex: Int,
    val planetPosition: Vector2,
    val cameraPosition: Vector2,
    val cameraRotation: Double,
    val cameraZoom: Double,
    val totalTiles: Int,
    val completedTiles: Int,
    val score: Int = 0
)

class PlayerEngine(
    private val levelJson: String,
    private val audioEngine: AudioEngine? = null,
    private val hitsoundEngine: HitsoundEngine? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private var levelData: LevelData = LevelParser.parse(levelJson)

    private val tileEngine = TileEngine(levelData)
    private val cameraEngine = CameraEngine(levelData)
    private val colorEngine = ColorEngine()
    private val positionTrackEngine = PositionTrackEngine(levelData)
    private val moveTrackEngine = MoveTrackEngine(levelData)
    private val planetEngine = PlanetEngine()
    private val decorationEngine = DecorationEngine(levelData)

    private lateinit var pipeline: EventPipeline

    private val _frameState = MutableStateFlow(PlayerFrameData(
        state = PlayerState.IDLE,
        currentTime = 0.0,
        currentTileIndex = 0,
        planetPosition = Vector2.ZERO,
        cameraPosition = Vector2.ZERO,
        cameraRotation = 0.0,
        cameraZoom = 100.0,
        totalTiles = levelData.angleData.size,
        completedTiles = 0
    ))
    val frameState: StateFlow<PlayerFrameData> = _frameState.asStateFlow()

    @kotlin.concurrent.Volatile
    private var isPaused = false
    private var timerCoroutine: Job? = null
    private var elapsedTime = 0.0
    private var beatCount = 0.0
    private var score = 0
    private var perfectTiles = 0

    private val onTileReachedCallbacks = mutableListOf<(Int) -> Unit>()

    fun init() {
        pipeline = EventPipeline.fromLevel(levelData)

        tileEngine.initialize()
        cameraEngine.resetState()
        cameraEngine.buildTimeline(pipeline.cameraEvents, tileEngine.tileStartTimes)
        positionTrackEngine.parseEvents()
        positionTrackEngine.calculateAllTransforms()
        moveTrackEngine.initEvents(pipeline.moveTrackEvents, tileEngine.tileStartTimes)
        decorationEngine.init()

        updateFrame()
    }

    fun start() {
        if (_frameState.value.state == PlayerState.PLAYING) return

        elapsedTime = 0.0
        isPaused = false
        score = 0
        perfectTiles = 0

        cameraEngine.resetState()
        cameraEngine.buildTimeline(pipeline.cameraEvents, tileEngine.tileStartTimes)

        audioEngine?.play()
        hitsoundEngine?.start()

        timerCoroutine = CoroutineScope(ioDispatcher).launch {
            val frameDuration = 1.0 / 60.0
            var accumulator = 0.0

            while (isActive && !isPaused) {
                val frameStart = currentTimeMillis()
                accumulator += frameDuration

                while (accumulator >= frameDuration) {
                    accumulator -= frameDuration
                    update(frameDuration)
                }

                withContext(Dispatchers.Main) {
                    updateFrame()
                }

                val frameTime = currentTimeMillis() - frameStart
                val sleepTime = (frameDuration * 1000 - frameTime).coerceAtLeast(0.0)
                delay(sleepTime.toLong())
            }
        }
    }

    private fun update(deltaTime: Double) {
        elapsedTime += deltaTime
        val prevTile = tileEngine.getCurrentTileIndex(elapsedTime)

        val planetPos = tileEngine.calculatePlanetPosition(elapsedTime)

        planetEngine.updatePlanetPosition(
            PlanetState(),
            com.adoktl.math.Vector3(planetPos.x, planetPos.y, 0.0),
            deltaTime
        )

        cameraEngine.update(elapsedTime, planetPos)
        moveTrackEngine.update(elapsedTime)

        val interp = cameraEngine.getInterpolated(elapsedTime)
        val targetPos = cameraEngine.calculateTargetPosition(planetPos, interp)

        val currentTile = tileEngine.getCurrentTileIndex(elapsedTime)
        if (currentTile != prevTile) {
            onTileReachedCallbacks.forEach { it(currentTile) }
            score += 100
            perfectTiles++
        }

        beatCount += deltaTime * (levelData.settings.bpm / 60.0)

        _frameState.update {
            it.copy(
                state = if (currentTile >= tileEngine.tileCount - 1) PlayerState.FINISHED else PlayerState.PLAYING,
                currentTime = elapsedTime,
                currentTileIndex = currentTile,
                planetPosition = planetPos,
                cameraPosition = targetPos,
                cameraRotation = interp.rotation,
                cameraZoom = interp.zoom,
                completedTiles = currentTile,
                score = score
            )
        }
    }

    fun pause() {
        isPaused = true
        audioEngine?.pause()
        hitsoundEngine?.stop()
        _frameState.update { it.copy(state = PlayerState.PAUSED) }
    }

    fun resume() {
        if (_frameState.value.state != PlayerState.PAUSED) return
        isPaused = false
        audioEngine?.resume()
        hitsoundEngine?.startAtOffset(elapsedTime)

        timerCoroutine = CoroutineScope(ioDispatcher).launch {
            val frameDuration = 1.0 / 60.0
            var accumulator = 0.0

            while (isActive && !isPaused) {
                val frameStart = currentTimeMillis()
                accumulator += frameDuration

                while (accumulator >= frameDuration) {
                    accumulator -= frameDuration
                    update(frameDuration)
                }

                withContext(Dispatchers.Main) {
                    updateFrame()
                }

                val frameTime = currentTimeMillis() - frameStart
                val sleepTime = (frameDuration * 1000 - frameTime).coerceAtLeast(0.0)
                delay(sleepTime.toLong())
            }
        }
    }

    fun stop() {
        isPaused = true
        timerCoroutine?.cancel()
        timerCoroutine = null
        audioEngine?.stop()
        hitsoundEngine?.stop()
        elapsedTime = 0.0
        score = 0
        perfectTiles = 0
        _frameState.update { it.copy(state = PlayerState.STOPPED, score = 0) }
    }

    fun seek(time: Double) {
        elapsedTime = time.coerceIn(0.0, getTotalDuration())
        audioEngine?.seek(time)
        updateFrame()
    }

    fun getTotalDuration(): Double {
        val n = tileEngine.tileCount
        return if (n > 0) tileEngine.tileStartTimes[n - 1] + 2.0 else 0.0
    }

    fun getLevelData(): LevelData = levelData
    fun getTileEngine() = tileEngine
    fun getCameraEngine() = cameraEngine
    fun getColorEngine() = colorEngine
    fun getPlanetEngine() = planetEngine
    fun getDecorationEngine() = decorationEngine
    fun getPositionTrackEngine() = positionTrackEngine
    fun getMoveTrackEngine() = moveTrackEngine

    fun onTileReached(callback: (Int) -> Unit) {
        onTileReachedCallbacks.add(callback)
    }

    fun dispose() {
        timerCoroutine?.cancel()
        audioEngine?.dispose()
        hitsoundEngine?.dispose()
    }

    private fun updateFrame() {
        _frameState.update { it.copy(currentTime = elapsedTime) }
    }
}