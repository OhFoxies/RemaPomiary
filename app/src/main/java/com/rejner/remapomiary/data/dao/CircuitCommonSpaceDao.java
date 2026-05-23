package com.rejner.remapomiary.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.rejner.remapomiary.data.entities.CircuitCommonSpace;

import java.util.List;

@Dao
public interface CircuitCommonSpaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CircuitCommonSpace circuit);

    @Update
    void update(CircuitCommonSpace circuit);

    @Delete
    void delete(CircuitCommonSpace circuit);

    // Remove a circuit(s) by board id
    @Query("DELETE FROM circuit_common_space WHERE boardId = :boardId")
    void deleteCircuitsByBoardId(int boardId);

    // Get all circuits in board by board ID
    @Query("SELECT * FROM circuit_common_space WHERE boardId = :boardId")
    LiveData<List<CircuitCommonSpace>> getCircuitsForBoard(int boardId);
}