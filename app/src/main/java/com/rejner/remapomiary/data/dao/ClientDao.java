package com.rejner.remapomiary.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.rejner.remapomiary.data.entities.Client;

import java.util.List;

@Dao
public interface ClientDao {
    @Query("SELECT * FROM clients WHERE second_name = :second_name AND first_name = :first_name")
    Client getClientByName(String second_name, String first_name);

    @Query("SELECT * FROM clients")
    List<Client> getAllClients();

    @Insert
    long insert(Client client);

    @Delete
    void delete(Client client);
}
