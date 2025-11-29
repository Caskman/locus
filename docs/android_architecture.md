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
    *   **Location Strategy:**
        *   **Primary:** Fused Location Provider (Google Play Services) for battery efficiency, rapid fix, and indoor accuracy.
        *   **Fallback:** Raw Android Location Manager (GPS/Network) if Play Services are unavailable or disabled by the user.
    *   **Stationary Manager:**
        *   **Primary:** Significant Motion Sensor (Hardware Interrupt) to allow Deep Sleep.
        *   **Fallback:** Periodic Accelerometer Polling if hardware sensor is missing.
    *   **Battery Monitor:** BroadcastReceiver to trigger "Battery Safety Protocol" state changes.
*   **Output:** Writes raw `Location` objects to the local Room Database.
*   **Implements Requirements:** [Data Collection & Tracking](requirements/data_collection.md)

## 3. Reliability Layer (The Watchdog)
*   **Role:** Ensures the Tracker Service remains active despite OS aggression.
*   **Component:** `WorkManager` (PeriodicWorkRequest, 15-minute interval).
*   **Logic:**
    1.  Check if `TrackerService` is running.
    2.  If **Running**: Do nothing.
    3.  If **Stopped**: Attempt to restart the service immediately.
    4.  If **Restart Fails**: Trigger a "Tracking Stopped" notification to alert the user.

## 4. Sync Worker (The Uploader)
*   **Role:** Handles reliable data transport and storage management.
*   **Component:** `WorkManager` (PeriodicWorkRequest).
*   **Responsibilities:**
    *   **Streaming Uploads:** Stream data directly from the Room DB through a Gzip compressor to the Network socket to minimize RAM usage.
    *   **Buffer Management (FIFO):** Enforce a **500MB Soft Limit**. If exceeded, delete the *oldest* unsynced records to preserve storage for new data.
    *   **Transport:** Upload to S3 using the Runtime Keys.
    *   **Cleanup:** Delete local records only after a successful S3 response (`200 OK`).
*   **Implements Requirements:** [Data Storage & Management](requirements/data_storage.md)

## 5. Visualizer (The View)
*   **Role:** Provides the user interface for exploring history.
*   **Components:** `osmdroid` MapView, Local Cache (File System).
*   **Responsibilities:**
    *   **Lazy-Load Indexing:** Maintain a local index of available dates. Verify against S3 using Prefix Search (`tracks/YYYY/MM/`) only when the user requests a specific month.
    *   **Rendering:** Draw tracks on the map using Bitmap Tiles (OSMDroid), applying downsampling for performance.
    *   **Caching:** Store downloaded track files locally to support offline viewing.
*   **Implements Requirements:** [Visualization & History](requirements/visualization.md)

## 6. Local Data Persistence
*   **Role:** Intermediate buffer and state storage.
*   **Components:**
    *   **Room Database:** Stores pending location points and application logs.
    *   **EncryptedSharedPreferences:** Stores sensitive AWS credentials (Runtime Keys) and configuration (Device ID).
