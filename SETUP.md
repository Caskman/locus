# Local Development Setup

## Prerequisites

This project requires the following tools to be installed and configured on your local machine.

### 1. Java Runtime (Java 21+)

**Install via Homebrew:**
```bash
brew install openjdk@21
```

**Configure in `~/.bash_profile` or `~/.zshrc`:**
```bash
# Java (Homebrew OpenJDK)
export PATH="/usr/local/opt/openjdk@21/bin:$PATH"
```

Verify installation:
```bash
source ~/.bash_profile
java -version
```

### 2. Android SDK

1. Download and install [Android Studio](https://developer.android.com/studio)
2. Launch Android Studio to complete initialization
3. The SDK will be installed at `~/Library/Android/sdk`

**Configure in `~/.bash_profile`:**
```bash
# Android SDK
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools:$PATH"
```

Verify installation:
```bash
source ~/.bash_profile
ls $ANDROID_HOME/platforms
```

## Setup Verification

Once all prerequisites are installed, verify your setup:

```bash
# Setup CI environment and install dependencies
./scripts/setup_ci_env.sh

# Run local validation (lint, tests, security checks)
./scripts/run_local_validation.sh
```

If both scripts pass, your local environment is correctly configured.

## Additional Resources

- [Gradle Documentation](https://docs.gradle.org/)
- [Android Developer Documentation](https://developer.android.com/docs)
- [OpenJDK Documentation](https://openjdk.org/)
