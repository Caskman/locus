# Data Storage & Management Requirements

## 2.1. Local Buffering
*   **Reliability:** The system must store captured data in a persistent local buffer immediately upon capture to prevent data loss in case of application termination or power failure.
*   **Retention:** Data must remain in the local buffer until it is successfully confirmed as stored in the remote destination.

## 2.2. Remote Synchronization
*   **User Ownership:** The system must transmit data exclusively to a storage repository owned and controlled by the user (e.g., a personal AWS S3 Bucket).
*   **Periodic Sync:** The system must attempt to batch and upload buffered data at regular intervals (e.g., every 15 minutes).
*   **Manual Sync:** The system must provide a mechanism for the user to force an immediate upload. This action must override non-critical battery safety checks.
*   **Immutability:** The system must configure the remote storage to prevent the modification or deletion of uploaded history files (e.g., using Object Lock/Compliance Mode).

## 2.3. Data Format
*   **Interoperability:** Data must be stored in a standard, open, text-based format (NDJSON) to ensure future readability.
*   **Efficiency:** Data bundles must be compressed (e.g., Gzip) before transmission to minimize network usage and storage costs.
