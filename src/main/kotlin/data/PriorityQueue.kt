package me.emaryllis.data

/**
 * PriorityQueue is a binary min-heap for [T] objects, ordered by a caller-provided comparator.
 *
 * Purpose: Efficiently retrieves and manages the lowest-cost [T] for A* search and related algorithms.
 *
 * Time & Space Complexity: See individual methods.
 */
class PriorityQueue<T>(private val compare: (T, T) -> Int) {
	private val heap = mutableListOf<T>()

	/**
	 * Time & Space Complexity: O(n)
	 *
	 * Returns An immutable list of the [heap]'s elements.
	 */
	val value: List<T> get() = heap.toList()

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
	 * Adds a [T] to the [heap] and restores heap order.
	 *
	 * Time Complexity: O(log n) -> n = [heap]'s size.
	 * Space Complexity: O(1)
	 *
	 * @param element The [T] to add.
	 */
	fun push(element: T) {
		heap.add(element)
		siftUp(heap.lastIndex)
	}

	/**
	 * Removes and returns the minimum-cost [T] from the [heap].
	 * After removal, restores the min-heap property by moving the
	 * new root down the tree until the heap order is correct.
	 *
	 * Time Complexity: O(log n) -> n = [heap]'s size.
	 * Space Complexity: O(1)
	 *
	 * @return The minimum-cost [T] from the heap.
	 */
	fun pop(): T {
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
			if (compare(value, heap[parent]) >= 0) break
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
			if (right < heap.size && compare(heap[right], heap[left]) < 0) {
				smallestChild = right
			}
			if (compare(heap[smallestChild], value) >= 0) break
			heap[i] = heap[smallestChild]
			i = smallestChild
		}
		heap[i] = value
	}
}
