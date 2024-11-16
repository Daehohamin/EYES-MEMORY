package com.example.eyesmemory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eyesmemory.databinding.ActivityLoginBinding;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseFirestore db;
    private CheckBox keepLoggedInCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();

        binding.btnLogin.setOnClickListener(view -> loginUser());
        binding.btnRegister.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        keepLoggedInCheckBox = binding.r8xy4ihly4wl;
        // 자동 로그인 확인
        checkAutoLogin();
    }

    private void checkAutoLogin() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String savedUserId = prefs.getString("userId", null);
        boolean keepLoggedIn = prefs.getBoolean("keepLoggedIn", false);

        if (savedUserId != null && keepLoggedIn) {
            // 자동 로그인 처리
            Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(mainIntent);
            finish();
        }
    }

    private void loginUser() {
        String userId = binding.inputId.getText().toString();
        String userPw = binding.inputPw.getText().toString();

        Log.d("LoginActivity", "Attempting login for user: " + userId);

        if (userId.isEmpty() || userPw.isEmpty()) {
            Toast.makeText(this, "모든 필드를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String storedPwd = document.getString("pwd");
                            String storedUserId = document.getString("userId");

                            if (storedPwd != null && storedPwd.equals(userPw) && storedUserId != null && storedUserId.equals(userId)) {
                                Log.d("LoginActivity", "Login successful");

                                // 로그인 상태 저장
                                SharedPreferences.Editor editor = getSharedPreferences("UserPrefs", MODE_PRIVATE).edit();
                                editor.putString("userId", userId);
                                editor.putBoolean("keepLoggedIn", keepLoggedInCheckBox.isChecked()); // 체크박스 상태 저장
                                editor.apply();

                                Toast.makeText(LoginActivity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();

                                // MainActivity로 이동
                                Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                                mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(mainIntent);

                                // LoginActivity 종료
                                finish();
                            } else {
                                Log.d("LoginActivity", "Login failed: Incorrect password or userId");
                                Toast.makeText(LoginActivity.this, "로그인에 실패했습니다. 아이디와 비밀번호를 확인하세요.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.d("LoginActivity", "User not found");
                            Toast.makeText(LoginActivity.this, "로그인에 실패했습니다. 아이디와 비밀번호를 확인하세요.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e("LoginActivity", "Error getting document: ", task.getException());
                        Toast.makeText(LoginActivity.this, "데이터베이스 오류: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}