package com.example.eyes_memory;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;


public class GameSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_game_selection);

        // 반대말 찾기 버튼 설정
        Button btnOppositeGame = findViewById(R.id.btnOppositeGame);
        btnOppositeGame.setOnClickListener(v -> showGameExplanationDialog());
    }

    private void showGameExplanationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("반대말 찾기 게임")
                .setMessage("이 게임은 주어진 단어의 반대말을 선택하는 게임입니다. " +
                        "정답을 맞추면 점수를 얻고, 틀리면 생명이 줄어듭니다. " +
                        "3번 틀리면 게임이 종료됩니다.")
                .setPositiveButton("게임 시작", (dialog, id) -> startOppositeGame())
                .setNegativeButton("취소", (dialog, id) -> dialog.dismiss())
                .setCancelable(true);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void startOppositeGame() {
        Intent intent = new Intent(GameSelectionActivity.this, OppositeGameActivity.class);
        startActivity(intent);
    }

}