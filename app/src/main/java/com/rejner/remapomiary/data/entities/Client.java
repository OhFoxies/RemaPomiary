package com.rejner.remapomiary.data.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "clients",
foreignKeys = {
        @ForeignKey(
                entity = Catalog.class,
                parentColumns = "id",
                childColumns = "catalogId",
                onDelete = ForeignKey.CASCADE
        )
})
public class Client {
    @PrimaryKey(autoGenerate = true)
    public int id;


    @ColumnInfo(name = "catalogId")
    public int catalogId;

    @ColumnInfo(name = "street")
    public String street;

    @ColumnInfo(name = "city")
    public String city;


    @ColumnInfo(name = "postal_code")
    public String postal_code;

    @ColumnInfo(name = "name")
    public String name;


    public Client(String street, String city, String postal_code, String name) {
        this.street = street;
        this.city = city;
        this.postal_code = postal_code;
        this.name = name;
    }
}
