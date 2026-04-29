package me.emaryllis.a_star

import me.emaryllis.Settings.CHUNK_DEBUG
import me.emaryllis.Settings.STACK_DEBUG
import me.emaryllis.data.Move
import me.emaryllis.data.PriorityQueue
import me.emaryllis.data.SearchDebugMetrics
import me.emaryllis.data.Stack
import me.emaryllis.utils.DebugUtils.getStackInfo

/**
 * Implements the A* search algorithm for chunk-based stack sorting.
 * Purpose: Finds the optimal sequence of moves to sort the current chunk in stack A using mixed heuristics.
 *
 * Time & Space Complexity: See [sort]
 */
class AStar {
	private val metrics = SearchDebugMetrics()
	private val mixedHeuristic = MixedHeuristic()
	private val bestStates = BestStates(metrics, mixedHeuristic)

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
		if (CHUNK_DEBUG) println("Starting mixed-mode search: ${getStackInfo(newStack, false)}")
		newStack = aStar(newStack)
		return newStack
	}

	private fun initAStar(start: Stack): Triple<PriorityQueue<Stack>, MutableSet<Long>, MutableMap<Long, Int>> {
		metrics.reset()
		val openList = PriorityQueue<Stack> { left, right ->
			val byF = left.currentCost.compareTo(right.currentCost)
			if (byF != 0) return@PriorityQueue byF
			val byH = left.heuristic.compareTo(right.heuristic)
			if (byH != 0) return@PriorityQueue byH
			left.moves.size.compareTo(right.moves.size)
		}
		openList.push(start)
		val visited = mutableSetOf<Long>()
		val bestGByHash = mutableMapOf<Long, Int>()
		bestGByHash[start.hash64()] = start.moves.size
		return Triple(openList, visited, bestGByHash)
	}

	/**
	 * Core A* search loop.
	 * Purpose: Pops states from [PriorityQueue], checks for goal,
	 * and expands the best successors [BestStates.getBestStates].
	 * - Uses [PriorityQueue] to always expand the most promising state.
	 * - Tracks visited states to avoid cycles.
	 *
	 * Time complexity: O(S * T), where S = unique states explored, T = See [BestStates.getBestStates].
	 * Space complexity: O(S * n): openList and visited set can each store up to S Stack objects, each using O(n) space.
	 *
	 * @return [Stack] that satisfies the goal condition.
	 * @see BestStates.getBestStates
	 * @see PriorityQueue
	 * @see Stack
	 */
	private fun aStar(start: Stack): Stack {
		val (openList, visited, bestGByHash) = initAStar(start)
		while (openList.isNotEmpty()) {
			val current = openList.pop()
			if (CHUNK_DEBUG || STACK_DEBUG) metrics.iteration++
			if (openList.size > metrics.maxOpenListSize) metrics.maxOpenListSize = openList.size
			if (STACK_DEBUG) println("\nI:${metrics.iteration} Size:${openList.size} ${getStackInfo(current)}")
			val currentHash = current.hash64()
			val bestKnownCurrentG = bestGByHash[currentHash]
			if (bestKnownCurrentG != null && current.moves.size > bestKnownCurrentG) {
				if (CHUNK_DEBUG) metrics.stalePops++
				continue
			}
			if (goal(current)) {
				if (CHUNK_DEBUG) {
					println("Chunk ${start.chunk.minValue}-${start.chunk.maxValue}: ${metrics.iteration} iterations, peak open list ${metrics.maxOpenListSize}")
					println("A* metrics ${start.chunk.minValue}-${start.chunk.maxValue}:")
					metrics.printSearchMetrics()
					metrics.printInvalidationMetrics()
				}
				return current
			}
			if (!visited.add(currentHash)) continue
			addSuccessors(current, visited, bestGByHash, openList)
		}
		error("Failed to find solution for chunk ${start.chunk.minValue}-${start.chunk.maxValue}")
	}

	private fun addSuccessors(
		current: Stack, visited: Set<Long>, bestGByHash: MutableMap<Long, Int>,
		openList: PriorityQueue<Stack>
	) {
		val successors = bestStates.getBestStates(current, Move.mixedAllowed)
		successors.forEach { successor ->
			if (CHUNK_DEBUG) metrics.consideredSuccessors++
			val successorHash = successor.hash64()
			if (visited.contains(successorHash)) {
				if (CHUNK_DEBUG) metrics.prunedVisitedSuccessors++
				return@forEach
			}
			val newG = successor.moves.size
			val oldG = bestGByHash[successorHash]
			if (oldG != null && newG >= oldG) {
				if (CHUNK_DEBUG) metrics.prunedDominatedSuccessors++
				return@forEach
			}
			bestGByHash[successorHash] = newG
			openList.push(successor)
		}
	}

	/**
	 * Goal check for A*.
	 * - B must be empty.
	 * - All chunk values form one contiguous ascending block in A.
	 *
	 * Time complexity: O(n) -> n = A's size.
	 * Space complexity: O(1).
	 *
	 * @return true if the goal condition is satisfied, false otherwise.
	 */
	private fun goal(stack: Stack): Boolean {
		if (stack.b.isNotEmpty()) return false
		val firstIndex = stack.a.indexOf(stack.chunk.minValue)
		require(firstIndex != -1) { "${stack.chunk.minValue} not found in A: ${stack.a.value}" }
		if (stack.prevChunkNum != null &&
			stack.prevChunkNum != stack.a[(firstIndex - 1 + stack.a.size) % stack.a.size]
		) return false
		var i = (firstIndex + 1) % stack.a.size
		var count = 1

		// Exits if all chunk values are found, or not all chunk values found after looping back
		while (count != stack.chunk.values.size && i != firstIndex) {
			if (stack.a[i] != stack.chunk.values[count]) return false
			count++
			i = (i + 1) % stack.a.size
		}
		return stack.chunk.values.size == count
	}
}