package com.example.eyesmemory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private EditText register_id, register_pw1, register_pw2, register_name;
    private LinearLayout btn_signup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("RegisterActivity", "onCreate 호출됨");

        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_register);

        db = FirebaseFirestore.getInstance();

        register_id = findViewById(R.id.register_id);
        register_pw1 = findViewById(R.id.register_pw1);
        register_pw2 = findViewById(R.id.register_pw2);
        register_name = findViewById(R.id.register_name);
        btn_signup = findViewById(R.id.btn_myProfile);

        TextView titleText = findViewById(R.id.rn8lo7ibnpeb);
        titleText.setText("회원가입");

        btn_signup.setOnClickListener(view -> registerUser());
    }

    private void registerUser() {
        Log.d("RegisterActivity", "registerUser 메서드 시작");

        String userId = register_id.getText().toString();
        String pwd = register_pw1.getText().toString();
        String confirmPwd = register_pw2.getText().toString();
        String userName = register_name.getText().toString();

        Log.d("RegisterActivity", "입력값: userId=" + userId + ", userName=" + userName);

        if (userId.isEmpty() || pwd.isEmpty() || confirmPwd.isEmpty() || userName.isEmpty()) {
            Log.d("RegisterActivity", "입력값 누락");
            Toast.makeText(this, "모든 필드를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!pwd.equals(confirmPwd)) {
            Log.d("RegisterActivity", "비밀번호가 일치하지 않습니다.");
            Toast.makeText(this, "비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("RegisterActivity", "사용자 ID 중복 확인 시작");

        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d("RegisterActivity", "이미 존재하는 사용자 ID입니다.");
                            Toast.makeText(RegisterActivity.this, "이미 존재하는 사용자 ID입니다.", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d("RegisterActivity", "새 사용자 등록 시작");
                            UserAccount newUser = new UserAccount(userId, pwd, userName);
                            db.collection("users").document(userId).set(newUser)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("RegisterActivity", "회원가입 성공");
                                        Toast.makeText(RegisterActivity.this, "회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show();

                                        // 자동 로그인 처리
                                        loginUser(userId, pwd); // 자동 로그인 메서드 호출

                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("RegisterActivity", "회원가입 실패", e);
                                        Toast.makeText(RegisterActivity.this, "회원가입에 실패하였습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Log.e("RegisterActivity", "사용자 확인 실패", task.getException());
                        Toast.makeText(RegisterActivity.this, "사용자 확인에 실패하였습니다: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 자동 로그인 메서드 추가
    private void loginUser(String userId, String userPw) {
        Log.d("RegisterActivity", "자동 로그인 시도: userId=" + userId);

        // Firestore에서 사용자 정보 확인
        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String storedPwd = document.getString("pwd");

                            if (storedPwd != null && storedPwd.equals(userPw)) {
                                Log.d("RegisterActivity", "자동 로그인 성공");

                                // 로그인 상태 저장
                                SharedPreferences.Editor editor = getSharedPreferences("UserPrefs", MODE_PRIVATE).edit();
                                editor.putString("userId", userId);
                                editor.apply();

                                // MainActivity로 이동
                                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Log.d("RegisterActivity", "자동 로그인 실패: 비밀번호가 일치하지 않음");
                                Toast.makeText(RegisterActivity.this, "자동 로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.d("RegisterActivity", "사용자 ID를 찾을 수 없음");
                            Toast.makeText(RegisterActivity.this, "자동 로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e("RegisterActivity", "자동 로그인 중 오류 발생: ", task.getException());
                        Toast.makeText(RegisterActivity.this, "자동 로그인 중 오류 발생: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}