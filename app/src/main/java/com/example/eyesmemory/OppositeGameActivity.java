package com.example.eyesmemory;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

import camp.visual.gazetracker.callback.UserStatusCallback;

public class OppositeGameActivity extends AppCompatActivity {
    private TextView wordTextView1, wordTextView2, wordTextView3, infoText;
    private TextView option1Button1, option2Button1, option1Button2, option2Button2, option1Button3, option2Button3, timeView;
    private LinearLayout wordLayout1, wordLayout2, wordLayout3, option1Layout1, option2Layout1, option1Layout2, option2Layout2, option1Layout3, option2Layout3;
    private ImageButton pauseButton, faqButton;
    private ImageView life1ImageView, life2ImageView, life3ImageView, leftEyeImageView, rightEyeImageView;
    private List<WordPair> wordList;
    private int currentWordIndex = 0;
    private static final int WORDS_PER_GAME = 9;
    private int score = 0;
    private int heartCount = 3;
    private static final int POINTS_PER_GAME = 10;
    private FirebaseFirestore db;
    private String currentUserId;
    private LinearLayout problemSet1, problemSet2, problemSet3;
    private int currentProblemIndex = 0;
    private CountDownTimer countDownTimer;
    private long remainingTime = 60000; // 60 seconds
    private boolean isTimerPaused = false;
    private int questionCount = 0;
    private AlertDialog gameOverDialog;
    private boolean isGameOver = false;
    private MediaPlayer correctSound;
    private MediaPlayer wrongSound;

    private final long BLINK_DETECTION_DELAY = 200; // 딜레이 설정
    private final float CLOSED_THRESHOLD = 0.245f; // 눈 감은 것으로 간주하는 임계값
    private final float OPEN_THRESHOLD = 0.55f;   // 눈 뜬 것으로 간주하는 임계값
    private final float MAX_EYE_DIFFERENCE = 0.255f;
    // 두 눈의 openness 차이가 이 값보다 작으면 양쪽 눈이 같은 상태로 간주
    private long lastBlinkTime = 0;
    // 마지막으로 감지된 상태를 기록해 중복 클릭 방지
    private boolean isLeftEyeLastClosed = false;
    private boolean isRightEyeLastClosed = false;
    private boolean bothEyesClosed = false; // 양쪽 눈이 동시에 감겼는지 추적

    private int leftEyeFillCount;
    private int rightEyeFillCount;

