package org.firstinspires.ftc.teamcode.Tests;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

@TeleOp(name = "Turret Recenter and Limits Test", group = "Test")
public class TurretRecenterAndLimitsTest extends OpMode {
    private static final String LEFT_NAME = "LRotation";
    private static final String RIGHT_NAME = "RRotation";

    /*
     * ======================== CHANGE THESE DURING SETUP ========================
     * Both servo gears must turn in the same physical direction.
     *
     * Start with both values at +1.0. With the 60-tooth gears DISENGAGED, move
     * the stick slightly to the right. If one servo shaft turns the wrong way,
     * change ONLY that servo's direction from +1.0 to -1.0.
     *
     * Do not also call Servo.setDirection(REVERSE); use these constants as the
     * single place where direction is configured.
     */
    private static final double LEFT_SERVO_DIRECTION = +1.0;  // Change to -1.0 if needed.
    private static final double RIGHT_SERVO_DIRECTION = +1.0; // Change to -1.0 if needed.

    /*
     * Position that centers each servo before the gears are installed. Adjust
     * these separately if the two programmed neutral positions do not line up.
     */
    private static final double LEFT_CENTER_POSITION = 0.500;
    private static final double RIGHT_CENTER_POSITION = 0.500;

    /*
     * Mechanical transmission:
     *   60T servo gear -> 15T shaft gear -> 25T pulley -> 59T turret pulley
     *
     * Turret degrees per servo degree = (60 / 15) * (25 / 59) = 100 / 59.
     * Therefore one turret degree requires 0.59 servo degree.
     */
    private static final double SERVO_GEAR_TEETH = 60.0;
    private static final double SHAFT_GEAR_TEETH = 15.0;
    private static final double DRIVE_PULLEY_TEETH = 25.0;
    private static final double TURRET_PULLEY_TEETH = 59.0;

    // The Axon servos must be programmed for a 360-degree position range.
    private static final double SERVO_RANGE_DEGREES = 360.0;

    private static final double TURRET_DEGREES_PER_SERVO_DEGREE =
            (SERVO_GEAR_TEETH / SHAFT_GEAR_TEETH)
                    * (DRIVE_PULLEY_TEETH / TURRET_PULLEY_TEETH);

    private static final double SERVO_POSITION_PER_TURRET_DEGREE =
            1.0 / (SERVO_RANGE_DEGREES * TURRET_DEGREES_PER_SERVO_DEGREE);

    // Test-control settings.
    private static final double STICK_SPEED_DEGREES_PER_SECOND = 60.0;
    private static final double DPAD_STEP_DEGREES = 1.0;
    private static final double DEADZONE = 0.06;

    private Servo left;
    private Servo right;

    private final ElapsedTime loopTimer = new ElapsedTime();

    // Zero degrees is the position represented by LEFT/RIGHT_CENTER_POSITION.
    private double turretAngleDegrees = 0.0;

    // Conservative starting limits. Set new limits with A and B during testing.
    private double safeMinDegrees = -90.0;
    private double safeMaxDegrees = 90.0;
    private boolean enforceLimits = true;

    private boolean prevA;
    private boolean prevB;
    private boolean prevX;
    private boolean prevY;
    private boolean prevDpadLeft;
    private boolean prevDpadRight;

    @Override
    public void init() {
        left = hardwareMap.get(Servo.class, LEFT_NAME);
        right = hardwareMap.get(Servo.class, RIGHT_NAME);

        // Configure both as Full Range Servos (500-2500 us) in the FTC Robot
        // Configuration so that position 0.0-1.0 represents 360 degrees.
        applyTurretAngle(0.0);

        telemetry.addLine("Turret test READY");
        telemetry.addLine("WARNING: Center both servos before engaging the gears.");
        telemetry.addLine("Left stick X = move at 60 deg/s");
        telemetry.addLine("D-pad left/right = 1 degree per press");
        telemetry.addLine("X = return to 0 degrees");
        telemetry.addLine("Y = toggle saved limit enforcement");
        telemetry.addLine("A = save current angle as MIN");
        telemetry.addLine("B = save current angle as MAX");
        telemetry.update();
    }

    @Override
    public void start() {
        loopTimer.reset();
    }

