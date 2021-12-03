package com.kuriosityrobotics.firstforward.robot.opmodes.tests;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Servo;

@Disabled
@TeleOp
public class PlebOuttakeLinkageTuner extends LinearOpMode {
    Servo linkage;

    double linkagePos = 0.5;

    @Override
    public void runOpMode() {
        linkage = hardwareMap.get(Servo.class, "linkage_servo");

        waitForStart();

        linkage.setPosition(linkagePos);

        while (opModeIsActive()) {
            linkagePos += gamepad1.right_stick_y * 0.00005;
            linkage.setPosition(linkagePos);

            telemetry.addData("linkage pos: ", linkagePos);
            telemetry.update();
        }
    }
}