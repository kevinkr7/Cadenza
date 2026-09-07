package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.models.AvatarPreferences

@Composable
fun AvatarCompanion(
    prefs: AvatarPreferences,
    modifier: Modifier = Modifier,
    isListening: Boolean = false
) {
    // Breathing/Bounce Animation
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_bounce")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    // Pulsing glow animation if speaking / listening
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Primary and secondary accent colors based on preferences
    val accentColor = when (prefs.colorTheme) {
        "purple" -> Color(0xFF8B5CF6)
        "coral" -> Color(0xFFF43F5E)
        "indigo" -> Color(0xFF6366F1)
        else -> Color(0xFF14B8A6) // default teal
    }
    val backgroundGrad = Brush.radialGradient(
        colors = listOf(accentColor.copy(alpha = 0.25f), Color.Transparent),
        radius = 350f
    )

    Box(
        modifier = modifier
            .size(190.dp)
            .drawBehindGlow(glowScale, accentColor)
            .background(backgroundGrad, shape = CircleShape)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = bounceOffset.dp)
                .padding(20.dp)
        ) {
            val w = size.width
            val h = size.height
            val centerX = w / 2f
            val centerY = h / 2f

            // --- 1. Background Aura Ring ---
            drawCircle(
                color = accentColor.copy(alpha = 0.15f),
                radius = w * 0.45f,
                center = Offset(centerX, centerY)
            )

            // --- 2. Body / Outfit ---
            val outfitColor = when (prefs.outfit) {
                "formal" -> Color(0xFF334155) // Dark slate jacket
                "studio" -> Color(0xFF0F172A) // Sleek studio black neck shirt
                "rockstar" -> Color(0xFF7F1D1D) // Dark leather red jacket
                else -> Color(0xFF2563EB) // Casual blue
            }
            // Torso path
            val bodyPath = Path().apply {
                moveTo(w * 0.2f, h)
                quadraticBezierTo(w * 0.25f, h * 0.65f, w * 0.35f, h * 0.65f)
                lineTo(w * 0.65f, h * 0.65f)
                quadraticBezierTo(w * 0.75f, h * 0.65f, w * 0.8f, h)
                close()
            }
            drawPath(path = bodyPath, color = outfitColor)

            // Shirt inner collar or star for rockstar
            if (prefs.outfit == "rockstar") {
                val starPath = Path().apply {
                    moveTo(centerX, h * 0.72f)
                    lineTo(centerX + 12, h * 0.85f)
                    lineTo(centerX + 30, h * 0.85f)
                    lineTo(centerX + 15, h * 0.93f)
                    lineTo(centerX + 22, h * 1.05f)
                    lineTo(centerX, h * 0.98f)
                    lineTo(centerX - 22, h * 1.05f)
                    lineTo(centerX - 15, h * 0.93f)
                    lineTo(centerX - 30, h * 0.85f)
                    lineTo(centerX - 12, h * 0.85f)
                    close()
                }
                drawPath(path = starPath, color = Color(0xFFFBBF24)) // gold star
            } else if (prefs.outfit == "formal") {
                // Draw tie/lapel
                val lapelPath = Path().apply {
                    moveTo(centerX - 10, h * 0.65f)
                    lineTo(centerX, h * 0.82f)
                    lineTo(centerX + 10, h * 0.65f)
                    close()
                }
                drawPath(path = lapelPath, color = accentColor)
            }

            // --- 3. Neck & Face ---
            val skinColor = Color(0xFFFFD1B3) // Soft warm skin tone
            // Neck
            drawRect(
                color = skinColor,
                topLeft = Offset(centerX - w * 0.08f, centerY),
                size = Size(w * 0.16f, h * 0.25f)
            )
            // Head (oval)
            drawOval(
                color = skinColor,
                topLeft = Offset(centerX - w * 0.22f, centerY - h * 0.26f),
                size = Size(w * 0.44f, h * 0.44f)
            )

            // --- 4. Face Features ---
            // Eyes
            val eyeColor = Color(0xFF1E293B)
            val eyeWidth = w * 0.04f
            val eyeOffsetY = centerY - h * 0.07f
            
            if (isListening) {
                // Closed singing eyes happy curves
                val eyeLPath = Path().apply {
                    moveTo(centerX - w * 0.12f, eyeOffsetY)
                    quadraticBezierTo(centerX - w * 0.09f, eyeOffsetY + 8, centerX - w * 0.06f, eyeOffsetY)
                }
                val eyeRPath = Path().apply {
                    moveTo(centerX + w * 0.06f, eyeOffsetY)
                    quadraticBezierTo(centerX + w * 0.09f, eyeOffsetY + 8, centerX + w * 0.12f, eyeOffsetY)
                }
                drawPath(eyeLPath, eyeColor, style = Stroke(3f, cap = StrokeCap.Round))
                drawPath(eyeRPath, eyeColor, style = Stroke(3f, cap = StrokeCap.Round))
            } else {
                // Open intelligent eyes
                drawCircle(color = eyeColor, radius = eyeWidth, center = Offset(centerX - w * 0.09f, eyeOffsetY))
                drawCircle(color = eyeColor, radius = eyeWidth, center = Offset(centerX + w * 0.09f, eyeOffsetY))

                // Eyebrows
                drawLine(
                    color = Color(0xFF475569),
                    start = Offset(centerX - w * 0.14f, eyeOffsetY - 12),
                    end = Offset(centerX - w * 0.04f, eyeOffsetY - 10),
                    strokeWidth = 3f
                )
                drawLine(
                    color = Color(0xFF475569),
                    start = Offset(centerX + w * 0.04f, eyeOffsetY - 10),
                    end = Offset(centerX + w * 0.14f, eyeOffsetY - 12),
                    strokeWidth = 3f
                )
            }

            // Smiling mouth
            val mouthOffsetY = centerY + h * 0.05f
            if (isListening) {
                // Singing open mouth (O shape)
                drawCircle(
                    color = Color(0xFFEA580C),
                    radius = 9f,
                    center = Offset(centerX, mouthOffsetY)
                )
                drawCircle(
                    color = Color(0xFFF43F5E),
                    radius = 7f,
                    center = Offset(centerX, mouthOffsetY)
                )
            } else {
                // Wide friendly smile
                val mouthPath = Path().apply {
                    moveTo(centerX - 16, mouthOffsetY)
                    quadraticBezierTo(centerX, mouthOffsetY + 14, centerX + 16, mouthOffsetY)
                }
                drawPath(mouthPath, Color(0xFF991B1B), style = Stroke(3.5f, cap = StrokeCap.Round))
            }

            // Highlight cheeks
            drawCircle(Color(0xFFF43F5E).copy(alpha = 0.25f), radius = 10f, center = Offset(centerX - w * 0.14f, centerY))
            drawCircle(Color(0xFFF43F5E).copy(alpha = 0.25f), radius = 10f, center = Offset(centerX + w * 0.14f, centerY))

            // --- 5. Hair (Customized) ---
            val hairColor = when (prefs.colorTheme) {
                "purple" -> Color(0xFF6B21A8) // Deep royal purple
                "coral" -> Color(0xFFC084FC) // Lilac rockstar
                "indigo" -> Color(0xFF1E1B4B) // Dark velvet navy
                else -> Color(0xFF11453E) // Dark teal / green
            }

            if (prefs.gender == "female") {
                // Female Hairstyles
                when (prefs.hairstyle) {
                    "classic" -> {
                        // Smooth bob cut around face
                        drawArc(
                            color = hairColor,
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = true,
                            topLeft = Offset(centerX - w * 0.25f, centerY - h * 0.32f),
                            size = Size(w * 0.5f, h * 0.4f)
                        )
                        // Side hair strands framing cheeks
                        drawRect(
                            color = hairColor,
                            topLeft = Offset(centerX - w * 0.25f, centerY - h * 0.12f),
                            size = Size(w * 0.08f, h * 0.26f)
                        )
                        drawRect(
                            color = hairColor,
                            topLeft = Offset(centerX + w * 0.17f, centerY - h * 0.12f),
                            size = Size(w * 0.08f, h * 0.26f)
                        )
                    }
                    "vibrant" -> {
                        // High Ponytail bun on top
                        drawCircle(
                            color = hairColor,
                            radius = w * 0.12f,
                            center = Offset(centerX, centerY - h * 0.30f)
                        )
                        // Fringe and top crown
                        drawArc(
                            color = hairColor,
                            startAngle = 175f,
                            sweepAngle = 190f,
                            useCenter = true,
                            topLeft = Offset(centerX - w * 0.24f, centerY - h * 0.28f),
                            size = Size(w * 0.48f, h * 0.36f)
                        )
                    }
                    "modern" -> {
                        // Asymmetric sleek crop cut
                        drawArc(
                            color = hairColor,
                            startAngle = 165f,
                            sweepAngle = 210f,
                            useCenter = true,
                            topLeft = Offset(centerX - w * 0.23f, centerY - h * 0.3f),
                            size = Size(w * 0.46f, h * 0.38f)
                        )
                        // Asymmetric sweeping fringe strand
                        val strand = Path().apply {
                            moveTo(centerX - w * 0.18f, centerY - h * 0.2f)
                            quadraticBezierTo(centerX - w * 0.05f, centerY - h * 0.14f, centerX, centerY - h * 0.02f)
                            quadraticBezierTo(centerX - w * 0.12f, centerY - h * 0.1f, centerX - w * 0.18f, centerY - h * 0.2f)
                        }
                        drawPath(strand, hairColor)
                    }
                    else -> { // retro
                        // Soft round waves/afro style
                        drawCircle(hairColor, radius = w * 0.15f, center = Offset(centerX - w * 0.16f, centerY - h * 0.14f))
                        drawCircle(hairColor, radius = w * 0.15f, center = Offset(centerX + w * 0.16f, centerY - h * 0.14f))
                        drawCircle(hairColor, radius = w * 0.18f, center = Offset(centerX, centerY - h * 0.23f))
                    }
                }
            } else {
                // Male Hairstyles
                when (prefs.hairstyle) {
                    "classic" -> {
                        // Short side parted neat cut
                        drawArc(
                            color = hairColor,
                            startAngle = 190f,
                            sweepAngle = 160f,
                            useCenter = true,
                            topLeft = Offset(centerX - w * 0.24f, centerY - h * 0.33f),
                            size = Size(w * 0.48f, h * 0.32f)
                        )
                    }
                    "vibrant" -> {
                        // Long rocker hair frame
                        drawArc(
                            color = hairColor,
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = true,
                            topLeft = Offset(centerX - w * 0.24f, centerY - h * 0.28f),
                            size = Size(w * 0.48f, h * 0.34f)
                        )
                        val hairStands = Path().apply {
                            moveTo(centerX - w * 0.23f, centerY - h * 0.1f)
                            lineTo(centerX - w * 0.26f, centerY + h * 0.23f)
                            lineTo(centerX - w * 0.13f, centerY)
                            moveTo(centerX + w * 0.23f, centerY - h * 0.1f)
                            lineTo(centerX + w * 0.26f, centerY + h * 0.23f)
                            lineTo(centerX + w * 0.13f, centerY)
                        }
                        drawPath(hairStands, hairColor)
                    }
                    "modern" -> {
                        // Spiky modern pompadour brush up
                        val pompadourPath = Path().apply {
                            moveTo(centerX - w * 0.22f, centerY - h * 0.16f)
                            quadraticBezierTo(centerX - w * 0.25f, centerY - h * 0.38f, centerX - w * 0.1f, centerY - h * 0.42f)
                            quadraticBezierTo(centerX + w * 0.15f, centerY - h * 0.44f, centerX + w * 0.22f, centerY - h * 0.2f)
                            quadraticBezierTo(centerX, centerY - h * 0.22f, centerX - w * 0.22f, centerY - h * 0.16f)
                        }
                        drawPath(pompadourPath, hairColor)
                    }
                    else -> { // retro
                        // Cool 70s sideburns + curly mop
                        drawCircle(hairColor, radius = w * 0.1f, center = Offset(centerX - w * 0.2f, centerY - h * 0.05f))
                        drawCircle(hairColor, radius = w * 0.12f, center = Offset(centerX + w * 0.18f, centerY - h * 0.18f))
                        drawCircle(hairColor, radius = w * 0.16f, center = Offset(centerX, centerY - h * 0.26f))
                    }
                }
            }

            // --- 6. Studio Neck Headphones (VocaAI Signature Key Visual) ---
            // Draw band behind neck
            val bandBrush = Brush.sweepGradient(
                colors = listOf(accentColor, Color(0xFF1E293B), accentColor)
            )
            drawArc(
                brush = bandBrush,
                startAngle = 10f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(centerX - w * 0.23f, centerY + h * 0.04f),
                size = Size(w * 0.46f, h * 0.32f),
                style = Stroke(12f, cap = StrokeCap.Round)
            )
            // Left/Right Ear cups resting on shoulders
            drawOval(
                color = accentColor,
                topLeft = Offset(centerX - w * 0.3f, centerY + h * 0.2f),
                size = Size(w * 0.14f, h * 0.18f)
            )
            drawOval(
                color = accentColor,
                topLeft = Offset(centerX + w * 0.16f, centerY + h * 0.2f),
                size = Size(w * 0.14f, h * 0.18f)
            )
        }
    }
}

// Custom modifier for avatar breathing neon aura glow
private fun Modifier.drawBehindGlow(scale: Float, glowColor: Color): Modifier = this.drawBehind {
    val radius = (size.minDimension / 2f) * scale
    val brush = Brush.radialGradient(
        colors = listOf(glowColor.copy(alpha = 0.12f), Color.Transparent),
        center = Offset(size.width / 2f, size.height / 2f),
        radius = radius
    )
    drawCircle(
        brush = brush,
        radius = radius,
        center = Offset(size.width / 2f, size.height / 2f)
    )
}
