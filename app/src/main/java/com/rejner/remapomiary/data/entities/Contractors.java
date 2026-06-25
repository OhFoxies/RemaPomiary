package com.rejner.remapomiary.data.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName = "contractors")
public class Contractors {
    @PrimaryKey(autoGenerate = true)
    public int id;


    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "surname")
    public String surname;


    @ColumnInfo(name = "e_permit")
    public String e_permit;

    @ColumnInfo(name = "d_permit")
    public String d_permit;

// 1- wykonwca 0 - sprawdziciel
    @ColumnInfo(name = "type")
    public int type;

    @ColumnInfo(name = "isActive", defaultValue = "0")
    public boolean isActive = false;

    @ColumnInfo(name = "isDefault", defaultValue = "0")
    public boolean isDefault = false;

    public Contractors(String name, String surname, String e_permit, String d_permit, int type, boolean isActive) {
        this.name = name;
        this.surname = surname;
        this.e_permit = e_permit;
        this.d_permit = d_permit;
        this.type = type;
        this.isActive = isActive;
        this.isDefault = false;
    }

    @Override
    public String toString() {
        return name + " " + surname + " (" + (type == 1 ? "W" : "S") + ")";
    }

    public boolean isChecker() {
        return type == 0;
    }
}
