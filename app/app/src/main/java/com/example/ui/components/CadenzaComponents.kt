package com.example.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import kotlin.math.sin
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.PI
import com.example.ui.theme.*

// --- NEUMORPHIC MODIFIER ---
fun Modifier.neumorphic(
    isPressed: Boolean = false,
    cornerRadius: Dp = 20.dp,
    lightShadowColor: Color = CadenzaShadowLight,
    darkShadowColor: Color = CadenzaShadowDark,
    surfaceColor: Color = CadenzaBackground,
    elevation: Dp = 10.dp
) = this.drawBehind {
    val cornerRadiusPx = cornerRadius.toPx()
    val elevationPx = elevation.toPx()
    val blurRadius = elevationPx * 2f // Massive blur for soft look
    
    drawIntoCanvas { canvas ->
        if (!isPressed) {
            // Light outer shadow (Top Left)
            val paintLight = Paint().asFrameworkPaint().apply {
                color = lightShadowColor.toArgb()
                isAntiAlias = true
                maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.nativeCanvas.drawRoundRect(
                -elevationPx, -elevationPx, 
                size.width, size.height, 
                cornerRadiusPx, cornerRadiusPx, paintLight
            )

            // Dark outer shadow (Bottom Right)
            val paintDark = Paint().asFrameworkPaint().apply {
                color = darkShadowColor.toArgb()
                isAntiAlias = true
                maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.nativeCanvas.drawRoundRect(
                elevationPx, elevationPx, 
                size.width + elevationPx, size.height + elevationPx, 
                cornerRadiusPx, cornerRadiusPx, paintDark
            )
            
            // Surface Color
            val paintSurface = Paint().asFrameworkPaint().apply {
                color = surfaceColor.toArgb()
                isAntiAlias = true
            }
            canvas.nativeCanvas.drawRoundRect(
                0f, 0f, size.width, size.height,
                cornerRadiusPx, cornerRadiusPx, paintSurface
            )
        } else {
            // Surface Color
            val paintSurface = Paint().asFrameworkPaint().apply {
                color = surfaceColor.toArgb()
                isAntiAlias = true
            }
            canvas.nativeCanvas.drawRoundRect(
                0f, 0f, size.width, size.height,
                cornerRadiusPx, cornerRadiusPx, paintSurface
            )

            canvas.save()
            val path = android.graphics.Path().apply {
                addRoundRect(
                    0f, 0f, size.width, size.height,
                    cornerRadiusPx, cornerRadiusPx,
                    android.graphics.Path.Direction.CW
                )
            }
            canvas.nativeCanvas.clipPath(path)

            // Dark inner shadow (Top Left)
            // Draw a stroke shifted slightly UP and LEFT so it bleeds into the top-left corner
            val paintDarkInner = Paint().asFrameworkPaint().apply {
                color = darkShadowColor.toArgb()
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = elevationPx * 2f
                maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.nativeCanvas.drawRoundRect(
                -elevationPx * 0.5f, -elevationPx * 0.5f, 
                size.width + elevationPx * 0.5f, size.height + elevationPx * 0.5f, 
                cornerRadiusPx, cornerRadiusPx, paintDarkInner
            )

            // Light inner shadow (Bottom Right)
            // Draw a stroke shifted slightly DOWN and RIGHT so it bleeds into the bottom-right corner
            val paintLightInner = Paint().asFrameworkPaint().apply {
                color = lightShadowColor.toArgb()
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = elevationPx * 2f
                maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.nativeCanvas.drawRoundRect(
                elevationPx * 0.5f, elevationPx * 0.5f, 
                size.width + elevationPx, size.height + elevationPx, 
                cornerRadiusPx, cornerRadiusPx, paintLightInner
            )
            canvas.restore()
        }
    }
}

// --- CADENZA BUTTON ---
@Composable
fun CadenzaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    color: Color = CadenzaTextPrimary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .neumorphic(
                isPressed = isPressed,
                cornerRadius = 14.dp,
                elevation = 6.dp
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = interactionSource, indication = null) {
                if (!isLoading) onClick()
            }
            .padding(vertical = 16.dp, horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            Text("...", color = color, fontWeight = FontWeight.Bold)
        } else {
            Text(
                text = text,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

// --- CADENZA CARD (Neumorphic Raised Panel) ---
@Composable
fun CadenzaCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .neumorphic(
                isPressed = false,
                cornerRadius = 20.dp,
                elevation = 8.dp
            )
            .clip(RoundedCornerShape(20.dp))
    ) {
        content()
    }
}

// --- CADENZA DISPLAY (Neumorphic Recessed Screen) ---
@Composable
fun CadenzaDisplay(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .neumorphic(
                isPressed = true, // recessed
                cornerRadius = 16.dp,
                elevation = 6.dp
            )
            .clip(RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        content()
    }
}

// --- CADENZA SECTION HEADER ---
@Composable
fun CadenzaSectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = CadenzaTextPrimary,
        modifier = modifier
    )
}

// --- CADENZA SWITCH ---
@Composable
fun CadenzaSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbOffset by animateDpAsState(if (checked) 24.dp else 4.dp, label = "thumb")

    Box(
        modifier = modifier
            .width(52.dp)
            .height(28.dp)
            .neumorphic(
                isPressed = true,
                cornerRadius = 14.dp,
                elevation = 3.dp
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp, bottom = 4.dp)
                .offset(x = thumbOffset)
                .size(20.dp)
                .clip(CircleShape)
                .neumorphic(
                    isPressed = false,
                    cornerRadius = 10.dp,
                    elevation = 2.dp
                )
        )
    }
}

