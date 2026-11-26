package com.orbit.live

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

private val glass = Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x0FFFFFFF)))
private val brand = Brush.horizontalGradient(listOf(Cyan, Violet, Pink))

private val categories = listOf(
    "news", "sports", "movies", "music", "kids", "entertainment", "documentary", "general", "comedy", "series",
    "science", "education", "lifestyle", "cooking", "travel", "religious", "weather", "business", "culture",
    "animation", "family", "outdoor", "relax", "shop", "public", "legislative", "classic", "auto"
)

private val genres = listOf(
    "pop", "rock", "news", "talk", "jazz", "classical", "dance", "electronic",
    "hip hop", "country", "latin", "chillout", "oldies", "reggae", "metal", "religion"
)

@Composable
fun Root(vm: AppVM) {
    BackHandler(vm.showPlayer) { vm.showPlayer = false }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF160C3A), cBg, cBg)))) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (vm.tab) {
                    0 -> TvScreen(vm)
                    1 -> RadioScreen(vm)
                    2 -> TrendingScreen(vm)
                    else -> LibraryScreen(vm)
                }
            }
            if (vm.current != null && !vm.showPlayer) MiniPlayer(vm)
            BottomBar(vm)
        }
        AnimatedVisibility(
            vm.showPlayer,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            PlayerScreen(vm)
        }
    }
}

// ───────────── building blocks ─────────────
@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Brush.horizontalGradient(listOf(Violet, Pink))
    else Brush.horizontalGradient(listOf(Color(0x22FFFFFF), Color(0x22FFFFFF)))
    Text(
        label,
        color = Color.White,
        fontSize = 13.sp,
        maxLines = 1,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    )
}

@Composable
fun LiveBadge() {
    val t = rememberInfiniteTransition(label = "live")
    val a by t.animateFloat(0.3f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "a")
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xCC000000))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).alpha(a).clip(CircleShape).background(Pink))
        Spacer(Modifier.width(4.dp))
        Text(stringResource(R.string.live_badge), fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
    }
}

@Composable
fun SearchField(
    value: String,
    hint: String,
    testTag: String,
    onChange: (String) -> Unit
) {
    TextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        placeholder = { Text(hint, color = Color(0x99FFFFFF)) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.cd_search), tint = Color(0xBBFFFFFF)) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_clear_search))
                }
            }
        },
        shape = RoundedCornerShape(18.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0x22FFFFFF),
            unfocusedContainerColor = Color(0x1AFFFFFF),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = Cyan
        ),
        modifier = Modifier.fillMaxWidth().testTag(testTag)
    )
}

@Composable
fun StationCard(vm: AppVM, s: Station, list: List<Station>) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .clip(shape)
            .background(glass)
            .border(1.dp, Color(0x22FFFFFF), shape)
            .clickable { vm.play(s, list) }
            .testTag("station_card_${s.id}")
            .padding(10.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFEDEBFA)),
            contentAlignment = Alignment.Center
        ) {
            if (s.logo != null) {
                AsyncImage(
                    model = s.logo,
                    contentDescription = s.name,
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(s.name.take(2).uppercase(), fontSize = 26.sp, fontWeight = FontWeight.Black, color = Violet)
            }
            Box(Modifier.align(Alignment.TopStart).padding(6.dp)) { LiveBadge() }
            val fav = vm.isFav(s.id)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { vm.toggleFav(s) }
                    .testTag("favorite_button_${s.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = stringResource(R.string.cd_favorite),
                    tint = if (fav) Pink else Color(0xFF6B6690),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(s.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        val flag = vm.countries[s.country]?.flag ?: ""
        val sub = if (s.radio) {
            listOf(flag + " " + s.country, s.categories.firstOrNull() ?: "", s.sub)
        } else {
            listOf(flag + " " + (vm.countries[s.country]?.name ?: s.country), s.categories.firstOrNull() ?: "")
        }
        Text(
            sub.filter { it.isNotBlank() }.joinToString(" · "),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 11.sp,
            color = Color(0x99FFFFFF)
        )
    }
}

private fun LazyGridScope.full(content: @Composable LazyGridItemScope.() -> Unit) =
    item(span = { GridItemSpan(maxLineSpan) }, content = content)

@Composable
private fun Title(text: String, sub: String) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(text, style = TextStyle(brush = brand, fontSize = 32.sp, fontWeight = FontWeight.Black))
        Text(sub, fontSize = 13.sp, color = Color(0x99FFFFFF))
    }
}

