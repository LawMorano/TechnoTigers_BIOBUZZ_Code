package org.firstinspires.ftc.teamcode.TeleOp;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.pedro.Constants;

/**
 * Robot-centric mecanum drive controlled by gamepad 1.
 *
 * Controls:
 * - Left stick up/down: drive forward/backward
 * - Left stick left/right: strafe
 * - Right stick left/right: turn
 * - A: turn both intake motors on
 * - B: turn both intake motors off
 * - Y: turn both shooter motors on
 * - X: turn both shooter motors off
 * - D-pad left: switch between the close and far red goals
 * - Start: set the robot center to the (8, 8) recenter point
 *
 * Pedro Pathing controls FL, FR, BL, BR, and pinpoint through Constants.
 * Other hardware names: intake1, intake2, TopShooter, BottomShooter,
 * LEFT-TurretServo, and RIGHT-TurretServo.
 */
@TeleOp(name = "MainTele", group = "Drive")
public class MainTele extends LinearOpMode {

    private static final double INTAKE_POWER = 0.8;
    private static final double SHOOTER_POWER = 0.9;

    private static final double CLOSE_RED_GOAL_X = 58.0;
    private static final double CLOSE_RED_GOAL_Y = 52.0;
    private static final double FAR_RED_GOAL_X = 58.0;
    private static final double FAR_RED_GOAL_Y = 94.0;

    private static final double RECENTER_X = 8.0;
    private static final double RECENTER_Y = 8.0;

    private static final String LEFT_TURRET_NAME = "LEFT-TurretServo";
    private static final String RIGHT_TURRET_NAME = "RIGHT-TurretServo";

    // Calibration shared with TurretRecenterAndLimitsTest.
    private static final double LEFT_SERVO_DIRECTION = +1.0;
    private static final double RIGHT_SERVO_DIRECTION = +1.0;
    private static final double LEFT_CENTER_POSITION = 0.500;
    private static final double RIGHT_CENTER_POSITION = 0.500;
    private static final double SERVO_RANGE_DEGREES = 355.0;
    private static final double TURRET_DEGREES_PER_SERVO_DEGREE =
            (60.0 / 15.0) * (25.0 / 59.0);
    private static final double SERVO_POSITION_PER_TURRET_DEGREE =
            1.0 / (SERVO_RANGE_DEGREES * TURRET_DEGREES_PER_SERVO_DEGREE);

    // Positive Pedro angles are counterclockwise. Change the sign only if the
    // physical turret tracks in the mirrored direction.
    private static final double FIELD_ANGLE_TO_TURRET_SIGN = +1.0;
    private static final double TURRET_ZERO_OFFSET_DEGREES = 0.0;

    // Conservative mechanical limits from TurretRecenterAndLimitsTest.
    private static final double TURRET_MIN_DEGREES = -90.0;
    private static final double TURRET_MAX_DEGREES = 90.0;

    private Follower follower;
    private DcMotor intake1;
    private DcMotor intake2;
    private Servo turretLeft;
    private Servo turretRight;

    private DcMotor shooterTop;
    private DcMotor shooterBottom;

    private boolean useFarRedGoal;
    private boolean previousDpadLeft;

