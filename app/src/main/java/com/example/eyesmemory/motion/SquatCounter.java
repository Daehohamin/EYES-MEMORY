package com.example.eyesmemory.motion;

import android.util.Log;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;
import android.graphics.PointF;

public class SquatCounter implements MotionCounter {
    private final int maxCount;
    private int squatCount = 0;
    private boolean wasSquatting = false;
    private boolean fullyStood = true;

    private OnCountListener listener;

    // 스쿼트 판단+ 엉덩이 위치 임계값
    private static final float SQUAT_THRESHOLD = 105f;  // 엉덩이가 이만큼 내려가면 스쿼트로 판단

    // 팔 위치 판단을 위한 임계값
    private static final float ARM_ANGLE_TARGET = 80f;    // 척추와 팔 사이의 목표 각도
    private static final float MAX_ARM_DEVIATION = 55f;   // 각도의 허용 오차

    public SquatCounter(int maxCount) {
        this.maxCount = maxCount;
    }

    public void setOnCountListener(OnCountListener listener) {
        this.listener = listener;
    }

    @Override
    public void onPoseDetected(Pose pose) {
        if (squatCount >= maxCount) return;

        // 양쪽 엉덩이와 무릎 랜드마크 추출
        PoseLandmark leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP);
        PoseLandmark leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE);
        PoseLandmark rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP);
        PoseLandmark rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE);

        // 팔 랜드마크 추출
        PoseLandmark leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
        PoseLandmark leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW);
        PoseLandmark leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
        PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
        PoseLandmark rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW);
        PoseLandmark rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);

        if (leftHip == null || leftKnee == null || rightHip == null || rightKnee == null ||
                leftShoulder == null || leftElbow == null || leftWrist == null ||
                rightShoulder == null || rightElbow == null || rightWrist == null) {
            return;
        }

        // 양쪽 엉덩이와 무릎의 y좌표 차이 계산
        float leftHipToKneeDistance = Math.abs(leftHip.getPosition().y - leftKnee.getPosition().y);
        float rightHipToKneeDistance = Math.abs(rightHip.getPosition().y - rightKnee.getPosition().y);

        // 양쪽 평균 거리 계산
        float averageHipToKneeDistance = (leftHipToKneeDistance + rightHipToKneeDistance) / 2;

        // 스쿼트 자세인지 판단 (엉덩이가 충분히 내려갔는지)
        boolean isSquatting = averageHipToKneeDistance < SQUAT_THRESHOLD;

        // 팔이 앞으로 뻗어진 상태인지 판단
        boolean areArmsExtendedForward = areArmsInFront(pose);

        // 스쿼트 카운트: 스쿼트 자세 및 팔 상태가 모두 만족할 때
        if (!wasSquatting && isSquatting && fullyStood && areArmsExtendedForward) {
            squatCount++;
            if (listener != null) {
                listener.onCountChanged(squatCount);
            }
            fullyStood = false;
        }

        // 완전히 선 자세로 돌아왔는지 확인
        if (averageHipToKneeDistance > SQUAT_THRESHOLD * 1.35) {  // 더 큰 임계값으로 확실히 섰는지 확인
            fullyStood = true;
        }

        // 현재 상태 저장
        wasSquatting = isSquatting;

        Log.d("SquatCounter", String.format(
                "스쿼트 횟수: %d, 왼쪽거리: %.2f, 오른쪽거리: %.2f, 평균거리: %.2f, 팔 상태: %s",
                squatCount, leftHipToKneeDistance, rightHipToKneeDistance, averageHipToKneeDistance,
                areArmsExtendedForward ? "확인됨" : "확인되지 않음"));
    }

    /*
     * 팔이 앞으로 뻗어진 상태인지 판단
     * 척추(어깨-엉덩이 선)과 팔 사이의 각도가 약 90도인지
     */
    private boolean areArmsInFront(Pose pose) {
        // 왼쪽 팔 랜드마크
        PoseLandmark leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
        PoseLandmark leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW);
        PoseLandmark leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
        // 오른쪽 팔 랜드마크
        PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
        PoseLandmark rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW);
        PoseLandmark rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
        // 엉덩이 랜드마크 (중앙)
        PoseLandmark leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP);
        PoseLandmark rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP);

        if (leftShoulder == null || leftElbow == null || leftWrist == null ||
                rightShoulder == null || rightElbow == null || rightWrist == null ||
                leftHip == null || rightHip == null) {
            return false;
        }

        // 척추 선 (양쪽 엉덩이의 중간점)
        float spineMidX = (leftHip.getPosition().x + rightHip.getPosition().x) / 2;
        float spineMidY = (leftHip.getPosition().y + rightHip.getPosition().y) / 2;
        PointF spineMid = new PointF(spineMidX, spineMidY);

        // 왼쪽 팔의 각도 계산
        float leftArmAngle = calculateAngle(spineMid, leftShoulder.getPosition(), leftElbow.getPosition());
        // 오른쪽 팔의 각도 계산
        float rightArmAngle = calculateAngle(spineMid, rightShoulder.getPosition(), rightElbow.getPosition());

        boolean leftArmCorrect = Math.abs(leftArmAngle - ARM_ANGLE_TARGET) < MAX_ARM_DEVIATION;
        boolean rightArmCorrect = Math.abs(rightArmAngle - ARM_ANGLE_TARGET) < MAX_ARM_DEVIATION;

        return leftArmCorrect && rightArmCorrect;
    }

    /*

     *
     * @param firstPoint 정점의 시작점 (척추 중간점)
     * @param midPoint   정점 (어깨)
     * @param lastPoint  정점의 끝점 (팔꿈치)
     * @return 각도 (0 ~ 180)
     */
    private float calculateAngle(PointF firstPoint, PointF midPoint, PointF lastPoint) {
        double angle = Math.toDegrees(Math.atan2(lastPoint.y - midPoint.y, lastPoint.x - midPoint.x) -
                Math.atan2(firstPoint.y - midPoint.y, firstPoint.x - midPoint.x));
        angle = Math.abs(angle);
        if (angle > 180) {
            angle = 360.0 - angle;
        }
        return (float) angle;
    }

    @Override
    public void reset() {
        squatCount = 0;
        wasSquatting = false;
        fullyStood = true;
    }

    @Override
    public int getCount() {
        return squatCount;
    }
}
