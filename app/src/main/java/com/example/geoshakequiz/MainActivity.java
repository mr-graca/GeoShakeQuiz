package com.example.geoshakequiz;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final float SHAKE_THRESHOLD = 2.80f;
    private static final long SHAKE_COOLDOWN_MS = 1200L;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Vibrator vibrator;
    private AppDatabase db;

    private ImageView ivFlag;
    private LinearLayout optionsContainer;
    private TextView tvScore, tvLives, tvShakePrompt;
    private final Button[] btns = new Button[4];

    private int score = 0;
    private int lives = 3;
    private long lastShakeTime;
    private String correctCountry;
    private boolean gameStarted = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = AppDatabase.getInstance(this);
        ivFlag           = findViewById(R.id.ivFlag);
        optionsContainer = findViewById(R.id.optionsContainer);
        tvScore          = findViewById(R.id.tvScore);
        tvLives          = findViewById(R.id.tvLives);
        tvShakePrompt    = findViewById(R.id.tvShakePrompt);
        
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        btns[0] = findViewById(R.id.btnOpt1);
        btns[1] = findViewById(R.id.btnOpt2);
        btns[2] = findViewById(R.id.btnOpt3);
        btns[3] = findViewById(R.id.btnOpt4);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer  = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        vibrator       = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        ensureDatabasePopulated();
    }

    private void ensureDatabasePopulated() {
        new Thread(() -> {
            if (db.flagDao().getCount() == 0) {
                List<FlagQuestion> initialFlags = new ArrayList<>();
                initialFlags.add(createFlag("Angola", R.drawable.flag_angola, "Mozambique", "Portugal", "Brazil"));
                initialFlags.add(createFlag("Hungary", R.drawable.flag_hungary, "Italy", "Bulgaria", "Romania"));
                initialFlags.add(createFlag("Portugal", R.drawable.flag_portugal, "Spain", "France", "Italy"));
                initialFlags.add(createFlag("Cape Verde", R.drawable.flag_caboverde, "Angola", "Senegal", "Guinea"));
                initialFlags.add(createFlag("Mozambique", R.drawable.flag_mozambique, "Zimbabwe", "South Africa", "Kenya"));
                initialFlags.add(createFlag("Sao Tome and Principe", R.drawable.flag_saotome, "Gabon", "Nigeria", "Togo"));
                initialFlags.add(createFlag("Nigeria", R.drawable.flag_nigeria, "Niger", "Ghana", "Cameroon"));
                initialFlags.add(createFlag("Germany", R.drawable.flag_germany, "Belgium", "Austria", "Netherlands"));
                initialFlags.add(createFlag("Russia", R.drawable.flag_russia, "Ukraine", "Kazakhstan", "Poland"));
                initialFlags.add(createFlag("France", R.drawable.flag_france, "Belgium", "Netherlands", "Ireland"));
                initialFlags.add(createFlag("Japan", R.drawable.flag_japan, "South Korea", "Bangladesh", "Singapore"));
                initialFlags.add(createFlag("Italy", R.drawable.flag_italy, "France", "Ireland", "Mexico"));
                initialFlags.add(createFlag("Sweden", R.drawable.flag_sweden, "Norway", "Denmark", "Finland"));
                db.flagDao().insertAll(initialFlags);
            }
        }).start();
    }

    private FlagQuestion createFlag(String name, int resId, String w1, String w2, String w3) {
        FlagQuestion f = new FlagQuestion();
        f.countryName = name;
        f.imageResId = resId;
        f.wrong1 = w1;
        f.wrong2 = w2;
        f.wrong3 = w3;
        return f;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
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
        new Thread(() -> {
            FlagQuestion q = db.flagDao().getRandomQuestion();
            if (q != null) {
                runOnUiThread(() -> {
                    correctCountry = q.countryName;
                    ivFlag.setImageResource(q.imageResId);

                    List<String> options = new ArrayList<>();
                    options.add(q.countryName); // Correct answer
                    options.add(q.wrong1);
                    options.add(q.wrong2);
                    options.add(q.wrong3);
                    Collections.shuffle(options); // Shuffle here to randomize button positions

                    for (int i = 0; i < 4; i++) {
                        btns[i].setText(options.get(i));
                        btns[i].setOnClickListener(v -> checkAnswer(((Button) v).getText().toString()));
                    }
                });
            }
        }).start();
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
                return;
            }
            String lifeWord = lives == 1 ? "life" : "lives";
            Toast.makeText(this, "Wrong! " + lives + " " + lifeWord + " remaining.", Toast.LENGTH_SHORT).show();
            nextQuestion();
        }
        updateUI();
    }

    private void updateUI() {
        tvScore.setText("Score: " + score);

        SpannableStringBuilder livesBuilder = new SpannableStringBuilder("Lives: ");
        int currentLives = Math.max(0, lives);
        for (int i = 0; i < currentLives; i++) {
            Drawable heartIcon = ContextCompat.getDrawable(this, R.drawable.ic_heart);
            if (heartIcon != null) {
                int sizePx = (int) tvLives.getTextSize();
                heartIcon.setBounds(0, 0, sizePx, sizePx);
                livesBuilder.append(" \u00A0");
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
                finish();
            });
        }).start();
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private void vibrate(int durationMs) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
