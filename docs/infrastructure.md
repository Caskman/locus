# Infrastructure & Security

## Infrastructure

### Storage
*   **Storage:** AWS S3 (User Owned).
*   **Authentication:**
    *   **Setup:** User creates IAM User with a bootstrap policy.
    *   **Runtime:** App uses these keys to deploy CloudFormation and write/read data.
*   **Encryption:** Standard AWS S3 Server-Side Encryption (SSE-S3).
*   **Immutability:** S3 Object Lock (Compliance/Governance Mode) to prevent overwrites or deletion.

### Backend (AWS CloudFormation)
The backend is serverless and purely composed of AWS managed resources owned by the user.

*   **S3 Bucket:**
    *   **Versioning:** Enabled (Required for Object Lock).
    *   **Object Lock:** Enabled (Prevents deletion/overwrite).
    *   **Retention:** Default Retention (e.g., 365 days or Indefinite).

## Cost Projections
*   **S3 Storage:** <$0.10/year (Compressed).
*   **Request Costs:** ~$0.02/month (Batched).
