#!/bin/bash
set -e

echo "Starting Local Validation Suite (Tier 2)..."

# 1. Static Analysis (Pre-commit)
echo "Running Pre-commit hooks (Ktlint, Detekt, Shellcheck, Secrets)..."
# Determine command name (pre-commit or python3 -m pre_commit)
if command -v pre-commit &> /dev/null; then
    PRE_COMMIT_CMD="pre-commit"
else
    PRE_COMMIT_CMD="python3 -m pre_commit"
fi
$PRE_COMMIT_CMD run --all-files || { echo "Pre-commit checks failed"; exit 1; }

# 2. Gradle Checks (Deep Verification)
echo "Running Lint and Unit Tests..."
./gradlew lintDebug testDebugUnitTest || { echo "Gradle checks failed"; exit 1; }

# 2. Security Checks
echo "Running Security Verification..."
./scripts/verify_security.sh || { echo "Security verification failed"; exit 1; }

echo "Local Validation Suite Passed."
