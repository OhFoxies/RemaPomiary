package com.rejner.remapomiary.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.rejner.remapomiary.data.entities.BoardCommonSpace;
import com.rejner.remapomiary.repository.BoardCommonSpaceRepository;

import java.util.List;

public class BoardCommonSpaceViewModel extends AndroidViewModel {

    private final BoardCommonSpaceRepository repository;

    public BoardCommonSpaceViewModel(@NonNull Application application) {
        super(application);
        repository = new BoardCommonSpaceRepository(application);
    }

    public LiveData<List<BoardCommonSpace>> getBoardsForFlat(int flatId) {
        return repository.getBoardsForFlat(flatId);
    }

    public void insert(BoardCommonSpace board) {
        repository.insert(board);
    }

    public void update(BoardCommonSpace board) {
        repository.update(board);
    }

    public void delete(BoardCommonSpace board) {
        repository.delete(board);
    }
}