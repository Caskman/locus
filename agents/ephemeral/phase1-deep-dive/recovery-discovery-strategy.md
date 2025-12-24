# Technical Deep Dive: Recovery & Discovery Strategy

## Overview
This document defines how Locus handles "Link Existing Store", "Admin Upgrade", and the discovery of valid S3 buckets. It introduces two new CloudFormation templates.

## CloudFormation Templates

### 1. `locus-stack.yaml` (Standard / New Store)
*   **Purpose:** Setup a completely new environment.
*   **Resources:** `S3Bucket` + `IAMUser` + `AccessKey` + `Policy`.
*   **Existing:** Defined in `docs/technical_discovery/locus-stack.yaml`.

### 2. `locus-user.yaml` (Link Existing Store)
*   **Purpose:** Add a new device (Identity) to an *existing* bucket.
*   **Input Parameters:**
    *   `StackName`: Unique Device Name (e.g., "Pixel8").
    *   `TargetBucket`: Name of the existing S3 bucket.
*   **Resources:**
    *   `IAMUser`: `locus-user-{StackName}`.
    *   `AccessKey`: For the new user.
    *   `Policy`: Scoped strictly to `arn:aws:s3:::{TargetBucket}`.
*   **Logic:** Does **NOT** create a Bucket.

### 3. `locus-admin.yaml` (Admin Upgrade)
*   **Purpose:** Create a privileged identity for auditing/management.
*   **Input Parameters:**
    *   `StackName`: Admin Device Name (e.g., "AdminTablet").
    *   `TargetBucket`: The "Home" bucket for this device.
*   **Resources:**
    *   `IAMUser`: `locus-admin-{StackName}`.
    *   `AccessKey`: For the admin user.
    *   `Policy`:
        *   **Discovery:** `s3:ListAllMyBuckets` and `s3:GetBucketTagging` on `*` (All Resources). Required to find valid buckets.
        *   **Read-Only:** `s3:ListBucket`, `s3:GetObject` on `*` (All Resources) where `Tag:LocusRole == DeviceBucket`.
        *   **Write:** `s3:PutObject` on `arn:aws:s3:::{TargetBucket}` (To allow the admin device to track itself).

## Discovery Logic (Link Existing Store)

When the user selects "Link Existing Store", the app must find valid buckets. We employ an **Async Tag Validation** strategy to balance speed and accuracy.

**Algorithm:**
1.  **List (Fast):** Use Bootstrap Credentials to call `s3.listBuckets()`.
2.  **Filter (Client-Side):** Keep buckets where `name.startsWith("locus-")`.
3.  **Display (Immediate):** Show the list to the user immediately.
    *   **State:** All items start as `Validating` (Spinner).
4.  **Validate (Background):**
    *   Launch parallel coroutines (one per bucket).
    *   Call `s3.getBucketTagging(bucketName)`.
    *   **Check:** Is Tag `LocusRole` == `DeviceBucket`?
    *   **Update State:**
        *   If Match -> `Available` (Clickable).
        *   If Mismatch/Error -> `Invalid` (Disabled/Red).
5.  **Selection:** User clicks an `Available` bucket.
6.  **Provisioning:** App deploys `locus-user.yaml` with the selected `TargetBucket`.

## Device ID Generation (Anti-Split-Brain)

To prevent data collisions when a device is re-installed or recovered:

1.  **New Install:** Generate `UUID.randomUUID()`.
2.  **Recovery (Link):** Generate `UUID.randomUUID()`.
3.  **Implication:** Every installation is effectively a "New Device" from the data perspective.
4.  **Data Structure:** `tracks/YYYY/MM/DD/{UUID}_{timestamp}.json.gz`.
    *   Since the UUID is unique, two devices (or two installs on the same device) will never overwrite each other's files, even if timestamps are identical.

## Admin Discovery

Admin users have broad read access.

1.  **Scope:** Admin keys can read *any* bucket that has the tag `LocusRole=DeviceBucket`.
2.  **Discovery:**
    *   Call `s3.listBuckets()`.
    *   Call `s3.getBucketTagging(bucketName)` for each candidate? -> **Too Slow (N+1)**.
    *   **Optimization:** Just try to `ListObjects` on the target bucket. If it fails (403), we assume it's not a Locus bucket or we don't have access.
    *   *Refined Strategy:* The Admin Map Screen will simply attempt to access the buckets it knows about (from a locally cached list or user input), or scan `locus-` buckets lazily.
