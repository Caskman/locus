# Android Services Rules

*   **Foreground Services:** Use Foreground Services for long-running operations that users must be aware of, such as location tracking.
*   **Battery Safety:** Implement a multi-stage battery safety protocol. Reduce recording interval to 10s when battery is < 10%, and to 60s when < 3%. Pause data uploads when < 10%. Resume normal operation when > 15%.
*   **Notification Channels:** Categorize notifications into appropriate channels to give users control over interruptions.
*   **WorkManager:** Utilize WorkManager for deferrable and guaranteed background tasks.
*   **Resource Management:** Release resources such as location listeners and sensors immediately when they are no longer needed.
