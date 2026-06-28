# Practice Exercises

DAY 15-21: Practice Exercises

Exercise 1: MVVM Profile Screen
Build a ProfileViewModel with StateFlow state and SharedFlow effects.
Requirements: loading, success, error, retry, and one-time snackbar on failure.

Exercise 2: MVI Transfer Screen
Implement intents, results, reducer, and ViewModel for a money transfer flow.
Add validation: amount &gt; 0, recipient not blank, insufficient balance.

Exercise 3: Clean Architecture Feature Design
For a "favorite article" feature, define:
- Domain model
- Repository interface
- Use case
- Data DTO/entity mapping
- UI model mapping

Exercise 4: Manual DI
Create an AppContainer that wires Api -&gt; Repository -&gt; UseCase -&gt; ViewModel.
Then replace Repository with a fake in a test.

Exercise 5: Interview Practice
Explain the difference between MVVM and MVI using a real screen example.
Explain when you would skip creating a use case.
Explain how DI improves tests.
