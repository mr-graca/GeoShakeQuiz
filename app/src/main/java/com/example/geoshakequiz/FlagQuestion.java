package com.example.geoshakequiz;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "flags")
public class FlagQuestion {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String countryName;
    public int imageResId;
    public String option2;
    public String option3;
    public String option4;
}