@Composable
private fun ActionPill(
    label: String,
    icon: ImageVector,
    active: Boolean = false,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) Brush.horizontalGradient(listOf(Cyan, Violet)) else glass)
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RecentRow(vm: AppVM) {
    if (vm.recents.isEmpty()) return
    Column {
        Text(stringResource(R.string.continue_playing), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(vm.recents, key = { it.id }) { s ->
                Column(
                    Modifier.width(88.dp).clickable { vm.play(s, vm.recents) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEDEBFA))
                            .border(2.dp, Violet, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (s.logo != null) {
                            AsyncImage(
                                model = s.logo,
                                contentDescription = s.name,
                                modifier = Modifier.fillMaxSize().padding(10.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text(s.name.take(2), color = Violet, fontWeight = FontWeight.Black)
                        }
                    }
                    Text(
                        s.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

// ───────────── screens ─────────────
private val gridPad = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp)

@Composable
fun TvScreen(vm: AppVM) {
    val byCount = vm.countryOrder
    val q = vm.query.trim()
    val list = remember(vm.tv, q, vm.country, vm.category) {
        vm.tv.filter {
            (vm.country == null || it.country == vm.country) &&
                (vm.category == null || vm.category in it.categories) &&
                (q.isBlank() || it.name.contains(q, true))
        }
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = gridPad,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        full {
            Title(
                stringResource(R.string.app_name),
                "${"%,d".format(vm.tv.size)} live TV channels · ${byCount.size} countries"
            )
        }
        full { SearchField(vm.query, stringResource(R.string.search_channels_hint), "tv_search_field") { vm.query = it } }
        full {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionPill(stringResource(R.string.surprise_me), Icons.Rounded.Shuffle, testTag = "tv_surprise_button") {
                    vm.tab = 0
                    vm.surprise()
                }
                ActionPill(stringResource(R.string.auto_surf), Icons.Rounded.Autorenew, vm.surf, testTag = "tv_auto_surf_button") {
                    vm.toggleSurf()
                    if (vm.surf) vm.surprise()
                }
            }
        }
        full {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Chip(stringResource(R.string.all_countries), vm.country == null) { vm.country = null } }
                items(byCount) { c ->
                    Chip(vm.countries[c]?.let { "${it.flag} ${it.name}" } ?: c, vm.country == c) { vm.country = c }
                }
            }
        }
        full {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Chip(stringResource(R.string.all_genres), vm.category == null) { vm.category = null } }
                items(categories) { c ->
                    Chip(c.replaceFirstChar { it.uppercase() }, vm.category == c) { vm.category = c }
                }
            }
        }
        if (q.isBlank() && vm.country == null && vm.category == null) full { RecentRow(vm) }
        when {
            vm.loading -> full {
                Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Cyan)
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.loading_channels))
                    }
                }
            }
            vm.error != null && vm.tv.isEmpty() -> full {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.error_load_channels), fontWeight = FontWeight.Bold)
                    Text(vm.error ?: "", fontSize = 12.sp, color = Color(0x99FFFFFF))
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { vm.loadTv() }, modifier = Modifier.testTag("tv_retry_button")) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
            list.isEmpty() -> full {
                Text(stringResource(R.string.no_channels_match), Modifier.padding(32.dp), color = Color(0x99FFFFFF))
            }
            else -> items(list, key = { it.id }, contentType = { "station" }) { StationCard(vm, it, list) }
        }
    }
}

