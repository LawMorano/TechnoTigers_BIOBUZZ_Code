package org.firstinspires.ftc.teamcode.Tests;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.Shooter.ShooterCalibration;

/** Live voltage-based PID + feedforward tuner for the mechanically coupled shooter. */
@Config("Shooter PIDF Tuner")
@TeleOp(name = "Shooter PIDF Tuner", group = "Test")
public class FlywheelPFTuner extends OpMode {
    // Public, static, non-final fields appear in the FTC Dashboard Config view.
    // Motor enable remains gamepad-only so opening Dashboard cannot start the shooter.
    public static double TARGET_RPM = 1000.0;
    public static double KS_VOLTS = 0.0;
    public static double KV_VOLTS_PER_RPM = 0.0020;
    public static double KA_VOLTS_PER_RPM_PER_SECOND = 0.0;
    public static double KP_VOLTS_PER_RPM = 0.0010;
    public static double KI_VOLTS_PER_RPM_SECOND = 0.0;
    public static double KD_VOLTS_PER_RPM_PER_SECOND = 0.0;
    public static double MAX_ACCELERATION_RPM_PER_SECOND = 12000.0;
    public static double INTEGRAL_ZONE_RPM = 250.0;
    public static double INTEGRAL_LIMIT_VOLTS = 1.0;
    public static double READY_TOLERANCE_RPM = 75.0;
    public static double READY_ACCELERATION_RPM_PER_SECOND = 400.0;
    public static double READY_TIME_SECONDS = 0.15;
    public static double MOTOR_TO_FLYWHEEL_RATIO = 1.0;

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
    private FtcDashboard dashboard;
    private int selectedParameter;
    private double stepMultiplier = 1.0;
    private boolean running;
    private String saveStatus = "Not saved this run";

    @Override
    public void init() {
        calibration = ShooterCalibration.load();
        copyCalibrationToDashboard();
        shooter = new DualMotorShooter(hardwareMap, calibration);

        dashboard = FtcDashboard.getInstance();
        // Code-side changes are not automatically pushed back to the Config view.
        dashboard.updateConfig();
        dashboard.setTelemetryTransmissionInterval(50);
        telemetry = new MultipleTelemetry(telemetry, dashboard.getTelemetry());

        telemetry.addLine("Shooter tuner initialized; motors remain OFF until A is pressed.");
        telemetry.addLine("Edit gains in Dashboard > Config > Shooter PIDF Tuner.");
        telemetry.update();
    }

    @Override
    public void loop() {
        sanitizeDashboardValues();

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
            dashboard.updateConfig();
        }
        if (gamepad1.dpadDownWasPressed()) {
            changeSelected(-BASE_STEPS[selectedParameter] * stepMultiplier);
            dashboard.updateConfig();
        }
        if (gamepad1.aWasPressed()) {
            running = !running;
            if (running) shooter.resetController();
            else shooter.stop();
        }
        if (gamepad1.bWasPressed()) shooter.resetController();
        if (gamepad1.xWasPressed()) {
            copyDashboardToCalibration();
            calibration.save();
            saveStatus = "Saved to FIRST/" + ShooterCalibration.FILE_NAME;
        }

        copyDashboardToCalibration();
        shooter.setCalibration(calibration);
        shooter.setTargetRpm(running ? TARGET_RPM : 0.0);
        shooter.update();

