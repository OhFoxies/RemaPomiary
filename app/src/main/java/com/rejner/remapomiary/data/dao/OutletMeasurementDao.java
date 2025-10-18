package com.rejner.remapomiary.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;


import com.rejner.remapomiary.data.entities.OutletMeasurement;

import java.util.List;

@Dao
public interface OutletMeasurementDao {

    @Insert
    long insert(OutletMeasurement measurement);

    @Update
    void update(OutletMeasurement measurement);

    @Delete
    void delete(OutletMeasurement measurement);

    @Query("SELECT * FROM outletMeasurement WHERE roomId = :roomId")
    LiveData<List<OutletMeasurement>> getMeasurementsForRoom(int roomId);

}
