package com.rejner.remapomiary.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.rejner.remapomiary.data.dao.BoardCommonSpaceDao;
import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.BoardCommonSpace;
import com.rejner.remapomiary.data.entities.BoardsFullData;

import java.util.List;

public class BoardCommonSpaceRepository {

    private final BoardCommonSpaceDao dao;

    public BoardCommonSpaceRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        dao = db.boardCommonSpaceDao(); // Make sure to add this method to your AppDatabase class
    }

    public void insert(BoardCommonSpace board) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.insert(board));
    }

    public void update(BoardCommonSpace board) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.update(board));
    }

    public void delete(BoardCommonSpace board) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.delete(board));
    }

    public LiveData<List<BoardCommonSpace>> getBoardsForFlat(int flatId) {
        return dao.getBoardsForFlat(flatId);
    }

    public LiveData<List<BoardsFullData>> getBoardsFullData(int flatId) {
        return dao.getBoardsWithCircuitsForFlat(flatId);
    }
}