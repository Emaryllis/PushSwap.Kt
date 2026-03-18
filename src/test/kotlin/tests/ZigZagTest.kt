package tests

import Checker
import Settings.DEBUG
import Utils.suppressAllOutput
import me.emaryllis.Settings
import me.emaryllis.chunk.ChunkSort
import me.emaryllis.data.Move
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.time.Duration

class ZigZagTest {
	private val chunkSort = ChunkSort()

	companion object {
		/**
		 * Generates a zigzag pattern of integers 1..n arranged as: [n, 1, n-1, 2, n-2, 3, ...].
		 * Example n=7 -> [7, 1, 6, 2, 5, 3, 4]
		 * n <= 0 returns an empty list.
		 * Time: O(n), Space: O(n).
		 */
		@Suppress("SameParameterValue")
		private fun generateZigZag(n: Int): List<Int> {
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
			return result.toList()
		}
	}

	private fun check(numList: List<Int>): Pair<Boolean, List<Move>> {
		val moves = chunkSort.chunkSort(numList)
		val status = Checker().boolOutput(moves, numList, numList.sorted())
		return Pair(status, moves)
	}

	private fun zigZag(n: Int) {
		val numList = generateZigZag(n)
		val (ok, moves) = if (DEBUG) {
			suppressAllOutput(::check, numList)
		} else {
			check(numList)
		}
		println("Solved in ${moves.size} moves.")
		assert(ok) { "Expected ${numList.sorted()}.\nGot ${Checker().applyMoves(moves, numList)}.\nMoves: $moves" }
	}

	@Test
	fun zigZag2Chunks() {
		assertTimeoutPreemptively(Duration.ofSeconds(15)) { zigZag(Settings.MAX_CHUNK_SIZE * 2) }
	}

	@Test
	fun zigZag100() {
		assertTimeoutPreemptively(Duration.ofMinutes(3)) { zigZag(100) }
	}

	@Test

	fun zigZag500() {
		assertTimeoutPreemptively(Duration.ofMinutes(10)) { zigZag(500) }
	}
}