# Behavioral Specification: Service Reliability (Watchdog)

**Bounded Context:** This specification governs the "Immune System" of the application, responsible for self-monitoring, detecting crashes or deadlocks, and autonomously recovering the service or alerting the user.

**Prerequisite:** Depends on **[Intelligent Tracking](02_intelligent_tracking.md)** (the system being watched).
**Downstream:** Triggers **[System Status & Feedback](05_system_status_feedback.md)** (for fatal errors).

---

## 1. Passive Heartbeat Monitoring
*   **While** the Tracking Service is in the `RECORDING` state, it **shall** write a timestamp to a lightweight persistence layer (e.g., SharedPreferences) every 15 minutes.
*   **When** writing the heartbeat timestamp, the service **shall** use standard `Context.MODE_PRIVATE` storage (not Encrypted) to ensure high availability and performance.
*   **While** the application is installed, a background `WatchdogWorker` **shall** execute periodically (e.g., every 15 minutes).
*   **When** the Watchdog executes, it **shall** check the age of the last heartbeat timestamp.
*   **If** the tracking state is `RECORDING` AND the last heartbeat is older than 45 minutes, **then** the Watchdog **shall** declare the service "Unresponsive" and initiate recovery.

## 2. Autonomous Recovery (Self-Healing)
*   **When** the service is declared Unresponsive, the Watchdog **shall** attempt to restart the service (`stopService` followed by `startService`).
*   **When** attempting a restart on Android 12+, the Watchdog **shall** try to launch the Foreground Service.
*   **If** the restart fails due to Android 12+ Background Start Restrictions (`ForegroundServiceStartNotAllowedException`), **then** the Watchdog **shall** post a High Priority Notification requiring manual user intervention ("Resume Tracking").

## 3. Circuit Breaker (Anti-Loop)
*   **When** the Watchdog successfully detects a fresh heartbeat (service running > 15 mins), it **shall** reset the `ConsecutiveRestartCount` to 0.
*   **When** the Watchdog attempts a recovery restart, it **shall** increment the `ConsecutiveRestartCount`.
*   **If** the `ConsecutiveRestartCount` reaches 3, **then** the Watchdog **shall** "Trip the Circuit Breaker" and cease all further automatic restart attempts.
*   **When** the Circuit Breaker trips, the system **shall** trigger a Tier 3 Fatal Error notification ("Service Unstable") requiring user action.
*   **When** the user manually opens the app or toggles tracking, the system **shall** reset the Circuit Breaker.

## 4. Invariant Checks
*   **When** the Watchdog executes, it **shall** verify that the `ACCESS_BACKGROUND_LOCATION` permission is still granted.
*   **If** the permission is revoked while `RECORDING`, **then** the Watchdog **shall** stop the service and trigger a Tier 3 Fatal Error ("Permission Revoked").
*   **When** the Watchdog executes, it **shall** check for "Upload Stuck" conditions (Buffer > 4 hours old, Network Connected, Battery > 15%).
*   **If** an Upload Stuck condition is detected, **then** the Watchdog **shall** trigger an immediate synchronization attempt (Strict Profile).
