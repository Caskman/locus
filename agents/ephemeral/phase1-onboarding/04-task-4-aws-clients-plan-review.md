# Plan Review: Task 4 - AWS Infrastructure Clients

**Plan under review:** `agents/ephemeral/phase1-onboarding/04-task-4-aws-clients-plan.md`
**Reviewer:** Jules (AI Agent)
**Date:** 2024-05-23

## Executive Summary
The implementation plan was reviewed against the `network_infrastructure_spec.md` and the broader architecture documentation. Four critical discrepancies were identified regarding Dependency Injection scoping, Security Boundaries, Interface Granularity, and Naming Conventions. Addressing these is essential to maintain the "Bootstrap vs. Runtime" security model.

## Detailed Findings

### 1. Dependency Injection Scope & Module Separation

*   **Issue:**
    The plan (Step 6) proposes a single `NetworkModule` providing `InfrastructureProvisioner` (Bootstrap), `RemoteStorageInterface` (Runtime), and `LocusCredentialsProvider` (Runtime).
*   **Analysis:**
    The `network_infrastructure_spec.md` (Section 1.2 & 6) mandates a strict separation into `BootstrapModule` and `RuntimeModule`.
    *   **Bootstrap Credentials** are high-privilege and ephemeral. Providing them in a Singleton `NetworkModule` risks extending their lifecycle beyond the Onboarding screen or exposing them to unrelated app components.
    *   **Runtime Components** should be singletons available to the whole app, but should not have access to provisioning logic.
*   **Proposed Resolution:**
    Split Step 6 into two distinct DI modules:
    *   **`BootstrapModule`:** `InstallIn(ViewModelComponent::class)`. Provides `InfrastructureProvisioner` (CloudFormation).
    *   **`RuntimeModule`:** `InstallIn(SingletonComponent::class)`. Provides `S3Client`, `RemoteStorageInterface`, and `LocusCredentialsProvider`.

### 2. Location of `listBuckets` & Credential Usage

*   **Issue:**
    The plan places `listBuckets` in `RemoteStorageInterface` (Runtime Client) in Steps 2 & 5.
*   **Analysis:**
    The System Recovery flow requires listing buckets to *find* the user's data *before* the runtime configuration exists.
    *   `RemoteStorageInterface` is designed to use `LocusCredentialsProvider` (Runtime Keys).
    *   Recovery relies on the **Bootstrap Credentials** (User-provided keys) which have `s3:ListAllMyBuckets` permission.
    *   Using the Runtime client for this operation would require initializing it with Bootstrap keys, violating the "Single Responsibility" principle and confusing the security boundaries.
*   **Proposed Resolution:**
    Move `listBuckets` (or a specific `findLocusBucket` method) to the `InfrastructureProvisioner` interface (or a dedicated `BootstrapDiscovery` interface) so it correctly utilizes the Bootstrap Credentials provided during the onboarding/recovery flow.

### 3. Interface Granularity (`describeStack` vs `createStack`)

*   **Issue:**
    The plan (Step 2 & 4) exposes `describeStack` and `createStack` as separate methods on the `InfrastructureProvisioner` interface.
*   **Analysis:**
    This exposes implementation details (polling mechanisms) to the Domain Layer.
    *   The Domain Layer (`OnboardingViewModel`) typically expects a "Fire and Return Result" contract (e.g., `deployStack`).
    *   Forcing the ViewModel to manage the polling loop (`create` -> loop `describe`) leaks complexity and makes the Domain Layer brittle to changes in the provisioning logic.
*   **Proposed Resolution:**
    Encapsulate the polling logic within the `CloudFormationClient` implementation.
    *   The interface should expose a single suspend function: `createStack(name: String): LocusResult<StackOutputs>`.
    *   Internally, this function should handle the `CreateStack` call and the subsequent polling loop until `CREATE_COMPLETE` or failure.

### 4. Module Naming Conventions

*   **Issue:**
    The plan refers to `NetworkModule`.
*   **Analysis:**
    The spec explicitly names the modules `BootstrapModule` and `RuntimeModule` to reinforce their lifecycle and security scope. Generic naming obscures these architectural decisions.
*   **Proposed Resolution:**
    Rename the modules in the plan to explicitly match `network_infrastructure_spec.md`:
    *   `NetworkModule` -> `RuntimeModule` (for general S3/Telemetry).
    *   Create `BootstrapModule` (for CloudFormation).
    *   (`NetworkModule` can still exist for shared, non-scoped dependencies like `OkHttpClient` or `Json` configuration, as per Spec Section 6.1).
