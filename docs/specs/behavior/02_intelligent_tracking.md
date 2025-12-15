# Behavioral Specification: Intelligent Tracking

**Bounded Context:** This specification governs the core data production behavior, including GPS acquisition, sensor fusion logic, moving/stationary state transitions, and hardware wake-up triggers.

**Prerequisite:** Depends on **[Onboarding & Identity](01_onboarding_identity.md)** (for valid session context).
**Downstream:** Produces data for **[Cloud Synchronization](03_cloud_synchronization.md)** and **[Historical Visualization](06_historical_visualization.md)**.
**Constraint:** Subject to overrides by **[Adaptive Battery Safety](04_adaptive_battery_safety.md)**.

---

## 1. Location Acquisition
*   **While** the tracking service is active, the system **shall** record geospatial location data (Latitude, Longitude, Altitude) at a default frequency of 1Hz.
*   **While** the tracking service is active, the system **shall** prioritize the Fused Location Provider (Google Play Services) where available.
*   **If** the Fused Location Provider is unavailable (e.g., FOSS flavor, Missing Play Services), **then** the system **shall** fall back to the raw GPS Location Manager.
*   **While** the application is in the background or the device is sleeping, the system **shall** maintain continuous data collection via a Foreground Service with a Partial Wake Lock.
*   **While** the OS "Battery Saver" mode is active, the system **shall** ignore OS throttling suggestions and continue standard data collection operations.

## 2. Sensor Fusion & Enrichment
*   **When** recording a location point, the system **shall** simultaneously record network quality metrics (Signal Level 0-4 and Raw dBm) for both Cellular and WiFi interfaces, if available.
*   **When** the device speed exceeds 4.5 m/s, the system **shall** include auxiliary environmental sensor data (accelerometer, magnetometer, barometer) in the recorded point.
*   **While** the device speed is below 4.5 m/s, the system **shall** omit auxiliary sensor data to conserve storage and processing power.

## 3. Stationary Logic (Optimization)
*   **When** no significant movement is detected for a continuous duration of 2 minutes, the system **shall** enter "Stationary Mode".
*   **While** in Stationary Mode, the system **shall** suspend active GPS acquisition.
*   **While** in Stationary Mode, the system **shall** register a hardware-backed Significant Motion trigger (or equivalent low-power sensor interrupt).
*   **When** the Significant Motion trigger fires, the system **shall** immediately exit Stationary Mode and resume standard GPS acquisition.
*   **If** hardware-backed Significant Motion is unavailable, **then** the system **shall** utilize periodic accelerometer polling (Periodic Burst) to detect movement.
