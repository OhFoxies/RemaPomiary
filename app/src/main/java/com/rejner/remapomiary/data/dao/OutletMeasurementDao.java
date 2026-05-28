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

    @Query("SELECT * FROM outletMeasurement WHERE roomId = :roomId")
    List<OutletMeasurement> getMeasurementsForRoomSync(int roomId);

    @Query("SELECT om.rcd_name FROM outletmeasurement om " +
            "INNER JOIN room r ON om.roomId = r.id " +
            "WHERE r.flatId = (SELECT flatId FROM room WHERE id = :currentRoomId) " +
            "AND om.rcd_name IS NOT NULL " +
            "AND trim(om.rcd_name) != '' " +
            "ORDER BY om.id DESC " +
            "LIMIT 1")
    String getLastRcdNameInFlat(int currentRoomId);

}
