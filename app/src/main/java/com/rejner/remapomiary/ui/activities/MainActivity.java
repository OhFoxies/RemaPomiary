package com.rejner.remapomiary.ui.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.flexbox.FlexboxLayout;
import com.rejner.remapomiary.R;
import com.rejner.remapomiary.data.entities.Catalog;
import com.rejner.remapomiary.ui.utils.PostalCodeTextWatcher;
import com.rejner.remapomiary.ui.viewmodels.CatalogViewModel;


import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ArrayAdapter arrayAdapter;
    private CatalogViewModel catalogViewModel;
    private boolean sortByCreation = false;

    private EditText catalogTitle;
    private EditText catalogCity;
    private EditText catalogStreet;
    private EditText catalogPostalCode;
    private List<EditText> inputs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        List<String> sortOptions = Arrays.asList("Data utworzenia", "Data edycji");
        catalogViewModel = new ViewModelProvider(this).get(CatalogViewModel.class);
        catalogTitle = findViewById(R.id.inputCatalogTitle);
        catalogCity = findViewById(R.id.inputCatalogCity);
        catalogStreet = findViewById(R.id.inputCatalogStreet);
        catalogPostalCode = findViewById(R.id.inputCatalogPostalCode);
        catalogPostalCode.addTextChangedListener(new PostalCodeTextWatcher(catalogPostalCode));
        inputs = new ArrayList<>(Arrays.asList(catalogTitle, catalogCity, catalogStreet, catalogPostalCode));

        arrayAdapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_spinner_item, sortOptions);
        Spinner spinner = findViewById(R.id.sortBySpinner);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean before = sortByCreation;
                if (sortOptions.get(position).equals("Data edycji")) {
                    sortByCreation = true;

                } else {
                    sortByCreation = false;
                }
                if (sortByCreation != before) {
                    updateCatalogsView();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        findViewById(R.id.catalogCreate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createCatalog();
            }
        });

        findViewById(R.id.catalogCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {clearInput();
            }
        });

        updateCatalogsView();
    }

    public void deleteCatalog(Catalog catalog) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Potwierdzenie");
        builder.setMessage("Czy na pewno chcesz usunąć ten katalog?");
        builder.setPositiveButton("Tak", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                catalogViewModel.repository.delete(catalog, MainActivity.this::updateCatalogsView);
                Toast.makeText(MainActivity.this, "Katalog oraz jego zawartość została usunięta", Toast.LENGTH_SHORT).show();
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

    public void updateUI(List<Catalog> catalogs) {

        runOnUiThread(() -> {
            if (catalogs.isEmpty()) {
                LinearLayout sortSection = findViewById(R.id.sortSection);
                sortSection.setVisibility(View.GONE);

                TextView noCatalogs = findViewById(R.id.noCatalogs);
                noCatalogs.setVisibility(View.VISIBLE);
            } else {
                LinearLayout sortSection = findViewById(R.id.sortSection);
                sortSection.setVisibility(View.VISIBLE);

                TextView noCatalogs = findViewById(R.id.noCatalogs);
                noCatalogs.setVisibility(View.GONE);
            }


            FlexboxLayout flexboxLayout = findViewById(R.id.catalogs);
            flexboxLayout.removeAllViews();
            for (Catalog catalog : catalogs) {
                View catalogView = getLayoutInflater().inflate(R.layout.measurement_item, flexboxLayout, false);

                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                SimpleDateFormat sdfh = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());

                TextView creationDate = catalogView.findViewById(R.id.catalogCreationDate);
                TextView editionDate = catalogView.findViewById(R.id.catalogLastEdited);
                TextView title = catalogView.findViewById(R.id.catalogTitle);
                TextView street = catalogView.findViewById(R.id.catalogStreet);
                TextView city = catalogView.findViewById(R.id.catalogCity);
                TextView postalCode = catalogView.findViewById(R.id.catalogPostalCode);
                creationDate.setText(sdf.format(catalog.creation_date));
                editionDate.setText(sdfh.format(catalog.edition_time));
                title.setText(catalog.title);
                city.setText(catalog.city);
                postalCode.setText(catalog.postal_code);
                street.setText(catalog.street);

                Button deleteButton = catalogView.findViewById(R.id.catalogDelete);
                Button editButton = catalogView.findViewById(R.id.catalogEdit);

                catalogView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openCatalog(catalog);
                    }
                });
                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteCatalog(catalog);
                    }
                });

                editButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        updateCatalog(catalog, catalogView, editButton, deleteButton);
                    }
                });

                flexboxLayout.addView(catalogView);

            }
        });

    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }


    public void openCatalog(Catalog catalog) {
        Intent intent = new Intent(MainActivity.this, CatalogActivity.class);
        intent.putExtra("catalogId", catalog.id);
        startActivity(intent);

    }

    public void updateCatalog(Catalog catalog, View catalogView, Button editButton, Button deleteButton) {
        catalogView.setOnClickListener(null);
        TextView title = catalogView.findViewById(R.id.catalogTitle);
        TextView street = catalogView.findViewById(R.id.catalogStreet);
        TextView city = catalogView.findViewById(R.id.catalogCity);
        TextView postalCode = catalogView.findViewById(R.id.catalogPostalCode);

        List<TextView> list = Arrays.asList(street, city, postalCode);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMarginStart(dpToPx(25));

        for (TextView element: list) {
            EditText editText = new EditText(MainActivity.this);
            editText.setText(element.getText().toString());
            LinearLayout linearLayout =  catalogView.findViewById(R.id.itemMeasurementData);
            editText.setLayoutParams(params);
            editText.setTextSize(TypedValue.COMPLEX_UNIT_SP,20);


            if (element == postalCode) {
                editText.addTextChangedListener(new PostalCodeTextWatcher(editText));
                editText.setMaxLines(1);
                editText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
            }

            int index = linearLayout.indexOfChild(element);
            linearLayout.removeView(element);
            linearLayout.addView(editText, index);
        }

        LinearLayout linearLayout =  catalogView.findViewById(R.id.measurementMain);
        EditText titleEditText = new EditText(MainActivity.this);
        titleEditText.setText(title.getText().toString());
        titleEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP,24);


        linearLayout.removeView(title);
        linearLayout.addView(titleEditText, 0);

        editButton.setText("✅ Zapisz");
        deleteButton.setText("❌ Anuluj");
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateCatalogsView();
            }
        });

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<String> list = new ArrayList<>();
                LinearLayout linearLayout =  catalogView.findViewById(R.id.itemMeasurementData);
                for (int i = 0; i < linearLayout.getChildCount(); i++) {
                    View child = linearLayout.getChildAt(i);
                    if (child instanceof EditText) {
                        EditText editText = (EditText) child;
                        String value = editText.getText().toString();
                        list.add(value);
                    }
                }
                catalogViewModel.update(list.get(0), list.get(1), list.get(2), titleEditText.getText().toString(), new Date(), catalog.id, MainActivity.this::updateCatalogsView);
            }
        });




    }
    public void updateCatalogsView() {
        List<Catalog> catalogs = new ArrayList<>();

        if (sortByCreation) {
            catalogViewModel.getAllCatalogsByCreation(catalogsList -> {
                catalogs.addAll(catalogsList);
                updateUI(catalogs);
            });
        } else {
            catalogViewModel.getAllCatalogsByEdition(catalogsList -> {
                catalogs.addAll(catalogsList);
                updateUI(catalogs);
            });
        }
    }
    private void clearInput() {
        for (EditText input : inputs) {
            input.setText("");
            input.clearFocus();
        }
    }
    private void createCatalog() {


        for (EditText input : inputs) {
            if (input.getText().toString().isEmpty()) {
                input.setError("Wymagane pole");
                Toast.makeText(MainActivity.this, input.getHint().toString() + " nie jest podany/e", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        catalogViewModel.getCatalogByName(catalogTitle.getText().toString(), catalog -> {
            runOnUiThread(() -> {
                if (catalog == null) {
                    Catalog newCatalog = new Catalog(catalogTitle.getText().toString(), catalogCity.getText().toString(), catalogStreet.getText().toString(), catalogPostalCode.getText().toString(), new Date(), new Date());
                    catalogViewModel.insert(newCatalog, MainActivity.this::updateCatalogsView);
                    clearInput();

                } else {
                    Toast.makeText(MainActivity.this, "Katalog z tym tytułem już istnieje!", Toast.LENGTH_SHORT).show();
                }
            });

        });
    }
}