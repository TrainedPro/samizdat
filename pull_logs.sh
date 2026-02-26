#!/bin/bash
# Pull logs from connected Android device running the P2P app.
# Groups logs by app version. Device name + timestamp are in filenames.
# All files accumulate in the version folder (no archiving).
#
# Usage:
#   ./pull_logs.sh              # Pull from only connected device
#   ./pull_logs.sh <serial>     # Pull from specific device
#
# Structure:
#   logs/
#     v<version>/
#       p2p_logs_v<version>_<DeviceName>_<timestamp>.csv
#       logcat_<DeviceName>_v<version>_<timestamp>.log

set -e

PACKAGE="com.fyp.resilientp2p"
LOG_DIR="./logs"
DEVICE_LOG_DIR="/sdcard/Android/data/$PACKAGE/files"

SERIAL_FLAG=""
if [ -n "$1" ]; then
    SERIAL_FLAG="-s $1"
fi

# Verify device is connected
if ! adb $SERIAL_FLAG get-state >/dev/null 2>&1; then
    echo "ERROR: No device connected."
    echo "Connected devices:"
    adb devices -l
    exit 1
fi

DEVICE_MODEL=$(adb $SERIAL_FLAG shell getprop ro.product.model | tr -d '\r' | tr ' ' '_')
PULL_TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Get app version from installed package
APP_VERSION=$(adb $SERIAL_FLAG shell dumpsys package "$PACKAGE" 2>/dev/null | grep versionName | head -1 | sed 's/.*versionName=//' | tr -d '\r ' || echo "unknown")
if [ -z "$APP_VERSION" ]; then
    APP_VERSION="unknown"
fi

VERSION_DIR="$LOG_DIR/v$APP_VERSION"

echo "Device: $DEVICE_MODEL"
echo "App Version: $APP_VERSION"

# Create version directory
mkdir -p "$VERSION_DIR"

# 1. Pull exported CSV logs from device storage (prefer versioned filenames)
echo "Pulling CSV exports from device..."
CSV_COUNT=0
CSV_FILES=$(adb $SERIAL_FLAG shell "ls $DEVICE_LOG_DIR/p2p_logs_v*.csv 2>/dev/null" | tr -d '\r' || true)
if [ -z "$CSV_FILES" ]; then
    # Fallback: pull old-format files if no versioned ones exist
    CSV_FILES=$(adb $SERIAL_FLAG shell "ls $DEVICE_LOG_DIR/p2p_logs_*.csv 2>/dev/null" | tr -d '\r' || true)
fi
if [ -n "$CSV_FILES" ]; then
    for remote in $CSV_FILES; do
        local_name=$(basename "$remote" | tr -d '\r')
        # If old-format filename (no device/date), rename with device+timestamp
        if ! echo "$local_name" | grep -q "${DEVICE_MODEL}"; then
            local_name="p2p_logs_v${APP_VERSION}_${DEVICE_MODEL}_${PULL_TIMESTAMP}_${CSV_COUNT}.csv"
        fi
        adb $SERIAL_FLAG pull "$remote" "$VERSION_DIR/$local_name" 2>/dev/null
        CSV_COUNT=$((CSV_COUNT + 1))
    done
    echo "  Pulled $CSV_COUNT CSV file(s)."
else
    echo "  No CSV exports found. Use the Export button in the app first."
fi

# 2. Capture live logcat snapshot (last 2000 lines from P2PManager)
LOGCAT_NAME="logcat_${DEVICE_MODEL}_v${APP_VERSION}_${PULL_TIMESTAMP}.log"
echo "Capturing logcat snapshot..."
adb $SERIAL_FLAG logcat -d -t 2000 | grep -i "P2PManager\|com.fyp.resilientp2p\|NearbyConnections\|NearbyMediums" > "$VERSION_DIR/$LOGCAT_NAME" 2>/dev/null || true
LINES=$(wc -l < "$VERSION_DIR/$LOGCAT_NAME")
echo "  Saved $LINES lines to $VERSION_DIR/$LOGCAT_NAME"

echo ""
echo "Done. Logs for $DEVICE_MODEL v$APP_VERSION in: $VERSION_DIR/"
ls -lh "$VERSION_DIR"/*.csv "$VERSION_DIR"/*.log 2>/dev/null || true
