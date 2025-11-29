# Data Strategy

## Storage Format (S3)
*   **Format:** NDJSON (Newline Delimited JSON), Gzipped.
*   **Compression:** `.gz` (Expected ~90% size reduction).
*   **Path Structure:**
    `s3://<bucket_name>/tracks/YYYY/MM/DD/<filename>`

## Schema Versioning
*   **Current Version:** `v1`
*   **Strategy:** The filename includes the version (`_v1`). The JSON payload also includes a header object.

## JSON Schema (v1)
Each file is a Gzipped text file.

**Filename Format:** `<device_id>_<start_timestamp>_v<version>.json.gz`
*   **Example:** `Pixel7_a8f3_1698300000_v1.json.gz`

**Line 1 (Header):**
```json
{
  "type": "header",
  "version": 1,
  "device_id": "Pixel7_a8f3",
  "start_time": 1698300000
}
```

**Lines 2..N (Data):**
```json
{
  "t": 1698300001,
  "lat": 37.7749,
  "lon": -122.4194,
  "acc": 4.5,
  "alt": 120,
  "spd": 1.2,
  "bat": 94,
  "cs": 3,
  "cd": -85,
  "ws": 2,
  "wd": -60
}
```

## Schema Definition
| Key | Type | Description | Unit |
| :--- | :--- | :--- | :--- |
| `t` | Number (Long) | Unix Timestamp (Epoch seconds). | Seconds |
| `lat` | Number (Double) | Latitude (WGS84). | Degrees |
| `lon` | Number (Double) | Longitude (WGS84). | Degrees |
| `acc` | Number (Float) | Horizontal Accuracy (Radius of 68% confidence). | Meters |
| `alt` | Number (Double) | Altitude above WGS84 ellipsoid. | Meters |
| `spd` | Number (Float) | Speed over ground. | m/s |
| `bat` | Integer | Battery Level (0-100). | % |
| `cs` | Integer | Cellular Signal Level (0-4). Optional. | Level (0-4) |
| `cd` | Integer | Cellular Signal Strength (dBm). Optional. | dBm |
| `ws` | Integer | WiFi Signal Level (0-4). Optional. | Level (0-4) |
| `wd` | Integer | WiFi Signal Strength (RSSI). Optional. | dBm |
| `ax` | Number (Float) | Accelerometer X (m/s²). Conditional (>4.5m/s). | m/s² |
| `ay` | Number (Float) | Accelerometer Y (m/s²). Conditional (>4.5m/s). | m/s² |
| `az` | Number (Float) | Accelerometer Z (m/s²). Conditional (>4.5m/s). | m/s² |
| `mx` | Number (Float) | Magnetometer X (µT). Conditional (>4.5m/s). | µT |
| `my` | Number (Float) | Magnetometer Y (µT). Conditional (>4.5m/s). | µT |
| `mz` | Number (Float) | Magnetometer Z (µT). Conditional (>4.5m/s). | µT |
| `bar` | Number (Float) | Barometer (Pressure). Conditional (>4.5m/s). | hPa |

## Synchronization Strategy: Lazy Loading

To respect user data plans and battery life, Locus does not "sync" the entire history.

### 1. Lazy-Load Indexing
The application builds its knowledge of history *on demand* rather than maintaining a strict local database of all remote files.
*   **Mechanism:** When the user navigates to a specific Month in the UI, the app performs an S3 Prefix Search (`ListObjects` with prefix `tracks/YYYY/MM/`) to identify which Days have data.
*   **Caching:** This index data is cached locally to prevent redundant network calls, but S3 remains the Source of Truth.

### 2. On-Demand Fetch
Full track data is only downloaded when the user explicitly interacts with a specific date.

## Retention Strategy
*   **Local Buffer:**
    *   **FIFO Protocol:** A 500MB soft limit is enforced. If the buffer is full, the oldest *unsynced* records are deleted to make room for new data.
    *   **Cleanup:** Synced records are deleted from the local buffer only after successful S3 upload verification (`200 OK`).
*   **Remote Storage:** Indefinite (100 Years). S3 Object Lock is enabled in Governance Mode to prevent accidental deletion while allowing administrative recovery if absolutely necessary.

## Identity & Write Patterns

### The "New Identity" Rule
Every fresh installation of Locus generates a **Unique Device ID** (e.g., `Pixel7_<RandomSuffix>`).
*   **Why:** To prevent "Split Brain" or overwrites. If a user factory resets their phone and reinstalls, the new installation acts as a distinct "writer" to the same "book" (Bucket).
*   **Merging:** The Visualization engine is responsible for merging tracks from multiple Device IDs that occur on the same day. The user sees a unified history.
