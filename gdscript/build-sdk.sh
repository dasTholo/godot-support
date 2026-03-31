#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PHP_DIR="$SCRIPT_DIR/php"
BUILD_DIR="$SCRIPT_DIR/build/sdk"
SDK_FILE="$BUILD_DIR/sdk.tar.xz"

# Pruefen ob PHP verfuegbar ist
if ! command -v php &> /dev/null; then
    echo "ERROR: php is required but not installed."
    exit 1
fi

# Pruefen ob bereits ein SDK existiert
if [ -f "$SDK_FILE" ]; then
    echo "SDK already exists at $SDK_FILE"
    echo "Delete it first if you want to rebuild: rm $SDK_FILE"
    exit 0
fi

echo "Building SDK stubs (Godot 4.6+ only)..."
mkdir -p "$BUILD_DIR"

# PHP SDK Builder ausfuehren
cd "$PHP_DIR"
php sdkBuilder.php

# Archiv in den Build-Ordner verschieben
mv "$PHP_DIR/gdscriptsdk.tar.xz" "$SDK_FILE"

# Aufraeumen
rm -rf "$PHP_DIR/sdk" "$PHP_DIR/godot-master" 2>/dev/null || true

echo "SDK built successfully: $SDK_FILE"
echo "Contents:"
tar -tf "$SDK_FILE" | head -20
echo "..."
tar -tf "$SDK_FILE" | wc -l
echo "total entries"
