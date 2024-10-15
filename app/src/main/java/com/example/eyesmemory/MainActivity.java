package com.example.eyesmemory;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private TextView textView;
    private LinearLayout blueButton, redButton, blackButton;
    private ImageView heart1, heart2, heart3;
    private int correctCount = 0;
    private int heartCount = 3;
    private int currentRound = 0;
    private final int totalRounds = 10;
    private int score = 0;
    private final Random random = new Random();
    private String[] colors = {"검정", "빨강", "파랑"};
    private String correctColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.change_text);
        blueButton = findViewById(R.id.blue_button);
        redButton = findViewById(R.id.red_button);
        blackButton = findViewById(R.id.black_button);
        heart1 = findViewById(R.id.left_heart);
        heart2 = findViewById(R.id.middle_heart);
        heart3 = findViewById(R.id.right_heart);

        startGame();

        blueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAnswer("파랑");
            }
        });

        redButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAnswer("빨강");
            }
        });

        blackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAnswer("검정");
            }
        });
    }

    private void startGame() {
        nextRound();
    }

    private void nextRound() {
        if (currentRound < totalRounds) {
            currentRound++;
            generateQuestion();
        } else {
            endGame();
        }
    }

    private void generateQuestion() {
        int randomColorIndex = random.nextInt(3); // 0 ~ 2
        int randomTextColorIndex = random.nextInt(3);

        correctColor = colors[randomColorIndex];
        textView.setText(colors[randomColorIndex]);

        // 색상 변경
        switch (colors[randomTextColorIndex]) {
            case "검정":
                textView.setTextColor(getResources().getColor(R.color.black));
                break;
            case "빨강":
                textView.setTextColor(getResources().getColor(R.color.red));
                break;
            case "파랑":
                textView.setTextColor(getResources().getColor(R.color.blue));
                break;
        }
    }

    private void checkAnswer(String selectedColor) {
        if (selectedColor.equals(correctColor)) {
            correctCount++;
        } else {
            decreaseHeart();
        }

        if (heartCount > 0) {
            nextRound();
        } else {
            endGame();
        }
    }

    private void decreaseHeart() {
        heartCount--;
        switch (heartCount) {
            case 2:
                heart3.setVisibility(View.INVISIBLE);
                break;
            case 1:
                heart2.setVisibility(View.INVISIBLE);
                break;
            case 0:
                heart1.setVisibility(View.INVISIBLE);
                break;
        }
    }

    private void endGame() {
        if (heartCount > 0) {
            score = 300;
            Toast.makeText(this, "게임 성공! 점수: " + score, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "게임 오버!", Toast.LENGTH_LONG).show();
        }
    }
}
