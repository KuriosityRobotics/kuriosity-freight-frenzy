package com.kuriosityrobotics.firstforward.robot.modules;

import static com.kuriosityrobotics.firstforward.robot.util.Constants.Intake.GOBILDA_1620_PPR;
import static com.kuriosityrobotics.firstforward.robot.util.Constants.Intake.INTAKE_DEACCEL_SD;
import static com.kuriosityrobotics.firstforward.robot.util.Constants.Intake.INTAKE_LEFT_EXTENDED_POS;
import static com.kuriosityrobotics.firstforward.robot.util.Constants.Intake.INTAKE_LEFT_RETRACTED_POS;
import static com.kuriosityrobotics.firstforward.robot.util.Constants.Intake.INTAKE_OCCUPIED_SD;
import static com.kuriosityrobotics.firstforward.robot.util.Constants.Intake.INTAKE_RETRACT_TIME;
import static com.kuriosityrobotics.firstforward.robot.util.Constants.Intake.INTAKE_RIGHT_EXTENDED_POS;
import static com.kuriosityrobotics.firstforward.robot.util.Constants.Intake.INTAKE_RIGHT_RETRACTED_POS;
import static com.kuriosityrobotics.firstforward.robot.util.Constants.Intake.RING_BUFFER_CAPACITY;
import static com.kuriosityrobotics.firstforward.robot.util.Constants.Intake.RPM_EPSILON;

import android.os.SystemClock;

import com.kuriosityrobotics.firstforward.robot.Robot;
import com.kuriosityrobotics.firstforward.robot.debug.telemetry.Telemeter;
import com.kuriosityrobotics.firstforward.robot.math.MathUtil;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;

