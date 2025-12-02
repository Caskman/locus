# Map (Visualization)

**Parent:** [UI & Presentation Specification](../ui_presentation_spec.md)

**Purpose:** Verify and explore historical movement data.

## 1. Layout Behavior
*   **Full Screen:** The map view occupies the entire screen behind transparent system bars.
*   **Overlays:** Controls and Action Buttons are anchored to the edges (safe area insets).
*   **Bottom Sheet:** A persistent sheet that peaks at the bottom (minimized height) and expands on drag or tap. It does *not* cover the whole map when minimized, only showing essential text.
    *   *Tablet Constraint:* On large screens, the Bottom Sheet must have a maximum width (e.g., `600dp`) and be centered horizontally to avoid excessive stretching.

## 2. Components
*   **Map View:** Full-screen `osmdroid` view.
    *   *Theme:* **Dark Mode Support:** The map tiles themselves must visually adapt to Dark Mode using a **Color Filter** (e.g., inversion or dimming matrix) applied to the MapView canvas when the system theme is Dark.
    *   *Performance:* **Downsampling:** The rendered path is visually simplified (e.g., Ramer-Douglas-Peucker) for performance; zooming in reveals more detail.
*   **Controls:**
    *   **Zoom Buttons (+/-):** Floating buttons anchored to the **Bottom Right**, just above the Bottom Sheet peek height.
    *   **Share/Snapshot:** Floating button anchored to the **Top Right**.
*   **Layer Switcher (Modal Bottom Sheet):**
    *   *Trigger:* FAB or Overlay Button.
    *   *Behavior:* Opens as a **Modal** Bottom Sheet (distinct from the persistent history sheet).
    *   *Content:* Radio selection for Map Type (Standard, Satellite), Toggles for Overlays (Heatmap).
*   **Empty State (No History):**
    *   If no data is recorded/selected, Map centers on user location. Bottom Sheet displays "No data recorded today."
*   **Empty State (Network Error):**
    *   If S3 Index cannot be fetched: Map centers on user. Bottom Sheet displays "Offline: Cannot fetch history." with a "Retry" text button.
*   **Bottom Sheet (Multi-Mode):**
    *   **Mode A (Day Summary):** Persistent summary of the selected day.
    *   **Loading State:** When fetching data, the top of the Bottom Sheet displays an indeterminate **Linear Progress Indicator**.
    *   **Mode B (Point Detail):** Displays details when a track point is tapped.
    *   **Dismissal:** Users can return to Mode A by tapping the map area, swiping the sheet down, or tapping the Close button.
    *   **Date Interaction:** The Date text is a clickable touch target that opens a **Custom Calendar Picker** (Modal Bottom Sheet).
        *   *Feature:* The Calendar must display **Data Indicators** (dots) on days that have verified historical data.
        *   *Loading State:* While fetching the "data dots" from the local database:
            *   Display an **Indeterminate Progress Indicator** (Spinner/Bar) over the calendar grid.
            *   **Disable** the "Previous Month" and "Next Month" controls to prevent rapid navigation/race conditions.
            *   Disable interaction with individual dates.
    *   **Accessibility:** Must have a clear Content Description (e.g., "Change Date, current is Oct 4").

## 3. Map Overlays
*   **Visual Discontinuity:** Track lines must break if the time gap > 5 minutes.
*   **Signal Quality:** When the "Heatmap" layer is active, the map displays a **True Heat Map Overlay** (gradient cloud) or a simplified line-style overlay.
    *   **Style:**
        *   **Cellular:** Solid Line (Colored by Strength).
        *   **WiFi:** Dashed/Dotted Line (Colored by Strength).
    *   **No Data:** Areas with *no* signal data must display a **Neutral Low-Gradient Cloud** (e.g., Gray mist) to visually distinguish "Unknown" from "Weak Signal" (Red) or "Strong Signal" (Green).

## 4. Wireframes

**ASCII Wireframe (Calendar Picker):**
```text
+--------------------------------------------------+
|  Select Date                                     |
|  ( Indeterminate Progress Bar if Loading... )    |
|                                                  |
|  < (Disabled)  October 2023  (Disabled) >        |
|  Su Mo Tu We Th Fr Sa                            |
|      1  2  3  4  5  6                            |
|                  .                               |
|   7  8  9 10 11 12 13                            |
|      .     .                                     |
|  ....................                            |
+--------------------------------------------------+
```

**ASCII Wireframe (Day Summary):**
```text
+--------------------------------------------------+
|                                [Share]  [Layers] |  <-- Action Overlays (Top Right)
|               ( Map Area )                       |
|         . . . . . . . . . . .                    |
|         .                   .                    |
|         .    (Track Line)   .                    |
|         .                   .                    |
|         . . . . . . . . . . .           [ + ]    |  <-- Zoom Buttons (Bottom Right)
|                                         [ - ]    |
+--------------------------------------------------+
|  [=== Loading... (Progress Indicator) ===]       |  <-- Linear Progress (if loading)
|  [ October 4, 2023 (v) ]                         |  <-- Clickable (Opens Data-Dot Calendar)
|  12.4 km  •  4h 20m  •  24 km/h avg              |
+--------------------------------------------------+
| [Dashboard]   [Map]      Logs      Settings      |
+--------------------------------------------------+
```

**ASCII Wireframe (Point Detail):**
*   *Note:* Fields with missing data (e.g., no Altitude or Signal info) must be **hidden completely** rather than displaying "N/A" or empty values.

```text
+--------------------------------------------------+
|               ( Map Area )                       |
|             (Selected Point O)                   |
+--------------------------------------------------+
|  [ X ] Close Detail                              |
|  14:02:15  •  35 km/h  •  Bat: 84%               |
|  Signal: WiFi (Level 3, -65 dBm)                 |
|  Altitude: 450m                                  |
+--------------------------------------------------+
```

**ASCII Wireframe (Network Error):**
```text
+--------------------------------------------------+
|                                                  |
|               ( Map Area )                       |
|                                                  |
+--------------------------------------------------+
|  Offline: Cannot fetch history index.            |
|               [ RETRY ]                          |
+--------------------------------------------------+
```
