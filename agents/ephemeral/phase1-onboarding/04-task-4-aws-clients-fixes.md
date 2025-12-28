# Remediation Plan: AWS Infrastructure Clients

This document details the necessary code changes to resolve discrepancies and testing gaps in the current implementation of Task 4.

## Overview
The current implementation of `CloudFormationClient` and `S3Client` is untestable due to direct instantiation of AWS SDK clients. Additionally, there is a mismatch between the CloudFormation template outputs and the Kotlin client's expectations.

**Note to Implementer:** There is a name collision between the local wrapper classes (e.g., `com.locus.core.data.source.remote.aws.S3Client`) and the AWS SDK classes (e.g., `aws.sdk.kotlin.services.s3.S3Client`). You must use **Fully Qualified Class Names (FQCN)** or **Import Aliases** to distinguish them.

## Required Changes

### 1. Fix CloudFormation Template Outputs
**Goal:** Align the YAML output keys with the Kotlin code's expectations to ensure credentials are parsed correctly.

*   **File:** `core/data/src/main/assets/locus-stack.yaml`
*   **Action:** Rename the keys in the `Outputs` section.
    *   Change `BucketName` -> `LocusBucketName`
    *   Change `AccessKeyId` -> `RuntimeAccessKeyId`
    *   Change `SecretAccessKey` -> `RuntimeSecretAccessKey`
*   **Justification:** `CloudFormationClient.kt` explicitly looks for these specific keys (`LocusBucketName`, etc.). The current generic names cause the parsing logic to fail.

### 2. Introduce `AwsClientFactory`
**Goal:** Decouple `CloudFormationClient` and `S3Client` from the static AWS SDK builders to enable unit testing.

*   **File:** `core/data/src/main/kotlin/com/locus/core/data/source/remote/aws/AwsClientFactory.kt` (Create New)
*   **Action:** Define an interface and implementation.
    ```kotlin
    package com.locus.core.data.source.remote.aws

    import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
    import javax.inject.Inject
    // Use aliases to avoid confusion with local wrapper classes
    import aws.sdk.kotlin.services.cloudformation.CloudFormationClient as SdkCloudFormationClient
    import aws.sdk.kotlin.services.s3.S3Client as SdkS3Client

    interface AwsClientFactory {
        fun createCloudFormationClient(region: String, credentialsProvider: CredentialsProvider): SdkCloudFormationClient
        fun createS3Client(region: String, credentialsProvider: CredentialsProvider): SdkS3Client
    }

    class AwsClientFactoryImpl @Inject constructor() : AwsClientFactory {
        override fun createCloudFormationClient(region: String, credentialsProvider: CredentialsProvider): SdkCloudFormationClient {
            return SdkCloudFormationClient {
                this.region = region
                this.credentialsProvider = credentialsProvider
            }
        }

        override fun createS3Client(region: String, credentialsProvider: CredentialsProvider): SdkS3Client {
            return SdkS3Client {
                this.region = region
                this.credentialsProvider = credentialsProvider
            }
        }
    }
    ```
*   **DI:** Update `core/data/src/main/kotlin/com/locus/core/data/di/NetworkModule.kt` (or `DataModule.kt`) to provide `AwsClientFactoryImpl` as a Singleton binding for `AwsClientFactory`.

### 3. Refactor `CloudFormationClient` (Local Wrapper)
**Goal:** Use the factory for SDK client instantiation.

*   **File:** `core/data/src/main/kotlin/com/locus/core/data/source/remote/aws/CloudFormationClient.kt`
*   **Action:**
    *   Inject `AwsClientFactory` in the constructor.
    *   In `createStack`, replace the `AwsCloudFormationClient { ... }` block with `awsClientFactory.createCloudFormationClient(...)`.
    *   In `findLocusBucket`, replace the `AwsS3Client { ... }` block with `awsClientFactory.createS3Client(...)`.
    *   **Crucial:** Ensure `waitForStackCreation` accepts `aws.sdk.kotlin.services.cloudformation.CloudFormationClient` (not the local wrapper type).

### 4. Refactor `S3Client` (Local Wrapper)
**Goal:** Use the factory for SDK client instantiation.

*   **File:** `core/data/src/main/kotlin/com/locus/core/data/source/remote/aws/S3Client.kt`
*   **Action:**
    *   Inject `AwsClientFactory` in the constructor.
    *   In `uploadTrack`, replace the `AwsS3Client { ... }` block with `awsClientFactory.createS3Client(...)`.

### 5. Implement Comprehensive Unit Tests
**Goal:** Verify logic using Mockk.

*   **File:** `core/data/src/test/kotlin/com/locus/core/data/source/remote/aws/CloudFormationClientTest.kt`
*   **Action:** Rewrite using `Mockk`.
    *   **Mocking Strategy:**
        ```kotlin
        // Use alias for SDK types
        import aws.sdk.kotlin.services.cloudformation.CloudFormationClient as SdkCloudFormationClient

        val mockSdkClient = mockk<SdkCloudFormationClient>()
        val mockFactory = mockk<AwsClientFactory>()
        every { mockFactory.createCloudFormationClient(any(), any()) } returns mockSdkClient
        ```
    *   **Test Polling:**
        *   Mock `describeStacks` to return `StackStatus.CreateInProgress` on the first call.
        *   Mock `describeStacks` to return `StackStatus.CreateComplete` on the second call.
        *   Verify that `waitForStackCreation` loops correctly and returns `Success`.
    *   **Test Failure:** Mock `StackStatus.CreateFailed` and verify `LocusResult.Failure`.
    *   **Test Outputs:** Verify `createStack` correctly maps the mock outputs to `StackOutputs`.

*   **File:** `core/data/src/test/kotlin/com/locus/core/data/source/remote/aws/S3ClientTest.kt`
*   **Action:** Rewrite using `Mockk`.
    *   Mock `AwsClientFactory` to return a mock `aws.sdk.kotlin.services.s3.S3Client`.
    *   Verify `uploadTrack` calls `putObject` with the correct bucket and key.
