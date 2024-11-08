package com.example.eyesmemory;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class GameSelectionActivity extends AppCompatActivity {

    private ImageButton choosingSelectButton;
    private ImageButton oppositeSelectButton;
    private TextView pointText;

    // Firestore 인스턴스
    private FirebaseFirestore db;
    private static final String TAG = "GameSelectionActivity";

    // SharedPreferences 키
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_ID = "userId";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_selection);

        // Firestore 초기화
        db = FirebaseFirestore.getInstance();

        choosingSelectButton = findViewById(R.id.chooing_select);
        oppositeSelectButton = findViewById(R.id.opposite_select);
        pointText = findViewById(R.id.point_text);

        // SharedPreferences에서 사용자 ID 가져오기
        SharedPreferences sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String userId = sharedPref.getString(KEY_USER_ID, null);

        if (userId != null) {
            // Firestore에서 포인트 가져오기
            fetchUserPoints(userId);
        } else {
            Log.d(TAG, "User ID not set");
        }

        // 색깔 고르기 게임으로 이동하는 버튼 동작 설정
        choosingSelectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GameSelectionActivity.this, ChooseColorActivity.class);
                startActivity(intent);
            }
        });

        // 반대말 찾기 게임으로 이동하는 버튼 동작 설정
        oppositeSelectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GameSelectionActivity.this, OppositeGameActivity.class);
                startActivity(intent);
            }
        });
    }

    // Firestore에서 사용자 포인트를 가져오는 메서드
    private void fetchUserPoints(String userId) {
        db.collection("users").document(userId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Long points = document.getLong("points");  // Firestore의 points 필드 값
                        if (points != null) {
                            pointText.setText(points + "P");  // TextView에 포인트 표시
                        }
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }


}
