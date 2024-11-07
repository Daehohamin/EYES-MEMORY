package com.example.eyesmemory;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.widget.ImageButton;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class OppositeGameActivity extends AppCompatActivity {
    private TextView wordTextView1, wordTextView2, wordTextView3;
    private TextView option1Button1, option2Button1, option1Button2, option2Button2, option1Button3, option2Button3;
    private LinearLayout option1Layout1, option2Layout1, option1Layout2, option2Layout2, option1Layout3, option2Layout3;
    private ImageButton pauseButton, faqButton;
    private ImageView life1ImageView, life2ImageView, life3ImageView;
    private List<WordPair> wordList;
    private int currentWordIndex = 0;
    private static final int WORDS_PER_GAME = 9;
    private int score = 0;
    private int lives = 3;
    private static final int POINTS_PER_GAME = 10;
    private FirebaseFirestore db;
    private String currentUserId;
    private LinearLayout problemSet1, problemSet2, problemSet3;
    private int currentProblemIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opposite_game);

        initializeViews();
        setupClickListeners();

        db = FirebaseFirestore.getInstance();
        currentUserId = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("userId", "");
        Log.d("OppositeGameActivity", "Current User ID: " + currentUserId);

        loadWordsFromJson();
        selectRandomWords();
        updateLivesDisplay();
        displayNextWords();
        updateProblemSetHighlight();
    }

    private void initializeViews() {
        wordTextView1 = findViewById(R.id.wordTextView1);
        wordTextView2 = findViewById(R.id.wordTextView2);
        wordTextView3 = findViewById(R.id.wordTextView3);

        option1Button1 = findViewById(R.id.option1Button1);
        option2Button1 = findViewById(R.id.option2Button1);
        option1Button2 = findViewById(R.id.option1Button2);
        option2Button2 = findViewById(R.id.option2Button2);
        option1Button3 = findViewById(R.id.option1Button3);
        option2Button3 = findViewById(R.id.option2Button3);

        option1Layout1 = findViewById(R.id.option1Layout1);
        option2Layout1 = findViewById(R.id.option2Layout1);
        option1Layout2 = findViewById(R.id.option1Layout2);
        option2Layout2 = findViewById(R.id.option2Layout2);
        option1Layout3 = findViewById(R.id.option1Layout3);
        option2Layout3 = findViewById(R.id.option2Layout3);

        pauseButton = findViewById(R.id.pauseButton);
        faqButton = findViewById(R.id.faqButton);

        life1ImageView = findViewById(R.id.life1ImageView);
        life2ImageView = findViewById(R.id.life2ImageView);
        life3ImageView = findViewById(R.id.life3ImageView);

        problemSet1 = findViewById(R.id.problemSet1);
        problemSet2 = findViewById(R.id.problemSet2);
        problemSet3 = findViewById(R.id.problemSet3);
    }

    private void setupClickListeners() {
        View.OnClickListener optionClickListener = this::handleOptionClick;

        option1Layout1.setOnClickListener(optionClickListener);
        option2Layout1.setOnClickListener(optionClickListener);
        option1Layout2.setOnClickListener(optionClickListener);
        option2Layout2.setOnClickListener(optionClickListener);
        option1Layout3.setOnClickListener(optionClickListener);
        option2Layout3.setOnClickListener(optionClickListener);

        pauseButton.setOnClickListener(v -> showPauseDialog());
        faqButton.setOnClickListener(v -> showGameExplanationDialog());
    }

    private void handleOptionClick(View view) {
        LinearLayout selectedLayout = (LinearLayout) view;
        TextView selectedTextView = (TextView) selectedLayout.getChildAt(0);
        String selectedAnswer = selectedTextView.getText().toString();
        checkAnswer(selectedAnswer);
    }

    private void updateLivesDisplay() {
        life1ImageView.setVisibility(lives >= 1 ? View.VISIBLE : View.INVISIBLE);
        life2ImageView.setVisibility(lives >= 2 ? View.VISIBLE : View.INVISIBLE);
        life3ImageView.setVisibility(lives >= 3 ? View.VISIBLE : View.INVISIBLE);

        if (lives <= 0) {
            showGameOverDialog();
        }
    }

    private void loadWordsFromJson() {
        try {
            InputStream is = getAssets().open("words.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            JSONObject jsonObject = new JSONObject(json);
            JSONArray jsonArray = jsonObject.getJSONArray("words");
            List<WordPair> allWords = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject wordObject = jsonArray.getJSONObject(i);
                WordPair wordPair = new WordPair(wordObject);
                allWords.add(wordPair);
            }

            Collections.shuffle(allWords);
            wordList = allWords.subList(0, Math.min(WORDS_PER_GAME, allWords.size()));

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "단어 로딩 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void selectRandomWords() {
        Collections.shuffle(wordList);
        if (wordList.size() > WORDS_PER_GAME) {
            wordList = wordList.subList(0, WORDS_PER_GAME);
        }
    }

    private void displayNextWords() {
        if (currentWordIndex + 2 < wordList.size()) {
            displayWord(wordTextView1, option1Button1, option2Button1, currentWordIndex);
            displayWord(wordTextView2, option1Button2, option2Button2, currentWordIndex + 1);
            displayWord(wordTextView3, option1Button3, option2Button3, currentWordIndex + 2);
        } else {
            showGameOverDialog();
        }
    }

    private void displayWord(TextView wordView, TextView option1View, TextView option2View, int index) {
        WordPair currentWord = wordList.get(index);
        wordView.setText(currentWord.getWord());
        List<String> options = new ArrayList<>(currentWord.getOptions());
        Collections.shuffle(options);
        option1View.setText(options.get(0));
        option2View.setText(options.get(1));
    }

    private void updateProblemSetHighlight() {
        problemSet1.setBackgroundResource(currentProblemIndex % 3 == 0 ? R.drawable.problem_set_highlighted : android.R.color.transparent);
        problemSet2.setBackgroundResource(currentProblemIndex % 3 == 1 ? R.drawable.problem_set_highlighted : android.R.color.transparent);
        problemSet3.setBackgroundResource(currentProblemIndex % 3 == 2 ? R.drawable.problem_set_highlighted : android.R.color.transparent);

        // 개별 선택지 활성화/비활성화
        setOptionLayoutsEnabled(option1Layout1, option2Layout1, currentProblemIndex % 3 == 0);
        setOptionLayoutsEnabled(option1Layout2, option2Layout2, currentProblemIndex % 3 == 1);
        setOptionLayoutsEnabled(option1Layout3, option2Layout3, currentProblemIndex % 3 == 2);
    }

    private void setOptionLayoutsEnabled(LinearLayout option1, LinearLayout option2, boolean enabled) {
        option1.setEnabled(enabled);
        option2.setEnabled(enabled);
        option1.setAlpha(enabled ? 1.0f : 0.5f);
        option2.setAlpha(enabled ? 1.0f : 0.5f);
    }


    private void checkAnswer(String selectedAnswer) {
        WordPair currentWord = wordList.get(currentWordIndex);
        String correctAnswer = currentWord.getAnswer();

        if (selectedAnswer.equals(correctAnswer)) {
            score++;
            Toast.makeText(this, "정답입니다!", Toast.LENGTH_SHORT).show();
        } else {
            lives--;
            updateLivesDisplay();
            Toast.makeText(this, "틀렸습니다. 정답은 " + correctAnswer + "입니다.", Toast.LENGTH_SHORT).show();

            if (lives <= 0) {
                showGameOverDialog();
                return;
            }
        }

        // 현재 문제 인덱스 증가
        currentWordIndex++;
        currentProblemIndex++;

        // 모든 문제를 다 풀었는지 확인
        if (currentWordIndex >= wordList.size()) {
            showGameOverDialog(); // 모든 문제를 다 풀었으면 게임 종료
            return;
        }

        // 3문제마다 새로운 문제를 표시
        if (currentWordIndex % 3 == 0) {
            displayNextWords();
        }

        updateProblemSetHighlight();
    }

    private void showPauseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("게임 일시정지")
                .setItems(new CharSequence[]{"계속하기", "나가기", "다시하기"}, (dialog, which) -> {
                    switch (which) {
                        case 0: // 계속하기
                            dialog.dismiss();
                            break;
                        case 1: // 나가기
                            finish();
                            break;
                        case 2: // 다시하기
                            restartGame();
                            break;
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void showGameExplanationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("게임 설명")
                .setMessage("이 게임은 주어진 단어의 반대말을 선택하는 게임입니다. " +
                        "정답을 맞추면 점수를 얻고, 틀리면 생명이 줄어듭니다. " +
                        "3번 틀리면 게임이 종료됩니다.")
                .setPositiveButton("확인", (dialog, id) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateUserPoints(int earnedPoints) {
        Log.d("OppositeGameActivity", "updateUserPoints called with " + earnedPoints + " points");
        if (currentUserId.isEmpty()) {
            Log.e("OppositeGameActivity", "User ID is empty. Cannot update points.");
            return;
        }

        DocumentReference userRef = db.collection("users").document(currentUserId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(userRef);
            Long currentPoints = snapshot.getLong("points");
            if (currentPoints == null) {
                currentPoints = 0L;
            }
            long newPoints = currentPoints + earnedPoints;
            transaction.update(userRef, "points", newPoints);
            return newPoints;
        }).addOnSuccessListener(newPoints -> {
            Log.d("OppositeGameActivity", "Points updated successfully. New total: " + newPoints);
            Toast.makeText(OppositeGameActivity.this,
                    earnedPoints + " 포인트가 추가되었습니다!", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Log.e("OppositeGameActivity", "Failed to update points: " + e.getMessage());
            Toast.makeText(OppositeGameActivity.this,
                    "포인트 업데이트에 실패했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void showGameOverDialog() {
        int earnedPoints = (lives > 0) ? POINTS_PER_GAME : 0;

        updateUserPoints(earnedPoints);

        String message = "당신의 점수: " + score + "/" + wordList.size();
        if (earnedPoints > 0) {
            message += "\n획득한 포인트: " + earnedPoints;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("게임 종료")
                .setMessage(message)
                .setPositiveButton("다시 시작", (dialog, which) -> restartGame())
                .setNegativeButton("종료", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }


    private void restartGame() {
        currentWordIndex = 0;
        currentProblemIndex = 0;
        score = 0;
        lives = 3;
        updateLivesDisplay();
        selectRandomWords();
        displayNextWords();
        updateProblemSetHighlight();
    }
}