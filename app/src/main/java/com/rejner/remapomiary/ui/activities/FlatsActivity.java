package com.rejner.remapomiary.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
import com.rejner.remapomiary.data.entities.BlockFullData;
import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.ui.viewmodels.BlockViewModel;
import com.rejner.remapomiary.ui.viewmodels.CatalogViewModel;
import com.rejner.remapomiary.ui.viewmodels.FlatViewModel;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FlatsActivity extends AppCompatActivity {

    private FlatViewModel flatViewModel;
    private EditText inputFlatNumber;
    private Spinner templatesSpinner, sortBySpinner;
    private Button flatAddButton, flatCancelButton;
    private FlexboxLayout flatsContainer;
    private TextView noFlatsText;
    private BlockViewModel blockViewModel;
    private int blockId;
    private List<Flat> currentFlats;
    private final SimpleDateFormat creationDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private CatalogViewModel catalogViewModel;
    private BlockFullData block;
    private final SimpleDateFormat editDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flats);
        blockId = getIntent().getIntExtra("blockId", -1);
        catalogViewModel = new ViewModelProvider(FlatsActivity.this).get(CatalogViewModel.class);

        if (blockId == -1) {
            Toast.makeText(this, "Błąd: nie przekazano ID bloku!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        blockViewModel = new ViewModelProvider(this).get(BlockViewModel.class);
        blockViewModel.getBlockById(blockId, block1 -> {
            block = block1;
            runOnUiThread(() -> {
                TextView textView = findViewById(R.id.flatsTitle);
                textView.setText("Mieszkania w bloku - " + block.block.street + "/" + block.block.number);
            });
        });
        flatViewModel = new ViewModelProvider(this).get(FlatViewModel.class);

        inputFlatNumber = findViewById(R.id.inputFlatNumber);
        templatesSpinner = findViewById(R.id.templatesSpinner);
        sortBySpinner = findViewById(R.id.sortBySpinner);
        flatAddButton = findViewById(R.id.flatAdd);
        flatCancelButton = findViewById(R.id.flatCancel);
        flatsContainer = findViewById(R.id.clients);
        noFlatsText = findViewById(R.id.noFlats);

        setupSortSpinner();

        flatAddButton.setOnClickListener(v -> {
            String flatNumber = inputFlatNumber.getText().toString().trim();

            if (flatNumber.isEmpty()) {
                Toast.makeText(this, "Podaj numer mieszkania!", Toast.LENGTH_SHORT).show();
                return;
            }
            boolean exists = false;
            if (currentFlats != null) {
                for (Flat f : currentFlats) {
                    if (f.number.equalsIgnoreCase(flatNumber)) {
                        exists = true;
                        break;
                    }
                }
            }

            if (exists) {
                Toast.makeText(this, "Mieszkanie o tym numerze już istnieje!", Toast.LENGTH_SHORT).show();
                return;
            }

            Date now = new Date();
            Flat newFlat = new Flat();
            newFlat.number = flatNumber;
            newFlat.creation_date = now;
            newFlat.edition_date = now;
            newFlat.status = "Pomiar niewykonany ❌";
            newFlat.blockId = blockId;

            flatViewModel.insert(newFlat);
            catalogViewModel.updateEdition(block.block.catalogId);
            blockViewModel.updateEdition(blockId);

            Toast.makeText(this, "Dodano mieszkanie nr " + flatNumber, Toast.LENGTH_SHORT).show();
            inputFlatNumber.setText("");
            hideKeyboard();
            inputFlatNumber.clearFocus();

        });

        flatCancelButton.setOnClickListener(v -> {
            inputFlatNumber.setText("");
            hideKeyboard();

            inputFlatNumber.clearFocus();

        });
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();

            }
        });
        flatViewModel.getFlatsByBlockId(blockId).observe(this, flats -> {
            currentFlats = flats;
            updateFlatsDisplay();
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
    private void setupSortSpinner() {
        String[] sortOptions = {"Numer mieszkania", "Data utworzenia", "Data edycji", "Status"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sortOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortBySpinner.setAdapter(adapter);

        sortBySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateFlatsDisplay();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void updateFlatsDisplay() {
        if (currentFlats == null) return;

        flatsContainer.removeAllViews();

        if (currentFlats.isEmpty()) {
            noFlatsText.setVisibility(View.VISIBLE);
            return;
        }

        noFlatsText.setVisibility(View.GONE);

        String selectedSort = (String) sortBySpinner.getSelectedItem();

        if (selectedSort.equals("Numer mieszkania")) {
            Collections.sort(currentFlats, Comparator.comparing(f -> f.number.toLowerCase(Locale.ROOT)));
        } else if (selectedSort.equals("Data utworzenia")) {
            Collections.sort(currentFlats, (f1, f2) -> f2.creation_date.compareTo(f1.creation_date));
        } else if (selectedSort.equals("Data edycji")) {
            Collections.sort(currentFlats, (f1, f2) -> f2.edition_date.compareTo(f1.edition_date));
        } else if (selectedSort.equals("Status")) {
            // "Pomiar gotowy" na górze, "Pomiar niewykonany" na dole
            Collections.sort(currentFlats, (f1, f2) -> {
                boolean done1 = f1.status.contains("gotowy");
                boolean done2 = f2.status.contains("gotowy");
                return Boolean.compare(done2, done1); // true = 1, false = 0, więc "gotowy" na górze
            });
        }

        for (Flat flat : currentFlats) {
            addFlatItemToFlex(flat);
        }
    }

    private void addFlatItemToFlex(Flat flat) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View itemView = inflater.inflate(R.layout.flat_item, flatsContainer, false);

        TextView title = itemView.findViewById(R.id.flatTitle);
        TextView creationDate = itemView.findViewById(R.id.flatCreationDate);
        TextView editDate = itemView.findViewById(R.id.flatLastEdited);
        TextView status = itemView.findViewById(R.id.blockLastEdited);
        Button markButton = itemView.findViewById(R.id.flatMark);
        Button deleteButton = itemView.findViewById(R.id.blockDelete);
        Button editButton = itemView.findViewById(R.id.blockEdit);
        LinearLayout flatMain = itemView.findViewById(R.id.flatMain);

        title.setText("Mieszkanie nr " + flat.number);
        creationDate.setText(creationDateFormat.format(flat.creation_date));
        editDate.setText(editDateFormat.format(flat.edition_date));
        status.setText(flat.status);
        EditText titleEdit = new EditText(this);


        titleEdit.setText(flat.number);
        titleEdit.setTextSize(24);
        titleEdit.setVisibility(View.GONE);

        flatMain.addView(titleEdit, 0);
        if (flat.status.contains("gotowy")) {
            markButton.setText("❌ Oznacz jako niewykonany");
            flatMain.setBackgroundResource(R.drawable.border_done);
            markButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF0000")));
        } else {
            markButton.setText("✅ Oznacz jako gotowy");
            flatMain.setBackgroundResource(R.drawable.border);
            markButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#3FAB1F")));
        }


        Button markBtn = markButton;
        TextView statusView = status;

        markBtn.setOnClickListener(v -> {
            boolean isDone = flat.status.contains("gotowy");

            if (isDone) {
                flat.status = "Pomiar niewykonany ❌";
                markBtn.setText("✅ Oznacz jako gotowy");
                flatMain.setBackgroundResource(R.drawable.border);
            } else {
                flat.status = "Pomiar gotowy ✅";
                markBtn.setText("❌ Oznacz jako niewykonany");
                flatMain.setBackgroundResource(R.drawable.border_done);
            }

            flat.edition_date = new Date();
            flatViewModel.update(flat);
            catalogViewModel.updateEdition(block.block.catalogId);
            blockViewModel.updateEdition(blockId);

            statusView.setText(flat.status);
        });

        editButton.setOnClickListener(v -> {
            boolean isEditing = titleEdit.getVisibility() == View.VISIBLE;

            if (!isEditing) {
                title.setVisibility(View.GONE);
                titleEdit.setVisibility(View.VISIBLE);

                editButton.setText("✅ Zapisz");
                deleteButton.setText("❌ Anuluj");
            } else {
                String newNumber = titleEdit.getText().toString().trim();

                if (newNumber.isEmpty()) {
                    Toast.makeText(this, "Numer mieszkania nie może być pusty!", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean exists = false;
                for (Flat f : currentFlats) {
                    if (f.number.equalsIgnoreCase(newNumber) && f.id != flat.id) {
                        exists = true;
                        break;
                    }
                }

                if (exists) {
                    Toast.makeText(this, "Mieszkanie o tym numerze już istnieje!", Toast.LENGTH_SHORT).show();
                    return;
                }
                hideKeyboard();
                titleEdit.clearFocus();
                flat.number = newNumber;
                flat.edition_date = new Date();
                flatViewModel.update(flat);
                blockViewModel.updateEdition(blockId);
                catalogViewModel.updateEdition(block.block.catalogId);


                title.setText(flat.number);
                title.setVisibility(View.VISIBLE);
                titleEdit.setVisibility(View.GONE);
                editButton.setText("✏️ Edytuj");
                deleteButton.setText("🗑️ Usuń");
            }
        });

        deleteButton.setOnClickListener(v -> {
            boolean isEditing = titleEdit.getVisibility() == View.VISIBLE;

            if (isEditing) {
                hideKeyboard();
                titleEdit.clearFocus();
                titleEdit.setVisibility(View.GONE);
                title.setVisibility(View.VISIBLE);
                editButton.setText("✏️ Edytuj");
                deleteButton.setText("🗑️ Usuń");
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(FlatsActivity.this);
                builder.setTitle("Potwierdzenie");
                builder.setMessage("Czy na pewno chcesz usunąć to mieszkanie?");
                builder.setPositiveButton("Tak", (dialog, which) -> {
                    flatViewModel.delete(flat);
                    blockViewModel.updateEdition(blockId);
                    catalogViewModel.updateEdition(block.block.catalogId);
                    Toast.makeText(FlatsActivity.this, "Usunięto mieszkanie nr " + flat.number, Toast.LENGTH_SHORT).show();
                });
                builder.setNegativeButton("Nie", (dialog, which) -> dialog.dismiss());
                builder.show();
            }
        });

        itemView.setOnClickListener(v -> {
            boolean isEditing = titleEdit.getVisibility() == View.VISIBLE;
            if (isEditing) {
                return;
            }
            Intent intent = new Intent(FlatsActivity.this, BoardActivity.class);
            intent.putExtra("flatId", flat.id);
            startActivity(intent);
        });
        flatsContainer.addView(itemView);
    }

}
