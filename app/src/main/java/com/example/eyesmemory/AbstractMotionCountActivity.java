package com.example.eyesmemory;

import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.VideoView;
import android.net.Uri;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.example.eyesmemory.motion.MotionCounter;
import com.example.eyesmemory.motion.MyOverlay;
import com.example.eyesmemory.motion.SingleArmCounter;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import java.util.List;
import java.util.concurrent.Executors;

import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;

import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

public abstract class AbstractMotionCountActivity extends AppCompatActivity {

    private static final String KEY_LENS_FACING = "lens_facing";
    private static final int PERMISSION_REQUEST_CODE = 10;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private CameraSelector lensFacing = CameraSelector.DEFAULT_BACK_CAMERA;
    private PreviewView previewView;
    private VideoView exampleVideo;

    private final PoseDetectorOptions options = new PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build();

    private final PoseDetector poseDetector = PoseDetection.getClient(options);
    private ImageAnalysis imageAnalysis;
    private MyOverlay overlay;
    private TextView counterTextView;
    private MotionCounter handsUpCounter;
    private TextToSpeech tts;
    private ActivityResultLauncher<Intent> exerciseEndLauncher;

    private class YourAnalyzer implements ImageAnalysis.Analyzer {

        @OptIn(markerClass = ExperimentalGetImage.class)
        @Override
        public void analyze(ImageProxy imageProxy) {
            Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

                Task<Pose> result =
                        poseDetector.process(image)
                                .addOnSuccessListener(pose -> {
                                    onSuccess(pose, imageProxy);
                                    imageProxy.close();
                                })
                                .addOnFailureListener(e -> {
                                    onFailure(e);
                                    imageProxy.close();
                                });
            } else {
                imageProxy.close();
            }
        }

