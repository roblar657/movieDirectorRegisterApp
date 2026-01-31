package com.example.MovieDirectorRegisterMobileApp.services

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.MovieDirectorRegisterMobileApp.R
import com.example.MovieDirectorRegisterMobileApp.managers.DatabaseManager
import com.example.MovieDirectorRegisterMobileApp.managers.DatabaseManager.Companion.ID
import com.example.MovieDirectorRegisterMobileApp.managers.DatabaseManager.Companion.MOVIE_DIRECTOR_IDS
import com.example.MovieDirectorRegisterMobileApp.managers.DatabaseManager.Companion.MOVIE_TITLE
import com.example.MovieDirectorRegisterMobileApp.managers.DatabaseManager.Companion.MOVIE_YEAR
import com.example.MovieDirectorRegisterMobileApp.managers.DatabaseManager.Companion.PERSON_FIRST_NAME
import com.example.MovieDirectorRegisterMobileApp.managers.DatabaseManager.Companion.PERSON_ID
import com.example.MovieDirectorRegisterMobileApp.managers.DatabaseManager.Companion.PERSON_LAST_NAME
import com.example.MovieDirectorRegisterMobileApp.managers.DatabaseManager.Companion.ROLE
import com.example.MovieDirectorRegisterMobileApp.managers.DatabaseManager.Companion.TABLE_MOVIE
import com.example.MovieDirectorRegisterMobileApp.managers.DatabaseManager.Companion.TABLE_MOVIE_PERSON
import com.example.MovieDirectorRegisterMobileApp.managers.DatabaseManager.Companion.TABLE_PERSON
import com.example.MovieDirectorRegisterMobileApp.managers.FileManager
import com.example.MovieDirectorRegisterMobileApp.data.Movie
import com.example.MovieDirectorRegisterMobileApp.data.Person
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class MovieAppService(private val context: Context) : IMovieAppService {

    private val tag = "MovieAppService"

    private val moviesFile = "movies_output.txt"
    private val personsFile = "persons_output.txt"
    private val moviePersonFile = "moviePerson_output.txt"

    private val fileManager = FileManager(context as Activity)
    private val databaseManager = DatabaseManager(context as Activity)

    /**
     * Genererer en unik nøkkel for en film basert på tittel, år og regissører.
     * Nøkkelen er i formatet: "tittel_år_regissør1:regissør2:..."
     *
     * @param title Filmens tittel
     * @param year Filmens utgivelsesår
     * @param directorsNames Rå streng med regissører separert med &
     * @return Unik nøkkel for filmen
     */
    private fun buildMovieKey(title: String, year: Int, directorsNames: String): String {
        val directorsKey = directorsNames
            .split("&")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .sorted() //Må være sortert, pga både director id liste  1:2 og 2:1 kan  forekomme
            .joinToString(":")
        val key = "${title.lowercase()}_${year}_$directorsKey"
        Log.v(tag, "buildMovieKey: $key")
        return key
    }


    override fun getAllDirectors(limit: Int, offset: Int): List<Person> {
        Log.d(tag, "getAllDirectors: limit=$limit, offset=$offset")
        val db = databaseManager.readableDatabase
        val list = mutableListOf<Person>()
        val query = """
            SELECT DISTINCT p._id, p.first_name, p.last_name
            FROM PERSON p
            JOIN MOVIE_PERSON mp ON mp.person_id = p._id
            WHERE mp.role = 'd'
            ORDER BY p.first_name ASC, p.last_name ASC
            LIMIT $limit OFFSET $offset
        """.trimIndent()
        val cursor = db.rawQuery(query, null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Person(
                        it.getLong(it.getColumnIndexOrThrow("_id")),
                        it.getString(it.getColumnIndexOrThrow("first_name")),
                        it.getString(it.getColumnIndexOrThrow("last_name"))
                    )
                )
            }
        }
        Log.d(tag, "getAllDirectors: found ${list.size} directors")
        return list
    }


    override fun getAllActors(limit: Int, offset: Int): List<Person> {
        Log.d(tag, "getAllActors: limit=$limit, offset=$offset")
        val db = databaseManager.readableDatabase
        val list = mutableListOf<Person>()
        val query = """
            SELECT DISTINCT p._id, p.first_name, p.last_name
            FROM PERSON p
            JOIN MOVIE_PERSON mp ON mp.person_id = p._id
            WHERE mp.role = 'a'
            ORDER BY p.first_name ASC, p.last_name ASC
            LIMIT $limit OFFSET $offset
        """.trimIndent()
        val cursor = db.rawQuery(query, null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Person(
                        it.getLong(it.getColumnIndexOrThrow("_id")),
                        it.getString(it.getColumnIndexOrThrow("first_name")),
                        it.getString(it.getColumnIndexOrThrow("last_name"))
                    )
                )
            }
        }
        Log.d(tag, "getAllActors: found ${list.size} actors")
        return list
    }


    override fun getDirectorName(directorId: Long): String {
        Log.v(tag, "getDirectorName: directorId=$directorId")
        val db = databaseManager.readableDatabase
        var name = ""
        val cursor = db.query(
            TABLE_PERSON,
            arrayOf(PERSON_FIRST_NAME, PERSON_LAST_NAME),
            "$ID=?",
            arrayOf(directorId.toString()),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                val firstName = it.getString(it.getColumnIndexOrThrow(PERSON_FIRST_NAME))
                val lastName = it.getString(it.getColumnIndexOrThrow(PERSON_LAST_NAME))
                name = "$firstName $lastName"
            }
        }
        Log.v(tag, "getDirectorName: result='$name'")
        return name
    }


    override fun getMovieFullTitle(movieId: Long): String {
        Log.d(tag, "getMovieFullTitle: movieId=$movieId")
        val db = databaseManager.readableDatabase
        var fullTitle = ""

        val cursor = db.query(
            TABLE_MOVIE,
            arrayOf(MOVIE_TITLE, MOVIE_YEAR, MOVIE_DIRECTOR_IDS),
            "$ID=?",
            arrayOf(movieId.toString()),
            null, null, null
        )

        cursor.use {
            if (it.moveToFirst()) {
                val title = it.getString(it.getColumnIndexOrThrow(MOVIE_TITLE))
                val year = it.getInt(it.getColumnIndexOrThrow(MOVIE_YEAR))
                val directorIdsStr = it.getString(it.getColumnIndexOrThrow(MOVIE_DIRECTOR_IDS))

                val directorNames = if (!directorIdsStr.isNullOrBlank()) {
                    directorIdsStr.split(":")
                        .mapNotNull { id -> id.toLongOrNull() }
                        .joinToString(" & ") { getDirectorName(it) }
                } else ""

                fullTitle = if (directorNames.isNotBlank()) {
                    "$title ($year) av $directorNames"
                } else {
                    "$title ($year)"
                }
            }
        }

        Log.d(tag, "getMovieFullTitle: result='$fullTitle'")
        return fullTitle
    }


    override fun getActorName(actorId: Long): String {
        Log.v(tag, "getActorName: actorId=$actorId")
        val db = databaseManager.readableDatabase
        var name = ""
        val cursor = db.query(
            TABLE_PERSON,
            arrayOf(PERSON_FIRST_NAME, PERSON_LAST_NAME),
            "$ID=?",
            arrayOf(actorId.toString()),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                val firstName = it.getString(it.getColumnIndexOrThrow(PERSON_FIRST_NAME))
                val lastName = it.getString(it.getColumnIndexOrThrow(PERSON_LAST_NAME))
                name = "$firstName $lastName"
            }
        }
        Log.v(tag, "getActorName: result='$name'")
        return name
    }


    override fun getActorsFromMovie(movieId: Long, limit: Int, offset: Int): List<Person> {
        Log.d(tag, "getActorsFromMovie: movieId=$movieId, limit=$limit, offset=$offset")
        val db = databaseManager.readableDatabase
        val list = mutableListOf<Person>()
        val query =
            """
                SELECT p._id, p.first_name, p.last_name
                FROM PERSON p
                JOIN MOVIE_PERSON mp ON mp.person_id = p._id
                WHERE mp.role = 'a' AND mp.movie_id = ?
                ORDER BY p.first_name ASC, p.last_name ASC
                LIMIT $limit OFFSET $offset
            """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(movieId.toString()))
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Person(
                        it.getLong(it.getColumnIndexOrThrow("_id")),
                        it.getString(it.getColumnIndexOrThrow("first_name")),
                        it.getString(it.getColumnIndexOrThrow("last_name"))
                    )
                )
            }
        }
        Log.d(tag, "getActorsFromMovie: found ${list.size} actors")
        return list
    }


    override fun searchMoviesByStart(startOfTitle: String, limit: Int, offset: Int): List<Movie> {
        Log.d(tag, "searchMoviesByStart: query='$startOfTitle', limit=$limit, offset=$offset")
        val db = databaseManager.readableDatabase
        val movies = mutableListOf<Movie>()

        val query = """
        SELECT * FROM $TABLE_MOVIE
        WHERE $MOVIE_TITLE LIKE ?
        ORDER BY $MOVIE_TITLE ASC, $MOVIE_YEAR ASC
        LIMIT ? OFFSET ?
    """.trimIndent()

        val args = arrayOf("${startOfTitle.trim()}%", limit.toString(), offset.toString())

        val cursor = db.rawQuery(query, args)

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(ID))
                val title = it.getString(it.getColumnIndexOrThrow(MOVIE_TITLE))
                val movieYear = it.getInt(it.getColumnIndexOrThrow(MOVIE_YEAR))
                val directorIdsStr = it.getString(it.getColumnIndexOrThrow(MOVIE_DIRECTOR_IDS))
                val directors = if (!directorIdsStr.isNullOrBlank()) {
                    directorIdsStr.split(":")
                        .mapNotNull { idStr -> idStr.toLongOrNull() }
                        .let { ids -> getDirectors(ids) }
                } else emptyList()

                movies.add(Movie(id, title, movieYear, directors))
            }
        }

        Log.d(tag, "searchMoviesByStart: found ${movies.size} movies")
        return movies
    }



    override fun searchDirectorsByStart(search: String, limit: Int, offset: Int): List<Person> {
        Log.d(tag, "searchDirectorsByStart: query='$search', limit=$limit, offset=$offset")
        val db = databaseManager.readableDatabase
        val directors = mutableListOf<Person>()

        val trimmedSearch = search.trim()
        if (trimmedSearch.isEmpty()) return directors

        val firstSpace = trimmedSearch.indexOf(' ')
        val firstNamePart: String
        val lastNamePart: String

        if (firstSpace >= 0) {
            firstNamePart = trimmedSearch.substring(0, firstSpace)
            lastNamePart = trimmedSearch.substring(firstSpace + 1).trim()
        } else {
            firstNamePart = ""
            lastNamePart = ""
        }

        val query: String
        val args: Array<String>

        if (firstNamePart.isNotEmpty() && lastNamePart.isNotEmpty()) {
            query = """
            SELECT * FROM $TABLE_PERSON
            WHERE ($PERSON_FIRST_NAME LIKE ? AND $PERSON_LAST_NAME LIKE ?)
               OR ($PERSON_LAST_NAME LIKE ?)
               OR ($PERSON_FIRST_NAME LIKE ?)
            ORDER BY $PERSON_FIRST_NAME ASC, $PERSON_LAST_NAME ASC
            LIMIT ? OFFSET ?
        """.trimIndent()

            args = arrayOf(
                "$firstNamePart%", "$lastNamePart%", // første ord = fornavn, resten = etternavn
                "$trimmedSearch%",                   // hele søket = etternavn
                "$trimmedSearch%",                   // hele søket = fornavn
                limit.toString(), offset.toString()
            )
        } else {
            query = """
            SELECT * FROM $TABLE_PERSON
            WHERE $PERSON_FIRST_NAME LIKE ? OR $PERSON_LAST_NAME LIKE ?
            ORDER BY $PERSON_FIRST_NAME ASC, $PERSON_LAST_NAME ASC
            LIMIT ? OFFSET ?
        """.trimIndent()

            val word = trimmedSearch.ifEmpty { "" }
            args = arrayOf("$word%", "$word%", limit.toString(), offset.toString())
        }

        val cursor = db.rawQuery(query, args)

        cursor.use { cursor ->
            while (cursor.moveToNext()) {
                val personId = cursor.getLong(cursor.getColumnIndexOrThrow(ID))

                val addCursor = db.query(
                    TABLE_MOVIE_PERSON,
                    arrayOf(ROLE),
                    "$PERSON_ID=? AND $ROLE='d'", // 'd' for director
                    arrayOf(personId.toString()),
                    null, null, null
                )

                addCursor.use { c ->
                    if (c.moveToFirst()) {
                        val firstName = cursor.getString(cursor.getColumnIndexOrThrow(PERSON_FIRST_NAME))
                        val lastName = cursor.getString(cursor.getColumnIndexOrThrow(PERSON_LAST_NAME))
                        directors.add(Person(personId, firstName, lastName))
                    }
                }
            }
        }

        Log.d(tag, "searchDirectorsByStart: found ${directors.size} directors")
        return directors
    }


    override fun searchActorsByStart(search: String, limit: Int, offset: Int): List<Person> {
        Log.d(tag, "searchActorsByStart: query='$search', limit=$limit, offset=$offset")
        val db = databaseManager.readableDatabase
        val actors = mutableListOf<Person>()

        val searchTrimmed = search.trim()
        if (searchTrimmed.isEmpty()) return actors

        val firstSpace = searchTrimmed.indexOf(' ')
        val firstNamePart: String
        val lastNamePart: String

        if (firstSpace >= 0) {
            firstNamePart = searchTrimmed.substring(0, firstSpace)
            lastNamePart = searchTrimmed.substring(firstSpace + 1).trim()
        } else {
            firstNamePart = searchTrimmed
            lastNamePart = ""
        }

        val query: String
        val args: Array<String>

        if (firstNamePart.isNotEmpty() && lastNamePart.isNotEmpty()) {
            // Flere ord: sjekk kombinasjon + hele søket som fornavn eller etternavn
            query = """
            SELECT * FROM $TABLE_PERSON
            WHERE ($PERSON_FIRST_NAME LIKE ? AND $PERSON_LAST_NAME LIKE ?)
               OR ($PERSON_LAST_NAME LIKE ?)
               OR ($PERSON_FIRST_NAME LIKE ?)
            ORDER BY $PERSON_FIRST_NAME ASC, $PERSON_LAST_NAME ASC
            LIMIT ? OFFSET ?
        """.trimIndent()

            args = arrayOf(
                "$firstNamePart%", "$lastNamePart%", // første ord = fornavn, resten = etternavn
                "$searchTrimmed%",                   // hele søket = etternavn
                "$searchTrimmed%",                   // hele søket = fornavn
                limit.toString(), offset.toString()
            )
        } else {
            // Ett ord: søk i fornavn eller etternavn
            query = """
            SELECT * FROM $TABLE_PERSON
            WHERE $PERSON_FIRST_NAME LIKE ? OR $PERSON_LAST_NAME LIKE ?
            ORDER BY $PERSON_FIRST_NAME ASC, $PERSON_LAST_NAME ASC
            LIMIT ? OFFSET ?
        """.trimIndent()

            args = arrayOf("$searchTrimmed%", "$searchTrimmed%", limit.toString(), offset.toString())
        }

        val cursor = db.rawQuery(query, args)

        cursor.use { cursor ->
            while (cursor.moveToNext()) {
                val personId = cursor.getLong(cursor.getColumnIndexOrThrow(ID))

                val addCursor = db.query(
                    TABLE_MOVIE_PERSON,
                    arrayOf(ROLE),
                    "$PERSON_ID=? AND $ROLE='a'",
                    arrayOf(personId.toString()),
                    null, null, null
                )

                addCursor.use { c ->
                    if (c.moveToFirst()) {
                        val firstName = cursor.getString(cursor.getColumnIndexOrThrow(PERSON_FIRST_NAME))
                        val lastName = cursor.getString(cursor.getColumnIndexOrThrow(PERSON_LAST_NAME))
                        actors.add(Person(personId, firstName, lastName))
                    }
                }
            }
        }

        Log.d(tag, "searchActorsByStart: found ${actors.size} actors")
        return actors
    }







    override fun getAllMovies(limit: Int, offset: Int): List<Movie> {
        Log.d(tag, "getAllMovies: limit=$limit, offset=$offset")
        val db = databaseManager.readableDatabase
        val movies = mutableListOf<Movie>()
        val query = """
        SELECT _id, title, year, director_ids
        FROM MOVIE
        ORDER BY title ASC, year ASC, _id ASC
        LIMIT $limit OFFSET $offset
    """.trimIndent()
        val cursor = db.rawQuery(query, null)
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow("_id"))
                val title = it.getString(it.getColumnIndexOrThrow("title"))
                val year = it.getInt(it.getColumnIndexOrThrow("year"))
                val directorIdsStr = it.getString(it.getColumnIndexOrThrow("director_ids"))
                val directors = if (!directorIdsStr.isNullOrBlank()) {
                    directorIdsStr.split(":")
                        .mapNotNull { idStr -> idStr.toLongOrNull() }
                        .let { ids -> getDirectors(ids) }
                } else emptyList()
                movies.add(Movie(id, title, year, directors))
            }
        }
        Log.d(tag, "getAllMovies: found ${movies.size} movies")
        return movies
    }


    override fun getMoviesFromActor(actorId: Long, limit: Int, offset: Int): List<Movie> {
        Log.d(tag, "getMoviesFromActor: actorId=$actorId, limit=$limit, offset=$offset")
        val db = databaseManager.readableDatabase
        val movies = mutableListOf<Movie>()
        val query =
            """
            SELECT m._id, m.title, m.year, m.director_ids
            FROM MOVIE m
            JOIN MOVIE_PERSON mp ON mp.movie_id = m._id
            WHERE mp.person_id = ? AND mp.role = 'a'
            ORDER BY m.title ASC, m.year ASC, m.director_ids ASC
            LIMIT $limit OFFSET $offset
           """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(actorId.toString()))
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow("_id"))
                val title = it.getString(it.getColumnIndexOrThrow("title"))
                val year = it.getInt(it.getColumnIndexOrThrow("year"))
                val directorIdsStr = it.getString(it.getColumnIndexOrThrow("director_ids"))
                val directors =
                    if (!directorIdsStr.isNullOrBlank()) {
                        directorIdsStr.split(":")
                            .mapNotNull { idStr -> idStr.toLongOrNull() }
                            .let { ids -> getDirectors(ids) }
                    }
                    else emptyList()

                movies.add(Movie(id, title, year, directors))
            }
        }
        Log.d(tag, "getMoviesFromActor: found ${movies.size} movies")
        return movies
    }


    override fun getMoviesFromDirector(directorId: Long, limit: Int, offset: Int): List<Movie> {
        Log.d(tag, "getMoviesFromDirector: directorId=$directorId, limit=$limit, offset=$offset")
        val db = databaseManager.readableDatabase
        val movies = mutableListOf<Movie>()
        val query = """
        SELECT m._id, m.title, m.year, m.director_ids
        FROM MOVIE m
        JOIN MOVIE_PERSON mp ON mp.movie_id = m._id
        WHERE mp.person_id = ? AND mp.role = 'd'
        ORDER BY m.title ASC, m.year ASC, m.director_ids ASC
        LIMIT $limit OFFSET $offset
    """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(directorId.toString()))
        cursor.use {

            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow("_id"))
                val title = it.getString(it.getColumnIndexOrThrow("title"))
                val year = it.getInt(it.getColumnIndexOrThrow("year"))
                val directorIdsStr = it.getString(it.getColumnIndexOrThrow("director_ids"))
                val directors = if (!directorIdsStr.isNullOrBlank()) {
                    directorIdsStr.split(":").mapNotNull { idStr -> idStr.toLongOrNull() }
                        .let { ids -> getDirectors(ids) }
                } else emptyList()

                movies.add(Movie(id, title, year, directors))
            }
        }
        Log.d(tag, "getMoviesFromDirector: found ${movies.size} movies")
        return movies
    }


    override fun getDirectors(directorIds: List<Long>): List<Person> {
        if (directorIds.isEmpty()) return emptyList()
        Log.v(tag, "getDirectors: directorIds=$directorIds")
        val db = databaseManager.readableDatabase
        val directorList = mutableListOf<Person>()

        val directorIdValues = directorIds.joinToString(",") { "?" }
        val args = directorIds.map { it.toString() }.toTypedArray()

        val sql =
            """
            SELECT _id, first_name, last_name
            FROM PERSON
            WHERE _id IN ($directorIdValues)
            ORDER BY first_name ASC, last_name ASC
           """.trimIndent()

        val cursor = db.rawQuery(sql, args)
        cursor.use {
            while (it.moveToNext()) {
                directorList.add(
                    Person(
                        it.getLong(it.getColumnIndexOrThrow("_id")),
                        it.getString(it.getColumnIndexOrThrow("first_name")),
                        it.getString(it.getColumnIndexOrThrow("last_name"))
                    )
                )
            }
        }
        Log.v(tag, "getDirectors: found ${directorList.size} directors")
        return directorList
    }


    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    override suspend fun loadAllData(): Unit = withContext(Dispatchers.IO) {
        Log.i(tag, "loadAllData: Starting data load process")
        databaseManager.clearDatabase(context)
        val writeDb = databaseManager.writableDatabase

        fileManager.writeHeader(moviesFile, "id,title,year,director_ids")
        fileManager.writeHeader(personsFile, "id,first_name,last_name")
        fileManager.writeHeader(moviePersonFile, "movie_id,person_id,role")

        val movieChannel = Channel<Pair<String, Long>>(Channel.UNLIMITED)
        val movieMap = mutableMapOf<String, Long>()

        //For debugging
        var movieCount = 0
        var personCount = 0
        var moviePersonCount = 0

        // Movie korutine
        val movieJob = launch {
            Log.d(tag, "movieJob: Started")
            fileManager.getReaderFromRawFolder(R.raw.movies).use { reader ->
                var line: String? = fileManager.readLine(R.raw.movies)

                //Kanskje noe naiv måte å hoppe over header på, kan medføre
                //krasj ved malformed header
                if (line?.startsWith("movie_title") == true) {
                    Log.d(tag, "movieJob: Skipping header")
                    line = fileManager.readLine(R.raw.movies)
                }

                while (line != null && line.isNotBlank()) {

                    val parts = line.split(",")

                    //Sjekker om det er gyldig format
                    if (parts.size < 3) {
                        Log.w(tag, "movieJob: Invalid format, terminating reading from file")
                        //line = " " trigger  line.isNotBlank() i while løkke,
                        //slik at den terminerer (er lettere å debugge, om en
                        //terminerer der feilen skjedde)
                        line = " "
                        continue
                    }

                    val title = parts[0].trim().lowercase()
                    val year = parts[1].trim().toIntOrNull()
                    if (year == null) {
                        Log.w(tag, "movieJob: Invalid year, terminating reading")
                        //line = " " trigger  line.isNotBlank() i while løkke,
                        //slik at den terminerer (er lettere å debugge, om en
                        //terminerer der feilen skjedde)
                        line = " "
                        continue
                    }
                    val directorNames = parts[2].trim()

                    val directors =
                        directorNames.split("&").map { it.trim() }.filter { it.isNotEmpty() }
                    val directorIds = mutableListOf<Long>()

                    for (directorFullName in directors) {
                        val parts = directorFullName.trim().lowercase().split(" ")
                        if (parts.size < 2){
                            //line = " " trigger terminering i while løkken,
                            //slik at den terminerer (er lettere å debugge, om en
                            //terminerer der feilen skjedde)
                            line = " "
                            continue
                        }

                        val lastName = parts.last()
                        val firstName = parts.dropLast(1).joinToString(" ")

                        val directorId = databaseManager.insertPersonIfNotExists(writeDb, firstName, lastName)

                        directorIds.add(directorId.first)

                        //Hvis lagt til database
                        if (directorId.second) {
                            fileManager.appendLine(personsFile, "${directorId.first},$firstName,$lastName")
                            personCount++
                            Log.v(tag, "movieJob: Added director $firstName $lastName (id=${directorId.first})")
                        }
                    }

                    val directorIdsStr = directorIds.joinToString(":")
                    val movieId = databaseManager.insertOrUpdateMovie(writeDb, title, year, directorIdsStr)
                    //Hvis den ble lagt til databasen
                    if(movieId.second) {
                        fileManager.appendLine(moviesFile, "${movieId.first},$title,$year,$directorIdsStr")
                        movieCount++
                        Log.d(tag, "movieJob: Added movie '($title,$year,$directorIds)'with id=${movieId.first}")
                    }

                    for (id in directorIds) {
                        //Id'er finnes, så er bare å legge til
                        databaseManager.insertMoviePerson(writeDb, movieId.first, id, "d")
                        fileManager.appendLine(moviePersonFile, "${movieId.first},$id,d")
                        moviePersonCount++
                    }

                    val movieKey = buildMovieKey(title, year, directorNames)
                    movieMap[movieKey] = movieId.first
                    //Sier ifra til potensielle nye, samt ventende personer,
                    //at ny film eksisterer.
                    movieChannel.send(movieKey to movieId.first)


                    line = fileManager.readLine(R.raw.movies)

                    //Ingen flere linjer, kan dermed lukke reader
                    if(line == null){
                        fileManager.closeReader(R.raw.movies)
                    }

                }
            }
            //Venter til person har behandlet ferdig movies
            //i movie channel
            while (!movieChannel.isEmpty)
                delay(5000)

            //Tvinger person korutine til å avslutte
            movieChannel.close()

            Log.i(tag, "movieJob: Completed. Processed $movieCount movies")
        }

        // Actor korutine
        val actorJob = launch {
            Log.d(tag, "personJob: Started")
            val waitingActors = mutableMapOf<String, MutableList<List<String>>>()

            fileManager.getReaderFromRawFolder(R.raw.actors).use { reader ->

                var line: String? = fileManager.readLine(R.raw.actors)
                if (line?.startsWith("first_name") == true) {
                    Log.d(tag, "actorJob: Skipping header")
                    line = fileManager.readLine(R.raw.actors)
                }

                while (line != null || waitingActors.isNotEmpty() || !movieChannel.isClosedForReceive) {

                    //Ny film er lagt til database, kan da koble
                    //skuespillere til den
                    movieChannel.receiveCatching().getOrNull()?.let { (key, mid) ->
                        movieMap[key] = mid
                        //Behandler alle ventende skuespillere, som ventet på denne film
                        waitingActors.remove(key)?.forEach { fields ->
                            val pid = databaseManager.insertPersonIfNotExists(writeDb, fields[0], fields[1])
                            //Hvis person ble lagt til database
                            if (pid.second) {
                                fileManager.appendLine(
                                    personsFile,
                                    "${pid.first},${fields[0]},${fields[1]}"
                                )
                                personCount++
                                Log.v(tag, "actorJob: Added waiting actor ${fields[0]} ${fields[1]} (id=${pid.first})")
                            }
                            //id'er finnes, så vil gå greit
                            databaseManager.insertMoviePerson(writeDb, mid, pid.first, "a")
                            fileManager.appendLine(moviePersonFile, "$mid,${pid.first},a")
                            moviePersonCount++
                        }
                    }
                    //Nye (ikke ventende actors) fra fil
                    if (line != null && line.isNotBlank()) {
                        val fields = line.split(",").map { it.trim().lowercase() }
                        val firstName = fields[0]
                        val lastName = fields[1]
                        val movieTitle = fields[2]
                        val movieYear = fields[3].toIntOrNull() ?: 0
                        val directorsNames = fields[4]

                        val movieKey = buildMovieKey(movieTitle, movieYear, directorsNames)
                        val movieId = movieMap[movieKey]
                        //Hvis filmen finnes
                        if (movieId != null) {
                            val pid = databaseManager.insertPersonIfNotExists(writeDb, firstName, lastName)
                            //Hvis person ble lagt til database
                            if (pid.second) {
                                fileManager.appendLine(personsFile, "${pid.first},$firstName,$lastName")
                                personCount++
                                Log.v(tag, "actorJob: Added actor $firstName $lastName (id=${pid.first})")
                            }
                            databaseManager.insertMoviePerson(writeDb, movieId, pid.first, "a")
                            fileManager.appendLine(moviePersonFile, "$movieId,${pid.first},a")
                            moviePersonCount++
                        } else {
                            waitingActors.computeIfAbsent(movieKey) { mutableListOf() }
                                .add(
                                    listOf(
                                        firstName,
                                        lastName,
                                        movieTitle,
                                        movieYear.toString(),
                                        directorsNames
                                    )
                                )
                            Log.v(tag, "actorJob: Actor $firstName $lastName waiting for movie '$movieTitle'")
                        }

                    }
                    else if (line == null && movieChannel.isClosedForReceive) {
                        Log.w(tag, "actorJob: Processing ${waitingActors.size} waiting actors")
                        waitingActors.forEach { (key, actorsList) ->
                            Log.w(tag, "actorJob: Movie not found for key: $key")
                            actorsList.forEach { fields ->
                                val firstName = fields[0]
                                val lastName = fields[1]

                                val pid = databaseManager.insertPersonIfNotExists(writeDb, firstName, lastName)
                                if (pid.second) {
                                    fileManager.appendLine(personsFile, "${pid.first},$firstName,$lastName")
                                    personCount++
                                    Log.w(tag, "actorJob: Added waiting actor $firstName $lastName (id=${pid.first})")
                                }
                            }
                        }
                        waitingActors.clear()
                    }

                    line = fileManager.readLine(R.raw.actors)
                    if (line == null) fileManager.closeReader(R.raw.actors)
                }
            }
            Log.i(tag, "personJob: Completed. Processed $personCount persons, $moviePersonCount movie-person relations")
        }

        joinAll(movieJob, actorJob)
        //Sørger for at alle filmer har minst 2 skuespillere.
        val (deletedMovies, deletedMoviePersons) = databaseManager.ensureMovieActorCount()
        movieCount -= deletedMovies
        moviePersonCount -= deletedMoviePersons
        Log.i(tag, "loadAllData: Completed successfully. Movies: $movieCount, Persons: $personCount, Relations: $moviePersonCount")
    }
}
