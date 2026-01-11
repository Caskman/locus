# Analysis of Task 11 Admin Upgrade Plan

**Date:** 2026-01-11
**Related Task:** Task 11 - Admin Upgrade Flow
**Target:** `agents/ephemeral/phase1-onboarding/11-admin-upgrade-plan.md`

## Overview
The proposed plan for Task 11 introduces a robust "Single Conditional Template" strategy to handle the Admin Upgrade flow, correctly identifying the risk of data loss associated with the original multi-template approach. However, the analysis has identified significant gaps regarding the recovery of Admin status on new devices and the handling of legacy stacks.

## Findings

### 1. Loss of Admin Status on Account Recovery (Critical)
- **Problem:** The plan updates `locus-stack.yaml` to include an `IsAdmin` input parameter but fails to add a corresponding `Output`.
- **Impact:** When `RecoverAccountUseCase` runs on a new device, it reads the Stack Outputs to construct the `RuntimeCredentials`. Without an explicit `IsAdmin` output, the system cannot determine if the recovered user is already an Admin. Consequently, the local `RuntimeCredentials.isAdmin` property will default to `false`. This forces the user to perform the "Upgrade" flow again (which is redundant) or creates a confusing state where the user knows they are an admin but the UI does not reflect it.
- **Resolution:** Add `IsAdmin` to the `Outputs` section of `locus-stack.yaml` (returning the value of the input parameter or a condition boolean) and update `RecoverAccountUseCase` to map this output to `RuntimeCredentials.isAdmin`.

### 2. Breaking Change for Legacy Stacks (Critical)
- **Problem:** The plan relies on a new `LocusStackName` tag to populate the mandatory `RuntimeCredentials.stackName` field during recovery (Step 7). Stacks created prior to this task (i.e., during development of Tasks 1-10) will not have this tag.
- **Impact:** `RecoverAccountUseCase` will fail for any existing stack because it cannot resolve the mandatory `stackName`. This renders all currently deployed stacks unrecoverable.
- **Resolution:** Explicitly acknowledge this as a **Breaking Change** in the plan. Since this is Phase 1 (Greenfield), a "Clean Slate" policy is acceptable, but it must be documented. The `RecoverAccountUseCase` should ideally handle this gracefully (e.g., by checking for the tag and returning a specific failure code like `LegacyStackUnsupported` instead of crashing).

### 3. Incomplete Data Layer Specification (Major)
- **Problem:** Step 3 instructs updating `RuntimeCredentials` and its DTOs to include the new mandatory `stackName` and `isAdmin` fields, but it omits the necessary update to the `SecureStorageDataSource` logic.
- **Impact:** `SecureStorageDataSource` typically requires manual mapping of fields to `SharedPreferences` keys. If the implementation is not updated to serialize and deserialize these new fields, the application will lose this critical context upon restart.
- **Resolution:** Add an explicit action to update `SecureStorageDataSource.saveCredentials` and `getCredentials` to handle the serialization of the new fields.

### 4. Verification Dependency (Minor)
- **Problem:** The plan adds the `tag:GetResources` permission for Admins to enable future discovery features, but it does not mention adding the `aws.sdk.kotlin:resourcegroupstaggingapi` dependency.
- **Impact:** While the *Upgrade Flow* itself doesn't strictly need to *call* this API, the "Verification" step (Step 16) or future admin features will require the client library.
- **Resolution:** Add a suggestion to include the `aws.sdk.kotlin:resourcegroupstaggingapi` dependency in `libs.versions.toml` and `core/domain/build.gradle.kts` to facilitate verification.

## Conclusion
The core strategy of using a single `locus-stack.yaml` with conditional logic is sound and superior to the original tasking. However, the plan must be amended to address the persistence of Admin status across recoveries and to explicitly handle the breaking change for legacy stacks.
