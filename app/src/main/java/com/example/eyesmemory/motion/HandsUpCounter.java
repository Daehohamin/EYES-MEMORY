package com.example.eyesmemory.motion;

import android.util.Log;

import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

public class HandsUpCounter implements MotionCounter {

    private final int maxCount;
    private int handsUpCount = 0;
    private boolean isLeftHandUp = false;
    private boolean isRightHandUp = false;
    private boolean wasHandsUp = false; // 이전에 만세 자세였는지 추적
    private boolean handsFullyDown = true; // 팔이 완전히 내려갔는지 추적

    private float prevLeftElbowY = -1;
    private float prevRightElbowY = -1;
    private float prevLeftShoulderY = -1;
    private float prevRightShoulderY = -1;
    private float prevLeftWristY = -1;
    private float prevRightWristY = -1;

    private OnCountListener listener;

    public HandsUpCounter(int maxCount) {
        this.maxCount = maxCount;
    }

    // 콜백 리스너 설정
    public void setOnCountListener(OnCountListener listener) {
        this.listener = listener;
    }

    // 만세 동작 감지
    public void onPoseDetected(Pose pose) {
        if (handsUpCount == maxCount) return;

        // 랜드마크 추출
        PoseLandmark leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
        PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
        PoseLandmark leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW);
        PoseLandmark rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW);
        PoseLandmark leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
        PoseLandmark rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);

        if (leftShoulder == null || rightShoulder == null || leftElbow == null || rightElbow == null || leftWrist == null || rightWrist == null) {
            return;  // 랜드마크가 감지되지 않으면 처리하지 않음
        }

        // 현재 팔꿈치, 어깨, 손목의 Y 좌표
        float currentLeftElbowY = leftElbow.getPosition().y;
        float currentRightElbowY = rightElbow.getPosition().y;
        float currentLeftShoulderY = leftShoulder.getPosition().y;
        float currentRightShoulderY = rightShoulder.getPosition().y;
        float currentLeftWristY = leftWrist.getPosition().y;
        float currentRightWristY = rightWrist.getPosition().y;

        // 팔을 충분히 위로 들었는지 확인하기 위한 임계값 (어깨 위로 최소 30% 이상)
        float minHeightThreshold = 0.3f;
        float leftShoulderToWristDistance = currentLeftShoulderY - currentLeftWristY;
        float rightShoulderToWristDistance = currentRightShoulderY - currentRightWristY;

        // 팔이 얼마나 수직에 가까운지 계산 (X좌표 차이가 작을수록 수직)
        float leftArmVerticalThreshold = Math.abs(leftWrist.getPosition().x - leftShoulder.getPosition().x);
        float rightArmVerticalThreshold = Math.abs(rightWrist.getPosition().x - rightShoulder.getPosition().x);
        float maxHorizontalDeviation = 150f; // 수직 허용 오차 증가

        // 팔꿈치가 펴졌는지 확인 (어깨-팔꿈치-손목이 일직선에 가까운지)
        float leftArmAngle = calculateAngle(leftShoulder, leftElbow, leftWrist);
        float rightArmAngle = calculateAngle(rightShoulder, rightElbow, rightWrist);
        float minStraightAngle = 130f; // 팔이 펴진 것으로 간주할 최소 각도 감소

        // 모든 조건을 만족하는지 확인
        boolean currentLeftHandUp = currentLeftElbowY < currentLeftShoulderY &&
                currentLeftWristY < currentLeftElbowY &&
                leftShoulderToWristDistance > (currentLeftShoulderY * minHeightThreshold) &&
                leftArmVerticalThreshold < maxHorizontalDeviation &&
                leftArmAngle > minStraightAngle;

        boolean currentRightHandUp = currentRightElbowY < currentRightShoulderY &&
                currentRightWristY < currentRightElbowY &&
                rightShoulderToWristDistance > (currentRightShoulderY * minHeightThreshold) &&
                rightArmVerticalThreshold < maxHorizontalDeviation &&
                rightArmAngle > minStraightAngle;

        boolean currentHandsUp = currentLeftHandUp && currentRightHandUp;

        // 팔이 완전히 내려갔는지 확인 (손목이 어깨보다 아래에 있는지)
        boolean leftHandDown = currentLeftWristY > currentLeftShoulderY;
        boolean rightHandDown = currentRightWristY > currentRightShoulderY;
        boolean currentHandsDown = leftHandDown && rightHandDown;

        // 이전에 만세 자세가 아니었다가 현재 만세 자세가 되었을 때만 카운트 증가
        // 단, 팔이 완전히 내려갔다가 올라갔을 때만 카운트
        if (!wasHandsUp && currentHandsUp && handsFullyDown) {
            handsUpCount++;
            if (listener != null) {
                listener.onCountChanged(handsUpCount);
            }
            handsFullyDown = false; // 팔을 들었으므로 false로 설정
        }

        // 팔이 완전히 내려갔을 때 플래그 업데이트
        if (currentHandsDown) {
            handsFullyDown = true;
        }

        // 현재 상태를 이전 상태로 저장
        wasHandsUp = currentHandsUp;

        // 이전 좌표 업데이트
        prevLeftElbowY = currentLeftElbowY;
        prevRightElbowY = currentRightElbowY;
        prevLeftShoulderY = currentLeftShoulderY;
        prevRightShoulderY = currentRightShoulderY;
        prevLeftWristY = currentLeftWristY;
        prevRightWristY = currentRightWristY;

        // 카운트 출력 (디버깅 용도)
        Log.d("HandsUpCounter", "만세 동작 횟수: " + handsUpCount);
    }

    // 세 점 각도
    private float calculateAngle(PoseLandmark firstPoint, PoseLandmark midPoint, PoseLandmark lastPoint) {
        double result = Math.toDegrees(Math.atan2(lastPoint.getPosition().y - midPoint.getPosition().y,
                lastPoint.getPosition().x - midPoint.getPosition().x)
                - Math.atan2(firstPoint.getPosition().y - midPoint.getPosition().y,
                firstPoint.getPosition().x - midPoint.getPosition().x));
        result = Math.abs(result); // 각도의 절대값을 구함
        if (result > 180) {
            result = 360.0 - result; // 각도를 0~180 범위로 조정
        }
        return (float) result;
    }

    public void reset() {
        handsUpCount = 0;
    }

    public int getCount() {
        return handsUpCount;
    }
}

