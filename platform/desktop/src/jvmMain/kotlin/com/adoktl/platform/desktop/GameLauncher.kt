package com.adoktl.platform.desktop

import com.adoktl.math.AdoktlColor
import com.adoktl.math.Vector2
import com.adoktl.player.JudgmentType
import com.adoktl.player.PlayerEngine
import com.adoktl.render.CameraData
import com.adoktl.render.Mesh
import com.adoktl.render.RenderConfig
import com.adoktl.render.Renderer
import com.adoktl.render.OverlayHUD
import com.adoktl.render.HUDSData
import kotlinx.coroutines.runBlocking
import org.lwjgl.glfw.GLFW.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

object GameLauncher {
    fun start(levelJson: String) {
        runBlocking {
            println("ADOKTL - Game Window Starting...")

            val config = RenderConfig(
                width = 1280,
                height = 720,
                backgroundColor = AdoktlColor(0f, 0f, 0f),
                clearColor = AdoktlColor(0f, 0f, 0f),
                vsync = true,
                multisampling = 4
            )

            val backend = DesktopOpenGLBackend()
            val renderer = Renderer(config, backend)
            renderer.init(backend, config)

            println("Loading level...")
            val playerEngine = PlayerEngine(levelJson)
            playerEngine.init()
            println("Level loaded: ${playerEngine.getLevelData().settings.song}")
            println("Total tiles: ${playerEngine.getTileEngine().tileCount}")

            // Wire up keyboard/mouse input
            val window = backend.getWindowHandle()
            glfwSetKeyCallback(window) { _, key, _, action, _ ->
                if (action == GLFW_PRESS && (key == GLFW_KEY_SPACE || key == GLFW_KEY_ENTER)) {
                    playerEngine.onPress()
                }
            }
            glfwSetMouseButtonCallback(window) { _, button, action, _ ->
                if (action == GLFW_PRESS && button == GLFW_MOUSE_BUTTON_LEFT) {
                    playerEngine.onPress()
                }
            }

            playerEngine.start()

            var camera = CameraData(
                position = Vector2.ZERO,
                zoom = 100.0,
                rotation = 0.0
            )

            val hud = OverlayHUD()
            var fpsCounter = 0.0
            var fpsAccum = 0.0
            var fpsValue = 60.0

            while (!backend.shouldClose()) {
                val frameStart = System.nanoTime()
                backend.pollEvents()
                renderer.beginFrame()

                val frameData = playerEngine.frameState.value
                camera = camera.copy(
                    position = frameData.cameraPosition,
                    zoom = frameData.cameraZoom,
                    rotation = frameData.cameraRotation
                )

                renderer.clear(config.clearColor)
                renderer.setCamera(camera.position, camera.zoom, camera.rotation)

                // Draw tiles
                val tileEngine = playerEngine.getTileEngine()
                val angles = tileEngine.resolveAngles()

                for (i in 0 until tileEngine.tileCount - 1) {
                    val mesh = com.adoktl.engine.TileMeshGenerator.generateTileMesh(
                        previousAngle = angles.getOrElse(i) { 180.0 },
                        currentAngle = angles.getOrElse(i + 1) { 180.0 },
                        is999 = false,
                        trackStyle = "Standard",
                        color = AdoktlColor(0.87f, 0.73f, 0.48f),
                        bgColor = AdoktlColor(0.06f, 0.07f, 0.13f)
                    )
                    if (mesh.vertices.isNotEmpty()) {
                        val renderMesh = Mesh(
                            vertices = mesh.vertices,
                            indices = mesh.indices,
                            colors = mesh.colors
                        )
                        renderer.drawMesh(renderMesh)
                    }
                }

                // Draw planet
                val planetPos = frameData.planetPosition
                val planetMesh = buildPlanetMesh(
                    planetPos.x, planetPos.y,
                    radius = 0.25,
                    if (frameData.combo > 10) AdoktlColor(1f, 0.8f, 0.2f)
                    else AdoktlColor(1f, 0.3f, 0.3f)
                )
                if (planetMesh != null) renderer.drawMesh(planetMesh)

                // Draw judgment indicator (overlay, in screen space)
                renderer.setCamera(Vector2.ZERO, 100.0, 0.0)
                val indicator = buildJudgmentIndicator(frameData.judgment)
                if (indicator != null) renderer.drawMesh(indicator)
                renderer.setCamera(camera.position, camera.zoom, camera.rotation)

                // Update and draw HUD
                hud.update(HUDSData(
                    fps = fpsValue,
                    time = frameData.currentTime,
                    tileIndex = frameData.currentTileIndex,
                    tileBPM = tileEngine.tileBPMs,
                    tileStartTimes = tileEngine.tileStartTimes,
                    totalTiles = frameData.totalTiles
                ))
                renderer.setCamera(Vector2.ZERO, 100.0, 0.0)
                hud.render(backend, config.width, config.height)
                renderer.setCamera(camera.position, camera.zoom, camera.rotation)

                renderer.endFrame()

                // FPS calculation
                fpsAccum += (System.nanoTime() - frameStart) / 1_000_000_000.0
                fpsCounter++
                if (fpsCounter >= 30) {
                    fpsValue = fpsCounter / fpsAccum
                    fpsCounter = 0.0
                    fpsAccum = 0.0
                }
            }

            playerEngine.dispose()
            renderer.shutdown()
            println("ADOKTL game window closed.")
        }
    }

