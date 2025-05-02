package com.example.thryftapp

import android.content.Intent
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.WindowInsets
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.thryftapp.theme.FluidBottomNavigationTheme
import com.example.thryftapp.ui.theme.DEFAULT_PADDING
import kotlin.math.PI
import kotlin.math.sin

@RequiresApi(Build.VERSION_CODES.S)
private fun getRenderEffect(): RenderEffect {
    val blurEffect = RenderEffect.createBlurEffect(80f, 80f, Shader.TileMode.MIRROR) //blur effect

    val alphaMatrix = RenderEffect.createColorFilterEffect(
        ColorMatrixColorFilter(
            ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f, 0f, 0f,
                    0f, 0f, 0f, 50f, -5000f
                )
            )
        )
    )

    return RenderEffect.createChainEffect(alphaMatrix, blurEffect) //combine effects
}

@Composable
fun MainScreen() {
    val isMenuExtended = remember { mutableStateOf(false) } //track fab state

    val fabAnimationProgress by animateFloatAsState(
        targetValue = if (isMenuExtended.value) 1f else 0f,
        animationSpec = tween(1000, easing = LinearEasing)
    )

    val clickAnimationProgress by animateFloatAsState(
        targetValue = if (isMenuExtended.value) 1f else 0f,
        animationSpec = tween(400, easing = LinearEasing)
    )

    val renderEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getRenderEffect().asComposeRenderEffect() //apply blur on supported versions
    } else null

    MainScreen(
        renderEffect = renderEffect,
        fabAnimationProgress = fabAnimationProgress,
        clickAnimationProgress = clickAnimationProgress
    ) {
        isMenuExtended.value = !isMenuExtended.value //toggle state
    }
}

@Composable
fun MainScreen(
    renderEffect: androidx.compose.ui.graphics.RenderEffect?,
    fabAnimationProgress: Float = 0f,
    clickAnimationProgress: Float = 0f,
    toggleAnimation: () -> Unit = {}
) {
    Box(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        CustomBottomNavigation()
        Circle(color = MaterialTheme.colors.primary.copy(alpha = 0.5f), animationProgress = 0.5f)
        FabGroup(renderEffect = renderEffect, animationProgress = fabAnimationProgress)
        FabGroup(renderEffect = null, animationProgress = fabAnimationProgress, toggleAnimation = toggleAnimation)
        Circle(color = Color.White, animationProgress = clickAnimationProgress)
    }
}

@Composable
fun Circle(color: Color, animationProgress: Float) {
    val animationValue = sin(PI * animationProgress).toFloat()

    Box(
        modifier = Modifier
            .padding(DEFAULT_PADDING.dp)
            .size(56.dp)
            .scale(2 - animationValue)
            .border(2.dp, color.copy(alpha = color.alpha * animationValue), CircleShape)
    )
}

@Composable
fun CustomBottomNavigation() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .paint(painterResource(R.drawable.bottom_navigation), contentScale = ContentScale.FillBounds)
            .padding(horizontal = 24.dp)
            .border(0.dp, Color.Transparent),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val context = LocalContext.current

        IconButton(onClick = {
            (context as? NavHostActivity)?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.fragment_container, HomeFragment())
                ?.addToBackStack(null)
                ?.commit()
        }) {
            Icon(painter = painterResource(R.drawable.ic_home), contentDescription = "Home", tint = Color.White, modifier = Modifier.size(30.dp))
        }

        IconButton(onClick = {
            (context as? NavHostActivity)?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.fragment_container, TransactionFragment())
                ?.addToBackStack(null)
                ?.commit()
        }) {
            Icon(painter = painterResource(R.drawable.ic_calendar), contentDescription = "Home", tint = Color.White, modifier = Modifier.size(30.dp))
        }

        Box(modifier = Modifier.size(56.dp)) {} //fab space

        IconButton(onClick = {
            (context as? NavHostActivity)?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.fragment_container, ComingSoonFragment())
                ?.addToBackStack(null)
                ?.commit()
        }) {
            Icon(painter = painterResource(R.drawable.ic_trophy), contentDescription = "Home", tint = Color.White, modifier = Modifier.size(30.dp))
        }

        IconButton(onClick = {
            (context as? NavHostActivity)?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.fragment_container, MenuFragment())
                ?.addToBackStack(null)
                ?.commit()
        }) {
            IconButton(onClick = {
                (context as? NavHostActivity)?.supportFragmentManager?.beginTransaction()
                    ?.replace(R.id.fragment_container, ProfileFragment())
                    ?.addToBackStack(null)
                    ?.commit()
            }) {
                Icon(painter = painterResource(R.drawable.ic_profile), contentDescription = "Home", tint = Color.White, modifier = Modifier.size(30.dp))
            }
        }
    }
}

