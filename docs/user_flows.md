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
**Goal:** Continuous, reliable data collection with minimal user intervention.

*   **Status Indication:**
    *   A persistent notification indicates the service is running ("Locus is tracking").
    *   The notification updates to show the latest sync status or error state.
*   **Battery Management:**
    *   **Low Battery (<10%):** The app pauses uploads and reduces tracking frequency. The user receives a notification about the conservation mode.
    *   **Critical Battery (<3%):** Tracking stops completely to preserve the phone's remaining life.
    *   **Recovery:** When charged >15%, the app automatically resumes full-fidelity tracking and syncing.
*   **User Action:**
    *   The user sees the persistent notification as assurance of operation. No active interaction is required unless an error occurs.

## 3. Visualization (History View)
**Goal:** Verify, explore, and analyze historical movements using S3 as the source of truth.

*   **Step 1: Access & Discovery:**
    *   The user opens the history tab.
    *   A calendar view appears. Days containing historical data are visually highlighted, based on a locally cached index of the S3 bucket.
*   **Step 2: Date Selection:**
    *   The user selects a highlighted date.
*   **Step 3: Smart Retrieval (Cache-First):**
    *   **Local Check:** The app checks internal private storage for a cached track file for that date.
    *   **Remote Fetch:** If not cached, the app queries S3 for that date prefix and downloads the relevant `.gz` segments.
    *   **Processing:** The app stitches segments together, standardizes the time series, and updates the local cache.
*   **Step 4: Rendering:**
    *   **Source Verification:** The map renders *only* data confirmed to be in S3. Data currently in the local upload buffer is explicitly excluded to strictly verify remote storage.
    *   **Route:** The path is drawn on the offline-capable map.
    *   **Rapid Acceleration:** Events categorized as rapid acceleration or hard braking are marked with distinct icons on the route.
*   **Step 5: Signal Quality Heat Map:**
    *   The user can toggle a "Signal Quality" overlay.
    *   The track is colored to represent signal strength.
    *   The view visually differentiates between **WiFi** sources and **Cellular** sources (e.g., via distinct color palettes or stroke styles).
*   **Step 6: Detailed Inspection:**
    *   The user taps any point on the route.
    *   A detail panel displays the precise Timestamp, Speed, Battery Level, and Network Signal Strength (dBm).

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
