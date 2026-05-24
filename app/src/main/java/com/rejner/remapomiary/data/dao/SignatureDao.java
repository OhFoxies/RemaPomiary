package com.rejner.remapomiary.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.rejner.remapomiary.data.entities.Signature;

@Dao
public interface SignatureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Signature signature);

    @Query("SELECT * FROM signatures WHERE flatId = :flatId LIMIT 1")
    LiveData<Signature> getSignatureForFlat(int flatId);

    @Query("DELETE FROM signatures WHERE flatId = :flatId")
    void deleteSignatureForFlat(int flatId);

}