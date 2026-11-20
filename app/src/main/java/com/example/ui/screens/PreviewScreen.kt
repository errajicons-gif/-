package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.components.GoldFestivalButton
import com.example.ui.components.KioskInactivityPill
import com.example.ui.components.MoroccanFestivalBackground
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.IvoryWhite
import com.example.ui.theme.RoyalBlueDeep
import com.example.ui.theme.RoyalBlueMid
import com.example.ui.theme.RoyalBlueSurface
import com.example.ui.theme.SoftSlateText
import java.io.File

@Composable
fun PreviewScreen(
    compositedPhotoPath: String?,
    inactivityRemainingSeconds: Int?,
    onRetake: () -> Unit,
    onApproveAndProceed: () -> Unit
) {
    BackHandler { onRetake() }

    MoroccanFestivalBackground {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            val isWide = maxWidth > maxHeight && maxWidth >= 680.dp

            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        PhotoPreviewCard(
                            compositedPhotoPath = compositedPhotoPath,
                            modifier = Modifier.fillMaxHeight()
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .weight(0.8f)
                            .widthIn(max = 420.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = RoyalBlueSurface.copy(alpha = 0.94f),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            KioskInactivityPill(remainingSeconds = inactivityRemainingSeconds)
                            Text(
                                text = stringResource(R.string.preview_title),
                                style = MaterialTheme.typography.headlineMedium,
                                color = IvoryWhite,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "تأكد من وضوح صورتك داخل الإطار الرسمي قبل إرسالها إلى بريدك الإلكتروني.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SoftSlateText,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            GoldFestivalButton(
                                text = stringResource(R.string.preview_approve),
                                onClick = onApproveAndProceed,
                                enabled = compositedPhotoPath != null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("approve_photo_button"),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = RoyalBlueDeep
                                    )
                                }
                            )
                            OutlinedButton(
                                onClick = onRetake,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("retake_photo_button"),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, GoldPrimary.copy(alpha = 0.65f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = RoyalBlueMid
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    tint = GoldBright
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.preview_retake),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = IvoryWhite
                                )
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.preview_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = GoldBright
                        )
                        KioskInactivityPill(remainingSeconds = inactivityRemainingSeconds)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        PhotoPreviewCard(
                            compositedPhotoPath = compositedPhotoPath,
                            modifier = Modifier.fillMaxHeight()
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 560.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GoldFestivalButton(
                            text = stringResource(R.string.preview_approve),
                            onClick = onApproveAndProceed,
                            enabled = compositedPhotoPath != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("approve_photo_button"),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = RoyalBlueDeep
                                )
                            }
                        )
                        OutlinedButton(
                            onClick = onRetake,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("retake_photo_button"),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, GoldPrimary.copy(alpha = 0.65f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = RoyalBlueSurface
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                tint = GoldBright,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.preview_retake),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = IvoryWhite
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoPreviewCard(
    compositedPhotoPath: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = RoyalBlueDeep,
        border = BorderStroke(2.dp, GoldPrimary),
        shadowElevation = 14.dp
    ) {
        if (!compositedPhotoPath.isNullOrBlank()) {
            AsyncImage(
                model = File(compositedPhotoPath),
                contentDescription = "معاينة الصورة النهائية مع الإطار",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .padding(4.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد صورة للمعاينة حالياً.",
                    color = SoftSlateText,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
