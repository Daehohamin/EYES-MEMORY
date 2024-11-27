package com.example.eyesmemory.motion;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;
public class MyOverlay extends View {
    private int[][] bodyConnections;
    private final Paint paint;
    private Pose currentPose;
    private boolean isReversed = false;
    private CoordinateMapper coordinateMapper;

    public MyOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(10f);
        paint.setAntiAlias(true);

        init();
    }

    public void setPose(Pose pose, int imageWidth, int imageHeight, boolean isReversed) {
        this.currentPose = pose;
        this.isReversed = isReversed;
        this.coordinateMapper = new CoordinateMapper(imageWidth, imageHeight, getWidth(), getHeight());
        invalidate(); // 뷰 다시 그리기
    }

    private void init() {
        // 신체 부위를 연결하는 선들의 좌표 배열 정의
        bodyConnections = new int[][]{
                // 코와 눈을 연결하는 선
                {PoseLandmark.NOSE, PoseLandmark.LEFT_EYE},     // 코-왼쪽 눈
                {PoseLandmark.NOSE, PoseLandmark.RIGHT_EYE},    // 코-오른쪽 눈
                {PoseLandmark.LEFT_EYE, PoseLandmark.LEFT_EAR}, // 왼쪽 눈-왼쪽 귀
                {PoseLandmark.RIGHT_EYE, PoseLandmark.RIGHT_EAR}, // 오른쪽 눈-오른쪽 귀

                // 어깨와 팔을 연결하는 선
                {PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER}, // 왼쪽 어깨-오른쪽 어깨
                {PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW},    // 왼쪽 어깨-왼쪽 팔꿈치
                {PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW},  // 오른쪽 어깨-오른쪽 팔꿈치
                {PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST},      // 왼쪽 팔꿈치-왼쪽 손목
                {PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST},    // 오른쪽 팔꿈치-오른쪽 손목

                // 몸통을 연결하는 선
                {PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP},     // 왼쪽 어깨-왼쪽 엉덩이
                {PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP},   // 오른쪽 어깨-오른쪽 엉덩이
                {PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP},        // 왼쪽 엉덩이-오른쪽 엉덩이

                // 다리를 연결하는 선
                {PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE},        // 왼쪽 엉덩이-왼쪽 무릎
                {PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE},      // 오른쪽 엉덩이-오른쪽 무릎
                {PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE},      // 왼쪽 무릎-왼쪽 발목
                {PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE},    // 오른쪽 무릎-오른쪽 발목

//            // 발을 연결하는 선
//            {PoseLandmark.LEFT_ANKLE, PoseLandmark.LEFT_HEEL},      // 왼쪽 발목-왼쪽 발뒤꿈치
//            {PoseLandmark.RIGHT_ANKLE, PoseLandmark.RIGHT_HEEL},    // 오른쪽 발목-오른쪽 발뒤꿈치
//            {PoseLandmark.LEFT_HEEL, PoseLandmark.LEFT_FOOT_INDEX}, // 왼쪽 발뒤꿈치-왼쪽 발가락
//            {PoseLandmark.RIGHT_HEEL, PoseLandmark.RIGHT_FOOT_INDEX}, // 오른쪽 발뒤꿈치-오른쪽 발가락
//
//            // 발가락을 연결하는 선
//            {PoseLandmark.LEFT_FOOT_INDEX, PoseLandmark.LEFT_PINKY},  // 왼쪽 발가락-왼쪽 새끼발가락
//            {PoseLandmark.RIGHT_FOOT_INDEX, PoseLandmark.RIGHT_PINKY}, // 오른쪽 발가락-오른쪽 새끼발가락
//            {PoseLandmark.LEFT_FOOT_INDEX, PoseLandmark.LEFT_INDEX},  // 왼쪽 발가락-왼쪽 검지발가락
//            {PoseLandmark.RIGHT_FOOT_INDEX, PoseLandmark.RIGHT_INDEX}, // 오른쪽 발가락-오른쪽 검지발가락
//
//            // 어깨와 손가락을 연결하는 선
//            {PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_PINKY},   // 왼쪽 어깨-왼쪽 새끼손가락
//            {PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_PINKY}, // 오른쪽 어깨-오른쪽 새끼손가락
//
//            // 팔꿈치와 손가락을 연결하는 선
//            {PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_PINKY},     // 왼쪽 팔꿈치-왼쪽 새끼손가락
//            {PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_PINKY},   // 오른쪽 팔꿈치-오른쪽 새끼손가락
//
//            // 손목과 손가락을 연결하는 선
//            {PoseLandmark.LEFT_WRIST, PoseLandmark.LEFT_PINKY},     // 왼쪽 손목-왼쪽 새끼손가락
//            {PoseLandmark.RIGHT_WRIST, PoseLandmark.RIGHT_PINKY}    // 오른쪽 손목-오른쪽 새끼손가락
        };
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (currentPose == null || coordinateMapper == null) return;

        // 랜드마크 포인트들을 연결하는 선 그리기
        for (int[] connection : bodyConnections) {
            PoseLandmark startLandmark = currentPose.getPoseLandmark(connection[0]);
            PoseLandmark endLandmark = currentPose.getPoseLandmark(connection[1]);

            if (startLandmark != null && endLandmark != null) {
                Float startX = coordinateMapper.transposeX(startLandmark.getPosition().x);
                Float startY = coordinateMapper.transposeY(startLandmark.getPosition().y);
                Float endX = coordinateMapper.transposeX(endLandmark.getPosition().x);
                Float endY = coordinateMapper.transposeY(endLandmark.getPosition().y);

                if (isReversed) {
                    startX = getWidth() - startX;
                    endX = getWidth() - endX;
                }

                canvas.drawLine(startX, startY, endX, endY, paint);
            }
        }

        // 랜드마크 포인트 그리기
        for (PoseLandmark landmark : currentPose.getAllPoseLandmarks()) {
            Float x = coordinateMapper.transposeX(landmark.getPosition().x);
            Float y = coordinateMapper.transposeY(landmark.getPosition().y);

            if (isReversed) {
                x = getWidth() - x;
            }

            canvas.drawCircle(x, y, 10f, paint);
        }
    }
}