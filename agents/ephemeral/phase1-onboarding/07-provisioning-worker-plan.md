# Implementation Plan - Task 7: Provisioning Worker

**Feature:** Onboarding & Identity
**Goal:** Implement the background worker responsible for orchestrating AWS CloudFormation provisioning and account recovery, ensuring process resilience.

## Prerequisites: Human Action Steps

*   **Update Notification Spec:** Update `docs/technical_discovery/specs/ui/notifications.md` to broaden the scope of `channel_tracking` to include "Long-Running Setup/Maintenance Operations" to legitimize its usage for provisioning.

## Implementation Steps

### Step 1: Configure Application & Dependencies
**Goal:** Prepare the `LocusApp` and build environment for WorkManager.

*   **Modify Build:** `app/build.gradle.kts`
    *   Add `testImplementation(libs.androidx.work.testing)` to ensure worker tests can run.
*   **Modify Application:** `app/src/main/kotlin/com/locus/android/LocusApp.kt`
    *   Implement `Configuration.Provider` interface.
    *   Inject `HiltWorkerFactory` via `@Inject lateinit var workerFactory: HiltWorkerFactory`.
    *   Implement `getWorkManagerConfiguration()` to return `Configuration.Builder().setWorkerFactory(workerFactory).build()`.
    *   **Disable Default Initializer:** Ensure `tools:node="remove"` is used for `androidx.work.impl.WorkManagerInitializer` in `AndroidManifest.xml` (or verify if Hilt handles this, standard practice is manual config for Hilt).
*   **Create Notification Channel:** `app/src/main/kotlin/com/locus/android/LocusApp.kt`
    *   In `onCreate()`, create the `channel_tracking` notification channel (Low Importance) using `NotificationManager`. This ensures the Worker can post its foreground notification immediately.

### Step 2: Update AuthRepository Contract
**Goal:** Enable secure retrieval of Bootstrap Credentials within the background worker.

*   **Modify Specification:** `docs/technical_discovery/specs/domain_layer_spec.md`
    *   Update `AuthRepository` interface definition to include `suspend fun getBootstrapCredentials(): LocusResult<BootstrapCredentials>`.
*   **Modify Interface:** `core/domain/src/main/kotlin/com/locus/core/domain/repository/AuthRepository.kt`
    *   Add `suspend fun getBootstrapCredentials(): LocusResult<BootstrapCredentials>`
*   **Update Implementation:** `core/data/src/main/kotlin/com/locus/core/data/repository/AuthRepositoryImpl.kt`
    *   Implement using `secureStorage.getBootstrapCredentials()`.
    *   **Security Check:** Verify state is `SetupPending` before returning credentials.
*   **Create Test Double:** `core/testing/src/main/kotlin/com/locus/core/testing/repository/FakeAuthRepository.kt`
    *   **Create File:** If it does not exist.
    *   Implement the full `AuthRepository` interface (stubs for methods not used in this task, functional mock for `getBootstrapCredentials`).

### Step 3: Implement ProvisioningWorker
**Goal:** Create the WorkManager worker.

*   **Create File:** `app/src/main/kotlin/com/locus/android/features/onboarding/work/ProvisioningWorker.kt`
*   **Dependencies:** `AuthRepository`, `ProvisioningUseCase`, `RecoverAccountUseCase`
*   **Configuration:**
    *   `CoroutineWorker`
    *   Use `@HiltWorker` and `@AssistedInject`.
    *   Input Data Constants: `KEY_MODE`, `KEY_DEVICE_NAME`, `KEY_BUCKET_NAME`
    *   Mode Enum: `PROVISION`, `RECOVER`
*   **Logic (`doWork`):**
    1.  **Notification:** Call `setForeground` immediately.
        *   Channel: `channel_tracking`
        *   Title: `Locus • Setup`
        *   Body: `Provisioning resources...`
        *   Icon: `@drawable/ic_stat_sync` (fallback: `android.R.drawable.stat_sys_upload` if unavailable).
    2.  **Credentials:** Call `authRepository.getBootstrapCredentials()`. Fail if missing or error.
    3.  **Dispatch:**
        *   Execute `runCatching` block.
        *   If `PROVISION`: Call `provisioningUseCase(creds, input.deviceName)`
        *   If `RECOVER`: Call `recoverAccountUseCase(creds, input.bucketName)`
    4.  **Result Handling:**
        *   **Success:** Return `Result.success()`.
        *   **Transient Error (Network):** Return `Result.retry()` (WorkManager handles exponential backoff by default or can be configured).
        *   **Terminal Failure:**
            *   Call `authRepository.updateProvisioningState(ProvisioningState.Error(msg))` to ensure UI updates.
            *   Return `Result.failure()`.

## Alignment Mapping

*   **R1.600 (Provisioning Background Task):** Implemented by `ProvisioningWorker` running as a Foreground Service. Ensure this ID is commented in code.
*   **R1.1350 (Recovery Background Task):** Implemented by `ProvisioningWorker` handling the `RECOVER` mode.
*   **R1.700 (Use Bootstrap Keys):** `AuthRepository.getBootstrapCredentials()` allows the worker to access keys.
*   **UI/Notifications Spec:** Adheres to the "Locus • [State]" title format and reuses `channel_tracking` (with updated spec scope).

## Testing Strategy

### Unit Tests
*   **File:** `app/src/test/kotlin/com/locus/android/features/onboarding/work/ProvisioningWorkerTest.kt`
*   **Tools:** `Robolectric`, `WorkManagerTestInitHelper` (requires `work-testing` dependency), `Mockk`.
*   **Cases:**
    *   **Provisioning Success:** Verify `ProvisioningUseCase` is called and `Result.success()` returned.
    *   **Recovery Success:** Verify `RecoverAccountUseCase` is called and `Result.success()` returned.
    *   **Transient Error:** Mock `IOException` from UseCase, verify `Result.retry()` is returned.
    *   **Terminal Error:** Mock `AuthException` from UseCase, verify `repo.updateProvisioningState(Error)` is called AND `Result.failure()` is returned.
    *   **Missing Credentials:** Verify failure if Repo returns error.
    *   **Notification:** Verify `setForeground` is called.

## Completion Criteria

*   `app/build.gradle.kts` includes `work-testing`.
*   `LocusApp` implements `Configuration.Provider`.
*   `FakeAuthRepository` is created and usable.
*   `ProvisioningWorker` exists, compiles, and passes tests (> 70% coverage).
*   `./scripts/run_local_validation.sh` passes.
