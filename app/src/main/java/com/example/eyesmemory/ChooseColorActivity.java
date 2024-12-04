package com.example.eyesmemory;

import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import android.content.res.ColorStateList;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import camp.visual.gazetracker.callback.GazeCallback;
import camp.visual.gazetracker.filter.OneEuroFilterManager;
import camp.visual.gazetracker.gaze.GazeInfo;
import camp.visual.gazetracker.state.EyeMovementState;
import camp.visual.gazetracker.util.ViewLayoutChecker;
import visual.camp.sample.view.GazePathView;

public class ChooseColorActivity extends AppCompatActivity {

    private TextView changeText, timeView;
    private ImageView leftHeart, middleHeart, rightHeart;
    private int score = 10; // 맞힌 문제 수
    private int heartCount = 3;
    private int questionCount = 0;
    private String correctColor;
    private CountDownTimer countDownTimer;
    private long remainingTime = 120000; // 초기 타이머 값 (2분)
    private boolean isTimerPaused = false;
    private FirebaseFirestore db;
    private String currentUserId;
    private static final int POINTS_PER_GAME = 10;

    private String[] colorNames = {"검정", "빨강", "파랑", "초록", "노랑", "주황", "자주", "하늘"};
    private int[] colorValues = {Color.BLACK, Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.rgb(255, 165, 0), Color.MAGENTA, Color.CYAN};

    private ImageButton colorButton1, colorButton2, colorButton3, pauseButton, questionButton;

    private MediaPlayer correctSound;
    private MediaPlayer wrongSound;

    private GazePathView gazePathView;
    private GazeTrackerManager gazeTrackerManager;
    private final ViewLayoutChecker viewLayoutChecker = new ViewLayoutChecker();

    private final OneEuroFilterManager oneEuroFilterManager = new OneEuroFilterManager(
            2, 30, 0.5F, 0.001F, 1.0F);

    private Map<ImageButton, Long> gazeStartTimeMap = new HashMap<>();
    private static final long GAZE_HOLD_DURATION = 1000; // 1.0초

    private final ActivityResultLauncher<Intent> pauseResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        boolean isResuming = data.getBooleanExtra("isResuming", false);
                        if (isResuming) {
                            remainingTime = data.getLongExtra("remainingTime", 60000);
                            heartCount = data.getIntExtra("heartCount", heartCount);
                            questionCount = data.getIntExtra("questionCount", questionCount);
                            updateHearts();
                            startTimer(remainingTime);
                        } else {
                            heartCount = 3;
                            questionCount = 0;
                            score = 10;
                            updateHearts();
                            startGame();
                            startTimer(60000);
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choose_color_game);

        db = FirebaseFirestore.getInstance();
        currentUserId = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("userId", "");
        Log.d("ChooseColorActivity", "Current User ID: " + currentUserId);

        changeText = findViewById(R.id.change_text);
        timeView = findViewById(R.id.time_view);
        leftHeart = findViewById(R.id.left_heart);
        middleHeart = findViewById(R.id.middle_heart);
        rightHeart = findViewById(R.id.right_heart);
        colorButton1 = findViewById(R.id.button_left);
        colorButton2 = findViewById(R.id.button_middle);
        colorButton3 = findViewById(R.id.button_right);
        pauseButton = findViewById(R.id.pauseButton);
        questionButton = findViewById(R.id.questionButton);

        gazePathView = findViewById(R.id.gazePathView);
        gazeTrackerManager = GazeTrackerManager.getInstance();

        startGame();
        startTimer(remainingTime);

        colorButton1.setOnClickListener(v -> checkAnswer((String) colorButton1.getTag()));
        colorButton2.setOnClickListener(v -> checkAnswer((String) colorButton2.getTag()));
        colorButton3.setOnClickListener(v -> checkAnswer((String) colorButton3.getTag()));
        pauseButton.setOnClickListener(v -> showPauseScreen());
        questionButton.setOnClickListener(v -> showGameExplanationDialog());

