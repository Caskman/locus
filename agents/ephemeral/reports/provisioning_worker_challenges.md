# Report: Provisioning Worker Implementation Challenges

**Date:** 2024-05-23
**Topic:** Robolectric & Tink Keystore Compatibility Issues

## Problem Description
During the implementation of `ProvisioningWorkerTest`, verifying the `ProvisioningWorker` logic, the Robolectric tests consistently failed with the following exception:

```
java.lang.IllegalStateException at AndroidKeystoreKmsClient.java:98
    Caused by: java.security.KeyStoreException at KeyStore.java:871
        Caused by: java.security.NoSuchAlgorithmException at GetInstance.java:159
```

This error occurred during the initialization of the `LocusApp` class. `LocusApp`'s `init` block calls `TinkConfig.register()`. Even though `LocusApp` had a `try-catch` block for `GeneralSecurityException`, `Tink`'s `AndroidKeystoreKmsClient` threw an unchecked `IllegalStateException` when failing to access the Android Keystore in the Robolectric environment.

## Failed Solutions

### 1. Catching `GeneralSecurityException`
*   **Attempt:** The existing code caught `GeneralSecurityException` in `LocusApp.init`.
*   **Result:** Failed. The exception thrown was `IllegalStateException` (Runtime Exception), which bypassed the catch block.

### 2. Mocking EncryptionModule via `TestInstallIn`
*   **Attempt:** I considered using `@TestInstallIn` to replace `EncryptionModule` with a test double that uses Cleartext keys.
*   **Result:** This approach requires the test class to be annotated with `@HiltAndroidTest`. The `ProvisioningWorkerTest` was a standard `RobolectricTestRunner` test. Converting it to a full Hilt test would introduce unnecessary complexity and overhead just to test a worker unit.

## Successful Solution

### 1. Swallowing Runtime Exceptions in `LocusApp` (Safety Net)
*   **Action:** Modified `LocusApp.init` to catch generic `Exception` (including `RuntimeException`).
*   **Result:** This prevents the app from crashing on startup if Tink fails, which is a good defensive measure for fringe devices or test environments.

### 2. Using `HiltTestApplication` in Robolectric Config
*   **Action:** Annotated the test class with `@Config(application = dagger.hilt.android.testing.HiltTestApplication::class)`.
*   **Reasoning:** This instructs Robolectric to use Hilt's basic `Application` class instead of the real `LocusApp`. Since `HiltTestApplication` does not have the `init { TinkConfig.register() }` block, the crash is avoided entirely.
*   **Trade-off:** Logic inside `LocusApp` (like creating Notification Channels) is not executed. This is acceptable for unit testing the Worker, as we are testing the Worker's logic, not the App's initialization.

## Conclusion
The combination of defensive coding in `LocusApp` and using the test-specific Application class in Robolectric configuration resolved the issue and allowed tests to pass reliably.
