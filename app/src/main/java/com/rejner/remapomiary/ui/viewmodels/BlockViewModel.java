package com.rejner.remapomiary.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.rejner.remapomiary.data.entities.Block;
import com.rejner.remapomiary.data.entities.BlockFullData;
import com.rejner.remapomiary.data.entities.Catalog;
import com.rejner.remapomiary.repository.BlockRepository;

import java.util.List;

public class BlockViewModel extends AndroidViewModel
{
    public final BlockRepository repository;

    public BlockViewModel(@NonNull Application application) {
        super(application);
        repository = new BlockRepository(application);
    }

    public interface BlocksCallback {
        void onResult(List<BlockFullData> blocks);
    }

    public interface BlockCallback {
        void onResult(BlockFullData block);
    }

    public void getBlockById(int blockId, BlockCallback callback) {
        repository.getBlockById(blockId, callback::onResult);
    }

    public LiveData<List<BlockFullData>> getBlocksWithFullData(int catalogId) {
        return repository.getBlocksWithFullData(catalogId);
    }

    public void insert(Block block) {
        repository.insert(block);
    }
    public void update(Block block) {
        repository.update(block);
    }

    public void delete(Block block) {
        repository.delete(block);
    }

}
