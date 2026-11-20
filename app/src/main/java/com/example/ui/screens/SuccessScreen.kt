package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.GoldDividerOrnament
import com.example.ui.components.GoldFestivalButton
import com.example.ui.components.MoroccanFestivalBackground
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.IvoryWhite
import com.example.ui.theme.RoyalBlueDeep
import com.example.ui.theme.RoyalBlueSurface
import com.example.ui.theme.SoftSlateText
import kotlinx.coroutines.delay

@Composable
fun SuccessScreen(
    onStartNewSession: () -> Unit
) {
    BackHandler { onStartNewSession() }

    var animateIn by remember { mutableStateOf(false) }
    var autoReturnSeconds by remember { mutableIntStateOf(15) }

    val badgeScale by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "success_scale"
    )

    LaunchedEffect(Unit) {
        animateIn = true
        while (autoReturnSeconds > 0) {
            delay(1000L)
            autoReturnSeconds--
        }
        onStartNewSession()
    }

    MoroccanFestivalBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                shape = RoundedCornerShape(32.dp),
                color = RoyalBlueSurface.copy(alpha = 0.95f),
                border = BorderStroke(1.5.dp, GoldPrimary.copy(alpha = 0.6f)),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = EmeraldContainer,
                        border = BorderStroke(3.dp, EmeraldSuccess),
                        modifier = Modifier
                            .size(112.dp)
                            .scale(badgeScale)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(66.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(R.string.success_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = GoldBright,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.success_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = IvoryWhite,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(22.dp))
                    GoldDividerOrnament()
                    Spacer(modifier = Modifier.height(22.dp))

                    GoldFestivalButton(
                        text = stringResource(R.string.success_new_photo),
                        onClick = onStartNewSession,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_photo_button"),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = null,
                                tint = RoyalBlueDeep
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "سيتم مسح بيانات هذه الجلسة والعودة للشاشة الرئيسية تلقائياً خلال $autoReturnSeconds ثانية",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SoftSlateText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
