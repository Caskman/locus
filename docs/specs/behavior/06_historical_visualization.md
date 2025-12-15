# Behavioral Specification: Historical Visualization

**Bounded Context:** This specification governs the consumption side of the system: retrieving, caching, and rendering the data that has been successfully collected and synced.

**Prerequisite:** Depends on **[Cloud Synchronization](03_cloud_synchronization.md)** (data availability).
**Downstream:** None (End of chain).

---

## 1. Data Retrieval & Caching
*   **When** the user opens the History/Map view, the system **shall** query the local File-Based Cache for track data.
*   **When** viewing a specific month, the system **shall** check the "Staleness" of the local index for that month.
*   **If** the target month is the "Current Month", **then** the system **shall** always query S3 (ListObjects) to update the index.
*   **If** the target month is a "Past Month" AND the local index is missing or stale (cached before month end), **then** the system **shall** query S3 to update the index.
*   **If** the target month is a "Past Month" AND the local index is fresh, **then** the system **shall** use the local index without querying S3.
*   **When** fetching track data, the system **shall** download the Gzipped NDJSON files from S3 and store them in the local File-Based Cache.

## 2. Visualization Logic
*   **When** rendering tracks, the system **shall** strictly display only data that has been successfully synced to S3 (or cached from S3), excluding data currently in the local upload buffer.
*   **When** multiple devices have uploaded data for the same date, the system **shall** merge these distinct track segments into a unified view.
*   **When** rendering a track, the system **shall** apply a downsampling algorithm (e.g., Ramer-Douglas-Peucker) to reduce point count while preserving geometry.
*   **If** the time difference between two sequential points exceeds 5 minutes, **then** the system **shall** render a "Clean Break" (gap) in the line to indicate discontinuity.

## 3. Map Interaction & Overlays
*   **When** the map is displayed, the system **shall** render Bitmap Tiles using the user's preferred theme (Light/Dark/System) via a Color Filter.
*   **When** the user toggles the "Signal Quality" layer, the system **shall** overlay a heatmap visualizing network strength, distinguishing between WiFi and Cellular sources.
*   **When** the user selects a specific point on the track, the system **shall** display a Detail Sheet with raw data (Speed, Altitude, Coordinates).
*   **If** the device is offline and cached tiles are unavailable, **then** the system **shall** display a "Map Offline" warning but allow interaction with the cached track geometry.
