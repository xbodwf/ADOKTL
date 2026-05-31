package com.adoktl.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusable
import com.adoktl.engine.TileMeshGenerator
import com.adoktl.player.JudgmentType
import com.adoktl.player.PlayerEngine
import com.adoktl.player.PlayerState

@Composable
fun rememberPlayerEngine(levelJson: String): PlayerEngine {
    val engine = remember(levelJson) {
        PlayerEngine(levelJson).also { it.init() }
    }
    DisposableEffect(levelJson) {
        onDispose { engine.dispose() }
    }
    return engine
}

@Composable
fun ComposeGameView(
    playerEngine: PlayerEngine,
    modifier: Modifier = Modifier
) {
    val frameData by playerEngine.frameState.collectAsState()

    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0f0f1a))
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.key == Key.Spacebar || event.key == Key.Enter) {
                    if (event.type == KeyEventType.KeyUp) {
                        playerEngine.onPress()
                    }
                    true
                } else false
            }
            .pointerInput(Unit) {
                detectTapGestures {
                    if (frameData.state == PlayerState.PLAYING || frameData.state == PlayerState.IDLE) {
                        if (frameData.state == PlayerState.IDLE) {
                            playerEngine.start()
                        } else {
                            playerEngine.onPress()
                        }
                    }
                }
            }
    ) {
        if (frameData.state == PlayerState.IDLE || frameData.state == PlayerState.STOPPED) {
            // Show tap to start prompt
            Text(
                text = "Tap or press Space to start",
                color = Color(0xFF888888),
                fontSize = 20.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (frameData.state == PlayerState.FINISHED) {
            // Show results
            Text(
                text = "Level Complete!\nScore: ${frameData.score}\nMax Combo: ${frameData.maxCombo}",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            // Game rendering
            Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 64.dp)) {
                renderGame(frameData, playerEngine, size.width, size.height)
            }

            // HUD overlay (score, combo, time)
            Box(modifier = Modifier.padding(12.dp).align(Alignment.TopStart)) {
                Text(
                    text = buildHudText(frameData),
                    color = Color(0xAAFFFFFF),
                    fontSize = 13.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            // Judgment indicator
            if (frameData.judgment != JudgmentType.NONE) {
                val jColor = when (frameData.judgment) {
                    JudgmentType.PERFECT -> Color(0f, 1f, 0.5f)
                    JudgmentType.GOOD -> Color(1f, 0.8f, 0f)
                    JudgmentType.MISS -> Color(1f, 0.2f, 0.2f)
                    else -> Color.Transparent
                }
                Text(
                    text = when (frameData.judgment) {
                        JudgmentType.PERFECT -> "PERFECT"
                        JudgmentType.GOOD -> "GOOD"
                        JudgmentType.MISS -> "MISS"
                        else -> ""
                    },
                    color = jColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center).padding(bottom = 120.dp)
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

private fun buildHudText(frameData: com.adoktl.player.PlayerFrameData): String {
    val timeSec = frameData.currentTime.toInt()
    val m = timeSec / 60
    val s = timeSec % 60
    return "Score: ${frameData.score}  Combo: ${frameData.combo}x  Time: ${m}:${s.toString().padStart(2, '0')}  Tiles: ${frameData.completedTiles}/${frameData.totalTiles}"
}

private fun DrawScope.renderGame(
    frameData: com.adoktl.player.PlayerFrameData,
    playerEngine: PlayerEngine,
    width: Float,
    height: Float
) {
    val tileEngine = playerEngine.getTileEngine()
    if (tileEngine.tileCount == 0) return

    val angles = tileEngine.resolveAngles()
    val cameraPos = frameData.cameraPosition
    val zoom = (frameData.cameraZoom / 100.0).toFloat()

    if (zoom <= 0f) return

    val scale = height / (2f * zoom)

    val aspect = width / height
    val left = -aspect * zoom
    val right = aspect * zoom
    val bottom = -zoom
    val top = zoom

    withTransform({
        translate(width / 2f, height / 2f)
        scale(scale, -scale)
        translate(-cameraPos.x.toFloat(), -cameraPos.y.toFloat())
        rotate(frameData.cameraRotation.toFloat())
    }) {
        // Draw tiles
        val goldColor = Color(0.87f, 0.73f, 0.48f)
        val bgColor = Color(0.06f, 0.07f, 0.13f)

        for (i in 0 until tileEngine.tileCount - 1) {
            val prevAngle = angles.getOrElse(i) { 180.0 }
            val currAngle = angles.getOrElse(i + 1) { 180.0 }
            val points = TileMeshGenerator.calculatePoints(
                -prevAngle, -currAngle,
                0.275, 0.5, 0.0
            )
            if (points.size >= 3) {
                val path = Path()
                path.moveTo(points[0].first.toFloat(), (-points[0].second).toFloat())
                for (j in 1 until points.size) {
                    path.lineTo(points[j].first.toFloat(), (-points[j].second).toFloat())
                }
                path.close()
                drawPath(path, goldColor)
            }
        }

        // Draw planet
        val planetPos = frameData.planetPosition
        val planetColor = if (frameData.combo > 10) Color(1f, 0.8f, 0.2f) else Color(1f, 0.3f, 0.3f)
        drawCircle(
            color = planetColor,
            radius = 0.25f,
            center = Offset(planetPos.x.toFloat(), planetPos.y.toFloat())
        )
    }
}
