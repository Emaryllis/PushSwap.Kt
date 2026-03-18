# A* Overview (High-Level)

## Purpose

Use A* search to integrate one chunk of values at a time into stack A so that all values of chunks 0..k form protected,
contiguous ascending blocks that can later be globally sorted by rotation without re‑touching earlier chunks.

## Glossary (Quick)

- Stack A / B: Working buffer (A) and auxiliary buffer (B) for pushes.
- Chunk: Dense ascending slice of the globally sorted list (size ≤ MAX_CHUNK_SIZE).
- Processed Chunk: Its values are all in A as one contiguous ascending block.
- Protected Block: A processed chunk's block; no internal reordering, no fragmentation, no removal.
- Natural Gap: Original, untouched region of unprocessed values that happens to sit between processed blocks; disappears
  once those values' chunk is processed.
- Normalizing Rotation: Conceptual rotation placing the smallest processed value at index 0 to inspect block order.

## High-Level Flow

1. Partition input into dense chunks (sorted order slices).
2. For each chunk k:

- Run A* to position all values of the current chunk so processed blocks remain/become contiguous ascending.
- When goal for chunk k is met, mark chunk k protected; forbid disruptive moves.

3. Final rotation (optional) yields globally sorted A.

## State Cost (f = g + h)

- g = move count (encoded in history / packed list).
- h = MixedHeuristic (estimates remaining work for current chunk):
  * Descending disorder among current chunk values in B.
  * Min push / pull rotation cost for next chunk value movement.
- Effective formula: `h = descendingInversionsB + nextCost(candidatePushCost, candidatePullCost)`
- Note: `prefixInversions` exists in the code but always returns 0 (see Heuristic Functions below).

## Heuristic Functions (MixedHeuristic)

Each function contributes one component of `h`. They are summed (or selected) in `calculate()`.

### `calculate(stack)`

Top-level entry point. Calls all sub-functions and combines their results into a single integer `h`.
Returns `0` immediately if the goal is already met (B empty and full chunk prefix in A).
Otherwise, returns:
`prefixInversions(always 0) + descendingInversionsB + nextCost(candidatePushCost, candidatePullCost)`.
Effectively: `h = descendingInversionsB + nextCost(...)`.

### `contiguousAscendingPrefixLen(stack, chunkSize)`

Scans from index 0 of A and counts how many leading elements belong to the current chunk **and** are in
strictly ascending order. Stops at the first element that is out-of-chunk or breaks ascending order.
Used as a shared input by `prefixInversions`, `candidatePushCost`, and `candidatePullCost`.

- Returns an integer `prefixLen` in `[0, chunkSize]`.
- Because this guarantees the returned prefix is already strictly ascending, `prefixInversions` on that
  prefix will always return 0.
- O(min(|A|, chunkSize)).

### `prefixInversions(stack, prefixLen)` *(currently inert)*

Counts pairwise inversions among the first `prefixLen` elements of A. An inversion is any pair `(i, j)`
with `i < j` and `A[i] > A[j]`. However, because the prefix produced by `contiguousAscendingPrefixLen`
is guaranteed to be strictly ascending, this function **always returns 0** and contributes nothing to `h`
in the current implementation.

### `descendingInversionsB(stack)`

Counts pairwise inversions in B among chunk elements, but in the **descending** direction: a pair
`(i, j)` with `i < j` and `B[i] < B[j]` is an inversion (B should be descending so that PA restores
ascending order in A cheaply). Penalises poor ordering of chunk values already pushed to B.

- Returns `0` if B has fewer than 2 elements.
- O(|B|²).

### `candidatePushCost(stack, prefixLen, chunkSize)`

Estimates the cheapest single push (PB) of a remaining chunk value from A to B, including the
rotations needed to align both stacks. For each candidate in A (index `i ≥ prefixLen`) and its
insertion point `j` in B, it evaluates four rotation strategies:
`max(i,j)`, `max(|A|-i, |B|-j)`, `i+(|B|-j)`, `(|A|-i)+j`, then takes the minimum and adds 1 for
the push itself. Returns the global minimum across all candidates, or `-1` if no push is needed
(`prefixLen ≥ chunkSize`).

### `candidatePullCost(stack, prefixLen)`

Estimates the cheapest single pull (PA) of a chunk value from B back to A, including the rotation
to bring it to B's head and an extra `+1` penalty if pulling it would create a new inversion with
the current prefix top in A. Returns the minimum cost across all chunk elements in B, or `-1` if B
is empty.

