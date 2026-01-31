package com.example.MovieDirectorRegisterMobileApp.data

data class Movie(
    val id: Long,
    val title: String,
    val year: Int,
    val directors: List<Person>? = null
)
