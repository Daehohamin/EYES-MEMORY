package com.example.eyesmemory;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.eyesmemory.GazeTrackerManager.LoadCalibrationResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import camp.visual.gazetracker.GazeTracker;
import camp.visual.gazetracker.callback.CalibrationCallback;
import camp.visual.gazetracker.callback.GazeCallback;
import camp.visual.gazetracker.callback.InitializationCallback;
import camp.visual.gazetracker.callback.StatusCallback;
import camp.visual.gazetracker.callback.UserStatusCallback;
import camp.visual.gazetracker.constant.AccuracyCriteria;
import camp.visual.gazetracker.constant.CalibrationModeType;
import camp.visual.gazetracker.constant.InitializationErrorType;
import camp.visual.gazetracker.constant.StatusErrorType;
import camp.visual.gazetracker.constant.UserStatusOption;
import camp.visual.gazetracker.filter.OneEuroFilterManager;
import camp.visual.gazetracker.gaze.GazeInfo;
import camp.visual.gazetracker.state.ScreenState;
import camp.visual.gazetracker.state.TrackingState;
import camp.visual.gazetracker.util.ViewLayoutChecker;
import visual.camp.sample.view.CalibrationViewer;
import visual.camp.sample.view.PointView;

public class GameSelectionActivity extends AppCompatActivity {

    private ImageButton choosingSelectButton;
    private ImageButton oppositeSelectButton;
    private TextView pointText;

    private GazeTrackerManager gazeTrackerManager;
    private ViewLayoutChecker viewLayoutChecker = new ViewLayoutChecker();
    private HandlerThread backgroundThread = new HandlerThread("background");
    private Handler backgroundHandler;

    private TextureView preview;
    private View layoutProgress;
    private View viewWarningTracking;
    private PointView viewPoint;
    private CalibrationViewer viewCalibration;

