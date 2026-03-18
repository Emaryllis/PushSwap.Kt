package tests

import Checker
import Utils.showNewline
import me.emaryllis.data.Move
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.stream.Stream

/**
 * Checks the Checker class with various test cases.
 * Assumes no user error for expectedNumList.
 * Checks:
 * - Throws error on illegal permutations (Handled in [illegalPermutation])
 * - Gives error when given a list containing duplicates or invalid moves
 * - Fails when given & expected lists are different with no moves
 * - Fails when given list does not meet expected lists with valid moves
 * - Passes when given a sorted list with no moves
 * - Passes move sequences that only affect stack A
 * - Passes move sequences that involve both stacks A and B
 * - Correct fail/pass state for negative numbers
 * - Handles complex move sequences correctly
 * Doesn't handle:
 * - Arguments not integers
 * - Arguments out of int range
 */
class CheckerTest {
	private val checker = Checker()

	companion object {
		@JvmStatic
		fun checkerTest(): Stream<Arguments> = Stream.of(
			Arguments.of(listOf(Move.DONOTUSEONLYFORTESTING), listOf(3, 2, 1), listOf(3, 2, 1), "Error\n"),
			Arguments.of(listOf(Move.DONOTUSEONLYFORTESTING), listOf(10, 11, 12), listOf(10, 11, 12), "Error\n"),
			Arguments.of(emptyList<Move>(), listOf(2, 2, 3, 4), listOf(2, 2, 3, 4), "Error\n"),
			Arguments.of(listOf(Move.PA), listOf(1, 1, 2, 3), listOf(1, 1, 2, 3), "Error\n"),

			Arguments.of(emptyList<Move>(), listOf(-2, -3, -1), listOf(-3, -2, -1), "KO\n"),
			Arguments.of(listOf(Move.PA), listOf(1, 2, 3), listOf(1, 2, 3), "KO\n"),
			Arguments.of(listOf(Move.SA), listOf(-3, -2, -1), listOf(-3, -2, -1), "KO\n"),
			Arguments.of(listOf(Move.SA), listOf(2, 1, 3), listOf(3, 2, 1), "KO\n"),

			Arguments.of(emptyList<Move>(), listOf(2, 3, 4), listOf(2, 3, 4), "OK\n"),
			Arguments.of(listOf(Move.SA), listOf(-2, -3, -4), listOf(-3, -2, -4), "OK\n"),
			Arguments.of(listOf(Move.SA), listOf(4, 3, 5), listOf(3, 4, 5), "OK\n"),
			Arguments.of(listOf(Move.RA), listOf(4, 5, 6), listOf(5, 6, 4), "OK\n"),
			Arguments.of(listOf(Move.RRA), listOf(5, 6, 7), listOf(7, 5, 6), "OK\n"),
			Arguments.of(listOf(Move.PB, Move.PA), listOf(6, 7, 8), listOf(6, 7, 8), "OK\n"),
			Arguments.of(listOf(Move.PB, Move.PB, Move.PA, Move.PA), listOf(7, 8, 9), listOf(7, 8, 9), "OK\n"),
			Arguments.of(
				listOf(Move.PB, Move.PB, Move.SB, Move.PA, Move.PA),
				listOf(1, 2, 3, 4),
				listOf(2, 1, 3, 4),
				"OK\n"
			),
			Arguments.of(
				listOf(Move.PB, Move.PB, Move.RRA, Move.PA, Move.PA),
				listOf(2, 3, 4, 5),
				listOf(2, 3, 5, 4),
				"OK\n"
			),
			Arguments.of(
				listOf(Move.PB, Move.PB, Move.PB, Move.RRR, Move.PA, Move.PA, Move.PA),
				listOf(1, 2, 3, 4, 5, 6),
				listOf(2, 3, 1, 6, 4, 5),
				"OK\n"
			),
		)
	}

	@Test
	fun illegalPermutation() {
		try {
			checker.output(listOf(), listOf(2, 1, 3), listOf(2, 1, 3))
			assertTrue(false)
		} catch (e: IllegalStateException) {
			assertTrue(true)
		}
	}

	@ParameterizedTest
	@MethodSource("checkerTest")
	fun checkerTest(moves: List<Move>, numList: List<Int>, expectedNumList: List<Int>, expected: String) {
		val outContent = ByteArrayOutputStream()
		val errContent = ByteArrayOutputStream()
		var status = listOf<String>()
		val originalOut = System.out
		val originalErr = System.err
		try {
			System.setErr(PrintStream(errContent))
			System.setOut(PrintStream(outContent))
			checker.output(moves, numList, expectedNumList)
			status = if (expected == "OK\n") {
				assertTrue(
					errContent.toString().isEmpty(),
					"Expected no stderr, but got: '${errContent.toString().showNewline()}'"
				)
				outContent
			} else {
				assertTrue(
					outContent.toString().isEmpty(),
					"Expected no stdout, but got: '${outContent.toString().showNewline()}'"
				)
				errContent
			}.toString().replace("\r", "").split("|")
			assertTrue(
				status.last().contains(expected),
				"Expected: '${expected.showNewline()}', but got: '${status.last().showNewline()}'"
			)
		} finally {
			System.setOut(originalOut)
			System.setErr(originalErr)
			if (status.size > 1) {
				System.err.println(status.dropLast(1).joinToString("\n"))
			}
		}
	}
}