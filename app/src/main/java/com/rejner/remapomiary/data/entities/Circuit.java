package com.rejner.remapomiary.data.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
@Entity(tableName = "circuit",
        foreignKeys = @ForeignKey(entity = Flat.class, parentColumns = "id", childColumns = "flatId", onDelete = ForeignKey.CASCADE))
public class Circuit {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "flatId")
    public int flatId;

    @ColumnInfo(name = "name")
    public String name;


//    L1, L2, L3, F3
    @ColumnInfo(name = "type")
    public String type;
}