        telemetry.addData("STATE", running ? "RUNNING" : "OFF");
        telemetry.addData("Ready", shooter.isReady());
        telemetry.addData("Encoders agree", shooter.encodersAgree());
        // Raw numeric values can be plotted together in Dashboard's Graph view.
        telemetry.addData("targetRpm", shooter.getTargetRpm());
        telemetry.addData("profiledTargetRpm", shooter.getProfiledTargetRpm());
        telemetry.addData("measuredRpm", shooter.getMeasuredRpm());
        telemetry.addData("motorARpm", shooter.getMotorARpm());
        telemetry.addData("motorBRpm", shooter.getMotorBRpm());
        telemetry.addData("errorRpm", shooter.getErrorRpm());
        telemetry.addData("accelerationRpmPerSecond",
                shooter.getAccelerationRpmPerSecond());
        telemetry.addData("requestedVolts", shooter.getRequestedVolts());
        telemetry.addData("appliedPower", shooter.getAppliedPower());
        telemetry.addData("batteryVolts", shooter.getBatteryVoltage());
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
            case 0: return TARGET_RPM;
            case 1: return KS_VOLTS;
            case 2: return KV_VOLTS_PER_RPM;
            case 3: return KA_VOLTS_PER_RPM_PER_SECOND;
            case 4: return KP_VOLTS_PER_RPM;
            case 5: return KI_VOLTS_PER_RPM_SECOND;
            case 6: return KD_VOLTS_PER_RPM_PER_SECOND;
            case 7: return MAX_ACCELERATION_RPM_PER_SECOND;
            case 8: return READY_TOLERANCE_RPM;
            case 9: return MOTOR_TO_FLYWHEEL_RATIO;
            default: return 0.0;
        }
    }

    private void changeSelected(double change) {
        switch (selectedParameter) {
            case 0: TARGET_RPM = Math.max(0.0, TARGET_RPM + change); break;
            case 1: KS_VOLTS = Math.max(0.0, KS_VOLTS + change); break;
            case 2: KV_VOLTS_PER_RPM = Math.max(0.0, KV_VOLTS_PER_RPM + change); break;
            case 3:
                KA_VOLTS_PER_RPM_PER_SECOND =
                        Math.max(0.0, KA_VOLTS_PER_RPM_PER_SECOND + change);
                break;
            case 4: KP_VOLTS_PER_RPM = Math.max(0.0, KP_VOLTS_PER_RPM + change); break;
            case 5:
                KI_VOLTS_PER_RPM_SECOND = Math.max(0.0, KI_VOLTS_PER_RPM_SECOND + change);
                break;
            case 6:
                KD_VOLTS_PER_RPM_PER_SECOND =
                        Math.max(0.0, KD_VOLTS_PER_RPM_PER_SECOND + change);
                break;
            case 7:
                MAX_ACCELERATION_RPM_PER_SECOND =
                        Math.max(100.0, MAX_ACCELERATION_RPM_PER_SECOND + change);
                break;
            case 8:
                READY_TOLERANCE_RPM = Math.max(1.0, READY_TOLERANCE_RPM + change);
                break;
            case 9:
                MOTOR_TO_FLYWHEEL_RATIO = Math.max(0.01, MOTOR_TO_FLYWHEEL_RATIO + change);
                break;
            default: break;
        }
    }

    private void copyCalibrationToDashboard() {
        // Always begin an OpMode at a conservative target; the motors still require A to run.
        TARGET_RPM = 1000.0;
        KS_VOLTS = calibration.kS;
        KV_VOLTS_PER_RPM = calibration.kV;
        KA_VOLTS_PER_RPM_PER_SECOND = calibration.kA;
        KP_VOLTS_PER_RPM = calibration.kP;
        KI_VOLTS_PER_RPM_SECOND = calibration.kI;
        KD_VOLTS_PER_RPM_PER_SECOND = calibration.kD;
        MAX_ACCELERATION_RPM_PER_SECOND = calibration.maxAccelerationRpmPerSecond;
        INTEGRAL_ZONE_RPM = calibration.integralZoneRpm;
        INTEGRAL_LIMIT_VOLTS = calibration.integralLimitVolts;
        READY_TOLERANCE_RPM = calibration.readyToleranceRpm;
        READY_ACCELERATION_RPM_PER_SECOND = calibration.readyAccelerationRpmPerSecond;
        READY_TIME_SECONDS = calibration.readyTimeSeconds;
        MOTOR_TO_FLYWHEEL_RATIO = calibration.motorToFlywheelRatio;
    }

    private void copyDashboardToCalibration() {
        calibration.kS = KS_VOLTS;
        calibration.kV = KV_VOLTS_PER_RPM;
        calibration.kA = KA_VOLTS_PER_RPM_PER_SECOND;
        calibration.kP = KP_VOLTS_PER_RPM;
        calibration.kI = KI_VOLTS_PER_RPM_SECOND;
        calibration.kD = KD_VOLTS_PER_RPM_PER_SECOND;
        calibration.maxAccelerationRpmPerSecond = MAX_ACCELERATION_RPM_PER_SECOND;
        calibration.integralZoneRpm = INTEGRAL_ZONE_RPM;
        calibration.integralLimitVolts = INTEGRAL_LIMIT_VOLTS;
        calibration.readyToleranceRpm = READY_TOLERANCE_RPM;
        calibration.readyAccelerationRpmPerSecond = READY_ACCELERATION_RPM_PER_SECOND;
        calibration.readyTimeSeconds = READY_TIME_SECONDS;
        calibration.motorToFlywheelRatio = MOTOR_TO_FLYWHEEL_RATIO;
    }

    private void sanitizeDashboardValues() {
        TARGET_RPM = Math.max(0.0, TARGET_RPM);
        KS_VOLTS = Math.max(0.0, KS_VOLTS);
        KV_VOLTS_PER_RPM = Math.max(0.0, KV_VOLTS_PER_RPM);
        KA_VOLTS_PER_RPM_PER_SECOND = Math.max(0.0, KA_VOLTS_PER_RPM_PER_SECOND);
        KP_VOLTS_PER_RPM = Math.max(0.0, KP_VOLTS_PER_RPM);
        KI_VOLTS_PER_RPM_SECOND = Math.max(0.0, KI_VOLTS_PER_RPM_SECOND);
        KD_VOLTS_PER_RPM_PER_SECOND = Math.max(0.0, KD_VOLTS_PER_RPM_PER_SECOND);
        MAX_ACCELERATION_RPM_PER_SECOND = Math.max(100.0, MAX_ACCELERATION_RPM_PER_SECOND);
        INTEGRAL_ZONE_RPM = Math.max(0.0, INTEGRAL_ZONE_RPM);
        INTEGRAL_LIMIT_VOLTS = Math.max(0.0, INTEGRAL_LIMIT_VOLTS);
        READY_TOLERANCE_RPM = Math.max(1.0, READY_TOLERANCE_RPM);
        READY_ACCELERATION_RPM_PER_SECOND =
                Math.max(0.0, READY_ACCELERATION_RPM_PER_SECOND);
        READY_TIME_SECONDS = Math.max(0.0, READY_TIME_SECONDS);
        MOTOR_TO_FLYWHEEL_RATIO = Math.max(0.01, MOTOR_TO_FLYWHEEL_RATIO);
    }
}