### `nextCost(minPushCost, minPullCost)`

Selects the lower of `candidatePushCost` and `candidatePullCost`. If only one is valid (≥ 0), that
value is returned. If neither is valid, returns `0`. This represents the estimated cost of the single
most productive next move toward completing the chunk.

---

## Goal Condition (Chunk k)

B is empty AND all values of the **current chunk**:

- Are strictly ascending in A (when read in their in-buffer order, via a linear scan from index 0) AND
- Form a single contiguous block starting from the first occurrence of any chunk value found during
  that scan; the scan breaks as soon as a non-chunk element is encountered after the block has begun.
- Note: the goal check is a **linear scan**, not a circular traversal. A chunk whose values wrap
  across the circular buffer boundary (e.g. values at physical indices [6,7,0,1]) will **not** satisfy
  the goal as currently implemented.

## Successor Generation & Pruning

- Disallow inverse backtracking: a move that is the direct inverse of the last applied move is pruned
  in `invalidFast`.
- PB is blocked unless the head of A (`a[0]`) is a value belonging to the current chunk.
- PA is blocked only when B is empty.
- **No structural rotation pruning is implemented**: rotations (RA, RB, RR, RRA, RRB, RRR) are never
  rejected for fragmenting a processed block. Protection from disruptive rotations is heuristic-driven
  rather than structurally enforced.
- States where the recomputed heuristic is `< 0` are discarded in `applyMoveIfValid`. This is the
  primary mechanism by which disruptive states are suppressed.

## Conditional Swap Optimisation (`conditionalOptimize` in `BestStates`)

After each valid move is applied, an opportunistic swap pass is run before the heuristic is finalised:

- **SS** is applied if both SA and SB conditions hold simultaneously.
- **SA** is applied if `canSA` is true: both `a[0]` and `a[1]` are current-chunk values, both are
  above `prevChunkNum` (so no processed value from a prior chunk is touched), and `a[0] > a[1]`
  (swap improves ascending order).
- **SB** is applied if B has at least 2 elements and `b[0] < b[1]` (swap improves descending order
  in B, since PA restores ascending order cheaply from a descending B).
- This is an optimisation layered on top of the move, not a branching search path. The heuristic is
  recalculated after the swap.

## Protection Rules (Design Intent — Not Structurally Enforced)

The following describe the intended correctness properties that the heuristic and pruning *steer toward*.
They are **not** guaranteed by hard checks in the search; they are aspirational invariants:

- Processed blocks should stay contiguous and ascending.
- Only moves that keep the block intact (rotations) should be permitted on processed elements; no
  internal swaps, no partial extractions.
- Natural gaps may exist only while their values remain unprocessed.

## Invariants (Must Hold After Each Chunk Finalization)

1. Each processed chunk = one protected ascending block.
2. Processed chunk blocks (after normalizing rotation) appear in ascending chunk order.
3. No new gap introduced between two processed blocks (any gap present is natural & still unprocessed).
4. No processed value resides in B.
5. Goal test for a chunk implies invariants 1–4.

Note: these invariants describe design intent. The implementation enforces them through heuristic
penalties and limited guards, not through exhaustive structural validation.

---

## Rules:

1. Fully processed chunk

- Definition: All values of that chunk are in buffer A, in strictly ascending order, stored in one contiguous run.
- Once a chunk is marked as fully processed. It is considered protected. See rule 6.
- Rules:
  * Must remain a single contiguous ascending run of exactly its values.
  * Relativity cannot be disrupted (no swapping of its elements).
  * Its elements cannot be pushed to B (no PB applied when head is processed value).
- Valid (chunk C1 = 6..10): A = [17,18,6,7,8,9,10,25,30] (6..10 contiguous ascending).
- Invalid: A = [17,6,7,9,8,10,25] (order broken: 9,8).
- Invalid: A = [17,6,7,25,8,9,10] (not contiguous: 8..10 split by 25).

2. Circular contiguity

- Since a CircularBuffer is used, the last physical index of A is logically adjacent to index 0.
  However, the **goal check does not exploit this**: it performs a linear scan and will not recognise
  a chunk block that wraps across the buffer boundary. Circular contiguity applies structurally to
  the buffer, but is not used in the current goal or heuristic logic.
- Valid (structurally): A = [12,13,14,1,2,3,4,5,6] with chunk C0 = {1,2,3,4,5,6}. Linear scan finds
  1..6 consecutively starting at index 3.
