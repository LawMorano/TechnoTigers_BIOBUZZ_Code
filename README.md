# TechoTigers BIOBUZZ Robot Code

FTC robot code for the TechoTigers BIOBUZZ robot, built on FTC SDK 12.0.0.

The current project contains:

- Robot-centric mecanum TeleOp using Pedro Pathing and goBILDA Pinpoint odometry
- Automatic turret aiming toward selectable field targets
- Dual intake and dual flywheel shooter controls
- A voltage-based shooter PID + feedforward controller
- Live shooter tuning through FTC Dashboard
- Turret and hood calibration OpModes
- Sloth hot reload for faster Java deployment

The autonomous class currently included in the project is only an empty starting template; it
does not yet drive a match routine.

## Requirements

- Android Studio with its bundled JDK 17 or newer
- FTC Robot Controller SDK 12.0.0
- A Control Hub or Android Robot Controller phone
- A Driver Station
- Correct hardware names in the active FTC Robot Configuration

## Robot hardware configuration

The code currently expects these hardware names:

| Device | Configuration name | Used by |
| --- | --- | --- |
| Front-left drive motor | `FL` | Pedro Pathing and TeleOp |
| Front-right drive motor | `FR` | Pedro Pathing and TeleOp |
| Back-left drive motor | `BL` | Pedro Pathing and TeleOp |
| Back-right drive motor | `BR` | Pedro Pathing and TeleOp |
| goBILDA Pinpoint | `pinpoint` | Localization and turret aiming |
| Intake motor 1 | `intake1` | MainTele |
| Intake motor 2 | `intake2` | MainTele |
| TeleOp top shooter motor | `TopShooter` | MainTele fixed-power shooter |
| TeleOp bottom shooter motor | `BottomShooter` | MainTele fixed-power shooter |
| PIDF shooter motor A | `ShooterMotor` | DualMotorShooter and tuner |
| PIDF shooter motor B | `ShooterMotor2` | DualMotorShooter and tuner |
| Left turret servo | `LEFT-TurretServo` | MainTele and turret test |
| Right turret servo | `RIGHT-TurretServo` | MainTele and turret test |
| Shooter hood servo | `Shooter hood` | Disabled hood test |

> **Shooter naming warning:** MainTele and the PIDF shooter controller currently use different
> motor names. If they control the same physical motors, choose one pair of Robot Configuration
> names and update the other class before using PIDF control in TeleOp. The tuned calibration is
> not currently used by MainTele's fixed-power shooter.

## Main TeleOp

Select **MainTele** in the Driver Station.

MainTele uses Pedro Pathing for robot-centric mecanum control and Pinpoint localization. It also
aims the turret toward one of two red goal coordinates while respecting a conservative
`-90°` to `+90°` turret range.

### Gamepad 1 controls

| Control | Action |
| --- | --- |
| Left stick Y | Drive forward and backward |
| Left stick X | Strafe left and right |
| Right stick X | Turn |
| A | Turn both intake motors on at 80% power |
| B | Stop both intake motors |
| Y | Turn both TeleOp shooter motors on at 90% power |
| X | Stop both TeleOp shooter motors |
| D-pad left | Toggle between the close and far red goals |
| Start | Reset the robot position to `(8, 8)` while preserving heading |

The selectable turret targets are:

- Close red goal: `(58, 52)` inches
- Far red goal: `(58, 94)` inches

MainTele publishes robot position, heading, turret request, and commanded turret angle to both
the Driver Station and FTC Dashboard.

## Shooter subsystem

The closed-loop shooter implementation is split into three classes with separate jobs.

### `DualMotorShooter`

`DualMotorShooter` directly controls `ShooterMotor` and `ShooterMotor2` as one mechanically
coupled flywheel system. It:

- Reads both motor encoders and converts motor velocity to flywheel RPM
- Averages both encoder measurements
- Applies an acceleration-limited RPM target
- Calculates voltage-based feedforward using `kS`, `kV`, and `kA`
- Calculates feedback using `kP`, `kI`, and `kD`
- Compensates motor power for measured battery voltage
- Limits integral accumulation to an RPM zone and voltage range
- Reports whether the shooter is stable and ready
- Detects when the two encoder readings disagree by more than 10%

