package com.kuriosityrobotics.firstforward.robot.opmodes.tests.outtake;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;

@Disabled
@TeleOp
public class PlebOuttakeVerticalSlideTuner extends LinearOpMode {
    DcMotorEx slide;

    int verticalSlideMotorPos = 0;

    @Override
    public void runOpMode() {
        slide = (DcMotorEx) hardwareMap.get(DcMotor.class, "otherLift");

        slide.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        slide.setTargetPosition(0);
        slide.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        slide.setPower(1);

        waitForStart();

        while (opModeIsActive()) {
            verticalSlideMotorPos += Math.signum(gamepad1.right_stick_y);
            slide.setTargetPosition(verticalSlideMotorPos);

            telemetry.addData("verticalSlideMotorPos: ", slide.getCurrentPosition());
            telemetry.update();
        }
    }
}
