package com.example.eyesmemory;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MyInformActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_information); // 레이아웃 파일 연결 (XML 파일 이름에 맞게 변경)

        // UI 요소 초기화
        ImageButton logoutButton = findViewById(R.id.logout);
        ImageButton accountDropButton = findViewById(R.id.accountdrop);

        // 이벤트 처리
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 로그아웃 버튼 클릭 시 동작
                Toast.makeText(MyInformActivity.this, "로그아웃 버튼 클릭됨", Toast.LENGTH_SHORT).show();
                // 로그아웃 로직 추가
            }
        });


        accountDropButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 계정 탈퇴 버튼 클릭 시 동작
                Toast.makeText(MyInformActivity.this, "계정 탈퇴 버튼 클릭됨", Toast.LENGTH_SHORT).show();
                // 계정 탈퇴 로직 추가
            }
        });
    }
}
