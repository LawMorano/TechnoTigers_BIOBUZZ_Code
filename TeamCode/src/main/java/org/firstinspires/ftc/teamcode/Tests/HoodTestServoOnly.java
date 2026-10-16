package org.firstinspires.ftc.teamcode.Tests;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Servo;

@TeleOp(name = "Hood Test (Servo Only)", group = "Test")
@Disabled
public class HoodTestServoOnly extends OpMode {
    private Servo hood;

    private double pos = 0.50;
    private double step = 0.005;
    private boolean lastUp;
    private boolean lastDown;
    private boolean lastA;
    private boolean lastB;
    private boolean lastX;
    private boolean lastY;
    private boolean lastLB;
    private boolean lastRB;

    private static final double PRESET_SAFE = 0.22;
    private static final double PRESET_CLOSE = 0.25;
    private static final double PRESET_MID = 0.525;
    private static final double PRESET_FAR = 0.80;

    @Override
    public void init() {
        hood = hardwareMap.get(Servo.class, "Shooter hood");
        hood.setPosition(pos);
    }

    @Override
    public void loop() {
        boolean lb = gamepad1.left_bumper;
        boolean rb = gamepad1.right_bumper;
        if (lb && !lastLB) {
            step = Math.max(0.001, step / 2.0);
        }
        if (rb && !lastRB) {
            step = Math.min(0.05, step * 2.0);
        }
        lastLB = lb;
        lastRB = rb;

        boolean up = gamepad1.dpad_up;
        boolean down = gamepad1.dpad_down;
        if (up && !lastUp) {
            pos += step;
        }
        if (down && !lastDown) {
            pos -= step;
        }
        lastUp = up;
        lastDown = down;

        boolean a = gamepad1.a;
        boolean b = gamepad1.b;
        boolean x = gamepad1.x;
        boolean y = gamepad1.y;
        if (a && !lastA) {
            pos = PRESET_CLOSE;
        }
        if (b && !lastB) {
            pos = PRESET_MID;
        }
        if (x && !lastX) {
            pos = PRESET_FAR;
        }
        if (y && !lastY) {
            pos = PRESET_SAFE;
        }
        lastA = a;
        lastB = b;
        lastX = x;
        lastY = y;

        pos = Math.max(0.0, Math.min(1.0, pos));
        hood.setPosition(pos);

        telemetry.addData("Hood Servo Pos", "%.3f", pos);
        telemetry.addData("Step", "%.4f (LB=halve, RB=double)", step);
        telemetry.addLine("Dpad Up/Down = bump | A=Close B=Mid X=Far Y=Safe");
        telemetry.update();
    }
}
