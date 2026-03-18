package tests

import Checker
import Settings.DEBUG
import Utils.permutations
import Utils.suppressAllOutput
import me.emaryllis.chunk.SmallSort
import me.emaryllis.data.Chunk
import me.emaryllis.data.CircularBuffer
import me.emaryllis.data.Stack
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class SmallSortTest {
	private val checker = Checker()
	private val smallSort = SmallSort()

	companion object {
		@JvmStatic
		fun smallSort(): Stream<Arguments> = listOf(3, 4, 5)
			.flatMap { (1..it).toList().permutations().toList() }
			.map { Arguments.of(it) }
			.stream()
	}

	private fun check(numList: List<Int>) {
		val stack = Stack(
			CircularBuffer(numList.size, numList),
			CircularBuffer(numList.size),
			Chunk(0, 0, emptyList()),
			null
		)
		assertTrue(checker.boolOutput(smallSort.smallSort(stack), numList, numList.sorted()))
	}

	@ParameterizedTest
	@MethodSource("smallSort")
	fun smallSort(numList: List<Int>) {
		if (DEBUG) {
			suppressAllOutput(::check, numList)
		} else {
			check(numList)
		}
	}

	@Test
	fun noOutputSmallSort() {
		smallSort(emptyList())
		smallSort(listOf(1))
		smallSort(listOf(1, 2))
		smallSort(listOf(1, 2, 3))
	}
}