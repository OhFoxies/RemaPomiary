package com.rejner.remapomiary.data.entities;


import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "flat",
        foreignKeys = {
                @ForeignKey(entity = Block.class,
                        parentColumns = "id",
                        childColumns = "blockId",
                        onDelete = ForeignKey.CASCADE)
        })

public class Flat {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "blockId")
    public int blockId;

    @ColumnInfo(name = "number")
    public String number;

    @ColumnInfo(name = "hasRCD", defaultValue = "1")
    public int hasRCD;

//    0 dopuszczone, 1 - dopuszczone po usunieciu, 2 - niedopuszczone
    @ColumnInfo(name = "grade", defaultValue = "0")
    public int grade;

    @ColumnInfo(name = "notes")
    public String notes;

    @ColumnInfo(name = "creation_date")
    public Date creation_date;

    @ColumnInfo(name = "edition_date")
    public Date edition_date;

    @ColumnInfo(name = "status")
    public String status;

}
