package com.rejner.remapomiary.data.entities;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "catalogs")
public class Catalog {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "city")
    public String city;

    @ColumnInfo(name = "street")
    public String street;

    @ColumnInfo(name = "postal_code")
    public String postal_code;


    @ColumnInfo(name = "creation_date")
    public Date creation_date;


    @ColumnInfo(name = "edition_time")
    public Date edition_time;

    public Catalog(String title, String city, String street, String postal_code, Date creation_date, Date edition_time) {
        this.city = city;
        this.title = title;
        this.street = street;
        this.postal_code = postal_code;
        this.creation_date = creation_date;
        this.edition_time = edition_time;
    }
}


