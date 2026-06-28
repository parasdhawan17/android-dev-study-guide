# Day 35-42: Debugging, Edge Cases, Coding Practice - Quick Reference

## Debugging Loop

1. Reproduce reliably.
2. Define expected vs actual.
3. Narrow scope with evidence.
4. Form one hypothesis.
5. Test hypothesis.
6. Fix root cause.
7. Add regression coverage.

## Edge Case Checklist

- Null, blank, empty.
- One item, many items, duplicates.
- Boundary numbers: 0, 1, min, max.
- Offline, timeout, slow network.
- Permission denied.
- Rotation and process death.
- Rapid taps and navigation away mid-request.
- Locale/time zone changes.

## Error Handling

Use explicit UI states:
- Loading
- Success
- Empty
- Error
- Offline/cached

Retry only transient failures. Use exponential backoff.

## Algorithm Patterns

| Pattern | Use For | Complexity Goal |
|---|---|---|
| Two pointers | Sorted arrays, palindromes | O(n), O(1) |
| Sliding window | Contiguous substring/subarray | O(n) |
| Hash map/set | Fast lookup/counting | O(n), O(n) space |
| BFS | Shortest path unweighted graph | O(V + E) |
| DFS | Components, backtracking | O(V + E) |
| DP | Overlapping subproblems | State * transition |

## Interview Communication

- Ask clarifying questions.
- State assumptions.
- Explain trade-offs.
- Mention edge cases before being asked.
- Analyze time and space complexity.
- Test with small examples.

## Final Questions To Ask

- What are the Android team’s current technical challenges?
- How does the team handle code reviews?
- What architecture patterns are used today?
- How do you balance product delivery and technical debt?
- What does success look like in the first six months?
