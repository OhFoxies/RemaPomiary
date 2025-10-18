package com.rejner.remapomiary.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.rejner.remapomiary.R;
import com.rejner.remapomiary.data.entities.BlockFullData;
import com.rejner.remapomiary.ui.viewmodels.BlockViewModel;

public class BlockActivity extends AppCompatActivity {


    private BlockFullData block;
    private int blockId;
    private BlockViewModel blockViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block);
        blockId = getIntent().getIntExtra("blockId", 0);
        BlockViewModel blockViewModel = new ViewModelProvider(this).get(BlockViewModel.class);
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
        LinearLayout lps = findViewById(R.id.lps);

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

            }
        });

        lps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

    }
}