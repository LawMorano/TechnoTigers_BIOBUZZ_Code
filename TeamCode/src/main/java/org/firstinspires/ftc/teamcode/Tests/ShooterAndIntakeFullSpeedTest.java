package org.firstinspires.ftc.teamcode.Tests;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

@TeleOp(name = "Shooter + Intake Full Speed Test", group = "Tests")
public class ShooterAndIntakeFullSpeedTest extends LinearOpMode {

    private static final double FULL_POWER = 1.0;

    @Override
    public void runOpMode() {
        DcMotor shooterTop = hardwareMap.get(DcMotor.class, "TopShooter");
        DcMotor shooterBottom = hardwareMap.get(DcMotor.class, "BottomShooter");
        DcMotor intake1 = hardwareMap.get(DcMotor.class, "intake1");
        DcMotor intake2 = hardwareMap.get(DcMotor.class, "intake2");

        shooterTop.setDirection(DcMotorSimple.Direction.FORWARD);
        shooterBottom.setDirection(DcMotorSimple.Direction.REVERSE);
        intake1.setDirection(DcMotorSimple.Direction.REVERSE);
        intake2.setDirection(DcMotorSimple.Direction.REVERSE);

        shooterTop.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        shooterBottom.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        intake1.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        intake2.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        setPower(shooterTop, shooterBottom, 0.0);
        setPower(intake1, intake2, 0.0);

        telemetry.addLine("A: shooters full speed");
        telemetry.addLine("B: stop shooters");
        telemetry.addLine("X: intakes full speed");
        telemetry.addLine("Y: stop intakes");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {
            if (gamepad1.aWasPressed()) {
                setPower(shooterTop, shooterBottom, FULL_POWER);
            }
            if (gamepad1.bWasPressed()) {
                setPower(shooterTop, shooterBottom, 0.0);
            }
            if (gamepad1.xWasPressed()) {
                setPower(intake1, intake2, FULL_POWER);
            }
            if (gamepad1.yWasPressed()) {
                setPower(intake1, intake2, 0.0);
            }

            telemetry.addData("Shooters", shooterTop.getPower() == 0.0 ? "STOPPED" : "FULL SPEED");
            telemetry.addData("Intakes", intake1.getPower() == 0.0 ? "STOPPED" : "FULL SPEED");
            telemetry.update();
        }

        setPower(shooterTop, shooterBottom, 0.0);
        setPower(intake1, intake2, 0.0);
    }

    private static void setPower(DcMotor first, DcMotor second, double power) {
        first.setPower(power);
        second.setPower(power);
    }
}
