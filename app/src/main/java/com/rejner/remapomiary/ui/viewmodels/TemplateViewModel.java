package com.rejner.remapomiary.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.rejner.remapomiary.data.entities.BlockFullData;
import com.rejner.remapomiary.data.entities.Template;
import com.rejner.remapomiary.repository.TemplateRepository;

import java.util.List;

public class TemplateViewModel extends AndroidViewModel {
    public final TemplateRepository repository;
    public TemplateViewModel(@NonNull Application application) {
        super(application);
        repository = new TemplateRepository(application);
    }

    public interface TemplateCallBack {
        void onResult(Template template);
    }

    public interface BooleanCallback {
        void onResult(Boolean x);
    }

    public LiveData<List<Template>> getTemplatesInCatalog(int catalogId) {
        return repository.getTemplatesInCatalog(catalogId);
    }
    public void doesTemplateNameExists(String name, int catalogId, BooleanCallback callback) {
        repository.doesTemplateNameExists(name, catalogId, callback::onResult);
    }

    public void update(Template template){
        repository.update(template);
    }

    public void insert(Template template){
        repository.insert(template);
    }

    public void delete(Template template){
        repository.delete(template);
    }
}
