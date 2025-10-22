#!/usr/bin/env bash

# Build script for creating platform-specific distributions locally

set -e

echo "=== Building jdx Distribution ==="
echo ""

# Detect OS
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    PLATFORM="linux"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    PLATFORM="macos"
elif [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]]; then
    PLATFORM="windows"
else
    PLATFORM="unknown"
fi

echo "Detected platform: $PLATFORM"
echo ""

# Clean previous builds
echo "Cleaning previous builds..."
mvn clean

# Build standard JAR
echo ""
echo "Building shaded JAR..."
mvn package -DskipTests

# Build with jlink runtime
echo ""
echo "Building distribution with jlink runtime..."
mvn package -Pjlink-runtime -DskipTests

# Show results
echo ""
echo "=== Build Complete ==="
echo ""
echo "Artifacts created:"
ls -lh target/*.jar target/*.zip target/*.tar.gz 2>/dev/null || true

echo ""
echo "To test the distribution:"
echo "  1. Extract: unzip target/jdx-*-${PLATFORM}-*.zip"
echo "  2. Run: ./jdx-*/bin/jdx --help"
