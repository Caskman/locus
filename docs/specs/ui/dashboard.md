# Dashboard (Home)

**Parent:** [UI & Presentation Specification](../ui_presentation_spec.md)

**Purpose:** Provide an "at-a-glance" view of system health and allow manual overrides.

## 1. Layout Behavior
*   **Phone (Portrait):** Scrollable Column. The **Status Card** and **Action Button** scroll with the content (not pinned). The "Recent Activity" list appears at the bottom.
*   **Phone (Landscape):** Scrollable Column (Standard).
*   **Tablet/Large Screen (Landscape > 600dp):** Two-pane layout.
    *   **Left Pane (Fixed Width):** Status Card and **"Sync Now" Action Button**. This acts as the control panel.
    *   **Right Pane (Scrollable):** Stats Grid and Recent Activity history. The Stats Grid scrolls **with** the content (not pinned).

## 2. Components
*   **Skeleton Loader (Initial State):** When the Dashboard first loads (before local DB query completes), all text values in the Status Card and Stats Grid must display a **Shimmer/Skeleton** placeholder effect to indicate loading.
*   **Status Card:** A prominent card mirroring the Persistent Notification state. Handles "Active", "Error", "Paused", and "User Stopped" states.
*   **Stats Grid:** "Local Buffer" count, "Last Sync" time, "Next Sync" estimate.
*   **Actions:** "Sync Now" button.
    *   *Placement:* On phones, this button is placed **below** the Stats Grid (scrolling). On tablets, it is fixed in the Left Pane.
    *   *Behavior:* When tapped, transforms into a **Linear Progress Indicator** showing "Uploading batch X of Y..." until completion.
    *   *Error Handling:* Transient failures (e.g., "Network Error") must revert the button state and appear as a **Snackbar** anchored above the bottom navigation.
*   **Sensor Status:** Small indicators for GPS, Network, and Battery state.
    *   *Design:* These must use an **Icon + Short Value** format (e.g., [Icon] High, [Icon] 85%) and leverage dynamic **color and icon changes** (e.g., Green Check, Red Alert, Grey Slash) to indicate state.
*   **Recent Activity:** A simple list showing the last few days of tracking summary (e.g., "Yesterday: 14km").
    *   *Empty State:* If no activity is found (0 records), display a centered "No recent activity recorded" message with a generic illustration/icon.

## 3. Wireframes

**ASCII Wireframe (Active - Phone Portrait):**
```text
+--------------------------------------------------+
|  [ STATUS CARD ]                                 |
|  Status: Recording (High Accuracy)               |
|  State:  Synced                                  |
|  ----------------------------------------------  |
|  [ (Sat) High ]  [ (Bat) 85% ]  [ (Wifi) On ]    | <-- Icon + Text, Colored by State
+--------------------------------------------------+
|                                                  |
|   +----------------+      +----------------+     |
|   |  1,240         |      |  5 mins ago    |     |
|   |  Buffered Pts  |      |  Next: ~10m    |     |
|   +----------------+      +----------------+     |
|                                                  |
+--------------------------------------------------+
|                                                  |
|           [  SYNC NOW (Cloud Icon)  ]            |  <-- Primary Action (Filled Tonal)
|      (Becomes: [=== 50% ===] Batch 1/2)          |
|                                                  |
+--------------------------------------------------+
|  Recent Activity                                 |
|  - Yesterday: 14km                               |
|  - Oct 4: 12km                                   |
|                                                  |
|  (Scrolls...)                                    |
+--------------------------------------------------+
| [Dashboard]    Map       Logs      Settings      |  <-- Bottom Nav
+--------------------------------------------------+
```

**ASCII Wireframe (Active - Tablet Landscape):**
```text
+------------------------------------+------------------------------------+
|  [ STATUS CARD ]                   |  Buffered: 1,240                   |
|  Status: Recording                 |  Last Sync: 5 mins ago             |
|  State:  Synced                    |                                    |
|  --------------------------------  |                                    |
|  [GPS] [Bat] [Wifi]                |  Recent Activity                   |
|  --------------------------------  |  - Yesterday: 14km                 |
|  [ SYNC NOW (Cloud Icon) ]         |  - Oct 4: 12km                     |
|                                    |                                    |
|  (This pane fixed height/width)    |                                    |
|                                    |                                    |
+------------------------------------+------------------------------------+
| [Dashboard]    Map        Logs       Settings                           |
+-------------------------------------------------------------------------+
```

**Status Card (Paused - Tier 2 Environmental):**
```text
+--------------------------------------------------+
|  [ STATUS CARD ] (Yellow/Warning Background)     |
|  Status: Paused (Low Battery)                    |
|  State:  Idle                                    |
|  ----------------------------------------------  |
|  Recording paused to save battery (<15%).        |
|  Will resume automatically when charged.         |
|                                                  |
|  (No Action Button - Passive State)              |
+--------------------------------------------------+
```

**Status Card (User Stopped):**
```text
+--------------------------------------------------+
|  [ STATUS CARD ] (Yellow/Grey Background)        |
|  Status: Stopped by User                         |
|  State:  Idle                                    |
|  ----------------------------------------------  |
|  Tracking paused. Tap to resume.                 |
|                                                  |
|           [ RESUME TRACKING ]                    |
+--------------------------------------------------+
```

**Status Card (Error State - Tier 3 Fatal):**
```text
+--------------------------------------------------+
|  [ STATUS CARD ] (Red Background)                |
|  Status: Service Halted                          |
|  Error:  Permission Revoked                      |
|  ----------------------------------------------  |
|  Locus requires "Always Allow" location access   |
|  to function.                                    |
|                                                  |
|  [ FIX ISSUE (Opens Settings) ]                  |
+--------------------------------------------------+
```
