package tests

import Checker
import me.emaryllis.chunk.ChunkSort
import me.emaryllis.data.Move
import org.junit.jupiter.params.provider.Arguments
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Stream

class DescendingTest {
	private val chunkSort = ChunkSort()

	companion object {
		private val failed = AtomicBoolean(false)

		@JvmStatic
		fun descendingTest(): Stream<Arguments> = perms(500, 500)

		@Suppress("SameParameterValue")
		private fun perms(from: Int, until: Int): Stream<Arguments> {
			require(from <= until) { "From must be less than or equal to until." }
			return (from..until).map { Arguments.of((it downTo 1).toList()) }.stream()
		}
	}

	private fun check(numList: List<Int>): Pair<Boolean, List<Move>> {
		val moves = chunkSort.chunkSort(numList)
		val status = Checker(moves, numList, numList.sorted()).boolOutput()
		return Pair(status, moves)
	}

//	@ParameterizedTest
//	@MethodSource("descendingTest")
//	fun descendingTest(numList: List<Int>) {
//		Assumptions.assumeFalse(failed.get())
//		if (DEBUG) {
//			check(numList)
//			return
//		}
//		val moves = suppressAllOutput(::check, numList).second
//		println("Solved $numList in ${moves.size} moves: $moves.")
//	}
}