package me.emaryllis.data

data class SearchDebugMetrics(
	var iteration: Int = 0,
	var maxOpenListSize: Int = 0,
	var stalePops: Int = 0,
	var consideredSuccessors: Int = 0,
	var prunedVisitedSuccessors: Int = 0,
	var prunedDominatedSuccessors: Int = 0,
	var inversePrunes: Int = 0,
	var sizePrunes: Int = 0,
	var chunkHeadRotationPrunes: Int = 0,
	var nonCanonicalRotationPrunes: Int = 0,
	var pbHeadGuardPrunes: Int = 0,
	var paEmptyGuardPrunes: Int = 0
) {
	fun reset() {
		iteration = 0
		maxOpenListSize = 0
		stalePops = 0
		consideredSuccessors = 0
		prunedVisitedSuccessors = 0
		prunedDominatedSuccessors = 0
		inversePrunes = 0
		sizePrunes = 0
		chunkHeadRotationPrunes = 0
		nonCanonicalRotationPrunes = 0
		pbHeadGuardPrunes = 0
		paEmptyGuardPrunes = 0
	}

	fun printSearchMetrics() {
		val staleRate = if (iteration == 0) 0.0 else stalePops.toDouble() / iteration
		val prunedSuccessors = prunedVisitedSuccessors + prunedDominatedSuccessors
		val pruneRate = if (consideredSuccessors == 0) 0.0 else prunedSuccessors.toDouble() / consideredSuccessors
		println(
			"Search metrics:\n${
				horizontalTable(
					"iters" to "$iteration",
					"peakOpen" to "$maxOpenListSize",
					"stalePops" to "$stalePops (${String.format("%.2f", staleRate * 100)}%)",
					"prunedSucc" to "$prunedSuccessors/$consideredSuccessors (${String.format("%.2f", pruneRate * 100)}%)",
					"visPrunes" to "$prunedVisitedSuccessors",
					"domPrunes" to "$prunedDominatedSuccessors"
				)
			}"
		)
	}

	fun printInvalidationMetrics() {
		println(
			"Invalidation metrics:\n${
				horizontalTable(
					"invPrunes" to "$inversePrunes",
					"sizePrunes" to "$sizePrunes",
					"chunkHeadRot" to "$chunkHeadRotationPrunes",
					"nonCanonRot" to "$nonCanonicalRotationPrunes",
					"pbGuard" to "$pbHeadGuardPrunes",
					"paGuard" to "$paEmptyGuardPrunes"
				)
			}"
		)
	}

	private fun horizontalTable(vararg columns: Pair<String, String>): String {
		if (columns.isEmpty()) return ""
		val widths = columns.map { (header, value) -> maxOf(header.length, value.length) }
		val headerRow = columns.mapIndexed { index, (header, _) -> header.padEnd(widths[index]) }.joinToString(" | ")
		val valueRow = columns.mapIndexed { index, (_, value) -> value.padEnd(widths[index]) }.joinToString(" | ")
		return "| $headerRow |\n| $valueRow |"
	}
}