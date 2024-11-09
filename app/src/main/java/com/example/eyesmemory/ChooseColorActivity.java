package com.example.eyesmemory;

import android.content.Intent;
import android.graphics.Color;
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

import java.util.Random;
import android.content.res.ColorStateList;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ChooseColorActivity extends AppCompatActivity {

    private TextView changeText, timeView;
    private ImageView leftHeart, middleHeart, rightHeart;
    private int score = 10; // 맞힌 문제 수
    private int heartCount = 3;
    private int questionCount = 0;
    private String correctColor;
    private CountDownTimer countDownTimer;
    private long remainingTime = 60000; // 초기 타이머 값 (1분)
    private boolean isTimerPaused = false;
    private FirebaseFirestore db;
    private String currentUserId;
    private static final int POINTS_PER_GAME = 10;

    private String[] colorNames = {"검정", "빨강", "파랑", "초록", "노랑", "주황", "자주", "하늘"};
    private int[] colorValues = {Color.BLACK, Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.rgb(255, 165, 0), Color.MAGENTA, Color.CYAN};

    private ImageButton colorButton1, colorButton2, colorButton3, pauseButton, questionButton;

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

        startGame();
        startTimer(remainingTime);

        colorButton1.setOnClickListener(v -> checkAnswer((String) colorButton1.getTag()));
        colorButton2.setOnClickListener(v -> checkAnswer((String) colorButton2.getTag()));
        colorButton3.setOnClickListener(v -> checkAnswer((String) colorButton3.getTag()));
        pauseButton.setOnClickListener(v -> showPauseScreen());
        questionButton.setOnClickListener(v -> showGameExplanationDialog());
    }

    private void startGame() {
        if (questionCount < 10 && heartCount > 0) {
            generateQuestion();
        } else {
            updateUserPoints(10);
            showEndDialog("당신의 점수:" + score + "/10" +"\n획득한 포인트: +10p");
            score = 10;
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
                showEndDialog("시간 초과!");
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
            startGame();
        } else {
            heartCount--;
            score--;
            updateHearts();
            if (heartCount == 0) {
                showEndDialog("하트가 모두 소진되었습니다.");
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
                .setPositiveButton("다시 시작", (dialog, which) -> {
                    heartCount = 3;
                    questionCount = 0;
                    updateHearts();
                    startGame();
                    score = 10;
                    startTimer(60000);
                })
                .setNegativeButton("종료", (dialog, which) -> {
                    Intent intent = new Intent(this, GameSelectionActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .show();
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
}
