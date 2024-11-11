package com.example.eyesmemory;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

public class PauseExitActivity extends AppCompatActivity {

    private ImageButton continueButton, exitButton, restartButton;
    private long remainingTime;
    private int heartCount, questionCount;
    private boolean isResuming = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pause_exit);

        continueButton = findViewById(R.id.continue_button);
        exitButton = findViewById(R.id.exit_button);
        restartButton = findViewById(R.id.restart_button);

        remainingTime = getIntent().getLongExtra("remainingTime", 60000);
        heartCount = getIntent().getIntExtra("heartCount", 3);
        questionCount = getIntent().getIntExtra("questionCount", 0);

        continueButton.setOnClickListener(v -> resumeGame());
        restartButton.setOnClickListener(v -> restartGame());
        exitButton.setOnClickListener(v -> exitGame());
    }

    private void resumeGame() {
        isResuming = true;
        Intent intent = new Intent();
        intent.putExtra("isResuming", isResuming);
        intent.putExtra("remainingTime", remainingTime);
        intent.putExtra("heartCount", heartCount);
        intent.putExtra("questionCount", questionCount);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void restartGame() {
        isResuming = false;
        Intent intent = new Intent();
        heartCount = 3;
        questionCount = 0;
        intent.putExtra("isResuming", isResuming);
        intent.putExtra("heartCount", heartCount);
        intent.putExtra("questionCount", questionCount);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void exitGame() {
        Intent intent = new Intent(PauseExitActivity.this, GameSelectionActivity.class);
        startActivity(intent);
        finish();
    }
}