    @Override
    public void loop() {
        double elapsedSeconds = Math.min(loopTimer.seconds(), 0.1);
        loopTimer.reset();

        boolean a = gamepad1.a;
        boolean b = gamepad1.b;
        boolean x = gamepad1.x;
        boolean y = gamepad1.y;
        boolean dpadLeft = gamepad1.dpad_left;
        boolean dpadRight = gamepad1.dpad_right;

        boolean aPressed = a && !prevA;
        boolean bPressed = b && !prevB;
        boolean xPressed = x && !prevX;
        boolean yPressed = y && !prevY;
        boolean dpadLeftPressed = dpadLeft && !prevDpadLeft;
        boolean dpadRightPressed = dpadRight && !prevDpadRight;

        if (xPressed) {
            turretAngleDegrees = 0.0;
        }

        if (yPressed) {
            enforceLimits = !enforceLimits;
        }

        double stick = gamepad1.left_stick_x;
        if (Math.abs(stick) < DEADZONE) {
            stick = 0.0;
        }
        turretAngleDegrees += stick * STICK_SPEED_DEGREES_PER_SECOND * elapsedSeconds;

        if (dpadLeftPressed) {
            turretAngleDegrees -= DPAD_STEP_DEGREES;
        }
        if (dpadRightPressed) {
            turretAngleDegrees += DPAD_STEP_DEGREES;
        }

        if (aPressed) {
            safeMinDegrees = Math.min(turretAngleDegrees, safeMaxDegrees);
        }
        if (bPressed) {
            safeMaxDegrees = Math.max(turretAngleDegrees, safeMinDegrees);
        }

        double servoLimitedMin = getServoLimitedMinAngle();
        double servoLimitedMax = getServoLimitedMaxAngle();

        // Always protect the servos' valid position range. The saved mechanical
        // limits are applied in addition when limit enforcement is enabled.
        double allowedMin = servoLimitedMin;
        double allowedMax = servoLimitedMax;
        if (enforceLimits) {
            allowedMin = Math.max(allowedMin, safeMinDegrees);
            allowedMax = Math.min(allowedMax, safeMaxDegrees);
        }

        turretAngleDegrees = clamp(turretAngleDegrees, allowedMin, allowedMax);
        applyTurretAngle(turretAngleDegrees);

        double servoDelta = turretAngleDegrees * SERVO_POSITION_PER_TURRET_DEGREE;
        double leftPosition = LEFT_CENTER_POSITION + LEFT_SERVO_DIRECTION * servoDelta;
        double rightPosition = RIGHT_CENTER_POSITION + RIGHT_SERVO_DIRECTION * servoDelta;

        telemetry.addLine("=== Turret Recenter and Limits Test ===");
        telemetry.addData("Turret target", "%.2f deg", turretAngleDegrees);
        telemetry.addData("Left servo position", "%.4f", leftPosition);
        telemetry.addData("Right servo position", "%.4f", rightPosition);
        telemetry.addData("Position per turret degree", "%.7f", SERVO_POSITION_PER_TURRET_DEGREE);
        telemetry.addData("Limits enforced (Y)", enforceLimits);
        telemetry.addData("Safe MIN (A)", "%.2f deg", safeMinDegrees);
        telemetry.addData("Safe MAX (B)", "%.2f deg", safeMaxDegrees);
        telemetry.addData("Servo-only range", "%.1f to %.1f deg",
                servoLimitedMin, servoLimitedMax);
        telemetry.update();

        prevA = a;
        prevB = b;
        prevX = x;
        prevY = y;
        prevDpadLeft = dpadLeft;
        prevDpadRight = dpadRight;
    }

    private void applyTurretAngle(double angleDegrees) {
        double servoDelta = angleDegrees * SERVO_POSITION_PER_TURRET_DEGREE;

        double leftPosition = LEFT_CENTER_POSITION + LEFT_SERVO_DIRECTION * servoDelta;
        double rightPosition = RIGHT_CENTER_POSITION + RIGHT_SERVO_DIRECTION * servoDelta;

        left.setPosition(clamp(leftPosition, 0.0, 1.0));
        right.setPosition(clamp(rightPosition, 0.0, 1.0));
    }

    private double getServoLimitedMinAngle() {
        return Math.max(
                getMinimumAngleForServo(LEFT_CENTER_POSITION, LEFT_SERVO_DIRECTION),
                getMinimumAngleForServo(RIGHT_CENTER_POSITION, RIGHT_SERVO_DIRECTION)
        );
    }

    private double getServoLimitedMaxAngle() {
        return Math.min(
                getMaximumAngleForServo(LEFT_CENTER_POSITION, LEFT_SERVO_DIRECTION),
                getMaximumAngleForServo(RIGHT_CENTER_POSITION, RIGHT_SERVO_DIRECTION)
        );
    }

    private double getMinimumAngleForServo(double centerPosition, double direction) {
        double angleAtPositionZero =
                (0.0 - centerPosition) / (direction * SERVO_POSITION_PER_TURRET_DEGREE);
        double angleAtPositionOne =
                (1.0 - centerPosition) / (direction * SERVO_POSITION_PER_TURRET_DEGREE);
        return Math.min(angleAtPositionZero, angleAtPositionOne);
    }

    private double getMaximumAngleForServo(double centerPosition, double direction) {
        double angleAtPositionZero =
                (0.0 - centerPosition) / (direction * SERVO_POSITION_PER_TURRET_DEGREE);
        double angleAtPositionOne =
                (1.0 - centerPosition) / (direction * SERVO_POSITION_PER_TURRET_DEGREE);
        return Math.max(angleAtPositionZero, angleAtPositionOne);
    }

    private double clamp(double value, double low, double high) {
        return Math.max(low, Math.min(high, value));
    }
}
