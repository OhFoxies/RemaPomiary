package com.rejner.remapomiary.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.rejner.remapomiary.data.converters.DateConverter;
import com.rejner.remapomiary.data.dao.BlockDao;
import com.rejner.remapomiary.data.dao.CatalogDao;
import com.rejner.remapomiary.data.dao.ClientDao;
import com.rejner.remapomiary.data.entities.Block;
import com.rejner.remapomiary.data.entities.Catalog;
import com.rejner.remapomiary.data.entities.Client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Catalog.class, Block.class, Client.class}, version = 2)
@TypeConverters(DateConverter.class)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    public abstract CatalogDao catalogDao();
    public abstract ClientDao clientDao();
    public abstract BlockDao blockDao();

    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "pomiary_db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
