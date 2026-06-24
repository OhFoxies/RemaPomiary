package com.rejner.remapomiary.data.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import java.util.Objects;

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

    @ColumnInfo(name = "photo_path")
    public String photoPath;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OutletMeasurement mystery = (OutletMeasurement) o;

        return id == mystery.id &&
                roomId == mystery.roomId &&
                number == mystery.number &&
                rcdStatus == mystery.rcdStatus &&
                Objects.equals(appliance, mystery.appliance) &&
                Objects.equals(switchName, mystery.switchName) &&
                Objects.equals(breakerType, mystery.breakerType) &&
                Objects.equals(amps, mystery.amps) &&
                Objects.equals(ohms, mystery.ohms) &&
                Objects.equals(note, mystery.note) &&
                Objects.equals(rcdName, mystery.rcdName) &&
                Objects.equals(rcdTime, mystery.rcdTime) &&
                Objects.equals(photoPath, mystery.photoPath) &&
                Objects.equals(rcdCurrent, mystery.rcdCurrent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, roomId, appliance, switchName, breakerType, amps, ohms, note, number, rcdStatus, rcdName, rcdTime, rcdCurrent, photoPath);
    }

    public OutletMeasurement() {}

    public OutletMeasurement(int roomId, String appliance) {
        this.roomId = roomId;
        this.appliance = appliance;
    }

    public OutletMeasurement copy() {
        OutletMeasurement copy = new OutletMeasurement();
        copy.id = this.id;
        copy.roomId = this.roomId;
        copy.number = this.number;
        copy.appliance = this.appliance;
        copy.switchName = this.switchName;
        copy.breakerType = this.breakerType;
        copy.amps = this.amps;
        copy.ohms = this.ohms;
        copy.note = this.note;
        copy.rcdStatus = this.rcdStatus;
        copy.rcdTime = this.rcdTime;
        copy.rcdName = this.rcdName;
        copy.rcdCurrent = this.rcdCurrent;
        copy.photoPath = this.photoPath;
        return copy;
    }
}