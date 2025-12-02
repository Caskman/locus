# Operational Architecture Gap Analysis & Decisions

**Date:** October 2023
**Status:** Approved

This document records the analysis of gaps in the initial Operational Architecture and the architectural decisions made to address them. These decisions have been incorporated into the respective specifications.

## 1. Identified Gaps

### 1.1. Infrastructure Incident Response (The "User as Admin" Gap)
*   **Gap:** Users own the infrastructure (S3) but lack the expertise to diagnose failures (e.g., distinguishing "Bucket Missing" from "Payment Suspended").
*   **Decision:** The application will not request elevated permissions to fix issues automatically. Instead, it will implement an **Infrastructure Diagnosis Layer**.
*   **Implementation:** The app will map specific AWS error codes (e.g., `NoSuchBucket`, `AccountProblem`) to pre-defined, actionable user instructions (e.g., "Your bucket is missing. Please check your AWS Console.").

### 1.2. Cost Safety (The "Infinite Bill" Risk)
*   **Gap:** A bug in the sync logic could theoretically cause an infinite retry loop, driving up S3 request costs and data transfer fees.
*   **Decision:** Implement a **Traffic Guardrail (Circuit Breaker)**.
*   **Implementation:**
    *   **Limit:** 50MB per day (Soft Cap). This accommodates normal usage (2-5MB/day) while preventing runaway costs.
    *   **Action:** If the limit is exceeded, all non-critical background syncs are paused. A notification is shown to the user.

### 1.3. Telemetry Trust
*   **Gap:** Users are asked to opt-in to Community Telemetry but cannot verify that the "Anonymization Salt" is actually working.
*   **Decision:** Implement a **"Preview Telemetry"** feature.
*   **Implementation:** A button in Settings will generate and display a sample JSON payload using the real device ID and salt, allowing the user to audit the data before enabling the feature.

### 1.4. Data Management (Deletion)
*   **Gap:** Users may want to delete accidental recordings.
*   **Decision:** To maintain the security principle of "Least Privilege," the Runtime Credentials will **NOT** be granted `s3:DeleteObject`.
*   **Implementation:** Data deletion is a manual process performed by the user via the AWS Console. A guide will be added to the documentation backlog.

### 1.5. Multi-Device Scope
*   **Gap:** Ambiguity regarding multiple devices sharing a bucket.
*   **Decision:** **Single Device per Bucket**.
*   **Implementation:** The architecture explicitly assumes a 1:1 relationship between a Device and an S3 Bucket to prevent data collisions and complexity.

### 1.6. External Monitoring ("Dead Man's Switch")
*   **Gap:** No external system notifies the user if the phone/app dies completely.
*   **Decision:** Out of scope for V1 due to CloudFormation complexity.
*   **Implementation:** Added to the Operational Backlog as a future "Advanced Monitoring" guide (using CloudWatch Alarms).

## 2. Summary of Changes

| Component | Change | Reason |
| :--- | :--- | :--- |
| **Network Spec** | Added `TrafficGuardrail` (50MB/day). | Cost Safety. |
| **UI Spec** | Added `Telemetry Preview` and `Actionable Error Dialogs`. | Trust & Usability. |
| **Watchdog Spec** | Added `InfrastructureDiagnosis` logic. | Incident Response. |
| **Backlog** | Added `Data Deletion Guide` and `Dead Man's Switch`. | Scope Management. |
