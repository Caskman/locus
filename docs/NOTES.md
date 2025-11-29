# Project Notes & Analysis

## 1. Multi-Bucket Visualization & Access Levels
*   **Requirement:** Enable the application to view data from multiple Locus buckets within the same AWS account ("Admin" mode), while restricting standard operation to the single bucket used for data recording ("Normal" mode).
*   **Feasibility:** High. AWS IAM supports resource-based policies and user-based policies that can be scoped to specific buckets or wildcard patterns (e.g., `arn:aws:s3:::locus-*`).
*   **Best Practice:** **Principle of Least Privilege**. The "Normal" user (or the runtime credentials on the device) should only have write access to their specific bucket and read access for history. "Admin" access should be a separate, deliberate elevation of privilege, ideally using temporary credentials.
*   **Recommendation:**
    *   Define two distinct IAM Policies: `LocusRecorderPolicy` (Write to specific bucket) and `LocusViewerPolicy` (Read from `locus-*`).
    *   During the Bootstrap/Onboarding phase, allow the user to choose their intent.
    *   If "Admin" is selected, the app requests `s3:ListBuckets` permission to discover all `locus-` buckets.
    *   Ensure the "Admin" feature is gated behind the initial setup (Bootstrap) to prevent accidental runtime scope expansion.

## 2. APK Build Documentation & Scripting
*   **Requirement:** Document the process for building the APK from source and provide a script to automate it.
*   **Feasibility:** High. Standard Android projects builds via Gradle.
*   **Best Practice:** **Reproducible Builds**. The build process should not depend on the developer's specific machine environment. Use of a wrapper (`gradlew`) is standard.
*   **Recommendation:**
    *   Create a `BUILDING.md` file.
    *   Provide a shell script `scripts/build_release.sh` that wraps `./gradlew assembleRelease`.
    *   Include instructions for setting up the Java Development Kit (JDK) and Android SDK, or provide a Docker container definition for a standardized build environment.

## 3. Concurrency & Multi-Device Data Conflicts
*   **Requirement:** Prevent data corruption or confusing user states when multiple devices write to the same bucket.
*   **Feasibility:** High. The S3 object key schema is the primary mechanism for collision avoidance.
*   **Best Practice:** **Partitioning**. Data should be strictly namespaced by the source (Device ID).
*   **Recommendation:**
    *   Adhere strictly to the schema: `tracks/YYYY/MM/DD/<device_id>_<start_timestamp>_v<version>.json.gz`.
    *   Since `<device_id>` is part of the S3 key, two devices cannot overwrite each other's files unless they share the same ID (which should be prevented during setup).
    *   The visualization layer must handle merging tracks from multiple device IDs if they are displayed simultaneously, or allow filtering by Device ID.

## 4. Cache Eviction Policies
*   **Requirement:** Prevent downloaded track data from consuming excessive device storage.
*   **Feasibility:** High. Android provides APIs for checking available disk space and managing cache directories.
*   **Best Practice:** **LRU (Least Recently Used)** eviction. When the cache hits a size limit (e.g., 500MB) or a time limit (e.g., data older than 90 days), delete the oldest accessed files first.
*   **Recommendation:**
    *   Implement a cache manager that runs periodically (via `WorkManager`).
    *   Maintain a local index (Room DB) of downloaded files with a `last_accessed` timestamp.
    *   Set a hard configurable limit (default 500MB).
    *   Ensure "Stationary" or "Buffer" data (not yet uploaded) is never evicted.

## 5. IAM Strategy Verification
*   **Requirement:** Verify that the project's IAM permissions adhere to AWS best practices and strictly defined security requirements.
*   **Feasibility:** Medium. Requires deep knowledge of AWS IAM nuances.
*   **Best Practice:** **Policy Simulation & Analysis**. Use tools like AWS Access Analyzer or `pmapper` to visualize access paths.
*   **Recommendation:**
    *   Audit current policy templates against the "Least Privilege" rule.
    *   Specifically check for `*` actions on `*` resources (e.g., `s3:*` on `*`).
    *   Document the justification for every permission granted in `docs/infrastructure.md`.

