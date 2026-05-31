package com.example.geoshakequiz;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        TextView tvHistory    = findViewById(R.id.tvHistory);
        TextView tvAverage    = findViewById(R.id.tvAverage);
        TextView tvTotalGames = findViewById(R.id.tvTotalGames);

        AppDatabase db = AppDatabase.getInstance(this);

        // Load all data in a background thread (Room disallows main-thread queries)
        new Thread(() -> {
            List<GameResult> results = db.resultDao().getAllResults();
            float average = db.resultDao().getAverageScore();
            int totalGames = results.size();

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < results.size(); i++) {
                GameResult res = results.get(i);
                String date = sdf.format(new Date(res.timestamp));
                sb.append("#").append(i + 1)
                  .append("  |  Score: ").append(res.correctAnswers)
                  .append("  |  ").append(date)
                  .append("\n");
            }

            final String historyText = totalGames > 0 ? sb.toString() : "No games played yet.";
            final String avgText     = totalGames > 0 ? String.format(Locale.getDefault(), "%.1f", average) : "—";
            final String totalText   = "Total games: " + totalGames;

            runOnUiThread(() -> {
                tvHistory.setText(historyText);
                tvAverage.setText(avgText);
                tvTotalGames.setText(totalText);
            });
        }).start();
    }
}