Motor A is configured forward and motor B is configured reverse. Verify both encoder readings
are positive at low speed before running the shooter at full speed.

### `ShooterCalibration`

`ShooterCalibration` holds the controller gains, limits, readiness criteria, and
motor-to-flywheel ratio. It loads and saves:

```text
FIRST/shooter-calibration.txt
```

This makes tuned values available again after the Robot Controller restarts.

### `FlywheelPFTuner`

The **Shooter PIDF Tuner** OpMode connects FTC Dashboard to `DualMotorShooter`. It exposes the
calibration values as live Dashboard configuration fields and publishes numeric telemetry that
can be plotted and exported.

For safety, changing Dashboard values cannot start the motors. Gamepad 1 **A** is always
required to start or stop the tuner.

## Tuning the shooter with FTC Dashboard

### 1. Prepare the robot

1. Secure the robot so it cannot move or launch a game element unexpectedly.
2. Remove game elements while initially checking motor direction and encoder signs.
3. Confirm the Robot Configuration contains `ShooterMotor` and `ShooterMotor2` as encoder-capable
   motors.
4. Perform a normal full `TeamCode` installation after changing dependencies or hardware names.
5. Connect the computer to the robot Wi-Fi network.

Open Dashboard at:

- Control Hub: <http://192.168.43.1:8080/dash>
- Android Robot Controller phone: <http://192.168.49.1:8080/dash>

### 2. Start the tuning OpMode

1. Select **Shooter PIDF Tuner** on the Driver Station.
2. Initialize and run the OpMode.
3. In Dashboard, open **Config** and expand **Shooter PIDF Tuner**.
4. Keep `TARGET_RPM` low for the first test. The OpMode begins at `1000 RPM`.
5. Press gamepad 1 **A** to start the motors. Press **A** again to stop them.
6. Confirm `motorARpm` and `motorBRpm` are both positive and reasonably close.

Stop immediately if the motors fight each other, an encoder reports the wrong sign, the belts
slip, or the mechanism vibrates excessively.

### 3. Dashboard tuning fields

| Dashboard field | Meaning |
| --- | --- |
| `TARGET_RPM` | Requested flywheel speed |
| `KS_VOLTS` | Voltage needed to overcome static friction |
| `KV_VOLTS_PER_RPM` | Steady-state voltage per RPM |
| `KA_VOLTS_PER_RPM_PER_SECOND` | Acceleration feedforward |
| `KP_VOLTS_PER_RPM` | Proportional correction for RPM error |
| `KI_VOLTS_PER_RPM_SECOND` | Integral correction for persistent error |
| `KD_VOLTS_PER_RPM_PER_SECOND` | Damping based on measured acceleration |
| `MAX_ACCELERATION_RPM_PER_SECOND` | Maximum rate applied to the profiled target |
| `INTEGRAL_ZONE_RPM` | Error range in which integral is allowed to accumulate |
| `INTEGRAL_LIMIT_VOLTS` | Maximum positive or negative integral contribution |
| `READY_TOLERANCE_RPM` | Allowed RPM error for the ready state |
| `READY_ACCELERATION_RPM_PER_SECOND` | Allowed speed change for the ready state |
| `READY_TIME_SECONDS` | Time that readiness conditions must remain true |
| `MOTOR_TO_FLYWHEEL_RATIO` | Motor RPM multiplier used to calculate flywheel RPM |

### 4. Create useful Dashboard graphs

Graph these values together to evaluate target tracking:

- `targetRpm`
- `profiledTargetRpm`
- `measuredRpm`

Graph these values to diagnose the motors and controller:

- `errorRpm`
- `motorARpm`
- `motorBRpm`
- `accelerationRpmPerSecond`
- `requestedVolts`
- `appliedPower`
- `batteryVolts`

### 5. Tune in this order

1. Set `KP`, `KI`, `KD`, and `KA` to zero.
2. Increase `KV_VOLTS_PER_RPM` until steady-state measured RPM is close to the target.
3. Increase `KS_VOLTS` only enough to overcome low-speed static friction.
4. Increase `KP_VOLTS_PER_RPM` gradually until the shooter recovers from disturbances promptly
   without sustained oscillation.
