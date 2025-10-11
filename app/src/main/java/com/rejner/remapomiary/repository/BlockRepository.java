package com.rejner.remapomiary.repository;

import android.content.Context;


import androidx.lifecycle.LiveData;

import com.rejner.remapomiary.data.dao.BlockDao;
import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.Block;
import com.rejner.remapomiary.data.entities.BlockFullData;
import com.rejner.remapomiary.data.entities.Catalog;

import java.util.List;
import java.util.function.Consumer;

public class BlockRepository {
    private final BlockDao dao;

    public BlockRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        dao = db.blockDao();
    }

    public void delete(Block block) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.delete(block));
    }

    public void insert(Block block) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.insert(block));
    }

    public LiveData<List<BlockFullData>> getBlocksWithFullData(int catalogId) {
        return dao.getBlocksWithFullData(catalogId);
    }

    public void update(Block block) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.update(block.street, block.city, block.postal_code, block.number, block.edition_date, block.clientId));

    }
    public void getBlockById(int blockId, Consumer<BlockFullData> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                BlockFullData block = dao.getBlockById(blockId);
                callback.accept(block);
            });
        });

    }


}