@Composable
fun RadioScreen(vm: AppVM) {
    LaunchedEffect(vm.radioQuery, vm.radioCountry, vm.radioTag) {
        val key = "${vm.radioCountry.orEmpty()}|${vm.radioTag.orEmpty()}|${vm.radioQuery.trim()}"
        if (key != vm.lastRadioFilterKey || vm.radio.isEmpty()) {
            if (vm.radioQuery.isNotBlank()) delay(350)
            vm.loadRadio()
        }
    }
    val cl = remember(vm.countries) { vm.countries.entries.sortedBy { it.value.name } }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = gridPad,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        full { Title(stringResource(R.string.radio_title), stringResource(R.string.radio_subtitle)) }
        full { SearchField(vm.radioQuery, stringResource(R.string.search_stations_hint), "radio_search_field") { vm.radioQuery = it } }
        full {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionPill(stringResource(R.string.surprise_me), Icons.Rounded.Shuffle, testTag = "radio_surprise_button") {
                    vm.tab = 1
                    vm.surprise()
                }
            }
        }
        full {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Chip(stringResource(R.string.worldwide_countries), vm.radioCountry == null) { vm.radioCountry = null } }
                items(cl) { e ->
                    Chip("${e.value.flag} ${e.value.name}", vm.radioCountry == e.key) { vm.radioCountry = e.key }
                }
            }
        }
        full {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Chip(stringResource(R.string.top_genres), vm.radioTag == null) { vm.radioTag = null } }
                items(genres) { g ->
                    Chip(g.replaceFirstChar { it.uppercase() }, vm.radioTag == g) { vm.radioTag = g }
                }
            }
        }
        when {
            vm.radioLoading && vm.radio.isEmpty() -> full {
                Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) {
                    CircularProgressIndicator(color = Cyan)
                }
            }
            vm.radioError != null && vm.radio.isEmpty() -> full {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.error_load_stations))
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { vm.loadRadio(force = true) }, modifier = Modifier.testTag("radio_retry_button")) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
            else -> items(vm.radio, key = { it.id }, contentType = { "station" }) { StationCard(vm, it, vm.radio) }
        }
    }
}

@Composable
fun LibraryScreen(vm: AppVM) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = gridPad,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        full { Title(stringResource(R.string.favorites_title), stringResource(R.string.favorites_subtitle)) }
        if (vm.favs.isEmpty()) {
            full {
                Text(
                    stringResource(R.string.favorites_empty),
                    Modifier.padding(vertical = 24.dp),
                    color = Color(0x99FFFFFF)
                )
            }
        } else {
            items(vm.favs, key = { it.id }, contentType = { "station" }) { StationCard(vm, it, vm.favs) }
        }
        if (vm.recents.isNotEmpty()) {
            full {
                Text(
                    stringResource(R.string.recently_played),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            items(vm.recents, key = { "rc" + it.id }, contentType = { "station" }) { StationCard(vm, it, vm.recents) }
        }
    }
}

@Composable
fun MiniPlayer(vm: AppVM) {
    val s = vm.current ?: return
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1D1940))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
            .clickable { vm.showPlayer = true }
            .testTag("mini_player")
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFEDEBFA)),
            Alignment.Center
        ) {
            if (s.logo != null) {
                AsyncImage(
                    model = s.logo,
                    contentDescription = stringResource(R.string.cd_station_logo),
                    modifier = Modifier.fillMaxSize().padding(5.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(s.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
            Text(
                text = when (vm.status) {
                    Status.Loading -> stringResource(R.string.status_connecting)
                    Status.Error -> stringResource(R.string.status_offline_retry)
                    else -> if (s.radio) stringResource(R.string.status_on_air) else stringResource(R.string.status_live)
                },
                fontSize = 12.sp,
                color = Cyan
            )
        }
        IconButton(onClick = { vm.togglePlay() }, modifier = Modifier.testTag("mini_player_play_pause")) {
            Icon(
                imageVector = if (vm.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(if (vm.isPlaying) R.string.cd_pause else R.string.cd_play)
            )
        }
        IconButton(onClick = { vm.stop() }, modifier = Modifier.testTag("mini_player_close")) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_close))
        }
    }
}

@Composable
fun BottomBar(vm: AppVM) {
    val tabs = listOf(
        Triple(stringResource(R.string.tab_tv), Icons.Rounded.Tv, "bottom_tab_tv" to 0),
        Triple(stringResource(R.string.tab_radio), Icons.Rounded.Radio, "bottom_tab_radio" to 1),
        Triple(stringResource(R.string.tab_trending), Icons.Rounded.Whatshot, "bottom_tab_trending" to 2),
        Triple(stringResource(R.string.tab_favorites), Icons.Rounded.Favorite, "bottom_tab_favorites" to 3)
    )
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xF2100D26))
            .navigationBarsPadding()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEach { (l, ic, tagIndex) ->
            val (tag, i) = tagIndex
            val sel = vm.tab == i
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (sel) Brush.horizontalGradient(listOf(Violet, Pink))
                        else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .clickable { vm.tab = i }
                    .testTag(tag)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(ic, contentDescription = l, Modifier.size(22.dp))
                AnimatedVisibility(sel) {
                    Text(l, Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
