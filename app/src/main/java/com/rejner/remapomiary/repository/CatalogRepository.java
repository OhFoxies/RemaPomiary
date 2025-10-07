package com.rejner.remapomiary.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.rejner.remapomiary.data.dao.CatalogDao;
import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.Catalog;

import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

public class CatalogRepository {
    private final CatalogDao dao;

    public CatalogRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        dao = db.catalogDao();
    }

    public void delete(Catalog catalog) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.delete(catalog));
    }

    public void insert(Catalog catalog) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.insert(catalog));
    }


    public void update(String city, String street, String postal_code, String title, Date time, int id, Runnable onFinished) {
        AppDatabase.databaseWriteExecutor.execute(() ->
        {dao.update(city, street, postal_code, title, time, id);
            if (onFinished != null) {
                onFinished.run();
            }}
        );
    }


    public void getCatalogByName(String title, Consumer<Catalog> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Catalog catalog = dao.getCatalogByName(title);
            callback.accept(catalog);
        });
    }

    public void getAllCatalogsByCreation(Consumer<List<Catalog>> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Catalog> catalogs = dao.getAllCatalogsByCreation();
            callback.accept(catalogs);
        });
    }

    public void getAllCatalogsByEdition(Consumer<List<Catalog>> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Catalog> catalogs = dao.getAllCatalogsByEdition();
            callback.accept(catalogs);
        });
    }


}