        private void onSuccess(Pose pose, ImageProxy imageProxy) {
            handsUpCounter.onPoseDetected(pose);

            // Get all PoseLandmarks. If no person was detected, the list will be empty
            List<PoseLandmark> allPoseLandmarks = pose.getAllPoseLandmarks();

            // Or get specific PoseLandmarks individually. These will all be null if no person
            // was detected
            PoseLandmark leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
            PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
            PoseLandmark leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW);
            PoseLandmark rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW);
            PoseLandmark leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
            PoseLandmark rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
            PoseLandmark leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP);
            PoseLandmark rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP);
            PoseLandmark leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE);
            PoseLandmark rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE);
            PoseLandmark leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE);
            PoseLandmark rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE);
            PoseLandmark leftPinky = pose.getPoseLandmark(PoseLandmark.LEFT_PINKY);
            PoseLandmark rightPinky = pose.getPoseLandmark(PoseLandmark.RIGHT_PINKY);
            PoseLandmark leftIndex = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX);
            PoseLandmark rightIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX);
            PoseLandmark leftThumb = pose.getPoseLandmark(PoseLandmark.LEFT_THUMB);
            PoseLandmark rightThumb = pose.getPoseLandmark(PoseLandmark.RIGHT_THUMB);
            PoseLandmark leftHeel = pose.getPoseLandmark(PoseLandmark.LEFT_HEEL);
            PoseLandmark rightHeel = pose.getPoseLandmark(PoseLandmark.RIGHT_HEEL);
            PoseLandmark leftFootIndex = pose.getPoseLandmark(PoseLandmark.LEFT_FOOT_INDEX);
            PoseLandmark rightFootIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_FOOT_INDEX);
            PoseLandmark nose = pose.getPoseLandmark(PoseLandmark.NOSE);
            PoseLandmark leftEyeInner = pose.getPoseLandmark(PoseLandmark.LEFT_EYE_INNER);
            PoseLandmark leftEye = pose.getPoseLandmark(PoseLandmark.LEFT_EYE);
            PoseLandmark leftEyeOuter = pose.getPoseLandmark(PoseLandmark.LEFT_EYE_OUTER);
            PoseLandmark rightEyeInner = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE_INNER);
            PoseLandmark rightEye = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE);
            PoseLandmark rightEyeOuter = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE_OUTER);
            PoseLandmark leftEar = pose.getPoseLandmark(PoseLandmark.LEFT_EAR);
            PoseLandmark rightEar = pose.getPoseLandmark(PoseLandmark.RIGHT_EAR);
            PoseLandmark leftMouth = pose.getPoseLandmark(PoseLandmark.LEFT_MOUTH);
            PoseLandmark rightMouth = pose.getPoseLandmark(PoseLandmark.RIGHT_MOUTH);

            // custom code
            runOnUiThread(() -> {
                boolean isReversed = (lensFacing == CameraSelector.DEFAULT_FRONT_CAMERA);
                int width = imageProxy.getWidth();
                int height = imageProxy.getHeight();

                if (imageProxy.getImageInfo().getRotationDegrees() == 90
                        || imageProxy.getImageInfo().getRotationDegrees() == 270) {
                    width = imageProxy.getHeight();
                    height = imageProxy.getWidth();
                }

                overlay.setPose(pose, width, height, isReversed);
            });
        }

        private void onFailure(Exception e) {

        }
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.hands_up);

        TextView titleView = findViewById(R.id.titleText);
        titleView.setText(title());

        // TTS 초기화
        tts = new TextToSpeech(this, status -> {});

        // 예시 동영상 설정
        exampleVideo = findViewById(R.id.exampleVideo);
        String videoPath = "android.resource://" + getPackageName() + "/" + videoId();
        Uri uri = Uri.parse(videoPath);
        exampleVideo.setVideoURI(uri);

        // 동영상 반복 재생 설정
        exampleVideo.setOnCompletionListener(mp -> {
            exampleVideo.start();
        });

        // 동영상 자동 시작
        exampleVideo.start();

        overlay = findViewById(R.id.overlay);

        previewView = findViewById(R.id.previewView);
        previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);

        ImageButton flipButton = findViewById(R.id.flip_button);
        flipButton.setOnClickListener(v -> {
            flipCamera();
        });

        ImageButton exitButton = findViewById(R.id.exitButton);
        exitButton.setOnClickListener(v -> finish());

        // 저장된 카메라 상태 복원
        if (savedInstanceState != null) {
            boolean isBackCamera = savedInstanceState.getBoolean(KEY_LENS_FACING, true);
            lensFacing = isBackCamera ? CameraSelector.DEFAULT_BACK_CAMERA : CameraSelector.DEFAULT_FRONT_CAMERA;
        }

        imageAnalysis = new ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), new YourAnalyzer());

        // 카메라 권한 체크
        if (!hasPermissions()) {
            requestPermissions();
        } else {
            startCamera();
        }

        counterTextView = findViewById(R.id.counterTextView);

        // ActivityResultLauncher 초기화
        exerciseEndLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // 결과 처리
                    runOnUiThread(() -> {
                        if (result.getResultCode() == RESULT_OK || result.getResultCode() == RESULT_CANCELED) {
                            handsUpCounter.reset();
                        }

                        counterTextView.setText("횟수 : 0회");
                    });
                }
        );

        // HandsUpCounter 초기화 및 리스너 설정
        handsUpCounter = armCounter();
        handsUpCounter.setOnCountListener(count -> {
            runOnUiThread(() -> {
                counterTextView.setText("횟수 : " + count + "회");

                String[] countWords = {"", "하나", "둘", "셋", "넷", "다섯", "여섯", "일곱", "여덟", "아홉", "열"};
                if (count <= maxCount()) {
                    speakOut(countWords[count]);
                }

                if (count >= maxCount()) {
                    Intent intent = new Intent(AbstractMotionCountActivity.this, ExerciseEndActivity.class);
                    exerciseEndLauncher.launch(intent);
                }
            });
        });
    }

    abstract int maxCount();
    abstract String title();
    abstract MotionCounter armCounter();
    abstract int videoId();

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    // 상태 저장
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // 현재 카메라가 후면 카메라인지 여부를 저장
        outState.putBoolean(KEY_LENS_FACING, lensFacing == CameraSelector.DEFAULT_BACK_CAMERA);
    }

    private boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 승인되면 카메라 시작
                startCamera();
            } else {
                // 권한이 거부되면 앱 종료 또는 다른 처리
                Toast.makeText(this,
                        "카메라 권한이 필요합니다.",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void flipCamera() {
        if (lensFacing == CameraSelector.DEFAULT_FRONT_CAMERA) lensFacing = CameraSelector.DEFAULT_BACK_CAMERA;
        else if (lensFacing == CameraSelector.DEFAULT_BACK_CAMERA) lensFacing = CameraSelector.DEFAULT_FRONT_CAMERA;
        startCamera();
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();

        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(
                        lensFacing == CameraSelector.DEFAULT_FRONT_CAMERA
                                ? CameraSelector.LENS_FACING_FRONT
                                : CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    private void speakOut(String text) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }
}
