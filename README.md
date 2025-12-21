# Locus

**A Sovereign, High-Precision Location Tracker for Android.**

> **Status:** 🚧 **Planning & Architecture Phase** 🚧
>
> This project is currently in **Phase 0 (Foundation & Validation)**. We are establishing the architecture, validation pipelines, and project skeleton. See [Implementation Roadmap](docs/implementation_roadmap.md) for details.

## 🎯 Core Philosophy
*   **Data Sovereignty:** Your location history belongs to you. Data is stored directly in your own AWS S3 bucket. No third-party servers.
*   **High Precision:** Designed for 1Hz tracking to capture every turn, not just "significant changes."
*   **Battery Efficiency:** Smart batching and deep-sleep handling to ensure all-day battery life even with high-fidelity tracking.
*   **Offline-First:** Robust local buffering ensures no data is lost when connectivity drops.

## 🗺️ Roadmap
The development is divided into 8 distinct phases:
*   **Phase 0:** Foundation & Validation Infrastructure (Current)
*   **Phase 1:** Onboarding & Identity
*   **Phase 2:** Intelligent Tracking
*   **Phase 3:** Cloud Synchronization
*   **Phase 4:** Adaptive Battery Safety
*   **Phase 5:** System Status & Feedback
*   **Phase 6:** Historical Visualization
*   **Phase 7:** Service Reliability

See the full [Implementation Roadmap](docs/implementation_roadmap.md) for detailed steps.

## 📚 Documentation
The project is heavily documented to ensure robustness and maintainability.
*   **[PLANNING.md](PLANNING.md):** The central index for all documentation.
*   **[Technical Discovery](docs/technical_discovery/):** Deep dives into Architecture, Infrastructure, and Security.
*   **[Behavioral Specs](docs/behavioral_specs/):** Detailed functional specifications for each feature.

## 🛠 Architecture
*   **Client:** Native Android (Kotlin)
    *   **Architecture:** Clean Architecture (Domain/Data/UI separation)
    *   **Pattern:** MVVM / MVI
    *   **Tech Stack:** Compose, Room, WorkManager, Hilt, Coroutines
*   **Cloud:** AWS S3 (User Provided) + CloudFormation (for setup)
*   **Map:** OpenStreetMap (via osmdroid)

## ⚖️ License
GPLv3
