package com.rejner.remapomiary.data.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "templates",
        foreignKeys = @ForeignKey(entity = Flat.class, parentColumns = "id", childColumns = "flatId", onDelete = ForeignKey.CASCADE))
public class Template {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "flatId")
    public int flatId;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "creationdate")
    public Date creationDate;

    @Override
    public String toString() {
        return name;
    }
}
