package com.example.eyesmemory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eyesmemory.databinding.ActivityLoginBinding;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseFirestore db;

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

        binding.r1oita9oihdr.setOnClickListener(view -> {
            // TODO: Implement ID/Password recovery functionality
            Toast.makeText(this, "아이디/비밀번호 찾기 기능은 아직 구현되지 않았습니다.", Toast.LENGTH_SHORT).show();
        });
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

                                // Save login state
                                SharedPreferences.Editor editor = getSharedPreferences("UserPrefs", MODE_PRIVATE).edit();
                                editor.putString("userId", userId);
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