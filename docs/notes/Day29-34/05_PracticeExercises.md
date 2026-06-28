# Practice Exercises

DAY 29-34: Practice Exercises

Exercise 1: RecyclerView DiffUtil
Implement ListAdapter for ChatMessage with payloads for read status and reaction count.

Exercise 2: Layout Optimization
Take a nested XML layout and rewrite it using ConstraintLayout.
Explain which measure/layout work was removed.

Exercise 3: Leak Hunt
Create three leak examples: listener leak, Fragment binding leak, Handler leak.
Then fix each and explain the ownership problem.

Exercise 4: Startup Audit
List everything initialized in Application.onCreate.
Classify each as critical-for-first-frame or deferrable.

Exercise 5: Jank Debugging Drill
Simulate heavy work inside onBindViewHolder.
Move it off the main thread or precompute it, then explain the performance impact.
