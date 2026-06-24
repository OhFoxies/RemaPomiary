package com.rejner.remapomiary.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.rejner.remapomiary.data.entities.FlatPhoto;

import java.util.List;

@Dao
public interface FlatPhotoDao {
    @Insert
    long insert(FlatPhoto photo);

    @Delete
    void delete(FlatPhoto photo);
    @Update
    void update(FlatPhoto photo);
    @Query("SELECT * FROM flat_photos WHERE flat_id = :flatId AND type = :type ORDER BY id ASC")
    LiveData<List<FlatPhoto>> getPhotosByFlatAndType(int flatId, int type);

    @Query("SELECT * FROM flat_photos WHERE flat_id = :flatId AND type = :type ORDER BY id ASC")
    List<FlatPhoto> getPhotosByFlatAndTypeSync(int flatId, int type);
}