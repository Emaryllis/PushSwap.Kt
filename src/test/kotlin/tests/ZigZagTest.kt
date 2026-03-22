package tests

import Checker
import Settings.DEBUG
import Utils.suppressAllOutput
import me.emaryllis.Settings
import me.emaryllis.chunk.ChunkSort
import me.emaryllis.data.Move
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.time.Duration
import kotlin.math.ceil

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
		assert(ok) { "Expected ${numList.sorted()}.\nGot ${Checker().applyMoves(moves, numList)}.\nMoves: $moves" }
		println("Solved $n numbers (${ceil(n.toDouble() / Settings.MAX_CHUNK_SIZE).toInt()} chunks) in ${moves.size} moves.")
	}

	/** Used for manually testing each zigzag pattern */
	@Test
	@Tag("manual")
	fun zigZagTest() {
		zigZag(Settings.MAX_CHUNK_SIZE * 3)
	}

	/**
	 * Used for manual testing of zigzag patterns with increasing chunk counts
	 */
	@Test
	@Tag("manual")
	fun zigZagIncrementChunk() {
		val testUpToChunks = 3
		for (i in 1..testUpToChunks) {
			assertTimeoutPreemptively(Duration.ofSeconds(15L * (1..i).sum())) {
				zigZag(Settings.MAX_CHUNK_SIZE * i)
			}
		}
	}

	@Test
	@Tag("manual")
	fun zigZagIncrement() {
		for (i in 1..21) {
			zigZag(i)
		}
	}

	@Test
	@Tag("manual")
	fun zigZag100() {
		assertTimeoutPreemptively(Duration.ofMinutes(3)) { zigZag(100) }
	}

	@Test
	@Tag("manual")
	fun zigZag500() {
		assertTimeoutPreemptively(Duration.ofMinutes(10)) { zigZag(500) }
	}
}