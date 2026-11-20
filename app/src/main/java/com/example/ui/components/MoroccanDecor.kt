package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldDeep
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.RoyalBlueDeep
import com.example.ui.theme.RoyalBlueMid
import com.example.ui.theme.RoyalBluePrimary
import java.io.File

@Composable
fun MoroccanFestivalBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(RoyalBlueDeep, RoyalBluePrimary, RoyalBlueMid, RoyalBlueDeep)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 76.dp.toPx()
            val starRadius = 18.dp.toPx()
            val strokeColor = GoldPrimary.copy(alpha = 0.08f)
            val cols = (size.width / step).toInt() + 2
            val rows = (size.height / step).toInt() + 2

            for (r in 0..rows) {
                for (c in 0..cols) {
                    val cx = c * step
                    val cy = r * step
                    val side = starRadius * 1.414f
                    val topLeft = Offset(cx - side / 2f, cy - side / 2f)
                    val rectSize = Size(side, side)
                    drawRect(
                        color = strokeColor,
                        topLeft = topLeft,
                        size = rectSize,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    rotate(degrees = 45f, pivot = Offset(cx, cy)) {
                        drawRect(
                            color = strokeColor,
                            topLeft = topLeft,
                            size = rectSize,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }
            }
        }
        content()
    }
}

@Composable
fun EventLogoBadge(
    customLogoPath: String?,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(RoyalBlueDeep)
            .border(
                width = 3.dp,
                brush = Brush.linearGradient(listOf(GoldBright, GoldPrimary, GoldDeep)),
                shape = CircleShape
            )
            .padding(4.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!customLogoPath.isNullOrBlank() && File(customLogoPath).exists()) {
            AsyncImage(
                model = File(customLogoPath),
                contentDescription = "شعار ملتقى التوجيه المدرسي والمهني",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.img_event_logo),
                contentDescription = "شعار ملتقى التوجيه المدرسي والمهني",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun GoldFestivalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(60.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = RoyalBlueDeep,
            disabledContainerColor = GoldPrimary.copy(alpha = 0.3f),
            disabledContentColor = RoyalBlueDeep.copy(alpha = 0.6f)
        ),
        contentPadding = PaddingValues(0.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (enabled) {
                        Brush.horizontalGradient(listOf(GoldPrimary, GoldBright, GoldPrimary))
                    } else {
                        Brush.horizontalGradient(listOf(GoldPrimary.copy(alpha = 0.35f), GoldPrimary.copy(alpha = 0.35f)))
                    },
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun GoldDividerOrnament(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(listOf(Color.Transparent, GoldPrimary.copy(alpha = 0.8f)))
                )
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .size(10.dp)
                .background(GoldBright, shape = RoundedCornerShape(3.dp))
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(listOf(GoldPrimary.copy(alpha = 0.8f), Color.Transparent))
                )
        )
    }
}

@Composable
fun KioskInactivityPill(
    remainingSeconds: Int?,
    modifier: Modifier = Modifier
) {
    if (remainingSeconds == null || remainingSeconds > 20) return
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = RoyalBlueDeep.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = null,
                tint = GoldBright,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "العودة التلقائية للرئيسية خلال $remainingSeconds ث",
                style = MaterialTheme.typography.labelMedium,
                color = GoldBright
            )
        }
    }
}
