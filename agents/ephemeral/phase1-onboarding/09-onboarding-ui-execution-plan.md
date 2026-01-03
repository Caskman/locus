# Implementation Plan - Task 9: Onboarding UI - Execution

**Status:** Planned
**Feature:** Onboarding UI (Provisioning, Success, Permissions)
**Task:** 9 (Execute Onboarding UI)
**Specs:**
- `docs/behavioral_specs/01_onboarding_identity.md`
- `docs/technical_discovery/specs/ui/onboarding.md`
- `docs/technical_discovery/user_flows/onboarding.md`

## Prerequisites: Human Action Steps

No automated refactoring required.

## Implementation Steps

### Step 1: Domain Layer Refactoring (Granular State & Tests)
**Goal:** Refactor `ProvisioningState` to support the required "Log-Style" UI and ensure tests remain green.

1.  **Modify `ProvisioningState` (Sealed Class):**
    -   Replace static states (e.g., `Deploying`) with a dynamic `Working` state:
        ```kotlin
        data class Working(val currentStep: String, val stepHistory: List<String>) : ProvisioningState()
        ```
    -   Ensure `Success` and `Failure` states remain.
2.  **Update Use Cases:**
    -   Update `ProvisioningUseCase` and `RecoverAccountUseCase` to emit granular `Working` updates as the Worker progresses (or simply map Worker progress if possible).
3.  **Refactor Tests (CRITICAL):**
    -   **Immediately** update `ProvisioningUseCaseTest`, `RecoverAccountUseCaseTest`, and `ProvisioningStateTest`.
    -   Fix compilation errors caused by the removal of static state objects.
    -   Assert against `Working.currentStep` strings instead of specific types.

### Step 2: Architecture & Persistence (The Setup Trap)
**Goal:** Implement the "Setup Trap" to ensure users cannot bypass critical steps (like Permissions) via process death.

1.  **Define `OnboardingStage`:**
    -   Create an enum or state in Domain: `Provisioning`, `ProvisioningSuccess`, `PermissionsPending`, `Completed`.
2.  **Update `AuthRepository` (Persistence):**
    -   Use `EncryptedSharedPreferences` (or `DataStore`) to persist the current `OnboardingStage`.
    -   Expose this stage as a `StateFlow`.
    -   Ensure `ProvisioningState.Success` automatically transitions the persisted stage to `PermissionsPending`.
3.  **Update `MainViewModel` & `MainActivity`:**
    -   Read the *persisted* stage on app launch.
    -   **Routing Logic:**
        -   If `PermissionsPending` -> Route directly to `PermissionScreen`.
        -   If `Provisioning` -> Route to `ProvisioningScreen`.
        -   If `Completed` -> Route to `Dashboard`.

### Step 3: UI Implementation (Provisioning & Permissions)
**Goal:** Build the visual feedback screens and the mandatory Permission flow.

1.  **Create `ProvisioningScreen` (Log-Style):**
    -   **State:** Observe `ProvisioningState.Working`.
    -   **UI:** Use a `LazyColumn` to display `stepHistory` (gray/checked) and `currentStep` (active/spinner).
    -   **Feedback:** Show "Provisioning your secure cloud..." header.
2.  **Create `SuccessScreen`:**
    -   **UI:** Large Checkmark + "Cloud Ready".
    -   **Action:** "Continue" button -> Navigates to **Permissions** (NOT Dashboard).
3.  **Create `PermissionScreen` (Two-Step):**
    -   **Step 1 (Foreground):** Rationale UI -> Request `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION`.
    -   **Step 2 (Background):** Rationale UI (Android 11+ requirement) -> Request `ACCESS_BACKGROUND_LOCATION` (or send to Settings).
    -   **Completion:** Only after Background is granted (or denied permanently) -> Call `viewModel.completeOnboarding()`.

### Step 4: Integration & Navigation Wiring
**Goal:** Connect the full flow: Provisioning -> Success -> Permissions -> Dashboard.

1.  **Update `OnboardingDestinations`:**
    -   Add `PROVISIONING`, `SUCCESS`, and `PERMISSIONS`.
2.  **Update `OnboardingNavigation`:**
    -   Link the screens.
    -   Ensure `SuccessScreen` explicitly navigates to `PermissionScreen`.
    -   Ensure `PermissionScreen` calls the final completion action.
3.  **Connect ViewModels:**
    -   Ensure `NewDeviceViewModel` triggers the Worker and navigates to `PROVISIONING`.
    -   Ensure `PermissionViewModel` handles the Android Permission API logic and updates the Repository state to `Completed`.

### Step 5: Verification

1.  **Unit Tests:**
    -   Verify `AuthRepository` correctly persists `OnboardingStage` updates.
    -   Verify `MainViewModel` routes to `PermissionsPending` on initialization if that was the last state.
2.  **Manual Verification (Process Death):**
    -   Run Provisioning -> Reach "Success" or "Permissions".
    -   **Kill the App.**
    -   Relaunch.
    -   **Verify:** App restores the "Permissions" screen (The Trap), NOT the Start Screen or Dashboard.
3.  **UI Verification:**
    -   Verify the Provisioning Log List scrolls and updates live.
    -   Verify the Permission flow handles "Deny" and "Allow" correctly.
