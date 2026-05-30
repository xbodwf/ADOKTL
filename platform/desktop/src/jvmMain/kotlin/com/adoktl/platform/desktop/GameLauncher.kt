package com.adoktl.platform.desktop

import com.adoktl.math.AdoktlColor
import com.adoktl.math.Vector2
import com.adoktl.player.PlayerEngine
import com.adoktl.render.CameraData
import com.adoktl.render.RenderConfig
import com.adoktl.render.Renderer
import kotlinx.coroutines.runBlocking
import org.lwjgl.glfw.GLFW.*

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

            playerEngine.start()

            var camera = CameraData(
                position = Vector2.ZERO,
                zoom = 100.0,
                rotation = 0.0
            )

            while (!backend.shouldClose()) {
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
                        val renderMesh = com.adoktl.render.Mesh(
                            vertices = mesh.vertices,
                            indices = mesh.indices,
                            colors = mesh.colors
                        )
                        renderer.drawMesh(renderMesh)
                    }
                }

                renderer.endFrame()
            }

            playerEngine.dispose()
            renderer.shutdown()
            println("ADOKTL game window closed.")
        }
    }
}