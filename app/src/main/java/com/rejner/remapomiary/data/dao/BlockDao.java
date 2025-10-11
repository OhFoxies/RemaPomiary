package com.rejner.remapomiary.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import com.rejner.remapomiary.data.entities.Block;
import com.rejner.remapomiary.data.entities.BlockFullData;

import java.util.Date;
import java.util.List;
@Dao
public interface BlockDao {

    @Transaction
    @Query("SELECT * FROM blocks WHERE catalogId = :catalogId")
    LiveData<List<BlockFullData>> getBlocksWithFullData(int catalogId);

    @Transaction
    @Query("SELECT * FROM blocks WHERE id = :id")
    BlockFullData getBlockById(int id);

    @Query("UPDATE blocks SET street = :street, city = :city, postal_code = :postal_code, number = :number, edition_date = :edition_date, clientId = :clientId")
    void update(String street, String city, String postal_code, String number, Date edition_date, int clientId);
    @Insert
    void insert(Block block);

    @Delete
    void delete(Block block);

}
