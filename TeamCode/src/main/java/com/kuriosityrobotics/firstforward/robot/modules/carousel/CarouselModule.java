package com.kuriosityrobotics.firstforward.robot.modules.carousel;

import static java.lang.Math.PI;

import android.os.SystemClock;

import com.kuriosityrobotics.firstforward.robot.debug.telemetry.Telemeter;
import com.kuriosityrobotics.firstforward.robot.modules.Module;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

import java.util.ArrayList;

public class CarouselModule implements Module, Telemeter {
    private static final double CAROUSEL_SPINNER_WHEEL_CIRCUMFERENCE = 2 * PI;
    private static final double CAROUSEL_WHEEL_CIRCUMFERENCE = 15 * PI;
    private static final double REVS_PER_CAROUSEL_REV = CAROUSEL_WHEEL_CIRCUMFERENCE / CAROUSEL_SPINNER_WHEEL_CIRCUMFERENCE;

    //states
    public volatile boolean spin = false;
    public volatile boolean clockwise = false;
    public volatile double maxSpeed;

    private Long spinStartTimeMillis = null;

    //motors
    private final DcMotorEx carouselMotor;

    public CarouselModule(HardwareMap hardwareMap) {
        // original:  1.4 * pi
        this.maxSpeed = 1.0 * PI;

        carouselMotor = (DcMotorEx) hardwareMap.dcMotor.get("carousel");
        carouselMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
    }

    public CarouselAction carouselAction() {
        return new CarouselAction(this);
    }

    private double target = 0;

    public void update() {
        if (spin) {
            if (spinStartTimeMillis == null) {
                spinStartTimeMillis = SystemClock.elapsedRealtime();
            }

            double MAX_SPEED_MS = 1300;
            double speed = maxSpeed * Range.clip((((double)(SystemClock.elapsedRealtime() - spinStartTimeMillis)) / MAX_SPEED_MS), 0, 1);
            carouselMotor.setVelocity(clockwise ? -speed : speed, AngleUnit.RADIANS);
            target = speed;
        } else {
            spinStartTimeMillis = null;
            carouselMotor.setVelocity(0);
        }
    }

    public boolean isOn() {
        return true;
    }

    public String getName() {
        return "CarouselModule";
    }

    @Override
    public ArrayList<String> getTelemetryData() {
        ArrayList<String> data = new ArrayList<>();

        data.add("spin: " + spin);
        data.add("velocity: " + carouselMotor.getVelocity(AngleUnit.RADIANS));
        data.add("target: " + target);

        return data;
    }

}