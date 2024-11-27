package com.example.eyesmemory;

import com.example.eyesmemory.motion.HandsUpCounter;
import com.example.eyesmemory.motion.MotionCounter;

public class HandsUpMotionCountActivity extends AbstractMotionCountActivity {

    @Override
    int maxCount() {
        return 10;
    }

    @Override
    String title() {
        return "만세 동작";
    }

    @Override
    MotionCounter armCounter() {
        return new HandsUpCounter(maxCount());
    }

    @Override
    int videoId() {
        return R.raw.example_video;
    }
}
