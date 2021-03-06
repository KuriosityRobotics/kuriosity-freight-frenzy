package com.kuriosityrobotics.firstforward.robot.debug;

import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FileDump {
    private static boolean activated = false;
    private static final Set<Pair<Field, Object>> dataFields = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<Field, Object> previousValues = new ConcurrentHashMap<>();

    private static PrintWriter writer;
    private static long lastTime;

    public static void activate() {
        try {
            File file = new File(AppUtil.ROBOT_DATA_DIR + "/" + new Date().getTime() + ".csv");
            writer = new PrintWriter(file);

            Log.v("FileDump", "Started dumping to: " + file.getAbsolutePath());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        dataFields.stream()
                .map(n -> n.first)
                .forEach(n -> n.setAccessible(true));

        writer.println(
                "time since last update, " + dataFields.stream()
                        .map(n -> n.first)
                        .map(Field::getName)
                        .collect(Collectors.joining(",")));
        lastTime = SystemClock.elapsedRealtime();

        activated = true;
    }

    public static void addField(String fieldName, Object receiver) {
        try {
            dataFields.add(new Pair<>(receiver.getClass().getDeclaredField(fieldName), receiver));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static void dumpImage(Mat frame) {
        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(".jpg", frame, mob);

        File file = new File(AppUtil.ROBOT_DATA_DIR + "/" + "webcam-frame-" + new Date().getTime() + ".jpg");
        try (FileOutputStream stream = new FileOutputStream(file)) {
            stream.write(mob.toArray());
        } catch (IOException e) {
            Log.w("VisionDump", e);
        }
    }

    public static void update() {
        if (activated) {
            boolean anyUpdated = FileDump.dataFields.stream().anyMatch(n -> {
                Field field = n.first;
                Object instance = n.second;
                Object previousValue = previousValues.getOrDefault(field, null);
                Object value;
                try {
                    field.setAccessible(true);
                    value = field.get(instance);
                    if(value == null)
                        return false;

                    previousValues.put(field, value);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Can't access field " + field.getName() + " of instance " + instance.toString(), e);
                }

                return !value.equals(previousValue);
            });
            if (anyUpdated) {
                long newTime = SystemClock.elapsedRealtime();
                writer.println(newTime - lastTime + "," + FileDump.dataFields.stream().map(n ->
                        {
                            Field field = n.first;
                            Object instance = n.second;

                            try {
                                field.setAccessible(true);
                                Object value = field.get(instance);
                                if(value == null)
                                    return "null";
                                return value.toString();
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        }
                ).collect(Collectors.joining(",")));
                writer.flush();
                lastTime = newTime;
            }
        }
    }

    public static void close() {
        if (activated) {
            writer.close();
            activated = false;

            Log.i("FileDump", "Filedump has been stopped");
        }
    }
}