// --- CADENZA METER ---
@Composable
fun CadenzaMeter(
    value: Float,
    modifier: Modifier = Modifier
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.coerceIn(-1f, 1f),
        animationSpec = tween(300),
        label = "meter_value"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .neumorphic(isPressed = true, cornerRadius = 12.dp, elevation = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f
            
            drawLine(
                color = CadenzaTextSecondary.copy(alpha = 0.5f),
                start = Offset(width / 2f, 10f),
                end = Offset(width / 2f, height - 10f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            val mappedX = ((animatedValue + 1f) / 2f) * width
            
            drawCircle(
                color = CadenzaTextPrimary,
                radius = 12.dp.toPx(),
                center = Offset(mappedX, centerY)
            )
        }
    }
}

// --- CADENZA SCORE GAUGE ---
@Composable
fun CadenzaScoreGauge(
    score: Int,
    modifier: Modifier = Modifier
) {
    val animatedScore by animateFloatAsState(
        targetValue = score.toFloat(),
        animationSpec = tween(1000),
        label = "score"
    )

    Box(
        modifier = modifier
            .size(160.dp)
            .clip(CircleShape)
            .neumorphic(
                isPressed = true, 
                cornerRadius = 80.dp,
                elevation = 8.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val strokeWidth = 14.dp.toPx()
            
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(CadenzaPrimary, CadenzaSecondary, CadenzaPrimary)
                ),
                startAngle = -90f,
                sweepAngle = 360f * (animatedScore / 100f),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        
        // Inner raised center
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .neumorphic(
                    isPressed = false,
                    cornerRadius = 45.dp,
                    elevation = 6.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${animatedScore.toInt()}%",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = CadenzaTextPrimary
            )
        }
    }
}

