package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.SunburstBackground
import com.example.ui.theme.LocalAppStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    val strings = LocalAppStrings.current
    val logoScale = remember { Animatable(0.4f) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1250, easing = FastOutSlowInEasing)
        )
        delay(120L)
        onFinished()
    }

    SunburstBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .testTag("splash_screen")
                .clickable(onClick = onFinished),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(144.dp)
                        .scale(logoScale.value)
                        .shadow(18.dp, RoundedCornerShape(36.dp))
                        .clip(RoundedCornerShape(36.dp))
                        .border(4.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(36.dp))
                ) {
                    Image(
                        painter = painterResource(R.drawable.arrow_puzzle_icon_fg_1795419274900),
                        contentDescription = stringResource(R.string.cd_arrow_logo),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = strings.appTitle.uppercase(),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = strings.tagline,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.92f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(42.dp))

                LinearProgressIndicator(
                    progress = { progress.value },
                    modifier = Modifier
                        .width(210.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.25f)),
                    color = Color(0xFFFFD600),
                    trackColor = Color.White.copy(alpha = 0.28f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "500 Handcrafted Solvable Levels",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}
