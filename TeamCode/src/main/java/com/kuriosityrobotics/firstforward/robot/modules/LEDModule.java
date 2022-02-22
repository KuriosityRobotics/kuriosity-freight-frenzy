package com.kuriosityrobotics.firstforward.robot.modules;

import com.kuriosityrobotics.firstforward.robot.Robot;
import com.kuriosityrobotics.firstforward.robot.debug.telemetry.Telemeter;
import com.qualcomm.hardware.rev.RevBlinkinLedDriver;

import java.util.ArrayList;

public class LEDModule implements Module, Telemeter {

    private final Robot robot;

    // states
    public RevBlinkinLedDriver.BlinkinPattern pattern = RevBlinkinLedDriver.BlinkinPattern.BREATH_RED;

    //servos
    private final RevBlinkinLedDriver LED;

    public LEDModule(Robot robot) {
        this.robot = robot;

        robot.telemetryDump.registerTelemeter(this);

        LED = robot.hardwareMap.get(RevBlinkinLedDriver.class, "LED");
    }

    public void update() {
        this.LED.setPattern(pattern);
    }

    @Override
    public boolean isOn() {
        return true;
    }

    @Override
    public String getName() {
        return "LED Module";
    }

    @Override
    public Iterable<String> getTelemetryData() {
        return new ArrayList<>() {{
            add("Color: " + pattern.toString());
        }};
    }
}
