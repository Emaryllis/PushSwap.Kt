package me.emaryllis.data

import me.emaryllis.data.PackedMoveList.Companion.BITS_PER_MOVE
import me.emaryllis.data.PackedMoveList.Companion.MOVES_PER_LONG
import me.emaryllis.data.PackedMoveList.Companion.MOVE_MASK
import kotlin.math.ceil
import kotlin.math.log2

/**
 * Stores a list of [Move] as a bit-packed array (4 bits per move, up to 16 moves per Long).
 *
 * Time & Space Complexity: See individual methods.
 *
 * @property BITS_PER_MOVE Number of bits used to store each move (4 bits for up to 16 possible moves).
 * @property MOVES_PER_LONG Number of moves that can be stored in a single Long (16 moves).
 * @property MOVE_MASK Bitmask to extract a single move from a Long (0xF).
 * @property data The underlying LongArray storing the packed moves.
 * @property size The number of moves currently stored in the list.
 */
class PackedMoveList private constructor(
	private val data: LongArray,
	override val size: Int
) : Cloneable, Collection<Move> {
	companion object {
		private var BITS_PER_MOVE = ceil(log2(Move.entries.size.toDouble())).toInt() // 4 bits for 16 moves
		private var MOVES_PER_LONG = Long.SIZE_BITS / BITS_PER_MOVE // 16 moves per Long
		private var MOVE_MASK = (1L shl BITS_PER_MOVE) - 1 // 0xF for 4 bits

		/**
		 * Time & Space Complexity: O(1)
		 * @return An empty [PackedMoveList].
		 */
		fun empty(): PackedMoveList = PackedMoveList(LongArray(1), 0)
	}

	/**
	 * The move's ordinal/index is shifted left, which positions it in the
	 * correct bit slot within the target [Long], then the result is
	 * combined with the existing [Long] value using bitwise OR, so the
	 * new move is stored without affecting other moves. This allows
	 * up to [MOVES_PER_LONG] to be stored in a single [Long], each
	 * occupying bits denoted by [BITS_PER_MOVE]. If the needs to
	 * grow, it is copied to a larger [LongArray].
	 *
	 * Time Complexity: O(1) (amortized, worse case O(n) when resizing).
	 * Space Complexity: O(n / 16) -> n = number of moves.
	 *
	 * @param move The move to add.
	 * @return A new [PackedMoveList] with the move added.
	 */
	fun add(move: Move): PackedMoveList {
		val newSize = size + 1
		val arrLen = (newSize + MOVES_PER_LONG - 1) / MOVES_PER_LONG
		val newArr = if (arrLen > data.size) data.copyOf(arrLen) else data.copyOf()
		val arrIdx = size / MOVES_PER_LONG
		val bitIdx = (size % MOVES_PER_LONG) * BITS_PER_MOVE
		newArr[arrIdx] = newArr[arrIdx] or (move.ordinal.toLong() shl bitIdx)
		return PackedMoveList(newArr, newSize)
	}

	/**
	 * For each move, the corresponding [Long] is shifted right,
	 * so the desired bits are moved to the lowest position.
	 * [MOVE_MASK] is applied to extract only [BITS_PER_MOVE],
	 * which represent the move's ordinal value. The ordinal
	 * is then mapped back to the corresponding [Move].
	 * This process is repeated for all moves in the list.
	 *
	 * Time Complexity: O(n) -> n = [size].
	 * Space Complexity: O(n)
	 *
	 * @return An ordered List containing all moves.
	 */
	fun toList(): List<Move> {
		val result = ArrayList<Move>(size)
		for (i in indices) {
			val arrIdx = i / MOVES_PER_LONG
			val bitIdx = (i % MOVES_PER_LONG) * BITS_PER_MOVE
			val ord = ((data[arrIdx] shr bitIdx) and MOVE_MASK).toInt()
			result.add(Move.entries[ord])
		}
		return result
	}

	/**
	 * Time Complexity: O(n) -> n = [size].
	 * Space Complexity: O(n / 16)
	 *
	 * @return A deep copy of [data].
	 */
	public override fun clone(): PackedMoveList = PackedMoveList(data.copyOf(), size)

	/**
	 * For each move, the corresponding [Long] is shifted right, so the
	 * desired bits are moved to the lowest position. [MOVE_MASK]
	 * is applied to extract only [BITS_PER_MOVE], which represent
	 * the move's ordinal value. The ordinal is then mapped back
	 * to the corresponding [Move] and compared to [element].
	 *
	 * Time Complexity: O(n)
	 * Space Complexity: O(1)
	 *
	 * @param element The move to search for.
	 * @return true if [element] is in the packed move list.
	 */
	override fun contains(element: Move): Boolean {
		for (i in indices) {
			val arrIdx = i / MOVES_PER_LONG
			val bitIdx = (i % MOVES_PER_LONG) * BITS_PER_MOVE
			val ord = ((data[arrIdx] shr bitIdx) and MOVE_MASK).toInt()
			if (Move.entries[ord] == element) return true
		}
		return false
	}

	/**
	 * Time Complexity: O(n * m) -> m = [elements]'s size.
	 * Space Complexity: O(1)
	 *
	 * @param elements The collection of moves to check for.
	 * @return true if all [elements] are in [data].
	 */
	override fun containsAll(elements: Collection<Move>): Boolean {
		for (e in elements) {
			if (!contains(e)) return false
		}
		return true
	}

	/**
	 * Time & Space Complexity: O(1)
	 *
	 * @return true if the packed move list is empty.
	 */
	@Suppress("ReplaceSizeZeroCheckWithIsEmpty")
	override fun isEmpty(): Boolean = size == 0 // NOSONAR

	/**
	 * For each move, the corresponding Long is shifted right,
	 * so the desired bits are moved to the lowest position.
	 * [MOVE_MASK] is applied to extract only those bits, which
	 * represent the move's ordinal value. The ordinal is then
	 * mapped back to the corresponding [Move] and returned.
	 *
	 * Time Complexity: O(n)
	 * Space Complexity: O(1)
	 *
	 * @return An [Iterator] over the packed moves.
	 */
	override fun iterator(): Iterator<Move> = object : Iterator<Move> {
		private var idx = 0
		override fun hasNext(): Boolean = idx < size
		override fun next(): Move {
			if (!hasNext()) throw NoSuchElementException()
			val arrIdx = idx / MOVES_PER_LONG
			val bitIdx = (idx % MOVES_PER_LONG) * BITS_PER_MOVE
			val ord = ((data[arrIdx] shr bitIdx) and MOVE_MASK).toInt()
			idx++
			return Move.entries[ord]
		}
	}
}
