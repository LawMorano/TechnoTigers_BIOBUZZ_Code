package org.firstinspires.ftc.teamcode.Shooter;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.VoltageSensor;

/** Controls two mechanically coupled shooter motors as one velocity-controlled plant. */
public class DualMotorShooter {
    public static final String MOTOR_A_NAME = "ShooterMotor";
    public static final String MOTOR_B_NAME = "ShooterMotor2";
    // Verify these directions at low power before installing/running the belts at full speed.
    public static final DcMotorSimple.Direction MOTOR_A_DIRECTION = DcMotorSimple.Direction.FORWARD;
    public static final DcMotorSimple.Direction MOTOR_B_DIRECTION = DcMotorSimple.Direction.FORWARD;

    private static final double NOMINAL_BATTERY_VOLTS = 12.0;
    private static final double ACCELERATION_FILTER_SECONDS = 0.05;

    private final DcMotorEx motorA;
    private final DcMotorEx motorB;
    private final HardwareMap hardwareMap;
    private final double ticksPerMotorRevolutionA;
    private final double ticksPerMotorRevolutionB;
    private ShooterCalibration calibration;
    private double targetRpm;
    private double profiledTargetRpm;
    private double integralVoltage;
    private double filteredAccelerationRpmPerSecond;
    private double previousMeasuredRpm;
    private double previousTimeSeconds;
    private double readyDurationSeconds;
    private double requestedVolts;
    private double appliedPower;
    private double motorARpm;
    private double motorBRpm;
    private double measuredRpm;
    private double errorRpm;

    public DualMotorShooter(HardwareMap hardwareMap, ShooterCalibration calibration) {
        this.hardwareMap = hardwareMap;
        this.calibration = calibration;
        motorA = hardwareMap.get(DcMotorEx.class, MOTOR_A_NAME);
        motorB = hardwareMap.get(DcMotorEx.class, MOTOR_B_NAME);
        motorA.setDirection(MOTOR_A_DIRECTION);
        motorB.setDirection(MOTOR_B_DIRECTION);
        motorA.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motorB.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motorA.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        motorB.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        ticksPerMotorRevolutionA = motorA.getMotorType().getTicksPerRev();
        ticksPerMotorRevolutionB = motorB.getMotorType().getTicksPerRev();
        if (ticksPerMotorRevolutionA <= 0 || ticksPerMotorRevolutionB <= 0) {
            throw new IllegalStateException("Both shooter motor types must define encoder ticks/revolution");
        }
    }

    public void setCalibration(ShooterCalibration calibration) { this.calibration = calibration; }
    public void setTargetRpm(double targetRpm) { this.targetRpm = Math.max(0.0, targetRpm); }

    public void update() {
        double now = System.nanoTime() * 1e-9;
        double dt = previousTimeSeconds == 0.0 ? 0.02 : now - previousTimeSeconds;
        previousTimeSeconds = now;
        dt = clamp(dt, 0.001, 0.100);

        motorARpm = ticksPerSecondToFlywheelRpm(motorA.getVelocity(), ticksPerMotorRevolutionA);
        motorBRpm = ticksPerSecondToFlywheelRpm(motorB.getVelocity(), ticksPerMotorRevolutionB);
        measuredRpm = (motorARpm + motorBRpm) / 2.0;
        double rawAcceleration = (measuredRpm - previousMeasuredRpm) / dt;
        previousMeasuredRpm = measuredRpm;
        double filterWeight = dt / (ACCELERATION_FILTER_SECONDS + dt);
        filteredAccelerationRpmPerSecond += filterWeight
                * (rawAcceleration - filteredAccelerationRpmPerSecond);

        double oldProfiledTarget = profiledTargetRpm;
        double maximumChange = Math.max(0.0, calibration.maxAccelerationRpmPerSecond) * dt;
        profiledTargetRpm += clamp(targetRpm - profiledTargetRpm, -maximumChange, maximumChange);
        double targetAcceleration = (profiledTargetRpm - oldProfiledTarget) / dt;
        errorRpm = profiledTargetRpm - measuredRpm;

        if (targetRpm <= 0.0 && profiledTargetRpm <= 1.0) {
            profiledTargetRpm = 0.0;
            integralVoltage = 0.0;
            requestedVolts = 0.0;
        } else {
            if (Math.abs(errorRpm) <= calibration.integralZoneRpm) {
                integralVoltage += calibration.kI * errorRpm * dt;
                integralVoltage = clamp(integralVoltage,
                        -calibration.integralLimitVolts, calibration.integralLimitVolts);
            } else {
                integralVoltage = 0.0;
            }
            double feedforwardVolts = calibration.kS * Math.signum(profiledTargetRpm)
                    + calibration.kV * profiledTargetRpm
                    + calibration.kA * targetAcceleration;
            double feedbackVolts = calibration.kP * errorRpm + integralVoltage
                    - calibration.kD * filteredAccelerationRpmPerSecond;
            requestedVolts = feedforwardVolts + feedbackVolts;
        }

        appliedPower = clamp(requestedVolts / getBatteryVoltage(), -1.0, 1.0);
        motorA.setPower(appliedPower);
        motorB.setPower(appliedPower);

        if (targetRpm > 0.0
                && Math.abs(targetRpm - measuredRpm) <= calibration.readyToleranceRpm
                && Math.abs(filteredAccelerationRpmPerSecond)
                <= calibration.readyAccelerationRpmPerSecond) {
            readyDurationSeconds += dt;
        } else {
            readyDurationSeconds = 0.0;
        }
    }

    public void resetController() {
        profiledTargetRpm = measuredRpm;
        integralVoltage = 0.0;
        filteredAccelerationRpmPerSecond = 0.0;
        readyDurationSeconds = 0.0;
        previousTimeSeconds = 0.0;
    }

    public void stop() {
        targetRpm = 0.0;
        profiledTargetRpm = 0.0;
        integralVoltage = 0.0;
        requestedVolts = 0.0;
        appliedPower = 0.0;
        motorA.setPower(0.0);
        motorB.setPower(0.0);
    }

    public boolean isReady() { return readyDurationSeconds >= calibration.readyTimeSeconds; }
    public boolean encodersAgree() {
        double reference = Math.max(250.0, (Math.abs(motorARpm) + Math.abs(motorBRpm)) / 2.0);
        return Math.abs(motorARpm - motorBRpm) / reference <= 0.10;
    }
    public double getTargetRpm() { return targetRpm; }
    public double getProfiledTargetRpm() { return profiledTargetRpm; }
    public double getMeasuredRpm() { return measuredRpm; }
    public double getMotorARpm() { return motorARpm; }
    public double getMotorBRpm() { return motorBRpm; }
    public double getErrorRpm() { return errorRpm; }
    public double getAccelerationRpmPerSecond() { return filteredAccelerationRpmPerSecond; }
    public double getRequestedVolts() { return requestedVolts; }
    public double getAppliedPower() { return appliedPower; }
    public double getBatteryVoltage() {
        double result = Double.POSITIVE_INFINITY;
        for (VoltageSensor sensor : hardwareMap.voltageSensor) {
            double voltage = sensor.getVoltage();
            if (voltage > 0.0) result = Math.min(result, voltage);
        }
        return Double.isFinite(result) ? result : NOMINAL_BATTERY_VOLTS;
    }

    private double ticksPerSecondToFlywheelRpm(double ticksPerSecond, double ticksPerRevolution) {
        return ticksPerSecond * 60.0 / ticksPerRevolution * calibration.motorToFlywheelRatio;
    }
    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
