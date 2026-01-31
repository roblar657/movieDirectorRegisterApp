package com.example.MovieDirectorRegisterMobileApp.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MovieDirectorRegisterMobileApp.data.Movie
import com.example.MovieDirectorRegisterMobileApp.services.MovieAppService
import com.example.MovieDirectorRegisterMobileApp.ui.theme.backgroundColors
import com.example.MovieDirectorRegisterMobileApp.ui.theme.darkBackgroundColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MovieListCompose(
    db: MovieAppService,
    limit: Int,
    isDarkMode: Boolean,
    backgroundColorName: String,
    searchText: String,
    onShowRelated: (Long) -> Unit
) {
    val backgroundColor = if (isDarkMode)
        darkBackgroundColors[backgroundColorName] ?: Color.Black
    else
        backgroundColors[backgroundColorName] ?: Color.White

    val textColor = if (isDarkMode) Color.White else Color.Black
    val dividerColor = textColor.copy(alpha = 0.2f)

    val listState = rememberLazyListState()
    var offset by remember { mutableIntStateOf(0) }
    var items by remember { mutableStateOf(listOf<Movie>()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasMore by remember { mutableStateOf(true) }
    var lastSearch by remember { mutableStateOf<String?>(null) }



    // Trigger nytt søk når searchText endres
    LaunchedEffect(searchText) {
        if (searchText != lastSearch) {
            lastSearch = searchText
            offset = 0
            items = emptyList()
            hasMore = true
            isLoading = true
            val newItems = withContext(Dispatchers.IO) {
                if (searchText.isBlank()) db.getAllMovies(limit, offset)
                else db.searchMoviesByStart(searchText, limit, offset)
            }
            items = newItems
            hasMore = newItems.isNotEmpty()
            isLoading = false
        }
    }

    LaunchedEffect(offset) {
        if (!hasMore || offset == 0) return@LaunchedEffect
        isLoading = true
        val newItems = withContext(Dispatchers.IO) {
            if (searchText.isBlank()) db.getAllMovies(limit, offset)
            else db.searchMoviesByStart(searchText, limit, offset)
        }
        items += newItems
        if (newItems.isEmpty()) hasMore = false
        isLoading = false
    }

    // Auto-load mer ved scroll
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        }.collect { lastVisible ->
            if (lastVisible >= items.size - 1 && !isLoading && hasMore) {
                offset += limit
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            itemsIndexed(items) { _, movie ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowRelated(movie.id) }
                        .padding(vertical = 10.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${movie.title} (${movie.year})",
                            color = textColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val directorsText = movie.directors?.takeIf { it.isNotEmpty() }
                            ?.joinToString(" & ") { "${it.firstName} ${it.lastName}" }
                        if (!directorsText.isNullOrBlank()) {
                            Text(
                                text = "av $directorsText",
                                color = textColor.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next",
                        tint = textColor
                    )
                }
                HorizontalDivider(Modifier, DividerDefaults.Thickness, color = dividerColor)
            }
        }

        if (isLoading && items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = textColor)
            }
        }
    }
}
