# Android Client Architecture

The Android application acts as both the data collector and the infrastructure controller. It implements the behaviors defined in the Functional Requirements.

## 1. Provisioner (Setup)
*   **Role:** Handles the one-time setup and infrastructure creation.
*   **Components:** UI Wizards, AWS SDK (CloudFormation client).
*   **Responsibilities:**
    *   Accept user credentials (Bootstrap Keys).
    *   Deploy the CloudFormation Stack to create the S3 Bucket.
    *   Validate unique Device IDs and Stack Names.
    *   Transition to Runtime Keys upon success.
*   **Implements Requirements:** [Setup & Onboarding](requirements/setup_onboarding.md)

## 2. Tracker Service (The Engine)
*   **Role:** Performs the "Always-on" data collection.
*   **Component:** `ForegroundService`.
*   **Key Mechanisms:**
    *   **Wake Locks:** Uses `PARTIAL_WAKE_LOCK` to ensure CPU uptime.
    *   **Location Manager:** direct interaction with standard Android Location APIs (GPS).
    *   **Sensor Manager:** Monitoring accelerometer for the "Stationary Mode" state machine.
    *   **Battery Monitor:** BroadcastReceiver to trigger "Battery Safety Protocol" state changes.
*   **Output:** Writes raw `Location` objects to the local Room Database.
*   **Implements Requirements:** [Data Collection & Tracking](requirements/data_collection.md)

## 3. Sync Worker (The Uploader)
*   **Role:** Handles reliable data transport and storage management.
*   **Component:** `WorkManager` (PeriodicWorkRequest).
*   **Responsibilities:**
    *   **Batching:** Query the oldest pending records from the Room DB.
    *   **Formatting:** Convert records to NDJSON and compress (Gzip).
    *   **Transport:** Upload to S3 using the Runtime Keys.
    *   **Cleanup:** Delete local records only after a successful S3 response (`200 OK`).
    *   **Indexing:** Update the local "History Index" upon successful upload.
*   **Implements Requirements:** [Data Storage & Management](requirements/data_storage.md)

## 4. Visualizer (The View)
*   **Role:** Provides the user interface for exploring history.
*   **Components:** `osmdroid` MapView, Local Cache (File System).
*   **Responsibilities:**
    *   **Indexing:** Maintain a local index of available dates (synced via `ListObjects` or Write-Through).
    *   **Rendering:** Draw tracks on the map, applying downsampling for performance.
    *   **Caching:** Store downloaded track files locally to support offline viewing.
*   **Implements Requirements:** [Visualization & History](requirements/visualization.md)

## 5. Local Data Persistence
*   **Role:** Intermediate buffer and state storage.
*   **Components:**
    *   **Room Database:** Stores pending location points and application logs.
    *   **EncryptedSharedPreferences:** Stores sensitive AWS credentials (Runtime Keys) and configuration (Device ID).
