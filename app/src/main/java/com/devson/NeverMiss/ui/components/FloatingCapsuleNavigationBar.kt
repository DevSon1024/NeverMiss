package com.devson.NeverMiss.ui.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devson.NeverMiss.navigation.Screen
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.roundToInt

enum class NavDestination(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val contentDescription: String
) {
    HOME(Screen.Home.route, "Home", Icons.Filled.Home, Icons.Outlined.Home, "Home"),
    APP_STREAKS(Screen.AppStreaksDashboard.route, "Apps", Icons.Filled.Apps, Icons.Outlined.Apps, "App Streaks"),
    INTERVAL_TIMERS(Screen.IntervalTimers.route, "Timers", Icons.Filled.HourglassBottom, Icons.Outlined.HourglassBottom, "Interval Timers"),
    SETTINGS(Screen.Settings.route, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "Settings")
}

private const val SHARED_LIQUID_BAR_SHADER_SRC = """
    uniform shader content;
    uniform float2 resolution;
    uniform float isDark;

    half4 main(float2 fragCoord) {
        float2 size = max(resolution, float2(1.0));
        float2 uv = fragCoord / size;
        float radius = size.y * 0.5;
        float2 center = size * 0.5;
        float2 p = fragCoord - center;
        float2 halfBox = max(size * 0.5 - float2(radius), float2(0.0));
        float2 q = abs(p) - halfBox;
        float dist = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;

        float edgeBand = smoothstep(-16.0, 0.0, dist);
        float2 normalDir = length(p) > 0.001 ? normalize(p) : float2(0.0, 0.0);
        float2 sampleCoord = clamp(fragCoord - normalDir * edgeBand * 6.0, float2(0.0), size - float2(1.0));
        half4 base = content.eval(sampleCoord);

        float luma = dot(base.rgb, half3(0.299, 0.587, 0.114));
        half3 saturated = mix(half3(luma), base.rgb, 1.25);
        half3 glassTint = isDark > 0.5 ? half3(0.09, 0.10, 0.13) : half3(0.96, 0.97, 0.99);
        float tintAlpha = isDark > 0.5 ? 0.44 : 0.54;
        half3 result = mix(saturated, glassTint, tintAlpha);

        float rim = smoothstep(-2.2, -0.3, dist) * (1.0 - smoothstep(-0.3, 0.8, dist));
        float topLight = clamp(1.0 - uv.y * 1.15, 0.0, 1.0);
        float bottomLight = clamp((uv.y - 0.55) * 1.6, 0.0, 1.0);
        float rimStrength = rim * (0.36 * topLight + 0.14 * bottomLight);
        result += half3(rimStrength);

        return half4(result, base.a);
    }
"""

private const val LIQUID_CAPSULE_SHADER_SRC = """
    uniform float2 resolution;
    uniform float dragFraction;
    uniform float isDark;

    half4 main(float2 fragCoord) {
        float2 size = max(resolution, float2(1.0));
        float2 uv = fragCoord / size;
        float radius = size.y * 0.5;
        float2 center = size * 0.5;
        float2 p = fragCoord - center;
        float2 halfBox = max(size * 0.5 - float2(radius), float2(0.0));
        float2 q = abs(p) - halfBox;
        float dist = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;

        float topHighlight = pow(clamp(1.0 - uv.y, 0.0, 1.0), 1.6);
        float edgeGlow = smoothstep(-10.0, 0.0, dist);
        float centerSheen = exp(-pow((uv.x - 0.5) * 1.9, 2.0)) * topHighlight;

        half3 baseCol = isDark > 0.5 ? half3(0.90, 0.93, 1.0) : half3(1.0, 1.0, 1.0);
        float baseAlpha = isDark > 0.5
            ? (0.14 + 0.08 * topHighlight + 0.06 * edgeGlow + 0.08 * dragFraction)
            : (0.42 + 0.20 * topHighlight + 0.12 * edgeGlow + 0.10 * dragFraction);

        float finalAlpha = clamp(baseAlpha + centerSheen * (0.08 + 0.06 * dragFraction), 0.0, 0.90);
        return half4(baseCol * finalAlpha, finalAlpha);
    }
"""

