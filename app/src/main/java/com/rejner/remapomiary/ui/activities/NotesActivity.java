package com.rejner.remapomiary.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.rejner.remapomiary.R;
import com.rejner.remapomiary.data.entities.Circuit;
import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.data.entities.OutletMeasurement;
import com.rejner.remapomiary.data.entities.RCD;
import com.rejner.remapomiary.data.entities.RoomInFlat;
import com.rejner.remapomiary.data.entities.Signature;
import com.rejner.remapomiary.data.entities.Template;
import com.rejner.remapomiary.data.utils.LiveDataUtil;
import com.rejner.remapomiary.ui.utils.Actions;
import com.rejner.remapomiary.ui.viewmodels.BlockViewModel;
import com.rejner.remapomiary.ui.viewmodels.CircuitViewModel;
import com.rejner.remapomiary.ui.viewmodels.FlatViewModel;
import com.rejner.remapomiary.ui.viewmodels.OutletMeasurementViewModel;
import com.rejner.remapomiary.ui.viewmodels.RCDViewModel;
import com.rejner.remapomiary.ui.viewmodels.RoomViewModel;
import com.rejner.remapomiary.ui.viewmodels.TemplateViewModel;
import com.rejner.remapomiary.ui.viewmodels.SignatureViewModel; // NOWE
import com.rejner.remapomiary.ui.activities.SignatureView; // NOWE

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.util.Date;

public class NotesActivity extends AppCompatActivity {

    private FlatViewModel flatViewModel;
    private TemplateViewModel templateViewModel;
    private SignatureViewModel signatureViewModel; // NOWE

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
    private EditText notes2EditText;
    private TextView currentMode;
    private Button saveButton;
    private Button notes2Save;
    private Button blockGrade;
    private EditText templateName;
    private Button templateSave;
    private int catalogId;
    private boolean areButtonsSet = false;

    // NOWE POLA DLA PODPISU
    private LinearLayout termsContainer;
    private LinearLayout signaturePadContainer;
    private LinearLayout savedSignatureContainer;
    private Button btnNextToSignature;
    private Button btnSaveSignature;
    private SignatureView signatureView;
    private ImageView savedSignatureImage;
    private EditText etSignerName;
    private Button saveName;
    private Button btnCancelSignature;
    private Button btnClearSignature;
    private Button btnShowTermsPostSign;
    private Button btnDeleteSignature;
    private TextView tvSavedSignatureInfo;
    private int isCommonSpace;
    private String blockName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        flatId = getIntent().getIntExtra("flatId", -1);
        catalogId = getIntent().getIntExtra("catalogId", -1);
        isCommonSpace = getIntent().getIntExtra("isCommonSpace", 0);
        blockName = getIntent().getStringExtra("name");
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
        notes2EditText = findViewById(R.id.notes2EditText);
        saveButton = findViewById(R.id.notesSave);
        notes2Save = findViewById(R.id.notes2Save);
        blockGrade = findViewById(R.id.blockGrade);
        currentMode = findViewById(R.id.currentMode);
        templateSave = findViewById(R.id.templateSave);
        templateName = findViewById(R.id.templateName);

        etSignerName = findViewById(R.id.etSignerName);
        btnCancelSignature = findViewById(R.id.btnCancelSignature);
        btnClearSignature = findViewById(R.id.btnClearSignature);
        btnShowTermsPostSign = findViewById(R.id.btnShowTermsPostSign);
        btnDeleteSignature = findViewById(R.id.btnDeleteSignature);
        tvSavedSignatureInfo = findViewById(R.id.tvSavedSignatureInfo);

        // NOWE WIDOKI
        termsContainer = findViewById(R.id.termsContainer);
        signaturePadContainer = findViewById(R.id.signaturePadContainer);
        savedSignatureContainer = findViewById(R.id.savedSignatureContainer);
        btnNextToSignature = findViewById(R.id.btnNextToSignature);
        btnSaveSignature = findViewById(R.id.btnSaveSignature);
        signatureView = findViewById(R.id.signatureView);
        savedSignatureImage = findViewById(R.id.savedSignatureImage);
        saveName = findViewById(R.id.confirmName);


