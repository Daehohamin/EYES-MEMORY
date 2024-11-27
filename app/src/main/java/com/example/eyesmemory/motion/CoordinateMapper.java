package com.example.eyesmemory.motion;

public class CoordinateMapper {

    private final float scale;
    private final float xOffset;
    private final float yOffset;

    public CoordinateMapper(float cameraWidth, float cameraHeight, float screenWidth, float screenHeight) {
        // 화면 기준 크기 조정 비율 계산
        this.scale =  Math.max(screenWidth / cameraWidth, screenHeight / cameraHeight);

        // 중앙 정렬 오프셋 계산
        this.xOffset = (screenWidth - (cameraWidth * scale)) / 2;
        this.yOffset = (screenHeight - (cameraHeight * scale)) / 2;
    }

    public Float transposeX(float cameraX) {
        return xOffset + (cameraX * scale);
    }

    public Float transposeY(float cameraY) {
        return yOffset + (cameraY * scale);
    }
}
