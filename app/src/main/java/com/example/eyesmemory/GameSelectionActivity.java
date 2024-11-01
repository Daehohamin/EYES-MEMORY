package com.example.eyesmemory;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

public class GameSelectionActivity extends AppCompatActivity {

    private ImageButton chooingSelectButton;
    private ImageButton oppositeSelectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_selection);

        // 색깔 고르기 게임 버튼
        chooingSelectButton = findViewById(R.id.chooing_select);
        // 반대말 찾기 버튼 (아직 동작 없음)
        oppositeSelectButton = findViewById(R.id.opposite_select);

        // 색깔 고르기 게임으로 이동하는 버튼 동작 설정
        chooingSelectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // MainActivity (색깔 고르기 게임 화면)로 이동
                Intent intent = new Intent(GameSelectionActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        // oppositeSelectButton은 아직 동작 없음
    }
}
