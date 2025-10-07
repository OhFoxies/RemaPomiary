package com.rejner.remapomiary.data.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "blocks",
        foreignKeys = {@ForeignKey(
                entity = Catalog.class,
                parentColumns = "id",
                childColumns = "catalogId",
                onDelete = ForeignKey.CASCADE),
                @ForeignKey(
                        entity = Client.class,
                        parentColumns = "id",
                        childColumns = "clientId",
                        onDelete = ForeignKey.SET_NULL
                )}
)
public class Block {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "catalogId")
    public int catalogId;

    @ColumnInfo(name = "street")
    public String street;

    @ColumnInfo(name = "city")
    public String city;

    @ColumnInfo(name = "number")
    public String number;

    @ColumnInfo(name = "creation_date")
    public Date creation_date;

    @ColumnInfo(name = "edition_date")
    public Date edition_date;

    @ColumnInfo(name = "postal_code")
    public String postal_code;

    @ColumnInfo(name = "clientId")
    public Integer clientId;

    public Block(int catalogId, String street, String city, String number, String postal_code, Integer clientId, Date creation_date, Date edition_date) {
        this.catalogId = catalogId;
        this.street = street;
        this.city = city;
        this.number = number;
        this.postal_code = postal_code;
        this.clientId = clientId;
        this.creation_date = creation_date;
        this.edition_date = edition_date;
    }
}
