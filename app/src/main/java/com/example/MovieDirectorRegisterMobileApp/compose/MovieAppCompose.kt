package com.example.MovieDirectorRegisterMobileApp.compose

import ActorFromMovieListCompose
import android.content.Intent
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.MovieDirectorRegisterMobileApp.PreferenceActivity
import com.example.MovieDirectorRegisterMobileApp.managers.MyPreferenceManager
import com.example.MovieDirectorRegisterMobileApp.services.MovieAppService
import com.example.MovieDirectorRegisterMobileApp.ui.theme.backgroundColors
import com.example.MovieDirectorRegisterMobileApp.ui.theme.darkBackgroundColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieAppCompose(activity: ComponentActivity) {
    val db = remember { MovieAppService(activity) }
    val prefs = remember { MyPreferenceManager(activity) }

    var currentList by remember { mutableStateOf("Filmer") }
    var isShowingRelated by remember { mutableStateOf(false) }
    var listId by remember { mutableStateOf<Long?>(null) }
    var detailName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    var isDarkMode by remember { mutableStateOf(prefs.isDarkMode()) }
    var backgroundColorName by remember { mutableStateOf(prefs.getBackgroundColor()) }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }

    val backgroundColor = if (isDarkMode) darkBackgroundColors[backgroundColorName] ?: Color.Black
    else backgroundColors[backgroundColorName] ?: Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black
    val topBottomBarColor = if (isDarkMode) backgroundColors[backgroundColorName] ?: Color.White
    else darkBackgroundColors[backgroundColorName] ?: Color.Black
    val topBottomTextColor = if (isDarkMode) Color.Black else Color.White

    LaunchedEffect(db) {
        withContext(Dispatchers.IO) { db.loadAllData() }
        isLoading = false
    }

    LaunchedEffect(Unit) {
        activity.lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    isDarkMode = prefs.isDarkMode()
                    backgroundColorName = prefs.getBackgroundColor()
                }
            }
        )
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (isShowingRelated) {
                        IconButton(onClick = {
                            isShowingRelated = false
                            listId = null
                            detailName = ""
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Tilbake",
                                tint = topBottomTextColor
                            )
                        }
                    }
                },
                title = { Text(if (isShowingRelated) "Tilbake" else "Film app", color = topBottomTextColor) },
                actions = {
                    Row(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clickable {
                                val intent = Intent(activity, PreferenceActivity::class.java)
                                activity.startActivity(intent)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(backgroundColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Endre bakgrunn", color = topBottomTextColor)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = topBottomBarColor,
                    titleContentColor = topBottomTextColor,
                    actionIconContentColor = topBottomTextColor,
                    navigationIconContentColor = topBottomTextColor
                )
            )
        },
        bottomBar = {
            if (!isShowingRelated) {
                val items = listOf(
                    "Filmer" to "\uD83C\uDFAC",
                    "Regissører" to "\uD83C\uDFA5",
                    "Skuespillere" to "\uD83C\uDFAD"
                )
                Column {
                    BottomAppBar(containerColor = topBottomBarColor) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            items.forEach { (key, icon) ->
                                var textWidth by remember { mutableIntStateOf(0) }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { currentList = key }
                                ) {
                                    Text(icon, color = topBottomTextColor, fontSize = 24.sp)
                                    Text(
                                        key,
                                        color = topBottomTextColor,
                                        onTextLayout = { textLayoutResult ->
                                            textWidth = textLayoutResult.size.width
                                        }
                                    )
                                    if (currentList == key) {
                                        Box(
                                            modifier = Modifier
                                                .height(2.dp)
                                                .width(with(LocalDensity.current) { textWidth.toDp() })
                                                .background(topBottomTextColor)
                                        )
                                    } else Spacer(modifier = Modifier.height(2.dp))
                                }
                            }
                        }
                    }
                    HorizontalDivider(thickness = 1.dp, color = topBottomTextColor)
                }
            }
        },
        content = { paddingValues ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(backgroundColor)
            ) {
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = textColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Laster inn…", color = textColor, fontSize = 16.sp, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    if (isLandscape) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {

                            if (isShowingRelated) {
                                val titleText = when (currentList) {
                                    "Filmer" -> "Skuespillere i filmen $detailName"
                                    "Regissører" -> "Filmer av regissør $detailName"
                                    "Skuespillere" -> "Filmer med skuespiller $detailName"
                                    else -> ""
                                }
                                Text(
                                    text = titleText,
                                    color = textColor,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )


                                Box(Modifier.fillMaxSize()) {
                                    when (currentList) {
                                        "Filmer" -> listId?.let {
                                            ActorFromMovieListCompose(it, db, 20, isDarkMode, backgroundColorName) {
                                                isShowingRelated = false
                                                detailName = ""
                                            }
                                        }
                                        "Regissører" -> listId?.let {
                                            MovieFromDirectorListCompose(it, db, 20, isDarkMode, backgroundColorName) {
                                                isShowingRelated = false
                                                detailName = ""
                                            }
                                        }
                                        "Skuespillere" -> listId?.let {
                                            MovieFromActorListCompose(it, db, 20, isDarkMode, backgroundColorName) {
                                                isShowingRelated = false
                                                detailName = ""
                                            }
                                        }
                                    }
                                }
                            } else {

                                Row(Modifier.fillMaxSize()) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        val searchTitle = when (currentList) {
                                            "Filmer" -> "Søk etter film"
                                            "Regissører" -> "Søk etter regissør"
                                            "Skuespillere" -> "Søk etter skuespiller"
                                            else -> "Søk"
                                        }
                                        val searchPlaceholder = when (currentList) {
                                            "Filmer" -> "Tittel"
                                            "Regissører" -> "Fornavn eller etternavn"
                                            "Skuespillere" -> "Fornavn eller etternavn"
                                            else -> "Søk"
                                        }
                                        Text(
                                            text = searchTitle,
                                            color = textColor,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        BasicTextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            textStyle = TextStyle(color = textColor, fontSize = 18.sp),
                                            singleLine = true,
                                            decorationBox = { innerTextField ->
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(if (isDarkMode) Color.DarkGray else Color.LightGray)
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Search,
                                                        contentDescription = "Søk",
                                                        tint = textColor
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Box(modifier = Modifier.fillMaxWidth()) {
                                                        if (searchQuery.text.isEmpty()) {
                                                            Text(
                                                                text = searchPlaceholder,
                                                                color = if (isDarkMode) Color.LightGray else Color.Gray,
                                                                fontSize = 18.sp
                                                            )
                                                        }
                                                        innerTextField()
                                                    }
                                                }
                                            }
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(
                                        modifier = Modifier
                                            .weight(1.5f)
                                            .fillMaxHeight()
                                    ) {
                                        when (currentList) {
                                            "Filmer" -> MovieListCompose(
                                                db = db,
                                                limit = 20,
                                                isDarkMode = isDarkMode,
                                                backgroundColorName = backgroundColorName,
                                                searchText = searchQuery.text,
                                                onShowRelated = { movieId ->
                                                    listId = movieId
                                                    detailName = db.getMovieFullTitle(movieId)
                                                    isShowingRelated = true
                                                }
                                            )
                                            "Regissører" -> DirectorListCompose(
                                                db = db,
                                                limit = 20,
                                                isDarkMode = isDarkMode,
                                                backgroundColorName = backgroundColorName,
                                                searchText = searchQuery.text,
                                                onShowRelated = { directorId ->
                                                    listId = directorId
                                                    detailName = db.getDirectorName(directorId)
                                                    isShowingRelated = true
                                                }
                                            )
                                            "Skuespillere" -> ActorListCompose(
                                                db = db,
                                                limit = 20,
                                                isDarkMode = isDarkMode,
                                                backgroundColorName = backgroundColorName,
                                                searchText = searchQuery.text,
                                                onShowRelated = { actorId ->
                                                    listId = actorId
                                                    detailName = db.getActorName(actorId)
                                                    isShowingRelated = true
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {

                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            val searchTitle = when (currentList) {
                                "Filmer" -> "Søk etter film"
                                "Regissører" -> "Søk etter regissør"
                                "Skuespillere" -> "Søk etter skuespiller"
                                else -> "Søk"
                            }
                            val searchPlaceholder = when (currentList) {
                                "Filmer" -> "Tittel"
                                "Regissører" -> "Fornavn eller etternavn"
                                "Skuespillere" -> "Fornavn eller etternavn"
                                else -> "Søk"
                            }

                            if (!isShowingRelated) {
                                Text(
                                    text = searchTitle,
                                    color = textColor,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            if (!isShowingRelated) {
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    textStyle = TextStyle(color = textColor, fontSize = 18.sp),
                                    singleLine = true,
                                    decorationBox = { innerTextField ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (isDarkMode) Color.DarkGray else Color.LightGray)
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "Søk",
                                                tint = textColor
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                if (searchQuery.text.isEmpty()) {
                                                    Text(
                                                        text = searchPlaceholder,
                                                        color = if (isDarkMode) Color.LightGray else Color.Gray,
                                                        fontSize = 18.sp
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        }
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            val titleText = when {
                                isShowingRelated && currentList == "Filmer" -> "Skuespillere i filmen $detailName"
                                isShowingRelated && currentList == "Regissører" -> "Filmer av regissør $detailName"
                                isShowingRelated && currentList == "Skuespillere" -> "Filmer med skuespiller $detailName"
                                else -> ""
                            }

                            if (isShowingRelated) {
                                Text(
                                    text = titleText,
                                    color = textColor,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            when {
                                !isShowingRelated -> {
                                    when (currentList) {
                                        "Filmer" -> MovieListCompose(
                                            db = db,
                                            limit = 20,
                                            isDarkMode = isDarkMode,
                                            backgroundColorName = backgroundColorName,
                                            searchText = searchQuery.text,
                                            onShowRelated = { movieId ->
                                                listId = movieId
                                                detailName = db.getMovieFullTitle(movieId)
                                                isShowingRelated = true
                                            }
                                        )
                                        "Regissører" -> DirectorListCompose(
                                            db = db,
                                            limit = 20,
                                            isDarkMode = isDarkMode,
                                            backgroundColorName = backgroundColorName,
                                            searchText = searchQuery.text,
                                            onShowRelated = { directorId ->
                                                listId = directorId
                                                detailName = db.getDirectorName(directorId)
                                                isShowingRelated = true
                                            }
                                        )
                                        "Skuespillere" -> ActorListCompose(
                                            db = db,
                                            limit = 20,
                                            isDarkMode = isDarkMode,
                                            backgroundColorName = backgroundColorName,
                                            searchText = searchQuery.text,
                                            onShowRelated = { actorId ->
                                                listId = actorId
                                                detailName = db.getActorName(actorId)
                                                isShowingRelated = true
                                            }
                                        )
                                    }
                                }
                                else -> {
                                    when (currentList) {
                                        "Filmer" -> listId?.let {
                                            ActorFromMovieListCompose(it, db, 20, isDarkMode, backgroundColorName) {
                                                isShowingRelated = false
                                                detailName = ""
                                            }
                                        }
                                        "Regissører" -> listId?.let {
                                            MovieFromDirectorListCompose(it, db, 20, isDarkMode, backgroundColorName) {
                                                isShowingRelated = false
                                                detailName = ""
                                            }
                                        }
                                        "Skuespillere" -> listId?.let {
                                            MovieFromActorListCompose(it, db, 20, isDarkMode, backgroundColorName) {
                                                isShowingRelated = false
                                                detailName = ""
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