## 6. Manual IAM Credential Creation (No CloudFormation)
*   **Requirement:** Update documentation to support a setup flow that does not rely on CloudFormation, possibly for users who prefer manual control or have restricted permissions.
*   **Feasibility:** High. All AWS resources can be created manually or via CLI.
*   **Best Practice:** **Transparency**. While IaC (Infrastructure as Code) like CloudFormation is preferred for reliability, providing a manual "break-glass" procedure increases trust and accessibility.
*   **Recommendation:**
    *   Create a "Manual Setup Guide" in `docs/manual_setup.md`.
    *   List exact JSON policy documents required.
    *   Provide AWS CLI commands as an alternative to Console screenshots (CLI is less likely to become outdated visually).

## 7. User-Configurable Recording Frequency
*   **Requirement:** Allow users to adjust how often the app records location points.
*   **Feasibility:** High. The Android `LocationRequest` API allows setting intervals.
*   **Best Practice:** **User Choice with Sensible Defaults**. Provide presets (e.g., "High Accuracy" (1s), "Balanced" (10s), "Power Saver" (60s)) rather than raw millisecond inputs.
*   **Recommendation:**
    *   Add a Settings screen for "Tracking Preferences".
    *   Update the `ForegroundService` to restart the location listener when preferences change.
    *   Warn the user about battery impact for high-frequency settings.

## 8. Independence from Google Play Services
*   **Requirement:** Ensure functionality on de-Googled devices (e.g., GrapheneOS, LineageOS).
*   **Feasibility:** High.
*   **Best Practice:** **Standard Standards**. Use `android.location.LocationManager` (Platform API) instead of `com.google.android.gms.location.FusedLocationProviderClient`. Use `osmdroid` for maps instead of Google Maps SDK.
*   **Recommendation:**
    *   Avoid any dependency on `com.google.android.gms`.
    *   Test on an Android Virtual Device (AVD) image that does not have Google APIs installed ("AOSP" image).
    *   Use OpenStreetMap (OSM) for all visualization.

## 9. Silent Failure Detection
*   **Requirement:** Detect and alert if tracking stops unexpectedly (e.g., due to OS kill or crash) without user knowledge.
*   **Feasibility:** Medium. The OS can be aggressive with background services.
*   **Best Practice:** **Watchdog / Heartbeat**. A separate component should periodically check if the primary service is running/updating.
*   **Recommendation:**
    *   Use `WorkManager` for a periodic "Health Check" task (e.g., every 15 minutes).
    *   Check the timestamp of the last recorded location in the database.
    *   If the gap exceeds a threshold (e.g., 20 minutes) and the user didn't manually stop tracking, trigger a high-priority notification: "Tracking appears to have stopped."

## 10. README & Documentation Updates
*   **Requirement:** Keep the README current with project status.
*   **Feasibility:** High.
*   **Best Practice:** **Living Documentation**. The README should be the "Entry Point" and link to specific docs.
*   **Recommendation:**
    *   Update `README.md` to link to this `NOTES.md`.
    *   Ensure the "Getting Started" section reflects the current "Implementation Definition" phase.

## 11. Lambda Compression for Daily Objects
*   **Requirement:** Compress multiple small JSON objects for a single day into a single Gzipped file to save storage and reduce GET request costs/latency.
*   **Feasibility:** High. Common S3 pattern.
*   **Best Practice:** **Event-Driven Compute**. Trigger a Lambda function on a schedule (e.g., daily at 02:00 UTC) or on object creation (complex for batching). Scheduled is better for daily aggregation.
*   **Recommendation:**
    *   Deploy a Python/Go Lambda function.
    *   Script: List objects for `tracks/YYYY/MM/DD-1/`. Download, concatenate NDJSON, GZIP, Upload `tracks/YYYY/MM/DD-1/archive.json.gz`. Delete originals (optional/careful).
    *   **Caution:** Deleting originals is risky. Maybe move them to a `glacier` class or just keep the archive as an "optimized view".
