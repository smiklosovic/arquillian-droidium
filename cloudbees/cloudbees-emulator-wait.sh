#!/bin/sh
#
# Starts emulator and waits until it is online
#
# https://arquillian.ci.cloudbees.com
#
# Author: Stefan Miklosovic
# e-mail: smikloso@redhat.com

AVD_NAME=test01
MEMORY=256
ADB_CMD=/opt/android/android-sdk-linux/platform-tools/adb

echo "Starting emulator $AVD_NAME"
echo "executing: 'emulator -avd $AVD_NAME -no-audio -no-window -memory $MEMORY -nocache -no-snapshot-save -no-snapstorage'"

emulator -avd $AVD_NAME -no-audio -no-window -memory $MEMORY -nocache -no-snapshot-save -no-snapstorage &

echo "Waiting until emulator $AVD_NAME is booted"

booted=""
until [[ "$booted" =~ "stopped" ]]; do
    # -e directs command to the only running emulator
    booted=`$ADB_CMD -e shell getprop init.svc.bootanim`
    echo -n $booted
    if [[ "$booted" =~ "not found" ]]; then
        echo "Could not boot emulator"
        exit 1
    fi
    sleep 2
done
echo "Emulator $AVD_NAME booted"