    private TextToSpeech tts;
    private int mode = 0; // 선택한 게임 모드를 저장하는 변수

    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.CAMERA // 시선 추적을 위한 카메라 권한
    };
    private static final int REQ_PERMISSION = 1000;

    private final OneEuroFilterManager oneEuroFilterManager = new OneEuroFilterManager(2);

    // Firestore 인스턴스
    private FirebaseFirestore db;
    private static final String TAG = "GameSelectionActivity";

    // SharedPreferences 키
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_ID = "userId";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_selection);


        // GazeTrackerManager 초기화
        gazeTrackerManager = GazeTrackerManager.makeNewInstance(getApplicationContext());
        Log.i(TAG, "gazeTracker version: " + GazeTracker.getVersionName());

        // TTS 초기화
        tts = new TextToSpeech(this, status -> {});

        // 뷰 초기화
        initView();
        checkPermission();
        initHandler();



        // Firestore 초기화
        db = FirebaseFirestore.getInstance();

        // SharedPreferences에서 사용자 ID 가져오기
        SharedPreferences sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String userId = sharedPref.getString(KEY_USER_ID, null);

        if (userId != null) {
            // Firestore에서 포인트 가져오기
            fetchUserPoints(userId);
        } else {
            Log.d(TAG, "User ID not set");
        }
    }

    // Firestore에서 사용자 포인트를 가져오는 메서드
    private void fetchUserPoints(String userId) {
        db.collection("users").document(userId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Long points = document.getLong("points");  // Firestore의 points 필드 값
                        if (points != null) {
                            pointText.setText(points + "P");  // TextView에 포인트 표시
                        }
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (preview.isAvailable()) {
            // TextureView가 사용 가능할 때
            gazeTrackerManager.setCameraPreview(preview);
        }

        gazeTrackerManager.setGazeTrackerCallbacks(gazeCallback, calibrationCallback, statusCallback, userStatusCallback);
        Log.i(TAG, "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        // 화면 전환 후에도 뷰의 오프셋을 다시 설정
        setOffsetOfView();
        gazeTrackerManager.startGazeTracking();
    }

    @Override
    protected void onPause() {
        super.onPause();
        gazeTrackerManager.stopGazeTracking();
        Log.i(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        gazeTrackerManager.removeCallbacks(gazeCallback, calibrationCallback, statusCallback, userStatusCallback);
        Log.i(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseHandler();
        viewLayoutChecker.releaseChecker();
        if (gazeTrackerManager != null) {
            gazeTrackerManager.deinitGazeTracker();
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    // handler 초기화 및 해제
    private void initHandler() {
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void releaseHandler() {
        backgroundThread.quitSafely();
    }

    // 권한 체크
    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 권한 상태 확인
            if (!hasPermissions(PERMISSIONS)) {
                requestPermissions(PERMISSIONS, REQ_PERMISSION);
            } else {
                checkPermission(true);
            }
        } else {
            checkPermission(true);
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private boolean hasPermissions(String[] permissions) {
        int result;
        // 권한 배열의 각 권한 상태 확인
        for (String perms : permissions) {
            result = ContextCompat.checkSelfPermission(this, perms);
            if (result == PackageManager.PERMISSION_DENIED) {
                // 권한이 허용되지 않음
                return false;
            }
        }
        // 모든 권한 허용됨
        return true;
    }

    private void checkPermission(boolean isGranted) {
        if (isGranted) {
            permissionGranted();
        } else {
            showToast("권한이 허용되지 않았습니다.", true);
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSION) {
            if (grantResults.length > 0) {
                boolean cameraPermissionAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                if (cameraPermissionAccepted) {
                    checkPermission(true);
                } else {
                    checkPermission(false);
                }
            }
        }
    }

    private void permissionGranted() {
        setViewAtGazeTrackerState();
    }

    // 뷰 초기화
    private void initView() {
        layoutProgress = findViewById(R.id.layout_progress);
        layoutProgress.setOnClickListener(null);

        viewWarningTracking = findViewById(R.id.view_warning_tracking);

        preview = findViewById(R.id.preview);
        preview.setSurfaceTextureListener(surfaceTextureListener);

        viewPoint = findViewById(R.id.view_point);
        viewCalibration = findViewById(R.id.view_calibration_game);

        choosingSelectButton = findViewById(R.id.chooing_select);
        oppositeSelectButton = findViewById(R.id.opposite_select);

        pointText = findViewById(R.id.point_text);

        // 색깔 고르기 게임으로 이동하는 버튼 동작 설정
        choosingSelectButton.setOnClickListener(v -> {
            if (startCalibration()) {
                startTracking();
                mode = 1; // 색깔 고르기 게임 선택
                setCalibration();
            }
        });

        // 반대말 찾기 게임으로 이동하는 버튼 동작 설정
        oppositeSelectButton.setOnClickListener(v -> {
            mode = 2; // 반대말 찾기 게임 선택
            Intent intent = new Intent(GameSelectionActivity.this, OppositeGameActivity.class);
            startActivity(intent);
        });

        hideProgress();
        setOffsetOfView();
        setViewAtGazeTrackerState();
        initGazeTracker();
    }

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            // TextureView가 사용 가능할 때
            gazeTrackerManager.setCameraPreview(preview);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // 크기 변경 시 처리할 내용
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            // TextureView가 파괴될 때 처리할 내용
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            // TextureView가 업데이트될 때 처리할 내용
        }
    };

    // 뷰의 오프셋 설정
    private void setOffsetOfView() {
        viewLayoutChecker.setOverlayView(viewPoint, new ViewLayoutChecker.ViewLayoutListener() {
            @Override
            public void getOffset(int x, int y) {
                viewPoint.setOffset(x, y);
                viewCalibration.setOffset(x, y);
            }
        });
    }

    private void showProgress() {
        if (layoutProgress != null) {
            runOnUiThread(() -> layoutProgress.setVisibility(View.VISIBLE));
        }
    }

    private void hideProgress() {
        if (layoutProgress != null) {
            runOnUiThread(() -> layoutProgress.setVisibility(View.INVISIBLE));
        }
    }

    private void showTrackingWarning() {
        runOnUiThread(() -> viewWarningTracking.setVisibility(View.VISIBLE));
    }

    private void hideTrackingWarning() {
        runOnUiThread(() -> viewWarningTracking.setVisibility(View.INVISIBLE));
    }

    private void showToast(final String msg, final boolean isShort) {
        runOnUiThread(() -> Toast.makeText(GameSelectionActivity.this, msg, isShort ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show());
    }

    private void showGazePoint(final float x, final float y, final ScreenState type) {
        runOnUiThread(() -> {
            viewPoint.setType(type == ScreenState.INSIDE_OF_SCREEN ? PointView.TYPE_DEFAULT : PointView.TYPE_OUT_OF_SCREEN);
            viewPoint.setPosition(x, y);
        });
    }

    private void setCalibrationPoint(final float x, final float y) {
        runOnUiThread(() -> {
            viewCalibration.setVisibility(View.VISIBLE);
            viewCalibration.changeDraw(true, null);
            viewCalibration.setPointPosition(x, y);
            viewCalibration.setPointAnimationPower(0);
        });
    }

    private void setCalibrationProgress(final float progress) {
        runOnUiThread(() -> viewCalibration.setPointAnimationPower(progress));
    }

    private void hideCalibrationView() {
        runOnUiThread(() -> viewCalibration.setVisibility(View.INVISIBLE));
    }

    private void setViewAtGazeTrackerState() {
        Log.i(TAG, "gaze : " + isTrackerValid() + ", tracking " + isTracking());
        runOnUiThread(() -> {
            if (!isTracking()) {
                hideCalibrationView();
            }
        });
    }

    // GazeTracker 상태 확인
    private boolean isTrackerValid() {
        return gazeTrackerManager.hasGazeTracker();
    }

    private boolean isTracking() {
        return gazeTrackerManager.isTracking();
    }

    // GazeTracker 초기화 콜백
    private final InitializationCallback initializationCallback = new InitializationCallback() {
        @Override
        public void onInitialized(GazeTracker gazeTracker, InitializationErrorType error) {
            if (gazeTracker != null) {
                initSuccess(gazeTracker);
            } else {
                initFail(error);
            }
        }
    };

    private void initSuccess(GazeTracker gazeTracker) {
        setViewAtGazeTrackerState();
        hideProgress();
    }

    private void initFail(InitializationErrorType error) {
        hideProgress();
        showToast("GazeTracker 초기화 실패: " + error.toString(), false);
    }

    // GazeCallback 구현
    private final GazeCallback gazeCallback = new GazeCallback() {
        @Override
        public void onGaze(GazeInfo gazeInfo) {
            processOnGaze(gazeInfo);
        }
    };

    private void processOnGaze(GazeInfo gazeInfo) {
        if (gazeInfo.trackingState == TrackingState.SUCCESS) {
            hideTrackingWarning();
            if (!gazeTrackerManager.isCalibrating()) {
                float[] filtered_gaze = filterGaze(gazeInfo);
                showGazePoint(filtered_gaze[0], filtered_gaze[1], gazeInfo.screenState);
            }
        } else {
            showTrackingWarning();
        }
    }

    private float[] filterGaze(GazeInfo gazeInfo) {
        if (oneEuroFilterManager.filterValues(gazeInfo.timestamp, gazeInfo.x, gazeInfo.y)) {
            return oneEuroFilterManager.getFilteredValues();
        }
        return new float[]{gazeInfo.x, gazeInfo.y};
    }

    // CalibrationCallback 구현
    private final CalibrationCallback calibrationCallback = new CalibrationCallback() {
        @Override
        public void onCalibrationProgress(float progress) {
            setCalibrationProgress(progress);
        }

        @Override
        public void onCalibrationNextPoint(final float x, final float y) {
            setCalibrationPoint(x, y);
            // 눈이 교정 좌표를 찾을 시간을 준 후 데이터 수집 시작
            backgroundHandler.postDelayed(() -> startCollectSamples(), 1000);
        }

        @Override
        public void onCalibrationFinished(double[] calibrationData) {
            hideCalibrationView();
            showToast("시선 교정 완료", true);
            if (mode == 1) {
                Intent intent = new Intent(GameSelectionActivity.this, ChooseColorActivity.class);
                startActivity(intent);
            }
        }
    };

    // StatusCallback 구현
    private final StatusCallback statusCallback = new StatusCallback() {
        @Override
        public void onStarted() {
            // 시선 추적 시작
            setViewAtGazeTrackerState();
        }

        @Override
        public void onStopped(StatusErrorType error) {
            // 시선 추적 중지
            setViewAtGazeTrackerState();

            if (error != StatusErrorType.ERROR_NONE) {
                switch (error) {
                    case ERROR_CAMERA_START:
                        showToast("카메라 시작 오류", false);
                        break;
                    case ERROR_CAMERA_INTERRUPT:
                        showToast("카메라 중단 오류", false);
                        break;
                }
            }
        }
    };

    // UserStatusCallback 구현 (필요에 따라 추가)

    private final UserStatusCallback userStatusCallback = new UserStatusCallback() {
        @Override
        public void onAttention(long timestampBegin, long timestampEnd, float attentionScore) {
            // 주의력 상태 처리
        }

        @Override
        public void onBlink(long timestamp,
                            boolean isBlinkLeft,
                            boolean isBlinkRight,
                            boolean isBlink,
                            float leftOpenness,
                            float rightOpenness) {
            // 눈 깜빡임 상태 처리
        }

        @Override
        public void onDrowsiness(long timestamp, boolean isDrowsiness, float intensity) {
            // 졸림 상태 처리
        }
    };

    // GazeTracker 초기화 및 추적 시작
    private void initGazeTracker() {
        showProgress();
        UserStatusOption option = new UserStatusOption();
        option.useBlink();
        gazeTrackerManager.initGazeTracker(initializationCallback, option);
    }

    private void startTracking() {
        gazeTrackerManager.startGazeTracking();
    }

    private void stopTracking() {
        gazeTrackerManager.stopGazeTracking();
    }

    // 시선 교정 시작
    private boolean startCalibration() {
        boolean isSuccess = gazeTrackerManager.startCalibration(CalibrationModeType.FIVE_POINT, AccuracyCriteria.DEFAULT);
        if (!isSuccess) {
            showToast("시선 교정 시작 실패", false);
        }
        setViewAtGazeTrackerState();
        return isSuccess;
    }

    // 교정 데이터 수집 시작
    private boolean startCollectSamples() {
        boolean isSuccess = gazeTrackerManager.startCollectingCalibrationSamples();
        setViewAtGazeTrackerState();
        return isSuccess;
    }

    private void setCalibration() {
        LoadCalibrationResult result = gazeTrackerManager.loadCalibrationData();
        switch (result) {
            case SUCCESS:
                speakOut("빨간 점을 바라보세요");
                showToast("빨간 점을 바라보세요", false);
                break;
            case FAIL_DOING_CALIBRATION:
                speakOut("빨간 점을 바라보세요");
                showToast("빨간 점을 바라보세요", false);
                break;
            case FAIL_NO_CALIBRATION_DATA:
                speakOut("빨간 점을 바라보세요");
                showToast("빨간 점을 바라보세요", true);
                break;
            case FAIL_HAS_NO_TRACKER:
                speakOut("빨간 점을 바라보세요");
                showToast("빨간 점을 바라보세요", true);
                break;
        }
        setViewAtGazeTrackerState();
    }

    private void speakOut(String text) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }
}
