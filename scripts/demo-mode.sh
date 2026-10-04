#!/usr/bin/env bash
# SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

# Usage: scripts/demo-mode.sh [on|off]   (default: on)
set -euo pipefail

CLOCK=1700
DATATYPE=5g

demo() { adb shell am broadcast -a com.android.systemui.demo "$@" > /dev/null; }

case "${1:-on}" in
    on)
        adb shell settings put global sysui_demo_allowed 1
        demo -e command enter
        demo -e command clock -e hhmm "$CLOCK"
        demo -e command battery -e level 100 -e plugged false
        demo -e command network -e fully true -e  wifi show -e level 4
        demo -e command network -e fully true -e mobile show -e datatype none -e level 4
        demo -e command status -e bluetooth hide -e location hide -e alarm hide -e zen hide \
            -e sync hide -e tty hide -e eri hide -e mute hide -e speakerphone hide
        adb shell cmd statusbar send-disable-flag notification-icons
        ;;
    off)
        adb shell cmd statusbar send-disable-flag none
        demo -e command exit
        adb shell settings put global sysui_demo_allowed 0
        ;;
    *)
        echo "Usage: $0 [on|off]" >&2
        exit 1
        ;;
esac
