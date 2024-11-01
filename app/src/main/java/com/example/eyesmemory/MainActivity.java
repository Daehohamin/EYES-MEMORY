package com.example.eyesmemory;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private TextView changeText, timeView;
    private ImageView leftHeart, middleHeart, rightHeart;
    private int heartCount = 3;
    private int questionCount = 0;
    private String correctColor;
    private CountDownTimer countDownTimer;
    private long remainingTime = 60000; // 초기 타이머 값 (1분)

    private String[] colorNames = {"검정", "빨강", "파랑", "초록", "노랑", "주황", "자주", "하늘"};
    private int[] colorValues = {Color.BLACK,Color.RED,Color.BLUE,Color.GREEN,Color.YELLOW,Color.rgb(255, 165, 0),Color.MAGENTA,Color.CYAN};

    private ImageButton colorButton1, colorButton2, colorButton3, pauseButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        changeText = findViewById(R.id.change_text);
        timeView = findViewById(R.id.time_view);
        leftHeart = findViewById(R.id.left_heart);
        middleHeart = findViewById(R.id.middle_heart);
        rightHeart = findViewById(R.id.right_heart);
        colorButton1 = findViewById(R.id.button_left);
        colorButton2 = findViewById(R.id.button_middle);
        colorButton3 = findViewById(R.id.button_right);
        pauseButton = findViewById(R.id.pause_button);

        startGame();
        startTimer(remainingTime);

        colorButton1.setOnClickListener(v -> checkAnswer((String) colorButton1.getTag()));
        colorButton2.setOnClickListener(v -> checkAnswer((String) colorButton2.getTag()));
        colorButton3.setOnClickListener(v -> checkAnswer((String) colorButton3.getTag()));
        pauseButton.setOnClickListener(v -> showPauseScreen());
    }

    private void startGame() {
        if (questionCount < 10 && heartCount > 0) {
            generateQuestion();
        } else {
            showEndDialog("게임 성공!\n+300p");
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
        button.setBackgroundColor(colorValues[colorIndex]);
        button.setTag(colorNames[colorIndex]);
    }

    private void checkAnswer(String color) {
        if (correctColor.equals(color)) {
            questionCount++;
            startGame();
        } else {
            heartCount--;
            updateHearts();
            if (heartCount == 0) {
                showEndDialog("하트가 모두 소진되었습니다.");
            }
        }
    }

    private void updateHearts() {
        leftHeart.setVisibility(View.VISIBLE);
        middleHeart.setVisibility(View.VISIBLE);
        rightHeart.setVisibility(View.VISIBLE);
        if (heartCount == 2) {
            rightHeart.setVisibility(View.INVISIBLE);
        } else if (heartCount == 1) {
            rightHeart.setVisibility(View.INVISIBLE);
            middleHeart.setVisibility(View.INVISIBLE);
        } else if (heartCount == 0) {
            rightHeart.setVisibility(View.INVISIBLE);
            middleHeart.setVisibility(View.INVISIBLE);
            leftHeart.setVisibility(View.INVISIBLE);
        }
    }

    private void showEndDialog(String message) {
        if (countDownTimer != null) countDownTimer.cancel();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setPositiveButton("다시 시작", (dialog, which) -> {
                    heartCount = 3;
                    questionCount = 0;
                    updateHearts();
                    startGame();
                    startTimer(60000);
                })
                .setNegativeButton("종료", (dialog, which) -> finish())
                .show();
    }


    private void showPauseScreen() {
        if (countDownTimer != null) countDownTimer.cancel();
        Intent intent = new Intent(this, PauseExitActivity.class);
        intent.putExtra("remainingTime", remainingTime);
        intent.putExtra("heartCount", heartCount);
        intent.putExtra("questionCount", questionCount);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            boolean isResuming = data.getBooleanExtra("isResuming", false);
            if (isResuming) {
                remainingTime = data.getLongExtra("remainingTime", 60000);
                heartCount = data.getIntExtra("heartCount", heartCount);
                questionCount = data.getIntExtra("questionCount", questionCount);
                updateHearts(); // Update heart UI when resuming
                startTimer(remainingTime);
            } else {
                heartCount = 3;
                questionCount = 0;
                updateHearts();
                startGame();
                startTimer(60000);
            }
        }
    }


}
