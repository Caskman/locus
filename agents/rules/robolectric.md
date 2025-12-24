# Robolectric Rules

*   **Scope:** Utilize Robolectric strictly for Tier 2 Local Integration tests involving Android Framework components such as Services, Workers, and Room DAOs.
*   **Location:** Place all Robolectric tests within the unit test source set alongside standard unit tests.
*   **Performance:** Prioritize standard JUnit tests for Pure Kotlin logic (Domain Layer, ViewModels) to maximize execution speed; reserve Robolectric for tests requiring an Android Context.
*   **Configuration:** Explicitly configure tests to use SDK 34 to prevent version-specific API failures.
*   **Gradle Setup:** Enable Android resource inclusion in the application module's unit test options and include the AndroidX JUnit extension in data modules.
*   **Database Testing:** Execute Room DAO tests against an in-memory database configuration allowing main thread queries to ensure synchronous reliability.
*   **Mandatory Integration:** Implement the Traffic Guardrail Integration Test to verify that network calls are correctly blocked when quotas are exceeded.
*   **Environment Simulation:** Leverage Shadows and the Application Provider to control and simulate the Android system state and lifecycle events.
