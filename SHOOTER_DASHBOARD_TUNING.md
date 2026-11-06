# Shooter PIDF tuning with FTC Dashboard

## Start the tuner

1. Install the current `TeamCode` build on the robot. A full install is required the first
   time because this tuner depends on Slothboard/FTC Dashboard.
2. Connect to the robot Wi-Fi and open FTC Dashboard:
   - Control Hub: <http://192.168.43.1:8080/dash>
   - Robot Controller phone: <http://192.168.49.1:8080/dash>
3. On the Driver Station, initialize and run **Shooter PIDF Tuner**.
4. Open **Config** in Dashboard and expand **Shooter PIDF Tuner**.
5. Press gamepad 1 **A** to start or stop the shooter. Dashboard configuration cannot start
   the motors by itself.

Start at a low `TARGET_RPM` and confirm both measured motor RPM values are positive and
similar before increasing speed.

## Recommended graphs

Graph these together to see tracking behavior:

- `targetRpm`
- `profiledTargetRpm`
- `measuredRpm`

Graph these together to diagnose the controller and mechanical coupling:

- `errorRpm`
- `motorARpm`
- `motorBRpm`
- `requestedVolts`
- `appliedPower`

## Practical tuning order

1. Set `KP`, `KI`, `KD`, and `KA` to zero.
2. Increase `KV_VOLTS_PER_RPM` until measured steady-state RPM is close to the target.
3. Increase `KS_VOLTS` only enough to overcome low-speed static friction.
4. Increase `KP_VOLTS_PER_RPM` gradually until target disturbances recover promptly without
   sustained oscillation.
5. Add a small `KI_VOLTS_PER_RPM_SECOND` only if a repeatable steady-state error remains.
6. Add `KD_VOLTS_PER_RPM_PER_SECOND` only if damping is needed; noisy acceleration makes
   excessive D gain counterproductive.
7. Tune `KA_VOLTS_PER_RPM_PER_SECOND` during acceleration ramps if feedforward tracking lags.

Press gamepad 1 **X** to save the current Dashboard values to
`FIRST/shooter-calibration.txt`. Press **B** to reset the controller state after a large gain
or target change.

Use a normal full install after changing dependencies or Gradle files. For Java-only tuner
changes under `org.firstinspires.ftc.teamcode`, `:TeamCode:deploySloth` can be used after the
initial full installation.
