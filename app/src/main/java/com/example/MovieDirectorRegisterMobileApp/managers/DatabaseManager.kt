package com.example.MovieDirectorRegisterMobileApp.managers

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns._ID
import android.util.Log

/**
 * Håndterer opprettelse og administrasjon av SQLite-databasen
 * for filmer, personer og relasjoner mellom dem.
 *
 * @param context Konteksten som brukes for å opprette eller åpne databasen.
 */
open class DatabaseManager(context: Context) :
	SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
	private val tag = "DatabaseManager"
	companion object {

		const val DATABASE_NAME = "MovieDatabase"
		const val DATABASE_VERSION = 6

		const val ID = "_id"

		const val TABLE_MOVIE = "MOVIE"
		const val MOVIE_TITLE = "title"
		const val MOVIE_YEAR = "year"
		const val MOVIE_DIRECTOR_IDS = "director_ids"

		const val TABLE_PERSON = "PERSON"
		const val PERSON_FIRST_NAME = "first_name"
		const val PERSON_LAST_NAME = "last_name"

		const val TABLE_MOVIE_PERSON = "MOVIE_PERSON"
		const val MOVIE_ID = "movie_id"
		const val PERSON_ID = "person_id"
		const val ROLE = "role"
	}

	override fun onCreate(db: SQLiteDatabase) {
		Log.i(tag, "onCreate: Creating database")
		createTables(db)
		Log.i(tag, "onCreate: Database created successfully")
	}

	/**
	 * Setter inn eller oppdaterer en film i databasen.
	 *
	 * @param db Skrivebar databaseinstans.
	 * @param title Tittelen på filmen.
	 * @param year Året filmen ble utgitt.
	 * @param directorIds Komma-separert liste av regissør-IDer.
	 * @return Pair<Long, Boolean> – ID-en til filmen og true hvis ny rad ble opprettet, false hvis den ble oppdatert.
	 */
	fun insertOrUpdateMovie(db: SQLiteDatabase, title: String, year: Int, directorIds: String): Pair<Long, Boolean> {
		val existingId = getMovieIdByTitleYearAndDirectorIds(db, title, year, directorIds)
		return if (existingId != null) {
			val values = ContentValues().apply { put(MOVIE_DIRECTOR_IDS, directorIds) }
			db.update(TABLE_MOVIE, values, "$ID=?", arrayOf(existingId.toString()))
			Log.d(tag, "insertOrUpdateMovie: Updated movie  '($title,$year,$directorIds)' with id=$existingId")
			existingId to false
		} else {
			val values = ContentValues().apply {
				put(MOVIE_TITLE, title.trim())
				put(MOVIE_YEAR, year)
				put(MOVIE_DIRECTOR_IDS, directorIds)
			}
			val newId = db.insert(TABLE_MOVIE, null, values)
			if (newId.toInt() != -1) {
				Log.d(tag, "insertOrUpdateMovie: Inserted new movie   '($title,$year,$directorIds)' with id=$newId")
				newId to true
			} else {
				Log.e(tag, "insertOrUpdateMovie: Failed to insert movie  '($title,$year,$directorIds)'")
				newId to false
			}
		}
	}

	/**
	 * Henter ID for en film basert på tittel, år og regissør-IDer.
	 *
	 * @param db Lesbar databaseinstans.
	 * @param title Tittel på filmen.
	 * @param year Utgivelsesår.
	 * @param directorIds Komma-separert liste av regissør-IDer.
	 * @return Long? – Filmens ID, eller null hvis ikke funnet.
	 */
	fun getMovieIdByTitleYearAndDirectorIds(
		db: SQLiteDatabase,
		title: String,
		year: Int,
		directorIds: String?
	): Long? {
		val sql = """
                SELECT $_ID 
                FROM $TABLE_MOVIE 
                WHERE $MOVIE_TITLE=? AND
                 $MOVIE_YEAR=? AND 
                 $MOVIE_DIRECTOR_IDS=? LIMIT 1
            """.trimIndent()
		val args = arrayOf(title, year.toString(), directorIds)
		db.rawQuery(sql, args).use { cursor ->
			val result = if (cursor.moveToFirst()) cursor.getLong(0) else null
			Log.d(tag, "getMovieIdByTitleYearAndDirectorIds:   '($title,$year,$directorIds)' : ${result ?: "not found"}")
			return result
		}
	}

	/**
	 * Sletter alle filmer med færre enn to skuespillere (rolle = 'a'),
	 * samt deres tilknyttede MOVIE_PERSON-rader.
	 *
	 * @return Pair<Int, Int> – antall slettede filmer og antall slettede relasjoner.
	 */
	fun ensureMovieActorCount(): Pair<Int, Int> {
		Log.i(tag, "ensureMovieActorCount: Starting cleanup")
		val db = writableDatabase

		val countMoviePersonsSql = """
        SELECT COUNT(*)
        FROM $TABLE_MOVIE_PERSON
        WHERE $MOVIE_ID IN (
            SELECT m.$ID
            FROM $TABLE_MOVIE m
            LEFT JOIN $TABLE_MOVIE_PERSON mp 
                ON mp.$MOVIE_ID = m.$ID AND mp.$ROLE = 'a'
            GROUP BY m.$ID
            HAVING COUNT(mp.$PERSON_ID) < 2
        );
    """.trimIndent()

		val cursorMoviePersons = db.rawQuery(countMoviePersonsSql, null)
		val deletedMoviePersonsCount = if (cursorMoviePersons.moveToFirst()) cursorMoviePersons.getInt(0) else 0
		cursorMoviePersons.close()

		val countMoviesSql = """
        SELECT COUNT(*)
        FROM $TABLE_MOVIE
        WHERE $ID IN (
            SELECT m.$ID
            FROM $TABLE_MOVIE m
            LEFT JOIN $TABLE_MOVIE_PERSON mp 
                ON mp.$MOVIE_ID = m.$ID AND mp.$ROLE = 'a'
            GROUP BY m.$ID
            HAVING COUNT(mp.$PERSON_ID) < 2
        );
    """.trimIndent()

		val cursorMovies = db.rawQuery(countMoviesSql, null)
		val deletedMoviesCount = if (cursorMovies.moveToFirst()) cursorMovies.getInt(0) else 0
		cursorMovies.close()

		val deleteMoviePersonsSql = """
        DELETE FROM $TABLE_MOVIE_PERSON
        WHERE $MOVIE_ID IN (
            SELECT m.$ID
            FROM $TABLE_MOVIE m
            LEFT JOIN $TABLE_MOVIE_PERSON mp 
                ON mp.$MOVIE_ID = m.$ID AND mp.$ROLE = 'a'
            GROUP BY m.$ID
            HAVING COUNT(mp.$PERSON_ID) < 2
        );
    """.trimIndent()

		val deleteMoviesSql = """
        DELETE FROM $TABLE_MOVIE
        WHERE $ID IN (
            SELECT m.$ID
            FROM $TABLE_MOVIE m
            LEFT JOIN $TABLE_MOVIE_PERSON mp 
                ON mp.$MOVIE_ID = m.$ID AND mp.$ROLE = 'a'
            GROUP BY m.$ID
            HAVING COUNT(mp.$PERSON_ID) < 2
        );
    """.trimIndent()

		db.execSQL(deleteMoviePersonsSql)
		db.execSQL(deleteMoviesSql)

		Log.i(tag, "ensureMovieActorCount: Deleted $deletedMoviesCount movies and $deletedMoviePersonsCount relations")
		return Pair(deletedMoviesCount, deletedMoviePersonsCount)
	}

	/**
	 * Setter inn en person hvis vedkommende ikke finnes fra før.
	 *
	 * @param db Skrivebar databaseinstans.
	 * @param firstName Fornavn på personen.
	 * @param lastName Etternavn på personen.
	 * @return Pair<Long, Boolean> – ID til personen og true hvis ny person ble lagt til, false hvis eksisterte.
	 */
	fun insertPersonIfNotExists(db: SQLiteDatabase, firstName: String, lastName: String): Pair<Long, Boolean> {
		val selection = "$PERSON_FIRST_NAME='$firstName' AND $PERSON_LAST_NAME='$lastName'"
		query(db, TABLE_PERSON, arrayOf(ID), selection).use { cursor ->
			return if (cursor.moveToFirst()) {
				val existingId = cursor.getLong(0)
				Log.d(tag, "insertPersonIfNotExists: Person '$firstName $lastName' already exists with id=$existingId")
				existingId to false
			} else {
				val newId = insertPerson(db, firstName, lastName)
				Log.d(tag, "insertPersonIfNotExists: Inserted new person '$firstName $lastName' with id=$newId")
				newId to true
			}
		}
	}

	/**
	 * Setter inn en ny person i databasen.
	 *
	 * @param db Skrivebar databaseinstans.
	 * @param firstName Fornavn.
	 * @param lastName Etternavn.
	 * @return Long – ID til den nyopprettede personen.
	 */
	private fun insertPerson(db: SQLiteDatabase, firstName: String, lastName: String): Long {
		val values = ContentValues().apply {
			put(PERSON_FIRST_NAME, firstName.trim())
			put(PERSON_LAST_NAME, lastName.trim())
		}
		val id = db.insert(TABLE_PERSON, null, values)
		Log.d(tag, "insertPerson: Inserted person '$firstName $lastName' with id=$id")
		return id
	}

	/**
	 * Oppretter alle nødvendige tabeller i databasen.
	 *
	 * @param db Skrivebar databaseinstans.
	 */
	private fun createTables(db: SQLiteDatabase) {
		Log.d(tag, "createTables: Creating MOVIE table")
		//UNIQUE($MOVIE_TITLE, $MOVIE_YEAR, $MOVIE_DIRECTOR_IDS)
		//er nødvendig ettersom flere filmer kan være laget
		//med samme tittel, samme år, men av forskjellige regisører.
		//Kan f.eks være når en lager flere innspillinger av samme bok,
		//med samme tittel samme år, i forskjellige land
		//Ved å kreve $MOVIE_DIRECTOR_IDS  NOT NULL, så sørger
		//en for at hver film har minst en regissør
		db.execSQL(
			"""CREATE TABLE $TABLE_MOVIE (
                $ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $MOVIE_TITLE TEXT NOT NULL,
                $MOVIE_YEAR INTEGER NOT NULL,
                $MOVIE_DIRECTOR_IDS TEXT NOT NULL,
                UNIQUE($MOVIE_TITLE, $MOVIE_YEAR, $MOVIE_DIRECTOR_IDS)
            );"""
		)

		//Har potensiell svakhet med at flere kan ha samme navn,
		//men blir litt kaotisk å gjøre det på en annen måte uten
		//å innføre nickname på skuespillere (og regissører)
		Log.d(tag, "createTables: Creating PERSON table")
		db.execSQL(
			"""CREATE TABLE $TABLE_PERSON (
                $ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $PERSON_FIRST_NAME TEXT NOT NULL,
                $PERSON_LAST_NAME TEXT NOT NULL,
                UNIQUE($PERSON_FIRST_NAME, $PERSON_LAST_NAME)
            );"""
		)

		//Hver person er skuespiller eller regissør
		Log.d(tag, "createTables: Creating MOVIE_PERSON table")
		db.execSQL(
			"""CREATE TABLE $TABLE_MOVIE_PERSON (
                $ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $MOVIE_ID NUMERIC,
                $PERSON_ID NUMERIC,
                $ROLE TEXT NOT NULL,
                FOREIGN KEY($MOVIE_ID) REFERENCES $TABLE_MOVIE($ID),
                FOREIGN KEY($PERSON_ID) REFERENCES $TABLE_PERSON($ID)
            );"""
		)
		Log.i(tag, "createTables: All tables created successfully")
	}

	/**
	 * Utføres ved oppgradering av databasen.
	 *
	 * @param db Skrivebar databaseinstans.
	 * @param oldVersion Forrige versjon.
	 * @param newVersion Ny versjon.
	 */
	override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
		Log.i(tag, "onUpgrade: Upgrading database from version $oldVersion to $newVersion")
		db.execSQL("DROP TABLE IF EXISTS $TABLE_MOVIE_PERSON")
		db.execSQL("DROP TABLE IF EXISTS $TABLE_PERSON")
		db.execSQL("DROP TABLE IF EXISTS $TABLE_MOVIE")
		Log.d(tag, "onUpgrade: Old tables dropped")
		createTables(db)
		Log.i(tag, "onUpgrade: Database upgraded successfully")
	}

	/**
	 * Sletter hele databasen fra enheten.
	 *
	 * @param context Konteksten brukt til å finne databasen.
	 */
	fun clearDatabase(context: Context) {
		Log.i(tag, "clearDatabase: Deleting database '$DATABASE_NAME'")
		val result = context.deleteDatabase(DATABASE_NAME)
		Log.i(tag, "clearDatabase: Database deletion ${if (result) "successful" else "failed"}")
	}

	/**
	 * Legger til relasjon mellom person og film i tabellen MOVIE_PERSON.
	 *
	 * @param db Skrivebar databaseinstans.
	 * @param movieId ID til filmen.
	 * @param personId ID til personen.
	 * @param role Rollen (f.eks. 'a' for actor, 'd' for director).
	 */
	fun insertMoviePerson(db: SQLiteDatabase, movieId: Long, personId: Long, role: String) {
		val values = ContentValues().apply {
			put(MOVIE_ID, movieId)
			put(PERSON_ID, personId)
			put(ROLE, role)
		}
		val id = db.insert(TABLE_MOVIE_PERSON, null, values)
		Log.d(tag, "insertMoviePerson: Inserted relation (movieId=$movieId, personId=$personId, role='$role') with id=$id")
	}

	/**
	 * Utfører en spørring mot databasen.
	 *
	 * @param db Lesbar databaseinstans.
	 * @param table Tabellen som skal spørres.
	 * @param columns Kolonner som skal hentes.
	 * @param selection Valgfritt WHERE-uttrykk for filtrering.
	 * @return Cursor – peker til resultatet av spørringen.
	 */
	fun query(db: SQLiteDatabase, table: String, columns: Array<String>, selection: String?): Cursor {
		Log.d(tag, "query: Querying table '$table' with selection: ${selection ?: "none"}")
		return db.query(table, columns, selection, null, null, null, null, null)
	}
}
