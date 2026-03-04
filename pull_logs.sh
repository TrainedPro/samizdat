#!/bin/bash
# Pull logs and test results from connected Android device running the P2P app.
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
#       test_results/
#         test_<DeviceName>_<timestamp>.json
#         test_<DeviceName>_<timestamp>.csv
#         test_log_<DeviceName>_<timestamp>.txt
#       endurance_results/
#         endurance_<DeviceName>_<timestamp>.json
#         endurance_<DeviceName>_<timestamp>.csv
#         endurance_log_<DeviceName>_<timestamp>.txt

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

# 3. Pull functional test results (JSON, CSV, log files)
TEST_RESULTS_DIR="$DEVICE_LOG_DIR/test_results"
LOCAL_TEST_DIR="$VERSION_DIR/test_results"
echo "Pulling functional test results..."
TEST_COUNT=0
TEST_FILES=$(adb $SERIAL_FLAG shell "ls $TEST_RESULTS_DIR/ 2>/dev/null" | tr -d '\r' || true)
if [ -n "$TEST_FILES" ]; then
    mkdir -p "$LOCAL_TEST_DIR"
    for fname in $TEST_FILES; do
        fname=$(echo "$fname" | tr -d '\r')
        [ -z "$fname" ] && continue
        adb $SERIAL_FLAG pull "$TEST_RESULTS_DIR/$fname" "$LOCAL_TEST_DIR/$fname" 2>/dev/null && TEST_COUNT=$((TEST_COUNT + 1))
    done
    echo "  Pulled $TEST_COUNT test result file(s)."
else
    echo "  No functional test results found."
fi

# 4. Pull endurance test results (JSON, CSV, log files)
ENDURANCE_RESULTS_DIR="$DEVICE_LOG_DIR/endurance_results"
LOCAL_ENDURANCE_DIR="$VERSION_DIR/endurance_results"
echo "Pulling endurance test results..."
ENDURANCE_COUNT=0
ENDURANCE_FILES=$(adb $SERIAL_FLAG shell "ls $ENDURANCE_RESULTS_DIR/ 2>/dev/null" | tr -d '\r' || true)
if [ -n "$ENDURANCE_FILES" ]; then
    mkdir -p "$LOCAL_ENDURANCE_DIR"
    for fname in $ENDURANCE_FILES; do
        fname=$(echo "$fname" | tr -d '\r')
        [ -z "$fname" ] && continue
        adb $SERIAL_FLAG pull "$ENDURANCE_RESULTS_DIR/$fname" "$LOCAL_ENDURANCE_DIR/$fname" 2>/dev/null && ENDURANCE_COUNT=$((ENDURANCE_COUNT + 1))
    done
    echo "  Pulled $ENDURANCE_COUNT endurance result file(s)."
else
    echo "  No endurance test results found."
fi

echo ""
echo "Done. Logs for $DEVICE_MODEL v$APP_VERSION in: $VERSION_DIR/"
echo "  CSV logs:           $(ls "$VERSION_DIR"/*.csv 2>/dev/null | wc -l) file(s)"
echo "  Logcat snapshots:   $(ls "$VERSION_DIR"/*.log 2>/dev/null | wc -l) file(s)"
echo "  Test results:       $TEST_COUNT file(s)"
echo "  Endurance results:  $ENDURANCE_COUNT file(s)"
echo ""
ls -lhR "$VERSION_DIR" 2>/dev/null || true
