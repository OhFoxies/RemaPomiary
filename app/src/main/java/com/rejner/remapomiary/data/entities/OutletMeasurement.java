package com.rejner.remapomiary.data.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "outletMeasurement",
foreignKeys = @ForeignKey(entity = RoomInFlat.class,
parentColumns = "id",
childColumns = "roomId",
onDelete = ForeignKey.CASCADE))
public class OutletMeasurement {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "roomId")
    public int roomId;


//    gniazdko - x, lodowka, kuchenka
    @ColumnInfo(name = "name")
    public String name;

//    B, C, D, Gg
    @ColumnInfo(name = "type")
    public String type;

//    Bezpiecznik producent/model
    @ColumnInfo(name = "breaker")
    public String breaker;

    @ColumnInfo(name = "notes")
    public String notes;
//Ile Amper
    @ColumnInfo(name = "value")
    public int value;
// Pomiar w Omach
    @ColumnInfo(name = "measurement")
    public double measurement;

}
