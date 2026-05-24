package com.rejner.remapomiary.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.rejner.remapomiary.data.entities.BoardCommonSpace;
import com.rejner.remapomiary.data.entities.BoardsFullData;

import java.util.List;

@Dao
public interface BoardCommonSpaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BoardCommonSpace board);

    @Update
    void update(BoardCommonSpace board);

    @Delete
    void delete(BoardCommonSpace board);

    // Get all boards in a flat by flatID
    @Query("SELECT * FROM board_common_space WHERE flatId = :flatId")
    LiveData<List<BoardCommonSpace>> getBoardsForFlat(int flatId);

    @Transaction
    @Query("SELECT * FROM board_common_space WHERE flatId = :flatId ORDER BY creation_date DESC")
    LiveData<List<BoardsFullData>> getBoardsWithCircuitsForFlat(int flatId);
}