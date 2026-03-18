import Settings.DEBUG
import me.emaryllis.data.CircularBuffer
import me.emaryllis.data.Move

class Checker {
	private fun getOpsMap(a: CircularBuffer, b: CircularBuffer) = mapOf(
		Move.SA to { a.swap() },
		Move.SB to { b.swap() },
		Move.SS to { a.swap(); b.swap() },
		Move.PA to { b.push(a) },
		Move.PB to { a.push(b) },
		Move.RA to { a.rotate() },
		Move.RB to { b.rotate() },
		Move.RR to { a.rotateBoth(b) },
		Move.RRA to { a.reverseRotate() },
		Move.RRB to { b.reverseRotate() },
		Move.RRR to { a.reverseRotateBoth(b) }
	)

	private fun checker(moves: List<Move>, numList: List<Int>, expectedNumList: List<Int>): Boolean? {
		val a = CircularBuffer(numList.size, numList)
		val b = CircularBuffer(numList.size)
		val keys = getOpsMap(a, b).keys
		if (moves.any { it !in keys }) {
			if (DEBUG) System.err.print("Invalid move found: $moves")
			return null
		}
		if (numList.size != numList.toSet().size || expectedNumList.size != expectedNumList.toSet().size) return null
		if (moves.isEmpty()) {
			if (numList.isEmpty() || numList == numList.sorted()) return true
			else if (expectedNumList != expectedNumList.sorted()) error("This permutation is never possible.")
		}
		moves.forEach {
			if (getOpsMap(a, b)[it]?.invoke() != true) {
				if (DEBUG) System.err.print("Failed to execute move: $it. Stack A: ${a.toList()}, Stack B: ${b.toList()}.|")
				return false
			}
		}
		if (b.isNotEmpty()) {
			if (DEBUG) System.err.print("Stack B is not empty. Size: ${b.size}|")
			return false
		}
		val status = a.value.toList() == expectedNumList
		if (!status && DEBUG) {
			System.err.print("Expected: $expectedNumList, Got: ${a.value.toList()}.|")
		}
		return status
	}

	fun applyMoves(moves: List<Move>, numList: List<Int>): List<Int>? {
		val a = CircularBuffer(numList.size, numList)
		val b = CircularBuffer(numList.size)
		val keys = getOpsMap(a, b).keys
		if (moves.any { it !in keys }) {
			if (DEBUG) System.err.print("Invalid move found: $moves")
			return null
		}
		if (numList.size != numList.toSet().size) return null
		if (moves.isEmpty() && (numList.isEmpty() || numList == numList.sorted())) return numList
		moves.forEach {
			if (getOpsMap(a, b)[it]?.invoke() != true) {
				if (DEBUG) System.err.print("Failed to execute move: $it. Stack A: ${a.toList()}, Stack B: ${b.toList()}.|")
				return listOf()
			}
		}
		if (b.isNotEmpty()) {
			if (DEBUG) System.err.print("Stack B is not empty. Size: ${b.size}|")
			return listOf()
		}
		return a.value
	}

	fun boolOutput(moves: List<Move>, numList: List<Int>, expectedNumList: List<Int>): Boolean {
		return checker(moves, numList, expectedNumList) ?: false
	}

	fun output(moves: List<Move>, numList: List<Int>, expectedNumList: List<Int>) {
		when (checker(moves, numList, expectedNumList)) {
			null -> System.err.println("Error")
			true -> println("OK")
			false -> System.err.println("KO")
		}
	}
}