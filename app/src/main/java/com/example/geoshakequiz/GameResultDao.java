package com.example.geoshakequiz;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface GameResultDao {

    @Insert
    void insert(GameResult result);

    /** Returns all past game results, newest first. */
    @Query("SELECT * FROM game_results ORDER BY timestamp DESC")
    List<GameResult> getAllResults();

    /** Returns the average number of correct answers across all sessions. */
    @Query("SELECT AVG(correctAnswers) FROM game_results")
    float getAverageScore();
}
