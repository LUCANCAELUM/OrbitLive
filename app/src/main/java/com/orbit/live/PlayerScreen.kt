package com.orbit.live

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun Equalizer(active: Boolean) {
    val t = rememberInfiniteTransition(label = "eq")
    Row(
        Modifier.height(70.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(16) { i ->
            val h by t.animateFloat(
                0.15f,
                1f,
                infiniteRepeatable(tween(380 + i * 47, easing = LinearEasing), RepeatMode.Reverse),
                label = "b$i"
            )
            Box(
                Modifier
                    .width(6.dp)
                    .fillMaxHeight(if (active) h else 0.12f)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brush.verticalGradient(listOf(Cyan, Violet)))
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(vm: AppVM) {
    val s = vm.current ?: return
    val act = LocalContext.current as Activity
    val view = LocalView.current
    var controls by remember { mutableStateOf(true) }
    var drag by remember { mutableFloatStateOf(0f) }
    var menu by remember { mutableStateOf(false) }

    LaunchedEffect(s.id) { controls = true }
    LaunchedEffect(controls, vm.isPlaying, menu) {
        if (controls && vm.isPlaying && !menu) {
            delay(3500)
            controls = false
        }
    }

    DisposableEffect(Unit) {
        val c = WindowCompat.getInsetsController(act.window, view)
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        c.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            c.show(WindowInsetsCompat.Type.systemBars())
            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) { detectTapGestures { controls = !controls } }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { drag = 0f },
                    onDragEnd = {
                        if (drag < -180) vm.zap(1) else if (drag > 180) vm.zap(-1)
                        drag = 0f
                    },
                    onVerticalDrag = { _, d -> drag += d }
                )
            }
    ) {
        if (!s.radio) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        player = vm.player
                    }
                },
                update = { it.player = vm.player },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.radialGradient(listOf(Color(0xFF3B1F8F), Color(0xFF0B0820)))),
                Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(36.dp))
                            .background(Color(0xFFEDEBFA)),
                        Alignment.Center
                    ) {
                        if (s.logo != null) {
                            AsyncImage(
                                model = s.logo,
                                contentDescription = s.name,
                                modifier = Modifier.fillMaxSize().padding(20.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Icon(
                                Icons.Rounded.Radio,
                                contentDescription = s.name,
                                Modifier.size(80.dp),
                                tint = Violet
                            )
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                    Equalizer(vm.isPlaying)
                }
            }
        }

        if (!vm.inPip) {
            // status overlay
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                when (vm.status) {
                    Status.Loading -> CircularProgressIndicator(color = Cyan)
                    Status.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.player_stream_offline), fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.player_swipe_hint_error),
                            fontSize = 12.sp,
                            color = Color(0x99FFFFFF)
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = { vm.togglePlay() }, modifier = Modifier.testTag("player_retry_button")) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                    else -> {}
                }
            }

            AnimatedVisibility(controls, enter = fadeIn(), exit = fadeOut()) {
                Box(Modifier.fillMaxSize()) {
                    // top bar
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent)))
                            .safeDrawingPadding()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { vm.showPlayer = false },
                            modifier = Modifier.testTag("player_minimize")
                        ) {
                            Icon(
                                Icons.Rounded.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.cd_minimize),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                s.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val c = vm.countries[s.country]
                            Text(
                                listOfNotNull(
                                    c?.let { "${it.flag} ${it.name}" },
                                    "Source ${vm.streamIdx + 1}/${s.urls.size}",
                                    if (vm.sleepMinutes > 0) "😴 ${vm.sleepMinutes}m" else null
                                ).joinToString(" · "),
                                fontSize = 12.sp,
                                color = Color(0xBBFFFFFF),
                                maxLines = 1
                            )
                        }
                        val fav = vm.isFav(s.id)
                        IconButton(
                            onClick = { vm.toggleFav(s) },
                            modifier = Modifier.testTag("player_favorite")
                        ) {
                            Icon(
                                if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = stringResource(R.string.cd_favorite),
                                tint = if (fav) Pink else Color.White
                            )
                        }
                        if (!s.radio) {
                            IconButton(
                                onClick = { (act as? MainActivity)?.enterPip() },
                                modifier = Modifier.testTag("player_pip")
                            ) {
                                Icon(Icons.Rounded.PictureInPictureAlt, contentDescription = stringResource(R.string.cd_pip))
                            }
                            IconButton(
                                onClick = {
                                    act.requestedOrientation = if (landscape) {
                                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                    }
                                },
                                modifier = Modifier.testTag("player_rotate")
                            ) {
                                Icon(Icons.Rounded.ScreenRotation, contentDescription = stringResource(R.string.cd_rotate))
                            }
                        }
                        Box {
                            IconButton(
                                onClick = { menu = true },
                                modifier = Modifier.testTag("player_sleep")
                            ) {
                                Icon(
                                    Icons.Rounded.Bedtime,
                                    contentDescription = stringResource(R.string.cd_sleep_timer),
                                    tint = if (vm.sleepMinutes > 0) Cyan else Color.White
                                )
                            }
                            DropdownMenu(menu, { menu = false }) {
                                listOf(0, 15, 30, 60, 90).forEach { m ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (m == 0) stringResource(R.string.sleep_timer_off)
                                                else "Sleep in $m min"
                                            )
                                        },
                                        onClick = {
                                            vm.setSleep(m)
                                            menu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // bottom bar
                    Column(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xDD000000))))
                            .safeDrawingPadding()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            RoundBtn(
                                icon = Icons.Rounded.SkipPrevious,
                                size = 52.dp,
                                contentDescription = stringResource(R.string.cd_previous_channel),
                                testTag = "player_prev"
                            ) { vm.zap(-1) }
                            RoundBtn(
                                icon = if (vm.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                size = 72.dp,
                                primary = true,
                                contentDescription = stringResource(if (vm.isPlaying) R.string.cd_pause else R.string.cd_play),
                                testTag = "player_play_pause"
                            ) { vm.togglePlay() }
                            RoundBtn(
                                icon = Icons.Rounded.SkipNext,
                                size = 52.dp,
                                contentDescription = stringResource(R.string.cd_next_channel),
                                testTag = "player_next"
                            ) { vm.zap(1) }
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PillBtn(
                                label = stringResource(R.string.auto_surf),
                                icon = Icons.Rounded.Autorenew,
                                active = vm.surf,
                                testTag = "player_auto_surf"
                            ) { vm.toggleSurf() }
                            if (s.urls.size > 1) {
                                PillBtn(
                                    label = stringResource(R.string.player_next_source),
                                    icon = Icons.Rounded.SwapHoriz,
                                    active = false,
                                    testTag = "player_next_source"
                                ) { vm.nextSource() }
                            }
                            PillBtn(
                                label = stringResource(R.string.player_surprise),
                                icon = Icons.Rounded.Shuffle,
                                active = false,
                                testTag = "player_surprise"
                            ) { vm.surprise() }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.player_swipe_hint),
                            fontSize = 11.sp,
                            color = Color(0x88FFFFFF),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundBtn(
    icon: ImageVector,
    size: Dp,
    primary: Boolean = false,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (primary) Brush.linearGradient(listOf(Violet, Pink))
                else Brush.linearGradient(listOf(Color(0x44FFFFFF), Color(0x22FFFFFF)))
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, Modifier.size(size * 0.55f))
    }
}

@Composable
private fun PillBtn(
    label: String,
    icon: ImageVector,
    active: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (active) Brush.horizontalGradient(listOf(Cyan, Violet))
                else Brush.horizontalGradient(listOf(Color(0x33FFFFFF), Color(0x33FFFFFF)))
            )
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
