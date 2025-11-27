# System Recovery & Reconnection

**Goal:** Restore access to an existing Locus Data Store on a new installation.

This flow handles "Reconnection" scenarios:
*   Installing Locus on a new device.
*   Re-installing on the same device after a factory reset.
*   Adding a secondary device (e.g., a tablet) to view existing data.

## Core Principles
1.  **Security First:** The app requires high-privilege keys to find your data, but immediately discards them in favor of restricted "Runtime Keys".
2.  **Lazy Loading:** We do not download your entire history (which could be gigabytes). We only fetch the "Inventory" (dates) initially.
3.  **New Identity:** To prevent data corruption, every fresh install generates a unique Device ID. You are a "New Writer" to the same "Old Book".

---

## Step 1: Authentication (The Bootstrap)

The user must provide AWS credentials to allow Locus to find the existing bucket.

### Option A: The Secure Standard (Recommended)
**Use temporary credentials generated via AWS CloudShell.**
*   **Expiration:** These keys automatically self-destruct in 1 hour.
*   **Process:**
    1.  User logs into AWS Console on their computer.
    2.  User opens **CloudShell** (terminal icon).
    3.  User runs: `aws sts get-session-token --duration-seconds 3600`
    4.  User enters the 3 resulting values into Locus:
        *   `AccessKeyId`
        *   `SecretAccessKey`
        *   `SessionToken`

### Option B: The Permanent Key (Convenience)
**Use the long-term IAM User credentials.**
*   **Risk:** If the phone is compromised during setup, these powerful keys could be exposed.
*   **Mitigation:** Locus stores these keys in **RAM Only**. Once the "Runtime Keys" are retrieved (Step 3), the Bootstrap Keys are wiped from memory.

---

## Step 2: Discovery

Locus scans the user's AWS account to find compatible Data Stores.

*   **Action:** App calls `s3:ListBuckets`.
*   **Filter:** App looks for buckets starting with `locus-`.
    *   *Why:* This avoids making N+1 network calls to check tags on every bucket.
*   **User Interface:**
    *   **Success:** Displays a list of found stores (e.g., `locus-pixel7-RT5`, `locus-backup-99X`).
    *   **Failure:** If no buckets are found, the app prompts the user to switch to the **Onboarding (New Setup)** flow.

---

## Step 3: The Key Swap

Once a bucket is selected, Locus secures itself for daily operation.

1.  **Retrieval:** The app reads the CloudFormation Stack Outputs associated with that bucket.
2.  **Runtime Keys:** It extracts the specific `RuntimeAccessKey` and `RuntimeSecretKey`.
    *   *Note:* These keys are strictly limited to **Read/Write access for this specific S3 bucket only**. They cannot create resources or delete the bucket.
3.  **Persistence:** The app saves the *Runtime Keys* to encrypted local storage.
4.  **Cleanup:** The app **immediately discards** the Bootstrap Keys (Step 1) from memory.

---

## Step 4: Data Synchronization (Lazy Inventory)

The app needs to know *what* data exists without downloading *all* the data.

*   **Action:** App performs a lightweight scan of the S3 folder structure (`tracks/YYYY/MM/DD/`).
*   **Result:**
    *   The "History" calendar is populated with indicators (dots) for every day that contains data.
    *   **No actual GPS tracks are downloaded yet.**
*   **Outcome:** The user sees their complete history timeline within seconds, preserving battery and data.

## Step 5: Visualization (On-Demand)

*   **Action:** User taps a specific date on the calendar.
*   **Fetch:** App downloads the compressed batch files for that specific day (e.g., `tracks/2023/10/27/*.json.gz`).
*   **Merge:** App merges the tracks from the "Old Device ID" (historical) and the "New Device ID" (current) onto the map transparently.
