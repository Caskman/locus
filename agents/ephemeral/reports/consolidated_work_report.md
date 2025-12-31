# Consolidated Refined Work Report
**Date:** 2025-12-31
**Status:** Ready for Implementation

This report consolidates the valid findings from previous code reviews and analysis. The following issues have been verified and require remediation.

### 1. Code Duplication in Provisioning & Recovery Logic
*   **Severity:** High
*   **Description:** `ProvisioningUseCase` and `RecoverAccountUseCase` share substantial identical logic for CloudFormation stack creation, polling loops, status checking, and output parsing. This duplication violates DRY and increases maintenance risk.
*   **Proposed Resolution:**
    1.  Extract the shared logic into a new Domain Service (e.g., `StackProvisioningService`).
    2.  This service should encapsulate stack creation, polling, and output parsing.
    3.  Refactor both Use Cases to delegate to this service, acting as orchestrators.

### 2. Write-Only Telemetry Salt Fallback
*   **Severity:** High
*   **Description:** The `ConfigurationRepositoryImpl` writes the Telemetry Salt to standard `SharedPreferences` as a fallback, but `getTelemetrySalt()` only attempts to read from `SecureStorage`. If secure storage fails, the fallback is never used.
*   **Proposed Resolution:**
    1.  Update `ConfigurationRepositoryImpl.getTelemetrySalt()` to implement the fallback read logic.
    2.  Attempt to read from `SecureStorage` first; if null, read from standard `SharedPreferences`.

### 3. Polling Loop Robustness (Fail Fast)
*   **Severity:** High
*   **Description:** The current polling loop retries all errors (including permanent ones like `AccessDenied` or `Throttling`) until the 10-minute timeout expires, leading to poor user experience.
*   **Proposed Resolution:**
    1.  Implement "Fail Fast" logic within the polling mechanism.
    2.  Retry only on Transient errors (Network, Timeout).
    3.  Fail immediately on Permanent errors (Auth, Client Error, Provisioning Failure).

### 4. Incorrect Theme Implementation
*   **Severity:** Medium
*   **Description:** `MainActivity.kt` defines a local `LocusTheme` function that shadows the comprehensive global `LocusTheme` (in `com.locus.android.ui.theme`), preventing the application of Dynamic Colors and Dark Mode.
*   **Proposed Resolution:**
    1.  Remove the local `fun LocusTheme` definition from `MainActivity.kt`.
    2.  Import and use the correct `LocusTheme` from the `ui.theme` package.

### 5. Side Effects in Repository Constructor
*   **Severity:** Medium
*   **Description:** `AuthRepositoryImpl` launches a coroutine in its `init` block to call `loadInitialState()`. This "fire-and-forget" side effect makes unit testing difficult and non-deterministic.
*   **Proposed Resolution:**
    1.  Remove the `init` block.
    2.  Trigger loading via `SharingStarted.Lazily` in the `stateIn` operator, or use an explicit `initialize()` method called by the ViewModel/Application.

### 6. Fragile CloudFormation Output Parsing & Hardcoded Keys
*   **Severity:** Medium
*   **Description:** Critical CloudFormation Output keys (`RuntimeAccessKeyId`, etc.) are hardcoded as string literals across multiple files. Stack ID parsing relies on brittle string splitting.
*   **Proposed Resolution:**
    1.  Consolidate all infrastructure keys into a single `InfrastructureConstants` object in the Domain layer.
    2.  Implement robust parsing logic for Stack IDs/ARNs.

### 7. Hardcoded Strings in Domain/Data Layer (I18n)
*   **Severity:** Medium
*   **Description:** The repository uses hardcoded English strings for validation errors (e.g., `BucketValidationStatus.Invalid("...")`), preventing localization.
*   **Proposed Resolution:**
    1.  Replace string payloads with sealed classes or enums (e.g., `BucketValidationError.MissingTag`).
    2.  Map these types to localized string resources in the UI layer.

### 8. Misleading Error Mapping for Local Resources
*   **Severity:** Medium
*   **Description:** A failure to load the local `locus-stack.yaml` asset is currently caught and mapped to a `NetworkError`, which is semantically incorrect.
*   **Proposed Resolution:**
    1.  Map this failure to a specific `ProvisioningError.InvalidConfiguration` or internal error type.

### 9. Unsafe/Brittle Casting in AuthRepository
*   **Severity:** Low
*   **Description:** The code uses explicit casting (`return result as LocusResult.Failure`), which is unsafe and may crash if the `LocusResult` hierarchy changes.
*   **Proposed Resolution:**
    1.  Refactor to use a `when` expression or safe type checking.