    private GazeTrackerManager gazeTrackerManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opposite_game);

        initializeViews();
        updateLayoutSize();
        setupClickListeners();

        gazeTrackerManager = GazeTrackerManager.getInstance();

        db = FirebaseFirestore.getInstance();
        currentUserId = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("userId", "");
        Log.d("OppositeGameActivity", "Current User ID: " + currentUserId);

        loadWordsFromJson();
        selectRandomWords();
        updateLivesDisplay();
        displayNextWords();
        updateProblemSetHighlight();
        startTimer(remainingTime);

        // 사운드 초기화
        correctSound = MediaPlayer.create(this, R.raw.correct_answer);
        wrongSound = MediaPlayer.create(this, R.raw.wrong_answer);
    }

    private void initializeViews() {
        wordTextView1 = findViewById(R.id.wordTextView1);
        wordTextView2 = findViewById(R.id.wordTextView2);
        wordTextView3 = findViewById(R.id.wordTextView3);
        infoText = findViewById(R.id.info_text);

        wordLayout1 = findViewById(R.id.wordLayout1);
        wordLayout2 = findViewById(R.id.wordLayout2);
        wordLayout3 = findViewById(R.id.wordLayout3);

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

        life1ImageView = findViewById(R.id.left_heart);
        life2ImageView = findViewById(R.id.middle_heart);
        life3ImageView = findViewById(R.id.right_heart);

        problemSet1 = findViewById(R.id.problemSet1);
        problemSet2 = findViewById(R.id.problemSet2);
        problemSet3 = findViewById(R.id.problemSet3);

        timeView = findViewById(R.id.time_view);

        leftEyeImageView = findViewById(R.id.img_left_eye);
        rightEyeImageView = findViewById(R.id.img_right_eye);
    }

    private void updateLayoutSize() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;

        int width = (int)(Math.min(screenWidth * 0.25, (screenHeight - dpToPx(200)) * 0.2) * 0.9);
        width = Math.max(width, dpToPx(88));

        int height = (int)(Math.min(screenWidth * 0.25, (screenHeight - dpToPx(200)) * 0.2) * 0.9);
        height = Math.max(height, dpToPx(88));

        float scale = width / (float)dpToPx(98) * 0.9f;
        TextView[] textViews = new TextView[] {
                wordTextView1, wordTextView2, wordTextView3,
                option1Button1, option1Button2, option1Button3,
                option2Button1, option2Button2, option2Button3
        };
        for (TextView textView : textViews) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textView.getTextSize() * scale);
        }

        View[] layouts = new View[] {
                wordLayout1, wordLayout2, wordLayout3,
                option1Layout1, option1Layout2, option1Layout3,
                option2Layout1, option2Layout2, option2Layout3
        };
        for (View layout : layouts) {
            ViewGroup.LayoutParams param = layout.getLayoutParams();
            param.height = height;
            param.width = width;
            layout.setLayoutParams(param);
        }
    }

    private void setupClickListeners() {
        View.OnClickListener optionClickListener = this::handleOptionClick;

        option1Layout1.setOnClickListener(optionClickListener);
        option2Layout1.setOnClickListener(optionClickListener);
        option1Layout2.setOnClickListener(optionClickListener);
        option2Layout2.setOnClickListener(optionClickListener);
        option1Layout3.setOnClickListener(optionClickListener);
        option2Layout3.setOnClickListener(optionClickListener);

        pauseButton.setOnClickListener(v -> showPauseScreen());
        faqButton.setOnClickListener(v -> showGameExplanationDialog());
    }

    private void handleOptionClick(View view) {
        clearFillButton();

        LinearLayout selectedLayout = (LinearLayout) view;
        TextView selectedTextView = (TextView) selectedLayout.getChildAt(0);
        String selectedAnswer = selectedTextView.getText().toString();
        checkAnswer(selectedAnswer);
    }

    private final ActivityResultLauncher<Intent> pauseResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        boolean isResuming = data.getBooleanExtra("isResuming", false);
                        if (isResuming) {
                            remainingTime = data.getLongExtra("remainingTime", 60000);
                            heartCount = data.getIntExtra("heartCount", heartCount);
                            questionCount = data.getIntExtra("questionCount", questionCount);

                            updateLivesDisplay();
                            updateProblemSetHighlight();
                            displayNextWords();

                            startTimer(remainingTime);
                        } else {
                            restartGame();
                        }
                    }
                }
            }
    );


    private void updateLivesDisplay() {
        life1ImageView.setVisibility(heartCount >= 1 ? View.VISIBLE : View.INVISIBLE);
        life2ImageView.setVisibility(heartCount >= 2 ? View.VISIBLE : View.INVISIBLE);
        life3ImageView.setVisibility(heartCount >= 3 ? View.VISIBLE : View.INVISIBLE);

        if (heartCount <= 0 && !isGameOver) {
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
        questionCount++;
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

    private void clearFillButton() {
        leftEyeFillCount = 0;
        rightEyeFillCount = 0;
        fillButton(0, true);
        fillButton(0, false);
    }


    private void fillButton(float percentage, boolean isLeft) {
        List<LinearLayout> buttons = isLeft
                ? List.of(option1Layout1, option1Layout2, option1Layout3)
                : List.of(option2Layout1, option2Layout2, option2Layout3);

        int index = currentProblemIndex % 3;
        LinearLayout button = buttons.get(index);

        int buttonHeight = button.getHeight();

        GradientDrawable whiteDrawable = new GradientDrawable();
        whiteDrawable.setColor(0xFFFFFFFF);
        whiteDrawable.setShape(GradientDrawable.RECTANGLE);
        whiteDrawable.setCornerRadius(dpToPx(19));
        whiteDrawable.setStroke(dpToPx(3), 0xFF1C6CC2);

        GradientDrawable redDrawable = new GradientDrawable();
        redDrawable.setColor(0xFFFF9E9E); // 붉은색
        redDrawable.setShape(GradientDrawable.RECTANGLE);
        redDrawable.setCornerRadius(dpToPx(19)); // 둥근 모서리
        redDrawable.setStroke(dpToPx(3), 0xFF1C6CC2);

        // ClipDrawable을 사용하여 빨간색 드로어블을 클리핑
        ClipDrawable clipDrawable = new ClipDrawable(redDrawable, Gravity.BOTTOM, ClipDrawable.VERTICAL);
        clipDrawable.setLevel((int) (percentage * 10000));

        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{whiteDrawable, clipDrawable});

        button.setBackground(layerDrawable);
    }


    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void checkAnswerByBlink(boolean isLeft) {
        List<LinearLayout> buttons = isLeft
                ? List.of(option1Layout1, option1Layout2, option1Layout3)
                : List.of(option2Layout1, option2Layout2, option2Layout3);

        int index = currentProblemIndex % 3;
        LinearLayout button = buttons.get(index);
        runOnUiThread(button::performClick);
    }

    private void checkAnswer(String selectedAnswer) {
        if (heartCount <= 0) return;
        WordPair currentWord = wordList.get(currentWordIndex);
        String correctAnswer = currentWord.getAnswer();
        if (selectedAnswer.equals(correctAnswer)) {
            score++;
            Toast.makeText(this, "정답입니다!", Toast.LENGTH_SHORT).show();
            // 정답일 때 사운드 재생
            if (correctSound != null) {
                correctSound.start();
            }
        } else {
            heartCount--;
            updateLivesDisplay();
            Toast.makeText(this, "틀렸습니다. 정답은 " + correctAnswer + "입니다.", Toast.LENGTH_SHORT).show();
            // 틀렸을 때 사운드 재생
            if (wrongSound != null) {
                wrongSound.start();
            }
            if (heartCount <= 0) {
                showGameOverDialog();
                return;
            }
        }
        currentWordIndex++;
        currentProblemIndex++;
        if (currentWordIndex >= wordList.size()) {
            showGameOverDialog();
            return;
        }
        if (currentWordIndex % 3 == 0) {
            displayNextWords();
        }
        updateProblemSetHighlight();
    }

    private void resumeTimer() {
        if (isTimerPaused) {
            startTimer(remainingTime);
            isTimerPaused = false;
        }
    }

    private void showPauseScreen() {
        if (countDownTimer != null) countDownTimer.cancel();
        Intent intent = new Intent(this, com.example.eyesmemory.PauseExitActivity.class);
        intent.putExtra("remainingTime", remainingTime);
        intent.putExtra("heartCount", heartCount);
        intent.putExtra("questionCount", questionCount);
        pauseResultLauncher.launch(intent);
    }

    private void showGameExplanationDialog() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            isTimerPaused = true;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("게임 설명")
                .setMessage("이 게임은 주어진 단어의 반대말을 선택하는 게임입니다. " +
                        "정답을 맞추면 점수를 얻고, 틀리면 생명이 줄어듭니다. " +
                        "3번 틀리면 게임이 종료됩니다.")
                .setPositiveButton("확인", (dialog, id) -> {
                    dialog.dismiss();
                    resumeTimer();
                });
        AlertDialog dialog = builder.create();
        dialog.setOnCancelListener(dialogInterface -> resumeTimer());
        dialog.show();
    }

    private void startTimer(long timeInMillis) {
        countDownTimer = new CountDownTimer(timeInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingTime = millisUntilFinished;
                timeView.setText(millisUntilFinished / 1000 + "초");
            }

            @Override
            public void onFinish() {
                // 타이머가 끝났을 때 게임 종료
                showGameOverDialog();
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        // MediaPlayer 리소스 해제
        if (correctSound != null) {
            correctSound.release();
            correctSound = null;
        }
        if (wrongSound != null) {
            wrongSound.release();
            wrongSound = null;
        }
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

    private void purchaseLife() {
        DocumentReference userRef = db.collection("users").document(currentUserId);
        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(userRef);
            Long currentPoints = snapshot.getLong("points");
            if (currentPoints == null || currentPoints < 5) {
                return null; // 포인트가 부족한 경우 null 반환
            }
            long newPoints = currentPoints - 5;
            transaction.update(userRef, "points", newPoints);
            return newPoints;
        }).addOnSuccessListener(result -> {
            if (result == null) {
                Toast.makeText(OppositeGameActivity.this, "포인트가 부족합니다.", Toast.LENGTH_SHORT).show();
                // GameSelectionActivity로 이동
                Intent intent = new Intent(OppositeGameActivity.this, GameSelectionActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            } else {
                heartCount = 1;
                updateLivesDisplay();
                Toast.makeText(OppositeGameActivity.this, "목숨이 회복되었습니다!", Toast.LENGTH_SHORT).show();
                if (gameOverDialog != null && gameOverDialog.isShowing()) {
                    gameOverDialog.dismiss();
                }
                isGameOver = false;  // 게임 오버 상태 해제
                continueGame();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(OppositeGameActivity.this, "오류가 발생했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void purchaseTime() {
        DocumentReference userRef = db.collection("users").document(currentUserId);
        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(userRef);
            Long currentPoints = snapshot.getLong("points");
            if (currentPoints == null || currentPoints < 5) {
                return null; // 포인트가 부족한 경우 null 반환
            }
            long newPoints = currentPoints - 5;
            transaction.update(userRef, "points", newPoints);
            return newPoints;
        }).addOnSuccessListener(result -> {
            if (result == null) {
                Toast.makeText(OppositeGameActivity.this, "포인트가 부족합니다.", Toast.LENGTH_SHORT).show();
                // GameSelectionActivity로 이동
                Intent intent = new Intent(OppositeGameActivity.this, GameSelectionActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            } else {
                remainingTime += 10000; // 10초 추가
                Toast.makeText(OppositeGameActivity.this, "시간이 10초 추가되었습니다!", Toast.LENGTH_SHORT).show();
                continueGame();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(OppositeGameActivity.this, "오류가 발생했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void continueGame() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        startTimer(remainingTime);
        if (currentWordIndex % 3 == 0) {
            displayNextWords();
        }
        updateProblemSetHighlight();
    }

    private void showGameOverDialog() {
        if (isGameOver) return;  // 이미 게임 오버 상태라면 다이얼로그를 다시 표시하지 않음
        isGameOver = true;

        clearFillButton();

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        int earnedPoints = 0;
        String message = "당신의 점수: " + score + "/" + wordList.size();
        if (currentWordIndex >= wordList.size() && heartCount > 0) {
            earnedPoints = POINTS_PER_GAME;
            message += "\n획득한 포인트: " + earnedPoints;
            updateUserPoints(earnedPoints);
        } else if (heartCount <= 0) {
            message += "\n목숨을 모두 잃었습니다. 목숨을 구입하시겠습니까?";
        } else {
            message += "\n시간이 초과되었습니다. 시간을 구입하시겠습니까?";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("게임 종료")
                .setMessage(message)
                .setPositiveButton("다시 시작", (dialog, which) -> restartGame())
                .setNegativeButton("종료", (dialog, which) -> {
                    Intent intent = new Intent(this, GameSelectionActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false);

        if (heartCount <= 0) {
            builder.setNeutralButton("목숨 구입 (5 포인트)", (dialog, which) -> purchaseLife());
        }else if(remainingTime<=1000){
            builder.setNeutralButton("시간 구입 (5 포인트)", (dialog, which) -> purchaseTime());
        }

        gameOverDialog = builder.create();
        gameOverDialog.show();
    }

    private void restartGame() {
        clearFillButton();
        Log.d("OppositeGameActivity", "restartGame called");
        if (gameOverDialog != null && gameOverDialog.isShowing()) {
            gameOverDialog.dismiss();
        }
        isGameOver = false;

        currentWordIndex = 0;
        currentProblemIndex = 0;
        score = 0;
        heartCount = 3;
        updateLivesDisplay();
        selectRandomWords();
        displayNextWords();
        updateProblemSetHighlight();

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        startTimer(60000);
        isTimerPaused = false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        gazeTrackerManager.setGazeTrackerCallbacks(userStatusCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        gazeTrackerManager.startGazeTracking();
    }

    @Override
    protected void onPause() {
        super.onPause();
        gazeTrackerManager.stopGazeTracking();
    }

    @Override
    protected void onStop() {
        super.onStop();
        gazeTrackerManager.removeCallbacks(userStatusCallback);
    }

    private final UserStatusCallback userStatusCallback = new UserStatusCallback() {
        @Override
        public void onAttention(long timestampBegin, long timestampEnd, float attentionScore) {
            // 주의력 상태 처리
        }

        private static final int CLICK_THRESHOLD = 2200;

        @Override
        public void onBlink(long timestamp,
                            boolean isBlinkLeft,
                            boolean isBlinkRight,
                            boolean isBlink,
                            float leftOpenness,
                            float rightOpenness) {
            runOnUiThread(() -> {
                String formatted1 = String.format("%010.5f", leftOpenness * 100);
                String formatted2 = String.format("%010.5f", rightOpenness * 100);
                infoText.setText(formatted1 + " " + formatted2);
            });
            if (isTimerPaused || isGameOver) return;
            if (leftOpenness == -1 || rightOpenness == -1) return;
            if (timestamp - lastBlinkTime < BLINK_DETECTION_DELAY) return; // 연속 감지 방지
            lastBlinkTime = timestamp;

            boolean isLeftEyeClosed = leftOpenness < CLOSED_THRESHOLD;
            boolean isRightEyeClosed = rightOpenness < CLOSED_THRESHOLD;
            boolean isLeftEyeOpen = leftOpenness > OPEN_THRESHOLD;
            boolean isRightEyeOpen = rightOpenness > OPEN_THRESHOLD;

            runOnUiThread(() -> {
                if (isLeftEyeOpen) {
                    leftEyeImageView.setImageResource(visual.camp.sample.view.R.drawable.baseline_visibility_black_48);
                } else {
                    leftEyeImageView.setImageResource(visual.camp.sample.view.R.drawable.baseline_visibility_off_black_48);

                    leftEyeFillCount++;
                    float percentage = leftEyeFillCount / ((float)CLICK_THRESHOLD / BLINK_DETECTION_DELAY);
                    fillButton(percentage, true);

                    if (percentage >= 1.0) {
                        clearFillButton();
                        checkAnswerByBlink(true);
                    }
                }

                if (isRightEyeOpen) {
                    rightEyeImageView.setImageResource(visual.camp.sample.view.R.drawable.baseline_visibility_black_48);
                } else {
                    rightEyeImageView.setImageResource(visual.camp.sample.view.R.drawable.baseline_visibility_off_black_48);

                    rightEyeFillCount++;
                    float percentage = rightEyeFillCount / ((float)CLICK_THRESHOLD / BLINK_DETECTION_DELAY);
                    fillButton(percentage, false);

                    if (percentage >= 1.0) {
                        clearFillButton();
                        checkAnswerByBlink(false);
                    }
                }
            });
        }

        @Override
        public void onDrowsiness(long timestamp, boolean isDrowsiness, float intensity) {
            // 졸림 상태 처리
        }
    };
}