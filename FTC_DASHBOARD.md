# FTC Dashboard

This project includes the Sloth-compatible build of FTC Dashboard `0.6.0` (Slothboard
`0.3.2+0.6.0`). `MainTele` sends the same telemetry to the Driver Station and Dashboard,
with numeric pose and turret values available for plots and CSV export.

## Run it on the robot

1. Build and install the `TeamCode` app on the Robot Controller or Control Hub.
2. Connect the viewing computer or phone to the Robot Controller Wi-Fi network.
3. Open the appropriate address:
   - Control Hub: <http://192.168.43.1:8080/dash>
   - Android Robot Controller phone: <http://192.168.49.1:8080/dash>
4. Select and initialize `MainTele`. Telemetry appears as soon as the OpMode initializes.

No Node.js, Yarn, cloned dashboard repository, or separate development server is required
for normal robot use. Those tools are only needed when modifying the Dashboard web client.

## Dashboard telemetry

The following `MainTele` values are numeric, so they can be plotted and exported:

- `robotXIn`
- `robotYIn`
- `robotHeadingDeg`
- `turretRequestedDeg`
- `turretCommandedDeg`

Dashboard gamepad controls are intended for testing only. Use the Driver Station gamepads
for normal operation and competition.
