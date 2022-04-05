package com.kuriosityrobotics.firstforward.robot.sensors;

import static com.kuriosityrobotics.firstforward.robot.util.Constants.Field.FULL_FIELD;
import static com.kuriosityrobotics.firstforward.robot.util.math.MathUtil.rotate;
import static java.lang.Math.PI;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.toRadians;

import com.kuriosityrobotics.firstforward.robot.LocationProvider;
import com.kuriosityrobotics.firstforward.robot.modules.Module;
import com.kuriosityrobotics.firstforward.robot.sensors.kf.ExtendedKalmanFilter;
import com.kuriosityrobotics.firstforward.robot.util.math.Point;
import com.kuriosityrobotics.firstforward.robot.util.math.Pose;
import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.apache.commons.geometry.euclidean.twod.Lines;
import org.apache.commons.geometry.euclidean.twod.Ray;
import org.apache.commons.geometry.euclidean.twod.Segment;
import org.apache.commons.geometry.euclidean.twod.Vector2D;
import org.apache.commons.numbers.core.Precision;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.ojalgo.matrix.Primitive64Matrix;

import kotlin.NotImplementedError;

public class DistanceSensors implements Module {
    public static final double SENSOR_X_DISPLACEMENT = 5.354;
    public static final double BACK_SENSORS_Y_DISPLACEMENT = 5.6345;
    public static final double FRONT_SENSORS_Y_DISPLACEMENT = 2.9505;
    static final Precision.DoubleEquivalence p = Precision.doubleEquivalenceOfEpsilon(.05);
    private final LocationProvider locationProvider;
    private final ExtendedKalmanFilter filter;

    private final DistanceSensor frontLeft, backLeft, frontRight, backRight;

    public DistanceSensors(HardwareMap hardwareMap, LocationProvider locationProvider, ExtendedKalmanFilter filter) {
        this.locationProvider = locationProvider;
        this.filter = filter;

        this.frontLeft = hardwareMap.get(DistanceSensor.class, "frontLeft");
        this.backLeft = hardwareMap.get(DistanceSensor.class, "backLeft");
        this.frontRight = hardwareMap.get(DistanceSensor.class, "frontRight");
        this.backRight = hardwareMap.get(DistanceSensor.class, "backRight");
    }

    private static Point c(Vector2D p) {
        return new Point(p.getX(), p.getY());
    }

    private static Vector2D c(Point p) {
        return Vector2D.of(p.x, p.y);
    }

    public void update() {
        {
            var bestWallLeft = SensorPair.LEFT.bestWall(locationProvider.getPose());
            if (bestWallLeft != null)
                SensorPair.LEFT.correctOnFilter(
                        filter,
                        frontLeft.getDistance(DistanceUnit.INCH),
                        backLeft.getDistance(DistanceUnit.INCH),
                        bestWallLeft
                );
        }

        {
            var bestWallRight = SensorPair.RIGHT.bestWall(locationProvider.getPose());
            if (bestWallRight != null)
                SensorPair.RIGHT.correctOnFilter(
                        filter,
                        frontRight.getDistance(DistanceUnit.INCH),
                        backRight.getDistance(DistanceUnit.INCH),
                        bestWallRight
                );
        }
    }

    @Override
    public boolean isOn() {
        return true;
    }

    @Override
    public String getName() {
        return "DistanceSensors";
    }

    public enum SensorPair {
        RIGHT(
                Vector2D.of(-SENSOR_X_DISPLACEMENT, -BACK_SENSORS_Y_DISPLACEMENT),
                Vector2D.of(-SENSOR_X_DISPLACEMENT, FRONT_SENSORS_Y_DISPLACEMENT)),
        LEFT(Vector2D.of(SENSOR_X_DISPLACEMENT, -BACK_SENSORS_Y_DISPLACEMENT),
                Vector2D.of(SENSOR_X_DISPLACEMENT, FRONT_SENSORS_Y_DISPLACEMENT));


        private final Vector2D offset1, offset2;

        SensorPair(Vector2D offset1, Vector2D offset2) {
            this.offset1 = offset1;
            this.offset2 = offset2;
        }

        public static double getWallHeading(double frontSensor, double backSensor) {
            return atan2(frontSensor - backSensor, FRONT_SENSORS_Y_DISPLACEMENT + BACK_SENSORS_Y_DISPLACEMENT);
        }

