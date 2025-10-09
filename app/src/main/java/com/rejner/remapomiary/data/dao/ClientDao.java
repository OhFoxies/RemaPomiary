package com.rejner.remapomiary.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.rejner.remapomiary.data.entities.Client;

import java.util.List;

@Dao
public interface ClientDao {
    @Query("SELECT * FROM clients WHERE catalogId = :catalogId")
    LiveData<List<Client>> getClientsInCatalog(int catalogId);

    @Query("SELECT * FROM clients")
    List<Client> getAllClients();

    @Insert
    long insert(Client client);

    @Delete
    void delete(Client client);

    @Query("UPDATE clients SET street = :street, city = :city, postal_code = :postalCode, name=:name WHERE catalogId = :catalogId")
    void update(String street, String city, String postalCode, String name, int catalogId);
}
