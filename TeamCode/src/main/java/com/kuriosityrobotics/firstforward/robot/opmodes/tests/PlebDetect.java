package com.kuriosityrobotics.firstforward.robot.opmodes.tests;

import com.kuriosityrobotics.firstforward.robot.LocationProvider;
import com.kuriosityrobotics.firstforward.robot.util.math.Pose;
import com.kuriosityrobotics.firstforward.robot.vision.ManagedCamera;
import com.kuriosityrobotics.firstforward.robot.vision.minerals.FreightDetectorConsumer;
import com.kuriosityrobotics.firstforward.robot.vision.minerals.FreightDetectorHelper;
import com.kuriosityrobotics.firstforward.robot.vision.minerals.PinholeCamera;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;

//@Disabled
@com.qualcomm.robotcore.eventloop.opmode.TeleOp
public class PlebDetect extends LinearOpMode {
    @Override
    public void runOpMode() {
        var detector = new FreightDetectorConsumer(LocationProvider.of(Pose.ZERO, Pose.ZERO), PinholeCamera.create(), false);
        var managedCamera = new ManagedCamera(hardwareMap.get(WebcamName.class, "Webcam 1"), LocationProvider.of(Pose.ZERO, Pose.ZERO), detector);

        waitForStart();
        while (opModeIsActive()) {
            detector.getTelemetryData().forEach(telemetry::addLine);
            telemetry.update();
            sleep(100);
        }
    }
}
