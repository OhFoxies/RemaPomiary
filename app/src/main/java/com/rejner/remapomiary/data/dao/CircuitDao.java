package com.rejner.remapomiary.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.rejner.remapomiary.data.entities.Circuit;

import java.util.List;

@Dao
public interface CircuitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Circuit circuit);

    @Update
    void update(Circuit circuit);

    @Delete
    void delete(Circuit circuit);

    @Query("SELECT * FROM circuit WHERE flatId = :flatId")
    LiveData<List<Circuit>> getCircuitsForFlat(int flatId);

    @Query("SELECT * FROM circuit WHERE flatId = :flatId")
    List<Circuit> getCircuitsForFlatSync(int flatId);

}