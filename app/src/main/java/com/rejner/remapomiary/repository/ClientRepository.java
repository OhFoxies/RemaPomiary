package com.rejner.remapomiary.repository;

import android.content.Context;

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
        AppDatabase.databaseWriteExecutor.execute(() -> dao.delete(client));
    }

    public void insert(Client client, Consumer<Long> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long insertId = dao.insert(client);
            callback.accept(insertId);
        });
    }

    public void getClientByName(String second_name, String first_name, Consumer<Client> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Client client = dao.getClientByName(second_name, first_name);
            callback.accept(client);
        });
    }

    public void getAllClients(Consumer<List<Client>> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Client> clients = dao.getAllClients();
            callback.accept(clients);
        });
    }
}
