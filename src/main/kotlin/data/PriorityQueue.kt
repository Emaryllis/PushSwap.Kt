package me.emaryllis.data

/**
 * PriorityQueue is a binary min-heap for [Stack] objects, ordered by their [Stack.currentCost].
 *
 * Purpose: Efficiently retrieves and manages the lowest-cost Stack for A* search and related algorithms.
 *
 * Time & Space Complexity: See individual methods.
 */
class PriorityQueue {
	private val heap = mutableListOf<Stack>()

	/**
	 * Time & Space Complexity: O(n)
	 *
	 * Returns An immutable list of the [heap]'s elements.
	 */
	val value: List<Stack> get() = heap.toList()

	/**
	 * Time & Space Complexity: O(1)
	 *
	 * Returns the number of elements in the [heap].
	 */
	val size: Int get() = heap.size

	/**
	 * Time & Space Complexity: O(1)
	 *
	 * @return true if the [heap] is not empty.
	 */
	fun isNotEmpty(): Boolean = heap.isNotEmpty()

	/**
	 * Adds a [Stack] to the [heap] and restores heap order.
	 *
	 * Time Complexity: O(log n) -> n = [heap]'s size.
	 * Space Complexity: O(1)
	 *
	 * @param element The Stack to add.
	 */
	fun push(element: Stack) {
		heap.add(element)
		siftUp(heap.lastIndex)
	}

	/**
	 * Removes and returns the minimum-cost [Stack] from the [heap].
	 * After removal, restores the min-heap property by moving the
	 * new root down the tree until the heap order is correct.
	 *
	 * Time Complexity: O(log n) -> n = [heap]'s size.
	 * Space Complexity: O(1)
	 *
	 * @return The minimum-cost [Stack] from the heap.
	 */
	fun pop(): Stack {
		if (heap.isEmpty()) error("Stack is empty")
		val top = heap.first()
		val last = heap.removeAt(heap.lastIndex)
		if (heap.isNotEmpty()) {
			heap[0] = last
			siftDown(0)
		}
		return top
	}

	/**
	 * Restores heap order by sifting the element at [index] up.
	 * Used in the [push] operation.
	 *
	 * Time Complexity: O(log n) -> n = [heap]'s size.
	 * Space Complexity: O(1)
	 */
	private fun siftUp(index: Int) {
		var i = index
		val value = heap[i]
		while (i > 0) {
			val parent = (i - 1) / 2
			if (value.currentCost >= heap[parent].currentCost) break
			heap[i] = heap[parent]
			i = parent
		}
		heap[i] = value
	}

	/**
	 * Restores heap order by sifting the element at [index] down.
	 * Used in the [pop] operation.
	 *
	 * Time Complexity: O(log n) -> n = [heap]'s size.
	 * Space Complexity: O(1)
	 */
	@Suppress("SameParameterValue")
	private fun siftDown(index: Int) {
		var i = index
		val value = heap[i]
		while (2 * i + 1 < heap.size) {
			val left = 2 * i + 1
			val right = 2 * i + 2
			var smallestChild = left
			if (right < heap.size && heap[right].currentCost < heap[left].currentCost) {
				smallestChild = right
			}
			if (heap[smallestChild].currentCost >= value.currentCost) break
			heap[i] = heap[smallestChild]
			i = smallestChild
		}
		heap[i] = value
	}
}
