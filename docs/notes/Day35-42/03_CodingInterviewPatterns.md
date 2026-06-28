# Coding Interview Patterns

DAY 35-42: Coding Interview Patterns in Kotlin

## 1. Two Pointers

Use when array/string is sorted or you compare from both ends.
Time: O(n), Space: O(1)

````kotlin
fun isPalindromeIgnoringNonLetters(input: String): Boolean {
    var left = 0
    var right = input.lastIndex
    while (left < right) {
        while (left < right && !input[left].isLetterOrDigit()) left++
        while (left < right && !input[right].isLetterOrDigit()) right--
        if (input[left].lowercaseChar() != input[right].lowercaseChar()) return false
        left++
        right--
    }
    return true
}
````

## 2. Sliding Window

Use for contiguous subarray/substring problems.
Maintain a window [left, right] and update counts/sum as it moves.

````kotlin
fun longestSubstringWithoutRepeating(s: String): Int {
    val lastSeen = mutableMapOf<Char, Int>()
    var left = 0
    var best = 0
    for (right in s.indices) {
        val c = s[right]
        lastSeen[c]?.let { previous ->
            if (previous >= left) left = previous + 1
        }
        lastSeen[c] = right
        best = maxOf(best, right - left + 1)
    }
    return best
}
````

## 3. BFS / DFS

BFS: shortest path in unweighted graph, level-order traversal.
DFS: explore all paths/components, recursion/backtracking.

````kotlin
fun shortestPath(graph: Map<Int, List<Int>>, start: Int, target: Int): Int {
    val queue = ArrayDeque<Pair<Int, Int>>()
    val visited = mutableSetOf<Int>()
    queue.add(start to 0)
    visited.add(start)

    while (queue.isNotEmpty()) {
        val (node, distance) = queue.removeFirst()
        if (node == target) return distance
        for (next in graph[node].orEmpty()) {
            if (visited.add(next)) queue.add(next to distance + 1)
        }
    }
    return -1
}
````

## 4. Hash Map Counts

Use maps/sets to trade space for speed.

````kotlin
fun twoSum(nums: IntArray, target: Int): IntArray {
    val indexByValue = mutableMapOf<Int, Int>()
    for (i in nums.indices) {
        val needed = target - nums[i]
        val match = indexByValue[needed]
        if (match != null) return intArrayOf(match, i)
        indexByValue[nums[i]] = i
    }
    return intArrayOf()
}
````

## 5. Dynamic Programming

Use when problem has:
- Overlapping subproblems.
- Optimal substructure.

Steps:
1. Define state.
2. Define transition.
3. Base cases.
4. Fill order.

````kotlin
fun climbStairs(n: Int): Int {
    if (n <= 2) return n
    var prev2 = 1
    var prev1 = 2
    for (step in 3..n) {
        val current = prev1 + prev2
        prev2 = prev1
        prev1 = current
    }
    return prev1
}
````

## Interview Habits

- Clarify input and output.
- State brute force first if useful.
- Choose pattern and explain why.
- Discuss time/space complexity.
- Test with small examples and edge cases.
- Keep Kotlin code readable: data classes, when, safe calls, meaningful names.
