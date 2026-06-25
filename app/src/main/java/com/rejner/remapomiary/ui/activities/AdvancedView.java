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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.rejner.remapomiary.R;
import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.ProtocolNumber;
import com.rejner.remapomiary.ui.viewmodels.ProtocolNumViewModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

public class AdvancedView extends AppCompatActivity {

    private ActivityResultLauncher<Intent> importFilePickerLauncher;
    private  Integer lastNumber;
    private  Integer currentNumber;
    private EditText editText;
    private Button button;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_view);

        ProtocolNumViewModel protocolNumViewModel = new ViewModelProvider(this).get(ProtocolNumViewModel.class);
        protocolNumViewModel.getLastNumber().observe(this, last -> {
            if (last != null) {
                lastNumber = last;
                runOnUiThread(this::updateLast);
            }
        });

        protocolNumViewModel.getCurrentNumber().observe(this, current -> {
            if (current != null) {
                currentNumber = current;
                runOnUiThread(this::updateCurrent);

            }
        });

        editText = findViewById(R.id.setNumber);
        button = findViewById(R.id.saveProtocolNum);
        Button btnExport = findViewById(R.id.btnExportDB);
        Button btnImport = findViewById(R.id.btnImportDB);
        Button back = findViewById(R.id.backButton);


        button.setOnClickListener(v -> {
            if (!editText.getText().toString().isEmpty()) {
                protocolNumViewModel.deleteOld();
                protocolNumViewModel.saveLast();

                ProtocolNumber newProtocolNumber = new ProtocolNumber();
                newProtocolNumber.number = Integer.parseInt(editText.getText().toString());
                newProtocolNumber.isCurrent = 1;
                newProtocolNumber.creationDate = new Date();
                protocolNumViewModel.insert(newProtocolNumber);
            }
        });
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

        btnImport.setOnClickListener(v -> openFilePicker());

        Button btnContractors = findViewById(R.id.btnContractors);
        btnContractors.setOnClickListener(v -> {
            Intent intent = new Intent(AdvancedView.this, ContractorsActivity.class);
            startActivity(intent);
        });

        back.setOnClickListener(v-> {finish();});
    }



    private  void  updateCurrent() {
        TextView textView = findViewById(R.id.currentProtocol);
        if (currentNumber != null) {
            textView.setText("" + currentNumber);

        } else {
            textView.setText("Brak");
        }
    }

    private  void  updateLast() {
        TextView textView = findViewById(R.id.lastNumber);
        if (lastNumber != null) {
            textView.setText("" + lastNumber);

        } else {
            textView.setText("Brak");
        }
    }
    private void backupDatabase() {
        File dbFile = getDatabasePath("pomiary_db");

        if (!dbFile.exists()) {
            Toast.makeText(this, "❌ Baza danych nie istnieje!", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "app_backup_" + System.currentTimeMillis() + ".db";

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

            Toast.makeText(this, " Baza danych została zaimportowana.", Toast.LENGTH_LONG).show();
            System.exit(0);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Błąd importu: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
