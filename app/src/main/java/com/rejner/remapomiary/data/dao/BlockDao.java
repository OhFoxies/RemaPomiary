package com.rejner.remapomiary.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import com.rejner.remapomiary.data.entities.Block;
import com.rejner.remapomiary.data.entities.BlockFullData;

import java.util.List;
@Dao
public interface BlockDao {

    @Transaction
    @Query("SELECT * FROM blocks WHERE catalogId = :catalogId")
    List<BlockFullData> getBlocksWithFullData(int catalogId);

    @Insert
    void insert(Block block);

    @Delete
    void delete(Block block);

}
