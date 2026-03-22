package me.emaryllis.a_star

import me.emaryllis.Settings.MAX_CHUNK_SIZE
import me.emaryllis.data.Chunk
import me.emaryllis.data.Stack

class MixedHeuristic {
	/**
	 * Calculates the heuristic value for the current stack state.
	 * Purpose: Estimates the cost to reach the goal state for the current chunk.
	 * 1. Finds the length of the contiguous ascending prefix of chunk elements in A. [contiguousAscendingPrefixLen]
	 * 2. If B is empty and prefix covers the full chunk, returns [alignmentGapCost].
	 * 3. Computes the minimal next move cost via [candidatePushCost], [candidatePullCost], and [nextCost].
	 * 4. For chunk 2 onwards ([Stack.prevChunkNum] set), adds alignment penalties:
	 *    [crossStackDisorder] + [remainingChunkPenalty] + [futureRotateBound].
	 *
	 * Time complexity: O((m-p)*k) -> m = A's size, p = prefixLen, k = B's size (Due to [candidatePushCost])
	 * Space complexity: O(1)
	 */
	fun calculate(stack: Stack): Int {
		val chunkSize = stack.chunk.values.size
		val prefixLen = contiguousAscendingPrefixLen(stack, chunkSize)
		if (stack.b.isEmpty() && prefixLen == chunkSize) return alignmentGapCost(stack, chunkSize)
		val next = nextCost(candidatePushCost(stack, prefixLen, chunkSize), candidatePullCost(stack, prefixLen))
		if (stack.prevChunkNum == null) return next
		return next + crossStackDisorder(stack, prefixLen) + remainingChunkPenalty(stack, prefixLen, chunkSize) + futureRotateBound(stack)
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
	 * Near-goal cost when B is empty and the full chunk prefix is formed
	 * in A. Does nothing if there is only 1 chunk. Otherwise, counts
	 * future elements still above the previous chunk's maximum
	 * value. Each requires at least 1 move to clear.
	 *
	 * Time Complexity: O(m) -> m = A's size.
	 * Space Complexity: O(1)
	 *
	 * @return Number of future elements above [Stack.prevChunkNum], or 0 if [Stack.prevChunkNum] is null.
	 */
	private fun alignmentGapCost(stack: Stack, chunkSize: Int): Int {
		if (stack.prevChunkNum == null) return 0
		var gap = 0
		var i = chunkSize
		while (i < stack.a.size && stack.a[i] > stack.prevChunkNum!!) {
			gap++
			i++
		}
		return gap
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
	 * Selects the next minimal move cost.
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

	/**
	 * Cross-stack disorder penalty: counts chunk elements in B that are smaller
	 * than the maximum element of the confirmed ascending prefix in A (a[[prefixLen] - 1]).
	 * Each such element requires at least 1 PA to restore ascending order,
	 * making this a valid admissible lower bound.
	 * Only applied when [Stack.prevChunkNum] is set (chunk 2 onwards).
	 *
	 * Time Complexity: O(k) -> k = B's size.
	 * Space Complexity: O(1)
	 *
	 * @return Count of out-of-order chunk elements in B relative to prefix max.
	 */
	private fun crossStackDisorder(stack: Stack, prefixLen: Int): Int {
		if (prefixLen == 0 || stack.b.isEmpty()) return 0
		val prefixMax = stack.a[prefixLen - 1]
		var count = 0
		for (i in 0 until stack.b.size) {
			val v = stack.b[i]
			if (v in stack.chunk && v < prefixMax) count++
		}
		return count
	}

	/**
	 * Prevents the heuristic from collapsing to near-zero when only a tiny
	 * prefix is formed but most chunk elements are unresolved.
	 * Trivially admissible, at least 1 more move is required if any remain.
	 * Only applied when [Stack.prevChunkNum] is set (chunk 2 onwards).
	 *
	 * Time Complexity: O(m + k) -> m = A's size, k = B's size.
	 * Space Complexity: O(1)
	 *
	 * @return 1 if any chunk elements remain outside the ascending
	 * prefix (still in A's suffix or in B), 0 otherwise
	 */
	private fun remainingChunkPenalty(stack: Stack, prefixLen: Int, chunkSize: Int): Int {
		if (prefixLen >= chunkSize) return 0
		for (i in prefixLen until stack.a.size) if (stack.a[i] in stack.chunk) return 1
		for (i in 0 until stack.b.size) if (stack.b[i] in stack.chunk) return 1
		return 0
	}

	/**
	 * Future rotate bound: counts future elements above the current [Chunk.maxValue] in
	 * A that appear after the prevChunk block. Each must be rotated past the eventual
	 * goal position, requiring at least 1 move each -> admissible lower bound.
	 * Skips leading prevChunk elements before counting.
	 * Only applied when [Stack.prevChunkNum] is set (chunks 2 onwards).
	 *
	 * Time Complexity: O(m) -> m = A's size.
	 * Space Complexity: O(1)
	 *
	 * @return Count of future elements after the prevChunk block.
	 */
	private fun futureRotateBound(stack: Stack): Int {
		if (stack.prevChunkNum == null) return 0
		var count = 0
		var i = 0
		while (i < stack.a.size && stack.a[i] <= stack.prevChunkNum!!) i++
		while (i < stack.a.size) {
			if (stack.a[i] > stack.chunk.maxValue) count++
			i++
		}
		return count
	}
}
