# Behavioral Specification: Adaptive Battery Safety

**Bounded Context:** This specification acts as a "Governor" layer, overriding the standard behaviors of Tracking and Synchronization based on the device's battery resource state.

**Prerequisite:** None (Logic layer).
**Downstream:** Governs **[Intelligent Tracking](02_intelligent_tracking.md)** and **[Cloud Synchronization](03_cloud_synchronization.md)**.

---

## 1. Low Battery State (< 10%)
*   **While** the battery capacity is less than 10% (and greater than 3%), the system **shall** enter "Low Power Mode".
*   **While** in Low Power Mode, the system **shall** reduce the GPS recording frequency to a fixed interval of 10 seconds (overriding the standard 1Hz).
*   **While** in Low Power Mode, the system **shall** pause all automatic background synchronization jobs.
*   **When** a Manual Sync is requested during Low Power Mode, the system **shall** permit the upload.

## 2. Critical Battery State (< 3%)
*   **While** the battery capacity is less than 3%, the system **shall** enter "Deep Sleep Mode".
*   **While** in Deep Sleep Mode, the system **shall** cease all GPS acquisition.
*   **While** in Deep Sleep Mode, the system **shall** release all Partial Wake Locks to allow the CPU to sleep.
*   **When** a Manual Sync is requested during Deep Sleep Mode, the system **shall** reject the request and display a "Battery Critical" error.

## 3. Recovery Logic
*   **When** the battery capacity rises above 15% (Hysteresis Threshold), the system **shall** exit Low Power/Deep Sleep modes and resume standard recording (1Hz) and upload schedules.
*   **When** the device is connected to an external power source (Charging), the system **shall** immediately exit any power-saving modes and resume standard operation, regardless of the percentage level.
