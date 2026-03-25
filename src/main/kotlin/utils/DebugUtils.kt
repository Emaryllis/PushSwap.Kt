package me.emaryllis.utils

import me.emaryllis.data.Stack

object DebugUtils {
	fun getMoveInfo(newStack: Stack, oldStack: Stack): String =
		"Moves(${newStack.moves.size - oldStack.moves.size}): ${
			newStack.moves.toList().subList(
				oldStack.moves.size,
				newStack.moves.size
			)
		}"

	fun getCostInfo(stack: Stack): String =
		"g: ${stack.moves.size}, h: ${stack.heuristic}, f: ${stack.currentCost}"

	fun getStackInfo(stack: Stack, moves: Boolean = true): String =
		 "A: ${stack.a.value}, B: ${stack.b.value}, Chunk: ${stack.chunk.minValue}-${stack.chunk.maxValue}, " +
			"${getCostInfo(stack)}, ${if (moves) "Moves(${stack.moves.size}): ${stack.moves.toList()}" else ""}"
}