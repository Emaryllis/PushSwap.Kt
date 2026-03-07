package me.emaryllis.a_star

import me.emaryllis.Settings.DEBUG
import me.emaryllis.data.Move
import me.emaryllis.data.Stack
import me.emaryllis.utils.Debug.getStackInfo

/**
 * BestStates generates all valid next states from a given stack and allowed moves.
 * - Uses MixedHeuristic for scoring.
 * - Applies conditional swap optimizations (SS > SA > SB).
 * - Guards against invalid moves and redundant expansions.
 *
 * - Time & space complexity: See [getBestStates].
 *
 * @see getBestStates
 */
class BestStates {
	private val heuristic = MixedHeuristic()

	/**
	 * Returns all valid successor states from originalStack using allowedMoves.
	 * - Applies each move, checks validity, and optimizes with swaps.
	 * - Lets the priority queue handle ordering; no beam narrowing.
	 *
	 * - Time complexity: O(m * f) -> m = [allowedMoves]'s size, f = [applyMoveIfValid].
	 * - Space complexity: O(m) -> output of [Stack] size
	 *
	 * @see Stack
	 * @see applyMoveIfValid
	 */
	fun getBestStates(
		originalStack: Stack,
		allowedMoves: List<Move>,
	): List<Stack> {
		val possibleStates = mutableListOf<Stack>()
		for (move in allowedMoves) {
			val currentStack = applyMoveIfValid(originalStack, move)
			if (currentStack != null) {
				if (DEBUG) println(
					"\nApplied move: $move (Valid moves: $allowedMoves)\n" +
							"Before:\t${getStackInfo(originalStack)}\nAfter:\t${getStackInfo(currentStack)}"
				)
				possibleStates.add(currentStack)
			}
		}
		return possibleStates
	}

	/**
	 * Fast invalidation check for moves.
	 * - Prunes inverse moves.
	 * - Guards PB to only push chunk elements from A to B. (Obtained from [Stack.chunk])
	 * - Guards PA to only pull if B not empty (B should only contain current chunk values by invariant).
	 *
	 * - Time & space complexity: O(1).
	 */
	private fun invalidFast(originalStack: Stack, move: Move): Boolean {
		if (originalStack.moves.isNotEmpty() && originalStack.moves.lastOrNull() == move.inverse()) return true
		if (move == Move.PB && (originalStack.a.isEmpty() || originalStack.a.first() !in originalStack.chunk)) return true
		if (move == Move.PA && originalStack.b.isEmpty()) return true
		return false
	}

	/**
	 * Applies a move to a clone of the original stack if valid.
	 * - Recomputes heuristic after move and after conditional swaps.
	 * - Returns null if move is invalid or heuristic < 0.
	 * - Time complexity: O(f), f = cost of [Stack.clone] + [Stack.apply] + [MixedHeuristic] + [conditionalOptimize].
	 * - Space complexity: O(m) -> m = [Stack.clone]'s size.
	 *
	 * @see Stack.clone
	 * @see Stack.apply
	 * @see MixedHeuristic
	 * @see conditionalOptimize
	 */
	private fun applyMoveIfValid(original: Stack, move: Move): Stack? {
		if (invalidFast(original, move)) return null
		val stack = original.clone()
		if (!stack.apply(move)) return null
		stack.heuristic = heuristic.calculate(stack)
		if (conditionalOptimize(stack)) {
			stack.heuristic = heuristic.calculate(stack)
		}
		if (stack.heuristic < 0) return null
		return stack
	}

	/**
	 * Opportunistically applies conditional swaps (SS > SA > SB) to improve ordering.
	 * Ignoring the return type of [Stack.apply] since preconditions were checked.
	 * - Checks if it is able to swap both.
	 * - Checks if it is able to swap A [canSA]
	 * - Checks if its able to swap B.
	 *
	 * Conditions:
	 * - Swapping A: [canSA]
	 * - Swapping B: only if B has at least 2 elements and b[0] < b[1].
	 * (Improves descending order since pushing is FILO)
	 *
	 * Time & Space Complexity: O(1).
	 *
	 * @return true if a swap was made, false otherwise.
	 */
	private fun conditionalOptimize(stack: Stack): Boolean {
		val canSA = canSA(stack)
		val canSB = stack.b.size >= 2 && stack.b[0] < stack.b[1]

		return when {
			canSA && canSB -> stack.apply(Move.SS)
			canSA -> stack.apply(Move.SA)
			canSB -> stack.apply(Move.SB)
			else -> false
		}
	}

	/**
	 * Checks if SA (swap A) is valid and beneficial.
	 * - Only swaps current chunk elements, not previous chunk values.
	 * - Improves ascending order if a[0] > a[1].
	 *
	 * Time & Space Complexity: O(1).
	 *
	 * @return true if SA is valid and beneficial, false otherwise.
	 */
	private fun canSA(stack: Stack): Boolean {
		if (stack.a.size < 2) return false
		if (stack.a[0] !in stack.chunk || stack.a[1] !in stack.chunk) return false
		if (stack.prevChunkNum != null && (stack.a[0] <= stack.prevChunkNum!! || stack.a[1] <= stack.prevChunkNum!!)) return false
		return stack.a[0] > stack.a[1]
	}
}