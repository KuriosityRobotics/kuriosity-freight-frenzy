package com.kuriosityrobotics.firstforward.robot.modules.leds;

import android.os.SystemClock;

import com.kuriosityrobotics.firstforward.robot.Robot;
import com.kuriosityrobotics.firstforward.robot.debug.telemetry.Telemeter;
import com.kuriosityrobotics.firstforward.robot.modules.Module;
import com.kuriosityrobotics.firstforward.robot.modules.intake.IntakeModule;
import com.qualcomm.hardware.rev.RevBlinkinLedDriver;
import com.qualcomm.hardware.rev.RevBlinkinLedDriver.BlinkinPattern;

import java.util.ArrayList;
import java.util.List;

public class LEDModule implements Module, Telemeter {
    BlinkinPattern VUF_INITING = BlinkinPattern.DARK_BLUE;
    BlinkinPattern INTAKE_OCCUPIED = BlinkinPattern.GREEN;
    BlinkinPattern IDLE = BlinkinPattern.BREATH_RED;

    BlinkinPattern VUF_USED = BlinkinPattern.BLUE;
    BlinkinPattern VUF_SAW = BlinkinPattern.GOLD;

    private static final long SHOW_VUF = 500;
    private long updateDuration = 0;
    private long timeOfLastUpdate = 0;

    // modules
    Robot robot;
    IntakeModule intake;

    //servos
    private final RevBlinkinLedDriver led;

    public LEDModule(Robot robot) {
        led = robot.hardwareMap.get(RevBlinkinLedDriver.class, "LED");

        this.robot = robot;
        this.intake = robot.intakeModule;
    }

    public void update() {
        if (intake != null && intake.hasMineral()) {
            led.setPattern(INTAKE_OCCUPIED);
        } else if (!robot.visionThread.started) {
            led.setPattern(VUF_INITING);
        } else if (SystemClock.elapsedRealtime() <= robot.visionThread.vuforiaLocalizationConsumer.getLastAcceptedTime() + SHOW_VUF) {
            led.setPattern(VUF_USED);
        } else if (SystemClock.elapsedRealtime() <= robot.visionThread.vuforiaLocalizationConsumer.getLastDetectedTime() + SHOW_VUF) {
            led.setPattern(VUF_SAW);
        } else {
            led.setPattern(IDLE);
        }

        long currentTime = SystemClock.elapsedRealtime();
        updateDuration = currentTime - timeOfLastUpdate;
        timeOfLastUpdate = currentTime;
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
    public List<String> getTelemetryData() {
        return new ArrayList<>() {{add("Update Time" + updateDuration);
            add("--");}};
    }

    @Override
    public int getShowIndex() {
        return 1;
    }
}
