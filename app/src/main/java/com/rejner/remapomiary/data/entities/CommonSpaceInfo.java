package com.rejner.remapomiary.data.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;


@Entity(tableName = "common_space_info",
        foreignKeys = {
                @ForeignKey(entity = Block.class,
                        parentColumns = "id",
                        childColumns = "blockId",
                        onDelete = ForeignKey.CASCADE)
        })

public class CommonSpaceInfo {

    @PrimaryKey(autoGenerate = true)
    public int id;
    @ColumnInfo(name = "blockId")
    public int blockId;

    @ColumnInfo(name = "switchName")
    public String switchName = "";

    // Typ zabezpieczenia (B, C, D, Gg)
    @ColumnInfo(name = "breakerType")
    public String breakerType = "B";

    // Wartość zabezpieczenia w Amperach
    @ColumnInfo(name = "amps", defaultValue = "16.0")
    public Double amps = 16.0;

    @ColumnInfo(name = "ohms_base")
    public Double ohmsBase = 0.0;

}
