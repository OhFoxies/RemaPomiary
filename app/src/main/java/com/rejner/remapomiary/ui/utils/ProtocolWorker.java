package com.rejner.remapomiary.ui.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.rejner.remapomiary.R;
import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.BlockFullData;
import com.rejner.remapomiary.generator.ProtocolGenerator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProtocolWorker extends Worker {

    public ProtocolWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        int blockId = getInputData().getInt("blockId", -1);
        int catalogId = getInputData().getInt("catalogId", -1);

        if (blockId == -1 || catalogId == -1) {
            showNotification("❌ Błąd", "Brak wymaganych danych do wygenerowania protokołu.");
            return Result.failure();
        }

        AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
        BlockFullData block = db.blockDao().getBlockById(blockId);
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

        ProtocolGenerator generator = new ProtocolGenerator(getApplicationContext());
        String nazwaPliku = "protokol_" + block.block.number + "_" + sdf.format(new Date()) + ".pdf";
        Uri plikPdfUri = generator.generate(nazwaPliku, blockId);

        if (plikPdfUri != null) {
            showNotification("✅ Sukces", "Protokół został wygenerowany: " + nazwaPliku);
            return Result.success();
        } else {
            showNotification("❌ Błąd", "Nie udało się wygenerować protokołu.");
            return Result.failure();
        }
    }

    private void showNotification(String title, String message) {
        String channelId = "protocol_channel";
        NotificationManager manager = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Generowanie protokołów",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // ← dodaj dowolną ikonę do res/drawable/
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
