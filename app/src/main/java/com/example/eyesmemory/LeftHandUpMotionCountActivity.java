package com.example.eyesmemory;

import com.example.eyesmemory.motion.MotionCounter;
import com.example.eyesmemory.motion.SingleArmCounter;

public class LeftHandUpMotionCountActivity extends AbstractMotionCountActivity {

    @Override
    int maxCount() {
        return 10;
    }

    @Override
    String title() {
        return "왼팔 위로";
    }

    @Override
    MotionCounter armCounter() {
        return new SingleArmCounter(maxCount(), true);
    }

    @Override
    int videoId() {
        return R.raw.example_video2;
    }
}
