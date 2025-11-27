# User Flows

This document defines the core user journeys for the Locus application, mapping the interaction from initialization to daily usage and data visualization.

## 1. Onboarding (The Bootstrap)
**Goal:** Transition from a fresh install to a fully provisioned, cloud-connected system.

*   **Prerequisite:** The user has manually created an IAM User in their AWS Console and attached the `iam-bootstrap-policy.json`.
*   **Step 1: Welcome & Permissions:**
    *   The app explains the need for "Always Allow" location access.
    *   The user grants location, notification, and battery optimization exemptions.
*   **Step 2: Credential Entry:**
    *   The user enters their AWS Access Key ID and Secret Access Key.
    *   *Constraint:* The app validates the format of the keys before proceeding.
*   **Step 3: Stack Configuration:**
    *   The user specifies a unique name for their S3 bucket (or accepts a generated default).
*   **Step 4: Provisioning:**
    *   The app triggers the CloudFormation deployment.
    *   A progress indicator shows the creation of resources (Bucket, Policies).
*   **Outcome:** The system confirms "Setup Complete" and immediately begins the background tracking service.

## 2. Daily Operation (Passive)
**Goal:** Continuous, reliable data collection with detailed transparency, resilience, and smart adaptation.

*   **Transparency (Status Indication):**
    *   The app utilizes a persistent notification to confirm active operation.
    *   **Format:** The notification content follows the structure `[Recording Status] • [Sync Status]`.
    *   **Examples:**
        *   *Healthy:* "Tracking (High Accuracy) • Synced"
        *   *Buffering:* "Tracking (Offline) • Buffered: 140 pts"
        *   *Acquiring:* "Searching for GPS..."

*   **Smart Adaptation (Sensing):**
    *   The system dynamically adjusts behavior based on movement and power states.
    *   **Stationary:** The app enters "Low Power Mode" (GPS only, 1Hz) to save battery.
    *   **Moving:** The app switches to "Full Fidelity Mode" (GPS + Sensors) for detailed tracking.
    *   **Battery Saver Mode:** The app ignores the OS "Battery Saver" toggle and continues to request the Partial Wake Lock to ensure data continuity.

*   **Battery Management:**
    *   **Low Battery (<10%):** Uploads pause, and tracking frequency reduces. The user sees a "Paused: Low Battery" status.
    *   **Critical Battery (<3%):** Tracking stops completely to preserve the phone's remaining life.
    *   **Recovery:** When charged >15%, the app automatically resumes full-fidelity tracking and syncing.

*   **Error Recovery Hierarchy:**
    *   **Tier 1: Transient Errors (Self-Healing):**
        *   *Scenarios:* Network timeouts, S3 5xx errors, GPS signal loss.
        *   *Action:* The app buffers data locally and retries silently with exponential backoff.
        *   *User Feedback:* Invisible to the user, other than a growing "Buffered" count in the notification.
    *   **Tier 2: Environmental Pauses (State Awareness):**
        *   *Scenarios:* Airplane Mode, No Internet, Low Battery.
        *   *Action:* Uploads (or tracking) pause until the condition clears.
        *   *User Feedback:* The notification text updates to explain the pause (e.g., "Paused: Waiting for Network"). No sound or vibration occurs.
    *   **Tier 3: Fatal Errors (Action Required):**
        *   *Scenarios:* AWS Auth Failure (403), Bucket Missing (404), Permission Revoked.
        *   *Action:* The app triggers a "Circuit Breaker," permanently stopping network or tracking attempts to prevent battery drain.
        *   *User Feedback:* A distinct, alerting notification appears (e.g., "Upload Failed: Access Denied"). Tapping it leads the user to the resolution screen.

*   **Resilience (Intervention Loops):**
    *   **Auto-Start:** The service automatically launches on device boot.
    *   **Permission Loss:** If the OS revokes background location, the app immediately fires a high-priority alert demanding user intervention.
    *   **Storage Limits:** If the local buffer exceeds a safety threshold (e.g., 10k points), the system warns the user to prevent data loss.

## 3. Visualization (History View)
**Goal:** Verify and explore historical movements.

*   **Step 1: Access:**
    *   The user opens the main application UI.
*   **Step 2: Date Selection:**
    *   The user selects a specific date from a calendar interface.
*   **Step 3: Data Retrieval:**
    *   The app queries the S3 bucket for all track segments matching that date.
    *   The app downloads and decompresses the relevant Gzip files.
*   **Step 4: Rendering:**
    *   The map displays the day's route.
    *   The interface shows summary statistics (Total Distance, Duration).
    *   Gaps in data (e.g., dead battery) are visually distinct from active tracking.

## 4. Manual Sync & Status
**Goal:** Immediate verification of data safety.

*   **Step 1: Status Check:**
    *   The main screen displays the "Last Successful Sync" timestamp.
    *   It shows the current size of the local buffer (number of points waiting to upload).
*   **Step 2: Forced Sync:**
    *   The user taps a "Sync Now" button.
    *   The app immediately packages the local buffer, compresses it, and attempts an upload.
*   **Step 3: Feedback:**
    *   **Success:** The local buffer count drops to zero; the "Last Sync" time updates to "Just now".
    *   **Failure:** An error message explains the issue (e.g., "No Network", "AWS Error").

## 5. System Recovery (Re-provisioning)
**Goal:** Restore access to existing data on a new device.

*   **Scenario:** A user installs the app on a new phone but wants to keep using their existing S3 bucket.
*   **Step 1: Credential Entry:**
    *   The user enters their AWS keys.
*   **Step 2: Bucket Discovery:**
    *   The app checks for existing Locus stacks/buckets associated with these keys.
    *   The user selects the existing bucket.
*   **Outcome:** The app links to the existing bucket without attempting to create a new CloudFormation stack. History becomes immediately available.
