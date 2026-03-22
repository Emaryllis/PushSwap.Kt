package me.emaryllis.chunk

import me.emaryllis.Settings.DEBUG
import me.emaryllis.Settings.MAX_CHUNK_SIZE
import me.emaryllis.a_star.AStar
import me.emaryllis.data.Chunk
import me.emaryllis.data.CircularBuffer
import me.emaryllis.data.Move
import me.emaryllis.data.Stack
import me.emaryllis.utils.Debug.getMoveInfo
import me.emaryllis.utils.Debug.getStackInfo
import java.lang.Integer.min

/**
 * ChunkSort divides a list of integer into chunks.
 * If there are only 5 or less integers, it uses [SmallSort].
 * If not, it sorts each chunk using A* search. ([AStar.sort])
 * Purpose: Efficiently sorts large lists by dividing them
 * into manageable chunks for A* sorting.
 *
 * Time & Space Complexity: See [chunkSort]
 *
 * @see chunkSort
 * @See AStar
 */
class ChunkSort {
	private val aStar = AStar()

	/**
	 * Sorts the input list using chunk-based A* search.
	 * Purpose: Divides the list into chunks with [defineChunkValues],
	 * sorts each chunk, and returns the sequence of moves.
	 * - If the list is already sorted, returns an empty move list.
	 * - For small lists (<=5), uses [SmallSort.smallSort].
	 * - For larger lists, defines chunks, sorts each chunk,
	 * and shifts the smallest to the top.
	 *
	 * Time complexity: O(n / s * m), where:
	 *   - n = number of integers in the list.
	 *   - s = Chunk size (In most cases, it's [MAX_CHUNK_SIZE]).
	 *   - m = See [processChunk].
	 *
	 * Space complexity: O(C * m) for stack, moves, and search structures.
	 */
	fun chunkSort(numList: List<Int>): List<Move> {
		if (numList == numList.sorted()) return emptyList()
		val chunks: List<Chunk> = defineChunkValues(numList)
		if (DEBUG) println("Defined ${chunks.size} chunk(s). ${chunks.map { "${it.minValue}-${it.maxValue}:${it.values}" }}")
		var stack = Stack(
			CircularBuffer(numList.size, numList), CircularBuffer(numList.size),
			chunks.first(), null
		)
		if (numList.size <= 5) return SmallSort().smallSort(stack)
		var i = 0
		while (chunks.size > i) {
			stack = processChunk(i, stack, chunks)
			i++
		}
		shiftSmallestToTop(stack, chunks.first().minValue)
		if (DEBUG) println("Shifted smallest to top, final ${getStackInfo(stack)}")
		return stack.moves.toList()
	}

	/**
	 * Sets up chunk boundaries ([Stack.chunk] and [Stack.prevChunkNum]),
	 * runs A* sort, and prepares for the next chunk.
	 *
	 * Time complexity: Sum of [AStar.sort] and [prepareNextChunkHead].
	 * Space complexity: See [AStar.sort].
	 *
	 * @return Updated [Stack] after processing the chunk.
	 * @see AStar.sort
	 * @see prepareNextChunkHead
	 */
	private fun processChunk(i: Int, stack: Stack, chunks: List<Chunk>): Stack {
		System.gc()
		val memBean = java.lang.management.ManagementFactory.getMemoryMXBean()
		val beforeMb = memBean.heapMemoryUsage.used / 1_048_576
		if (i > 0) stack.prevChunkNum = chunks[i - 1].maxValue
		stack.chunk = chunks[i]
		if (DEBUG) println(
			"Sorting chunk ${chunks[i].minValue} - ${chunks[i].maxValue}, ${getStackInfo(stack, false)}"
		)
		val oldStack = stack.clone()
		val newStack = aStar.sort(stack)
		if (DEBUG) println("Chunk ${chunks[i].minValue} - ${chunks[i].maxValue} sorted.")
		if (DEBUG) println("New ${getMoveInfo(newStack, oldStack)}, ${getStackInfo(newStack, false)}")
		// Prepare next chunk: rotate A so that a value of next chunk is at head to enable PB
		if (i + 1 < chunks.size) {
			prepareNextChunkHead(newStack, chunks[i + 1])
		}
		val afterMb = memBean.heapMemoryUsage.used / 1_048_576
		println("Chunk ${i + 1}: ${afterMb - beforeMb}MB live heap delta, ${newStack.moves.size - oldStack.moves.size} moves")
		return newStack
	}

	/**
	 * Divides the sorted input list into chunks.
	 * The size is determined by [MAX_CHUNK_SIZE] or the list size.
	 * Most of the time it would be the former.
	 *
	 * Time complexity:
	 * - n = number of integers in the list.
	 * - O(n log n) for sorting, O(n) for chunk division.
	 *
	 * Space complexity: O(n) for chunk storage.
	 *
	 * @return List of defined [Chunk].
	 */
	private fun defineChunkValues(numList: List<Int>): List<Chunk> {
		val sorted = numList.sorted()
		val chunkSize = min(MAX_CHUNK_SIZE, sorted.size)
		val chunks = mutableListOf<Chunk>()
		for (i in sorted.indices step chunkSize) {
			val chunkValues = sorted.subList(i, min(i + chunkSize, sorted.size))
			chunks.add(Chunk(chunkValues.first(), chunkValues.last(), chunkValues))
		}
		return chunks
	}

	/**
	 * Rotates [Stack.a] minimally so that some element of [nextChunk] ends up at the head (index 0).
	 * Purpose: Prepares A for the next chunk sort by enabling [Move.PB] on a chunk value.
	 * - Finds the first occurrence of a nextChunk value in A.
	 * - Rotates or reverse rotates A, whichever is minimal.
	 *
	 * Time complexity: O(n) for search and rotation.
	 *
	 * Space complexity: O(1) (in-place rotation).
	 */
	private fun prepareNextChunkHead(stack: Stack, nextChunk: Chunk) {
		var targetIdx = -1
		var i = 0
		while (i < stack.a.size) {
			if (stack.a[i] in nextChunk) {
				targetIdx = i
				break
			}
			i++
		}
		if (targetIdx <= 0) return
		if (targetIdx <= stack.a.size - targetIdx) {
			repeat(targetIdx) { stack.apply(Move.RA) }
		} else {
			repeat(stack.a.size - targetIdx) { stack.apply(Move.RRA) }
		}
	}

	/**
	 * Shifts the smallest element in [Stack.a] to the top using minimal moves.
	 * Purpose: Ensures the smallest value is at the top after all chunks are sorted and merged.
	 * - Finds the index of the smallest value.
	 * - Rotates A forward or backward, whichever is minimal.
	 *
	 * Time complexity: O(n) for search and rotation.
	 *
	 * Space complexity: O(k), k = number of moves applied.
	 */
	private fun shiftSmallestToTop(stack: Stack, minValue: Int) {
		val aList = stack.a.value
		val minIdx = stack.a.indexOf(minValue)
		if (minIdx == -1) error("$minValue not found in A: ${stack.a.value}")
		else if (minIdx == 0) return // already at top
		if (minIdx <= aList.size / 2) {
			repeat(minIdx) { stack.apply(Move.RA) }
		} else {
			repeat(aList.size - minIdx) { stack.apply(Move.RRA) }
		}
	}
}