package com.kuriosityrobotics.firstforward.robot.opmodes.auto;

import static com.kuriosityrobotics.firstforward.robot.util.math.MathUtil.angleWrap;

import android.os.SystemClock;

import com.kuriosityrobotics.firstforward.robot.Robot;
import com.kuriosityrobotics.firstforward.robot.modules.outtake.OuttakeModule;
import com.kuriosityrobotics.firstforward.robot.pathfollow.Action;
import com.kuriosityrobotics.firstforward.robot.pathfollow.PurePursuit;
import com.kuriosityrobotics.firstforward.robot.pathfollow.VelocityLock;
import com.kuriosityrobotics.firstforward.robot.pathfollow.WayPoint;
import com.kuriosityrobotics.firstforward.robot.util.math.Point;
import com.kuriosityrobotics.firstforward.robot.util.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import java.util.ArrayList;

//@Disabled
@com.qualcomm.robotcore.eventloop.opmode.Autonomous
public class BlueCycle extends LinearOpMode {

    public static final Pose BLUE_START_W = Pose.fieldMirror(9.75, 64.5, Math.toRadians(-90)); //start near warehouse
    public static final Pose FIRST_WOBBLE = Pose.fieldMirror(27, 73, Math.toRadians(-110));

    public static final Pose BLUE_WOBBLE_W = Pose.fieldMirror(24, 73, Math.toRadians(-110));
    public static final Pose BLUE_WOBBLE_WALL_POINT = Pose.fieldMirror(7.5, 68, Math.toRadians(180));


    public static final Pose BLUE_BETWEEN_WOBBLE_WALLGAP = Pose.fieldMirror(7, 62.5, Math.toRadians(180));
    public static final Pose BLUE_WALL_GAP = Pose.fieldMirror(7, 46.5, Math.toRadians(180));
    public static Pose blueWarehouse = Pose.fieldMirror(8, 33, Math.toRadians(175));

    public static final Point BLUE_EXIT_WALLGAP = Point.fieldMirror(9, 64);