    @Override
    public void runOpMode() {
        // Send every telemetry update to both the Driver Station and FTC Dashboard.
        telemetry = new MultipleTelemetry(
                telemetry,
                FtcDashboard.getInstance().getTelemetry());

        follower = Constants.create(hardwareMap);

        intake1 = hardwareMap.get(DcMotor.class, "intake1");
        intake2 = hardwareMap.get(DcMotor.class, "intake2");
        shooterTop = hardwareMap.get(DcMotor.class, "TopShooter");
        shooterBottom = hardwareMap.get(DcMotor.class, "BottomShooter");
        turretLeft = hardwareMap.get(Servo.class, LEFT_TURRET_NAME);
        turretRight = hardwareMap.get(Servo.class, RIGHT_TURRET_NAME);

        applyTurretAngle(0.0);

        // Reverse one intake here if the two motors need opposite directions.
        intake1.setDirection(DcMotorSimple.Direction.REVERSE);
        intake2.setDirection(DcMotorSimple.Direction.REVERSE);
        shooterTop.setDirection(DcMotorSimple.Direction.FORWARD);
        shooterBottom.setDirection(DcMotorSimple.Direction.REVERSE);

        intake1.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        intake2.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        shooterTop.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        shooterBottom.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        shooterTop.setPower(0.0);
        shooterBottom.setPower(0.0);

        telemetry.addLine("MainTele ready");
        telemetry.addLine("Pedro robot-centric drive enabled");
        telemetry.addLine("A: intakes on | B: intakes off");
        telemetry.addLine("Y: shooters on | X: shooters off");
        telemetry.addLine("D-pad left: switch red goal");
        telemetry.addLine("Start: recenter robot at (8, 8)");
        telemetry.update();
        waitForStart();

        while (opModeIsActive()) {
            follower.manual(
                    -gamepad1.left_stick_y,
                    gamepad1.left_stick_x,
                    gamepad1.right_stick_x
            );
            follower.update();

            boolean recenteredThisLoop = gamepad1.startWasPressed();
            if (recenteredThisLoop) {
                Pose currentPose = follower.pose();
                follower.setPose(new Pose(
                        RECENTER_X,
                        RECENTER_Y,
                        currentPose.heading()));
            }

            boolean dpadLeft = gamepad1.dpad_left;
            if (dpadLeft && !previousDpadLeft) {
                useFarRedGoal = !useFarRedGoal;
            }
            previousDpadLeft = dpadLeft;

            Pose robotPose = follower.pose();
            double goalX = useFarRedGoal ? FAR_RED_GOAL_X : CLOSE_RED_GOAL_X;
            double goalY = useFarRedGoal ? FAR_RED_GOAL_Y : CLOSE_RED_GOAL_Y;

            double fieldAngleToGoal = Math.atan2(
                    goalY - robotPose.y(),
                    goalX - robotPose.x());
            double robotRelativeAngle = normalizeRadians(
                    fieldAngleToGoal - robotPose.heading());
            double requestedTurretAngleDegrees = TURRET_ZERO_OFFSET_DEGREES
                    + FIELD_ANGLE_TO_TURRET_SIGN * Math.toDegrees(robotRelativeAngle);
            double commandedTurretAngleDegrees = clamp(
                    requestedTurretAngleDegrees,
                    TURRET_MIN_DEGREES,
                    TURRET_MAX_DEGREES);

            applyTurretAngle(commandedTurretAngleDegrees);

            if (gamepad1.aWasPressed()) {
                intake1.setPower(INTAKE_POWER);
                intake2.setPower(INTAKE_POWER);
            }
            if (gamepad1.bWasPressed()) {
                intake1.setPower(0);
                intake2.setPower(0);
            }
            if (gamepad1.yWasPressed()) {
                shooterTop.setPower(SHOOTER_POWER);
                shooterBottom.setPower(SHOOTER_POWER);
            }
            if (gamepad1.xWasPressed()) {
                shooterTop.setPower(0);
                shooterBottom.setPower(0);
            }

            // Keep these values numeric so Dashboard can plot and export them.
            telemetry.addData("robotXIn", robotPose.x());
            telemetry.addData("robotYIn", robotPose.y());
            telemetry.addData("robotHeadingDeg", Math.toDegrees(robotPose.heading()));
            telemetry.addData("Recenter point (Start)", "(%.1f, %.1f) in",
                    RECENTER_X, RECENTER_Y);
            if (recenteredThisLoop) {
                telemetry.addLine("Pose recentered; heading preserved");
            }
            telemetry.addData("Tracking", useFarRedGoal
                    ? "FAR RED (58, 94)"
                    : "CLOSE RED (58, 52)");
            telemetry.addData("turretRequestedDeg", requestedTurretAngleDegrees);
            telemetry.addData("turretCommandedDeg", commandedTurretAngleDegrees);
            telemetry.addData("Turret at limit",
                    requestedTurretAngleDegrees != commandedTurretAngleDegrees);
            telemetry.update();
        }

        follower.stop();
        intake1.setPower(0.0);
        intake2.setPower(0.0);
        shooterTop.setPower(0.0);
        shooterBottom.setPower(0.0);
    }

    private void applyTurretAngle(double angleDegrees) {
        double servoDelta = angleDegrees * SERVO_POSITION_PER_TURRET_DEGREE;
        double leftPosition = LEFT_CENTER_POSITION + LEFT_SERVO_DIRECTION * servoDelta;
        double rightPosition = RIGHT_CENTER_POSITION + RIGHT_SERVO_DIRECTION * servoDelta;

        turretLeft.setPosition(clamp(leftPosition, 0.0, 1.0));
        turretRight.setPosition(clamp(rightPosition, 0.0, 1.0));
    }

    private static double normalizeRadians(double angle) {
        return Math.atan2(Math.sin(angle), Math.cos(angle));
    }

    private static double clamp(double value, double low, double high) {
        return Math.max(low, Math.min(high, value));
    }
}
