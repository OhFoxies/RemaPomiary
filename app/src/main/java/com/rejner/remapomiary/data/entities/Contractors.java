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

    public Contractors(String name, String surname, String e_permit, String d_permit, int type) {
        this.name = name;
        this.surname = surname;
        this.e_permit = e_permit;
        this.d_permit = d_permit;
        this.type = type;
    }



    public boolean isChecker() {
        return type == 0;
    }
}
