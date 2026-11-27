package com.example.engine3d

import android.annotation.SuppressLint
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("ClickableViewAccessibility")
@Composable
fun GwbWorldGlView(
    simulation: GwbWorldSimulation,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            GLSurfaceView(context).apply {
                setEGLContextClientVersion(3)
                preserveEGLContextOnPause = true
                setRenderer(GwbGlRenderer(simulation))
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

                var lastX = 0f
                var lastY = 0f
                setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            lastX = event.x
                            lastY = event.y
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = event.x - lastX
                            val dy = event.y - lastY
                            lastX = event.x
                            lastY = event.y
                            simulation.orbitCamera(dx * 0.25f, dy * 0.16f)
                            true
                        }
                        else -> true
                    }
                }
            }
        }
    )
}
