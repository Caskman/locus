# Setup & Onboarding Requirements

## 4.1. Infrastructure Provisioning
*   **Automated Setup:** The system must provide a mechanism to automatically provision the necessary remote infrastructure (e.g., storage buckets, permissions) using user-supplied credentials.
*   **Idempotency:** The provisioning process must handle re-runs gracefully, checking for existing resources to avoid duplication or errors.

## 4.2. Identity Management
*   **Unique Device ID:** The system must generate a new, unique identifier for every installation to prevent data collisions ("Split Brain") when multiple devices (or re-installs) write to the same storage.
*   **Credential Handling:** The system must support the use of temporary, high-privilege credentials for the initial setup (Bootstrap) and switch to restricted, low-privilege credentials for ongoing operation (Runtime).
