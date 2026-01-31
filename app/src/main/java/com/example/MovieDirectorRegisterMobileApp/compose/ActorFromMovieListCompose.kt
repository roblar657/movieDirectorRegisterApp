import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
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
fun ActorFromMovieListCompose(
    movieId: Long,
    db: MovieAppService,
    limit: Int = 20,
    isDarkMode: Boolean,
    backgroundColorName: String,
    onBack: () -> Unit
) {
    val listState = rememberLazyListState()
    var offset by remember { mutableIntStateOf(0) }
    var items by remember { mutableStateOf(listOf<Person>()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasMore by remember { mutableStateOf(true) }


    val backgroundColor = if (isDarkMode)
        darkBackgroundColors[backgroundColorName] ?: Color.Black
    else
        backgroundColors[backgroundColorName] ?: Color.White

    val textColor = if (isDarkMode) Color.White else Color.Black
    val dividerColor = textColor.copy(alpha = 0.2f)

    LaunchedEffect(movieId, offset) {
        if (!hasMore) return@LaunchedEffect
        isLoading = true
        val newItems = withContext(Dispatchers.IO) { db.getActorsFromMovie(movieId, limit, offset) }
        if (newItems.isEmpty()) {
            hasMore = false
        } else {
            // Legger kun til nye elementer som ikke allerede finnes
            val currentIds = items.map { it.id }.toSet()
            items += newItems.filter { it.id !in currentIds }
        }
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
            itemsIndexed(items) { _, actor ->
                Text(
                    "${actor.firstName} ${actor.lastName}",
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 16.dp)
                )
                HorizontalDivider(Modifier, DividerDefaults.Thickness, color = dividerColor)
                if (items.last() == actor && !isLoading) offset += limit
            }
        }

        if (isLoading && items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = textColor)
            }
        }
    }
}
