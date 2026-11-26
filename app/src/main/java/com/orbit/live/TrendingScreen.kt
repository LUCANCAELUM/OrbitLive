package com.orbit.live

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.util.Locale
import kotlin.math.abs

private val glass = Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x0FFFFFFF)))
private val brand = Brush.horizontalGradient(listOf(Cyan, Violet, Pink))
private val scrim = Brush.verticalGradient(listOf(Color.Transparent, Color(0xD907060F)))
private val trendingGridPad = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp)

private data class TrendingCategoryChip(
    val label: String,
    val icon: ImageVector
)

private val trendingCategories = listOf(
    TrendingCategoryChip("Gaming", Icons.Rounded.SportsEsports),
    TrendingCategoryChip("Music", Icons.Rounded.MusicNote),
    TrendingCategoryChip("Just Chatting", Icons.Rounded.ChatBubbleOutline),
    TrendingCategoryChip("News", Icons.Rounded.Newspaper),
    TrendingCategoryChip("Sports", Icons.Rounded.SportsSoccer),
    TrendingCategoryChip("Electronic", Icons.Rounded.GraphicEq),
    TrendingCategoryChip("Chillout", Icons.Rounded.SelfImprovement)
)

private val fallbackThumbnails = listOf(
    R.drawable.img_stream_studio_1795696703169,
    R.drawable.img_stream_global_1795696704403,
    R.drawable.img_stream_stage_1795696704555
)

@DrawableRes
private fun fallbackThumbFor(id: String): Int {
    val index = abs(id.hashCode()) % fallbackThumbnails.size
    return fallbackThumbnails[index]
}

fun formatViewerCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000f)
        count >= 1_000 -> String.format(Locale.US, "%.1fK", count / 1_000f)
        else -> count.toString()
    }
}

private fun LazyGridScope.fullWidth(content: @Composable LazyGridItemScope.() -> Unit) =
    item(span = { GridItemSpan(maxLineSpan) }, content = content)

@Composable
fun TrendingScreen(vm: AppVM) {
    val q = vm.trendingQuery.trim()
    val streams = remember(vm.trending, q) {
        if (q.isBlank()) vm.trending
        else vm.trending.filter { stream ->
            stream.streamerName.contains(q, ignoreCase = true) ||
                stream.streamTitle.contains(q, ignoreCase = true) ||
                stream.category.contains(q, ignoreCase = true) ||
                stream.station.categories.any { it.contains(q, ignoreCase = true) }
        }
    }
    val stationQueue = remember(streams) { streams.map { it.station } }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(300.dp),
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("trending_screen"),
        contentPadding = trendingGridPad,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        fullWidth {
            Column(Modifier.padding(top = 8.dp)) {
                Text(
                    text = stringResource(R.string.trending_title),
                    style = TextStyle(brush = brand, fontSize = 32.sp, fontWeight = FontWeight.Black)
                )
                Text(
                    text = stringResource(R.string.trending_subtitle),
                    fontSize = 13.sp,
                    color = Color(0x99FFFFFF)
                )
            }
        }
        fullWidth {
            SearchField(
                value = vm.trendingQuery,
                hint = stringResource(R.string.search_trending_hint),
                testTag = "trending_search_field"
            ) { vm.updateTrendingQuery(it) }
        }
        fullWidth {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(glass)
                        .clickable {
                            vm.tab = 2
                            vm.surprise()
                        }
                        .testTag("trending_surprise_button")
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.surprise_me), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(glass)
                        .clickable { vm.loadTrending() }
                        .testTag("trending_refresh_button")
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.refresh), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        fullWidth {
            TrendingCategoryRow(
                selectedCategory = vm.trendingCategory,
                onToggleCategory = { vm.toggleTrendingCategory(it) }
            )
        }
        when {
            vm.trendingLoading && streams.isEmpty() -> fullWidth {
                Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Cyan)
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.loading_trending))
                    }
                }
            }
            vm.trendingError != null && streams.isEmpty() -> fullWidth {
                Column(
                    Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.error_load_trending), fontWeight = FontWeight.Bold)
                    Text(vm.trendingError ?: "", fontSize = 12.sp, color = Color(0x99FFFFFF))
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { vm.loadTrending() },
                        modifier = Modifier.testTag("trending_retry_button")
                    ) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
            streams.isEmpty() -> fullWidth {
                Text(
                    stringResource(R.string.no_trending_match),
                    Modifier.padding(32.dp),
                    color = Color(0x99FFFFFF)
                )
            }
            else -> itemsIndexed(
                items = streams,
                key = { _, item -> item.station.id },
                contentType = { _, _ -> "trending_card" }
            ) { idx, stream ->
                TrendingStreamCard(
                    rank = idx + 1,
                    stream = stream,
                    countryFlag = vm.countries[stream.station.country]?.flag.orEmpty(),
                    isFavorite = vm.isFav(stream.station.id),
                    onClick = { vm.play(stream.station, stationQueue) },
                    onToggleFavorite = { vm.toggleFav(stream.station) }
                )
            }
        }
    }
}