@Composable
private fun rememberLiquidGlassBorderBrush(
    isDark: Boolean,
    liquidGlassEnabled: Boolean
): Brush {
    val solidBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    return remember(isDark, liquidGlassEnabled, solidBorderColor) {
        if (!liquidGlassEnabled) {
            Brush.verticalGradient(listOf(solidBorderColor, solidBorderColor))
        } else {
            Brush.verticalGradient(
                colors = if (isDark) {
                    listOf(
                        Color.White.copy(alpha = 0.30f),
                        Color.White.copy(alpha = 0.08f)
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = 0.75f),
                        Color.Black.copy(alpha = 0.08f)
                    )
                }
            )
        }
    }
}

@Composable
private fun BoxScope.LiquidGlassBarBackground(
    liquidGlassEnabled: Boolean,
    isDark: Boolean,
    backdropLayer: GraphicsLayer?,
    pillOffsetInRoot: Offset,
    containerOffsetInRoot: Offset,
    pillSizePx: IntSize
) {
    val density = LocalDensity.current
    val blurRadiusPx = with(density) { 24.dp.toPx() }

    val barShader = remember(liquidGlassEnabled) {
        if (liquidGlassEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { RuntimeShader(SHARED_LIQUID_BAR_SHADER_SRC) }.getOrNull()
        } else {
            null
        }
    }

    val backdropRenderEffect = remember(liquidGlassEnabled, barShader, blurRadiusPx, pillSizePx, isDark) {
        if (liquidGlassEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                val blurEffect = RenderEffect.createBlurEffect(
                    blurRadiusPx,
                    blurRadiusPx,
                    Shader.TileMode.CLAMP
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    barShader != null &&
                    pillSizePx.width > 0 &&
                    pillSizePx.height > 0
                ) {
                    barShader.setFloatUniform(
                        "resolution",
                        pillSizePx.width.toFloat(),
                        pillSizePx.height.toFloat()
                    )
                    barShader.setFloatUniform("isDark", if (isDark) 1f else 0f)
                    val shaderEffect = RenderEffect.createRuntimeShaderEffect(barShader, "content")
                    RenderEffect.createChainEffect(shaderEffect, blurEffect).asComposeRenderEffect()
                } else {
                    blurEffect.asComposeRenderEffect()
                }
            }.getOrNull()
        } else {
            null
        }
    }

    val fallbackBarTint = if (!liquidGlassEnabled) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f)
    }

    if (liquidGlassEnabled && backdropLayer != null && backdropRenderEffect != null) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    renderEffect = backdropRenderEffect
                    clip = true
                }
                .drawBehind {
                    if (backdropLayer.size.width > 0 && backdropLayer.size.height > 0) {
                        val relX = pillOffsetInRoot.x - containerOffsetInRoot.x
                        val relY = pillOffsetInRoot.y - containerOffsetInRoot.y
                        translate(left = -relX, top = -relY) {
                            drawLayer(backdropLayer)
                        }
                    }
                    drawRect(
                        color = if (isDark) {
                            Color.Black.copy(alpha = 0.14f)
                        } else {
                            Color.White.copy(alpha = 0.18f)
                        }
                    )
                }
        )
    } else {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(fallbackBarTint)
        )
    }
}