    public void runOpMode() {
        Robot robot = new Robot(hardwareMap, telemetry, this);

        Robot.setBlue(true);
        Robot.setCarousel(false);

        robot.resetPose(BLUE_START_W);

        AutoPaths.calibrateVuforia(robot);

        waitForStart();

        long startTime = SystemClock.elapsedRealtime();

        robot.resetPose(BLUE_START_W);

        OuttakeModule.VerticalSlideLevel detection = AutoPaths.awaitBarcodeDetection(robot);

        ArrayList<Action> wobbleActions = new ArrayList<>();
        wobbleActions.add(robot.getOuttakeModule().dumpOuttakeAction());
        PurePursuit blueStartwToWobble = new PurePursuit(new WayPoint[]{
                new WayPoint(BLUE_START_W),
                new WayPoint(BLUE_START_W.between(BLUE_WOBBLE_W)),
                new WayPoint(FIRST_WOBBLE, 0)
        }, 4);

        PurePursuit wobbleToWarehouse = new PurePursuit(new WayPoint[]{
                new WayPoint(BLUE_WOBBLE_W, new VelocityLock(15, false)),
                new WayPoint(BLUE_BETWEEN_WOBBLE_WALLGAP, new VelocityLock(18
                        , true)),//, 0.7 * MotionProfile.ROBOT_MAX_VEL, new ArrayList<>()),
                new WayPoint(BLUE_WALL_GAP),//, 0.55 * MotionProfile.ROBOT_MAX_VEL, new ArrayList<>()),
                new WayPoint(blueWarehouse, AutoPaths.INTAKE_VELO)
        }, 4);

        PurePursuit wobbleToWarehouseOdometryOnly = new PurePursuit(new WayPoint[]{
                new WayPoint(BLUE_WOBBLE_W, new VelocityLock(25, true)),
                new WayPoint(BLUE_WOBBLE_WALL_POINT, new VelocityLock(25, true)),
                new WayPoint(BLUE_BETWEEN_WOBBLE_WALLGAP, new VelocityLock(22, true)),//, 0.7 * MotionProfile.ROBOT_MAX_VEL, new ArrayList<>()),
                new WayPoint(BLUE_WALL_GAP),//, 0.55 * MotionProfile.ROBOT_MAX_VEL, new ArrayList<>()),
                new WayPoint(blueWarehouse, AutoPaths.INTAKE_VELO)
        }, 4);

        ArrayList<Action> exitActions = new ArrayList<>();
        exitActions.add(robot.getOuttakeModule().extendOuttakeAction(OuttakeModule.VerticalSlideLevel.TOP));
        exitActions.add(robot.getIntakeModule().intakePowerAction(0));
        PurePursuit warehouseToWobble = new PurePursuit(new WayPoint[]{
                new WayPoint(blueWarehouse),
                new WayPoint(BLUE_WALL_GAP),//,  0.7 * MotionProfile.ROBOT_MAX_VEL, new ArrayList<>()),
                new WayPoint(BLUE_EXIT_WALLGAP),//,  0.55 * MotionProfile.ROBOT_MAX_VEL, new ArrayList<>()),
                new WayPoint(BLUE_WOBBLE_W, 0)
        }, true, 4);

        robot.followPath(blueStartwToWobble);

        long startSleep = SystemClock.elapsedRealtime();
//        AutoPaths.waitForVuforia(robot, this, 1250, Pose.flipped(0.25, 0, 0));

        if (detection == OuttakeModule.VerticalSlideLevel.DOWN_NO_EXTEND) {
            sleep(500);
        } else {
            sleep(500);
        }
        assert robot.getVisionThread().getVuforiaLocalizationConsumer() != null;
        boolean sawFirst = robot.getVisionThread().getVuforiaLocalizationConsumer().getLastAcceptedTime() >= startSleep;

        if (sawFirst) {
            robot.followPath(wobbleToWarehouse);
        } else {
            AutoPaths.wallRidePath(robot, wobbleToWarehouseOdometryOnly);
        }

        int numCycles = 4;
        for (int i = 0; i < numCycles; i++) {
            if ((30*1000) - (SystemClock.elapsedRealtime() - startTime) < 6000) {
                return;
            }

            Pose intakeVary;

            intakeVary = Pose.relativeMirror(1.5*i, -4, Math.toRadians(-18));

//            AutoPaths.intakePath(robot, blueWarehouse.add(intakeVary), 4500);

//            if (blueWarehouse.y > 7.5)
            if (i % 2 == 1) {
                blueWarehouse = blueWarehouse.add(Pose.relativeMirror(0, -2, 0));
            }

            if (sawFirst) {
                PurePursuit backToWobble = new PurePursuit(new WayPoint[]{
                        new WayPoint(robot.getPose()),
                        new WayPoint(BLUE_WALL_GAP, new VelocityLock(40, false)),//,  0.7 * MotionProfile.ROBOT_MAX_VEL, new ArrayList<>()),
                        new WayPoint(BLUE_EXIT_WALLGAP),//,  0.55 * MotionProfile.ROBOT_MAX_VEL, new ArrayList<>()),
                        new WayPoint(BLUE_EXIT_WALLGAP.x-7,BLUE_EXIT_WALLGAP.y+2),
                        new WayPoint(BLUE_WOBBLE_W, 0)
                }, true, 4);

                robot.followPath(backToWobble);
            } else {
                PurePursuit backToWobble = new PurePursuit(new WayPoint[]{
                        new WayPoint(robot.getPose()),
                        new WayPoint(BLUE_WALL_GAP.add(Pose.relativeMirror(-1, 0, 0)),  new VelocityLock(40, true)),//,  0.7 * MotionProfile.ROBOT_MAX_VEL, new ArrayList<>()),
                        new WayPoint(BLUE_EXIT_WALLGAP.add(Pose.relativeMirror(-1, 0, 0))),//,  0.55 * MotionProfile.ROBOT_MAX_VEL, new ArrayList<>()),
                        new WayPoint(BLUE_EXIT_WALLGAP.x-7,BLUE_EXIT_WALLGAP.y+2),
                        new WayPoint(BLUE_WOBBLE_W.add(Pose.relativeMirror(-1, -3, 0)), 0)
                }, true, 4);

                AutoPaths.wallRidePath(robot, backToWobble);
            }
            startSleep = SystemClock.elapsedRealtime();

            if (sawFirst) {
                AutoPaths.waitForVuforia(robot, this, 250, Pose.relativeMirror(0, 0, 0));
            } else {
                sleep(150);
            }

            assert robot.getVisionThread().getVuforiaLocalizationConsumer() != null;
            sawFirst = robot.getVisionThread().getVuforiaLocalizationConsumer().getLastAcceptedTime() >= startSleep;

            if (sawFirst) {
                robot.followPath(wobbleToWarehouse);
            } else {
                AutoPaths.wallRidePath(robot, wobbleToWarehouseOdometryOnly);
            }
        }
    }
}