@Composable
fun FabGroup(
    animationProgress: Float = 0f,
    renderEffect: androidx.compose.ui.graphics.RenderEffect? = null,
    toggleAnimation: () -> Unit = {}
) {
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { this.renderEffect = renderEffect }
            .padding(bottom = DEFAULT_PADDING.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        val context = LocalContext.current

        AnimatedFab(
            icon = Icons.Default.Add,
            modifier = Modifier.padding(
                PaddingValues(bottom = 72.dp, end = 210.dp) * FastOutSlowInEasing.transform(0f, 0.8f, animationProgress)
            ),
            opacity = LinearEasing.transform(0.2f, 0.7f, animationProgress)
        ) {
            val fragment = TransactionDetailsFragment()
            (context as? AppCompatActivity)?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.fragment_container, fragment)
                ?.addToBackStack(null)
                ?.commit()
        }

        AnimatedFab(
            icon = Icons.Default.AutoGraph,
            modifier = Modifier.padding(
                PaddingValues(bottom = 88.dp) * FastOutSlowInEasing.transform(0.1f, 0.9f, animationProgress)
            ),
            opacity = LinearEasing.transform(0.3f, 0.8f, animationProgress)
        ) {
            val fragment = AnalyticsFragment()
            (context as? AppCompatActivity)?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.fragment_container, fragment)
                ?.addToBackStack(null)
                ?.commit()
        }

        AnimatedFab(
            icon = Icons.Default.Chat,
            modifier = Modifier.padding(
                PaddingValues(bottom = 72.dp, start = 210.dp) * FastOutSlowInEasing.transform(0.2f, 1.0f, animationProgress)
            ),
            opacity = LinearEasing.transform(0.4f, 0.9f, animationProgress)
        ) {
            val fragment = ComingSoonFragment()
            (context as? AppCompatActivity)?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.fragment_container, fragment)
                ?.addToBackStack(null)
                ?.commit()
        }

        AnimatedFab(
            modifier = Modifier.scale(1f - LinearEasing.transform(0.5f, 0.85f, animationProgress))
        )

        AnimatedFab(
            icon = Icons.Default.Add,
            modifier = Modifier.rotate(225 * FastOutSlowInEasing.transform(0.35f, 0.65f, animationProgress)),
            backgroundColor = Color.Transparent,
            onClick = toggleAnimation
        )
    }
}

@Composable
fun AnimatedFab(
    modifier: Modifier,
    icon: ImageVector? = null,
    opacity: Float = 1f,
    backgroundColor: Color = MaterialTheme.colors.secondary,
    onClick: () -> Unit = {}
) {
    FloatingActionButton(
        onClick = onClick,
        elevation = FloatingActionButtonDefaults.elevation(0.dp),
        backgroundColor = backgroundColor,
        modifier = modifier.scale(1.25f)
    ) {
        icon?.let {
            Icon(imageVector = it, contentDescription = null, tint = Color.White.copy(alpha = opacity))
        }
    }
}

@Composable
@Preview(device = "id:pixel_4a", showBackground = true, backgroundColor = 0xFF3A2F6E)
private fun MainScreenPreview() {
    FluidBottomNavigationTheme {
        MainScreen()
    }
}
