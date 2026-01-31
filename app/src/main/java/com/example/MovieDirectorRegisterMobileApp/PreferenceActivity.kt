package com.example.MovieDirectorRegisterMobileApp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.MovieDirectorRegisterMobileApp.compose.PreferenceCompose
import com.example.MovieDirectorRegisterMobileApp.managers.MyPreferenceManager


class PreferenceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preference = MyPreferenceManager(this)
        val isDarkMode = preference.isDarkMode()

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                if (isDarkMode) android.graphics.Color.BLACK else android.graphics.Color.WHITE,
                if (isDarkMode) android.graphics.Color.WHITE else android.graphics.Color.BLACK
            ),
            navigationBarStyle = SystemBarStyle.light(
                if (isDarkMode) android.graphics.Color.BLACK else android.graphics.Color.WHITE,
                if (isDarkMode) android.graphics.Color.WHITE else android.graphics.Color.BLACK
            )
        )
        setContent {

            PreferenceCompose(preference = preference, onBack = { finish() })
        }
    }
}

