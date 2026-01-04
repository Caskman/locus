# Task 10 Implementation Plan: Permissions & Setup Trap

**Goal:** Implement the "Permission Trap" and finalizes the Onboarding flow, ensuring the user cannot enter the main application without granting necessary Location permissions. It adheres to Android 11+ (API 30+) best practices for two-stage location requests.

## Prerequisites
- **Human Actions:** None.
- **Dependencies:** `AuthRepository` (Implemented), `MainActivity` routing (Implemented).

## Alignment Mapping
| Requirement ID | Requirement Summary | Implementation Component |
| :--- | :--- | :--- |
| **R1.1550** | Request permissions in two distinct stages (Foreground then Background). | `PermissionViewModel` (Sequence logic) + `PermissionScreen` (Two UI steps). |
| **R1.1555** | If Foreground denied, inform user and do not request Background. | `PermissionViewModel` (Stops flow on denial) + `PermissionScreen` (Shows "Permission Required" state). |
| **R1.1560** | **Permission Trap:** Force user back to permission screen on next launch. | `MainActivity` (Routes based on `PERMISSIONS_PENDING` persisted state) + `PermissionScreen` (Blocking UI). |
| **R1.1900** | **Setup Trap:** Restore last known provisioning state. | `AuthRepository` (Persists `OnboardingStage`) + `MainActivity`. |
| **R1.1600** | Manual confirmation ("Go to Dashboard"). | `PermissionViewModel` handles the final transition to `COMPLETE` after permissions are granted. |

## Implementation Steps

### Phase 1: Manifest & ViewModel
**Goal:** Declare permissions and encapsulate logic.

1.  **Update `AndroidManifest.xml`**
    -   Add `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, and `ACCESS_BACKGROUND_LOCATION`.
    -   *Verification:* Read Manifest file.

2.  **Create `PermissionViewModel`** (`features/onboarding/viewmodel`)
    -   Inject `AuthRepository`.
    -   State: `PermissionUiState` (ForegroundPending, BackgroundPending, DeniedForever, Granted).
    -   Logic: Two-stage check, `completeOnboarding()` (updates repo), `onResume` auto-check.
    -   *Verification:* Read ViewModel file.

### Phase 2: UI Implementation (Compose)
**Goal:** Build the two-stage permission screen with educational context.

3.  **Create `PermissionScreen.kt`** (`features/onboarding/ui`)
    -   Composables: `ForegroundPermissionContent`, `BackgroundPermissionContent` (Education), `PermanentDenialContent`.
    -   Lifecycle: Check permissions on Resume.
    -   *Verification:* Read UI file.

### Phase 3: Integration & The Trap
**Goal:** Connect the UI to the App Navigation and enforce the trap.

4.  **Update `OnboardingNavigation.kt`**
    -   Add `PERMISSIONS` composable.
    -   Connect `onPermissionsGranted` -> `viewModel.completeOnboarding()`.
    -   *Verification:* Read Navigation file.

### Phase 4: Verification & Testing
**Goal:** Ensure robustness across API levels and user behaviors.

5.  **Create `PermissionViewModelTest.kt`**
    -   Test flows: Initial state, Foreground Grant -> Background Pending, All Granted -> Complete.
    -   *Verification:* Read Test file.

6.  **Execute Tests**
    -   Run `./gradlew testDebugUnitTest`.

## Completion Criteria
- Unit tests pass.
- Manual verification confirms the "Trap" works (cannot bypass permissions).
- Android 11+ flow works (Education -> Settings).
