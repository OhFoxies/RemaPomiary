package com.rejner.remapomiary.ui.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.rejner.remapomiary.R;
import com.rejner.remapomiary.data.db.AppDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AdvancedView extends AppCompatActivity {

    private ActivityResultLauncher<Intent> importFilePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_view);

        Button btnExport = findViewById(R.id.btnExportDB);
        Button btnImport = findViewById(R.id.btnImportDB);
        Button back = findViewById(R.id.backButton);

        btnExport.setOnClickListener(v -> backupDatabase());

        importFilePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getData() != null) {
                        Uri selectedFileUri = result.getData().getData();
                        if (selectedFileUri != null) {
                            importDatabase(selectedFileUri);
                        } else {
                            Toast.makeText(this, "❌ Nie wybrano pliku.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // 🔹 Import bazy danych
        btnImport.setOnClickListener(v -> openFilePicker());
        back.setOnClickListener(v-> {finish();});
    }

    // ====================================
    // 📤 EKSPORT BAZY DANYCH
    // ====================================
    private void backupDatabase() {
        File dbFile = getDatabasePath("pomiary_db"); // ← nazwa Twojej bazy danych

        if (!dbFile.exists()) {
            Toast.makeText(this, "❌ Baza danych nie istnieje!", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "app_backup_" + System.currentTimeMillis() + ".db";

        // 🔹 Przygotowanie wpisu w MediaStore (Downloads)
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Downloads.IS_PENDING, 1);
        }

        Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        Uri item = getContentResolver().insert(collection, values);

        if (item == null) {
            Toast.makeText(this, "❌ Nie udało się utworzyć pliku w Pobranych.", Toast.LENGTH_SHORT).show();
            return;
        }

        try (OutputStream out = getContentResolver().openOutputStream(item);
             FileInputStream in = new FileInputStream(dbFile)) {

            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear();
                values.put(MediaStore.Downloads.IS_PENDING, 0);
                getContentResolver().update(item, values, null, null);
            }

            Toast.makeText(this, "✅ Zapisano w Pobranych jako: " + fileName, Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "❌ Błąd: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ====================================
    // 📥 IMPORT BAZY DANYCH
    // ====================================

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"application/octet-stream", "application/x-sqlite3", "application/vnd.sqlite3"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        importFilePickerLauncher.launch(intent);
    }

    private void importDatabase(Uri selectedFileUri) {
        File dbFile = getDatabasePath("pomiary_db");
        AppDatabase db = AppDatabase.getDatabase(this);
        db.close();
        try (InputStream in = getContentResolver().openInputStream(selectedFileUri);
             FileOutputStream out = new FileOutputStream(dbFile, false)) {

            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }

            Toast.makeText(this, "✅ Baza danych została zaimportowana.", Toast.LENGTH_LONG).show();
            System.exit(0);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "❌ Błąd importu: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