@Composable
private fun TrendingCategoryRow(
    selectedCategory: String?,
    onToggleCategory: (String?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trending_filter_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            TrendingFilterChip(
                label = stringResource(R.string.all_genres),
                icon = Icons.Rounded.AutoAwesome,
                selected = selectedCategory == null,
                testTag = "trending_filter_chip_all",
                onClick = { onToggleCategory(null) }
            )
        }
        items(trendingCategories, key = { it.label }) { item ->
            val tagSlug = item.label.lowercase().replace(' ', '_')
            TrendingFilterChip(
                label = item.label,
                icon = item.icon,
                selected = selectedCategory == item.label,
                testTag = "trending_filter_chip_$tagSlug",
                onClick = { onToggleCategory(item.label) }
            )
        }
    }
}

@Composable
private fun TrendingFilterChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val bg = if (selected) {
        Brush.horizontalGradient(listOf(Violet, Pink))
    } else {
        Brush.horizontalGradient(listOf(Color(0x22FFFFFF), Color(0x16FFFFFF)))
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(
                width = 1.dp,
                color = if (selected) Color(0x66FFFFFF) else Color(0x22FFFFFF),
                shape = RoundedCornerShape(50)
            )
            .semantics {
                role = Role.Checkbox
                this.selected = selected
            }
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (selected) Icons.Rounded.Check else icon,
            contentDescription = null,
            tint = if (selected) Color.White else Cyan,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            maxLines = 1,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun TrendingStreamCard(
    rank: Int,
    stream: TrendingStream,
    countryFlag: String,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(22.dp)
    val fallbackRes = remember(stream.station.id) { fallbackThumbFor(stream.station.id) }
    val thumbRequest = remember(stream.thumbnailUrl, fallbackRes) {
        ImageRequest.Builder(context)
            .data(stream.thumbnailUrl ?: fallbackRes)
            .crossfade(true)
            .build()
    }
    val viewerText = formatViewerCount(stream.viewerCount)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(glass)
            .border(1.dp, Color(0x26FFFFFF), shape)
            .clickable(onClick = onClick)
            .testTag("trending_card_${stream.station.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(cSurf)
        ) {
            AsyncImage(
                model = thumbRequest,
                contentDescription = stream.streamerName,
                placeholder = painterResource(fallbackRes),
                error = painterResource(fallbackRes),
                fallback = painterResource(fallbackRes),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrim)
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiveBadge()
                Text(
                    text = "#$rank",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(Violet, Pink)))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xCC07060F))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(50))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
                    .testTag("trending_viewers_${stream.station.id}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Visibility,
                    contentDescription = stringResource(R.string.cd_viewer_count),
                    tint = Cyan,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = viewerText,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stream.category,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x9914122A))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
                if (countryFlag.isNotBlank() || stream.station.country.isNotBlank()) {
                    Text(
                        text = "$countryFlag ${stream.station.country}".trim(),
                        color = Color(0xDDFFFFFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0x9914122A))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEDEBFA))
                    .border(2.dp, Violet, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (stream.thumbnailUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(stream.thumbnailUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = stream.streamerName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = stream.streamerName.take(2).uppercase(),
                        color = Violet,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = stream.streamerName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${stringResource(R.string.watching_count, viewerText)} · ${stream.streamTitle}",
                    fontSize = 12.sp,
                    color = Color(0xAAFFFFFF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.testTag("trending_favorite_${stream.station.id}")
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = stringResource(R.string.cd_favorite),
                    tint = if (isFavorite) Pink else Color(0xBBFFFFFF)
                )
            }
        }
    }
}
