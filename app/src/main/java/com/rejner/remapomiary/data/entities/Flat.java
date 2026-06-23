package com.rejner.remapomiary.data.entities;


import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import com.rejner.remapomiary.ui.utils.Settings;

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
    public int hasRCD = 1;

//    0 dopuszczone, 1 - dopuszczone po usunieciu, 2 - niedopuszczone
    @ColumnInfo(name = "grade", defaultValue = "0")
    public int grade = 0;

    @ColumnInfo(name = "notes", defaultValue = "")
    public String notes ="";

    @ColumnInfo(name = "notesProtocol", defaultValue = "")
    public String notesProtocol = "";

    @ColumnInfo(name = "circuitNotes", defaultValue = "")
    public String circuitNotes = "";

    @ColumnInfo(name = "gradeByUser", defaultValue = "0")
    public int gradeByUser = 0;

    @ColumnInfo(name = "type", defaultValue = Settings.installationTypeTNS)
    public String type = Settings.installationTypeTNS;


    @ColumnInfo(name = "creation_date")
    public Date creation_date;

    @ColumnInfo(name = "edition_date")
    public Date edition_date;

    @ColumnInfo(name = "status")
    public String status;

    @ColumnInfo(name = "istemplate", defaultValue = "0")
    public int isTemplate = 0;

    @ColumnInfo(name = "isCommonSpace", defaultValue = "0")
    public int isCommonSpace = 0;

    public boolean getIsCommonSpace() {
        return isCommonSpace == 1;
    }

    public void setIsCommonSpace(int isCommonSpace) {
        this.isCommonSpace = isCommonSpace;
    }
}
