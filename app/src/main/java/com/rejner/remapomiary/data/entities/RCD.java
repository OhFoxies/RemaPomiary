package com.rejner.remapomiary.data.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "rcd",
        foreignKeys = @ForeignKey(entity = Flat.class, parentColumns = "id", childColumns = "flatId", onDelete = ForeignKey.CASCADE))
public class RCD {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "flatId")
    public int flatId;

//    producent
    @ColumnInfo(name = "name")
    public String name;

//    gniazdko - 1, lodowka, gniazdko - 2
    @ColumnInfo(name = "measurementLocation")
    public String measurementLocation;

//    A, AC
    @ColumnInfo(name="type")
    public String type;

    @ColumnInfo(name="notes")
    public String notes;

    @ColumnInfo(name = "isGood", defaultValue = "1")
    public int isGood = 1;

    @ColumnInfo(name = "time1")
    public int time1;

    @ColumnInfo(name = "time2")
    public int time2;
}