    private fun buildPlanetMesh(x: Double, y: Double, radius: Double, color: AdoktlColor): Mesh? {
        val segments = 16
        val verts = FloatArray((segments + 2) * 3)
        val idx = IntArray(segments * 3)
        val cols = FloatArray((segments + 2) * 4)

        verts[0] = x.toFloat(); verts[1] = y.toFloat(); verts[2] = 0f
        cols[0] = color.r; cols[1] = color.g; cols[2] = color.b; cols[3] = 1f

        for (i in 0 until segments) {
            val a = (2.0 * PI * i / segments).toFloat()
            val vi = (i + 1) * 3
            verts[vi] = (x + radius * cos(a.toDouble())).toFloat()
            verts[vi + 1] = (y + radius * sin(a.toDouble())).toFloat()
            verts[vi + 2] = 0f
            val ci = (i + 1) * 4
            cols[ci] = color.r; cols[ci + 1] = color.g; cols[ci + 2] = color.b; cols[ci + 3] = 0.8f
        }

        for (i in 0 until segments) {
            idx[i * 3] = 0
            idx[i * 3 + 1] = i + 1
            idx[i * 3 + 2] = (i + 1) % segments + 1
        }

        return Mesh(verts, idx, cols)
    }

    private fun buildJudgmentIndicator(judgment: JudgmentType): Mesh? {
        if (judgment == JudgmentType.NONE) return null
        val color = when (judgment) {
            JudgmentType.PERFECT -> AdoktlColor(0f, 1f, 0.5f, 0.9f)
            JudgmentType.GOOD -> AdoktlColor(1f, 0.8f, 0f, 0.9f)
            JudgmentType.MISS -> AdoktlColor(1f, 0.2f, 0.2f, 0.9f)
            else -> return null
        }
        val w = 60f; val h = 8f
        val x = -w / 2f; val y = 340f
        val verts = floatArrayOf(
            x, y, 0f, x + w, y, 0f, x, y - h, 0f, x + w, y - h, 0f
        )
        val idx = intArrayOf(0, 1, 2, 1, 3, 2)
        val cols = floatArrayOf(
            color.r, color.g, color.b, color.a,
            color.r, color.g, color.b, color.a,
            color.r, color.g, color.b, color.a,
            color.r, color.g, color.b, color.a
        )
        return Mesh(verts, idx, cols)
    }
}