# Architecture Selection: Phase 1 - Onboarding & Identity

## Comparison Matrix

| Dimension | Path A (ViewModel) | Path B (Service + Repo) | Path C (WorkManager) |
|-----------|--------------------|-------------------------|----------------------|
| **Resilience (R1.600)** | Low | **High** | High |
| **Security (Keys)** | High | **High** | Low (Serialization Risk) |
| **Real-time UX** | Instant | **Instant** | Delayed |
| **Complexity** | Low | **Medium** | Medium |
| **State Recovery** | Poor | **Excellent** | Good |

---

## Selected Path

**Picked:** **Path C (Enhanced): WorkManager + EncryptedPrefs**

**Why:**
1.  **Android 14+ Compliance:** Foreground Services (specifically `dataSync`) have strict system timeouts and usage requirements that a CloudFormation polling loop might violate, leading to process death. `WorkManager` (Long-Running/Expedited) is the platform-sanctioned way to guarantee execution for deferred tasks.
2.  **Security Mitigation:** Instead of serializing "Bootstrap Keys" into `WorkRequest` data, we will store them immediately in `EncryptedSharedPreferences` and pass only a reference/flag to the Worker. This mitigates the original security concern of Path C.
3.  **Resilience:** WorkManager handles retries, backoff, and process resurrection natively, offering superior stability over a manual Foreground Service implementation.
4.  **Tombstoning:** The Worker will write logs to the same local file-based repository as Path B, ensuring the "Setup Trap" and error visibility remain intact.

**Risks we're accepting:**
- **Feedback Latency:** We must ensure the Worker updates the local repository frequently so the UI (observing the repo) feels responsive.
- **State Synchronization:** The UI must reactively observe the Repository state, not the Service directly, to decouple the View from the Process.

**Deferred Choices:**
- **Admin Provisioning:** We will build the engine for Standard users first. Admin templates are supported by the architecture but won't be exposed in the UI yet.
