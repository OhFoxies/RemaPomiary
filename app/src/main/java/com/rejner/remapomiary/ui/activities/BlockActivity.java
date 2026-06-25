package com.rejner.remapomiary.ui.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.rejner.remapomiary.R;
import com.rejner.remapomiary.data.entities.Block;
import com.rejner.remapomiary.data.entities.BlockFullData;
import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.data.utils.LiveDataUtil;
import com.rejner.remapomiary.ui.utils.ProtocolWorker;
import com.rejner.remapomiary.ui.viewmodels.BlockViewModel;
import com.rejner.remapomiary.ui.viewmodels.FlatViewModel;

import java.util.Date;

public class BlockActivity extends AppCompatActivity {


    private BlockFullData block;
    private int blockId;
    private int commonSpaceId;
    private BlockViewModel blockViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block);
        blockId = getIntent().getIntExtra("blockId", 0);
        BlockViewModel blockViewModel = new ViewModelProvider(this).get(BlockViewModel.class);
        FlatViewModel flatViewModel = new ViewModelProvider(this).get(FlatViewModel.class);
        LiveDataUtil.observeOnce(flatViewModel.getCommonSpace(blockId), this, flat -> {
            if (flat == null) {
                Date now = new Date();

                Flat commonSpace = new Flat();
                commonSpace.isCommonSpace = 1;
                commonSpace.blockId = Math.toIntExact(blockId);
                commonSpace.number = "Część wspólna";
                commonSpace.creation_date = now;
                commonSpace.edition_date = now;
                commonSpace.status = "";
                flatViewModel.insertWithId(commonSpace, id ->{
                    commonSpaceId = Math.toIntExact(id);
                });

            } else {
                commonSpaceId = flat.id;

            }

        });
        flatViewModel.getCommonSpace(blockId);
        blockViewModel.getBlockById(blockId, block1 -> {
            block = block1;
            runOnUiThread(this::initializeElements);
        });
    }

    private void initializeElements() {
        TextView catalogTitle = findViewById(R.id.blockAddress);
        catalogTitle.setText("Blok - " + block.block.street + " " + block.block.number);


        TextView city = findViewById(R.id.blockInfoCity);
        TextView street = findViewById(R.id.blockInfoStreet);
        TextView number = findViewById(R.id.blockInfoNumber);
        TextView clientName = findViewById(R.id.blockInfoClientName);
        TextView clientAddress = findViewById(R.id.blockInfoClientAdress);

        city.setText(block.block.city);
        street.setText(block.block.street);
        number.setText(block.block.number);
        clientName.setText(block.getClient().name);
        clientAddress.setText(block.getClient().city + ", "  + block.getClient().street + ", " + block.getClient().postal_code );

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BlockActivity.this, BlocksActivity.class);
                intent.putExtra("catalogId", block.catalog.id);
                startActivity(intent);
            }
        });

        LinearLayout flats = findViewById(R.id.flats);
        LinearLayout commonSpace = findViewById(R.id.commonSpace);
//        LinearLayout lps = findViewById(R.id.lps);

        flats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BlockActivity.this, FlatsActivity.class);
                intent.putExtra("blockId", block.block.id);
                startActivity(intent);
            }
        });

        commonSpace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BlockActivity.this, BoardCommonSpace.class);
                intent.putExtra("flatId", commonSpaceId);
                intent.putExtra("blockId", block.block.id);

                intent.putExtra("commonSpace", 1);

                startActivity(intent);
            }
        });

        Button generateProtocolBtn = findViewById(R.id.generateCommonSpaceProtocol);
        generateProtocolBtn.setOnClickListener(v -> {
            if (commonSpaceId == 0) {
                Toast.makeText(this, "Pobieranie danych części wspólnej, spróbuj ponownie za chwilę", Toast.LENGTH_SHORT).show();
                return;
            }
            onGenerateCommonSpaceProtocol();
        });
//
//        lps.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//            }
//        });

    }

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;

    private void onGenerateCommonSpaceProtocol() {
        new AlertDialog.Builder(this)
                .setTitle("Potwierdzenie")
                .setMessage("Generować protokół dla części wspólnej?")
                .setPositiveButton("Tak", (d, w) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                            && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                REQUEST_NOTIFICATION_PERMISSION);
                        return;
                    }
                    startProtocolWorker(blockId, block.catalog.id, commonSpaceId, 0);
                })
                .setNegativeButton("Nie", null)
                .show();
    }

    private void startProtocolWorker(int b, int c, int f, int p) {
        Data data = new Data.Builder()
                .putInt("blockId", b)
                .putInt("catalogId", c)
                .putInt("flatId", f)
                .putInt("protocolNumber", p)
                .build();
        WorkManager.getInstance(getApplicationContext())
                .enqueue(new OneTimeWorkRequest.Builder(ProtocolWorker.class).setInputData(data).build());
        Toast.makeText(this, "🔄 Rozpoczęto generowanie protokołu części wspólnej.", Toast.LENGTH_SHORT).show();
    }
}
