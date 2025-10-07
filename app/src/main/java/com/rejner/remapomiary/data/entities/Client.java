package com.rejner.remapomiary.data.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "clients")
public class Client {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "first_name")
    public String first_name;

    @ColumnInfo(name = "second_name")
    public String second_name;

    public Client(String first_name, String second_name) {
        this.first_name = first_name;
        this.second_name = second_name;
    }
}
