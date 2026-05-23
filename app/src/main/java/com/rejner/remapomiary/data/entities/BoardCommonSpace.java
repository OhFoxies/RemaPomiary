package com.rejner.remapomiary.data.entities;


import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "board_common_space",
        foreignKeys = @ForeignKey(entity = Flat.class, parentColumns = "id", childColumns = "flatId", onDelete = ForeignKey.CASCADE))
public class BoardCommonSpace {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "flatId")
    public int flatId;
    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "type", defaultValue = "TN-S")
    public String type = "TN-S";

    @ColumnInfo(name = "notes", defaultValue = "")
    public String notes;

    @ColumnInfo(name = "creation_date")
    public Date creation_date;

}