        public static double getWallDistance(double frontSensor, double backSensor) {
            double wallHeading = getWallHeading(frontSensor, backSensor);
            double d1 = frontSensor * cos(wallHeading);
            double d2 = backSensor * cos(wallHeading);
            double A = FRONT_SENSORS_Y_DISPLACEMENT / (FRONT_SENSORS_Y_DISPLACEMENT + BACK_SENSORS_Y_DISPLACEMENT);
            double B = BACK_SENSORS_Y_DISPLACEMENT / (FRONT_SENSORS_Y_DISPLACEMENT + BACK_SENSORS_Y_DISPLACEMENT);

            double d = A * d1 + B * d2;
            double dOffset = SENSOR_X_DISPLACEMENT / cos(wallHeading);

            return d + dOffset;
        }

        public void correctOnFilter(ExtendedKalmanFilter filter, double frontSensor, double backSensor, Wall wall) {
            var wallHeading = getWallHeading(frontSensor, backSensor);
            var wallDistance = getWallDistance(frontSensor, backSensor);

            double heading = 0;

            switch (wall) {
                case FRONT:
                    wallDistance = FULL_FIELD - wallDistance;
                    if (this == LEFT) {
                        heading = PI / 2 + wallHeading;
                    } else {
                        heading = -PI / 2 - wallHeading;
                    }
                    break;
                case RIGHT:
                    wallDistance = FULL_FIELD - wallDistance;
                    if (this == LEFT) {
                        heading = PI + wallHeading;
                    } else {
                        heading = -wallHeading;
                    }
                    break;
                case BACK:
                    if (this == LEFT) {
                        heading = -PI / 2 + wallHeading;
                    } else {
                        heading = PI / 2 - wallHeading;
                    }
                    break;
                case LEFT:
                    if (this == LEFT) {
                        heading = wallHeading;
                    } else {
                        heading = PI - wallHeading;
                    }
                    break;
            }
            Primitive64Matrix H;
            if (wall == Wall.FRONT || wall == Wall.BACK) {
                H = Primitive64Matrix.FACTORY.rows(new double[][] {
                        {0, 1, 0}, // y
                        {0, 0, 1} // heading
                });
            } else {
                H = Primitive64Matrix.FACTORY.rows(new double[][] {
                        {1, 0, 0}, // x
                        {0, 0, 1} // heading
                });
            }

            filter.builder()
                    .mean(wallDistance, heading)
                    .stateToOutput(H)
                    .variance(.5, toRadians(4))
                    .correct();
        }

        public Wall bestWall(Pose robotPose) {
            var sensor1 = c(robotPose).add(rotate(offset1, robotPose.heading));
            var sensor2 = c(robotPose).add(rotate(offset2, robotPose.heading));

            var sensorHeading = this == RIGHT ? PI - robotPose.heading : -robotPose.heading;

            var sensor1Line = Lines.fromPointAndAngle(sensor1, sensorHeading, p);
            var sensor1Segment = sensor1Line.segment(sensor1, sensor1.add(sensor1Line.getDirection().multiply(10)));

            var sensor2Line = Lines.fromPointAndAngle(sensor2, sensorHeading, p);
            var sensor2Segment = sensor2Line.segment(sensor2, sensor2.add(sensor2Line.getDirection().multiply(10)));

            if (sensor1Segment.intersection(Wall.LEFT.segment) != null && sensor2Segment.intersection(Wall.LEFT.segment) != null)
                return Wall.LEFT;
            else if (sensor1Segment.intersection(Wall.RIGHT.segment) != null && sensor2Segment.intersection(Wall.RIGHT.segment) != null)
                return Wall.RIGHT;
            else if (sensor1Segment.intersection(Wall.FRONT.segment) != null && sensor2Segment.intersection(Wall.FRONT.segment) != null)
                return Wall.FRONT;
            else if (sensor1Segment.intersection(Wall.BACK.segment) != null && sensor2Segment.intersection(Wall.BACK.segment) != null)
                return Wall.BACK;
            else
                return null;
        }
    }

    public enum Wall {
        RIGHT(Lines.segmentFromPoints(Vector2D.of(0, 0), Vector2D.of(0, FULL_FIELD), p)),
        LEFT(Lines.segmentFromPoints(Vector2D.of(FULL_FIELD, 0), Vector2D.of(FULL_FIELD, FULL_FIELD), p)),
        FRONT(Lines.segmentFromPoints(Vector2D.of(0, FULL_FIELD), Vector2D.of(FULL_FIELD, FULL_FIELD), p)),
        BACK(Lines.segmentFromPoints(Vector2D.of(0, 0), Vector2D.of(FULL_FIELD, 0), p));

        public final Segment segment;

        Wall(Segment segment) {
            this.segment = segment;
        }
    }
}
