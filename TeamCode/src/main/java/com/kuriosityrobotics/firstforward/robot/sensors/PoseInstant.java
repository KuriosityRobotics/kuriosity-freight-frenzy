package com.kuriosityrobotics.firstforward.robot.sensors;

import com.kuriosityrobotics.firstforward.robot.util.math.Pose;

class PoseInstant extends Pose {
    final double timestamp;

    public PoseInstant(Pose pose, double timestamp) {
        super(pose);

        this.timestamp = timestamp;
    }
}
