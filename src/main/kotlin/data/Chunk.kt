package me.emaryllis.data

/**
 * Represents a chunk of integers with a minimum and maximum value.
 *
 * @property minValue The minimum value in the chunk.
 * @property maxValue The maximum value in the chunk.
 * @property values The list of integer values contained in the chunk.
 */
data class Chunk(val minValue: Int, val maxValue: Int, val values: List<Int>) {
	/**
	 * Checks if a given integer is within the chunk's range.
	 *
	 * Time & Space Complexity: O(1).
	 *
	 * @param x The integer to check.
	 * @return True if x is between minValue and maxValue (inclusive), false otherwise.
	 */
	operator fun contains(x: Int): Boolean = x in minValue..maxValue
}
