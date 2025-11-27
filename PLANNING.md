# Architecture: User-Owned Precision Tracker

## 1. Core Philosophy
*   **Data Sovereignty:** User owns the S3 bucket and keys.
*   **Precision:** 1Hz (1/second) tracking, 24/7.
*   **Efficiency:** Batched uploads, Gzip compression, Battery safety protocols.

## 2. Infrastructure & Security
*   **Storage:** AWS S3 (User Owned).
*   **Authentication:**
    *   **Setup:** User creates IAM User with "S3 Only" policy.
    *   **Runtime:** App uses these keys to check/create its own bucket.
*   **Encryption:** Standard AWS S3 Server-Side Encryption (SSE-S3).
*   **Monitoring:** Firebase Crashlytics & Analytics.

## 3. Data Strategy
### A. Storage Format (S3)
*   **Format:** NDJSON (Newline Delimited JSON), Gzipped.
*   **Compression:** `.gz` (Expected ~90% size reduction).
*   **Path Structure:**
    `s3://<bucket_name>/tracks/YYYY/MM/DD/<device_id>_<start_timestamp>_v<version>.json.gz`

### B. Schema Versioning
*   **Current Version:** `v1`
*   **Strategy:** The filename includes the version (`_v1`). The JSON payload also includes a header object.

### C. JSON Schema (v1)
Each file is a Gzipped text file.
**Line 1 (Header):**
```json
{"type": "header", "version": 1, "device_id": "Pixel7_a8f3", "start_time": 1698300000}
```
**Lines 2..N (Data):**
```json
{"t": 1698300001, "lat": 37.7749, "lon": -122.4194, "acc": 4.5, "alt": 120, "spd": 1.2}
```

## 4. Android Client Architecture
### A. Service Layer (The Engine)
*   **Component:** `ForegroundService`.
*   **Behavior:**
    *   Acquires `PARTIAL_WAKE_LOCK`.
    *   Requests Location Updates (GPS Provider, minTime=1000ms).
    *   **Battery Safety:** Automatically stops tracking if battery drops below **10%**.

### B. Visualization
*   **Provider:** **OpenStreetMap (via `osmdroid`)**.
*   **Benefits:** No API keys required, open source, offline caching capability.

### C. Persistence Layer (The Buffer)
*   **Local DB:** Room (SQLite).
*   **Purge Strategy:** Rows are deleted only after successful S3 upload confirmation.

### D. Sync Worker
*   **Trigger:** Periodic (e.g., every 15 mins).
*   **Action:**
    1.  Query oldest points.
    2.  Gzip compress.
    3.  Upload to S3.
    4.  On Success -> Delete points from DB.

## 5. Deployment
*   **Distribution:** Manual APK Build (User builds from source).
*   **Permissions:** Critical UX flow to request `ACCESS_BACKGROUND_LOCATION` ("Allow all the time").

## 6. Cost Projections
*   **S3 Storage:** <$0.10/year (Compressed).
*   **Request Costs:** ~$0.02/month (Batched).
