package com.example.eyesmemory;

import com.example.eyesmemory.motion.MotionCounter;
import com.example.eyesmemory.motion.SingleArmCounter;

public class RightHandUpMotionCountActivity extends AbstractMotionCountActivity {

    @Override
    int maxCount() {
        return 10;
    }

    @Override
    String title() {
        return "오른팔 위로";
    }

    @Override
    MotionCounter armCounter() {
        return new SingleArmCounter(maxCount(), false);
    }

    @Override
    int videoId() {
        return R.raw.example_video3;
    }
}
