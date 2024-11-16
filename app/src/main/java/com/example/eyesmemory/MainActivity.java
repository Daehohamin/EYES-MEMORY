package com.example.eyesmemory;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class MainActivity extends AppCompatActivity {

    private Button btnNavigateToLogin;
    private ImageButton btnMyProfile, btnBrainGame, btnExercise;
    private SharedPreferences sharedPreferences;
    private ActivityResultLauncher<Intent> loginLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        btnNavigateToLogin = findViewById(R.id.btnNavigateToLogin);
        btnMyProfile = findViewById(R.id.btn_myProfile);
        btnBrainGame = findViewById(R.id.btn_brainGame);
        btnExercise = findViewById(R.id.btn_exercise);

        loginLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getBooleanExtra("LOGIN_SUCCESS", false)) {
                            updateLoginLogoutButton();
                            Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // 뒤로 가기 버튼 처리를 위한 콜백 추가
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitConfirmationDialog();
            }
        });

        btnNavigateToLogin.setOnClickListener(v -> handleLoginLogout());

        btnMyProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MyInformActivity.class);
            startActivity(intent);
        });

        btnBrainGame.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GameSelectionActivity.class);
            startActivity(intent);
        });

        btnExercise.setOnClickListener(v -> {
            Toast.makeText(this, "체조 기능은 아직 구현되지 않았습니다.", Toast.LENGTH_SHORT).show();
        });

        updateLoginLogoutButton();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLoginLogoutButton();
    }

    private void showExitConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("앱 종료")
                .setMessage("앱을 종료하시겠습니까?")
                .setPositiveButton("예", (dialog, which) -> {
                    // 사용자가 "예"를 선택한 경우 앱 종료
                    finishAffinity();
                })
                .setNegativeButton("아니오", (dialog, which) -> {
                    // 사용자가 "아니오"를 선택한 경우 대화상자만 닫힘
                    dialog.dismiss();
                })
                .show();
    }

    private void updateLoginLogoutButton() {
        String userId = sharedPreferences.getString("userId", "");
        if (!userId.isEmpty()) {
            btnNavigateToLogin.setText("로그아웃");
        } else {
            btnNavigateToLogin.setText("로그인");
        }
    }

    private void handleLoginLogout() {
        String userId = sharedPreferences.getString("userId", "");
        if (!userId.isEmpty()) {
            // 로그아웃 처리
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove("userId");
            editor.apply();
            Toast.makeText(this, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show();

            // 로그아웃 후 LoginActivity로 이동
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); // MainActivity 종료
        } else {
            // 로그인 화면으로 이동
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            loginLauncher.launch(intent);
        }

    }
}