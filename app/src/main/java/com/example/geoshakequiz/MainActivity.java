package com.example.geoshakequiz;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements SensorEventListener {


    private static final float SHAKE_THRESHOLD = 2.80f;
    private static final long SHAKE_COOLDOWN_MS = 1200L;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Vibrator vibrator;
    private AppDatabase db;
    private final Random random = new Random();

    private ConstraintLayout rootLayout;
    private ImageView ivFlag;
    private LinearLayout optionsContainer;
    private TextView tvScore, tvLives, tvShakePrompt;
    private Button[] btns = new Button[4];
    private Button btnBack;

    private int score = 0;
    private int lives = 3;
    private long lastShakeTime;
    private String correctCountry;
    private boolean gameStarted = false;

    static class Question {
        final String country;
        final int image;
        final List<String> wrongs;

        Question(String country, int image, String... wrongs) {
            this.country = country;
            this.image = image;
            this.wrongs = Arrays.asList(wrongs);
        }
    }

    private final List<Question> questionBank = new ArrayList<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = AppDatabase.getInstance(this);
        rootLayout       = findViewById(R.id.rootLayout);
        ivFlag           = findViewById(R.id.ivFlag);
        optionsContainer = findViewById(R.id.optionsContainer);
        tvScore          = findViewById(R.id.tvScore);
        tvLives          = findViewById(R.id.tvLives);
        tvShakePrompt    = findViewById(R.id.tvShakePrompt);
        btnBack          = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        btns[0] = findViewById(R.id.btnOpt1);
        btns[1] = findViewById(R.id.btnOpt2);
        btns[2] = findViewById(R.id.btnOpt3);
        btns[3] = findViewById(R.id.btnOpt4);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer  = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        vibrator       = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        loadQuestionBank();
    }

    private void loadQuestionBank() {
        questionBank.add(new Question("Angola",       R.drawable.flag_angola,     "Mozambique", "Portugal",     "Brazil"));
        questionBank.add(new Question("Hungary",      R.drawable.flag_hungary,    "Italy",      "Bulgaria",     "Romania"));
        questionBank.add(new Question("Portugal",     R.drawable.flag_portugal,   "Spain",      "France",       "Italy"));
        questionBank.add(new Question("Cape Verde",   R.drawable.flag_caboverde,  "Angola",     "Senegal",      "Guinea"));
        questionBank.add(new Question("Mozambique",   R.drawable.flag_mozambique, "Zimbabwe",   "South Africa", "Kenya"));
        questionBank.add(new Question("Sao Tome and Principe",     R.drawable.flag_saotome,    "Gabon",      "Nigeria",      "Togo"));
        questionBank.add(new Question("Nigeria",      R.drawable.flag_nigeria,    "Niger",      "Ghana",        "Cameroon"));
        questionBank.add(new Question("Germany",      R.drawable.flag_germany,    "Belgium",    "Austria",      "Netherlands"));
        questionBank.add(new Question("Russia",       R.drawable.flag_russia,     "Ukraine",    "Kazakhstan",   "Poland"));
        questionBank.add(new Question("France",       R.drawable.flag_france,     "Belgium",    "Netherlands",  "Ireland"));
        questionBank.add(new Question("Japan",        R.drawable.flag_japan,      "South Korea","Bangladesh",   "Singapore"));
        questionBank.add(new Question("Italy",        R.drawable.flag_italy,      "France",     "Ireland",      "Mexico"));
        questionBank.add(new Question("Sweden",       R.drawable.flag_sweden,     "Norway",     "Denmark",      "Finland"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            // SENSOR_DELAY_GAME ensures near real-time accelerometer readings
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        long now = System.currentTimeMillis();
        if (now - lastShakeTime > SHAKE_COOLDOWN_MS) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            double acceleration = Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;

            if (acceleration > SHAKE_THRESHOLD) {
                lastShakeTime = now;
                runOnUiThread(this::handleShakeGesture);
            }
        }
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private void handleShakeGesture() {
        if (!gameStarted) {
            gameStarted = true;
            tvShakePrompt.setVisibility(View.GONE);
            ivFlag.setVisibility(View.VISIBLE);
            optionsContainer.setVisibility(View.VISIBLE);
        }
        vibrate(100);
        nextQuestion();
    }

    private void nextQuestion() {
        Question q = questionBank.get(random.nextInt(questionBank.size()));
        correctCountry = q.country;
        ivFlag.setImageResource(q.image);

        List<String> options = new ArrayList<>(q.wrongs);
        options.add(q.country);
        Collections.shuffle(options);

        for (int i = 0; i < 4; i++) {
            btns[i].setText(options.get(i));
            btns[i].setOnClickListener(v -> checkAnswer(((Button) v).getText().toString()));
        }
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private void checkAnswer(String selected) {
        if (selected.equals(correctCountry)) {
            score++;
            vibrate(80);
            Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show();
            nextQuestion();
        } else {
            lives--;
            vibrate(400);
            if (lives <= 0) {
                endGame();
                return; // Do not update UI after game over
            }
            String lifeWord = lives == 1 ? "life" : "lives";
            Toast.makeText(this, "Wrong! " + lives + " " + lifeWord + " remaining.", Toast.LENGTH_SHORT).show();
            nextQuestion();
        }
        updateUI();
    }

    private void updateUI() {
        tvScore.setText("Score: " + score);

        // Build the lives display using heart icons instead of emoji
        SpannableStringBuilder livesBuilder = new SpannableStringBuilder("Lives: ");
        int currentLives = Math.max(0, lives);
        for (int i = 0; i < currentLives; i++) {
            Drawable heartIcon = ContextCompat.getDrawable(this, R.drawable.ic_heart);
            if (heartIcon != null) {
                int sizePx = (int) tvLives.getTextSize();
                heartIcon.setBounds(0, 0, sizePx, sizePx);
                livesBuilder.append(" \u00A0"); // non-breaking space as placeholder
                livesBuilder.setSpan(
                    new ImageSpan(heartIcon, ImageSpan.ALIGN_BOTTOM),
                    livesBuilder.length() - 2,
                    livesBuilder.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }
        tvLives.setText(livesBuilder);
    }

    private void endGame() {
        new Thread(() -> {
            GameResult result = new GameResult();
            result.correctAnswers = score;
            result.timestamp = System.currentTimeMillis();
            db.resultDao().insert(result);

            runOnUiThread(() -> {
                Toast.makeText(this, "Game Over! Final score: " + score, Toast.LENGTH_LONG).show();
                finish(); // Return to the start screen
            });
        }).start();
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private void vibrate(int durationMs) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(durationMs);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
