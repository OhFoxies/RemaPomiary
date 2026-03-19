package com.rejner.remapomiary.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.rejner.remapomiary.data.entities.OutletMeasurement;
import com.rejner.remapomiary.data.entities.ProtocolNumber;

import java.util.List;
@Dao
public interface ProtocolNumberDao {
    @Insert
    long insert(ProtocolNumber protocolNumber);

    @Update
    void update(ProtocolNumber protocolNumber);

    @Query("UPDATE protocolnum SET number = number + 1 where is_current = 1")
    void incrementNum();

    @Query("DELETE FROM protocolnum WHERE is_current = 0")
    void deleteOld();

    @Query("UPDATE protocolnum SET is_current = 0 WHERE is_current = 1")
    void saveLast();

    @Query("SELECT number FROM protocolnum WHERE is_current = 0")
    LiveData<Integer> getLastNumber();

    @Query("SELECT number FROM protocolnum WHERE is_current = 1")
    int getCurrentNumber();

    @Query("UPDATE protocolnum SET number = :newNumber WHERE is_current = 1")
    void updateCurrent(int newNumber);

    @Query("SELECT number FROM protocolnum WHERE is_current = 1")
    LiveData<Integer> getCurrentNumberLiveData();



    @Query("SELECT * FROM protocolnum")
    List<ProtocolNumber> getAllProtocols();
}
