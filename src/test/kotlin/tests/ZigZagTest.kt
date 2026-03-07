package tests

import Checker
import Settings.DEBUG
import Utils.suppressAllOutput
import me.emaryllis.chunk.ChunkSort
import me.emaryllis.data.Move
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

class ZigZagTest {
	private val chunkSort = ChunkSort()

	companion object {

		@JvmStatic
		fun zigZag() = generateZigZag(7)

		/**
		 * Generates a zigzag pattern of integers 1..n arranged as: [n, 1, n-1, 2, n-2, 3, ...].
		 * Example n=7 -> [7, 1, 6, 2, 5, 3, 4]
		 * n <= 0 returns an empty list.
		 * Time: O(n), Space: O(n).
		 */
		@Suppress("SameParameterValue")
		private fun generateZigZag(n: Int): Stream<Arguments> {
			require(n > 0) { "n must be greater than 0." }
			val result = mutableListOf<Int>()
			var low = 1
			var high = n
			var takeHigh = true
			while (low <= high) {
				if (takeHigh) {
					result.add(high)
					high--
				} else {
					result.add(low)
					low++
				}
				takeHigh = !takeHigh
			}
			return Stream.of(Arguments.of(result.toList()))
		}
	}

	private fun check(numList: List<Int>): Pair<Boolean, List<Move>> {
		val moves = chunkSort.chunkSort(numList)
		val status = Checker(moves, numList, numList.sorted()).boolOutput()
		return Pair(status, moves)
	}

	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	@ParameterizedTest
	@MethodSource("zigZag")
	fun zigZag(numList: List<Int>) {
		if (!DEBUG) {
			check(numList)
			return
		}

		val (ok, moves) = suppressAllOutput(::check, numList)
		println("Solved in ${moves.size} moves.")
		assert(ok)
	}
}