package com.kuriosityrobotics.firstforward.robot.opmodes.tests.TestAutos;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Servo;

@TeleOp
public class PlebCamTurretTuner extends LinearOpMode {
    Servo pivot;

    double pivotPos = .1906;

    @Override
    public void runOpMode() {
        pivot = hardwareMap.get(Servo.class, "webcamPivot");

        waitForStart();

        pivot.setPosition(pivotPos);

        while (opModeIsActive()) {
            pivotPos += gamepad1.right_stick_y * 0.00005;
            pivot.setPosition(pivotPos);

            telemetry.addData("pivot pos: ", pivotPos);
            telemetry.update();
        }
    }
}