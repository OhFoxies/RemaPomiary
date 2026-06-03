package com.rejner.remapomiary.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.rejner.remapomiary.data.entities.CommonSpaceInfo;

import java.util.List;

@Dao
public interface CommonSpaceInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CommonSpaceInfo info);

    @Update
    void update(CommonSpaceInfo info);

    @Delete
    void delete(CommonSpaceInfo info);

    @Query("SELECT * FROM common_space_info")
    LiveData<List<CommonSpaceInfo>> getAll();

    @Query("SELECT * FROM common_space_info")
    List<CommonSpaceInfo> getAllSync();

    // Selects by blockId, just in case you need to retrieve the specific data
    @Query("SELECT * FROM common_space_info WHERE blockId = :blockId")
    LiveData<List<CommonSpaceInfo>> getInfoByBlockId(int blockId);

    @Query("SELECT * FROM common_space_info WHERE blockId = :blockId LIMIT 1")
    CommonSpaceInfo getInfoByBlockIdSync(int blockId);

    // Checks if data exists for the provided blockId.
    // Returns LiveData so you can easily observe it in the UI without worrying about background threads.
    @Query("SELECT EXISTS(SELECT 1 FROM common_space_info WHERE blockId = :blockId)")
    LiveData<Boolean> checkIfExistsLive(int blockId);

    // Synchronous version for background checks (e.g., inside an executor)
    @Query("SELECT EXISTS(SELECT 1 FROM common_space_info WHERE blockId = :blockId)")
    boolean checkIfExistsSync(int blockId);

    @Query("SELECT EXISTS(" +
            "SELECT 1 FROM common_space_info " +
            "WHERE blockId = :blockId " +
            "AND switchName IS NOT NULL AND switchName != '' " +
            "AND breakerType IS NOT NULL AND breakerType != '' " +
            "AND amps IS NOT NULL " +
            "AND ohms_base IS NOT NULL AND ohms_base != 0.0" +
            ")")
    boolean areAllFieldsFilledSync(int blockId);

}
