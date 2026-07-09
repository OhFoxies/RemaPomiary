package com.rejner.remapomiary.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.rejner.remapomiary.data.entities.CircuitCommonSpace;
import com.rejner.remapomiary.ui.utils.Settings;

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

    @Query("SELECT * FROM circuit_common_space WHERE boardId = :boardId AND type = '" + Settings.installation1f + "'")
    List<CircuitCommonSpace> getCircuitsForBoardSync1f(int boardId);

    @Query("SELECT * FROM circuit_common_space WHERE boardId = :boardId AND type = '" + Settings.installation3f + "'")
    List<CircuitCommonSpace> getCircuitsForBoardSync3f(int boardId);

    @Query("SELECT * FROM circuit_common_space WHERE boardId = :boardId")
    List<CircuitCommonSpace> getCircuitsForBoardSync(int boardId);

    @Query("SELECT * FROM circuit_common_space WHERE boardId = :boardId AND name = :name LIMIT 1")
    CircuitCommonSpace getCircuitByNameSync(int boardId, String name);

    @Query("SELECT EXISTS(SELECT 1 FROM circuit_common_space WHERE boardId = :boardId AND notes = 'Brak dostępu')")
    boolean areThereNotDoneFlatsBoard(int boardId);
}