package com.rejner.remapomiary.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.rejner.remapomiary.data.entities.Catalog;
import com.rejner.remapomiary.data.entities.Client;
import com.rejner.remapomiary.repository.ClientRepository;

import java.util.Date;
import java.util.List;

public class ClientViewModel extends AndroidViewModel {
    public final ClientRepository repository;

    public ClientViewModel(@NonNull Application application) {
        super(application);
        this.repository = new ClientRepository(application);
    }

    public interface clientsCallback {
        void onResult(List<Client> clients);
    }

    public void getClientsInCatalog(int id, ClientViewModel.clientsCallback callback) {
        repository.getClientsInCatalog(id, callback::onResult);
    }

    public void getAllClients(ClientViewModel.clientsCallback callback) {
        repository.getAllClients(callback::onResult);
    }

    public void insert(Client client, Runnable runnable) {
        repository.insert(client, runnable);
    }

    public void delete(Client client, Runnable runnable) {
        repository.delete(client, runnable);
    }
}