@Composable
fun FloatingCapsuleNavigationBar(
    currentRoute: String,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier,
    liquidGlassEnabled: Boolean = false,
    backdropLayer: GraphicsLayer? = null,
    containerOffsetInRoot: Offset = Offset.Zero
) {
    val haptic = LocalHapticFeedback.current
    var lastClickTime by remember { mutableLongStateOf(0L) }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val items = remember { NavDestination.entries }

    val activeIndex = items.indexOfFirst { it.route == currentRoute }
    val currentActiveIndex by rememberUpdatedState(activeIndex)
    val currentOnNavigate by rememberUpdatedState(onNavigateToRoute)
    var targetIndex by remember {
        mutableIntStateOf(if (activeIndex >= 0) activeIndex else 0)
    }
    var movingForward by remember { mutableStateOf(true) }
    var isDragging by remember { mutableStateOf(false) }
    var dragCenterPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentRoute) {
        if (!isDragging && activeIndex >= 0 && activeIndex != targetIndex) {
            movingForward = activeIndex > targetIndex
            targetIndex = activeIndex
        }
    }

    var barWidthPx by remember { mutableIntStateOf(0) }
    var barHeightPx by remember { mutableIntStateOf(0) }
    var pillOffsetInRoot by remember { mutableStateOf(Offset.Zero) }
    var pillSizePx by remember { mutableStateOf(IntSize.Zero) }

    val tabWidthPx = if (items.isNotEmpty() && barWidthPx > 0) {
        barWidthPx.toFloat() / items.size
    } else {
        0f
    }

    val leftTargetPx = if (isDragging && tabWidthPx > 0f) {
        (dragCenterPx - tabWidthPx / 2f).coerceIn(0f, (barWidthPx - tabWidthPx).coerceAtLeast(0f))
    } else {
        tabWidthPx * targetIndex
    }
    val rightTargetPx = if (isDragging && tabWidthPx > 0f) {
        leftTargetPx + tabWidthPx
    } else {
        tabWidthPx * (targetIndex + 1)
    }

    val leadingSpring = remember {
        spring<Float>(
            dampingRatio = 0.76f,
            stiffness = 1600f
        )
    }
    val trailingSpring = remember {
        spring<Float>(
            dampingRatio = 0.82f,
            stiffness = 1100f
        )
    }
    val dragTrackingSpring = remember {
        spring<Float>(
            dampingRatio = 0.88f,
            stiffness = 3600f
        )
    }

    val animatedLeftPx by animateFloatAsState(
        targetValue = leftTargetPx,
        animationSpec = when {
            isDragging -> dragTrackingSpring
            movingForward -> trailingSpring
            else -> leadingSpring
        },
        label = "liquid_pill_left"
    )

    val animatedRightPx by animateFloatAsState(
        targetValue = rightTargetPx,
        animationSpec = when {
            isDragging -> dragTrackingSpring
            movingForward -> leadingSpring
            else -> trailingSpring
        },
        label = "liquid_pill_right"
    )

    val dragFraction by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.70f, stiffness = 900f),
        label = "liquid_pill_drag_lift"
    )

    val barBorderBrush = rememberLiquidGlassBorderBrush(
        isDark = isDark,
        liquidGlassEnabled = liquidGlassEnabled
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = if (liquidGlassEnabled) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, barBorderBrush),
            tonalElevation = if (liquidGlassEnabled) 0.dp else 6.dp,
            shadowElevation = if (liquidGlassEnabled) 12.dp else 10.dp,
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    pillOffsetInRoot = coords.positionInRoot()
                    pillSizePx = coords.size
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
            ) {
                LiquidGlassBarBackground(
                    liquidGlassEnabled = liquidGlassEnabled,
                    isDark = isDark,
                    backdropLayer = backdropLayer,
                    pillOffsetInRoot = pillOffsetInRoot,
                    containerOffsetInRoot = containerOffsetInRoot,
                    pillSizePx = pillSizePx
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .pointerInput(items.size, barWidthPx) {
                            if (barWidthPx <= 0 || tabWidthPx <= 0f) return@pointerInput
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val downX = down.position.x
                                val downY = down.position.y
                                val downIndex = (downX / tabWidthPx).toInt().coerceIn(0, items.lastIndex)
                                val longPressTimeout = viewConfiguration.longPressTimeoutMillis.coerceAtMost(220L)
                                val touchSlop = viewConfiguration.touchSlop

                                var isScrubbing = false
                                var pointerReleased = false
                                var currentHoverIndex = downIndex

                                withTimeoutOrNull(longPressTimeout) {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id }
                                        if (change == null || !change.pressed) {
                                            pointerReleased = true
                                            change?.consume()
                                            break
                                        }
                                        val dx = abs(change.position.x - downX)
                                        val dy = abs(change.position.y - downY)
                                        if (dx > touchSlop && dx >= dy) {
                                            isScrubbing = true
                                            isDragging = true
                                            dragCenterPx = change.position.x.coerceIn(
                                                tabWidthPx / 2f,
                                                (barWidthPx - tabWidthPx / 2f).coerceAtLeast(tabWidthPx / 2f)
                                            )
                                            currentHoverIndex = (change.position.x / tabWidthPx).toInt()
                                                .coerceIn(0, items.lastIndex)
                                            targetIndex = currentHoverIndex
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            change.consume()
                                            break
                                        }
                                    }
                                }

                                if (pointerReleased) {
                                    val now = System.currentTimeMillis()
                                    if (downIndex != targetIndex && now - lastClickTime > 150L) {
                                        lastClickTime = now
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        movingForward = downIndex > targetIndex
                                        targetIndex = downIndex
                                        currentOnNavigate(items[downIndex].route)
                                    }
                                    return@awaitEachGesture
                                }

                                if (!isScrubbing) {
                                    isScrubbing = true
                                    isDragging = true
                                    dragCenterPx = downX.coerceIn(
                                        tabWidthPx / 2f,
                                        (barWidthPx - tabWidthPx / 2f).coerceAtLeast(tabWidthPx / 2f)
                                    )
                                    currentHoverIndex = downIndex
                                    targetIndex = downIndex
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                    if (change == null) {
                                        isDragging = false
                                        if (currentActiveIndex >= 0) targetIndex = currentActiveIndex
                                        break
                                    }
                                    if (!change.pressed) {
                                        change.consume()
                                        isDragging = false
                                        val finalIndex = currentHoverIndex
                                        lastClickTime = System.currentTimeMillis()
                                        if (finalIndex != currentActiveIndex) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            movingForward = finalIndex > currentActiveIndex
                                            targetIndex = finalIndex
                                            currentOnNavigate(items[finalIndex].route)
                                        } else {
                                            targetIndex = finalIndex
                                        }
                                        break
                                    }

                                    change.consume()
                                    val clampedX = change.position.x.coerceIn(
                                        tabWidthPx / 2f,
                                        (barWidthPx - tabWidthPx / 2f).coerceAtLeast(tabWidthPx / 2f)
                                    )
                                    dragCenterPx = clampedX
                                    val nextIndex = (change.position.x / tabWidthPx).toInt()
                                        .coerceIn(0, items.lastIndex)
                                    if (nextIndex != currentHoverIndex) {
                                        currentHoverIndex = nextIndex
                                        targetIndex = nextIndex
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            }
                        }
                ) {
                    LiquidIndicatorPill(
                        leftPx = animatedLeftPx,
                        rightPx = animatedRightPx,
                        barHeightPx = barHeightPx,
                        tabWidthPx = tabWidthPx,
                        dragFraction = dragFraction,
                        isDark = isDark,
                        liquidGlassEnabled = liquidGlassEnabled
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onSizeChanged { size ->
                                barWidthPx = size.width
                                barHeightPx = size.height
                            },
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items.forEachIndexed { index, item ->
                            val isSelected = (index == targetIndex)
                            CapsuleNavItem(
                                item = item,
                                isSelected = isSelected,
                                isScrubbing = isDragging && isSelected,
                                liquidGlassEnabled = liquidGlassEnabled,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidIndicatorPill(
    leftPx: Float,
    rightPx: Float,
    barHeightPx: Int,
    tabWidthPx: Float,
    dragFraction: Float,
    isDark: Boolean,
    liquidGlassEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (tabWidthPx <= 0f || barHeightPx <= 0) return

    val density = LocalDensity.current
    val minWidthPx = tabWidthPx * 0.45f
    val currentWidthPx = maxOf(rightPx - leftPx, minWidthPx)
    val widthDp = with(density) { currentWidthPx.toDp() }
    val heightDp = with(density) { barHeightPx.toDp() }

    val stretchFactor = ((currentWidthPx - tabWidthPx) / tabWidthPx).coerceIn(0f, 1f)
    val liftScale = 1f + (0.07f * dragFraction)
    val scaleY = (1f - (stretchFactor * 0.10f)) * liftScale
    val scaleX = (1f + (stretchFactor * 0.03f)) * liftScale

    val capsuleShader = remember(liquidGlassEnabled) {
        if (liquidGlassEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { RuntimeShader(LIQUID_CAPSULE_SHADER_SRC) }.getOrNull()
        } else {
            null
        }
    }

    val fallbackBrush = remember(isDark, dragFraction) {
        Brush.verticalGradient(
            colors = if (isDark) {
                listOf(
                    Color.White.copy(alpha = 0.24f + 0.08f * dragFraction),
                    Color.White.copy(alpha = 0.10f + 0.05f * dragFraction)
                )
            } else {
                listOf(
                    Color.White.copy(alpha = 0.65f + 0.12f * dragFraction),
                    Color.White.copy(alpha = 0.35f + 0.10f * dragFraction)
                )
            }
        )
    }

    val solidPillColor = MaterialTheme.colorScheme.secondaryContainer
    val solidPillBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val rimBrush = remember(isDark, dragFraction, liquidGlassEnabled, solidPillBorderColor) {
        if (!liquidGlassEnabled) {
            Brush.verticalGradient(listOf(solidPillBorderColor, solidPillBorderColor))
        } else {
            Brush.verticalGradient(
                colors = if (isDark) {
                    listOf(
                        Color.White.copy(alpha = 0.48f + 0.18f * dragFraction),
                        Color.White.copy(alpha = 0.12f)
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = 0.85f),
                        Color.Black.copy(alpha = 0.08f)
                    )
                }
            )
        }
    }

    Box(
        modifier = modifier
            .offset { IntOffset(x = leftPx.roundToInt(), y = 0) }
            .width(widthDp)
            .height(heightDp)
            .graphicsLayer {
                this.scaleY = scaleY
                this.scaleX = scaleX
            }
            .padding(horizontal = 2.dp)
            .clip(CircleShape)
            .drawBehind {
                if (!liquidGlassEnabled) {
                    drawRect(color = solidPillColor)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && capsuleShader != null) {
                    capsuleShader.setFloatUniform("resolution", size.width, size.height)
                    capsuleShader.setFloatUniform("dragFraction", dragFraction)
                    capsuleShader.setFloatUniform("isDark", if (isDark) 1f else 0f)
                    drawRect(brush = ShaderBrush(capsuleShader))
                } else {
                    drawRect(brush = fallbackBrush)
                }
            }
            .border(
                width = if (liquidGlassEnabled) (0.75 + 0.35 * dragFraction).dp else 0.5.dp,
                brush = rimBrush,
                shape = CircleShape
            )
    )
}

@Composable
private fun CapsuleNavItem(
    item: NavDestination,
    isSelected: Boolean,
    isScrubbing: Boolean,
    liquidGlassEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val selectionFraction by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.80f, stiffness = 1400f),
        label = "nav_item_selection"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) {
            if (liquidGlassEnabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            }
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = spring(dampingRatio = 0.80f, stiffness = 1400f),
        label = "nav_icon_color"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = spring(dampingRatio = 0.80f, stiffness = 1400f),
        label = "nav_text_color"
    )

    val scale = 1f + (0.04f * selectionFraction) + (if (isScrubbing) 0.04f else 0f)

    Box(
        modifier = modifier
            .scale(scale)
            .padding(horizontal = 2.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(22.dp),
                contentAlignment = Alignment.Center
            ) {
                if (selectionFraction < 0.99f) {
                    Icon(
                        imageVector = item.unselectedIcon,
                        contentDescription = item.contentDescription,
                        tint = iconColor,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = 1f - selectionFraction
                                scaleX = 1f - (0.08f * selectionFraction)
                                scaleY = 1f - (0.08f * selectionFraction)
                            }
                    )
                }
                if (selectionFraction > 0.01f) {
                    Icon(
                        imageVector = item.selectedIcon,
                        contentDescription = item.contentDescription,
                        tint = iconColor,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = selectionFraction
                                scaleX = 0.92f + (0.08f * selectionFraction)
                                scaleY = 0.92f + (0.08f * selectionFraction)
                            }
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.title,
                fontSize = 11.sp,
                fontWeight = if (selectionFraction >= 0.5f) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                maxLines = 1
            )
        }
    }
}
