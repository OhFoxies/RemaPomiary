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

    public void delete(Client client, Runnable onFinished) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            dao.delete(client);
            if (onFinished != null) {
                onFinished.run();
            }
        });
    }

    public void insert(Client client, Runnable onFinished) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            dao.insert(client);
            if (onFinished != null) {
                onFinished.run();
            }
        });
    }

    public void getClientsInCatalog(int id, Consumer<List<Client>> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Client> clients = dao.getClientsInCatalog(id);
            callback.accept(clients);
        });
    }

    public void getAllClients(Consumer<List<Client>> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Client> clients = dao.getAllClients();
            callback.accept(clients);
        });
    }
}
