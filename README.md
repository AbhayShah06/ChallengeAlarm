# Challenge Alarm

An Android-first alarm whose dismissal requires a deliberate challenge. The first foundation proves the difficult part first: exact alarm permission, an alarm-clock trigger, lock-screen presentation, sound/vibration, and a math-gated dismissal.

## Starter included

- One-minute exact-alarm reliability harness
- Explicit exact-alarm permission route
- Lock-screen alarm activity
- Looping vibration and system alarm sound
- Math challenge that must be correct to dismiss
- Boot receiver seam for persisted alarm restoration

## Hard release plan

1. **Persistence:** Room database for alarms, schedules, challenge settings, and completion logs; reschedule after reboot/timezone/time changes.
2. **Reliability:** foreground alarm service, notification full-screen intent, Doze tests, silent-mode policy, process-death tests, and manufacturer-specific battery guidance.
3. **Challenges:** configurable math, QR/barcode, step count, photo match, and destination arrival. Every challenge needs an offline fallback so a broken sensor cannot trap the user.
4. **Safety:** emergency dismiss route with a logged penalty/delay, volume ceiling, accessibility review, and escalating but finite retries.
5. **Release gate:** repeated physical-device tests on Android 10–15, reboot, low battery, airplane mode, and denied permissions.

## Build status

The source foundation is present. No APK was produced because Java and the Android SDK were not installed on this machine.

