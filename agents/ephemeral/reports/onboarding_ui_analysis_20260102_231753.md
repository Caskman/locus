# Deep Analysis Report: Onboarding UI Execution Plan Discrepancies

**Date:** 2026-01-02
**Target:** `agents/ephemeral/phase1-onboarding/09-onboarding-ui-execution-plan.md`
**References:**
- `docs/technical_discovery/specs/ui/onboarding.md` (Authoritative Spec)
- `docs/technical_discovery/user_flows/onboarding.md` (User Flow)

## Executive Summary

A deep analysis of the Task 9 Execution Plan reveals significant discrepancies between the proposed implementation and the authoritative behavioral specifications. The plan currently overlooks the mandatory "Permissions" phase and the "Setup Trap" mechanism required for robust onboarding. Additionally, the proposed UI for provisioning feedback contradicts the detailed requirements.

## Detailed Findings

### 1. Missing "Permissions Phase" & "Setup Trap"

**Finding:**
The execution plan incorrectly routes the user directly from the **Provisioning Success** state to the **Dashboard**. It completely skips the mandatory "Permissions" phase (Location Foreground & Background requests) and fails to implement the "Setup Trap" logic mandated by the specification to ensure users cannot enter the app without granting permissions.

**Discrepancy:**
*   **Plan (Task 9, Step 2):** Logic transitions from `ProvisioningState.Success` directly to `Dashboard` (via `completeOnboarding()` which sets state to `Idle`/`Authenticated`).
*   **Spec (`onboarding.md`, Section 2 & 3.8):** "Upon successful provisioning, the state updates to `PERMISSIONS_PENDING`. If the user closes the app... future launches land directly on Permission Step 1."

**Impact:**
Implementing the plan as written would result in a critical bug where users are authenticated but lack the necessary permissions to perform tracking, bypassing the core functionality setup.

**Recommendation:**
*   Introduce `ProvisioningState.PermissionsPending`.
*   Route `ProvisioningState.Success` to the Permissions Screen.
*   Implement the Two-Step Permission UI (Foreground -> Background).
*   Only transition to Dashboard after permissions are confirmed.

### 2. Incorrect Provisioning UI (Visual Feedback)

**Finding:**
The execution plan proposes a simple "CircularProgressIndicator" for the provisioning screen. This contradicts the specification, which requires a detailed step-by-step list to provide visibility into the long-running infrastructure deployment process.

**Discrepancy:**
*   **Plan (Task 9, Step 3):** "`Deploying`: CircularProgressIndicator + 'Provisioning your secure cloud infrastructure...'"
*   **Spec (`onboarding.md`, Section 3.7):** Explicitly lists displayed steps (e.g., "Validating CloudFormation Template...", "Creating Storage Stack...") and states "Progress bar + Step".

**Impact:**
The proposed UI fails to meet the "Transparency" and "Feedback" requirements for long-running operations (>1 minute), potentially leading to user anxiety or perceived app freeze.

**Recommendation:**
*   Update the UI implementation plan to use a "Step List" or "Current Action Text" subscribed to the `ProvisioningWorker` progress updates.

### 3. Incomplete Domain State Machine

**Finding:**
The plan's domain state updates (`Deploying` -> `Success`) are insufficient to support the "Setup Trap" logic. The system needs to persist a state indicating that infrastructure is ready but permissions are still pending.

**Discrepancy:**
*   **Plan (Task 9, Step 1):** "Call `authRepository.updateProvisioningState(ProvisioningState.Success)`... at successful end."
*   **Spec Requirement:** The state must be granular enough to distinguish "Fully Setup" from "Provisioned but Unpermitted".

**Recommendation:**
*   Update `AuthRepository` and `ProvisioningUseCase` to persist the `PERMISSIONS_PENDING` state (or equivalent) immediately after CloudFormation completes.

## Conclusion

The execution plan requires immediate revision to incorporate the Permissions flow and align the UI implementation with the specifications before coding begins. Proceeding with the current plan will result in a non-compliant implementation requiring significant rework.
