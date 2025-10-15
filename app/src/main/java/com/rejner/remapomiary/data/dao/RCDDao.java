package com.rejner.remapomiary.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.rejner.remapomiary.data.entities.RCD;

import java.util.List;

@Dao
public interface RCDDao {

    @Insert
    void insert(RCD rcd);

    @Update
    void update(RCD rcd);

    @Delete
    void delete(RCD rcd);

    @Query("SELECT * FROM rcd WHERE flatId = :flatId ORDER BY name ASC")
    LiveData<List<RCD>> getRcdsForFlat(int flatId);
}
