package com.rejner.remapomiary.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.rejner.remapomiary.data.entities.Contractors;

import java.util.List;

@Dao
public interface ContractorsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Contractors contractor);

    @Update
    void update(Contractors contractor);

    @Delete
    void delete(Contractors contractor);

    @Query("SELECT * FROM contractors")
    LiveData<List<Contractors>> getAllContractors();

    @Query("SELECT * FROM contractors WHERE type = 1 AND isActive = 1 LIMIT 1")
    Contractors getActiveContractorSync();

    @Query("SELECT * FROM contractors WHERE type = 0 AND isActive = 1 LIMIT 1")
    Contractors getActiveCheckerSync();

    @Query("SELECT * FROM contractors WHERE type = 1 AND isDefault = 1 LIMIT 1")
    Contractors getDefaultContractorSync();

    @Query("SELECT * FROM contractors WHERE type = 0 AND isDefault = 1 LIMIT 1")
    Contractors getDefaultCheckerSync();

    @Query("UPDATE contractors SET isActive = 0 WHERE type = :type")
    void deactivateAllByType(int type);

    @Query("UPDATE contractors SET isDefault = 0 WHERE type = :type")
    void deactivateAllDefaultsByType(int type);

    @Query("SELECT * FROM contractors WHERE type = :type")
    LiveData<List<Contractors>> getContractorsByType(int type);

    @Query("SELECT * FROM contractors WHERE id = :id LIMIT 1")
    Contractors getContractorByIdSync(int id);
}
