package com.rejner.remapomiary.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.data.entities.FlatFullData;


import java.util.List;

@Dao
public interface FlatDao {

    @Insert
    void insert(Flat flat);

    @Insert
    long insertWithId(Flat flat);

    @Update
    void update(Flat flat);

    @Delete
    void delete(Flat flat);

    @Query("SELECT * FROM flat WHERE id = :flatId")
    LiveData<Flat> getFlatById(int flatId);

    @Query("SELECT * FROM flat WHERE istemplate = 0 ORDER BY creation_date ASC")
    LiveData<List<Flat>> getAllFlats();
    @Query("SELECT * FROM flat WHERE blockId = :blockId AND istemplate = 0 ORDER BY creation_date ASC")
    LiveData<List<Flat>> getFlatsByBlockId(int blockId);

    @Transaction
    @Query("SELECT * FROM flat WHERE id = :flatId")
    LiveData<FlatFullData> getFlatFullData(int flatId);

    @Transaction
    @Query("SELECT * FROM flat WHERE istemplate = 0 ORDER BY creation_date ASC")
    LiveData<List<FlatFullData>> getAllFlatsFullData();
    @Query("SELECT * FROM flat WHERE blockId = :blockId AND istemplate = 0 ORDER BY creation_date ASC")
    LiveData<List<FlatFullData>> getFlatsFullDataByBlockId(int blockId);

    @Transaction
    @Query("SELECT * FROM flat " +
            "INNER JOIN blocks ON flat.blockId = blocks.id " +
            "INNER JOIN catalogs ON blocks.catalogId = catalogs.id " +
            "WHERE flat.istemplate = 1 AND catalogs.id = :catalogId")
    LiveData<List<FlatFullData>> getTemplatesForCatalog(int catalogId);
    @Transaction
    @Query("SELECT * FROM flat WHERE id = :flatId")
    FlatFullData getFlatFullDataSync(int flatId);

    @Query("SELECT EXISTS (" +
            "SELECT 1 FROM outletMeasurement " +
            "INNER JOIN room ON outletMeasurement.roomId = room.id " +
            "WHERE room.flatId = :flatId " +
            "AND outletMeasurement.note IS NOT NULL " +
            "AND outletMeasurement.note != 'brak uwag' " +
            "UNION ALL " +
            "SELECT 1 FROM rcd " +
            "WHERE rcd.flatId = :flatId " +
            "AND rcd.notes IS NOT NULL " +
            "AND rcd.notes != '' " +
            "UNION ALL " +
            "SELECT 1 FROM flat " +
            "WHERE flat.id = :flatId " +
            "AND flat.circuitNotes IS NOT NULL " +
            "AND flat.circuitNotes != ''" +
            ")")
    LiveData<Boolean> shouldSetGradeToOne(int flatId);
    @Query("SELECT EXISTS (" +
            "SELECT 1 FROM outletMeasurement " +
            "INNER JOIN room ON outletMeasurement.roomId = room.id " +
            "WHERE room.flatId = :flatId " +
            "AND outletMeasurement.note IS NOT NULL " +
            "AND outletMeasurement.note != 'brak uwag' " +
            "UNION ALL " +
            "SELECT 1 FROM rcd " +
            "WHERE rcd.flatId = :flatId " +
            "AND rcd.notes IS NOT NULL " +
            "AND rcd.notes != '' " +
            "UNION ALL " +
            "SELECT 1 FROM flat " +
            "WHERE flat.id = :flatId " +
            "AND flat.circuitNotes IS NOT NULL " +
            "AND flat.circuitNotes != ''" +
            ")")
    boolean shouldSetGradeToOneSync(int flatId);
}