5. Add a small `KI_VOLTS_PER_RPM_SECOND` only when a repeatable steady-state error remains.
6. Add `KD_VOLTS_PER_RPM_PER_SECOND` only when additional damping is needed. Acceleration is
   noisy, so excessive D gain can make the output unstable.
7. Tune `KA_VOLTS_PER_RPM_PER_SECOND` during acceleration ramps if the measured speed lags the
   profiled target.
8. Test at several target speeds and battery voltages before accepting the calibration.

Make small changes. If a large gain or target change causes unexpected behavior, press gamepad
1 **A** to stop the shooter. Press **B** to reset the controller state before restarting.

### 6. Save the calibration

Press gamepad 1 **X** while the tuner OpMode is active to save the current Dashboard values to
`FIRST/shooter-calibration.txt`.

Dashboard changes are live but are not persistent until **X** is pressed. The next time the
tuner initializes, it loads the saved values and pushes them into the Dashboard Config view.

The tuner can also be adjusted from the gamepad:

| Control | Action |
| --- | --- |
| D-pad left/right | Select a parameter |
| D-pad up/down | Increase or decrease the selected value |
| Left/right bumper | Divide or multiply the adjustment size by 10 |
| A | Start or stop the shooter |
| B | Reset controller state |
| X | Save calibration |

## Turret and hood tests

### Turret Recenter and Limits Test

This enabled test moves both turret servos together, finds safe mechanical limits, and displays
the calculated servo positions.

Important controls include:

- Left stick X: move the turret
- D-pad left/right: move one degree per press
- X: return to zero
- A/B: save the current minimum/maximum
- Y: toggle saved-limit enforcement

Center both servos before engaging the gears, and test at low speed first.

### Hood Test

`HoodTestServoOnly` contains close, middle, far, and safe servo presets, but it is currently
annotated `@Disabled` and does not appear on the Driver Station until that annotation is removed.

## FTC Dashboard

The project uses Slothboard `0.3.2+0.6.0`, the Sloth-compatible build of FTC Dashboard 0.6.0.
Node.js and Yarn are not required for normal robot use.

MainTele Dashboard channels include:

- `robotXIn`
- `robotYIn`
- `robotHeadingDeg`
- `turretRequestedDeg`
- `turretCommandedDeg`

Use Driver Station gamepads for normal driving and competition. Browser gamepad support should
be treated as a testing convenience only.

## Sloth hot reload

Perform one normal full installation before using Sloth. After changing Java code under
`org.firstinspires.ftc.teamcode`, end the active OpMode and run:

```text
:TeamCode:deploySloth
```

Use a normal full install after changing Gradle files, dependencies, Android resources,
manifests, files outside the TeamCode package, or `@Pinned` annotations.

Before a normal install, remove remotely loaded classes with:

```text
:TeamCode:removeSlothRemote
```

For the complete setup and cleanup procedure, see [SLOTH.md](SLOTH.md).

## Project layout

```text
TeamCode/src/main/java/org/firstinspires/ftc/teamcode/
├── TeleOp/
│   └── MainTele.java
├── Shooter/
│   ├── DualMotorShooter.java
│   └── ShooterCalibration.java
├── Tests/
│   ├── FlywheelPFTuner.java
│   ├── HoodTestServoOnly.java
│   └── TurretRecenterAndLimitsTest.java
├── pedro/
│   ├── Constants.java
│   ├── BlueSide/BlueSidePath.java
│   └── procedures/
└── pinpoint/
    └── GoBildaPinpointDriver.java
```

Additional guides:

- [FTC Dashboard setup](FTC_DASHBOARD.md)
- [Shooter Dashboard tuning](SHOOTER_DASHBOARD_TUNING.md)
- [Sloth hot reload](SLOTH.md)

## Current limitations and next steps

- `BlueSidePath` is an empty autonomous template.
- MainTele uses fixed shooter power rather than `DualMotorShooter` closed-loop RPM control.
- MainTele and the PIDF tuner use different shooter hardware names.
- The hood test is disabled and the hood is not controlled by MainTele.
- Tuned PIDF values must be integrated into MainTele before they affect match operation.

Before competition, unify the shooter motor names and replace MainTele's fixed-power shooter
logic with `DualMotorShooter` so the saved calibration controls the match shooter.
