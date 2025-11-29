# Visualization & History Requirements

## 3.1. Map Interface
*   **Rendering Engine:** The visualization engine must use **Bitmap Tiles** (via `osmdroid` or similar) to render the map.
*   **Offline Capability:** The system must implement an intelligent tile caching mechanism.
    *   **Primary Goal:** To improve rendering performance and reduce battery drain by avoiding repeated network calls for the same map tiles.
    *   **Secondary Goal:** To provide offline viewing capability for previously visited areas.
*   **Signal Quality:** The interface must include a user-toggleable overlay that visualizes the quality of the network signal (Signal Strength). This overlay must visually differentiate between signal sources (e.g., WiFi vs. Cellular) and signal levels (e.g., via a heat map or color coding).
*   **Performance & Optimization:** To ensure responsive rendering of large datasets, the system must apply geometric simplification (e.g., Ramer-Douglas-Peucker algorithm) to track data before drawing it on the map. This algorithm reduces the total number of points by removing redundant data along straight lines while preserving the visual shape of the track.
*   **Summary Statistics:** For any selected day, the interface must calculate and display summary statistics, including Total Distance, Total Duration, and Average Speed.
*   **Visual Discontinuity:** The map visualization must intentionally display a gap (no connecting line) between two sequential data points if the time difference between them exceeds 5 minutes, indicating a lack of continuous data.

## 3.2. History Retrieval
*   **Remote Verification:** The history view must source data exclusively from the remote storage (or a local cache of verified remote data) to confirm data sovereignty and successful upload. Local buffer data should not be mixed into the "History" view until it is uploaded.
*   **Lazy Loading:** The system must index the existence of historical data (e.g., which days have tracks) by querying S3 on-demand.
    *   **Mechanism:** When the user views a month:
        *   **Cache Hit:** If the month is fully in the past (e.g., viewing March in April) AND the local cache entry was created *after* the last day of that month, do *not* query S3.
        *   **Cache Miss/Refresh:** If the month is the *Current Month*, or the cache entry is missing, or the cache entry is stale (created before the month ended), issue a `ListObjects` request for that month prefix (`tracks/YYYY/MM/`).
*   **Data Merging:** The system must identify and merge track segments from multiple unique Device IDs that occurred on the same calendar day, presenting them as a unified history view to the user.
