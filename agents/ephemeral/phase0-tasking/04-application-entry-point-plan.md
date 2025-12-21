# Implementation Plan - Task 4: Application Entry Point

## 1. Objective
Wire up the Android Application layer with Hilt, create the main entry point, and display the "Hello World" UI with the version retrieved from the Domain layer. This task validates the integration between the `:app`, `:core:domain`, and `:core:data` layers using the "Tracer Bullet" feature (App Version).

## 2. Prerequisites
- **Task 1 (Scaffolding):** Completed. Module structure (`:app`, `:core:*`) and Gradle files are in place.
- **Task 3 (Domain/Data):** Completed.
    - `AppVersionRepository` interface exists in `:core:domain`.
    - `AppVersionRepositoryImpl` implementation exists in `:core:data`.
    - `GetAppVersionUseCase` exists in `:core:domain`.
- **Environment:** Android Studio / Gradle environment is healthy (`./gradlew projects` passes).

## 3. Architecture Decisions
- **Versioning:** Use `git describe --tags` in `app/build.gradle.kts` to dynamically generate `versionName`, and commit count for `versionCode`.
- **Dependency Injection:**
    - Use Hilt for DI.
    - Create `LocusApp` annotated with `@HiltAndroidApp`.
    - Create `AppModule` in `:app` to bind `AppVersionRepositoryImpl` to `AppVersionRepository` (per Task 4 instructions, although DataModule in `:core:data` is a valid alternative for future refactoring).
- **Presentation Pattern:** MVVM (Model-View-ViewModel).
    - `DashboardViewModel` injects `GetAppVersionUseCase`.
    - Exposes `StateFlow<LocusResult<String>>` (or a specific UI State sealed class).
- **UI Toolkit:** Jetpack Compose (Material 3).

## 4. Implementation Steps

### Step 1: Build Configuration (Versioning)
**Goal:** Ensure the app reports the correct version from Git.
- **File:** `app/build.gradle.kts`
- **Action:**
    - Define logic to run `git describe --tags` (fallback to "0.0.0-dev").
    - Define logic to count commits for `versionCode`.
    - Apply these to `defaultConfig`.

### Step 2: Hilt Application Setup
**Goal:** Initialize the Hilt dependency graph.
- **File:** `app/src/main/kotlin/com/locus/android/LocusApp.kt`
    - Create class extending `Application`.
    - Annotate with `@HiltAndroidApp`.
- **File:** `app/src/main/AndroidManifest.xml`
    - Update `<application>` tag to set `android:name=".LocusApp"`.
    - Ensure basic permissions (INTERNET, ACCESS_NETWORK_STATE) are present if needed (though not strictly required for local version check).

### Step 3: Dependency Injection Wiring
**Goal:** Provide the Data Layer implementation to the Domain Layer consumers.
- **File:** `app/src/main/kotlin/com/locus/android/di/AppModule.kt`
    - Annotate with `@Module` and `@InstallIn(SingletonComponent::class)`.
    - Define `@Provides` or `@Binds` method for `AppVersionRepository`.
    - *Note:* Since `AppVersionRepositoryImpl` is in `:core:data`, ensure `:app` depends on `:core:data` in `build.gradle.kts` (should be done in Task 1, verify).

### Step 4: Presentation Logic (ViewModel)
**Goal:** Orchestrate the data flow for the Dashboard.
- **File:** `app/src/main/kotlin/com/locus/android/features/dashboard/DashboardViewModel.kt`
    - Annotate `@HiltViewModel`.
    - Inject `GetAppVersionUseCase`.
    - Initialize by calling the use case and updating a `StateFlow` (e.g., `uiState`).
- **File:** `app/src/test/kotlin/com/locus/android/features/dashboard/DashboardViewModelTest.kt`
    - Add a unit test using `Mockk` and `Turbine` to verify the ViewModel updates state correctly upon success/failure.

### Step 5: User Interface (Compose)
**Goal:** Display the "Hello World" message.
- **File:** `app/src/main/kotlin/com/locus/android/features/dashboard/DashboardScreen.kt`
    - Create a Composable `DashboardScreen`.
    - Observe the ViewModel state.
    - Render a `Scaffold` with a `Text` component showing "Hello Locus vX.X.X".
- **File:** `app/src/main/kotlin/com/locus/android/MainActivity.kt`
    - Annotate `@AndroidEntryPoint`.
    - Set content to `LocusTheme { DashboardScreen() }`.

## 5. Validation
**Manual Verification:**
1. Run `./scripts/build_artifacts.sh` to verify the build succeeds and versioning logic works.
2. Launch the app on an emulator/device.
3. Verify the screen displays "Hello Locus v[Git-Tag]".

**Automated Verification:**
1. Run `./gradlew :app:testDebugUnitTest` to pass ViewModel tests.
2. Run `./gradlew :app:connectedDebugAndroidTest` (if instrumented tests are added, optional for this step).

## 6. Definition of Done
- [ ] App builds successfully with Hilt.
- [ ] App launches without crashing.
- [ ] "Hello Locus" and correct version are visible.
- [ ] ViewModel unit tests pass.
