package com.rejner.remapomiary.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
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

    @Query("SELECT * FROM contractors")
    LiveData<List<Contractors>> getAllContractors();
}