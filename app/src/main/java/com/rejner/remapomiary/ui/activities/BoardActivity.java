package com.rejner.remapomiary.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class BoardActivity extends AppCompatActivity {

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
            "Pralka",
            "Piekarnia",
            "Lodówka",
            "Zmywarka",
            "Podgrzewacz",
            "Rolety",
            "Oświetlenie",
            "Gniazdka -",
            "Inny"
    };
    private final String[] phases = {"L1", "L2", "L3", "3f"};

    // --- NOWE: pola dla uwag ---
    private EditText circuitNotesEditText;
    private Button saveNotesButton;
    private LinearLayout notesContainer; // kontener z EditText + przyciskiem
    // ---------------------------

    // --- NOWE: pola dla wyboru typu instalacji ---
    private LinearLayout installationTypeContainer;
    private RadioGroup installationRadioGroup;
    private RadioButton tnSRadio;
    private RadioButton tnCRadio;
    private boolean firstLoad = true;

    private boolean isSettingInstallation = false;
    // ---------------------------------------------

    // --- OPTIMIZACJE: cache i ponowne użycie ---
    private ArrayAdapter<String> nameAdapter;
    private ArrayAdapter<String> phaseAdapter;
    private Set<String> nameSet;
    private float dp;
    private int smallPad;
    private int mediumPad;
    private int largePad;
    private int smallMargin;
    private android.content.res.ColorStateList greenTint;
    private android.content.res.ColorStateList redTint;
    private android.content.res.ColorStateList blueTint;
    private android.graphics.drawable.Drawable rowBackgroundDrawable;
    private android.graphics.drawable.Drawable inputDrawable;
    private InputMethodManager imm;
    private int catalogId;
    // -------------------------------------------

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board);
        flatId = getIntent().getIntExtra("flatId", -1);
        catalogId = getIntent().getIntExtra("catalogId", -1);

        // CACHE: dp i wartości paddingów/marginów/zasobów
        dp = getResources().getDisplayMetrics().density;
        smallPad = (int) (8 * dp);
        mediumPad = (int) (16 * dp);
        largePad = (int) (24 * dp);
        smallMargin = (int) (4 * dp);

        greenTint = ContextCompat.getColorStateList(this, android.R.color.holo_green_dark);
        redTint = ContextCompat.getColorStateList(this, android.R.color.holo_red_dark);
        blueTint = ContextCompat.getColorStateList(this, android.R.color.holo_blue_dark);
        rowBackgroundDrawable = ContextCompat.getDrawable(this, R.drawable.circuit_row_background);
        inputDrawable = ContextCompat.getDrawable(this, R.drawable.input);

        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        // Przygotuj ponownie używane adaptery i set zawierający nazwy
        nameAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, circuitNames);
        nameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        phaseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, phases);
        phaseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        nameSet = new HashSet<>(Arrays.asList(circuitNames));

        flatViewModel = new ViewModelProvider(this).get(FlatViewModel.class);
        // Pobierz flat (asynchronicznie) i przypisz do pola
        flatViewModel.getFlatByIdSync(flatId, flat1 -> {
            flat = flat1.flat;
            runOnUiThread(() -> {
                setupUIElements();
                prepareInstallationTypeViews(); // przygotuj widok typ instalacji
                // jeśli notes zostały dodane wcześniej do layoutu, zaktualizuj ich tekst
                if (circuitNotesEditText != null && flat != null) {
                    circuitNotesEditText.setText(flat.circuitNotes != null ? flat.circuitNotes : "");
                }
                // ustawienie wyboru instalacji na podstawie obiektu flat (jeśli istnieje)
                if (installationRadioGroup != null && flat != null) {
                    isSettingInstallation = true;
                    if (flat.type != null && flat.type.equals("TN-C")) {
                        installationRadioGroup.check(tnCRadio.getId());
                    } else {
                        // domyślnie TN-S
                        installationRadioGroup.check(tnSRadio.getId());
                        if (flat.type == null) {
                            flat.type = "TN-S";
                            flatViewModel.update(flat);
                        }
                    }
                    isSettingInstallation = false;
                }
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
        // przygotuj widoki uwag (kontener, EditText, przyciskiem) — nie dodajemy ich jeszcze do boardLayout,
        // bo najpierw chcemy, żeby przy obserwacji listy obwodów były dodane zawsze po addCircuitButton.
        prepareNotesViews();

        // Upewnij się, że widok wyboru instalacji został przygotowany także jeśli flat załaduje się później
        if (installationTypeContainer == null) prepareInstallationTypeViews();
        circuitViewModel.getCircuitsForFlat(flatId).observe(this, circuits -> {
            isInitializing = true;

            // ZAMIANA: usuwamy wszystkie podwidoki i tworzymy je na nowo (jak było), ale tylko raz per zmiana listy.
            boardLayout.removeAllViews();
            boardLayout.addView(sectionTitle);

            if (installationTypeContainer == null) {
                prepareInstallationTypeViews();
            }
            if (installationTypeContainer != null) {
                boardLayout.addView(installationTypeContainer);
            }

            boardLayout.addView(headerLayout);

            int counter = 1;
            for (Circuit c : circuits) {
                addCircuitView(c, counter++);
            }

            boardLayout.addView(addCircuitButton);
            addOrUpdateNotesView();

            isInitializing = false;

            // 🟢 tylko przy pierwszym załadowaniu przewiń na górę
            if (firstLoad) {
                boardLayout.post(() -> {
                    ScrollView scrollView = findScrollView(boardLayout);
                    if (scrollView != null) {
                        scrollView.scrollTo(0, 0);
                    }
                });
                firstLoad = false;
            }
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
                View focused = getCurrentFocus();
                if (imm != null && focused != null) {
                    imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
                }
            });
        });
    }

    private ScrollView findScrollView(View view) {
        View parent = (View) view.getParent();
        while (parent != null && !(parent instanceof ScrollView)) {
            parent = (View) parent.getParent();
        }
        return (ScrollView) parent;
    }

    private void setupUIElements() {
        Button backButton = findViewById(R.id.backButton);
        Button notesButton = findViewById(R.id.notesButton);
        Button RCDButton = findViewById(R.id.RCDButton);
        Button roomsButton = findViewById(R.id.roomsButton);
        if (catalogId != -1) {
            notesButton.setVisibility(View.GONE);
        }

        notesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BoardActivity.this, NotesActivity.class);
                if (catalogId != -1) {
                    intent.putExtra("catalogId", catalogId);

                }
                intent.putExtra("flatId", flat.id);
                startActivity(intent);
            }
        });


        RCDButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BoardActivity.this, RCDActivity.class);
                if (catalogId != -1) {
                    intent.putExtra("catalogId", catalogId);

                }
                intent.putExtra("flatId", flat.id);
                startActivity(intent);
            }
        });

        roomsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (flat == null) return;
                Intent intent = new Intent(BoardActivity.this, RoomActivity.class);
                if (catalogId != -1) {
                    intent.putExtra("catalogId", catalogId);

                }
                intent.putExtra("flatId", flat.id);
                startActivity(intent);
            }
        });


        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (flat == null) return;
                if (catalogId != -1) {
                    Intent intent = new Intent(BoardActivity.this, TemplatesActivity.class);
                    intent.putExtra("catalogId", catalogId);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(BoardActivity.this, FlatsActivity.class);
                    intent.putExtra("blockId", flat.blockId);
                    startActivity(intent);
                }

            }
        });

        TextView mainTitle  = findViewById(R.id.flatTitle);
        mainTitle.setText("Mieszkanie numer - " + (flat != null ? flat.number : "") + " rodzielnia");
    }

    private void setupSectionTitle() {
        sectionTitle = new TextView(this);
        sectionTitle.setText("Rozdzielnia - obwody");
        sectionTitle.setTextSize(20f);
        sectionTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        sectionTitle.setPadding(mediumPad, mediumPad, mediumPad, smallPad);
        sectionTitle.setGravity(Gravity.CENTER_HORIZONTAL);
        sectionTitle.setTextColor(ContextCompat.getColor(this, android.R.color.black));
    }

    private void setupTableHeader() {
        headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setPadding(smallPad, smallPad, smallPad, smallPad);

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
        addCircuitButton.setBackgroundTintList(greenTint);
        addCircuitButton.setAllCaps(false);
    }

    public void hideKeyboard() {
        View view = getCurrentFocus();
        if (view == null) {
            view = new View(this);
        }
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void addCircuitView(Circuit circuit, int number) {
        // Tworzymy wiersz
        LinearLayout circuitRow = new LinearLayout(this);
        circuitRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, smallMargin, 0, smallMargin);
        circuitRow.setLayoutParams(params);
        circuitRow.setPadding(smallPad, smallPad, smallPad, smallPad);
        circuitRow.setBackground(rowBackgroundDrawable);
        circuitRow.setGravity(Gravity.CENTER_VERTICAL);

        // Numer
        TextView numberView = new TextView(this);
        numberView.setText(String.valueOf(number));
        numberView.setGravity(Gravity.CENTER);
        numberView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f));

        // Nazwa container
        LinearLayout nameContainer = new LinearLayout(this);
        nameContainer.setOrientation(LinearLayout.HORIZONTAL);
        nameContainer.setGravity(Gravity.CENTER_VERTICAL);
        nameContainer.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f));

        Spinner nameSpinner = new Spinner(this);
        // używamy wcześniej przygotowanego adaptera
        nameSpinner.setAdapter(nameAdapter);

        EditText customName = new EditText(this);
        customName.setHint("Własny opis");
        customName.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        customName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        customName.setVisibility(View.GONE);
        customName.setFocusable(true);
        customName.setFocusableInTouchMode(true);

        Button saveButton = new Button(this);
        saveButton.setText("✔");
        saveButton.setTextSize(16f);
        saveButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        saveButton.setVisibility(View.GONE);

        // Wybór domyślny / customName
        int selectedIndex = 0;
        for (int i = 0; i < circuitNames.length; i++) {
            if (circuitNames[i].equals(circuit.name)) {
                selectedIndex = i;
                break;
            }
        }
        if (circuit.name != null && !circuit.name.isEmpty() && !nameSet.contains(circuit.name)) {
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
        deleteButton.setBackgroundTintList(redTint);
        deleteButton.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.7f));

        // Listener dla spinnera nazw
        nameSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
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

                    customName.post(() -> {
                        customName.requestFocus();
                        if (imm != null) {
                            imm.showSoftInput(customName, InputMethodManager.SHOW_IMPLICIT);
                        }
                        boardLayout.postDelayed(() -> {
                            if (imm != null) {
                                imm.showSoftInput(customName, InputMethodManager.SHOW_IMPLICIT);
                            }
                        }, 80);
                    });
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

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        customName.setOnFocusChangeListener((v, hasFocus) ->
                saveButton.setVisibility(hasFocus ? View.VISIBLE : View.GONE)
        );

        customName.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                saveButton.setVisibility(View.VISIBLE);
                customName.post(() -> {
                    customName.requestFocus();
                    if (imm != null) imm.showSoftInput(customName, InputMethodManager.SHOW_IMPLICIT);
                });
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
            if (imm != null) {
                imm.hideSoftInputFromWindow(customName.getWindowToken(), 0);
            }
            customName.clearFocus();
            saveButton.setVisibility(View.GONE);
        });

        phaseSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (isInitializing) return;
                String newType = phases[position];
                if (!newType.equals(circuit.type)) {
                    circuit.type = newType;
                    circuitViewModel.update(circuit);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        deleteButton.setOnClickListener(v -> {
            if (!isInitializing) circuitViewModel.delete(circuit);
        });

        circuitRow.addView(numberView);
        circuitRow.addView(nameContainer);
        circuitRow.addView(phaseSpinner);
        circuitRow.addView(deleteButton);

        boardLayout.addView(circuitRow);
    }

    // --- NOWE METODY I PRZYGOTOWANIE WIDOKÓW UWAG ---

    private void prepareNotesViews() {
        if (notesContainer == null) {
            notesContainer = new LinearLayout(this);
            notesContainer.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams ncParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            int margin = (int) (6 * dp);
            ncParams.setMargins(0, margin, 0, margin);
            notesContainer.setLayoutParams(ncParams);
            notesContainer.setPadding(mediumPad, (int)(20*dp), mediumPad, (int)(20*dp));
            notesContainer.setBackground(rowBackgroundDrawable);
        }

        // 🔹 Nagłówek
        TextView notesTitle = new TextView(this);
        notesTitle.setText("Uwagi do rozdzielni");
        notesTitle.setTextSize(22f);
        notesTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        notesTitle.setPadding(0, 0, 0, (int) (16 * dp));
        notesTitle.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        notesTitle.setGravity(Gravity.START);

        // 🔹 Pole tekstowe
        circuitNotesEditText = new EditText(this);
        if (catalogId != -1) {
            circuitNotesEditText.setEnabled(false);

        }
        circuitNotesEditText.setHint("Wpisz uwagi...");
        circuitNotesEditText.setMinLines(5);
        circuitNotesEditText.setMaxLines(10);
        circuitNotesEditText.setGravity(Gravity.TOP | Gravity.START);
        circuitNotesEditText.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        circuitNotesEditText.setFocusable(true);
        circuitNotesEditText.setFocusableInTouchMode(true);

        LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        circuitNotesEditText.setLayoutParams(etParams);

        // 🔹 Usuwamy underline i dajemy ramkę
        circuitNotesEditText.setBackground(inputDrawable);
        circuitNotesEditText.setPadding((int)(20*dp), (int)(16*dp), (int)(20*dp), (int)(16*dp));
        circuitNotesEditText.setText(flat != null && flat.circuitNotes != null ? flat.circuitNotes : "");

        // 🔹 Przycisk Zapisz
        saveNotesButton = new Button(this);
        saveNotesButton.setText("✔ Zapisz");
        if (catalogId != -1) {
            saveNotesButton.setEnabled(false);

        }
        saveNotesButton.setTextSize(16f);
        saveNotesButton.setVisibility(View.GONE);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        btnParams.gravity = Gravity.END;
        saveNotesButton.setLayoutParams(btnParams);
        saveNotesButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        saveNotesButton.setBackgroundTintList(blueTint);

        // Obsługa fokusa i zapisu
        circuitNotesEditText.setOnFocusChangeListener((v, hasFocus) ->
                saveNotesButton.setVisibility(hasFocus ? View.VISIBLE : View.GONE)
        );

        circuitNotesEditText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                saveNotesButton.setVisibility(View.VISIBLE);
                circuitNotesEditText.post(() -> {
                    circuitNotesEditText.requestFocus();
                    if (imm != null) imm.showSoftInput(circuitNotesEditText, InputMethodManager.SHOW_IMPLICIT);
                });
            }
            return false;
        });

        saveNotesButton.setOnClickListener(v -> {
            final String newNotes = circuitNotesEditText.getText() != null ? circuitNotesEditText.getText().toString() : "";
            if (flat != null) {
                flat.circuitNotes = newNotes;
                flatViewModel.update(flat);
            }
            boardLayout.requestFocus();
            if (imm != null) {
                imm.hideSoftInputFromWindow(circuitNotesEditText.getWindowToken(), 0);
            }
            circuitNotesEditText.clearFocus();
            saveNotesButton.setVisibility(View.GONE);
        });

        notesContainer.removeAllViews();
        notesContainer.addView(notesTitle);
        notesContainer.addView(circuitNotesEditText);
        notesContainer.addView(saveNotesButton);
    }

    private void prepareInstallationTypeViews() {
        if (installationTypeContainer != null) return;

        installationTypeContainer = new LinearLayout(this);
        installationTypeContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) (40 * dp);
        containerParams.setMargins(0, margin, 0, margin);
        installationTypeContainer.setLayoutParams(containerParams);

        installationTypeContainer.setPadding(largePad, largePad, largePad, largePad);
        installationTypeContainer.setBackground(rowBackgroundDrawable);
        installationTypeContainer.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView label = new TextView(this);
        label.setText("Typ instalacji");
        label.setTextSize(22f);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        label.setPadding(0, 0, 0, (int) (20 * dp));
        label.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        label.setGravity(Gravity.CENTER);

        installationRadioGroup = new RadioGroup(this);
        installationRadioGroup.setOrientation(RadioGroup.HORIZONTAL);
        LinearLayout.LayoutParams rgParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        installationRadioGroup.setLayoutParams(rgParams);
        installationRadioGroup.setGravity(Gravity.CENTER);

        tnSRadio = new RadioButton(this);

        tnSRadio.setText("TN-S");
        tnSRadio.setId(View.generateViewId());
        tnSRadio.setTextSize(20f);
        LinearLayout.LayoutParams tnSParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        tnSParams.setMargins((int) (40 * dp), 0, (int) (40 * dp), 0);
        tnSRadio.setLayoutParams(tnSParams);

        tnCRadio = new RadioButton(this);
        tnCRadio.setText("TN-C");
        tnCRadio.setId(View.generateViewId());
        tnCRadio.setTextSize(20f);
        LinearLayout.LayoutParams tnCParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        tnCParams.setMargins((int) (40 * dp), 0, (int) (40 * dp), 0);
        tnCRadio.setLayoutParams(tnCParams);

        installationRadioGroup.addView(tnSRadio);
        installationRadioGroup.addView(tnCRadio);

        // Domyślnie TN-S
        installationRadioGroup.check(tnSRadio.getId());

        installationRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (isSettingInstallation) return;
            if (flat == null) return;
            String newType = (checkedId == tnCRadio.getId()) ? "TN-C" : "TN-S";
            if (flat.type == null || !flat.type.equals(newType)) {
                flat.type = newType;
                flatViewModel.update(flat);
            }
        });

        installationTypeContainer.removeAllViews();
        installationTypeContainer.addView(label);
        installationTypeContainer.addView(installationRadioGroup);
    }

    private void addOrUpdateNotesView() {
        if (notesContainer == null) {
            prepareNotesViews();
        }
        if (flat != null && circuitNotesEditText != null) {
            circuitNotesEditText.setText(flat.circuitNotes != null ? flat.circuitNotes : "");
        }
        // Usuń istniejący notesContainer (jeśli był dodany wcześniej), żeby nie duplikować
        int childCount = boardLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = boardLayout.getChildAt(i);
            if (child == notesContainer) {
                boardLayout.removeView(child);
                break;
            }
        }
        boardLayout.addView(notesContainer);
    }
    // --- KONIEC NOWYCH METOD ---
}
