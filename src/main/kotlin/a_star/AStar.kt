package me.emaryllis.a_star

import me.emaryllis.Settings.DEBUG
import me.emaryllis.data.Move
import me.emaryllis.data.PriorityQueue
import me.emaryllis.data.Stack
import me.emaryllis.utils.Debug.getStackInfo

/**
 * Implements the A* search algorithm for chunk-based stack sorting.
 * Purpose: Finds the optimal sequence of moves to sort the current chunk in stack A using mixed heuristics.
 *
 * Time & Space Complexity: See [sort]
 */
class AStar {
	private val mixedHeuristic = MixedHeuristic()

	/**
	 * Entry point for A* sorting.
	 * Purpose: Clones the input stack, computes its heuristic, and runs the A* search.
	 *
	 * Time complexity:
	 *   - O(n^2) See [MixedHeuristic].
	 *   - O(S * T) for search -> n = A's size
	 *       - S = unique states explored (can be exponential in n in worst case)
	 *       - T = time to generate successors (O(moves * n) per state, moves = allowed moves)
	 *
	 * Space complexity:
	 *   - O(n) for stack.clone() (deep copy of stack data).
	 *   - O(S * n) for search structures: openList and visited set each store up to S Stack objects, each using O(n) space.
	 * @return [Stack] sorted for the current chunk.
	 * @see MixedHeuristic
	 * @see Stack
	 */
	fun sort(stack: Stack): Stack {
		var newStack = stack.clone()
		newStack.heuristic = mixedHeuristic.calculate(newStack)
		if (DEBUG) println("Starting mixed-mode search: ${getStackInfo(newStack, false)}")
		newStack = aStar(newStack)
		return newStack
	}

	/**
	 * Core A* search loop.
	 * Purpose: Pops states from [PriorityQueue], checks for goal,
	 * and expands the best successors [BestStates.getBestStates].
	 * - Uses [PriorityQueue] to always expand the most promising state.
	 * - Tracks visited states to avoid cycles.
	 *
	 * Time complexity: O(S * T), where S = unique states explored, T = See [BestStates.getBestStates]
	 * Space complexity: O(S * n): openList and visited set can each store up to S Stack objects, each using O(n) space.
	 *
	 * @return [Stack] that satisfies the goal condition.
	 * @see BestStates.getBestStates
	 * @see PriorityQueue
	 * @see Stack
	 */
	private fun aStar(start: Stack): Stack {
		val openList = PriorityQueue<Stack> { it.currentCost }
		openList.push(start)
		val visited = mutableSetOf<Int>()
		var iteration = 0
		var maxOpenListSize = 0
		while (openList.isNotEmpty()) {
			val current = openList.pop()
			iteration++
			if (openList.size > maxOpenListSize) maxOpenListSize = openList.size
			if (DEBUG) println("\nI:$iteration Size:${openList.size} ${getStackInfo(current)}")
			if (goal(current)) {
				println("Chunk ${start.chunk.minValue}-${start.chunk.maxValue}: $iteration iterations, peak open list $maxOpenListSize")
				return current
			}
			if (!visited.add(current.hashCode())) continue
			val successors = BestStates().getBestStates(current, Move.mixedAllowed)
			successors.forEach { openList.push(it) }
		}
		error("Failed to find solution for chunk ${start.chunk.minValue}-${start.chunk.maxValue}")
	}

	/**
	 * Goal check for A*.
	 * - B must be empty.
	 * - All chunk values form one contiguous ascending block in A via [findChunkBlock].
	 * - No future elements appear after the block via [hasFutureElementsAfter].
	 *
	 * Time complexity: O(n) -> n = A's size.
	 * Space complexity: O(1).
	 *
	 * @return true if the goal condition is satisfied.
	 */
	private fun goal(stack: Stack): Boolean {
		if (stack.b.isNotEmpty()) return false
		val blockEnd = findChunkBlock(stack) ?: return false
		return !hasFutureElementsAfter(stack, blockEnd)
	}

	/**
	 * Scans A for a contiguous ascending block of all current chunk values.
	 * - Returns the index immediately after the block if found.
	 * - Returns null if the block is incomplete, out of order, or fragmented.
	 *
	 * Time complexity: O(n) -> n = A's size.
	 * Space complexity: O(1).
	 *
	 * @return Index after the chunk block end, or null if no valid block found.
	 */
	private fun findChunkBlock(stack: Stack): Int? {
		var count = 0
		var last = Int.MIN_VALUE
		for (i in 0 until stack.a.size) {
			if (stack.a[i] in stack.chunk) {
				if (count > 0 && stack.a[i] < last) return null
				last = stack.a[i]
				count++
			} else if (count > 0) {
				return if (count == stack.chunk.values.size) i else null
			}
		}
		return if (count == stack.chunk.values.size) stack.a.size else null
	}

	/**
	 * Checks whether any future elements above [Stack.prevChunkNum] exist in A at/after
	 * [fromIndex]. Future elements in this region violate the alignment invariant since
	 * the chunk block must sit directly above prevChunk's block. Always returns
	 *
	 * Time complexity: O(n) -> n = A's size.
	 * Space complexity: O(1).
	 *
	 * @param fromIndex Index immediately after the chunk block end.
	 * @return true if any future element is found after the chunk block,
	 * false if [Stack.prevChunkNum] is null (first chunk has no constraint)
	 */
	private fun hasFutureElementsAfter(stack: Stack, fromIndex: Int): Boolean {
		if (stack.prevChunkNum == null) return false
		for (j in fromIndex until stack.a.size) {
			if (stack.a[j] > stack.prevChunkNum!!) return true
		}
		return false
	}
}