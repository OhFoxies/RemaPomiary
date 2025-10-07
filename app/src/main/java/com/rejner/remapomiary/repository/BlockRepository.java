package com.rejner.remapomiary.repository;

import android.content.Context;


import com.rejner.remapomiary.data.dao.BlockDao;
import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.Block;
import com.rejner.remapomiary.data.entities.BlockFullData;

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

    public void getBlocksWithFullData(int catalogId, Consumer<List<BlockFullData>> callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<BlockFullData> blocks = dao.getBlocksWithFullData(catalogId);
            callback.accept(blocks);
        });
    }


}
