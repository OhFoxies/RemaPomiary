package com.rejner.remapomiary.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.rejner.remapomiary.data.dao.TemplateDao;
import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.BlockFullData;
import com.rejner.remapomiary.data.entities.Template;

import java.util.List;
import java.util.function.Consumer;

public class TemplateRepository {
    private final TemplateDao dao;

    public TemplateRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        dao = db.templateDao();
    }

    public void delete(Template template) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            dao.delete(template);
        });
    }

    public void insert(Template template) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            dao.insert(template);
        });
    }

    public void update(Template template) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            dao.update(template);
        });
    }

    public void getTemplateByFlatId(int flatId, Consumer<Template> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                Template template = dao.getTemplateByFlatId(flatId);
                callback.accept(template);
            });
        });
    }
    public void doesTemplateNameExists(String name, int catalogId, Consumer<Boolean> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                Boolean template = dao.doesTemplateNameExists(name, catalogId);
                callback.accept(template);
            });
        });
    }
    public LiveData<List<Template>> getTemplatesInCatalog(int catalogId) {
        return dao.getTemplatesInCatalog(catalogId);
    }
}
