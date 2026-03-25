package me.emaryllis.data

import me.emaryllis.Settings.HASH_PRIME
import me.emaryllis.Settings.MOVE_DEBUG

/**
 * Stack represents the state of the PushSwap problem, including
 * both buffers, chunk boundaries, move history, and heuristic.
 *
 * Time complexity: Most operations are O(1), except clone, equals, hashCode.
 * Space complexity: O(n) for stack and move storage.
 */
class Stack(
	val a: CircularBuffer,
	val b: CircularBuffer,
	var chunk: Chunk,
	var prevChunkNum: Int?,
	var moves: PackedMoveList = PackedMoveList.empty(),
	var heuristic: Int = Int.MAX_VALUE
) : Cloneable {
	/**
	 * Time & Space Complexity: O(1)
	 *
	 * Returns the total cost for this state: move count plus heuristic estimate.
	 */
	val currentCost: Int
		get() = moves.size + heuristic

	/**
	 * Time & Space Complexity: O(n), n = Dataset size
	 *
	 * @return A deep copy of all Stack data.
	 */
	public override fun clone(): Stack = Stack(
		a.clone(),
		b.clone(),
		chunk,
		prevChunkNum,
		moves.clone(),
		heuristic
	)

	/**
	 * Purpose: Executes the given move, updating the stack state
	 * and recording the move if successful. For moves affecting
	 * both stacks, uses temp variables to ensure both operations
	 * are executed. (Very dumb) If the move is invalid, logs an
	 * error if [log] is true and [MOVE_DEBUG] is enabled.
	 *
	 * Time Complexity: O(1) (amortised, except for resizing of [moves] by [PackedMoveList]).
	 * Space Complexity: O(1) for mutation, O(m) for history -> m = [moves]'s size.
	 *
	 * @param move The move to apply.
	 * @param log Whether to log invalid moves (default true).
	 * @return true if the move was successfully applied, false otherwise.
	 */
	fun apply(move: Move, log: Boolean = true): Boolean {
		val temp1: Boolean
		val temp2: Boolean
		val status = when (move) {
			Move.SA -> a.swap()
			Move.SB -> b.swap()
			Move.SS -> {
				temp1 = a.swap()
				temp2 = b.swap()
				temp1 && temp2
			}

			Move.PA -> b.push(a)
			Move.PB -> a.push(b)
			Move.RA -> a.rotate()
			Move.RB -> b.rotate()
			Move.RR -> {
				temp1 = a.rotate()
				temp2 = b.rotate()
				temp1 && temp2
			}

			Move.RRA -> a.reverseRotate()
			Move.RRB -> b.reverseRotate()
			Move.RRR -> {
				temp1 = a.reverseRotate()
				temp2 = b.reverseRotate()
				temp1 && temp2
			}

			else -> error("Invalid move: $move") // Only reached if using DONOTUSEONLYFORTESTING
		}
		if (status) {
			moves = moves.add(move)
		} else if (log && MOVE_DEBUG) System.err.println("Invalid move: $move.")
		return status
	}

	/**
	 * Checks logical equality of Stack contents,
	 * move history size, and heuristic.
	 *
	 * Time Complexity: O(n), n = stack size
	 * Space Complexity: O(1)
	 *
	 * @param other Object to compare.
	 * @return True if stacks are logically equal, false otherwise.
	 */
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is Stack) return false
		return hashCode() == other.hashCode()
	}

	/**
	 * Hashes the Stack by combining the hashes of both buffers.
	 * Purpose: Enables fast lookup and deduplication in hash-based collections.
	 *
	 * Time Complexity: O(n), n = stack size
	 * Space Complexity: O(1)
	 *
	 * @return Hash code based on both buffers.
	 */
	override fun hashCode(): Int {
		var result = a.hashCode()
		result = HASH_PRIME * result + b.hashCode()
		return result
	}
}