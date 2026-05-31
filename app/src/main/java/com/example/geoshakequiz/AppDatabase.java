package com.example.geoshakequiz;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {GameResult.class, FlagQuestion.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {

    public abstract GameResultDao resultDao();
    public abstract FlagDao flagDao();

    private static AppDatabase instance;

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "geoshake_db")
                    .fallbackToDestructiveMigration(true)
                    .build();
        }
        return instance;
    }
}
