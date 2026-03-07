package me.emaryllis.data

import me.emaryllis.Settings.HASH_PRIME

/**
 * CircularBuffer is a fixed-size, circular queue for integers.
 * Purpose: Efficiently supports stack/queue operations for PushSwap, including rotation, swap, and push.
 *
 * Time complexity: All operations are O(1) except value/indexOf/containsAll, which are O(n).
 * Space complexity: O(capacity) for buffer storage.
 */
class CircularBuffer(val capacity: Int, numList: List<Int> = emptyList()) : Collection<Int>, Cloneable {
	/**
	 * Backing array for buffer storage. Size is fixed at [capacity].
	 */
	var buffer: IntArray = IntArray(capacity)
	private var head = 0

	/**
	 * Logical number of elements in the buffer.
	 */
	override var size = 0
		private set

	/**
	 * Initializes the buffer with values from the list of integers.
	 * @throws IllegalArgumentException if the number of integers exceeds the capacity.
	 */
	init {
		buffer = IntArray(capacity)
		if (numList.isNotEmpty()) {
			require(numList.size <= capacity) { "List size exceeds buffer capacity" }
			numList.forEach { value ->
				buffer[(head + size) % capacity] = value
				size++
			}
		}
	}

	/**
	 * Swaps the first two elements in the buffer.
	 *
	 * Time & Space Complexity: O(1)
	 *
	 * @return true if swap was performed, false if size < 2.
	 */
	fun swap(): Boolean {
		if (size < 2) return false
		val temp = get(0)
		this[0] = get(1)
		this[1] = temp
		return true
	}

	/**
	 * Rotates the buffer to the left (decreases indices for most elements).
	 *
	 * Time & Space Complexity: O(1)
	 *
	 * @return true if rotation was performed, false if size < 2.
	 */
	fun rotate(): Boolean {
		if (size < 2) return false
		return enqueue(dequeue())
	}

	/**
	 * Rotates both buffers to the left.
	 *
	 * Time & Space Complexity: O(1)
	 *
	 * @param other The other buffer to rotate.
	 * @return true if both rotations succeed.
	 */
	fun rotateBoth(other: CircularBuffer): Boolean = this.rotate() && other.rotate()

	/**
	 * Rotates the buffer to the right (increases indices for most elements).
	 *
	 * Time & Space Complexity: O(1)
	 *
	 * @return true if rotation was performed, false if size < 2.
	 */
	fun reverseRotate(): Boolean {
		if (size < 2) return false
		val lastIndex = (head + size - 1 + capacity) % capacity
		val last = buffer[lastIndex]
		buffer[lastIndex] = 0
		head = (head - 1 + capacity) % capacity
		buffer[head] = last
		return true
	}

	/**
	 * Rotates both buffers to the right.
	 *
	 * Time & Space Complexity: O(1)
	 *
	 * @param other The other buffer to rotate.
	 * @return true if both rotations succeed.
	 */
	fun reverseRotateBoth(other: CircularBuffer): Boolean = this.reverseRotate() && other.reverseRotate()

	/**
	 * Pushes the front element of this buffer to the front of [dest].
	 *
	 * Time & Space Complexity: O(1)
	 *
	 * @param dest Destination buffer to push to.
	 * @return true if push was performed, false if source is empty or dest is full.
	 */
	fun push(dest: CircularBuffer): Boolean {
		if (isEmpty() || dest.size == dest.capacity) return false
		// put into front of dest
		dest.head = (dest.head - 1 + dest.capacity) % dest.capacity
		dest.buffer[dest.head] = dequeue()
		dest.size++
		return true
	}

	/**
	 * Time & Space Complexity: O(n), n = [capacity].
	 *
	 * @return Deep copy of this [buffer].
	 */
	public override fun clone(): CircularBuffer {
		val copy = CircularBuffer(capacity)
		copy.buffer = buffer.copyOf()
		copy.head = head
		copy.size = size
		return copy
	}

	/**
	 * Time & Space Complexity: O(n)
	 *
	 * @returns List view of the buffer's logical contents.
	 */
	val value: List<Int>
		get() = List(size) { get(it) }

	/**
	 * Time & Space Complexity: O(1)
	 *
	 * @throws NoSuchElementException if [isEmpty] (Just as a precaution)
	 * @returns The first element in the buffer.
	 */
	fun first() = if (isEmpty()) throw NoSuchElementException("CircularBuffer is empty.") else get(0)

