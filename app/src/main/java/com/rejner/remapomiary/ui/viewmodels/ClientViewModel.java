package com.rejner.remapomiary.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

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

    public LiveData<List<Client>> getClientsInCatalog(int id) {
        return repository.getClientsInCatalog(id);
    }

    public void getAllClients(ClientViewModel.clientsCallback callback) {
        repository.getAllClients(callback::onResult);
    }

    public void insert(Client client) {
        repository.insert(client);
    }

    public void delete(Client client) {
        repository.delete(client);
    }
    public void update(String street, String city, String postalCode, String name, int catalogId) {
        repository.update(street, city, postalCode, name, catalogId);
    }
}
