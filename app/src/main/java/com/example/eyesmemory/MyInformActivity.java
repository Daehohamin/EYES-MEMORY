package com.example.eyesmemory;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MyInformActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private DocumentReference userRef;
    private SharedPreferences sharedPreferences;
    private TextView userNameTextView;
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_ID = "userId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_information);

        firestore = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        userNameTextView = findViewById(R.id.username_change);

        String userId = getUserIdFromPreferences();
        if (userId != null) {
            userRef = firestore.collection("users").document(userId);
            loadUserName();
        } else {
            showUserIdDialog();
        }

        Button logoutButton = findViewById(R.id.logout);
        Button accountDropButton = findViewById(R.id.accountdrop);

        logoutButton.setOnClickListener(v -> handleLogout());
        accountDropButton.setOnClickListener(v -> handleAccountDrop());
    }

    // Firestore에서 사용자 이름을 가져오는 메서드
    private void loadUserName() {
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    String userName = document.getString("userName");
                    userNameTextView.setText(userName != null ? userName : "이름 불러오기 실패");
                } else {
                    Toast.makeText(this, "사용자 데이터가 없습니다.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "데이터 불러오기 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 로그아웃 처리 메서드
    private void handleLogout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_USER_ID);
        editor.apply();

        Toast.makeText(this, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // 계정 탈퇴 처리 메서드
    private void handleAccountDrop() {
        new AlertDialog.Builder(this)
                .setTitle("계정 탈퇴")
                .setMessage("계정을 정말로 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.")
                .setPositiveButton("삭제", (dialog, which) -> {
                    // Firestore에서 사용자 문서 삭제
                    userRef.delete()
                            .addOnSuccessListener(aVoid -> {
                                // SharedPreferences에서 사용자 ID 삭제
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.remove(KEY_USER_ID);
                                editor.apply();

                                Toast.makeText(this, "계정이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                                // 로그인 화면으로 이동
                                Intent intent = new Intent(this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "계정 삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("취소", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // SharedPreferences에서 사용자 ID를 가져오는 메서드
    private String getUserIdFromPreferences() {
        return sharedPreferences.getString(KEY_USER_ID, null);
    }

    // SharedPreferences에 사용자 ID를 저장하는 메서드
    private void saveUserIdToPreferences(String userId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_ID, userId);
        editor.apply();
    }

    // 사용자 ID 설정 다이얼로그
    private void showUserIdDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("사용자 ID 설정");

        final EditText input = new EditText(this);
        input.setHint("사용자 ID 입력");
        builder.setView(input);

        builder.setPositiveButton("저장", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String userId = input.getText().toString().trim();
                if (!userId.isEmpty()) {
                    saveUserIdToPreferences(userId);
                    userRef = firestore.collection("users").document(userId);
                    Toast.makeText(MyInformActivity.this, "사용자 ID가 설정되었습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MyInformActivity.this, "사용자 ID를 입력해 주세요.", Toast.LENGTH_SHORT).show();
                    showUserIdDialog();
                }
            }
        });

        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // Firestore 필드를 업데이트하는 다이얼로그 생성 메서드
    private void showUpdateDialog(String title, String hint, String field) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_edit_text, null);
        EditText editText = dialogView.findViewById(R.id.edittextfield);
        editText.setHint(hint);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton("저장", (dialog, which) -> {
                    String newValue = editText.getText().toString().trim();
                    if (!newValue.isEmpty()) {
                        userRef.update(field, newValue)
                                .addOnSuccessListener(aVoid -> Toast.makeText(this, title + "이(가) 성공적으로 변경되었습니다.", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(this, title + " 변경 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(this, hint + "을(를) 입력해 주세요.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("취소", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // "이름 변경" 버튼 클릭 이벤트
    public void onReviseNameClick(View view) {
        showUpdateDialog("이름 변경", "새로운 이름 입력", "userName");
    }

    // "아이디 변경" 버튼 클릭 이벤트
    public void onReviseIdClick(View view) {
        showUpdateDialog("아이디 변경", "새로운 아이디 입력", "userId");
    }

    // "비밀번호 변경" 버튼 클릭 이벤트
    public void onRevisePasswordClick(View view) {
        showUpdateDialog("비밀번호 변경", "새로운 비밀번호 입력", "pwd");
    }
}
