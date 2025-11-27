# Android Client Architecture

The Android application acts as both the data collector and the infrastructure controller.

## 1. Provisioner (Setup)
*   **Role:** One-time setup wizard.
*   **Action:** Uses user-provided API Keys to deploy a **CloudFormation Stack**.
*   **Outcome:** Creates the S3 Bucket with correct settings (Object Lock, Versioning).

## 2. Tracker Service (The Engine)
*   **Role:** Always-on data collection.
*   **Component:** `ForegroundService` with `PARTIAL_WAKE_LOCK`.
*   **Action:** Captures GPS (1Hz Normal, 10s < 10%, 60s < 3%), buffers to local Room DB.
*   **Safety:** Reduces sample rate and pauses uploads when battery is low. Resumes > 15%.

## 3. Sync Worker (The Uploader)
*   **Role:** Reliable data transport.
*   **Trigger:** Periodic (e.g., every 15 mins) via WorkManager. Paused if battery < 10%.
*   **Action:**
    *   Query oldest points from Room DB.
    *   Compress to Gzip.
    *   Upload to S3.
    *   **On Success:** Delete from local Room DB.

## 4. Visualizer (The View)
*   **Role:** User interface for history.
*   **Action:**
    *   Lists S3 objects by date prefix.
    *   Downloads and decompresses tracks.
    *   Renders on OpenStreetMap (osmdroid).
