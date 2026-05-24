package com.rejner.remapomiary.data.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "signatures",
        foreignKeys = @ForeignKey(
                entity = Flat.class,
                parentColumns = "id",
                childColumns = "flatId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("flatId")})
public class Signature {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int flatId;

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    public byte[] signatureData;

    public String signerName;
    public Date signatureDate;
}