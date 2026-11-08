package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    size: Dp = 130.dp,
    onMove: (x: Float, y: Float) -> Unit
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                val maxRadius = size.toPx() / 2f
                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(maxRadius, maxRadius)
                        val drag = offset - center
                        val dist = drag.getDistance()
                        val clamped = if (dist > maxRadius) drag * (maxRadius / dist) else drag
                        thumbOffset = clamped
                        onMove(clamped.x / maxRadius, clamped.y / maxRadius)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = thumbOffset + dragAmount
                        val dist = newOffset.getDistance()
                        val clamped = if (dist > maxRadius) newOffset * (maxRadius / dist) else newOffset
                        thumbOffset = clamped
                        onMove(clamped.x / maxRadius, clamped.y / maxRadius)
                    },
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val outerRadius = size.toPx() / 2f - 4f
            val thumbRadius = outerRadius * 0.42f

            // Outer Base Ring
            drawCircle(
                color = Color(0x3300E5FF),
                radius = outerRadius,
                center = center
            )
            drawCircle(
                color = Color(0x8800E5FF),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 2.5f)
            )

            // Directional cross markers
            val crossLength = outerRadius * 0.25f
            drawLine(
                color = Color(0x55FFFFFF),
                start = Offset(center.x, center.y - outerRadius),
                end = Offset(center.x, center.y - outerRadius + crossLength),
                strokeWidth = 2f
            )
            drawLine(
                color = Color(0x55FFFFFF),
                start = Offset(center.x, center.y + outerRadius - crossLength),
                end = Offset(center.x, center.y + outerRadius),
                strokeWidth = 2f
            )
            drawLine(
                color = Color(0x55FFFFFF),
                start = Offset(center.x - outerRadius, center.y),
                end = Offset(center.x - outerRadius + crossLength, center.y),
                strokeWidth = 2f
            )
            drawLine(
                color = Color(0x55FFFFFF),
                start = Offset(center.x + outerRadius - crossLength, center.y),
                end = Offset(center.x + outerRadius, center.y),
                strokeWidth = 2f
            )

            // Draggable Thumb Knob
            val currentThumbPos = center + thumbOffset
            drawCircle(
                color = Color(0xEE1A237E),
                radius = thumbRadius,
                center = currentThumbPos
            )
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = thumbRadius,
                center = currentThumbPos,
                style = Stroke(width = 3f)
            )
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = thumbRadius * 0.35f,
                center = currentThumbPos
            )
        }
    }
}
