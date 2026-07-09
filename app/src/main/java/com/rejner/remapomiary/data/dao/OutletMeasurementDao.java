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

    @Query("SELECT * FROM outletMeasurement WHERE roomId = :roomId ORDER BY number")
    LiveData<List<OutletMeasurement>> getMeasurementsForRoom(int roomId);

    @Query("SELECT * FROM outletMeasurement WHERE roomId = :roomId")
    List<OutletMeasurement> getMeasurementsForRoomSync(int roomId);

    @Query("SELECT EXISTS(SELECT 1 FROM outletMeasurement WHERE roomId = :roomId AND appliance = :appliance)")
    boolean existsByApplianceSync(int roomId, String appliance);

    @Query("SELECT * FROM outletMeasurement WHERE roomId = :roomId AND appliance = :appliance")
    OutletMeasurement getOutletMeasurementSync(int roomId, String appliance);

    @Query("SELECT om.rcd_name FROM outletmeasurement om " +
            "INNER JOIN room r ON om.roomId = r.id " +
            "WHERE r.flatId = (SELECT flatId FROM room WHERE id = :currentRoomId) " +
            "AND om.rcd_name IS NOT NULL " +
            "AND trim(om.rcd_name) != '' " +
            "ORDER BY om.id DESC " +
            "LIMIT 1")
    String getLastRcdNameInFlat(int currentRoomId);

    @Query("SELECT EXISTS(SELECT 1 FROM outletMeasurement om " +
            "INNER JOIN room r ON om.roomId = r.id " +
            "WHERE r.flatId = :flatId AND (om.rcd_status = 1 OR om.rcd_status = 2))")
    boolean hasAnyCommonSpaceRcdSync(int flatId);

    @Query("SELECT EXISTS(SELECT 1 FROM outletMeasurement om " +
            "INNER JOIN room r ON om.roomId = r.id " +
            "WHERE r.flatId = :flatId AND (om.rcd_status = 1 OR om.rcd_status = 2))")
    LiveData<Boolean> hasAnyCommonSpaceRcd(int flatId);

    @Query("SELECT om.* FROM outletMeasurement om " +
            "INNER JOIN room r ON om.roomId = r.id " +
            "WHERE r.flatId = :flatId AND om.photo_path IS NOT NULL AND om.photo_path != ''")
    List<OutletMeasurement> getMeasurementsWithPhotosForFlatSync(int flatId);

    @Query("SELECT EXISTS(SELECT 1 FROM outletMeasurement om " +
            "INNER JOIN room r ON om.roomId = r.id " +
            "WHERE r.flatId = :flatId AND om.ohms != 0.0 AND om.ohms IS NOT NULL)")
    boolean hasMeasurementsWithOhmsSync(int flatId);

    @Query("SELECT om.* FROM outletMeasurement om " +
            "INNER JOIN room r ON om.roomId = r.id " +
            "WHERE r.flatId = :flatId AND om.ohms != 0.0 AND om.ohms IS NOT NULL")
    List<OutletMeasurement> getMeasurementsWithOhmsForFlatSync(int flatId);
}
