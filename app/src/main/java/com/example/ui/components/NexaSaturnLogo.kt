package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NexaGreenBright
import com.example.ui.theme.NexaGreenMid
import com.example.ui.theme.NexaNeonGreen

@Composable
fun NexaSaturnLogo(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val width = this.size.width
            val height = this.size.height

            // Ring 1: tilted -32 degrees
            rotate(degrees = -32f, pivot = Offset(width / 2f, height / 2f)) {
                drawOval(
                    color = NexaNeonGreen.copy(alpha = 0.85f),
                    topLeft = Offset(width * 0.05f, height * 0.32f),
                    size = Size(width * 0.90f, height * 0.36f),
                    style = Stroke(width = width * 0.05f)
                )
            }

            // Ring 2: tilted +35 degrees
            rotate(degrees = 35f, pivot = Offset(width / 2f, height / 2f)) {
                drawOval(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            NexaGreenBright.copy(alpha = 0.9f),
                            NexaGreenMid.copy(alpha = 0.4f)
                        )
                    ),
                    topLeft = Offset(width * 0.08f, height * 0.35f),
                    size = Size(width * 0.84f, height * 0.30f),
                    style = Stroke(width = width * 0.04f)
                )
            }
        }

        // Bold italic 'N' with futuristic style
        Text(
            text = "N",
            color = NexaGreenBright,
            fontSize = (size.value * 0.68f).sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            fontFamily = FontFamily.SansSerif
        )
    }
}
