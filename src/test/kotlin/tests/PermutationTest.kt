package tests

import Checker
import Settings.DEBUG
import Utils.nthPermutation
import Utils.permutations
import Utils.suppressAllOutput
import me.emaryllis.chunk.ChunkSort
import me.emaryllis.data.Move
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.streams.asStream

class PermutationTest {
	private val chunkSort = ChunkSort()

	companion object {
		@JvmStatic
		fun allPermTest(): Stream<Arguments> = generatePermutations()

		private fun generatePermutations(tests: List<Int> = emptyList()): Stream<Arguments> {
			val size = 7
			val baseList = (1..size).toList()
			return if (tests.isEmpty()) {
				baseList.permutations().map { Arguments.of(it) }.asStream()
			} else {
				tests.map { Arguments.of(baseList.nthPermutation(it - 1)) }.stream()
			}
		}
	}

	private fun check(numList: List<Int>): Pair<Boolean, List<Move>> {
		val moves = chunkSort.chunkSort(numList)
		val status = Checker(moves, numList, numList.sorted()).boolOutput()
		return Pair(status, moves)
	}

	@ParameterizedTest
	@MethodSource("allPermTest")
	fun allPermutations(numList: List<Int>) {
		if (!DEBUG) {
			check(numList)
			return
		}
		val moves = suppressAllOutput(::check, numList).second
		println("Solved $numList in ${moves.size} moves: $moves.")
	}

//    @Test
//    fun reverseSort() = allPermutations((500 downTo 1).toList())
}