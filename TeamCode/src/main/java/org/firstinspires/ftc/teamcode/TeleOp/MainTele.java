package org.firstinspires.ftc.teamcode.TeleOp;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
// import com.qualcomm.robotcore.hardware.DcMotorEx;
// import com.qualcomm.robotcore.hardware.Servo;

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
@Disabled
public class MainTele extends LinearOpMode {

    private static final double INTAKE_POWER = 1.0;

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
        frontLeft = hardwareMap.get(DcMotor.class, "frontLeft");
        frontRight = hardwareMap.get(DcMotor.class, "frontRight");
        backLeft = hardwareMap.get(DcMotor.class, "backLeft");
        backRight = hardwareMap.get(DcMotor.class, "backRight");
        intake1 = hardwareMap.get(DcMotor.class, "intake1");
        intake2 = hardwareMap.get(DcMotor.class, "intake2");

        // Test hardware initialization (disabled for now).
        // shooterMotor = hardwareMap.get(DcMotorEx.class, "ShooterMotor");
        // shooterHood = hardwareMap.get(Servo.class, "Shooter hood");
        // turretLeft = hardwareMap.get(Servo.class, "LRotation");
        // turretRight = hardwareMap.get(Servo.class, "RRotation");

        frontLeft.setDirection(DcMotorSimple.Direction.REVERSE);
        backLeft.setDirection(DcMotorSimple.Direction.REVERSE);
        frontRight.setDirection(DcMotorSimple.Direction.FORWARD);
        backRight.setDirection(DcMotorSimple.Direction.FORWARD);

        // Reverse one intake here if the two motors need opposite directions.
        intake1.setDirection(DcMotorSimple.Direction.FORWARD);
        intake2.setDirection(DcMotorSimple.Direction.FORWARD);

        setDriveZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        intake1.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        intake2.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        double intakePower = 0.6;

        telemetry.addLine("MainTele ready");
        telemetry.addLine("A: intakes on | B: intakes off");
        telemetry.update();
        waitForStart();

        while (opModeIsActive()) {
            // FTC gamepad Y is negative when the stick is pushed forward.
            double forward = -gamepad1.left_stick_y;
            double strafe = gamepad1.left_stick_x;
            double turn = gamepad1.right_stick_x;

            // Normalize the wheel powers so none exceeds the valid [-1, 1] range.
            double denominator = Math.max(
                    Math.abs(forward) + Math.abs(strafe) + Math.abs(turn),
                    1.0
            );

            double frontLeftPower = (forward + strafe + turn) / denominator;
            double backLeftPower = (forward - strafe + turn) / denominator;
            double frontRightPower = (forward - strafe - turn) / denominator;
            double backRightPower = (forward + strafe - turn) / denominator;

            frontLeft.setPower(frontLeftPower);
            backLeft.setPower(backLeftPower);
            frontRight.setPower(frontRightPower);
            backRight.setPower(backRightPower);

            if (gamepad1.a) {
                intakePower = INTAKE_POWER;
            }
            if (gamepad1.b) {
                intakePower = 0.0;
            }

            intake1.setPower(intakePower);
            intake2.setPower(intakePower);

            telemetry.addData("Front left", "%.2f", frontLeftPower);
            telemetry.addData("Front right", "%.2f", frontRightPower);
            telemetry.addData("Back left", "%.2f", backLeftPower);
            telemetry.addData("Back right", "%.2f", backRightPower);
            telemetry.addData("Intakes", intakePower == 0.0 ? "OFF" : "ON");
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
