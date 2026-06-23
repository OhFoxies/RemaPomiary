package com.rejner.remapomiary.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.Transaction;
import androidx.room.Update;

import com.rejner.remapomiary.data.entities.RoomFullData;
import com.rejner.remapomiary.data.entities.RoomInFlat;
import com.rejner.remapomiary.ui.utils.Settings;

import java.util.List;
@Dao
public interface RoomDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(RoomInFlat room);

    @Insert
    long insertWithId(RoomInFlat room);

    @Update
    void update(RoomInFlat room);

    @Delete
    void delete(RoomInFlat room);

    @Query("SELECT * FROM room WHERE flatId = :flatId ORDER BY id ASC")
    LiveData<List<RoomInFlat>> getRoomsForFlat(int flatId);

    @Query("SELECT * FROM room WHERE flatId = :flatId ORDER BY id ASC")
    List<RoomInFlat> getRoomsForFlatSync(int flatId);

    @Query("SELECT * FROM room WHERE flatId = :flatId AND name = '" + Settings.mainRoomName  + "'")
    RoomInFlat getMainCommonSpaceRoom(int flatId);
    @Transaction
    @Query("SELECT * FROM room WHERE id = :roomId")
    LiveData<RoomFullData> getRoomFullData(int roomId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert2(RoomInFlat room);

    // Metoda synchroniczna (bez LiveData), potrzebna do sprawdzenia w tle
    @Query("SELECT * FROM room WHERE flatId = :flatId AND name = '" + Settings.mainRoomName  + "' LIMIT 1")
    RoomInFlat getMainRoomSync(int flatId);
}