        if (catalogId != -1) {
            radioDopuszczona.setEnabled(false);
            radioDopuszczonaUsterki.setEnabled(false);
            radioNiedopuszczona.setEnabled(false);
            notesEditText.setEnabled(false);
            notes2EditText.setEnabled(false);
            saveButton.setEnabled(false);
            notes2Save.setEnabled(false);
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
        signatureViewModel = new ViewModelProvider(this).get(SignatureViewModel.class); // INICJALIZACJA
        if (isCommonSpace == 1) {
            termsContainer.setVisibility(View.GONE);
            findViewById(R.id.other).setVisibility(View.GONE);
            signaturePadContainer.setVisibility(View.GONE);
            savedSignatureContainer.setVisibility(View.GONE);


            ((TextView) findViewById(R.id.notesTitle)).setText("Uwagi do części wspólnej (niewidoczne w protokole)");
            ((TextView) findViewById(R.id.notes2Title)).setText("Uwagi do części wspólnej (WIDOCZNE W PROTOKOLE)");

            ((TextView) findViewById(R.id.descNotes)).setText("Miejsce na dowolne informacje - braki, plany na następny pomiar itd. To co tutaj wpiszesz nie pojawi się w protokole.");

            findViewById(R.id.other).setVisibility(View.GONE);

        }
        flatViewModel.getCombinedFlat(flatId).observe(this, flat -> {
            if (flat != null) {
                currentFlat = flat;
                notesEditText.setText(flat.notes);
                notes2EditText.setText(flat.notesProtocol);
                setGradeSelection();
                gradeButtonState();
            }
            if (!areButtonsSet) {
                setupUIElements();
                areButtonsSet = true;
            }
        });

        // OBSŁUGA LOGIKI PODPISÓW
        if (isCommonSpace != 1) {
            setupSignatureLogic();

        }

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
            flatViewModel.toggleGradeBlock(currentFlat.id);
        });

        saveButton.setOnClickListener(v -> {
            if (currentFlat != null) {
                currentFlat.notes = notesEditText.getText().toString();
                hideKeyboard();
                notesEditText.clearFocus();
                flatViewModel.update(currentFlat);
                Toast.makeText(this, "Zapisano uwagi", Toast.LENGTH_SHORT).show();
            }
        });

        notes2Save.setOnClickListener(v -> {
            if (currentFlat != null) {
                currentFlat.notesProtocol = notes2EditText.getText().toString();
                hideKeyboard();
                notes2EditText.clearFocus();
                flatViewModel.update(currentFlat);
                Toast.makeText(this, "Zapisano uwagi do protokołu", Toast.LENGTH_SHORT).show();
            }
        });

        View.OnFocusChangeListener focusChangeListener = (v, hasFocus) -> {
            if (!hasFocus && currentFlat != null) {
                currentFlat.notes = notesEditText.getText().toString();
                currentFlat.notesProtocol = notes2EditText.getText().toString();
                flatViewModel.update(currentFlat);
                Toast.makeText(this, "Zapisano zmiany", Toast.LENGTH_SHORT).show();
            }
        };

        notesEditText.setOnFocusChangeListener(focusChangeListener);
        notes2EditText.setOnFocusChangeListener(focusChangeListener);

        templateSave.setOnClickListener(v -> saveAsTemplate());

    }

    // NOWA METODA - Logika widoczności i zapisu podpisu
    private void setupSignatureLogic() {
        saveName.setOnClickListener(v-> {
            hideKeyboard();
            etSignerName.clearFocus();
        });
        etSignerName.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard();          // Ukrywa klawiaturę (korzysta z Twojej metody)
                etSignerName.clearFocus(); // Usuwa focus (kursor) z pola tekstowego
                return true;             // Konsumuje zdarzenie
            }
            return false;
        });
        signatureViewModel.getSignatureForFlat(flatId).observe(this, signature -> {
            if (signature != null && signature.signatureData != null) {
                // Podpis istnieje w bazie - ukryj panele wprowadzania, pokaż podgląd
                termsContainer.setVisibility(View.GONE);
                signaturePadContainer.setVisibility(View.GONE);
                savedSignatureContainer.setVisibility(View.VISIBLE);

                // Wyświetlenie danych tekstowych (Kto i Kiedy)
                String dateStr = signature.signatureDate != null ?
                        android.text.format.DateFormat.format("yyyy-MM-dd HH:mm", signature.signatureDate).toString() : "-";
                tvSavedSignatureInfo.setText("Podpis złożony przez: " + signature.signerName + "\nData: " + dateStr);

                // Wyświetlenie rysunku podpisu
                Bitmap bitmap = BitmapFactory.decodeByteArray(signature.signatureData, 0, signature.signatureData.length);
                savedSignatureImage.setImageBitmap(bitmap);
            } else {
                // Brak podpisu - pokaż panel startowy (Regulamin)
                if (catalogId == -1) {
                    termsContainer.setVisibility(View.VISIBLE);
                    signaturePadContainer.setVisibility(View.GONE);
                    savedSignatureContainer.setVisibility(View.GONE);
                    // Czyszczenie pól tekstowych i padu rysowania na wszelki wypadek
                    etSignerName.setText("");
                    signatureView.clear();
                }
            }
        });

        // Przejście z regulaminu do podpisywania
        btnNextToSignature.setOnClickListener(v -> {
            termsContainer.setVisibility(View.GONE);
            signaturePadContainer.setVisibility(View.VISIBLE);
        });

        // Przycisk: ANULUJ składanie podpisu (wraca do regulaminu)
        btnCancelSignature.setOnClickListener(v -> {
            hideKeyboard();
            signatureView.clear();
            etSignerName.setText("");
            signaturePadContainer.setVisibility(View.GONE);
            termsContainer.setVisibility(View.VISIBLE);
        });

        // Przycisk: WYCZYŚĆ pad (czyści tylko płótno rysunku w trakcie podpisywania)
        btnClearSignature.setOnClickListener(v -> {
            signatureView.clear();
        });

        // Przycisk: ZAPISZ podpis w bazie
        btnSaveSignature.setOnClickListener(v -> {
            String name = etSignerName.getText().toString().trim();
            if (name.isEmpty()) {
                etSignerName.setError("Wprowadź imię i nazwisko!");
                return;
            }

            Bitmap signatureBitmap = signatureView.getSignatureBitmap();

            // Kompresja grafiki bitmapy do tablicy bajtów
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            signatureBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();

            // Budowanie nowego obiektu z datą i nazwiskiem
            Signature newSignature = new Signature();
            newSignature.flatId = flatId;
            newSignature.signatureData = byteArray;
            newSignature.signerName = name;
            newSignature.signatureDate = new Date(); // Bieżąca data i czas

            hideKeyboard();
            signatureViewModel.insert(newSignature);
            Toast.makeText(this, "Podpis został pomyślnie zapisany!", Toast.LENGTH_SHORT).show();
        });

        // Przycisk: POKAŻ REGULAMIN po złożeniu podpisu (wyświetla Dialog)
        btnShowTermsPostSign.setOnClickListener(v -> {
            TextView termsTv = findViewById(R.id.tvTermsText);
            CharSequence termsContent = termsTv != null ? termsTv.getText() : "Treść regulaminu";

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Regulamin")
                    .setMessage(termsContent)
                    .setPositiveButton("Zamknij", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        // Przycisk: PODPISZ PONOWNIE / USUŃ (Kasuje stary podpis i pozwala zacząć od nowa)
        btnDeleteSignature.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Usuwanie podpisu")
                    .setMessage("Czy na pewno chcesz usunąć obecny podpis i podpisać dokument ponownie?")
                    .setPositiveButton("Tak, usuń", (dialog, which) -> {
                signatureViewModel.deleteSignatureForFlat(flatId);
                Toast.makeText(this, "Stary podpis usunięty.", Toast.LENGTH_SHORT).show();
            })
                    .setNegativeButton("Nie", null)
                    .show();
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
                    runOnUiThread(() -> Toast.makeText(NotesActivity.this, "Szablon z tą nazwą już istnieje", Toast.LENGTH_SHORT).show());
                    return;
                }
                Flat templateFlat = new Flat();
                templateFlat.hasRCD = currentFlat.hasRCD;
                templateFlat.isTemplate = 1;
                templateFlat.type = currentFlat.type;
                templateFlat.number = "MIESZKANIE SZABLONOWE";
                templateFlat.blockId = currentFlat.blockId;
                flatViewModel.insertWithId(templateFlat, id -> {
                    Template template = new Template();
                    template.creationDate = new Date();
                    template.flatId = Math.toIntExact(id);
                    template.name = name;
                    templateViewModel.insert(template);

                    NotesActivity.this.runOnUiThread(() -> {
                        LiveDataUtil.observeOnce(circuitViewModel.getCircuitsForFlat(currentFlat.id), NotesActivity.this, circuits -> {
                            for (Circuit c : circuits) {
                                Circuit newCircuit = new Circuit();
                                newCircuit.flatId = Math.toIntExact(id);
                                newCircuit.name = c.name;
                                newCircuit.type = c.type;
                                circuitViewModel.insert(newCircuit);
                            }
                        });

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
                                                outletMeasurementViewModel.insert(newOutlet, x -> {});
                                            }
                                        });
                                    });
                                });
                            }
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
        if (isCommonSpace == 1) {
            titleView.setText("Podsumowanie - " + blockName);

        }
        Button backSave = findViewById(R.id.backSave);
        if (isCommonSpace == 1) {
            backSave.setVisibility(View.GONE);
            RCDButton.setVisibility(View.GONE);
        }
        backSave.setOnClickListener(v -> {
            Actions.saveAndMarkReady(currentFlat, this);

            Intent intent = new Intent(NotesActivity.this, FlatsActivity.class);
            intent.putExtra("blockId", currentFlat.blockId);
            startActivity(intent);
        });

        boardButton.setOnClickListener(v -> {
            if (isCommonSpace == 1) {
                Intent intent = new Intent(NotesActivity.this, BoardCommonSpace.class);
                intent.putExtra("isCommonSpace", 1);
                intent.putExtra("blockId", currentFlat.blockId);
                intent.putExtra("flatId", flatId);
                startActivity(intent);
                return;
            }
            Intent intent = new Intent(NotesActivity.this, BoardActivity.class);
            if (catalogId != -1) intent.putExtra("catalogId", catalogId);
            intent.putExtra("flatId", currentFlat.id);
            startActivity(intent);

        });

        roomButton.setOnClickListener(v -> {
            if (isCommonSpace == 1) {
                Intent intent = new Intent(NotesActivity.this, RoomActivity.class);
                intent.putExtra("isCommonSpace", 1);
                intent.putExtra("name", blockName);
                intent.putExtra("flatId", flatId);
                startActivity(intent);
                return;
            }
            Intent intent = new Intent(NotesActivity.this, RoomActivity.class);
            if (catalogId != -1) intent.putExtra("catalogId", catalogId);
            intent.putExtra("flatId", currentFlat.id);
            startActivity(intent);
        });


        RCDButton.setOnClickListener(v -> {
            Intent intent = new Intent(NotesActivity.this, RCDActivity.class);
            if (catalogId != -1) intent.putExtra("catalogId", catalogId);
            intent.putExtra("flatId", currentFlat.id);
            startActivity(intent);
        });

        backButton.setOnClickListener(v -> {
            if (isCommonSpace == 1) {
                Intent intent = new Intent(this, BlockActivity.class);
                intent.putExtra("blockId", currentFlat.blockId);
                startActivity(intent);
            }
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