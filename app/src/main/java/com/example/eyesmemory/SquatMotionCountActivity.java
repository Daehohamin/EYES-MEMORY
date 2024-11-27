package com.example.eyesmemory;

import com.example.eyesmemory.motion.MotionCounter;
import com.example.eyesmemory.motion.SquatCounter;

public class SquatMotionCountActivity extends AbstractMotionCountActivity {
    @Override
    int maxCount() {
        return 10;
    }

    @Override
    String title() {
        return "스쿼트 동작";
    }

    @Override
    MotionCounter armCounter() {
        return new SquatCounter(maxCount());
    }

    @Override
    int videoId() {return R.raw.example_video4; }
}