import org.apache.commons.collections4.BoundedCollection;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class IntakeModule implements Module, Telemeter {
    private static final double HOLD_POWER = 1;
    public volatile double lastSd = 0;

    private final CircularFifoQueue<Double> intakeRpmRingBuffer = new CircularFifoQueue<>(RING_BUFFER_CAPACITY);

    private final DcMotorEx intakeMotor;
    private final Servo extenderLeft;
    private final Servo extenderRight;

    private Robot robot;

    // states
    public volatile IntakePosition intakePosition = IntakePosition.EXTENDED;
    public volatile double intakePower;

    private Long intakerRetractionStartTime;
    private volatile boolean intakeOccupied = false;
    private boolean started = false;

    List<Double> avgRPMs;

    private boolean hasOccupationStatusChanged() {
        if (getRPM() < 500 || intakePower < 0.1 || intakeRpmRingBuffer.isEmpty())
            return false;

        avgRPMs = new ArrayList<>(RING_BUFFER_CAPACITY / 10);
        for (int i = 0; i < RING_BUFFER_CAPACITY; i += 10)
            avgRPMs.add(MathUtil.mean(intakeRpmRingBuffer, i, 10));

        double sd = MathUtil.sd(avgRPMs);
        lastSd = sd;
//        return sd > INTAKE_OCCUPIED_SD;
        return false;
    }

    private boolean hasDecelerated() {
        if (avgRPMs.size() < 2)
            return false;
        return avgRPMs.get(avgRPMs.size() - 1) < avgRPMs.get(0);
    }

    public static <T> void fill(BoundedCollection<T> collection, T value) {
        for (int i = 0; i < collection.maxSize(); i++) {
            collection.add(value);
        }
    }

    private boolean isOn;

    public enum IntakePosition {
        EXTENDED,
        RETRACTED
    }

    public IntakeModule(Robot robot, boolean isOn) {
        this.robot = robot;

        this.isOn = isOn;

        this.extenderLeft = robot.hardwareMap.servo.get("extenderLeft");
        this.extenderRight = robot.hardwareMap.servo.get("extenderRight");
        this.intakeMotor = (DcMotorEx) robot.hardwareMap.dcMotor.get("intake");

        intakeMotor.setDirection(DcMotorSimple.Direction.FORWARD);

        robot.telemetryDump.registerTelemeter(this);
    }

    public double getRPM() {
        synchronized (intakeRpmRingBuffer) {
            return intakeRpmRingBuffer.get(intakeRpmRingBuffer.size() - 1); // hopefully this is the most, not least, recent element
        }
    }

    public void setIntakePosition(IntakePosition intakePosition) {
        synchronized (extenderLeft) {
            synchronized (extenderRight) {
                this.intakePosition = intakePosition;
                switch (intakePosition) {
                    case EXTENDED:
                        extenderLeft.setPosition(INTAKE_LEFT_EXTENDED_POS);
                        extenderRight.setPosition(INTAKE_RIGHT_EXTENDED_POS);
                        break;
                    case RETRACTED:
                        extenderLeft.setPosition(INTAKE_LEFT_RETRACTED_POS);
                        extenderRight.setPosition(INTAKE_RIGHT_RETRACTED_POS);
                        break;
                }
            }
        }
    }

    public void update() {
        synchronized (intakeRpmRingBuffer) {
            if (!robot.isOpModeActive()) {
                setIntakePosition(IntakePosition.RETRACTED);
            } else if (!started) {
                setIntakePosition(IntakePosition.EXTENDED);
                started = true;
            }

            doOccupationStatusProcessing();

            // If we're done retracting (or the driver has pushed
            // the intake joystick up, we should re-extend
            if (inRetractionState() &&
                    (intakePower < 0 || doneRetracting()))
                startIntakeExtension();

            // If the intake is occupied and we haven't
            // started retracting yet, we should do that.
            if (intakeOccupied && !inRetractionState())
                if (robot.outtakeModule.readyForIntake())
                    startIntakeRetraction();
                else
                    robot.outtakeModule.setSlideLevel(OuttakeModule.VerticalSlideLevel.DOWN);

            intakeMotor.setPower(
                    inRetractionState() ? HOLD_POWER : intakePower
            );
        }
    }

    public void requestRetraction() {
        if (!inRetractionState())
            startIntakeRetraction();
    }

    private void startIntakeRetraction() {
        setIntakePosition(IntakePosition.RETRACTED);
        intakerRetractionStartTime = SystemClock.elapsedRealtime();

    }

    private boolean doneRetracting() {
        return SystemClock.elapsedRealtime() - intakerRetractionStartTime
                >= INTAKE_RETRACT_TIME;
    }

    private void doOccupationStatusProcessing() {
        intakeRpmRingBuffer.add(intakeMotor.getVelocity() * 60 / GOBILDA_1620_PPR);

        // since it's a ring buffer we could make the following more efficient
        // but it's literally ten elements so it doesn't matter
        if (getRPM() < RPM_EPSILON) {
            intakeOccupied = false;
        } else if (hasOccupationStatusChanged()) {
            intakeOccupied = hasDecelerated();
            fill(intakeRpmRingBuffer, getRPM()); // heinous hackery to set the stddev to 0
        }
    }

    private synchronized void startIntakeExtension() {
        if (robot != null) {
            robot.outtakeModule.hopperOccupied();
            robot.outtakeModule.raise();
        }
        intakeOccupied = false;
        intakerRetractionStartTime = null;
        setIntakePosition(IntakePosition.EXTENDED);
    }

    public boolean inRetractionState() {
        return intakerRetractionStartTime != null;
    }

    public boolean isOn() {
        return isOn;
    }

    @Override
    public ArrayList<String> getTelemetryData() {
        ArrayList<String> data = new ArrayList<>();

        data.add(String.format(Locale.US, "Intake speed (RPM): %f", getRPM()));
//        data.add("retract: " + inRetractionState());
        data.add(String.format(Locale.US, "Intake position:  %s", intakePosition));
        data.add(String.format(Locale.US, "Intake occupied:  %b", intakeOccupied));
//        data.add(String.format(Locale.US, "sd:  %f", lastSd));
//        data.add(String.format(Locale.US, "buf len:  %d", intakeRpmRingBuffer.size()));

        return data;
    }

    public String getName() {
        return "IntakeModule";
    }
}
