package com.rejner.remapomiary.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.rejner.remapomiary.data.entities.Client;

import java.util.List;

@Dao
public interface ClientDao {
    @Query("SELECT * FROM clients WHERE catalogId = :catalogId")
    List<Client> getClientsInCatalog(int catalogId);

    @Query("SELECT * FROM clients")
    List<Client> getAllClients();

    @Insert
    long insert(Client client);

    @Delete
    void delete(Client client);
}
