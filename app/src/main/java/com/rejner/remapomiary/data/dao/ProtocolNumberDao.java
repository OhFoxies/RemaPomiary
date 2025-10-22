package com.rejner.remapomiary.data.dao;

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

    @Query("UPDATE protocolnum SET number = number + 1")
    void incrementNum();

    @Query("SELECT number FROM protocolnum")
    int getCurrentNumber();

    @Query("SELECT * FROM protocolnum")
    List<ProtocolNumber> getAllProtocols();
}
