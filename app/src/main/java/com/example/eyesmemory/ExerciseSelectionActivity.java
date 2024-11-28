package com.example.eyesmemory;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ExerciseSelectionActivity extends AppCompatActivity {

    private ImageButton handsUpSelectButton;
    private ImageButton leftHandUpSelectButton;
    private ImageButton rightHandUpSelectButton;
    private ImageButton squatSelectButton;
    private ImageButton backButton;

    private TextToSpeech tts;

    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.CAMERA // 모션 인식을 위한 카메라 권한
    };
    private static final int REQ_PERMISSION = 1000;


    // Firestore 인스턴스
    private FirebaseFirestore db;
    private static final String TAG = "GameSelectionActivity";

    // SharedPreferences 키
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_ID = "userId";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.exercise_selection);

        // TTS 초기화
        tts = new TextToSpeech(this, status -> {});

        // 뷰 초기화
        initView();
        backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        checkPermission();


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

    // 뷰 초기화
    private void initView() {
        handsUpSelectButton = findViewById(R.id.hands_up_select);
        leftHandUpSelectButton = findViewById(R.id.left_hand_up_select);
        rightHandUpSelectButton = findViewById(R.id.right_hand_up_select);
        squatSelectButton = findViewById(R.id.squat_select);

        handsUpSelectButton.setOnClickListener(v -> {
            Intent intent = new Intent(ExerciseSelectionActivity.this, HandsUpMotionCountActivity.class);
            startActivity(intent);
        });

        leftHandUpSelectButton.setOnClickListener(v -> {
            Intent intent = new Intent(ExerciseSelectionActivity.this, LeftHandUpMotionCountActivity.class);
            startActivity(intent);
        });

        rightHandUpSelectButton.setOnClickListener(v -> {
            Intent intent = new Intent(ExerciseSelectionActivity.this, RightHandUpMotionCountActivity.class);
            startActivity(intent);
        });

        squatSelectButton.setOnClickListener(v -> {
            Intent intent = new Intent(ExerciseSelectionActivity.this, SquatMotionCountActivity.class);
            startActivity(intent);
        });
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
                            // pointText.setText(points + "P");  // TextView에 포인트 표시
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
    protected void onDestroy() {
        super.onDestroy();
        
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
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
            //
            // permissionGranted();
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

    private void showToast(final String msg, final boolean isShort) {
        runOnUiThread(() -> Toast.makeText(ExerciseSelectionActivity.this, msg, isShort ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show());
    }

    private void speakOut(String text) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }
}
