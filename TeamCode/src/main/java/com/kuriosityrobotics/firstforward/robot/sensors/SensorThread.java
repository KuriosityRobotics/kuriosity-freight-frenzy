package com.kuriosityrobotics.firstforward.robot.sensors;

import static de.esoco.coroutine.Coroutine.first;
import static de.esoco.coroutine.CoroutineScope.launch;
import static de.esoco.coroutine.step.CodeExecution.consume;

import android.os.SystemClock;
import android.util.Log;

import com.kuriosityrobotics.firstforward.robot.Robot;
import com.kuriosityrobotics.firstforward.robot.telemetry.Telemeter;
import com.kuriosityrobotics.firstforward.robot.vision.vuforia.LocalizationConsumer;
import com.qualcomm.hardware.lynx.LynxModule;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import java.util.ArrayList;
import java.util.List;

import de.esoco.coroutine.Coroutine;

public class SensorThread implements Runnable, Telemeter {
    private static final Coroutine<LynxModule, Void> bulkDataCoroutine = first(consume(LynxModule::getBulkData));

    private final String configLocation;
    private final Robot robot;

    private final Odometry odometry;
    private final LocalizeKalmanFilter kalmanFilter;

    private long updateTime = 0;
    private long lastLoopTime = 0;

    private final List<LocalizationConsumer> localizationConsumers;

    public SensorThread(Robot robot, String configLocation, List<LocalizationConsumer> localizationConsumers) {
        this.robot = robot;
        this.configLocation = configLocation;
        this.localizationConsumers = localizationConsumers;

        robot.telemetryDump.registerTelemeter(this);

        this.odometry = new Odometry(robot);
        this.kalmanFilter = new LocalizeKalmanFilter(robot, odometry.getWorldMatrix());
    }


    @Override
    public void run() {
        while (robot.running()) {
            launch(scope -> {
                bulkDataCoroutine.runAsync(scope, robot.revHub1);
//                bulkDataCoroutine.runAsync(scope, robot.revHub2);
            });
            this.odometry.update();

            RealMatrix odo = this.odometry.getDeltaMatrix();
            RealMatrix delta = normalize(odo, updateTime);
            RealMatrix obs = this.localizationConsumers.get(0).getFormattedMatrix();

            this.kalmanFilter.update(delta, obs);
//            // for test
//            this.kalmanFilter.update(delta, null);

            long currentTime = SystemClock.elapsedRealtime();
            updateTime = currentTime - lastLoopTime;
            lastLoopTime = currentTime;
        }

        Log.v("SensorThread", "Exited due to opMode no longer being active.");
    }

    private RealMatrix normalize(RealMatrix odo, long updateInterval) {
        return MatrixUtils.createRealMatrix(new double[][]{
                {odo.getEntry(0,0) * (updateTime)},
                {odo.getEntry(1,0) * (updateTime)},
                {odo.getEntry(2,0)}
        });
    }

    @Override
    public ArrayList<String> getTelemetryData() {
        ArrayList<String> data = new ArrayList<>();

        data.add("Update time: " + updateTime);

        return data;
    }

    @Override
    public String getName() {
        return "SensorThread";
    }

    @Override
    public boolean isOn() {
        return true;
    }
}
