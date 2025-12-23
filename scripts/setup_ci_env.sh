#!/bin/bash
set -e

echo "Starting CI Environment Setup..."

# 1. Check for Python 3.11
if command -v python3.11 &> /dev/null; then
    echo "Python 3.11 is available."
else
    echo "Warning: python3.11 not found. Checking for python3..."
    if ! command -v python3 &> /dev/null; then
        echo "Error: python3 could not be found."
        exit 1
    fi
    PY_VER=$(python3 --version)
    echo "Found $PY_VER. Python 3.11 is the strict standard."
fi

# 2. Install Python Dependencies
echo "Installing Python dependencies from scripts/requirements.txt..."
python3 -m pip install -r scripts/requirements.txt
echo "Dependencies installed."

# 2.5 Install Pre-commit hooks
if [ -f ".pre-commit-config.yaml" ]; then
    echo "Installing pre-commit hooks..."
    python3 -m pre_commit install
else
    echo "Warning: .pre-commit-config.yaml not found. Skipping hook installation."
fi

# 3. Verify Trufflehog
if ! command -v trufflehog &> /dev/null; then
    echo "Trufflehog not found. Installing..."
    # Install pinned version
    TH_VERSION="v3.63.0"
    if [ "$(uname -s)" = "Linux" ]; then
        curl -sSfL "https://github.com/trufflesecurity/trufflehog/releases/download/${TH_VERSION}/trufflehog_${TH_VERSION#v}_linux_amd64.tar.gz" -o trufflehog.tar.gz
        tar -xzf trufflehog.tar.gz
        chmod +x trufflehog
        if command -v sudo &> /dev/null; then
             sudo mv trufflehog /usr/local/bin/
        else
             mkdir -p "$HOME/.local/bin"
             mv trufflehog "$HOME/.local/bin/"
             export PATH="$HOME/.local/bin:$PATH"
        fi
        rm trufflehog.tar.gz
    else
        echo "Error: trufflehog not found. Please install it."
        exit 1
    fi
fi
echo "trufflehog is available."

# 4. Verify Java
if ! command -v java &> /dev/null; then
    echo "Error: java is not installed."
    exit 1
fi
echo "Java is available."

# 5. Verify AWS CLI (required for infrastructure audit)
if ! command -v aws &> /dev/null; then
    echo "Warning: AWS CLI is not installed. Infrastructure audit (Tier 4) will fail."
    echo "Install from: https://aws.amazon.com/cli/"
else
    echo "AWS CLI is available: $(aws --version)"
fi

# 6. Verify Shellcheck
if ! command -v shellcheck &> /dev/null; then
    echo "shellcheck not found. Attempting install..."
    OS="$(uname -s)"
    if [ "$OS" = "Linux" ]; then
        if command -v apt-get &> /dev/null; then
            echo "Installing via apt-get..."
            # Use sudo if available, else try without (root)
            if command -v sudo &> /dev/null; then
                sudo apt-get update && sudo apt-get install -y shellcheck
            else
                apt-get update && apt-get install -y shellcheck
            fi
        else
             echo "Error: shellcheck not found and cannot install via apt-get."
             exit 1
        fi
    elif [ "$OS" = "Darwin" ]; then
         if command -v brew &> /dev/null; then
             brew install shellcheck
         else
             echo "Error: shellcheck not found and brew is missing."
             exit 1
         fi
    else
        echo "Error: shellcheck not found. Please install it manually."
        exit 1
    fi
fi
echo "shellcheck is available."

echo "CI Environment Setup Complete."
