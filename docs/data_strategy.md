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
  "sig": 3,
  "dbm": -85,
  "net": "cell"
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
| `sig` | Integer | Signal Level (0-4), as reported by Android `SignalStrength`. | Level (0-4) |
| `dbm` | Integer | Raw Signal Strength. | dBm |
| `net` | String | Network Type (e.g., "wifi", "cell", "none"). | N/A |

## Synchronization Strategy: Lazy Loading

To respect user data plans and battery life, Locus does not "sync" the entire history.

### 1. Inventory First
When connecting to an existing store, the app performs a lightweight scan of the S3 directory structure (`tracks/`).
*   **Goal:** Build a local index of *which days* have data.
*   **Mechanism:** Uses `ListObjects` with delimiters to identify Year/Month/Day prefixes.
*   **Result:** The History Calendar is populated with indicators. No track data is downloaded.

### 2. On-Demand Fetch
Full track data is only downloaded when the user explicitly interacts with a specific date.

## Identity & Write Patterns

### The "New Identity" Rule
Every fresh installation of Locus generates a **Unique Device ID** (e.g., `Pixel7_<RandomSuffix>`).
*   **Why:** To prevent "Split Brain" or overwrites. If a user factory resets their phone and reinstalls, the new installation acts as a distinct "writer" to the same "book" (Bucket).
*   **Merging:** The Visualization engine is responsible for merging tracks from multiple Device IDs that occur on the same day. The user sees a unified history.
