package org.firstinspires.ftc.teamcode.Tests;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.Shooter.DualMotorShooter;
import org.firstinspires.ftc.teamcode.Shooter.ShooterCalibration;

/** Live voltage-based PID + feedforward tuner for the mechanically coupled shooter. */
@TeleOp(name = "Shooter PIDF Tuner", group = "Test")
public class FlywheelPFTuner extends OpMode {
    private static final String[] PARAMETER_NAMES = {
            "Target RPM", "kS", "kV", "kA", "kP", "kI", "kD",
            "Max acceleration", "Ready RPM tolerance", "Motor:flywheel ratio"
    };
    private static final double[] BASE_STEPS = {
            100.0, 0.01, 0.00001, 0.000001, 0.00001, 0.000001, 0.000001,
            500.0, 5.0, 0.01
    };

    private ShooterCalibration calibration;
    private DualMotorShooter shooter;
    // Start low so motor direction and encoder signs can be checked before high-speed tuning.
    private double targetRpm = 1000.0;
    private int selectedParameter;
    private double stepMultiplier = 1.0;
    private boolean running;
    private String saveStatus = "Not saved this run";

    @Override
    public void init() {
        calibration = ShooterCalibration.load();
        shooter = new DualMotorShooter(hardwareMap, calibration);
        telemetry.addLine("Shooter tuner initialized; motors remain OFF until A is pressed.");
        telemetry.update();
    }

    @Override
    public void loop() {
        if (gamepad1.dpadRightWasPressed()) {
            selectedParameter = (selectedParameter + 1) % PARAMETER_NAMES.length;
        }
        if (gamepad1.dpadLeftWasPressed()) {
            selectedParameter = (selectedParameter - 1 + PARAMETER_NAMES.length)
                    % PARAMETER_NAMES.length;
        }
        if (gamepad1.leftBumperWasPressed()) {
            stepMultiplier = Math.max(0.01, stepMultiplier / 10.0);
        }
        if (gamepad1.rightBumperWasPressed()) {
            stepMultiplier = Math.min(100.0, stepMultiplier * 10.0);
        }
        if (gamepad1.dpadUpWasPressed()) {
            changeSelected(BASE_STEPS[selectedParameter] * stepMultiplier);
        }
        if (gamepad1.dpadDownWasPressed()) {
            changeSelected(-BASE_STEPS[selectedParameter] * stepMultiplier);
        }
        if (gamepad1.aWasPressed()) {
            running = !running;
            if (running) shooter.resetController();
            else shooter.stop();
        }
        if (gamepad1.bWasPressed()) shooter.resetController();
        if (gamepad1.xWasPressed()) {
            calibration.save();
            saveStatus = "Saved to FIRST/" + ShooterCalibration.FILE_NAME;
        }

        shooter.setCalibration(calibration);
        shooter.setTargetRpm(running ? targetRpm : 0.0);
        shooter.update();

        telemetry.addData("STATE", running ? "RUNNING" : "OFF");
        telemetry.addData("Ready", shooter.isReady());
        telemetry.addData("Encoders agree", shooter.encodersAgree());
        telemetry.addData("Target / profiled RPM", "%.0f / %.0f",
                targetRpm, shooter.getProfiledTargetRpm());
        telemetry.addData("Average RPM", "%.1f", shooter.getMeasuredRpm());
        telemetry.addData("Motor A / B RPM", "%.1f / %.1f",
                shooter.getMotorARpm(), shooter.getMotorBRpm());
        telemetry.addData("Error / acceleration", "%.1f / %.1f RPM/s",
                shooter.getErrorRpm(), shooter.getAccelerationRpmPerSecond());
        telemetry.addData("Requested volts / power", "%.2f V / %.3f",
                shooter.getRequestedVolts(), shooter.getAppliedPower());
        telemetry.addData("Battery", "%.2f V", shooter.getBatteryVoltage());
        telemetry.addLine("--------------------------------");
        telemetry.addData("Selected", "%s = %.10f", PARAMETER_NAMES[selectedParameter],
                getSelectedValue());
        telemetry.addData("Adjustment", "%.10f", BASE_STEPS[selectedParameter] * stepMultiplier);
        telemetry.addData("Gains", "S %.5f  V %.7f  A %.8f", calibration.kS,
                calibration.kV, calibration.kA);
        telemetry.addData("PID", "P %.7f  I %.8f  D %.8f", calibration.kP,
                calibration.kI, calibration.kD);
        telemetry.addData("Save", saveStatus);
        telemetry.addLine("A run/stop | X save | B reset controller");
        telemetry.addLine("Dpad L/R select | U/D change | LB/RB step size");
        telemetry.update();
    }

    @Override
    public void stop() {
        if (shooter != null) shooter.stop();
    }

    private double getSelectedValue() {
        switch (selectedParameter) {
            case 0: return targetRpm;
            case 1: return calibration.kS;
            case 2: return calibration.kV;
            case 3: return calibration.kA;
            case 4: return calibration.kP;
            case 5: return calibration.kI;
            case 6: return calibration.kD;
            case 7: return calibration.maxAccelerationRpmPerSecond;
            case 8: return calibration.readyToleranceRpm;
            case 9: return calibration.motorToFlywheelRatio;
            default: return 0.0;
        }
    }

    private void changeSelected(double change) {
        switch (selectedParameter) {
            case 0: targetRpm = Math.max(0.0, targetRpm + change); break;
            case 1: calibration.kS = Math.max(0.0, calibration.kS + change); break;
            case 2: calibration.kV = Math.max(0.0, calibration.kV + change); break;
            case 3: calibration.kA = Math.max(0.0, calibration.kA + change); break;
            case 4: calibration.kP = Math.max(0.0, calibration.kP + change); break;
            case 5: calibration.kI = Math.max(0.0, calibration.kI + change); break;
            case 6: calibration.kD = Math.max(0.0, calibration.kD + change); break;
            case 7:
                calibration.maxAccelerationRpmPerSecond =
                        Math.max(100.0, calibration.maxAccelerationRpmPerSecond + change);
                break;
            case 8:
                calibration.readyToleranceRpm =
                        Math.max(1.0, calibration.readyToleranceRpm + change);
                break;
            case 9:
                calibration.motorToFlywheelRatio =
                        Math.max(0.01, calibration.motorToFlywheelRatio + change);
                break;
            default: break;
        }
    }
}
