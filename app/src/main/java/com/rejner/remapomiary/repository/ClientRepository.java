package com.rejner.remapomiary.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.rejner.remapomiary.data.dao.ClientDao;
import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.Client;

import java.util.List;
import java.util.function.Consumer;

public class ClientRepository {
    private final ClientDao dao;

    public ClientRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        dao = db.clientDao();
    }

    public void delete(Client client) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            dao.delete(client);
        });
    }

    public void insert(Client client) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            dao.insert(client);
        });
    }

    public LiveData<List<Client>> getClientsInCatalog(int id) {
        return dao.getClientsInCatalog(id);
    }

    public void update(String street, String city, String postalCode, String name, int catalogId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            dao.update(street, city, postalCode, name, catalogId);
        });
    }
    public void getAllClients(Consumer<List<Client>> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Client> clients = dao.getAllClients();
            callback.accept(clients);
        });
    }
}
