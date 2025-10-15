package com.rejner.remapomiary.ui.activities;

import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.rejner.remapomiary.R;
import com.rejner.remapomiary.data.entities.Circuit;
import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.ui.viewmodels.CircuitViewModel;
import com.rejner.remapomiary.ui.viewmodels.FlatViewModel;

import org.w3c.dom.Text;

import java.util.Arrays;

public class FlatActivity extends AppCompatActivity {

    private LinearLayout boardLayout;
    private Button addCircuitButton;
    private CircuitViewModel circuitViewModel;
    private int flatId;
    private boolean isInitializing = false;
    private LinearLayout headerLayout;
    private TextView sectionTitle;

    private Flat flat;
    private FlatViewModel flatViewModel;
    private final String[] circuitNames = {
            "Gniazdka łazienka",
            "Indukcja",
            "Gniazdka pokoje",
            "Oświetlenie",
            "Gniazdka -",
            "Inny"
    };
    private final String[] phases = {"L1", "L2", "L3", "3f"};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flat);
        flatId = getIntent().getIntExtra("flatId", -1);

        flatViewModel = new ViewModelProvider(this).get(FlatViewModel.class);
        flatViewModel.getFlatByIdSync(flatId, flat1 -> {
            flat = flat1.flat;
            runOnUiThread(() -> {
                setupUIElements();
            });
        });
        boardLayout = findViewById(R.id.board);
        addCircuitButton = findViewById(R.id.addCircuitButton);
        circuitViewModel = new ViewModelProvider(this).get(CircuitViewModel.class);

        boardLayout.setFocusable(true);
        boardLayout.setFocusableInTouchMode(true);



        setupSectionTitle();
        setupTableHeader();
        setupAddButton();

        circuitViewModel.getCircuitsForFlat(flatId).observe(this, circuits -> {
            isInitializing = true;

            boardLayout.removeAllViews();
            boardLayout.addView(sectionTitle);
            boardLayout.addView(headerLayout);

            int counter = 1;
            for (Circuit c : circuits) {
                addCircuitView(c, counter++);
            }

            boardLayout.addView(addCircuitButton);

            boardLayout.post(boardLayout::requestFocus);
            isInitializing = false;
        });

        addCircuitButton.setOnClickListener(v -> {
            if (flatId == -1) return;
            Circuit newCircuit = new Circuit();
            newCircuit.flatId = flatId;
            newCircuit.name = "";
            newCircuit.type = "L1";
            circuitViewModel.insert(newCircuit);

            boardLayout.post(() -> {
                boardLayout.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                View focused = getCurrentFocus();
                if (imm != null && focused != null) {
                    imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
                }
            });
        });
    }


    private void setupUIElements() {
        Button backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();

            }
        });

        TextView mainTitle  = findViewById(R.id.flatTitle);
        mainTitle.setText("Mieszkanie numer - " + flat.number);

    }

    private void setupSectionTitle() {
        sectionTitle = new TextView(this);
        sectionTitle.setText("Rozdzielnia - obwody");
        sectionTitle.setTextSize(20f);
        sectionTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        sectionTitle.setPadding(16, 16, 16, 8);
        sectionTitle.setGravity(Gravity.CENTER_HORIZONTAL);
        sectionTitle.setTextColor(ContextCompat.getColor(this, android.R.color.black));
    }

    private void setupTableHeader() {
        headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        int pad = (int) (8 * getResources().getDisplayMetrics().density);
        headerLayout.setPadding(pad, pad, pad, pad);

        String[] headers = {"Nr", "Nazwa obwodu", "Faza", "Usuń"};
        float[] weights = {0.5f, 2f, 1f, 0.7f};

        for (int i = 0; i < headers.length; i++) {
            TextView header = new TextView(this);
            header.setText(headers[i]);
            header.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            header.setTextSize(16f);
            header.setTypeface(null, android.graphics.Typeface.BOLD);
            header.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, weights[i]));
            header.setGravity(Gravity.CENTER);
            headerLayout.addView(header);
        }
    }

    private void setupAddButton() {
        addCircuitButton.setText("➕ Dodaj obwód");
        addCircuitButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        addCircuitButton.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_green_dark));
        addCircuitButton.setAllCaps(false);
    }

    private void addCircuitView(Circuit circuit, int number) {
        LinearLayout circuitRow = new LinearLayout(this);
        circuitRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) (4 * getResources().getDisplayMetrics().density);
        params.setMargins(0, margin, 0, margin);
        circuitRow.setLayoutParams(params);
        int pad = (int) (8 * getResources().getDisplayMetrics().density);
        circuitRow.setPadding(pad, pad, pad, pad);
        circuitRow.setBackground(ContextCompat.getDrawable(this, R.drawable.circuit_row_background));
        circuitRow.setGravity(Gravity.CENTER_VERTICAL);

        // Numer
        TextView numberView = new TextView(this);
        numberView.setText(String.valueOf(number));
        numberView.setGravity(Gravity.CENTER);
        numberView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f));

        // Nazwa
        LinearLayout nameContainer = new LinearLayout(this);
        nameContainer.setOrientation(LinearLayout.HORIZONTAL);
        nameContainer.setGravity(Gravity.CENTER_VERTICAL);
        nameContainer.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f));

        Spinner nameSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, circuitNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        nameSpinner.setAdapter(adapter);

        EditText customName = new EditText(this);
        customName.setHint("Własny opis");
        customName.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        customName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        customName.setVisibility(View.GONE);

        Button saveButton = new Button(this);
        saveButton.setText("✔");
        saveButton.setTextSize(16f);
        saveButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        saveButton.setVisibility(View.GONE);

        int selectedIndex = 0;
        for (int i = 0; i < circuitNames.length; i++) {
            if (circuitNames[i].equals(circuit.name)) {
                selectedIndex = i;
                break;
            }
        }
        if (circuit.name != null && !circuit.name.isEmpty() && !Arrays.asList(circuitNames).contains(circuit.name)) {
            selectedIndex = circuitNames.length - 1;
            customName.setText(circuit.name);
            customName.setVisibility(View.VISIBLE);
            saveButton.setVisibility(View.GONE);
        }
        nameSpinner.setSelection(selectedIndex, false);

        nameContainer.addView(nameSpinner);
        nameContainer.addView(customName);
        nameContainer.addView(saveButton);

        Spinner phaseSpinner = new Spinner(this);
        ArrayAdapter<String> phaseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, phases);
        phaseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        phaseSpinner.setAdapter(phaseAdapter);

        int phaseIndex = 0;
        for (int i = 0; i < phases.length; i++) {
            if (phases[i].equals(circuit.type)) {
                phaseIndex = i;
                break;
            }
        }
        phaseSpinner.setSelection(phaseIndex, false);
        phaseSpinner.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        phaseSpinner.setGravity(Gravity.CENTER);

        Button deleteButton = new Button(this);
        deleteButton.setText("USUŃ");
        deleteButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        deleteButton.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_red_dark));
        deleteButton.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.7f));

        nameSpinner.post(() -> nameSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (isInitializing) return;
                String selected = circuitNames[position];

                if (selected.equals("Inny") || selected.equals("Gniazdka -")) {
                    customName.setVisibility(View.VISIBLE);
                    saveButton.setVisibility(View.VISIBLE);

                    if (selected.equals("Gniazdka -") && customName.getText().toString().isEmpty()) {
                        customName.setText("Gniazdka ");
                        customName.setSelection(customName.getText().length());
                    }

                    customName.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) imm.showSoftInput(customName, InputMethodManager.SHOW_IMPLICIT);
                } else {
                    customName.setVisibility(View.GONE);
                    saveButton.setVisibility(View.GONE);
                    if (!selected.equals(circuit.name)) {
                        circuit.name = selected;
                        circuitViewModel.update(circuit);
                    }
                    boardLayout.post(boardLayout::requestFocus);
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        }));

        customName.setOnFocusChangeListener((v, hasFocus) ->
                saveButton.setVisibility(hasFocus ? View.VISIBLE : View.GONE)
        );

        customName.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                saveButton.setVisibility(View.VISIBLE);
                customName.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(customName, InputMethodManager.SHOW_IMPLICIT);
            }
            return false;
        });

        saveButton.setOnClickListener(v -> {
            String newText = customName.getText() != null ? customName.getText().toString().trim() : "";
            if (!newText.equals(circuit.name)) {
                circuit.name = newText;
                circuitViewModel.update(circuit);
            }
            boardLayout.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            View focused = getCurrentFocus();
            if (imm != null && focused != null) {
                imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
            }
            customName.clearFocus();
            saveButton.setVisibility(View.GONE);
        });

        phaseSpinner.post(() -> phaseSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (isInitializing) return;
                String newType = phases[position];
                if (!newType.equals(circuit.type)) {
                    circuit.type = newType;
                    circuitViewModel.update(circuit);
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        }));

        deleteButton.setOnClickListener(v -> {
            if (!isInitializing) circuitViewModel.delete(circuit);
        });

        circuitRow.addView(numberView);
        circuitRow.addView(nameContainer);
        circuitRow.addView(phaseSpinner);
        circuitRow.addView(deleteButton);

        boardLayout.addView(circuitRow);
    }
}