	/**
	 * Time & Space Complexity: O(1)
	 *
	 * @throws NoSuchElementException if [isEmpty] (Just as a precaution)
	 * @returns The last element in the buffer.
	 */
	fun last() = if (isEmpty()) throw NoSuchElementException("CircularBuffer is empty.") else get(size - 1)

	/**
	 * Time Complexity: O(n)
	 * Space Complexity: O(1)
	 *
	 * @param element Element to search for.
	 * @return The logical index of the first occurrence of [element], or -1 if not found.
	 */
	fun indexOf(element: Int?): Int {
		for (i in indices) {
			if (get(i) == element) return i
		}
		return -1
	}

	/**
	 * Checks logical equality of buffer contents and size.
	 *
	 * Time Complexity: O(n)
	 * Space Complexity: O(1)
	 *
	 * @param other Object to compare.
	 * @return True if [other] is a [CircularBuffer] with the same size and contents, false otherwise.
	 */
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is CircularBuffer) return false
		if (this.size != other.size) return false
		for (i in indices) {
			if (this.buffer[(head + i) % capacity] != other.buffer[(other.head + i) % other.capacity]) return false
		}
		return true
	}

	/**
	 * Hashes buffer contents and size.
	 * Time Complexity: O(n)
	 * Space Complexity: O(1)
	 *
	 * @return Hash code based on size and contents.
	 */
	override fun hashCode(): Int {
		var result = size
		for (i in indices) {
			result = HASH_PRIME * result + get(i)
		}
		return result
	}

	/**
	 * Time & Space Complexity: O(1)
	 *
	 * @return true if buffer is empty, false otherwise.
	 */
	@Suppress("ReplaceSizeZeroCheckWithIsEmpty")
	override fun isEmpty(): Boolean = size == 0 // NOSONAR: Replacing with 'isEmpty' causes inf recursion loop

	/**
	 * Time Complexity: O(n)
	 * Space Complexity: O(1)
	 *
	 * @return [Iterator] over the buffer's logical contents.
	 */
	override fun iterator(): Iterator<Int> = value.iterator() // NOSONAR: Can't use 'by' since list size is dynamic

	/**
	 * Time Complexity: O(n)
	 * Space Complexity: O(1)
	 *
	 * @param element Element to check.
	 * @return true if [element] is in the buffer.
	 */
	override fun contains(element: Int): Boolean = value.contains(element)

	/**
	 * Time Complexity: O(n * m), m = [elements]'s size
	 * Space Complexity: O(1)
	 *
	 * @param elements Collection of elements to check.
	 * @return true if all [elements] are in the buffer.
	 */
	override fun containsAll(elements: Collection<Int>): Boolean = elements.all { contains(it) }

	/**
	 * Time & Space Complexity: O(1)
	 * @param i Logical index to get.
	 * @throws IndexOutOfBoundsException if [i] is out of bounds.
	 * @return Element at logical index [i].
	 */
	operator fun get(i: Int): Int {
		require(i in indices) { "Index $i out of bounds (size=$size)" }
		return buffer[(head + i) % capacity]
	}

	/**
	 * Time & Space Complexity: O(1)
	 * @param i Logical index to set.
	 * @param value Value to set at index [i].
	 * @throws IndexOutOfBoundsException if [i] is out of bounds.
	 */
	private operator fun set(i: Int, value: Int) {
		require(i in indices) { "Index $i out of bounds (size=$size)" }
		val idx = (head + i) % capacity
		buffer[idx] = value
	}

	/**
	 * Inserts [value] at the logical tail of the buffer.
	 *
	 * Time & Space Complexity: O(1)
	 * @param value Value to enqueue.
	 * @return true if enqueue succeeded, false if buffer is full.
	 */
	private fun enqueue(value: Int): Boolean {
		if (size == capacity) return false
		buffer[(head + size) % capacity] = value
		size++
		return true
	}

	/**
	 * Time & Space Complexity: O(1)
	 * @throws IllegalStateException if buffer is empty.
	 * @return The removed head element.
	 */
	private fun dequeue(): Int {
		require(isNotEmpty()) { "Buffer is empty" }
		val value = buffer[head]
		buffer[head] = 0 // clear garbage
		head = (head + 1) % capacity
		size--
		return value
	}
}
