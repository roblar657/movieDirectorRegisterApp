package com.example.MovieDirectorRegisterMobileApp.managers

import android.app.Activity
import android.content.Context
import android.util.Log
import java.io.*
import java.nio.charset.Charset

/**
 * Håndterer lesing og skriving av filer.
 */
class FileManager(private val activity: Activity) {

	private var readerMap: HashMap<Int, BufferedReader> = HashMap()
	private  val tag = "FileManager"
	companion object {

		val CHARSET: Charset = Charsets.UTF_8
	}

	init {
		Log.d(tag, "FileManager: Initializing file manager")
	}

	/**
	 * Lukker leser for gitt fil-ID.
	 * @param fileId ID-en til filen som skal lukkes
	 */
	fun closeReader(fileId: Int) {
		Log.d(tag, "closeReader: Closing reader for file ID $fileId")
		readerMap[fileId]?.close()
		readerMap.remove(fileId)
	}

	/**
	 * Leser en linje fra fil.
	 * @param fileId ID-en til filen som skal leses fra
	 * @return Linjen som ble lest, eller null hvis slutten av filen er nådd
	 */
	fun readLine(fileId: Int): String? {
		val line = readerMap[fileId]?.readLine()
		Log.v(tag, "readLine: Read line from file ID $fileId: ${line?.take(50)}")
		return line
	}

	/**
	 * Legger til linje på slutten av fil.
	 * @param filename Navnet på filen
	 * @param line Linjen som skal legges til
	 */
	fun appendLine(filename: String, line: String) {
		Log.d(tag, "appendLine: Appending line to file $filename")
		activity.openFileOutput(filename, Context.MODE_APPEND).use { output ->
			output.write((line + "\n").toByteArray(CHARSET))
		}
	}

	/**
	 * Skriver overskrift som ny linje.
	 * Oppretter filen, hvis den ikke finnes.
	 * @param filename Navnet på filen
	 * @param header Overskriften som skal skrives
	 */
	fun writeHeader(filename: String, header: String) {
		val file = File(activity.filesDir, filename)
		val mode = if (file.exists()) Context.MODE_APPEND else Context.MODE_PRIVATE
		Log.d(tag, "writeHeader: Writing header to file $filename (exists: ${file.exists()})")

		activity.openFileOutput(filename, mode).use { output ->
			output.write("$header\n".toByteArray(CHARSET))
		}
	}

	/**
	 * Leser fra res/raw/ og returnerer BufferedReader.
	 * @param fileId ID-en til ressursfilen i raw-mappen
	 * @return BufferedReader for filen, eller null hvis filen ikke kan åpnes
	 */
	fun getReaderFromRawFolder(fileId: Int): BufferedReader? {
		Log.d(tag, "getReaderFromRawFolder: Opening raw resource with ID $fileId")
		val reader = InputStreamReader(activity.resources.openRawResource(fileId), CHARSET).buffered()
		readerMap[fileId] = reader
		return reader
	}
}