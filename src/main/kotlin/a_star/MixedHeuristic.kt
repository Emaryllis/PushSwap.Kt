package me.emaryllis.a_star

import me.emaryllis.data.Stack

class MixedHeuristic {
	/**
	 * Calculates the heuristic value for the current stack state.
	 * Purpose: Estimates the cost to reach the goal state for the current chunk.
	 * 1. Finds the length of the contiguous ascending prefix of chunk elements in A. [contiguousAscendingPrefixLen]
	 * 2. If B is empty and prefix covers the full chunk, returns 0.
	 * 3. Computes a conservative lower bound via [candidatePushCost], [candidatePullCost], and [nextCost].
	 * 4. Combines independent lower bounds with max() to avoid double-counting.
	 *
	 * Time complexity: O((m-p)*k) -> m = A's size, p = prefixLen, k = B's size (Due to [candidatePushCost])
	 * Space complexity: O(1).
	 */
	fun calculate(stack: Stack): Int {
		val chunkSize = stack.chunk.values.size
		if (isChunkGoal(stack, chunkSize)) return 0
		val prefixLen = contiguousAscendingPrefixLen(stack, chunkSize)

		// Strictly admissible composition: combine only independent lower bounds via max.
		val directionalLowerBound = nextCost(
			candidatePushCost(stack, prefixLen, chunkSize),
			candidatePullCost(stack)
		)
		val mustEmptyBLowerBound = stack.b.size
		val chunkRunLowerBound = cyclicChunkRunDeficit(stack)
		return maxOf(directionalLowerBound, mustEmptyBLowerBound, chunkRunLowerBound)
	}

	/**
	 * Returns true when the current chunk already satisfies the circular goal shape:
	 * B is empty and the chunk values appear as one ascending cyclic block in A.
	 */
	private fun isChunkGoal(stack: Stack, chunkSize: Int): Boolean {
		if (stack.b.isNotEmpty()) return false
		if (chunkSize == 0) return true
		val firstIndex = stack.a.indexOf(stack.chunk.minValue)
		if (firstIndex == -1) return false
		if (stack.prevChunkNum != null && stack.prevChunkNum != stack.a[(firstIndex - 1 + stack.a.size) % stack.a.size]) return false

		var i = (firstIndex + 1) % stack.a.size
		var count = 1
		while (count != chunkSize && i != firstIndex) {
			if (stack.a[i] != stack.chunk.values[count]) return false
			count++
			i = (i + 1) % stack.a.size
		}
		return count == chunkSize
	}

	/**
	 * 1. Finds the length of the contiguous ascending prefix of chunk elements in stack A.
	 * For the top of stack A:
	 * - Start from index 0 and increment prefixLen while:
	 *    - The element is in the current chunk.
	 *    - The element is greater than or equal to the previous (ascending order).
	 * - Stop at the first element that breaks these conditions.
	 *
	 * Time Complexity: O(min(m, c)) -> m = A's size, c = [chunkSize].
	 * Space Complexity: O(1).
	 *
	 * @return Length of the contiguous ascending prefix of chunk elements in stack A.
	 */
	private fun contiguousAscendingPrefixLen(stack: Stack, chunkSize: Int): Int {
		var prefixLen = 0
		var last = Int.MIN_VALUE
		while (prefixLen < stack.a.size && prefixLen < chunkSize) {
			val v = stack.a[prefixLen]
			if (v !in stack.chunk) break
			if (last != Int.MIN_VALUE && v < last) break
			last = v
			prefixLen++
		}
		return prefixLen
	}

	/**
	 * Computes the minimum combined rotation cost to bring index [i] in A and index [j] in B
	 * both to their respective heads simultaneously, using RA/RB, RRA/RRB, or their combinations.
	 * Since A and B are circular buffers, backward rotation cost wraps around:
	 * - aRev = ([aSize] - [i]) % [aSize]: RRA cost for A to bring index [i] to head.
	 * - bRev = ([bSize] - [j]) % [bSize]: RRB cost for B to bring index [j] to head.
	 * The modulo handles [i]/[j] = 0, where rotation/reverse-rotation cost is 0, not [aSize].
	 *
	 * Four candidate costs:
	 * - Rotate both forward: max([i], [j])
	 * - Rotate both backward: max(aRev, bRev)
	 * - Rotate A forward, B backward: [i] + bRev
	 * - Rotate A backward, B forward: aRev + [j]
	 *
	 * Time & Space Complexity: O(1).
	 *
	 * @return Minimum rotation cost to align both indices to head.
	 */
	private fun minRotationCost(i: Int, j: Int, aSize: Int, bSize: Int): Int {
		val aRev = if (aSize > 0) (aSize - i) % aSize else 0
		val bRev = if (bSize > 0) (bSize - j) % bSize else 0
		return minOf(
			maxOf(i, j), maxOf(aRev, bRev),
			i + bRev, aRev + j
		)
	}