- Not recognised by goal: A = [4,5,6,12,13,1,2,3] with C0 = {1,2,3,4,5,6}. Values wrap; goal fails.

3. "Behind previous chunk" (relative order)

- If you were to rotate so the globally smallest processed value is at index 0, the processed chunks appear in
  strictly ascending chunk order. Each processed chunk is individually contiguous & ascending; between two processed
  chunks, there may exist only unprocessed values (future chunks) that happened to lie there originally.
- Valid (C0=1..4, C1=5..8): A = [7,8,20,1,2,3,4, X, 5,6] where X is an unprocessed value; rotation to 1
  yields [1,2,3,4,X,5,6,7,8,20] — chunk order preserved, X sits between processed C0 and C1.
- Invalid: A = [7,5,6,8,1,2,3,4] → rotate to 1 gives [1,2,3,4,7,5,6,8] (C1 block split / out of order).
- Note: this ordering is a design intent invariant, not a hard-checked constraint during search.

4. Allowed values between processed chunks

- Unprocessed (future) chunk values may appear before, after, or BETWEEN processed chunk blocks ONLY if they were
  never inserted there after processing (i.e. they remained from the original arrangement at those relative
  positions). The algorithm must NOT actively inject new unprocessed values between two already processed chunks.
- Valid (natural gap): Original A led to processed layout [1,2,3,4, 15,16,17,18, 5,6,7,8] where 15..18 are future
  values not yet processed; C0 and C1 each intact & contiguous.
- Invalid (injected gap): After C0 and C1 processed, PA inserts a future value 50 between
  them → [1,2,3,4,50,5,6,7,8]. This is forbidden.
- Invalid (fragmentation): A = [1,2, 9,10, 3,4] (future 9,10 split C0 block).
- Note: PA injection between processed chunks is not blocked by a hard guard; it is suppressed by
  heuristic penalties causing such states to score poorly or return heuristic < 0.

5. Chunk boundaries by min/max (dense slices)

- Chunks are always allocated as contiguous ascending slices of the globally sorted list with fixed maximum size (
  e.g., up to 7). Therefore, each chunk's value set is dense: S = [min, min+1, ..., max] with no intentional gaps.
- There is no scenario where an interior integer in that closed range is "missing" unless it does not exist in the
  original input (which cannot happen for a permutation). Sparse examples do NOT apply.
- Valid (chunk slice 11..17): Block = [11,12,13,14,15,16,17] contiguous ascending.
- Invalid: [11,12,13,14,16,17] (missing 15).
- Invalid: [10,11,12,13,14,15,16,17] (includes 10 from a previous chunk).

6. Processed chunks are protected

- Definition (design intent):
  * No PA may insert an unprocessed value inside a processed chunk OR between two processed chunks (only fully
    before all processed blocks or fully after all processed blocks).
  * Future chunk operations may rotate the entire structure (RA/RRA etc.) as long as each processed block stays
    contiguous and relative chunk order (after a normalization rotation to the smallest processed) remains
    ascending.
- In the current implementation, protection is enforced by:
  * `canSA` in `conditionalOptimize`: prevents SA/SS from swapping processed-chunk values.
  * Heuristic < 0 discard: states that would badly disorder processed blocks tend to score negatively and
    are dropped in `applyMoveIfValid`.
  * There is no rotation guard: RA/RRA/RR/RRR are never blocked for fragmenting a processed block.
- Valid examples:
  * Rotation causing wrap: [7,8,1,2,3,4,5,6].
  * Natural gap (context: ONLY chunks up to C1 have been processed): [1,2,3,4, 15,16,17,18, 5,6,7,8]
    * Here 1..4 (C0) and 5..8 (C1) are processed; 15..18 (C2) are still unprocessed future values left in their
      original positions.
    * This layout is TEMPORARILY valid because 15..18 have yet to be processed.
    * Once chunk C2 is marked as processed, this arrangement becomes **INVALID**. A valid final layout after
      processing that chunk would be [1,2,3,4,5,6,7,8,15,16,17,18] (or any relative position).
- Invalid examples:
  * Inserting future value (PA) between processed chunks: [1,2,3,4,50,5,6,7,8] (50 inserted between C0 and C1).
  * Swapping processed values (SA/SB/SS): [1,2,4,3,5,6,7,8] (3 and 4 swapped inside C0).