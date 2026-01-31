package com.example.MovieDirectorRegisterMobileApp.managers

import android.content.Context
import android.util.Log

/**
 * Håndterer lagring og henting av prefereanser (bakgrunnsfarge og mørkmodus).
 */
class MyPreferenceManager(context: Context) {

	private val preferences = context.getSharedPreferences("my_preferences", Context.MODE_PRIVATE)
	private val editor get() = preferences.edit()

	private val tag = "MyPreferenceManager"

	init {
		Log.d(tag, "MyPreferenceManager: Initializing preference manager")
	}

	/**
	 * Lagrer bakgrunnsfarge.
	 * @param colorName Navnet på fargen som skal lagres
	 */
	fun putBackgroundColor(colorName: String) {
		Log.d(tag, "putBackgroundColor: Saving color $colorName")
		editor.putString("background_color", colorName.lowercase()).apply()
	}

	/**
	 * Henter lagret bakgrunnsfarge.
	 * @return Navnet på den lagrede fargen, eller "white" som standardverdi
	 */
	fun getBackgroundColor(): String {
		val color = preferences.getString("background_color", "white") ?: "white"
		Log.v(tag, "getBackgroundColor: Retrieved color $color")
		return color
	}

	/**
	 * Setter mørk modus på eller av.
	 * @param enabled true for å aktivere mørk modus, false for å deaktivere
	 */
	fun setDarkMode(enabled: Boolean) {
		Log.d(tag, "setDarkMode: Setting dark mode to $enabled")
		editor.putBoolean("dark_mode", enabled).apply()
	}

	/**
	 * Sjekker om mørk modus er aktivert.
	 * @return true hvis mørk modus er aktivert, false ellers
	 */
	fun isDarkMode(): Boolean {
		val darkMode = preferences.getBoolean("dark_mode", false)
		Log.v(tag, "isDarkMode: Dark mode is $darkMode")
		return darkMode
	}
}