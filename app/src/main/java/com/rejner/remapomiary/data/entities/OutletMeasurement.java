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

    // Powiązanie z pokojem
    @ColumnInfo(name = "roomId", index = true)
    public int roomId;

    // Numer gniazdka lub 0 dla innych urządzeń
    @ColumnInfo(name = "number")
    public int number;

    // Urządzenie: gniazdko, indukcja, pralka, inne
    @ColumnInfo(name = "appliance")
    public String appliance;

    // Nazwa wyłącznika
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

    // Uwagi (brak bolca, urwane, inne)
    @ColumnInfo(name = "note")
    public String note;

    public OutletMeasurement() {}

    public OutletMeasurement(int roomId, String appliance) {
        this.roomId = roomId;
        this.appliance = appliance;
    }
}
