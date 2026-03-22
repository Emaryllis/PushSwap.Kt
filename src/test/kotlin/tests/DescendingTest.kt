package tests

import Checker
import Settings.DEBUG
import Utils.suppressAllOutput
import me.emaryllis.chunk.ChunkSort
import me.emaryllis.data.Move
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Stream

class DescendingTest {
	private val checker = Checker()
	private val chunkSort = ChunkSort()

	companion object {
		private val failed = AtomicBoolean(false)

		@JvmStatic
		fun descendingTest(): Stream<Arguments> = perms(500, 1)

		@Suppress("SameParameterValue")
		private fun perms(from: Int, until: Int): Stream<Arguments> {
			require(from <= until) { "From must be less than or equal to until." }
			return (from..until).map { Arguments.of((it downTo 1).toList()) }.stream()
		}
	}

	private fun check(numList: List<Int>): Pair<Boolean, List<Move>> {
		val moves = chunkSort.chunkSort(numList)
		val status = checker.boolOutput(moves, numList, numList.sorted())
		return Pair(status, moves)
	}

	@Tag("manual")
	@ParameterizedTest
	@MethodSource("descendingTest")
	fun descendingTest(numList: List<Int>) {
		Assumptions.assumeFalse(failed.get())
		val (_, moves) = if (DEBUG) {
			suppressAllOutput(::check, numList)
		} else {
			check(numList)
		}
		println("Solved $numList in ${moves.size} moves: $moves.")
	}
}