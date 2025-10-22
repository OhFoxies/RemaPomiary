
package com.rejner.remapomiary.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.rejner.remapomiary.R;
import com.rejner.remapomiary.data.dao.RCDDao;
import com.rejner.remapomiary.data.entities.Circuit;
import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.data.entities.FlatFullData;
import com.rejner.remapomiary.data.entities.OutletMeasurement;
import com.rejner.remapomiary.data.entities.RCD;
import com.rejner.remapomiary.data.entities.RoomInFlat;
import com.rejner.remapomiary.data.entities.Template;
import com.rejner.remapomiary.data.utils.LiveDataUtil;
import com.rejner.remapomiary.ui.viewmodels.BlockViewModel;
import com.rejner.remapomiary.ui.viewmodels.CircuitViewModel;
import com.rejner.remapomiary.ui.viewmodels.FlatViewModel;
import com.rejner.remapomiary.ui.viewmodels.OutletMeasurementViewModel;
import com.rejner.remapomiary.ui.viewmodels.RCDViewModel;
import com.rejner.remapomiary.ui.viewmodels.RoomViewModel;
import com.rejner.remapomiary.ui.viewmodels.TemplateViewModel;

import java.util.Date;

public class NotesActivity extends AppCompatActivity {

    private FlatViewModel flatViewModel;
    private TemplateViewModel templateViewModel;
    private Flat currentFlat;
    private int flatId;
    private BlockViewModel blockViewModel;
    private CircuitViewModel circuitViewModel;
    private RCDViewModel rcdViewModel;
    private OutletMeasurementViewModel outletMeasurementViewModel;

    private RoomViewModel roomViewModel;

