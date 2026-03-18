package me.emaryllis.a_star

import me.emaryllis.Settings.MAX_CHUNK_SIZE
import me.emaryllis.data.Stack

class MixedHeuristic {
	/**
	 * Calculates the heuristic value for the current stack state.
	 * Purpose: Estimates the cost to reach the goal state for the current chunk.
	 * 1. Finds the length of the contiguous ascending prefix of chunk elements in stack A.
	 * 2. Finds minimal cost to push a chunk element from A to B.
	 * 3. Finds minimal cost to pull a chunk element from B to A.
	 * 4. Selects the next minimal move cost.
	 *
	 * Time complexity: O((m-p)*k) -> m = A's size, p = prefixLen, k = B's size (Due to [candidatePushCost])
	 * Space complexity: O(1) ([Int]'s size is constant)
	 */
	fun calculate(stack: Stack): Int {
		val chunkSize = stack.chunk.values.size
		val prefixLen = contiguousAscendingPrefixLen(stack, chunkSize)
		if (stack.b.isEmpty() && prefixLen == chunkSize) return 0 // If goal state is reached
		return nextCost(candidatePushCost(stack, prefixLen, chunkSize), candidatePullCost(stack, prefixLen))
	}

	/**
	 * 1. Finds the length of the contiguous ascending prefix of chunk elements in stack A.
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
	 * Time & Space Complexity: O(1)
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
	 * 2. Finds minimal cost to push a chunk element from A to B.
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
	 * - If it would sit above a smaller element in the current prefix (i.e. b[[k]] > a[0]),
	 *   add 1 to account for the extra move needed to resolve the resulting inversion.
	 * - The candidate cost is: min(k, (B's size - k) % B's size) + 1 (+1 if inversion).
	 * - Return the smallest candidate cost among all valid elements.
	 *
	 * Time Complexity: O(k) -> k = B's size.
	 * Space Complexity: O(1)
	 *
	 * @return The minimal pull cost among all valid candidates, or -1 if B is empty.
	 */
	private fun candidatePullCost(stack: Stack, prefixLen: Int): Int {
		if (stack.b.isEmpty()) return -1
		var minPullCost = Int.MAX_VALUE
		val prefixMin = if (prefixLen > 0) stack.a[0] else Int.MAX_VALUE
		for (k in 0 until stack.b.size) {
			if (stack.b[k] !in stack.chunk) continue
			val invInc = if (prefixLen > 0 && stack.b[k] > prefixMin) 1 else 0
			val candidate = minOf(k, (stack.b.size - k) % stack.b.size) + 1 + invInc
			if (candidate < minPullCost) minPullCost = candidate
		}
		return if (minPullCost == Int.MAX_VALUE) -1 else minPullCost
	}

	/**
	 * 4. Selects the next minimal move cost.
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