	/**
	 * Finds minimal cost to push a chunk element from A to B.
	 * For each possible element to push from stack A to B:
	 * - Find its index i in A and target index j in B.
	 * - Compute the minimum combined rotation cost via [minRotationCost].
	 * - Add 1 to account for the push operation itself.
	 *
	 * Time Complexity: O((m-p)*k) -> m = A's size, p = [prefixLen], k = B's size.
	 * Space Complexity: O(1).
	 *
	 * @param prefixLen The output of [contiguousAscendingPrefixLen]
	 * @return The minimal push cost among all valid candidates, or -1 if none exist.
	 */
	private fun candidatePushCost(stack: Stack, prefixLen: Int, chunkSize: Int): Int {
		if (prefixLen >= chunkSize) return -1
		var minPushCost = Int.MAX_VALUE
		for (i in prefixLen until stack.a.size) {
			if (stack.a[i] !in stack.chunk) continue
			var j = 0
			while (j < stack.b.size) {
				if (stack.b[j] < stack.a[i]) break
				j++
			}
			val candidate = minRotationCost(i, j, stack.a.size, stack.b.size) + 1
			if (candidate < minPushCost) minPushCost = candidate
		}
		return if (minPushCost == Int.MAX_VALUE) -1 else minPushCost
	}

	/**
	 * 3. Finds minimal cost to pull a chunk element from B to A.
	 * For each chunk element in stack B:
	 * - Calculate the minimal rotation needed to bring it to the top (forward or backward).
	 * - The candidate cost is: min(k, (B's size - k) % B's size) + 1.
	 * - Return the smallest candidate cost among all valid elements.
	 *
	 * Time Complexity: O(k) -> k = B's size.
	 * Space Complexity: O(1).
	 *
	 * @return The minimal pull cost among all valid candidates, or -1 if B is empty.
	 */
	private fun candidatePullCost(stack: Stack): Int {
		if (stack.b.isEmpty()) return -1
		var minPullCost = Int.MAX_VALUE
		for (k in stack.b.indices) {
			val candidate = minOf(k, (stack.b.size - k) % stack.b.size) + 1
			if (candidate < minPullCost) minPullCost = candidate
		}
		return if (minPullCost == Int.MAX_VALUE) -1 else minPullCost
	}

	/**
	 * Lower bound based on how fragmented the current chunk is in A.
	 * Each additional cyclic run of current-chunk values needs at least one
	 * move to merge into a single contiguous block.
	 */
	private fun cyclicChunkRunDeficit(stack: Stack): Int {
		if (stack.a.isEmpty()) return 0
		val indices = stack.a.indices.filter { stack.a[it] in stack.chunk }
		if (indices.isEmpty()) return 0

		var runs = 1
		for (i in 1 until indices.size) {
			if (indices[i] != indices[i - 1] + 1) runs++
		}
		if (indices.first() == 0 && indices.last() == stack.a.size - 1) runs--
		return maxOf(0, runs - 1)
	}

	/**
	 * Selects the next minimal move cost.
	 * Given the minimal push and pull costs:
	 * - If both are valid (>= 0), return the smaller.
	 * - If only one is valid, return that one.
	 * - If neither is valid, return 0.
	 *
	 * Time & Space Complexity: O(1).
	 */
	private fun nextCost(minPushCost: Int, minPullCost: Int): Int =
		when {
			minPushCost >= 0 && minPullCost >= 0 -> minOf(minPushCost, minPullCost)
			minPushCost >= 0 -> minPushCost
			minPullCost >= 0 -> minPullCost
			else -> 0
		}
}