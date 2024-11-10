package com.example.eyesmemory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MyInformActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private FirebaseFirestore firestore;
    private TextView userNameTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_information);

        firestore = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        userNameTextView = findViewById(R.id.username_change); // '이름' TextView
        ImageButton logoutButton = findViewById(R.id.logout);
        ImageButton accountDropButton = findViewById(R.id.accountdrop);
        loadUserName();

        // 로그아웃 버튼 이벤트 처리
        logoutButton.setOnClickListener(v -> handleLogout());
        // 계정 탈퇴 버튼 이벤트 처리
        accountDropButton.setOnClickListener(v -> handleAccountDrop());

    }

    private void loadUserName() {
        // Firestore 컬렉션과 문서 참조
        String userId = "12"; // Firestore에서 가져올 사용자 ID
        firestore.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            // Firestore에서 userName 값 가져오기
                            String userName = document.getString("userName");
                            userNameTextView.setText(userName != null ? userName : "이름 불러오기 실패");
                        } else {
                            Toast.makeText(MyInformActivity.this, "사용자 데이터가 없습니다.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MyInformActivity.this, "데이터 불러오기 실패", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleLogout() {
        // 로그아웃 로직
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("userId");
        editor.apply();

        Toast.makeText(MyInformActivity.this, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show();

        // 로그아웃 후 LoginActivity로 이동
        Intent intent = new Intent(MyInformActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // MyInformActivity 종료
    }

    private void handleAccountDrop() {
        // 계정 탈퇴 로직 추가 (기능 확장 필요)
        Toast.makeText(MyInformActivity.this, "계정 탈퇴 버튼 클릭됨", Toast.LENGTH_SHORT).show();
        // 실제 계정 탈퇴 로직 구현 필요
    }

    public void onReviseNameClick(View view) {
        // "이름 변경" 클릭 시 실행될 코드
        Toast.makeText(this, "이름 변경 버튼 클릭됨", Toast.LENGTH_SHORT).show();
    }

    public void onReviseIdClick(View view) {
        // "아이디 변경" 클릭 시 실행될 코드
        Toast.makeText(this, "아이디 변경 버튼 클릭됨", Toast.LENGTH_SHORT).show();
    }

    public void onRevisePasswordClick(View view) {
        // "비밀번호 변경" 클릭 시 실행될 코드
        Toast.makeText(this, "비밀번호 변경 버튼 클릭됨", Toast.LENGTH_SHORT).show();
    }


}
