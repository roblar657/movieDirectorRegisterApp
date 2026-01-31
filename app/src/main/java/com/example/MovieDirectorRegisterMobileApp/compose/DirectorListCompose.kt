package com.example.MovieDirectorRegisterMobileApp.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MovieDirectorRegisterMobileApp.data.Person
import com.example.MovieDirectorRegisterMobileApp.services.MovieAppService
import com.example.MovieDirectorRegisterMobileApp.ui.theme.backgroundColors
import com.example.MovieDirectorRegisterMobileApp.ui.theme.darkBackgroundColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DirectorListCompose(
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
    var items by remember { mutableStateOf(listOf<Person>()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasMore by remember { mutableStateOf(true) }
    var lastSearch by remember { mutableStateOf<String?>(null) }

    // Last alle directors ved oppstart
    LaunchedEffect(Unit) {
        isLoading = true
        val initialItems = withContext(Dispatchers.IO) { db.getAllDirectors(limit, offset) }
        items = initialItems
        hasMore = initialItems.isNotEmpty()
        isLoading = false
    }

    // Trigger nytt søk ved endring
    LaunchedEffect(searchText) {
        if (searchText != lastSearch) {
            lastSearch = searchText
            offset = 0
            items = emptyList()
            hasMore = true
            isLoading = true
            val newItems = withContext(Dispatchers.IO) {
                if (searchText.isBlank()) db.getAllDirectors(limit, offset)
                else db.searchDirectorsByStart(searchText, limit, offset)
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
            if (searchText.isBlank()) db.getAllDirectors(limit, offset)
            else db.searchDirectorsByStart(searchText, limit, offset)
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
            itemsIndexed(items) { _, director ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowRelated(director.id) }
                        .padding(vertical = 10.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${director.firstName} ${director.lastName}",
                        color = textColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = "Vis detaljer",
                        tint = textColor
                    )
                }
                Divider(color = dividerColor)
            }
        }

        if (isLoading && items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = textColor)
            }
        }
    }
}
