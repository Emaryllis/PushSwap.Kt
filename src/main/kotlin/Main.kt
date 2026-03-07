package me.emaryllis

import me.emaryllis.chunk.ChunkSort
import me.emaryllis.utils.Utils.hasDuplicates

/**
 * Processes a list of strings, converting each to an integer.
 * If it encounters a noninteger string, it returns false.
 * If all strings are valid integers, it returns true.
 */
fun processNumList(strNumList: List<String>): Pair<Boolean, List<Int>> {
	val parsed = strNumList.mapNotNull { it.toIntOrNull() }
	return if (parsed.size == strNumList.size && !parsed.hasDuplicates()) {
		true to parsed
	} else {
		false to emptyList()
	}
}

/**
 * If no parameters are specified, the program must not display anything and should
 * return to the prompt.
 *
 * In case of error, it must display "Error" followed by an '\n' on the standard error.
 * Errors include, for example: some arguments not being integers, some arguments
 * exceeding the integer limits, and/or the presence of duplicates.
 */
fun main(strNumList: Array<String>) {
	if (strNumList.isEmpty()) return
	val parsed: Pair<Boolean, List<Int>> = if (strNumList.size == 1) {
		processNumList(strNumList[0].split(" "))
	} else {
		processNumList(strNumList.toList())
	}
	if (!parsed.first) {
		return System.err.println("Error")
	}
	val moves = ChunkSort().chunkSort(parsed.second)
	println(moves.joinToString("\n"))
}