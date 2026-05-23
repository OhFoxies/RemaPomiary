package com.rejner.remapomiary.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.rejner.remapomiary.data.entities.CircuitCommonSpace;
import com.rejner.remapomiary.repository.CircuitCommonSpaceRepository;

import java.util.List;

public class CircuitCommonSpaceViewModel extends AndroidViewModel {

    private final CircuitCommonSpaceRepository repository;

    public CircuitCommonSpaceViewModel(@NonNull Application application) {
        super(application);
        repository = new CircuitCommonSpaceRepository(application);
    }

    public LiveData<List<CircuitCommonSpace>> getCircuitsForBoard(int boardId) {
        return repository.getCircuitsForBoard(boardId);
    }

    public void insert(CircuitCommonSpace circuit) {
        repository.insert(circuit);
    }

    public void update(CircuitCommonSpace circuit) {
        repository.update(circuit);
    }

    public void delete(CircuitCommonSpace circuit) {
        repository.delete(circuit);
    }

    public void deleteCircuitsByBoardId(int boardId) {
        repository.deleteCircuitsByBoardId(boardId);
    }
}