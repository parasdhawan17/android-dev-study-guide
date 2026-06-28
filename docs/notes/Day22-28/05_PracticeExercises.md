# Practice Exercises

DAY 22-28: Practice Exercises

Exercise 1: Validator Tests
Write parameterized tests for email, password, and amount validation.

Exercise 2: ViewModel Test
Build a ViewModel that loads articles and exposes StateFlow&lt;ArticleState&gt;.
Test loading, success, error, and retry behavior with runTest.

Exercise 3: Use Case Test
Implement TransferMoneyUseCase with rules:
- source != destination
- amount &gt; 0
- currency must match account currency
Write tests for all branches.

Exercise 4: Repository Test
Create a fake API and fake DAO.
Test cache hit, cache miss, force refresh, API error fallback.

Exercise 5: Testability Refactor
Take code that calls System.currentTimeMillis(), Dispatchers.IO, and a singleton directly.
Refactor using Clock, DispatcherProvider, and constructor injection.
