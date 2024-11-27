package com.example.eyesmemory;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.ImageButton;

public class ExerciseEndActivity extends Activity {

    private ImageButton exitButton, restartButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.exercise_end);

        exitButton = findViewById(R.id.exit_button);
        restartButton = findViewById(R.id.restart_button);

        restartButton.setOnClickListener(v -> restartGame());
        exitButton.setOnClickListener(v -> exitGame());
    }

    private void restartGame() {
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }

    private void exitGame() {
        Intent intent = new Intent(ExerciseEndActivity.this, ExerciseSelectionActivity.class);
        startActivity(intent);
        finish();
    }
}
