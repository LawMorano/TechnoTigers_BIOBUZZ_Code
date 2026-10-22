package org.firstinspires.ftc.teamcode.TeleOp;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.IMU;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;

/**
 * Robot-centric mecanum drive controlled by gamepad 1.
 *
 * Controls:
 * - Left stick up/down: drive forward/backward
 * - Left stick left/right: strafe
 * - Right stick left/right: turn
 * - A: turn both intake motors on
 * - B: turn both intake motors off
 *
 * Configure the motors on the Driver Station with these exact names:
 * frontLeft, frontRight, backLeft, backRight, intake1, and intake2.
 */
@TeleOp(name = "MainTele", group = "Drive")
public class MainTele extends LinearOpMode {

    private static final double INTAKE_POWER = 0.8;
    private DcMotor frontLeft;
    private DcMotor frontRight;
    private DcMotor backLeft;
    private DcMotor backRight;
    private DcMotor intake1;
    private DcMotor intake2;

    // Shooter, hood, and turret hardware from the disabled test OpModes.
    // Keep these commented out until the mechanisms are ready for TeleOp use.
    // private DcMotorEx shooterMotor;
    // private Servo shooterHood;
    // private Servo turretLeft;
    // private Servo turretRight;

    @Override
    public void runOpMode() {
        frontLeft = hardwareMap.get(DcMotor.class, "FL");
        frontRight = hardwareMap.get(DcMotor.class, "FR");
        backLeft = hardwareMap.get(DcMotor.class, "BL");
        backRight = hardwareMap.get(DcMotor.class, "BR");

        intake1 = hardwareMap.get(DcMotor.class, "intake1");
        intake2 = hardwareMap.get(DcMotor.class, "intake2");

        // Test hardware initialization (disabled for now).
        // shooterMotor = hardwareMap.get(DcMotorEx.class, "ShooterMotor");
        // shooterHood = hardwareMap.get(Servo.class, "Shooter hood");
        // turretLeft = hardwareMap.get(Servo.class, "LRotation");
        // turretRight = hardwareMap.get(Servo.class, "RRotation");

        frontLeft.setDirection(DcMotorSimple.Direction.REVERSE);
        backLeft.setDirection(DcMotorSimple.Direction.FORWARD);
        frontRight.setDirection(DcMotorSimple.Direction.FORWARD);
        backRight.setDirection(DcMotorSimple.Direction.REVERSE);

        // Reverse one intake here if the two motors need opposite directions.

        intake1.setDirection(DcMotorSimple.Direction.REVERSE);
        intake2.setDirection(DcMotorSimple.Direction.REVERSE);

        setDriveZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        intake1.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        intake2.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        double intakePower = 0.8;

        // Retrieve the IMU from the hardware map
        IMU imu = hardwareMap.get(IMU.class, "imu");
        // Adjust the orientation parameters to match your robot
        IMU.Parameters parameters = new IMU.Parameters(new RevHubOrientationOnRobot(
                RevHubOrientationOnRobot.LogoFacingDirection.LEFT,
                RevHubOrientationOnRobot.UsbFacingDirection.UP));
        // Without this, the REV Hub's orientation is assumed to be logo up / USB forward
        imu.initialize(parameters);

        telemetry.addLine("MainTele ready");
        telemetry.addLine("A: intakes on | B: intakes off");
        telemetry.update();
        waitForStart();

        while (opModeIsActive()) {
            double y = -gamepad1.left_stick_y; // Remember, Y stick value is reversed
            double x = gamepad1.left_stick_x * 1.1; // Counteract imperfect strafing
            double rx = gamepad1.right_stick_x;

            // Denominator is the largest motor power (absolute value) or 1
            // This ensures all the powers maintain the same ratio,
            // but only if at least one is out of the range [-1, 1]
            double denominator = Math.max(Math.abs(y) + Math.abs(x) + Math.abs(rx), 1);
            double frontLeftPower = (y + x + rx) / denominator;
            double backLeftPower = (y - x + rx) / denominator;
            double frontRightPower = (y - x - rx) / denominator;
            double backRightPower = (y + x - rx) / denominator;

            frontLeft.setPower(frontLeftPower);
            backLeft.setPower(backLeftPower);
            frontRight.setPower(frontRightPower);
            backRight.setPower(backRightPower);

            if (gamepad1.aWasPressed()) {
                intake1.setPower(INTAKE_POWER);
                intake2.setPower(INTAKE_POWER);
            }
            if (gamepad1.bWasPressed()) {
                intake1.setPower(0);
                intake2.setPower(0);
            }

            telemetry.addData("Front left", "%.2f", frontLeftPower);
            telemetry.addData("Front right", "%.2f", frontRightPower);
            telemetry.addData("Back left", "%.2f", backLeftPower);
            telemetry.addData("Back right", "%.2f", backRightPower);
            //telemetry.addData("Intakes", intakePower == 0.0 ? "OFF" : "ON");
            telemetry.update();
        }

        intake1.setPower(0.0);
        intake2.setPower(0.0);
    }

    private void setDriveZeroPowerBehavior(DcMotor.ZeroPowerBehavior behavior) {
        frontLeft.setZeroPowerBehavior(behavior);
        frontRight.setZeroPowerBehavior(behavior);
        backLeft.setZeroPowerBehavior(behavior);
        backRight.setZeroPowerBehavior(behavior);
    }
}
