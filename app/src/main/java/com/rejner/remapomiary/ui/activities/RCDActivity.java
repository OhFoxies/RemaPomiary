package com.rejner.remapomiary.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.rejner.remapomiary.R;
import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.data.entities.RCD;
import com.rejner.remapomiary.ui.viewmodels.FlatViewModel;
import com.rejner.remapomiary.ui.viewmodels.RCDViewModel;

import org.w3c.dom.Text;

import java.util.Date;

public class RCDActivity extends AppCompatActivity {

    private int flatId;
    private Flat flat;
    private RCD rcd;

    private FlatViewModel flatViewModel;
    private RCDViewModel rcdViewModel;

    private Button noRCDButton;
    private LinearLayout rcdFieldsContainer;

    private EditText manufacturerEditText;
    private Spinner typeSpinner;
    private EditText time1EditText;
    private EditText time2EditText;
    private EditText notedEditText;
    private Button notesSaveButton;
    private Button rcdBrokenButton;
    private Button saveManufacturerButton;
    private Button saveTime1Button;
    private Button saveTime2Button;
    private int catalogId;

    private boolean isInitializing = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rcd);

        flatId = getIntent().getIntExtra("flatId", -1);
        catalogId = getIntent().getIntExtra("catalogId", -1);
        if (flatId == -1) {
            Toast.makeText(this, "Brak ID mieszkania", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        rcdViewModel = new ViewModelProvider(this).get(RCDViewModel.class);
        flatViewModel = new ViewModelProvider(this).get(FlatViewModel.class);

        findViews();

        flatViewModel.getFlatById(flatId).observe(this, flatEntity -> {
            if (flatEntity != null) {
                this.flat = flatEntity;
                updateNoRCDButtonState();
                setupUIElements();
            }
        });

        rcdViewModel.getRcdsForFlat(flatId).observe(this, rcds -> {
            if (rcds != null && !rcds.isEmpty()) {
                this.rcd = rcds.get(0);
                populateFields();
            } else {
                this.rcd = new RCD();
                this.rcd.flatId = flatId;
                this.rcd.type = "A";
                rcdViewModel.insert(this.rcd);
            }
            if (this.rcd != null) {
                isInitializing = true;
                updateRCDBrokenButton();
                populateFields();
                isInitializing = false;
            }
        });

        setupListeners();
    }

    private void findViews() {
        noRCDButton = findViewById(R.id.noRCD);
        rcdFieldsContainer = findViewById(R.id.rcdInfo);
        manufacturerEditText = findViewById(R.id.manufacturerEditText);
        typeSpinner = findViewById(R.id.typeSpinner);
        time1EditText = findViewById(R.id.time1EditText);
        time2EditText = findViewById(R.id.time2EditText);
        notedEditText = findViewById(R.id.notedEditText);
        notesSaveButton = findViewById(R.id.notesSave);
        rcdBrokenButton = findViewById(R.id.RCDBroken);
        saveManufacturerButton = findViewById(R.id.saveManufacturerButton);
        saveTime1Button = findViewById(R.id.saveTime1Button);
        saveTime2Button = findViewById(R.id.saveTime2Button);
        if (catalogId != -1) {
            time1EditText.setEnabled(false);
            time2EditText.setEnabled(false);
            notedEditText.setEnabled(false);
            notesSaveButton.setEnabled(false);
            rcdBrokenButton.setEnabled(false);
        }
    }
    private void setupUIElements() {
        Button backButton = findViewById(R.id.backButton);
        Button notesButton = findViewById(R.id.notesButton);
        Button roomsButton = findViewById(R.id.roomsButton);
        Button boardButton = findViewById(R.id.boardButton);
        TextView titleView = findViewById(R.id.rcdTitle);
        titleView.setText("Mieszkanie numer - " + (flat != null ? flat.number : "") + " różnicówka");
        Button backSave = findViewById(R.id.backSave);

        backSave.setOnClickListener(v -> {
            flat.status = "Pomiar gotowy ✅";
            flat.edition_date = new Date();
            flatViewModel.update(flat);
            Intent intent = new Intent(RCDActivity.this, FlatsActivity.class);
            intent.putExtra("blockId", flat.blockId);
            startActivity(intent);
        });

        if (catalogId != -1) {
            notesButton.setVisibility(View.GONE);
        }
        boardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RCDActivity.this, BoardActivity.class);
                if (catalogId != -1) {
                    intent.putExtra("catalogId", catalogId);

                }
                intent.putExtra("flatId", flat.id);
                startActivity(intent);
            }
        });

        notesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RCDActivity.this, NotesActivity.class);
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
                Intent intent = new Intent(RCDActivity.this, RoomActivity.class);
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
                    Intent intent = new Intent(RCDActivity.this, TemplatesActivity.class);
                    intent.putExtra("catalogId", catalogId);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(RCDActivity.this, FlatsActivity.class);
                    intent.putExtra("blockId", flat.blockId);
                    startActivity(intent);
                }
            }
        });

    }
    private void setupListeners() {
        noRCDButton.setOnClickListener(v -> toggleRcdFields());

        setupInlineSave(manufacturerEditText, saveManufacturerButton, value -> {
            if (rcd != null) {
                rcd.name = value;
                rcdViewModel.update(rcd);
            }
        });
        setupInlineSave(time1EditText, saveTime1Button, value -> {
            if (rcd != null) {
                try {
                    rcd.time1 = Integer.parseInt(value);
                    rcdViewModel.update(rcd);
                } catch (NumberFormatException e) {
                    time1EditText.setError("Nieprawidłowa liczba");
                }
            }
        });
        setupInlineSave(time2EditText, saveTime2Button, value -> {
            if (rcd != null) {
                try {
                    rcd.time2 = Integer.parseInt(value);
                    rcdViewModel.update(rcd);
                } catch (NumberFormatException e) {
                    time2EditText.setError("Nieprawidłowa liczba");
                }
            }
        });

        rcdBrokenButton.setOnClickListener(v -> {
            if (rcd != null) {
                if (rcd.isGood == 0) {
                    rcd.isGood = 1;

                } else {
                    rcd.isGood = 0;

                }
                rcdViewModel.update(rcd);
            }
        });
        notesSaveButton.setVisibility(View.VISIBLE);
        notedEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        notedEditText.setSingleLine(false);
        notedEditText.setImeOptions(EditorInfo.IME_ACTION_NONE);

        notesSaveButton.setOnClickListener(v -> {
            if (rcd != null) {
                rcd.notes = notedEditText.getText().toString();
                rcdViewModel.update(rcd);
                hideKeyboard(notedEditText);
            }
        });

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"A", "AC"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);

        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isInitializing || rcd == null) return;
                String selectedType = (String) parent.getItemAtPosition(position);
                if (!selectedType.equals(rcd.type)) {
                    rcd.type = selectedType;
                    rcdViewModel.update(rcd);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void populateFields() {
            manufacturerEditText.setText(rcd.name);
            if (rcd.type != null) {
                ArrayAdapter<String> adapter = (ArrayAdapter<String>) typeSpinner.getAdapter();
                int position = adapter.getPosition(rcd.type);
                typeSpinner.setSelection(position);
            }
            time1EditText.setText(rcd.time1 != 0 ? String.valueOf(rcd.time1) : "");
            time2EditText.setText(rcd.time2 != 0 ? String.valueOf(rcd.time2) : "");
            notedEditText.setText(rcd.notes);




    }
    private void updateRCDBrokenButton() {
        if (flat != null) {
            boolean rcdIsGood = rcd.isGood == 1;
            if (rcdIsGood) {
                LinearLayout notes = findViewById(R.id.notes);
                LinearLayout headers = findViewById(R.id.headers);
                LinearLayout data = findViewById(R.id.data);
                notes.setVisibility(View.VISIBLE);
                headers.setVisibility(View.VISIBLE);
                data.setVisibility(View.VISIBLE);

                TextView brokenRCD = findViewById(R.id.brokenRCD);
                brokenRCD.setVisibility(View.GONE);

                rcdBrokenButton.setText("ZMIEŃ NA: RÓŻNICÓWKA NIESPRAWNA");
                rcdBrokenButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F44336")));

            } else {
                LinearLayout notes = findViewById(R.id.notes);
                LinearLayout headers = findViewById(R.id.headers);
                LinearLayout data = findViewById(R.id.data);
                notes.setVisibility(View.GONE);
                headers.setVisibility(View.GONE);
                data.setVisibility(View.GONE);

                TextView brokenRCD = findViewById(R.id.brokenRCD);
                brokenRCD.setVisibility(View.VISIBLE);
                rcdBrokenButton.setText("Zmień na: Różnicówka sprawna");
                rcdBrokenButton.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_green_dark));
            }
        }
    }
    private void updateNoRCDButtonState() {
        if (flat != null) {
            boolean hasRcd = flat.hasRCD == 1;
            if (hasRcd) {
                rcdFieldsContainer.setVisibility(View.VISIBLE);
                noRCDButton.setText("Brak różnicówki");
            } else {
                rcdFieldsContainer.setVisibility(View.GONE);
                noRCDButton.setText("Jednak jest różnicówka");
            }
        }
    }

    private void toggleRcdFields() {
        if (flat == null) return;
        boolean willHaveRcd = rcdFieldsContainer.getVisibility() == View.GONE;
        if (willHaveRcd) {
            rcdFieldsContainer.setVisibility(View.VISIBLE);
            noRCDButton.setText("Brak różnicówki");
            flat.hasRCD = 1;
        } else {
            rcdFieldsContainer.setVisibility(View.GONE);
            noRCDButton.setText("Jednak jest różnicówka");
            flat.hasRCD = 0;
        }
        flatViewModel.update(flat);
    }

    private void setupInlineSave(EditText editText, Button saveButton, java.util.function.Consumer<String> saveAction) {
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) editText.getLayoutParams();

// Usunięcie wszystkich marginesów
                params.setMargins(0, 0, 0, 0);  // left, top, right, bottom

// Lub tylko marginEnd
                params.setMarginEnd(0);

                editText.setLayoutParams(params);
                saveButton.setVisibility(View.VISIBLE);
            } else {
                // Don't hide if the save button is the next view to get focus
                if (getCurrentFocus() != saveButton) {
                    saveButton.setVisibility(View.GONE);
                }
            }
        });

        saveButton.setOnClickListener(v -> {
            String value = editText.getText().toString();
            saveAction.accept(value);
            saveButton.setVisibility(View.GONE);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) editText.getLayoutParams();

            final float scale = getResources().getDisplayMetrics().density;
            int marginInPx = (int) (20 * scale + 0.5f); // konwersja dp -> px
            params.setMarginEnd(marginInPx);

            editText.setLayoutParams(params);
            hideKeyboard(editText);
        });

        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveButton.performClick();
                return true;
            }
            return false;
        });
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        view.clearFocus();
    }
}
