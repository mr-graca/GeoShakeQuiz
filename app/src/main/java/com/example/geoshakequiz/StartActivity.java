package com.example.geoshakequiz;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        findViewById(R.id.btnStart).setOnClickListener(v -> {
            Intent intent = new Intent(StartActivity.this, MainActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnHistory).setOnClickListener(v -> {
            Intent intent = new Intent(StartActivity.this, HistoryActivity.class);
            startActivity(intent);
        });
    }
}
