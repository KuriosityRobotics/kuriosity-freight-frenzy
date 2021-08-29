package com.kuriosityrobotics.firstforward.robot.sensors;

import com.kuriosityrobotics.firstforward.robot.Robot;
import com.kuriosityrobotics.firstforward.robot.telemetry.Telemeter;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.util.ArrayList;
import java.util.Objects;

public class Odometry implements Telemeter {
    private final Robot robot;

    private final DcMotor yLeftEncoder;
    private final DcMotor yRightEncoder;
    private final DcMotor mecanumEncoder;

    private int lastYLeft;
    private int lastYRight;
    private int lastMecanum;

    public Odometry(Robot robot) {
        this.robot = robot;

        robot.telemetryDump.registerTelemeter(this);

        yLeftEncoder = robot.getHardware("fLeft");
        yRightEncoder = robot.getHardware("fRight");
        mecanumEncoder = robot.getHardware("bLeft");
    }

    void process() {
        lastYLeft = yLeftEncoder.getCurrentPosition();
        lastYRight = yRightEncoder.getCurrentPosition();
        lastMecanum = mecanumEncoder.getCurrentPosition();
    }

    @Override
    public ArrayList<String> getTelemetryData() {
        ArrayList<String> data = new ArrayList<>();

        data.add("lastYLeft: " + lastYLeft);
        data.add("lastYRight: " + lastYRight);
        data.add("lastMecanum: " + lastMecanum);

        return data;
    }

    @Override
    public String getName() {
        return "Odometry";
    }

    @Override
    public boolean isOn() {
        return true;
    }
}
