package com.example.geoshakequiz;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface FlagDao {

    @Insert
    void insertAll(List<FlagQuestion> flags);

    @Query("SELECT * FROM flags ORDER BY RANDOM() LIMIT 1")
    FlagQuestion getRandomQuestion();

    @Query("SELECT COUNT(*) FROM flags")
    int getCount();
}
