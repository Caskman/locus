# Analysis of Task 11: Admin Upgrade Plan

## Executive Summary
The proposed plan for the Admin Upgrade feature (`agents/ephemeral/phase1-onboarding/11-admin-upgrade-plan.md`) contains a **critical architectural risk** that could lead to catastrophic data loss. The proposal to create a separate `locus-admin.yaml` template introduces the high probability of Logical ID mismatch or template drift, which would cause CloudFormation to delete and recreate the user's S3 bucket during the upgrade process.

I recommend **rejecting the separate template strategy** in favor of a **Single Template with Conditional Logic**. This ensures the persistence of resources is guaranteed by the CloudFormation engine while enabling the "Admin" capabilities via parameters.

## Detailed Findings & Resolutions

### 1. Critical Risk: Separate CloudFormation Templates
*   **Finding:** The plan instructs to create `core/data/src/main/assets/locus-admin.yaml` as a separate file.
*   **Analysis:** CloudFormation resource identity is tied to the **Logical ID** in the template. If `locus-admin.yaml` has even a minor deviation in the Logical ID of the `LocusDataBucket` (e.g., typo, or if `locus-stack.yaml` is refactored later but `locus-admin.yaml` is not), the `UpdateStack` operation will treat the bucket as a *new* resource and **delete the old bucket**, destroying all user history. Furthermore, maintaining two templates violates the DRY principle and increases the testing burden.
*   **Resolution:** **Do not create a separate file.** Instead, modify the existing `locus-stack.yaml`:
    1.  Add a Parameter: `IsAdmin` (Type: String, Default: "false", AllowedValues: ["true", "false"]).
    2.  Add a Condition: `AdminEnabled` equals `true`.
    3.  Modify `LocusPolicy` to include an additional `Statement` that is active only when `Condition: AdminEnabled` is true.
    *   This guarantees that the Bucket resource (which is unconditional) remains untouched during the update.

### 2. Missing Data Requirement: Stack Name Reconstruction
*   **Finding:** The plan correctly identifies the need to add `deviceName` to `RuntimeCredentials`, but the justification assumes it is only for "persistence".
*   **Analysis:** To perform an `UpdateStack` call, the system *must* know the exact Stack Name. The current convention is `locus-user-${deviceName}`. Without storing `deviceName` locally in `RuntimeCredentials` during the initial setup, the app cannot reliably reconstruct the stack name to target the update (especially if the user customized the name).
*   **Resolution:** Explicitly mandate that the migration/update of `RuntimeCredentials` includes logic to store `deviceName`. For existing Phase 0 installs (which lack this), the upgrade path might be blocked or require re-entering the name. Given Phase 0 is ephemeral, it is acceptable to enforce this only for new installs or wipe existing data, but the code must handle the `null` case safely (e.g., force a reset if `deviceName` is missing during upgrade attempt).

### 3. Missing Infrastructure Capability: `UpdateStack`
*   **Finding:** The plan correctly identifies the need for `updateStack` in `CloudFormationClient`.
*   **Analysis:** The current interface only supports `createStack`. `UpdateStack` has different error modes (e.g., "No updates to be performed" is a success case, whereas for Create it's not applicable).
*   **Resolution:** Ensure the implementation of `updateStack` specifically catches and suppresses the `ValidationError` with message "No updates are to be performed", treating it as a successful completion.

### 4. IAM Policy Specifics
*   **Finding:** The plan mentions a "Hybrid IAM Policy" but doesn't define the JSON/YAML structure.
*   **Analysis:** The Admin policy requires `s3:ListBucket` and `s3:GetObject` on *any* resource tagged with `LocusRole: DeviceBucket`.
*   **Resolution:** The `locus-stack.yaml` modification should look like this:
    ```yaml
    Parameters:
      IsAdmin:
        Type: String
        Default: "false"
    Conditions:
      CreateAdminResources: !Equals [!Ref IsAdmin, "true"]
    Resources:
      LocusPolicy:
        Type: AWS::IAM::Policy
        Properties:
          PolicyDocument:
            Statement:
              - Effect: Allow
                Action: ... (Standard Actions on Own Bucket) ...
              - Fn::If:
                - CreateAdminResources
                - Effect: Allow
                  Action:
                    - s3:ListBucket
                    - s3:GetObject
                  Resource: "*"
                  Condition:
                    StringEquals:
                      aws:ResourceTag/LocusRole: DeviceBucket
                - !Ref "AWS::NoValue"
    ```

### 5. Verification Gap
*   **Finding:** The plan lists Unit Tests and Manual Verification but misses automated integration checks for the template validity.
*   **Analysis:** Since we are modifying the core infrastructure template, we must ensure it is valid YAML and CloudFormation syntax.
*   **Resolution:** Add a step to run `cfn-lint` (already available in the environment) on `locus-stack.yaml` as part of the validation steps.

## Revised Plan Recommendation

The plan should be rewritten to focus on **modifying the existing stack template** rather than creating a new one.

**Revised Steps:**
1.  **Modify `locus-stack.yaml`**: Add `IsAdmin` parameter and conditional logic for the expanded IAM policy.
2.  **Update Domain Models**: Add `deviceName` and `isAdmin` to `RuntimeCredentials`.
3.  **Update Infrastructure**: Implement `CloudFormationClient.updateStack` (handling "No updates" case).
4.  **Implement Logic**: `UpgradeAccountUseCase` uses `IsAdmin="true"` parameter when calling `updateStack`.
5.  **UI & Worker**: Implement the UI and Worker modes as originally planned.
