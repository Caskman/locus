# Robolectric Rules

*   **Scope:** Utilize Robolectric strictly for Tier 2 Local Integration tests involving Android Framework components such as Services, Workers, and Room DAOs.
*   **Performance:** Prioritize standard JUnit tests for Pure Kotlin logic (Domain Layer, ViewModels) to maximize execution speed; reserve Robolectric for tests requiring an Android Context.
*   **Environment Simulation:** Leverage Shadows and the `ApplicationProvider` to control and simulate the Android system state and lifecycle events.
*   **Database Testing:** Execute Room DAO tests against an in-memory database configuration allowing main thread queries to ensure synchronous reliability.
*   **Integration Gate:** Use Robolectric tests as a cost-effective logic gate to verify system integration locally before executing expensive Tier 5 Device Farm tests.
*   **Configuration:** Ensure test configurations align with the project's Target SDK to prevent version-specific API failures.
