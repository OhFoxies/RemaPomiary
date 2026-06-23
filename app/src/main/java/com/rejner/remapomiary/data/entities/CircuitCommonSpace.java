package com.rejner.remapomiary.data.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "circuit_common_space",
        foreignKeys = @ForeignKey(entity = BoardCommonSpace.class, parentColumns = "id", childColumns = "boardId", onDelete = ForeignKey.CASCADE))
public class CircuitCommonSpace {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "boardId")
    public int boardId;

    @ColumnInfo(name = "name")
    public String name;

    // 1f 3f
    @ColumnInfo(name = "type")
    public String type;

    @ColumnInfo(name = "notes")
    public String notes = "";
}
