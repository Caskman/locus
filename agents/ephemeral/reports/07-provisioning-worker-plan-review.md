# Plan Review: Provisioning Worker (Task 7)

**Document:** `agents/ephemeral/phase1-onboarding/07-provisioning-worker-plan.md`
**Reviewer:** Jules (AI Agent)
**Date:** 2024-05-22

## Executive Summary
The proposed plan for the Provisioning Worker is largely sound but contains four specific discrepancies regarding UI feedback consistency, specification alignment, and error handling robustness that must be addressed to ensure a seamless user experience and maintain architectural integrity.

## Identified Issues & Resolutions

### 1. Notification Channel Semantics
**Problem:**
The plan assigns the Provisioning Worker notification to `channel_tracking`.
*   *Current Spec:* `channel_tracking` is defined in `docs/technical_discovery/specs/ui/notifications.md` as "Low Importance" for "Routine operations (Recording, Uploading)".
*   *Context:* CloudFormation provisioning is a critical, user-initiated setup phase that blocks the application flow. While it is a "routine" part of setup, it is distinct from the passive "Always On" tracking context.
*   *Risk:* Using the tracking channel might lead to future regressions if the tracking channel behavior is tuned strictly for location services (e.g., silent minimization).

**Resolution:**
*   **Action:** Explicitly update `docs/technical_discovery/specs/ui/notifications.md` to broaden the scope of `channel_tracking` to include "Long-Running Setup/Maintenance Operations".
*   **Justification:** Creating a dedicated channel just for setup (which happens once) is overkill. Reusing the "Low Importance/Silent" channel is acceptable for setup *provided* the specification explicitly sanctions this usage to prevent confusion.

### 2. Notification Icon Mismatch
**Problem:**
The plan specifies `Icon: @drawable/ic_stat_tracking`.
*   *Context:* This icon is semantically tied to location/radar visualization.
*   *Issue:* Using a tracking icon for a "Provisioning Resources" or "Recovering Account" state provides confusing visual feedback to the user.

**Resolution:**
*   **Action:** Change the plan to use `@drawable/ic_stat_sync` (as defined in `notifications.md` for "Active (Uploading)") or `android.R.drawable.stat_sys_upload` if a specific resource is missing.
*   **Justification:** "Sync" or "Upload" icons strictly better represent the action of deploying CloudFormation stacks than a "Tracking" icon.

### 3. Architecture Specification Drift (AuthRepository)
**Problem:**
The plan requires adding `suspend fun getBootstrapCredentials(): LocusResult<BootstrapCredentials>` to `AuthRepository`.
*   *Issue:* This method is not currently defined in `docs/technical_discovery/specs/domain_layer_spec.md`.
*   *Risk:* Implementing methods in code that are missing from the primary architectural specification leads to documentation rot and ambiguity about allowed data access paths.

**Resolution:**
*   **Action:** Add a specific step to the plan: "Update `docs/technical_discovery/specs/domain_layer_spec.md` to include `getBootstrapCredentials()` in the `AuthRepository` interface definition."
*   **Justification:** The Domain Specification must remain the Source of Truth. If the implementation requires a new access pattern (exposing raw bootstrap keys to a worker), the spec must reflect that this is an allowed pattern.

### 4. Error State Propagation Gap
**Problem:**
The plan states the Worker should "Return `Result.success()` or `Result.failure()`" based on the UseCase result.
*   *Issue:* The `ProvisioningUseCase` performs the logic, but the UI (Dashboard/Onboarding) is observing `AuthRepository.getProvisioningState()`.
*   *Gap:* If the UseCase returns `Failure` (e.g., Network Error) and the Worker simply returns `Result.failure()`, the WorkManager job ends. Unless the `ProvisioningUseCase` *internally* updates the repo state to `ProvisioningState.Error`, the UI will remain stuck in the `Provisioning` state indefinitely (or until a timeout), providing no feedback to the user.
*   *Risk:* "Zombie" loading screens.

**Resolution:**
*   **Action:** Update the plan's "Logic" section to explicitly mandate that on failure, the Worker (or the UseCase, if preferred) must call `authRepository.updateProvisioningState(ProvisioningState.Error(msg))` *before* returning `Result.failure()`.
*   **Justification:** The UI is reactive. The background worker must drive the state machine to a terminal state (Error) so the UI can react and show the "Retry" screen.

## Conclusion
Resolving these four items will ensure the implementation matches the high standards of the Locus project and prevents immediate technical debt.