        // 사운드 초기화
        correctSound = MediaPlayer.create(this, R.raw.correct_answer);
        wrongSound = MediaPlayer.create(this, R.raw.wrong_answer);
    }

    private void startGame() {
        if (questionCount < 10 && heartCount > 0) {
            generateQuestion();
        } else if (remainingTime==0){
            showEndDialog("시간이 모두 지났습니다.\n시간을 구입하시겠습니까?");
        }else if (heartCount > 0) {
            updateUserPoints(10);
            showEndDialog("당신의 점수:" + score + "/10" +"\n획득한 포인트: +10p");
        } else {
            showEndDialog("하트가 모두 소진되었습니다.\n목숨을 구입하시겠습니까?");
        }
    }


    private void startTimer(long timeInMillis) {
        countDownTimer = new CountDownTimer(timeInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingTime = millisUntilFinished;
                timeView.setText(millisUntilFinished / 1000 + "초");
            }

            @Override
            public void onFinish() {
                showEndDialog("시간이 초과되었습니다.\n시간을 구입하시겠습니까?");
            }

        }.start();
    }

    private void generateQuestion() {
        Random random = new Random();

        int[] selectedIndices = new int[3];
        for (int i = 0; i < 3; i++) {
            int index;
            do {
                index = random.nextInt(colorNames.length);
            } while (contains(selectedIndices, index)); // 중복되지 않도록 검사
            selectedIndices[i] = index;
        }

        int textIndex = random.nextInt(3);

        // changeText에 텍스트 설정
        changeText.setText(colorNames[selectedIndices[textIndex]]);

        // 정답의 색상은 selectedIndices의 나머지 색상 중 하나로 설정
        int colorIndex = (textIndex + 1 + random.nextInt(2)) % 3; // 나머지 색 중 하나 선택
        changeText.setTextColor(colorValues[selectedIndices[colorIndex]]);

        // 정답 색상 저장
        correctColor = colorNames[selectedIndices[textIndex]];

        // 버튼 색상 설정
        for (int i = 0; i < 3; i++) {
            ImageButton button = (i == 0) ? colorButton1 : (i == 1) ? colorButton2 : colorButton3;
            setButtonColor(button, selectedIndices[i]); // 버튼에 색상 설정
            button.setTag(colorNames[selectedIndices[i]]); // 태그 설정
        }
    }

    private boolean contains(int[] array, int value) {
        for (int index : array) {
            if (index == value) {
                return true;
            }
        }
        return false;
    }

    private void setButtonColor(ImageButton button, int colorIndex) {
        button.setBackgroundTintList(ColorStateList.valueOf(colorValues[colorIndex]));
        button.setTag(colorNames[colorIndex]);
    }

    private void checkAnswer(String color) {
        if (correctColor.equals(color)) {
            questionCount++;
            // 정답일 때 사운드 재생
            if (correctSound != null) {
                correctSound.start();
            }
            startGame();
        } else {
            heartCount--;
            score--;
            updateHearts();
            // 틀렸을 때 사운드 재생
            if (wrongSound != null) {
                wrongSound.start();
            }
            if (heartCount == 0) {
                showEndDialog("목숨을 모두 잃었습니다. 목숨을 구입하시겠습니까?");
            } else {
                generateQuestion();
            }
        }
    }


    private void updateHearts() {
        leftHeart.setVisibility(heartCount >= 1 ? View.VISIBLE : View.INVISIBLE);
        middleHeart.setVisibility(heartCount >= 2 ? View.VISIBLE : View.INVISIBLE);
        rightHeart.setVisibility(heartCount >= 3 ? View.VISIBLE : View.INVISIBLE);
    }

    private void showEndDialog(String message) {
        if (countDownTimer != null) countDownTimer.cancel();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("게임 종료")
                .setMessage(message)
                .setPositiveButton("다시 시작", (dialog, which) -> restartGame())
                .setNegativeButton("종료", (dialog, which) -> {
                    Intent intent = new Intent(this, GameSelectionActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });

        if (heartCount <= 0) {
            builder.setNeutralButton("목숨 구입 (5 포인트)", (dialog, which) -> purchaseLife());
        }else if(remainingTime<=1000){
            builder.setNeutralButton("시간 구입 (5 포인트)", (dialog, which) -> purchaseTime());
        }

        builder.show();
    }

    private void purchaseLife() {
        DocumentReference userRef = db.collection("users").document(currentUserId);
        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(userRef);
            Long currentPoints = snapshot.getLong("points");
            if (currentPoints == null || currentPoints < 5) {
                return null; // 포인트가 부족한 경우 null 반환
            }
            long newPoints = currentPoints - 5;
            transaction.update(userRef, "points", newPoints);
            return newPoints;
        }).addOnSuccessListener(result -> {
            if (result == null) {
                Toast.makeText(ChooseColorActivity.this, "포인트가 부족합니다.", Toast.LENGTH_SHORT).show();
                // 포인트가 부족할 경우 게임 선택 화면으로 이동
                Intent intent = new Intent(ChooseColorActivity.this, GameSelectionActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            } else {
                heartCount = 1;
                updateHearts();
                Toast.makeText(ChooseColorActivity.this, "목숨이 회복되었습니다!", Toast.LENGTH_SHORT).show();
                continueGame();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(ChooseColorActivity.this, "오류가 발생했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void purchaseTime() {
        DocumentReference userRef = db.collection("users").document(currentUserId);
        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(userRef);
            Long currentPoints = snapshot.getLong("points");
            if (currentPoints == null || currentPoints < 5) {
                return null; // 포인트 부족 시 null 반환
            }
            long newPoints = currentPoints - 5;
            transaction.update(userRef, "points", newPoints);
            return newPoints;
        }).addOnSuccessListener(result -> {
            if (result == null) {
                Toast.makeText(ChooseColorActivity.this, "포인트가 부족합니다.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(ChooseColorActivity.this, GameSelectionActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            } else {
                remainingTime += 10000; // 10초 추가
                Toast.makeText(ChooseColorActivity.this, "시간이 10초 추가되었습니다!", Toast.LENGTH_SHORT).show();
                startTimer(remainingTime);
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(ChooseColorActivity.this, "오류가 발생했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void continueGame() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        startTimer(remainingTime);
        generateQuestion();
    }

    private void restartGame() {
        heartCount = 3;
        questionCount = 0;
        score = 10;
        updateHearts();
        startGame();
        startTimer(60000);
    }

    private void showGameExplanationDialog() {
        pauseTimer(); // 타이머 일시정지

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("게임 설명")
                .setMessage("이 게임은 주어진 단어의 뜻과 일치하는 색을 선택하는 게임입니다. " +
                        "정답을 맞추면 점수를 얻고, 틀리면 생명이 줄어듭니다. " +
                        "3번 틀리면 게임이 종료됩니다.")
                .setPositiveButton("확인", (dialog, id) -> {
                    dialog.dismiss();
                    resumeTimer(); // 타이머 재개
                });
        AlertDialog dialog = builder.create();
        dialog.setOnCancelListener(dialogInterface -> resumeTimer()); // 다이얼로그가 취소될 때 타이머 재개
        dialog.show();
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            isTimerPaused = true;
        }
    }

    private void resumeTimer() {
        if (isTimerPaused) {
            startTimer(remainingTime);
            isTimerPaused = false;
        }
    }

    private void updateUserPoints(int earnedPoints) {
        Log.d("ChooseColorActivity", "updateUserPoints called with " + earnedPoints + " points");
        if (currentUserId.isEmpty()) {
            Log.e("ChooseColorActivity", "User ID is empty. Cannot update points.");
            return;
        }

        DocumentReference userRef = db.collection("users").document(currentUserId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(userRef);
            Long currentPoints = snapshot.getLong("points");
            if (currentPoints == null) {
                currentPoints = 0L;
            }
            long newPoints = currentPoints + earnedPoints;
            transaction.update(userRef, "points", newPoints);
            return newPoints;
        }).addOnSuccessListener(newPoints -> {
            Log.d("ChooseColorActivity", "Points updated successfully. New total: " + newPoints);
            Toast.makeText(ChooseColorActivity.this,
                    earnedPoints + " 포인트가 추가되었습니다!", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Log.e("ChooseColorActivity", "Failed to update points: " + e.getMessage());
            Toast.makeText(ChooseColorActivity.this,
                    "포인트 업데이트에 실패했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }


    private void showPauseScreen() {
        if (countDownTimer != null) countDownTimer.cancel();
        Intent intent = new Intent(this, PauseExitActivity.class);
        intent.putExtra("remainingTime", remainingTime);
        intent.putExtra("heartCount", heartCount);
        intent.putExtra("questionCount", questionCount);
        pauseResultLauncher.launch(intent);
    }


    @Override
    protected void onStart() {
        super.onStart();
        gazeTrackerManager.setGazeTrackerCallbacks(gazeCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        gazeTrackerManager.startGazeTracking();
        setOffsetOfView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        gazeTrackerManager.stopGazeTracking();
    }

    @Override
    protected void onStop() {
        super.onStop();
        gazeTrackerManager.removeCallbacks(gazeCallback);
    }


    private final GazeCallback gazeCallback = new GazeCallback() {
        @Override
        public void onGaze(GazeInfo gazeInfo) {
            if (oneEuroFilterManager.filterValues(gazeInfo.timestamp, gazeInfo.x, gazeInfo.y)) {
                float[] filtered = oneEuroFilterManager.getFilteredValues();
                gazePathView.onGaze(filtered[0], filtered[1], gazeInfo.eyeMovementState == EyeMovementState.FIXATION);
                handleGazeEvent(filtered[0], filtered[1]);
            }
        }
    };

    private void setOffsetOfView() {
        viewLayoutChecker.setOverlayView(gazePathView, new ViewLayoutChecker.ViewLayoutListener() {
            @Override
            public void getOffset(int x, int y) {
                gazePathView.setOffset(x, y);
            }
        });
    }

    private void handleGazeEvent(float gazeX, float gazeY) {
        long currentTime = System.currentTimeMillis();

        List<ImageButton> buttons = List.of(colorButton1, colorButton2, colorButton3);

        for (ImageButton buttonView : buttons) {
            if (buttonView == null) continue;

            int[] location = new int[2];
            buttonView.getLocationOnScreen(location);
            float x = location[0] ; //POINT_RADIUS
            float y = location[1] ;
            float width = buttonView.getWidth() + 40;
            float height = buttonView.getHeight() + 40;

            // 시선이 특정 imageView 위에 있는지 확인
            if (gazeX >= x && gazeX <= x + width && gazeY >= y && gazeY <= y + height) {
                if (!gazeStartTimeMap.containsKey(buttonView)) {
                    gazeStartTimeMap.put(buttonView, currentTime);
                } else {
                    long gazeDuration = currentTime - gazeStartTimeMap.get(buttonView);
                    if (gazeDuration >= GAZE_HOLD_DURATION) {
                        runOnUiThread(() -> buttonView.performClick());
                        gazeStartTimeMap.remove(buttonView); // 시선이 유지된 후 맵에서 제거
                    }
                }
            } else {
                gazeStartTimeMap.remove(buttonView); // 시선이 벗어나면 맵에서 제거
            }
        }
    }
}
