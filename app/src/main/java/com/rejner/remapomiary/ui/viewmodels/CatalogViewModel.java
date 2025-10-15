package com.rejner.remapomiary.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.rejner.remapomiary.data.entities.Catalog;
import com.rejner.remapomiary.repository.CatalogRepository;

import java.util.Date;
import java.util.List;

public class CatalogViewModel extends AndroidViewModel {

    public final CatalogRepository repository;

    public CatalogViewModel(@NonNull Application application) {
        super(application);
        repository = new CatalogRepository(application);
    }

    public interface CatalogCallback {
        void onResult(Catalog catalog);
    }

    public interface CatalogsCallback {
        void onResult(List<Catalog> catalog);
    }

    public void getAllCatalogsByEdition(CatalogsCallback callback) {
        repository.getAllCatalogsByEdition(callback::onResult);
    }

    public void getAllCatalogsByCreation(CatalogsCallback callback) {
        repository.getAllCatalogsByCreation(callback::onResult);
    }
    public void update(String city, String street, String postal_code, String title, Date time, int id, Runnable runnable) {
        repository.update(city, street, postal_code, title, time, id, runnable);
    }

    public void getCatalogByName(String title, CatalogCallback callback) {
        repository.getCatalogByName(title, callback::onResult);
    }

    public void getCatalogById(int id, CatalogCallback callback) {
        repository.getCatalogById(id, callback::onResult);
    }

    public void insert(Catalog catalog, Runnable runnable) {
        repository.insert(catalog, runnable);
    }

    public void delete(Catalog catalog, Runnable runnable) {
        repository.delete(catalog, runnable);
    }

    public void updateEdition(int catalogId) {
        repository.updateEdition(catalogId);
    }
}
