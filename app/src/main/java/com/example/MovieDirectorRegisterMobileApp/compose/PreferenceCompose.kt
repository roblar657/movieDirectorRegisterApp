package com.example.MovieDirectorRegisterMobileApp.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.MovieDirectorRegisterMobileApp.managers.MyPreferenceManager
import com.example.MovieDirectorRegisterMobileApp.ui.theme.backgroundColors
import com.example.MovieDirectorRegisterMobileApp.ui.theme.darkBackgroundColors


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferenceCompose(
    preference: MyPreferenceManager,
    onBack: () -> Unit
) {
    var isDarkMode by remember { mutableStateOf(preference.isDarkMode()) }
    var currentColor by remember { mutableStateOf(preference.getBackgroundColor()) }

    fun backgroundColor() = if (isDarkMode) darkBackgroundColors[currentColor] ?: Color.Black
    else backgroundColors[currentColor] ?: Color.White

    fun getTextColor() =
        if (isDarkMode) Color.White
        else Color.Black
    fun getBorderColor() =
        if (isDarkMode) Color.White
        else Color.Black
    fun getButtonBackground() =
        if (isDarkMode) Color.White
        else Color.Black
    fun getButtonTextColor() =
        if (isDarkMode) Color.Black
        else Color.White
    fun getBackgroundPreviewColor() =
        if (!isDarkMode) darkBackgroundColors[currentColor] ?: Color.Black
        else backgroundColors[currentColor] ?: Color.White

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tilbake", color = getTextColor()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tilbake", tint = getTextColor())
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = backgroundColor(),
                    titleContentColor = getTextColor()
                )
            )
        },
        containerColor = backgroundColor()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(paddingValues)
        ) {


            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(getButtonBackground())
                        .clickable {
                            isDarkMode = !isDarkMode
                            preference.setDarkMode(isDarkMode)
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isDarkMode) "Bytt til lys modus" else "Bytt til mørk modus",
                        color = getButtonTextColor()
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))


                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(getBackgroundPreviewColor())
                        .border(1.dp, getBorderColor())
                )

                Spacer(modifier = Modifier.width(16.dp))


                Box(
                    modifier = Modifier.height(28.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = if (isDarkMode) "Farge i lys modus" else "Farge i mørk modus",
                        color = getTextColor()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))


            Text("Velg bakgrunnsfarge:", style = MaterialTheme.typography.titleMedium, color = getTextColor())
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(backgroundColors.keys.toList()) { name ->
                    val lightColor = backgroundColors[name] ?: Color.White
                    val darkColor = darkBackgroundColors[name] ?: Color.Black

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (currentColor == name) {
                                    isDarkMode = !isDarkMode
                                    preference.setDarkMode(isDarkMode)
                                } else {
                                    currentColor = name
                                    preference.putBackgroundColor(name)
                                }
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(backgroundColor())
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(lightColor)
                                    .align(Alignment.TopStart)
                            )
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(darkColor)
                                    .align(Alignment.BottomEnd)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Box(
                            modifier = Modifier.height(32.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = name.replaceFirstChar { it.uppercase() },
                                color = getTextColor()
                            )
                        }
                    }
                    HorizontalDivider(
                        thickness = DividerDefaults.Thickness,
                        color = getTextColor().copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}