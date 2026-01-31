package com.example.MovieDirectorRegisterMobileApp.services

import com.example.MovieDirectorRegisterMobileApp.data.Movie
import com.example.MovieDirectorRegisterMobileApp.data.Person

/**
 * Service  for håndtering av film- og persondata.
 * Tilbyr funksjonalitet for søk, henting og lasting av data fra database og filer.
 *
 */
interface IMovieAppService {

    /**
     * Henter en liste over regissører fra databasen.
     *
     * @param limit Maksimalt antall regissører som skal hentes (standard: 10)
     * @param offset Hvor mange rader som skal hoppes over (standard: 0)
     * @return Liste med Person med rollen regissører
     */
    fun getAllDirectors(limit: Int = 10, offset: Int = 0): List<Person>

    /**
     * Henter en liste over skuespillere fra databasen.
     *
     * @param limit Maksimalt antall skuespillere som skal hentes (standard: 10)
     * @param offset Hvor mange rader som skal hoppes over (standard: 0)
     * @return Liste med Person med rollen skuespiller
     */
    fun getAllActors(limit: Int = 10, offset: Int = 0): List<Person>

    /**
     * Henter regissørens fulle navn basert på ID.
     *
     * @param directorId ID-en til regissøren
     * @return Regissørens fulle navn (fornavn etternavn), eller tom streng hvis ikke funnet
     */
    fun getDirectorName(directorId: Long): String

    /**
     * Henter filmens fullstendige tittel med årstall og regissører.
     * Format: "Tittel (år) av Regissør1 & Regissør2"
     *
     * @param movieId ID-en til filmen
     * @return Fullstendig tittel, eller tom streng hvis filmen ikke finnes
     */
    fun getMovieFullTitle(movieId: Long): String

    /**
     * Henter skuespillerens fulle navn basert på ID.
     *
     * @param actorId ID-en til skuespilleren
     * @return Skuespillerens fulle navn (fornavn etternavn), eller tom streng hvis ikke funnet
     */
    fun getActorName(actorId: Long): String

    /**
     * Henter skuespillere som er med i en bestemt film.
     *
     * @param movieId ID-en til filmen
     * @param limit Maksimalt antall skuespillere som skal hentes (standard: 10)
     * @param offset Hvor mange rader som skal hoppes over (standard: 0)
     * @return Liste med Person med rollen skuespillere i filmen
     */
    fun getActorsFromMovie(movieId: Long, limit: Int = 10, offset: Int = 0): List<Person>

    /**
     * Søker etter filmer hvor tittelen starter med den gitte strengen.
     *
     * @param startOfTitle Starten på filmtittelen å søke etter
     * @param limit Maksimalt antall filmer som skal returneres (standard: 10)
     * @param offset Hvor mange rader som skal hoppes over (standard: 0)
     * @return Liste med Movie som matcher søket
     */
    fun searchMoviesByStart(startOfTitle: String, limit: Int = 10, offset: Int = 0): List<Movie>

    /**
     * Søker etter regissører hvor navnet starter med den gitte søkestrengen.
     * Støtter søk på fornavn, etternavn eller begge deler.
     *
     * @param search Søkestreng (kan være fornavn, etternavn eller "fornavn etternavn")
     * @param limit Maksimalt antall regissører som skal returneres (standard: 10)
     * @param offset Hvor mange rader som skal hoppes over (standard: 0)
     * @return Liste med Person med rollen regissør som matcher søket
     */
    fun searchDirectorsByStart(search: String, limit: Int = 10, offset: Int = 0): List<Person>

    /**
     * Søker etter skuespillere hvor navnet starter med den gitte søkestrengen.
     * Støtter søk på fornavn, etternavn eller begge deler.
     *
     * @param search Søkestreng (kan være fornavn, etternavn eller "fornavn etternavn")
     * @param limit Maksimalt antall skuespillere som skal returneres (standard: 10)
     * @param offset Hvor mange rader som skal hoppes over (standard: 0)
     * @return Liste med Person med rollen skuespiller som matcher søket
     */
    fun searchActorsByStart(search: String, limit: Int = 10, offset: Int = 0): List<Person>

    /**
     * Henter alle filmer fra databasen.
     *
     * @param limit Maksimalt antall filmer som skal hentes (standard: 10)
     * @param offset Hvor mange rader som skal hoppes over (standard: 0)
     * @return Liste med Movie
     */
    fun getAllMovies(limit: Int = 10, offset: Int = 0): List<Movie>

    /**
     * Henter alle filmer som en skuespiller er med i.
     *
     * @param actorId ID-en til skuespilleren
     * @param limit Maksimalt antall filmer som skal hentes (standard: 10)
     * @param offset Hvor mange rader som skal hoppes over (standard: 0)
     * @return Liste med Movie hvor skuespilleren er med i
     */
    fun getMoviesFromActor(actorId: Long, limit: Int = 10, offset: Int = 0): List<Movie>

    /**
     * Henter alle filmer laget av en bestemt regissør.
     *
     * @param directorId ID-en til regissøren
     * @param limit Maksimalt antall filmer som skal hentes (standard: 10)
     * @param offset Hvor mange rader som skal hoppes over (standard: 0)
     * @return Liste med Movie som er regissert av denne regissør
     */
    fun getMoviesFromDirector(directorId: Long, limit: Int = 10, offset: Int = 0): List<Movie>

    /**
     * Henter regissører basert på en liste med ID-er.
     *
     * @param directorIds Liste med regissør-ID-er
     * @return Liste med Person med rollen regissør
     */
    fun getDirectors(directorIds: List<Long>): List<Person>

    /**
     * Laster inn all data fra raw-ressurser (movies.csv, actors.csv) til databasen.
     * Prosessen kjører asynkront og håndterer:
     * - Lasting av filmer med regissører
     * - Lasting av skuespillere og kobling til filmer
     * - Synkronisering mellom filmer og actors via channels
     * - Sletting av filmer med færre enn to skuespillere
     *
     * Data lagres også i lokale filer for backup.
     * Funksjonen sletter eksisterende database før lasting.
     */
    suspend fun loadAllData()
}