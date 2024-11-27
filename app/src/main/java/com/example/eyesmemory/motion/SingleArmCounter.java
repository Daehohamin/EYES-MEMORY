package com.example.eyesmemory.motion;

import android.util.Log;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

public class SingleArmCounter implements MotionCounter {
    private final int maxCount;
    private int armUpCount = 0;
    private boolean isArmUp = false;
    private boolean wasArmUp = false;
    private boolean armFullyDown = true;
    private final boolean isLeftArm; // true면 왼팔, false면 오른팔

    private OnCountListener listener;

    public SingleArmCounter(int maxCount, boolean isLeftArm) {
        this.maxCount = maxCount;
        this.isLeftArm = isLeftArm;
    }

    @Override
    public void setOnCountListener(OnCountListener listener) {
        this.listener = listener;
    }

    @Override
    public void onPoseDetected(Pose pose) {
        if (armUpCount == maxCount) return;

        // 필요한 랜드마크 추출 (현재 팔과 반대쪽 팔 모두)
        PoseLandmark shoulder = pose.getPoseLandmark(isLeftArm ? PoseLandmark.LEFT_SHOULDER : PoseLandmark.RIGHT_SHOULDER);
        PoseLandmark elbow = pose.getPoseLandmark(isLeftArm ? PoseLandmark.LEFT_ELBOW : PoseLandmark.RIGHT_ELBOW);
        PoseLandmark wrist = pose.getPoseLandmark(isLeftArm ? PoseLandmark.LEFT_WRIST : PoseLandmark.RIGHT_WRIST);
        
        // 반대쪽 팔 랜드마크
        PoseLandmark oppositeWrist = pose.getPoseLandmark(isLeftArm ? PoseLandmark.RIGHT_WRIST : PoseLandmark.LEFT_WRIST);
        PoseLandmark oppositeShoulder = pose.getPoseLandmark(isLeftArm ? PoseLandmark.RIGHT_SHOULDER : PoseLandmark.LEFT_SHOULDER);

        if (shoulder == null || elbow == null || wrist == null || oppositeWrist == null || oppositeShoulder == null) {
            return;
        }

        // 현재 관절 위치
        float currentShoulderY = shoulder.getPosition().y;
        float currentElbowY = elbow.getPosition().y;
        float currentWristY = wrist.getPosition().y;
        
        // 반대쪽 팔 위치
        float oppositeWristY = oppositeWrist.getPosition().y;
        float oppositeShoulderY = oppositeShoulder.getPosition().y;

        // 팔을 충분히 위로 들었는지 확인 (어깨 위로 30% 이상)
        float minHeightThreshold = 0.3f;
        float shoulderToWristDistance = currentShoulderY - currentWristY;

        // 팔이 수직에 가까운지 계산
        float armVerticalThreshold = Math.abs(wrist.getPosition().x - shoulder.getPosition().x);
        float maxHorizontalDeviation = 150f;

        // 팔꿈치가 펴졌는지 확인
        float armAngle = calculateAngle(shoulder, elbow, wrist);
        float minStraightAngle = 130f;

        // 현재 팔이 올라갔는지 확인
        boolean currentArmUp = currentElbowY < currentShoulderY &&
                currentWristY < currentElbowY &&
                shoulderToWristDistance > (currentShoulderY * minHeightThreshold) &&
                armVerticalThreshold < maxHorizontalDeviation &&
                armAngle > minStraightAngle;

        // 반대쪽 팔이 내려가 있는지 확인
        boolean oppositeArmDown = oppositeWristY > oppositeShoulderY;

        // 팔이 완전히 내려갔는지 확인
        boolean currentArmDown = currentWristY > currentShoulderY;

        // 이전에 팔이 내려가 있었고, 현재 올라갔을 때만 카운트 (단, 반대쪽 팔은 내려가 있어야 함)
        if (!wasArmUp && currentArmUp && armFullyDown && oppositeArmDown) {
            armUpCount++;
            if (listener != null) {
                listener.onCountChanged(armUpCount);
            }
            armFullyDown = false;
        }

        // 팔이 완전히 내려갔을 때 플래그 업데이트
        if (currentArmDown) {
            armFullyDown = true;
        }

        // 현재 상태를 이전 상태로 저장
        wasArmUp = currentArmUp;

        // 디버깅용 로그
        String armSide = isLeftArm ? "왼팔" : "오른팔";
        Log.d("SingleArmCounter", armSide + " 들기 횟수: " + armUpCount);
    }

    private float calculateAngle(PoseLandmark firstPoint, PoseLandmark midPoint, PoseLandmark lastPoint) {
        double result = Math.toDegrees(Math.atan2(lastPoint.getPosition().y - midPoint.getPosition().y,
                lastPoint.getPosition().x - midPoint.getPosition().x)
                - Math.atan2(firstPoint.getPosition().y - midPoint.getPosition().y,
                firstPoint.getPosition().x - midPoint.getPosition().x));
        result = Math.abs(result);
        if (result > 180) {
            result = 360.0 - result;
        }
        return (float) result;
    }

    @Override
    public void reset() {
        armUpCount = 0;
    }

    @Override
    public int getCount() {
        return armUpCount;
    }
}
