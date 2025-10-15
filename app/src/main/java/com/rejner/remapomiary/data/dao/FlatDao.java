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

    @Update
    void update(Flat flat);

    @Delete
    void delete(Flat flat);

    @Query("SELECT * FROM flat WHERE id = :flatId")
    LiveData<Flat> getFlatById(int flatId);

    @Query("SELECT * FROM flat ORDER BY creation_date ASC")
    LiveData<List<Flat>> getAllFlats();
    @Query("SELECT * FROM flat WHERE blockId = :blockId ORDER BY creation_date ASC")
    LiveData<List<Flat>> getFlatsByBlockId(int blockId);

    @Transaction
    @Query("SELECT * FROM flat WHERE id = :flatId")
    LiveData<FlatFullData> getFlatFullData(int flatId);

    @Transaction
    @Query("SELECT * FROM flat ORDER BY creation_date ASC")
    LiveData<List<FlatFullData>> getAllFlatsFullData();
    @Query("SELECT * FROM flat WHERE blockId = :blockId ORDER BY creation_date ASC")
    LiveData<List<FlatFullData>> getFlatsFullDataByBlockId(int blockId);
    @Transaction
    @Query("SELECT * FROM flat WHERE id = :flatId")
    FlatFullData getFlatFullDataSync(int flatId);
}
