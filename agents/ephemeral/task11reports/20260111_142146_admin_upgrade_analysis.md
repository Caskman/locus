# Report: Analysis of Admin Upgrade Plan

**File:** `agents/ephemeral/phase1-onboarding/11-admin-upgrade-plan.md`
**Timestamp:** 20260111_142146

## 1. Findings Summary

A deep analysis of the Admin Upgrade plan reveals two critical gaps that would prevent functionality, particularly for recovered accounts, and one implementation defect.

1.  **Critical Recovery Gap:** `RecoverAccountUseCase` cannot restore the `stackName` required for future upgrades because the current `locus-stack.yaml` does not tag the bucket with the stack name. Without this, a recovered user is permanently locked out of the Admin Upgrade feature.
2.  **Missing Capability in Update:** The plan fails to specify `CAPABILITY_NAMED_IAM` for the `updateStack` operation. CloudFormation updates involving IAM resources (even if unchanged) require this capability, or the update will fail.
3.  **Ambiguous `AWS::NoValue` Usage:** The plan suggests using `AWS::NoValue` to remove a policy block but implies removing an entire resource block in one sentence and a list item in another. This requires precise CloudFormation syntax definition to avoid template validation errors.

## 2. Detailed Analysis & Resolution

### Finding 1: Recovery Gap (Critical)

**Analysis:**
The plan correctly identifies that `RuntimeCredentials` must store the `stackName` to target the CloudFormation stack for an update later.
- For **New Users** (Provisioning), the `stackName` is known at creation time.
- For **Recovered Users**, the `RecoverAccountUseCase` scans S3 buckets to find `locus-role=DeviceBucket`. It **does not** currently have access to the original CloudFormation Stack Name.
- The plan attempts to solve this by "Extracting StackName from CloudFormation outputs". However, `RecoverAccountUseCase` typically interacts with S3 (`ListBuckets`), not CloudFormation, because it doesn't know *which* stack to query (Circular dependency: You need the Stack Name to query the Stack).
- Without the Stack Name, a recovered user cannot perform `UpdateStack` later.

**Resolution:**
We must persist the Stack Name in a discoverable location on the S3 Bucket itself.

1.  **Modify `locus-stack.yaml`:** Add a new tag to the `LocusDataBucket` resource.
    ```yaml
    Tags:
      - Key: LocusRole
        Value: DeviceBucket
      - Key: LocusStackName
        Value: !Ref "AWS::StackName"
    ```
2.  **Update `ScanBucketsUseCase` / `RecoverAccountUseCase`:**
    - When scanning buckets, read the tag set.
    - Extract the value of `LocusStackName`.
    - Use this value to populate the mandatory `stackName` field in `RuntimeCredentials`.

**Impact:** This ensures that even if local data is wiped, the app can recover the link to the CloudFormation stack, enabling future upgrades.

---

### Finding 2: Missing Capability in Update

**Analysis:**
The plan specifies: `Calls client.updateStack`.
CloudFormation requires explicit acknowledgement (`CAPABILITY_NAMED_IAM`) when working with stacks that contain IAM resources. Even if the *update* doesn't change IAM resources, the *stack* contains them, and the capability must be asserted during the update call.
Failing to pass this parameter causes the AWS SDK to throw `InsufficientCapabilitiesException`.

**Resolution:**
The `CloudFormationClient.updateStack` implementation must explicitly include this capability.

**Action:**
Update the plan to specify:
- `request.capabilities = listOf(Capability.CapabilityNamedIam)`

---

### Finding 3: Ambiguous `AWS::NoValue` Usage

**Analysis:**
The plan states: *"If `AdminEnabled` is false, use `Ref: AWS::NoValue` to remove the block entirely."*
In CloudFormation, `AWS::NoValue` removes a property or a list item. The `Statement` property of `AWS::IAM::Policy` is a list.
Using `Fn::If` directly inside the list to conditionally include an object is the correct approach.

**Resolution:**
Clarify the template syntax in the plan to ensure correct implementation:

```yaml
Statement:
  - Effect: Allow
    Action: ... (Standard Permissions)
  - !If
    - AdminEnabled
    - Effect: Allow
      Action: ... (Admin Permissions)
    - !Ref "AWS::NoValue"
```

## 3. Revised Plan Actions

The following adjustments should be made to the implementation steps:

1.  **Step 2 (locus-stack.yaml):** Explicitly add the `LocusStackName` tag to the Bucket resource.
2.  **Step 2 (locus-stack.yaml):** Clarify the `Fn::If` syntax for the Policy Statement.
3.  **Step 4 (CloudFormationClient):** Explicitly require `CAPABILITY_NAMED_IAM` in the `updateStack` method.
4.  **Step 7 (RecoverAccountUseCase):** Explicitly require reading the `LocusStackName` tag from S3 to populate `RuntimeCredentials`.
