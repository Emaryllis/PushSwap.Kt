package me.emaryllis.a_star

import me.emaryllis.Settings.MAX_CHUNK_SIZE
import me.emaryllis.data.Stack

class MixedHeuristic {
	/**
	 * Calculates the heuristic value for the current stack state.
	 * Purpose: Estimates the cost to reach the goal state for the current chunk.
	 * 1. Finds the length of the contiguous ascending prefix of chunk elements in stack A.
	 * 2. If stack B is empty and the prefix length equals the chunk size, return 0 (goal state).
	 * 3. Counts inversions in the prefix of stack A.
	 * 4. Counts descending inversions in stack B for chunk elements.
	 * 5. Finds minimal cost to push a chunk element from A to B.
	 * 6. Finds minimal cost to pull a chunk element from B to A.
	 * 7. Selects the next minimal move cost.
	 *
	 * Time complexity: O(n²) -> n = max(m, k), m = A's size, k = B's size (Due to [prefixInversions])
	 * Space complexity: O(1) ([Int]'s size is constant)
	 */
	fun calculate(stack: Stack): Int {
		val chunkSize = stack.chunk.values.size
		val prefixLen = contiguousAscendingPrefixLen(stack, chunkSize)
		if (stack.b.isEmpty() && prefixLen == chunkSize) return 0 // If goal state is reached
		val cNext = nextCost(candidatePushCost(stack, prefixLen, chunkSize), candidatePullCost(stack, prefixLen))
		return prefixInversions(stack, prefixLen) + descendingInversionsB(stack) + cNext
	}

	/**
	 * Finds the length of the contiguous ascending prefix of chunk elements in stack A.
	 * For the top of stack A:
	 * - Start from index 0 and increment prefixLen while:
	 *    - The element is in the current chunk.
	 *    - The element is greater than or equal to the previous (ascending order).
	 * - Stop at the first element that breaks these conditions.
	 *
	 * Time Complexity: O(min(m, c)) -> m = A's size, c = chunk size. (For most cases, it's [MAX_CHUNK_SIZE])
	 * Space Complexity: O(1)
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
	 * Counts inversions in the prefix of stack A.
	 * Only considers the contiguous ascending prefix
	 * of chunk elements at the top of stack A.
	 *
	 * Time Complexity: O(p²) -> p = prefixLen [contiguousAscendingPrefixLen]
	 * Space Complexity: O(1)
	 *
	 * @return Total inversions, where an inversion is a pair of elements in the
	 * prefix such that the earlier element is greater than the later one.
	 */
	private fun prefixInversions(stack: Stack, prefixLen: Int): Int {
		if (prefixLen <= 1) return 0
		var inversions = 0
		for (i in 0 until prefixLen) {
			for (j in i + 1 until prefixLen) {
				if (stack.a[i] > stack.a[j]) inversions++
			}
		}
		return inversions
	}

	/**
	 * Counts descending inversions in stack B for chunk elements.
	 * Only considers elements that are part of the current chunk.
	 *
	 * Time Complexity: O(k²) -> k = B's size.
	 * Space Complexity: O(1)
	 *
	 * @return Total descending inversions, where a descending
	 * inversion is a pair of chunk elements in B such that
	 * the earlier element is less than the later one.
	 */
	private fun descendingInversionsB(stack: Stack): Int {
		if (stack.b.size <= 1) return 0
		var pB = 0
		for (i in 0 until stack.b.size) {
			if (stack.b[i] !in stack.chunk) continue
			for (j in i + 1 until stack.b.size) {
				if (stack.b[j] !in stack.chunk) continue
				if (stack.b[i] < stack.b[j]) pB++
			}
		}
		return pB
	}

	/**
	 * 5. Finds minimal cost to push a chunk element from A to B.
	 * For each possible element to push from stack A to B:
	 * - Find its index i in A and target index j in B.
	 * - Compute four possible rotation costs:
	 * - Rotate both stacks forward together: max(i, j)
	 * - Rotate both stacks backward together: max(A's size - i, B's size - j)
	 * - Rotate A forward, B backward: i + (B's size - j)
	 * - Rotate A backward, B forward: (A's size - i) + j
	 * - Take the minimum of these four costs.
	 * - Add 1 to account for the push operation itself.
	 *
	 * Time Complexity: O((m-p)*k) -> m = A's size, p = prefixLen [contiguousAscendingPrefixLen], k = B's size.
	 * Space Complexity: O(1).
	 *
	 * @return The minimal push cost from the smallest candidate value found, or -1 if no valid push candidates exist.
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
			val candidate = minOf(
				maxOf(i, j), maxOf((stack.a.size - i), stack.b.size - j),
				i + stack.b.size - j, (stack.a.size - i) + j
			) + 1
			if (candidate < minPushCost) minPushCost = candidate
		}
		return if (minPushCost == Int.MAX_VALUE) -1 else minPushCost
	}

	/**
	 * 6. Finds minimal cost to pull a chunk element from B to A.
	 * For each chunk element in stack B:
	 * - Calculate the minimal rotation needed to bring it to the top (forward or backward).
	 * - If it would create an inversion with the current prefix in A, add 1 to the cost.
	 * - The candidate cost is: min(rotation forward, rotation backward) + 1 (+1 if inversion).
	 * - Return the smallest candidate cost among all valid elements.
	 *
	 * Time Complexity: O(k) -> k = B's size.
	 * Space Complexity: O(1)
	 */
	private fun candidatePullCost(stack: Stack, prefixLen: Int): Int {
		if (stack.b.isEmpty()) return -1
		var minPullCost = Int.MAX_VALUE
		val prefixMax = if (prefixLen > 0) stack.a[prefixLen - 1] else Int.MIN_VALUE
		for (k in 0 until stack.b.size) {
			if (stack.b[k] !in stack.chunk) continue
			val invInc = if (prefixLen > 0 && stack.b[k] < prefixMax) 1 else 0
			val candidate = minOf(k, stack.b.size - k) + 1 + invInc
			if (candidate < minPullCost) minPullCost = candidate
		}
		return if (minPullCost == Int.MAX_VALUE) -1 else minPullCost
	}

	/**
	 * 7. Selects the next minimal move cost.
	 * Given the minimal push and pull costs:
	 * - If both are valid (>= 0), return the smaller.
	 * - If only one is valid, return that one.
	 * - If neither is valid, return 0.
	 *
	 * Time & Space Complexity: O(1)
	 */
	private fun nextCost(minPushCost: Int, minPullCost: Int): Int =
		when {
			minPushCost >= 0 && minPullCost >= 0 -> minOf(minPushCost, minPullCost)
			minPushCost >= 0 -> minPushCost
			minPullCost >= 0 -> minPullCost
			else -> 0
		}
}
