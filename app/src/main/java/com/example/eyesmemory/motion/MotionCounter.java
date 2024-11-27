package com.example.eyesmemory.motion;

import com.google.mlkit.vision.pose.Pose;

public interface MotionCounter {
    void setOnCountListener(OnCountListener listener);

    void onPoseDetected(Pose pose);

    void reset();

    int getCount();
}
