package com.rejner.remapomiary.data.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "outletMeasurement",
        foreignKeys = @ForeignKey(
                entity = RoomInFlat.class,
                parentColumns = "id",
                childColumns = "roomId",
                onDelete = ForeignKey.CASCADE
        )
)
public class OutletMeasurement {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "roomId", index = true)
    public int roomId;

    @ColumnInfo(name = "number")
    public int number;

    @ColumnInfo(name = "appliance")
    public String appliance;

    @ColumnInfo(name = "switchName")
    public String switchName;

    // Typ zabezpieczenia (B, C, D, Gg)
    @ColumnInfo(name = "breakerType")
    public String breakerType;

    // Wartość zabezpieczenia w Amperach
    @ColumnInfo(name = "amps", defaultValue = "16.0")
    public Double amps = 16.0;

    // Pomiar w omach
    @ColumnInfo(name = "ohms")
    public Double ohms;

    @ColumnInfo(name = "note")
    public String note;

    //    0 - gone, 1 - good, 2 - broken
    @ColumnInfo(name = "rcd_status", defaultValue = "0")
    public int rcdStatus;

    @ColumnInfo(name = "rcd_time")
    public Integer rcdTime;

    @ColumnInfo(name = "rcd_name")
    public String rcdName;
    @ColumnInfo(name = "rcd_current")
    public Integer rcdCurrent;


    public OutletMeasurement() {}

    public OutletMeasurement(int roomId, String appliance) {
        this.roomId = roomId;
        this.appliance = appliance;
    }
}
