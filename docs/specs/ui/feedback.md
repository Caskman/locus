# Feedback Mechanisms

**Parent:** [UI & Presentation Specification](../ui_presentation_spec.md)

**Related Requirements:** [UI Feedback](../../requirements/ui_feedback.md)

## 1. Persistent Notification
**Purpose:** Keep the service alive and provide status without opening the app.
**Format:** `[Recording Status] • [Sync Status]`

**ASCII Wireframe:**
```text
+--------------------------------------------------+
|  (Locus Icon)  Locus • Recording                 |
|  Tracking (High Accuracy) • Synced               |
|  [ STOP TRACKING ]                               |  <-- Action Button
+--------------------------------------------------+
```

## 2. In-App Feedback
*   **Toast:** Used only for simple confirmations (e.g., "Sync Complete").
*   **Snackbar:** Used for transient warnings or actionable info (e.g., "Network Timeout - Retrying... [Retry Now]").
*   **Blocking Full-Screen Error:** Reserved for **Tier 3 Fatal Errors** (e.g., Permission Revoked) where the app cannot function.
    *   *Behavior:* This screen appears **only** when the user opens the application (or brings it to the foreground). It does **not** overlay other apps or appear over the lock screen.
    *   *Rationale UI:* Any runtime permission re-request flows must utilize the **Rationale UI** designs defined in the [Onboarding UI Specification](../ui_onboarding_spec.md) to ensure consistent education.
*   **Dialogs:** Reserved strictly for destructive confirmations (e.g., "Delete History").

**Blocking Error Screen (Wireframe):**
```text
+--------------------------------------------------+
|                                                  |
|              ( Alert Icon )                      |
|                                                  |
|            Action Required                       |
|                                                  |
|      Background Location Permission              |
|      has been revoked.                           |
|                                                  |
|      Locus cannot record tracks without it.      |
|                                                  |
+--------------------------------------------------+
|            [ OPEN SETTINGS ]                     |
+--------------------------------------------------+
```

**Stop Tracking Confirmation (Wireframe):**
```text
+--------------------------------------------------+
|  Stop Tracking?                                  |
|                                                  |
|  Location recording will cease.                  |
|  The app will enter a "User Stopped" state.      |
|                                                  |
|  (Note: You can resume anytime from the          |
|   Dashboard).                                    |
|                                                  |
|      [ CANCEL ]       [ STOP TRACKING ]          |
+--------------------------------------------------+
```

**Clear Buffer Confirmation (Wireframe):**
```text
+--------------------------------------------------+
|  Delete Unsynced Data?                           |
|                                                  |
|  You are about to delete 1,240 points from       |
|  the local device buffer.                        |
|                                                  |
|  This data has NOT been uploaded to S3 yet.      |
|  This action cannot be undone.                   |
|                                                  |
|      [ CANCEL ]       [ DELETE PERMANENTLY ]     |
+--------------------------------------------------+
```
