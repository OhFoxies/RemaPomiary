package com.rejner.remapomiary.ui.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.rejner.remapomiary.R;
import com.rejner.remapomiary.data.entities.Catalog;
import com.rejner.remapomiary.ui.viewmodels.CatalogViewModel;

public class CatalogActivity extends AppCompatActivity {


    private int catalogId;
    private Catalog catalog;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);
        catalogId = getIntent().getIntExtra("catalogId", 0);
        CatalogViewModel catalogViewModel = new ViewModelProvider(this).get(CatalogViewModel.class);

        catalogViewModel.getCatalogById(catalogId, catalog1 -> {
            catalog = catalog1;
            runOnUiThread(this::initializeElements);
        });

    }

    public void initializeElements() {
        TextView catalogTitle = findViewById(R.id.catalogName);
        catalogTitle.setText("Katalog - " + catalog.title);

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CatalogActivity.this, MainActivity.class);
                startActivity(intent);

            }
        });

        LinearLayout templates = findViewById(R.id.templates);
        LinearLayout blocks = findViewById(R.id.blocks);
        LinearLayout clients = findViewById(R.id.clients);

        templates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CatalogActivity.this, TemplatesActivity.class);
                intent.putExtra("catalogId", catalogId);
                startActivity(intent);
            }
        });

        blocks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CatalogActivity.this, BlocksActivity.class);
                intent.putExtra("catalogId", catalogId);
                startActivity(intent);
            }
        });

        clients.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CatalogActivity.this, ClientsActivity.class);
                intent.putExtra("catalogId", catalogId);
                startActivity(intent);
            }
        });


    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view == null) {
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

}