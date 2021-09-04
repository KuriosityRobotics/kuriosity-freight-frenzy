package com.kuriosityrobotics.firstforward.robot.telemetry;

import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TelemetryDump {
    private final Telemetry telemetry;
    private final boolean debug;

    private final ArrayList<Telemeter> telemeters = new ArrayList<>();

    public void registerTelemeter(Telemeter telemeter) {
        telemeters.add(telemeter);
    }

    public TelemetryDump(Telemetry telemetry, boolean debug) {
        this.telemetry = telemetry;
        this.debug = debug;
    }

    public void update() {
        StringBuilder msg = new StringBuilder();

        for (Telemeter telemeter : telemeters) {
            if (telemeter.isOn()) {
                // ---Name---\n
                msg.append("---").append(telemeter.getName()).append("---\n");

                if (debug) {
                    for (var pair : getAllFields(telemeter)) {
                        // Key: Value \n
                        msg.append(pair.getKey()).append(": ").append(pair.getValue()).append("\n");
                    }
                } else {
                    for (var line : telemeter.getTelemetryData()) {
                        // telemetry_line\n
                        msg.append(line).append("\n");
                    }
                }

                // newline for every section
                msg.append("\n");
            }
        }

        telemetry.addLine(msg.toString());
        telemetry.update();
    }

    private Set<Map.Entry<String, Object>> getAllFields(Telemeter telemeter) {
        return Arrays.stream(telemeter.getClass().getDeclaredFields()) // cursed
                .filter(n -> Modifier.isPublic(n.getModifiers()))
                .collect(Collectors.toMap(Field::getName, n -> {
                    try {
                        return n.get(telemeter);
                    } catch (IllegalAccessException e) {
                        return null;
                    }
                })).entrySet();
    }
}