    private RadioGroup radioGroup;
    private RadioButton radioDopuszczona;
    private RadioButton radioDopuszczonaUsterki;
    private RadioButton radioNiedopuszczona;
    private EditText notesEditText;
    private TextView currentMode;
    private Button saveButton;
    private Button blockGrade;
    private EditText templateName;
    private Button templateSave;
    private int catalogId;
    private boolean areButtonsSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);


        flatId = getIntent().getIntExtra("flatId", -1);
        catalogId = getIntent().getIntExtra("catalogId", -1);
        if (flatId == -1) {
            Toast.makeText(this, "Nieprawidłowe ID mieszkania", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        radioGroup = findViewById(R.id.radioGroup_instalacja);
        radioDopuszczona = findViewById(R.id.radio_dopuszczona);
        radioDopuszczonaUsterki = findViewById(R.id.radio_dopuszczona_usterki);
        radioNiedopuszczona = findViewById(R.id.radio_niedopuszczona);
        notesEditText = findViewById(R.id.notedEditText);
        saveButton = findViewById(R.id.notesSave);
        blockGrade = findViewById(R.id.blockGrade);
        currentMode = findViewById(R.id.currentMode);
        templateSave = findViewById(R.id.templateSave);
        templateName = findViewById(R.id.templateName);

        if (catalogId != -1) {
            radioDopuszczona.setEnabled(false);
            radioDopuszczonaUsterki.setEnabled(false);
            radioNiedopuszczona.setEnabled(false);
            notesEditText.setEnabled(false);
            saveButton.setEnabled(false);
            blockGrade.setEnabled(false);
            templateName.setEnabled(false);
            templateSave.setEnabled(false);
        }
        flatViewModel = new ViewModelProvider(this).get(FlatViewModel.class);
        templateViewModel = new ViewModelProvider(this).get(TemplateViewModel.class);
        blockViewModel = new ViewModelProvider(this).get(BlockViewModel.class);
        circuitViewModel = new ViewModelProvider(this).get(CircuitViewModel.class);
        rcdViewModel = new ViewModelProvider(this).get(RCDViewModel.class);
        roomViewModel = new ViewModelProvider(this).get(RoomViewModel.class);
        outletMeasurementViewModel = new ViewModelProvider(this).get(OutletMeasurementViewModel.class);

        // Main observer - single source of truth for UI updates
        flatViewModel.getCombinedFlat(flatId).observe(this, flat -> {
            if (flat != null) {
                currentFlat = flat;
                notesEditText.setText(flat.notes);
                setGradeSelection();
                gradeButtonState();
            }
            if (!areButtonsSet) {
                setupUIElements();
                areButtonsSet = true;
            }
        });

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (currentFlat == null) return;

            int newGrade = currentFlat.grade;
            if (checkedId == R.id.radio_dopuszczona) {
                newGrade = 0;
            } else if (checkedId == R.id.radio_dopuszczona_usterki) {
                newGrade = 1;
            } else if (checkedId == R.id.radio_niedopuszczona) {
                newGrade = 2;
            }

            if (currentFlat.grade != newGrade) {
                currentFlat.grade = newGrade;
                flatViewModel.update(currentFlat);
            }
        });

        blockGrade.setOnClickListener(v -> {
            if (currentFlat == null) return;

            flatViewModel.toggleGradeBlock(currentFlat.id); // Załóżmy, że Flat ma metodę getId()

        });

        saveButton.setOnClickListener(v -> {
            if (currentFlat != null) {
                currentFlat.notes = notesEditText.getText().toString();
                hideKeyboard();
                notesEditText.clearFocus();
                flatViewModel.update(currentFlat);
            }
        });

        templateName = findViewById(R.id.templateName);

        templateSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAsTemplate();
            }
        });
    }

    private void saveAsTemplate() {
        String name = templateName.getText().toString();
        if (name.isEmpty()) {
            templateName.setError("Podaj nazwe");
            Toast.makeText(NotesActivity.this, "Nie podano nazwy", Toast.LENGTH_SHORT).show();
            return;
        }
        blockViewModel.getBlockById(currentFlat.blockId, block -> {
            templateViewModel.doesTemplateNameExists(name, block.catalog.id, doesExist -> {
                if (doesExist) {
                    // Must run on the Main Thread for UI (Toast)
                    runOnUiThread(() -> {
                        Toast.makeText(NotesActivity.this, "Szablon z tą nazwą już istnieje", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                Flat templateFlat = new Flat();
                templateFlat.hasRCD = currentFlat.hasRCD;
                templateFlat.isTemplate = 1;
                templateFlat.type = currentFlat.type;
                templateFlat.number = "MIESZKANIE SZABLONOWE";
                templateFlat.blockId = currentFlat.blockId;
                flatViewModel.insertWithId(templateFlat, id -> {
                    // This callback runs on a background thread (where the DB operation finished).

                    // 1. **Perform non-UI/non-LiveData operations on the background thread:**
                    Template template = new Template();
                    template.creationDate = new Date();
                    template.flatId = Math.toIntExact(id);
                    template.name = name;
                    templateViewModel.insert(template);

                    // 2. **Switch to the Main Thread for LiveData observation and UI updates:**
                    NotesActivity.this.runOnUiThread(() -> {
                        // Now we are on the Main Thread.

                        // Observe Circuits
                        LiveDataUtil.observeOnce(circuitViewModel.getCircuitsForFlat(currentFlat.id), NotesActivity.this, circuits -> {
                            for (Circuit c : circuits) {
                                Circuit newCircuit = new Circuit();
                                newCircuit.flatId = Math.toIntExact(id);
                                newCircuit.name = c.name;
                                newCircuit.type = c.type;
                                circuitViewModel.insert(newCircuit);
                            }
                        });

                        // Observe RCDs
                        if (templateFlat.hasRCD == 1) {
                            LiveDataUtil.observeOnce(rcdViewModel.getRcdsForFlat(currentFlat.id), NotesActivity.this, rcds -> {
                                for (RCD r : rcds) {
                                    RCD newRcd = new RCD();
                                    newRcd.name = r.name;
                                    newRcd.flatId = Math.toIntExact(id);
                                    newRcd.type = r.type;
                                    rcdViewModel.insert(newRcd);
                                }
                            });
                        }

                        // Observe Rooms and Outlet Measurements
                        LiveDataUtil.observeOnce(roomViewModel.getRoomsForFlat(currentFlat.id), NotesActivity.this, roomInFlats -> {
                            for (RoomInFlat r : roomInFlats) {
                                RoomInFlat room = new RoomInFlat();
                                room.name = r.name;
                                room.flatId = Math.toIntExact(id);
                                roomViewModel.insertWithId(room, roomId -> {
                                    NotesActivity.this.runOnUiThread(() -> {
                                        LiveDataUtil.observeOnce(outletMeasurementViewModel.getMeasurementsForRoom(r.id), NotesActivity.this, outletMeasurements -> {
                                            for (OutletMeasurement o : outletMeasurements) {
                                                OutletMeasurement newOutlet = new OutletMeasurement();
                                                newOutlet.amps = o.amps;

                                                newOutlet.appliance = o.appliance;

                                                newOutlet.breakerType = o.breakerType;

                                                newOutlet.switchName = o.switchName;
                                                newOutlet.roomId = Math.toIntExact(roomId);
                                                outletMeasurementViewModel.insert(newOutlet, x -> {
                                                });
                                            }
                                        });
                                    });
                                });
                            }

                            // UI updates (Toast, hideKeyboard, clearFocus) must be on the main thread:
                            Toast.makeText(NotesActivity.this, "Szablon został zapisany", Toast.LENGTH_SHORT).show();
                            templateName.setText("");
                            hideKeyboard();
                            templateName.clearFocus();
                        });
                    });
                });
            });
        });


    }

    private void setupUIElements() {
        Button backButton = findViewById(R.id.backButton);
        Button roomButton = findViewById(R.id.roomsButton);
        Button RCDButton = findViewById(R.id.RCDButton);
        Button boardButton = findViewById(R.id.boardButton);
        TextView titleView = findViewById(R.id.rcdTitle);
        titleView.setText("Mieszkanie numer - " + currentFlat.number + " podsumowanie");
        Button backSave = findViewById(R.id.backSave);

        backSave.setOnClickListener(v -> {
            currentFlat.status = "Pomiar gotowy ✅";
            currentFlat.edition_date = new Date();
            flatViewModel.update(currentFlat);
            Intent intent = new Intent(NotesActivity.this, FlatsActivity.class);
            intent.putExtra("blockId", currentFlat.blockId);
            startActivity(intent);
        });

        boardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NotesActivity.this, BoardActivity.class);
                if (catalogId != -1) {
                    intent.putExtra("catalogId", catalogId);

                }
                intent.putExtra("flatId", currentFlat.id);
                startActivity(intent);
            }
        });

        roomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NotesActivity.this, RoomActivity.class);
                if (catalogId != -1) {
                    intent.putExtra("catalogId", catalogId);

                }
                intent.putExtra("flatId", currentFlat.id);
                startActivity(intent);
            }
        });


        RCDButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NotesActivity.this, RCDActivity.class);
                if (catalogId != -1) {
                    intent.putExtra("catalogId", catalogId);

                }
                intent.putExtra("flatId", currentFlat.id);
                startActivity(intent);
            }
        });


        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentFlat == null) return;
                if (catalogId != -1) {
                    Intent intent = new Intent(NotesActivity.this, CatalogActivity.class);
                    intent.putExtra("catalogId", catalogId);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(NotesActivity.this, FlatsActivity.class);
                    intent.putExtra("blockId", currentFlat.blockId);
                    startActivity(intent);
                }
            }
        });

    }

    private void gradeButtonState() {
        if (currentFlat != null) {
            if (currentFlat.gradeByUser == 0) {
                currentMode.setText("Automatyczna aktualizacja włączona!");
                blockGrade.setText("Wyłącz automatyczną aktualizacje");
                blockGrade.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F44336")));
            } else {
                currentMode.setText("Automatyczna aktualizacja wyłączona!");

                blockGrade.setText("Włącz automatyczną aktualizacje");
                blockGrade.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ff669900")));
            }
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view == null) {
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void setGradeSelection() {
        if (currentFlat != null) {
            if (currentFlat.grade == 0) {
                radioDopuszczona.setChecked(true);
            } else if (currentFlat.grade == 1) {
                radioDopuszczonaUsterki.setChecked(true);
            } else if (currentFlat.grade == 2) {
                radioNiedopuszczona.setChecked(true);
            }
        }
    }
}