// --- CADENZA WAVEFORM ---
@Composable
fun CadenzaWaveform(modifier: Modifier = Modifier, isAnimating: Boolean = true) {
    val infiniteTransition = rememberInfiniteTransition()
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "wave_phase"
    )

    val actualPhase = if (isAnimating) phase else 0f

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        
        // Vertical frequency bars
        val barCount = 60
        val barWidth = width / barCount
        val maxBarHeight = height * 0.35f
        for (i in 0 until barCount) {
            val nx = i.toFloat() / barCount
            val xPos = i * barWidth + barWidth / 2f
            
            // Pseudo-random varying height
            val bHeight = maxBarHeight * abs(sin(nx * 12f + actualPhase * 0.5f)) * abs(cos(nx * 5f)) + maxBarHeight * 0.2f
            
            // Edge fade factor
            val edgeFactor = if (nx < 0.2f) nx / 0.2f else if (nx > 0.8f) (1f - nx) / 0.2f else 1f
            val barAlpha = 0.15f * edgeFactor
            
            drawLine(
                color = CadenzaSecondary.copy(alpha = barAlpha),
                start = Offset(xPos, centerY - bHeight),
                end = Offset(xPos, centerY + bHeight),
                strokeWidth = barWidth * 0.6f,
                cap = StrokeCap.Round
            )
        }

        val path1 = android.graphics.Path()
        val path2 = android.graphics.Path()
        
        val amplitude1 = height * 0.25f
        val amplitude2 = height * 0.15f
        val frequency = 1.5f 
        
        for (x in 0..width.toInt() step 5) {
            val xPos = x.toFloat()
            val normalizedX = xPos / width
            
            val y1 = centerY + sin((normalizedX * frequency * 2 * PI) + actualPhase).toFloat() * amplitude1
            val y2 = centerY + cos((normalizedX * frequency * 1.5 * PI) - actualPhase * 1.2f).toFloat() * amplitude2
            
            if (x == 0) {
                path1.moveTo(xPos, y1)
                path2.moveTo(xPos, y2)
            } else {
                path1.lineTo(xPos, y1)
                path2.lineTo(xPos, y2)
            }
        }
        
        val gradient1 = Brush.horizontalGradient(
            0.0f to Color.Transparent,
            0.2f to CadenzaPrimary,
            0.5f to CadenzaSecondary,
            0.8f to CadenzaPrimary,
            1.0f to Color.Transparent
        )
        val gradient2 = Brush.horizontalGradient(
            0.0f to Color.Transparent,
            0.2f to CadenzaSecondary,
            0.5f to CadenzaPrimary,
            0.8f to CadenzaSecondary,
            1.0f to Color.Transparent
        )
        
        drawPath(
            path = path1.asComposePath(),
            brush = gradient1,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        drawPath(
            path = path2.asComposePath(),
            brush = gradient2,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

// --- CADENZA SESSION DIAL ---
@Composable
fun CadenzaSessionDial(
    modifier: Modifier = Modifier,
    isAnimating: Boolean = false,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .size(160.dp)
            .clip(CircleShape)
            // Outer recessed hole
            .neumorphic(isPressed = true, cornerRadius = 80.dp, elevation = 10.dp)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Draw tick marks
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.width / 2f - 12.dp.toPx() 
            val center = Offset(size.width / 2f, size.height / 2f)
            val tickLength = 4.dp.toPx()
            val tickPaint = Paint().asFrameworkPaint().apply {
                color = CadenzaTextSecondary.copy(alpha = 0.2f).toArgb()
                strokeWidth = 2f
                isAntiAlias = true
            }

            for (i in 0 until 60) {
                val angle = (i * 6f) * (PI / 180f)
                val startX = center.x + cos(angle).toFloat() * (radius - tickLength)
                val startY = center.y + sin(angle).toFloat() * (radius - tickLength)
                val endX = center.x + cos(angle).toFloat() * radius
                val endY = center.y + sin(angle).toFloat() * radius

                if (i % 5 == 0) {
                   tickPaint.strokeWidth = 3f
                   tickPaint.color = CadenzaTextSecondary.copy(alpha = 0.35f).toArgb()
                   drawContext.canvas.nativeCanvas.drawLine(
                       center.x + cos(angle).toFloat() * (radius - tickLength * 1.5f),
                       center.y + sin(angle).toFloat() * (radius - tickLength * 1.5f),
                       endX, endY, tickPaint
                   )
                } else {
                   tickPaint.strokeWidth = 2f
                   tickPaint.color = CadenzaTextSecondary.copy(alpha = 0.2f).toArgb()
                   drawContext.canvas.nativeCanvas.drawLine(startX, startY, endX, endY, tickPaint)
                }
            }
        }
        
        // Inner raised button
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .neumorphic(isPressed = isPressed, cornerRadius = 45.dp, elevation = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Start Session",
                tint = CadenzaPrimary,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

// --- CADENZA SESSION BUTTON ---
@Composable
fun CadenzaSessionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Outer Container (Recessed)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(32.dp))
            .neumorphic(isPressed = true, cornerRadius = 32.dp, elevation = 8.dp)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Waveform underneath the button
        CadenzaWaveform(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            isAnimating = !isPressed // Animates when idle
        )
        
        // Inner Tactile Microphone Button (Raised)
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .neumorphic(
                    isPressed = isPressed,
                    cornerRadius = 40.dp,
                    elevation = 6.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Start Session",
                tint = if (isPressed) CadenzaPrimary else CadenzaPrimary,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
