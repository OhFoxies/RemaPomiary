package com.rejner.remapomiary.data.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "flat_photos",
        foreignKeys = @ForeignKey(
                entity = Flat.class,
                parentColumns = "id",
                childColumns = "flat_id",
                onDelete = ForeignKey.CASCADE
        )
)
public class FlatPhoto {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "flat_id", index = true)
    public int flatId;

    @ColumnInfo(name = "photo_path")
    public String photoPath;

    @ColumnInfo(name = "description")
    public String description;

    @ColumnInfo(name = "type")
    public int type; // 0 - rozdzielnia, 1 - uwagi
}