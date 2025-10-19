package com.rejner.remapomiary.ui.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.flexbox.FlexboxLayout;
import com.rejner.remapomiary.R;
import com.rejner.remapomiary.data.entities.Catalog;
import com.rejner.remapomiary.data.entities.Template;
import com.rejner.remapomiary.ui.viewmodels.BlockViewModel;
import com.rejner.remapomiary.ui.viewmodels.CatalogViewModel;
import com.rejner.remapomiary.ui.viewmodels.FlatViewModel;
import com.rejner.remapomiary.ui.viewmodels.TemplateViewModel;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TemplatesActivity extends AppCompatActivity {

    private int catalogId;
    private BlockViewModel blockViewModel;
    private Catalog catalog;
    private TextView noTemplates;
    private FlexboxLayout templatesContainer;
    private Button backButton;
    private TextView templatesTitle;
    private TemplateViewModel templateViewModel;
    private FlatViewModel flatViewModel;
    private CatalogViewModel catalogViewModel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_templates);
        catalogId = getIntent().getIntExtra("catalogId", 0);
        catalogViewModel = new ViewModelProvider(TemplatesActivity.this).get(CatalogViewModel.class);
        templateViewModel = new ViewModelProvider(TemplatesActivity.this).get(TemplateViewModel.class);
        flatViewModel = new ViewModelProvider(TemplatesActivity.this).get(FlatViewModel.class);
        findElements();
        catalogViewModel.getCatalogById(catalogId, catalog1 -> {
            catalog = catalog1;
            runOnUiThread(this::updateDisplay);
        });

        templateViewModel.getTemplatesInCatalog(catalogId).observe(this, this::updateTemplates);

    }

    private void updateTemplates(List<Template> templates) {
        if (templates.isEmpty()) {
            noTemplates.setVisibility(View.VISIBLE);
        } else {
            noTemplates.setVisibility(View.GONE);
        }
        templatesContainer.removeAllViews();
        for (Template t: templates) {
            View templateView = getLayoutInflater().inflate(R.layout.template_item, templatesContainer, false);
            TextView title = templateView.findViewById(R.id.templateTitle);
            TextView date = templateView.findViewById(R.id.templateDate);
            Button templateDelete = templateView.findViewById(R.id.templateDelete);

            title.setText("Szablon - " + t.name);
            SimpleDateFormat sdfh = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());

            date.setText(sdfh.format(t.creationDate));
            templateDelete.setOnClickListener(v -> {
                deleteTemplate(t);
            });

            templateView.setOnClickListener(v -> {
                openTemplate(t);
            });
            templatesContainer.addView(templateView);
        }

    }

    private void openTemplate(Template t) {
        Intent intent = new Intent(TemplatesActivity.this, BoardActivity.class);
        intent.putExtra("flatId", t.flatId);
        intent.putExtra("catalogId", catalogId);
        startActivity(intent);
    }

    private void deleteTemplate(Template template) {
        AlertDialog.Builder builder = new AlertDialog.Builder(TemplatesActivity.this);
        builder.setTitle("Potwierdzenie");
        builder.setMessage("Czy na pewno chcesz usunąć ten szablon?");
        builder.setPositiveButton("Tak", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                templateViewModel.delete(template);
                flatViewModel.getFlatByIdSync(template.flatId, flat -> {
                    flatViewModel.delete(flat.flat);

                });
                Toast.makeText(TemplatesActivity.this, "Szablon został usuniety!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Nie", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void findElements() {
        noTemplates = findViewById(R.id.noTemplates);
        backButton = findViewById(R.id.backButton);
        templatesContainer = findViewById(R.id.templatesContainer);
        templatesTitle = findViewById(R.id.templatesTitle);

    }
    private void updateDisplay() {
        templatesTitle.setText("Szablony w katalogu - " + catalog.title);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(TemplatesActivity.this, CatalogActivity.class);
            intent.putExtra("catalogId", catalogId);
            startActivity(intent);
        });
    }
}