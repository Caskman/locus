# Implementation Plan - Task 11: Admin Upgrade Flow

## Prerequisites
- [x] Confirmed `locus-admin.yaml` requirements (Hybrid Policy).
- [x] Confirmed "Soft Restart" strategy (Intent flags).
- [x] Confirmed architecture (Dedicated `SettingsScreen`).

## 1. Data Layer & Assets
*Establish the infrastructure and data foundations for the Admin identity.*

1.  **Create Admin CloudFormation Template**
    - **File:** `core/data/src/main/assets/locus-admin.yaml`
    - **Action:** Create file with the Hybrid IAM Policy defined in Phase 1 (Self-Tracking + Read-Only Community Access).
    - **Validation:** Verify file exists and is valid YAML.

2.  **Update Domain Models**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/model/auth/RuntimeCredentials.kt`
    - **Action:** Add `val isAdmin: Boolean = false`.
    - **File:** `core/data/src/main/kotlin/com/locus/core/data/model/RuntimeCredentialsDto.kt`
    - **Action:** Add `isAdmin` field to DTO and update mappers in `AuthRepositoryImpl`.

3.  **Update Resource Provider**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/infrastructure/ResourceProvider.kt` (Interface)
    - **File:** `core/data/src/main/kotlin/com/locus/core/data/infrastructure/ResourceProviderImpl.kt` (Impl)
    - **Action:** Add `getAdminStackTemplate(): String` method.

4.  **Enhance AuthRepository**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/repository/AuthRepository.kt`
    - **File:** `core/data/src/main/kotlin/com/locus/core/data/repository/AuthRepositoryImpl.kt`
    - **Action:**
        - Add `replaceRuntimeCredentials(creds: RuntimeCredentials)` method.
        - Ensure `SecureStorageDataSource` correctly serializes/deserializes the new `isAdmin` flag.

## 2. Domain Logic & Background Processing
*Enable the provisioning logic to handle the Admin upgrade.*

5.  **Update Provisioning Use Case**
    - **File:** `core/domain/src/main/kotlin/com/locus/core/domain/usecase/ProvisioningUseCase.kt`
    - **Action:**
        - Update `invoke` to accept `isAdmin: Boolean = false`.
        - Logic: If `isAdmin` is true, use `resourceProvider.getAdminStackTemplate()` and set `isAdmin = true` in the resulting `RuntimeCredentials`.

6.  **Update Provisioning Worker**
    - **File:** `app/src/main/kotlin/com/locus/android/features/onboarding/work/ProvisioningWorker.kt`
    - **Action:**
        - Add constant `MODE_ADMIN_UPGRADE`.
        - In `doWork`, handle this mode by calling `provisioningUseCase` with `isAdmin = true`.

## 3. UI Implementation
*Create the Settings and Upgrade screens.*

7.  **Create Settings Screen**
    - **File:** `app/src/main/kotlin/com/locus/android/features/settings/SettingsScreen.kt` (New)
    - **Action:** Create a scaffold screen with an "Admin Upgrade" button (visible only if `!isAdmin`).
    - **Navigation:** Add `OnboardingDestinations.SETTINGS` (or similar) to navigation graph in `OnboardingNavigation` or `MainActivity`.

8.  **Create Admin Upgrade Screen**
    - **File:** `app/src/main/kotlin/com/locus/android/features/settings/AdminUpgradeScreen.kt` (New)
    - **Action:** Implement UI for entering Bootstrap Keys (reusing components from `CredentialEntryScreen` if possible, or duplicating simplified logic).
    - **Logic:** "Start Upgrade" button triggers `AdminUpgradeViewModel`.

9.  **Create Admin Upgrade ViewModel**
    - **File:** `app/src/main/kotlin/com/locus/android/features/settings/AdminUpgradeViewModel.kt` (New)
    - **Action:** Handle "Dry Run" validation -> Dispatch `ProvisioningWorker` with `MODE_ADMIN_UPGRADE` -> Observe State.

10. **Integrate Dashboard Entry Point**
    - **File:** `app/src/main/kotlin/com/locus/android/features/dashboard/DashboardScreen.kt`
    - **Action:** Add a "Settings" icon/action to the Top App Bar that navigates to `SettingsScreen`.

## 4. Restart Logic
*Handle the critical post-upgrade lifecycle.*

11. **Implement Soft Restart**
    - **File:** `app/src/main/kotlin/com/locus/android/features/settings/AdminUpgradeScreen.kt`
    - **Action:** On `ProvisioningState.Success`, execute:
      ```kotlin
      val intent = Intent(context, MainActivity::class.java)
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
      context.startActivity(intent)
      ```

## 5. Verification
*Ensure the feature works as expected.*

12. **Unit Tests**
    - **Action:** Test `ProvisioningUseCase` selects the correct template.
    - **Action:** Test `RuntimeCredentials` serialization.

13. **Manual Verification**
    - **Action:** Deploy app, navigate to Settings -> Admin Upgrade.
    - **Action:** Enter keys, verify `locus-user-admin` stack creation in AWS console.
    - **Action:** Verify app restarts and user remains logged in.

## 6. Pre-commit & Submit
- [ ] Run `scripts/run_local_validation.sh`.
- [ ] Submit changes.
