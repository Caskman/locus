# Implementation Plan - Task 4: AWS Infrastructure Clients

## Prerequisites: Human Action Steps

Execute these refactoring operations using your IDE or terminal before implementation begins:

### Step 1: Move CloudFormation Template

*   **File:** `docs/technical_discovery/locus-stack.yaml`
*   **Action:** Move to Assets Directory
*   **New Path:** `core/data/src/main/assets/locus-stack.yaml`
*   **Note:** Create the `assets` directory if it does not exist.

## Implementation Steps

### Step 1: Add Dependencies
**Goal:** Enable AWS SDK for Kotlin in the Data module.

*   **File:** `core/data/build.gradle.kts`
*   **Action:** Add dependencies:
    *   `implementation(libs.aws.sdk.s3)`
    *   `implementation(libs.aws.sdk.cloudformation)`
    *   `implementation(libs.aws.sdk.sts)`
*   **Verification:** `./gradlew :core:data:dependencies`

### Step 2: Define Data Source Interfaces
**Goal:** Decouple Repositories from concrete AWS Clients.

*   **File:** `core/data/src/main/kotlin/com/locus/core/data/source/remote/aws/InfrastructureProvisioner.kt`
    *   Interface: `createStack(name: String): LocusResult<StackOutputs>` (Encapsulates polling).
    *   Interface: `findLocusBucket(): LocusResult<List<String>>` (Uses Bootstrap Credentials for Discovery).
    *   **Note:** `getBucketTags` is internal to validation logic, not part of the public VM interface.
*   **File:** `core/data/src/main/kotlin/com/locus/core/data/source/remote/aws/RemoteStorageInterface.kt`
    *   Interface: `uploadTrack`. (Deferred to Phase 3/Task, but interface can exist).
    *   **Note:** `listBuckets` removed from here as it belongs to Bootstrap/Recovery flow.

### Step 3: Implement Dynamic Credentials Provider
**Goal:** Allow S3Client to use keys stored in `SecureStorageDataSource`.

*   **File:** `core/data/src/main/kotlin/com/locus/core/data/source/remote/aws/LocusCredentialsProvider.kt`
*   **Logic:**
    *   Implements `aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider`.
    *   Injects `SecureStorageDataSource`.
    *   `resolve()` reads `RuntimeCredentials` from storage.
    *   Maps to `Credentials(accessKeyId, secretAccessKey, sessionToken)`.

### Step 4: Implement Bootstrap Client (CloudFormation)
**Goal:** Implement `InfrastructureProvisioner` using AWS SDK.

*   **File:** `core/data/src/main/kotlin/com/locus/core/data/source/remote/aws/CloudFormationClient.kt`
*   **Logic:**
    *   Accepts `BootstrapCredentials` in constructor (or method).
    *   Uses `StaticCredentialsProvider`.
    *   Region: Hardcoded `us-east-1`.
    *   Reads `locus-stack.yaml` from `assets`.
    *   **Implements `createStack`:**
        *   Calls `CreateStack`.
        *   **Internally manages polling loop** (calling `DescribeStacks` every 5s) until `CREATE_COMPLETE` or Failure.
        *   Returns `LocusResult`.
    *   **Implements `findLocusBucket`:**
        *   Uses `s3:ListAllMyBuckets` (via S3Client or directly if possible, but likely needs a temporary S3 client with Bootstrap creds) or `tag:GetResources` if permissible. *Refinement:* Since this client holds Bootstrap Creds, it can instantiate a temporary S3Client to list buckets or tags.

### Step 5: Implement Runtime Client (S3)
**Goal:** Implement `RemoteStorageInterface` using AWS SDK.

*   **File:** `core/data/src/main/kotlin/com/locus/core/data/source/remote/aws/S3Client.kt`
*   **Logic:**
    *   Injects `LocusCredentialsProvider` (Runtime Keys).
    *   Region: Configurable (passed in methods or config).
    *   Implements `uploadTrack` (stub or logic).
    *   **Note:** Does NOT implement `listBuckets` for recovery.

### Step 6: Dependency Injection
**Goal:** Provide clients via Hilt with correct scoping.

*   **File:** `core/data/src/main/kotlin/com/locus/core/data/di/BootstrapModule.kt`
    *   **Scope:** `InstallIn(ViewModelComponent::class)`
    *   **Logic:** `@Provides` `InfrastructureProvisioner` (CloudFormationClient).
*   **File:** `core/data/src/main/kotlin/com/locus/core/data/di/RuntimeModule.kt`
    *   **Scope:** `InstallIn(SingletonComponent::class)`
    *   **Logic:**
        *   `@Provides` `RemoteStorageInterface` (S3Client).
        *   `@Provides` `LocusCredentialsProvider`.
*   **File:** `core/data/src/main/kotlin/com/locus/core/data/di/NetworkModule.kt` (Optional/Shared)
    *   `@Provides` Shared components like `OkHttpClient` or JSON configuration if needed.

### Step 7: Testing
**Goal:** Verify behavior with Mockk.

*   **File:** `core/data/src/test/kotlin/com/locus/core/data/source/remote/aws/CloudFormationClientTest.kt`
*   **File:** `core/data/src/test/kotlin/com/locus/core/data/source/remote/aws/S3ClientTest.kt`
*   **Logic:**
    *   Mock the underlying AWS SDK clients directly with Mockk.
    *   Verify `createStack` handles polling correctly (e.g., mocks returning IN_PROGRESS then COMPLETE).
    *   Verify `findLocusBucket` calls the correct list/tag API.

## Validation Strategy
*   **Compilation:** Ensure `:core:data` compiles with new dependencies.
*   **Unit Tests:** Run `./gradlew :core:data:testDebugUnitTest`.
*   **Asset Check:** Verify `locus-stack.yaml` is readable via `InstrumentationRegistry` or ClassLoader in tests.
