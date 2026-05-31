package com.example.geoshakequiz;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "game_results")
public class GameResult {

    @PrimaryKey(autoGenerate = true)
    public int id;

    /** Number of countries the player guessed correctly in this session. */
    public int correctAnswers;

    /** Unix timestamp (ms) when the game session ended. */
    public long timestamp;
}
