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
        *   **Read-Only:** `s3:ListBucket`, `s3:GetObject` on `*` (All Resources) where `Tag:LocusRole == DeviceBucket`.
        *   **Write:** `s3:PutObject` on `arn:aws:s3:::{TargetBucket}` (To allow the admin device to track itself).

## Discovery Logic (Link Existing Store)

When the user selects "Link Existing Store", the app must find valid buckets.

**Algorithm:**
1.  **Credentials:** Use the provided Bootstrap Credentials.
2.  **API Call:** `s3.listBuckets()`.
3.  **Client-Side Filter:**
    *   Iterate through `ListBucketsResponse.buckets`.
    *   **Keep if:** `bucket.name.startsWith("locus-")`.
    *   *Note:* We rely on the naming convention for speed and simplicity in Phase 1.
4.  **UI Presentation:**
    *   Show list of names (e.g., `locus-data-2024`, `locus-my-archive`).
    *   User selects one.
5.  **Provisioning:**
    *   App deploys `locus-user.yaml`.
    *   Passes selected name as `TargetBucket`.

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
