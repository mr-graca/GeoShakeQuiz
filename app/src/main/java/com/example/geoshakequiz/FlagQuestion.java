package com.example.geoshakequiz;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "flags")
public class FlagQuestion {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String countryName; // This is the correct answer
    public int imageResId;
    public String wrong1;
    public String wrong2;
    public String wrong3;
}
