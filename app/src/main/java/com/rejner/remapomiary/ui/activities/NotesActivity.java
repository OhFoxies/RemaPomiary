package com.rejner.remapomiary.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.rejner.remapomiary.R;
import com.rejner.remapomiary.data.entities.Circuit;
import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.data.entities.FlatPhoto;
import com.rejner.remapomiary.data.entities.OutletMeasurement;
import com.rejner.remapomiary.data.entities.RCD;
import com.rejner.remapomiary.data.entities.RoomInFlat;
import com.rejner.remapomiary.data.entities.Signature;
import com.rejner.remapomiary.data.entities.Template;
import com.rejner.remapomiary.data.utils.LiveDataUtil;
import com.rejner.remapomiary.ui.utils.Actions;
import com.rejner.remapomiary.ui.utils.LegalTexts;
import com.rejner.remapomiary.ui.viewmodels.BlockViewModel;
import com.rejner.remapomiary.ui.viewmodels.CircuitViewModel;
import com.rejner.remapomiary.ui.viewmodels.FlatViewModel;
import com.rejner.remapomiary.ui.viewmodels.OutletMeasurementViewModel;
import com.rejner.remapomiary.ui.viewmodels.RCDViewModel;
import com.rejner.remapomiary.ui.viewmodels.RoomViewModel;
import com.rejner.remapomiary.ui.viewmodels.TemplateViewModel;
import com.rejner.remapomiary.ui.viewmodels.SignatureViewModel;
import com.rejner.remapomiary.ui.viewmodels.FlatPhotoViewModel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

public class NotesActivity extends AppCompatActivity {

    private FlatViewModel flatViewModel;
    private TemplateViewModel templateViewModel;
    private SignatureViewModel signatureViewModel;

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
    private CheckBox cbRefusedInspection;
    private TextView currentMode;
    private Button saveButton;
    private Button notes2Save;
    private Button blockGrade;
    private EditText templateName;
    private Button templateSave;
    private int catalogId;
    private boolean areButtonsSet = false;
    private boolean isSignatureLogicSetup = false;

    // POLA DLA PODPISU
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

    // NOWE POLA DLA SYSTEMU ZDJĘĆ Z OPISAMI (TABELA)
    private FlatPhotoViewModel flatPhotoViewModel;
    private ActivityResultLauncher<Uri> takeNotesPhotoLauncher;
    private File tempNotesPhotoFile;
    private boolean isNotesPhotosExpanded = false;
    private LinearLayout notesPhotosContainer;
    private HorizontalScrollView notesPhotosScrollView;
    private Button toggleNotesPhotosButton, addNotesPhotoBtn;
    private float dpScale;
    private TextView textViewRefused;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        flatId = getIntent().getIntExtra("flatId", -1);
        catalogId = getIntent().getIntExtra("catalogId", -1);
        isCommonSpace = getIntent().getIntExtra("commonSpace", 0);
        blockName = getIntent().getStringExtra("name");
        dpScale = getResources().getDisplayMetrics().density;

        if (flatId == -1) {
            Toast.makeText(this, "Nieprawidłowe ID mieszkania", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        radioGroup = findViewById(R.id.radioGroup_instalacja);
        radioDopuszczona = findViewById(R.id.radio_dopuszczona);
        radioDopuszczonaUsterki = findViewById(R.id.radio_dopuszczona_usterki);
        radioNiedopuszczona = findViewById(R.id.radio_niedopuszczona);
        cbRefusedInspection = findViewById(R.id.cbRefusedInspection);
        textViewRefused = findViewById(R.id.textViewRefused);
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

        TextView tvTermsText = findViewById(R.id.tvTermsText);
        if (tvTermsText != null) {
            tvTermsText.setText(LegalTexts.SIGNATURE_TERMS);
        }

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
            cbRefusedInspection.setEnabled(false);
        }

        flatViewModel = new ViewModelProvider(this).get(FlatViewModel.class);
        templateViewModel = new ViewModelProvider(this).get(TemplateViewModel.class);
        blockViewModel = new ViewModelProvider(this).get(BlockViewModel.class);
        circuitViewModel = new ViewModelProvider(this).get(CircuitViewModel.class);
        rcdViewModel = new ViewModelProvider(this).get(RCDViewModel.class);
        roomViewModel = new ViewModelProvider(this).get(RoomViewModel.class);
        outletMeasurementViewModel = new ViewModelProvider(this).get(OutletMeasurementViewModel.class);
        signatureViewModel = new ViewModelProvider(this).get(SignatureViewModel.class);
        flatPhotoViewModel = new ViewModelProvider(this).get(FlatPhotoViewModel.class);

        flatViewModel.getCombinedFlat(flatId).observe(this, flat -> {
            if (flat != null) {
                currentFlat = flat;
                notesEditText.setText(flat.notes);
                notes2EditText.setText(flat.notesProtocol);
                cbRefusedInspection.setChecked(flat.refusedInspection == 1);
                updateTermsText();
                setGradeSelection();
                gradeButtonState();

                blockViewModel.getBlockById(flat.blockId, b -> {
                    if (b == null || b.block == null) return;
                    runOnUiThread(() -> {
                        boolean isHouse = b.block.buildingType == 1;

                        if (isCommonSpace == 1) {
                            if (isHouse) {

                                ((TextView) findViewById(R.id.notesTitle)).setText("Uwagi do domu (niewidoczne w protokole)");
                                ((TextView) findViewById(R.id.notes2Title)).setText("Uwagi do domu (WIDOCZNE W PROTOKOLE)");
                                ((TextView) findViewById(R.id.descNotes)).setText("Miejsce na dowolne informacje - braki, plany na następny pomiar itd. To co tutaj wpiszesz nie pojawi się w protokole.");
                                findViewById(R.id.other).setVisibility(View.VISIBLE);

                                if (!isSignatureLogicSetup) {
                                    setupSignatureLogic();
                                    isSignatureLogicSetup = true;
                                }
                            } else {
                                cbRefusedInspection.setVisibility(View.GONE);
                                textViewRefused.setVisibility(View.GONE);
                                ((TextView) findViewById(R.id.notesTitle)).setText("Uwagi do części wspólnej (niewidoczne w protokole)");
                                ((TextView) findViewById(R.id.notes2Title)).setText("Uwagi do części wspólnej (WIDOCZNE W PROTOKOLE)");
                                ((TextView) findViewById(R.id.descNotes)).setText("Miejsce na dowolne informacje - braki, plany na następny pomiar itd. To co tutaj wpiszesz nie pojawi się w protokole.");

                                findViewById(R.id.other).setVisibility(View.GONE);
                                termsContainer.setVisibility(View.GONE);
                                signaturePadContainer.setVisibility(View.GONE);
                                savedSignatureContainer.setVisibility(View.GONE);
                            }
                        } else {
                            // Mieszkanie
                            ((TextView) findViewById(R.id.notesTitle)).setText("Uwagi do mieszkania (niewidoczne w protokole)");
                            ((TextView) findViewById(R.id.notes2Title)).setText("Uwagi do mieszkania (WIDOCZNE W PROTOKOLE)");
                            findViewById(R.id.other).setVisibility(View.VISIBLE);

                            if (!isSignatureLogicSetup) {
                                setupSignatureLogic();
                                isSignatureLogicSetup = true;
                            }
                        }
                    });
                });
            }
            if (!areButtonsSet) {
                setupUIElements();
                areButtonsSet = true;
            }
        });

        cbRefusedInspection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentFlat == null) return;
            int refused = isChecked ? 1 : 0;
            if (currentFlat.refusedInspection != refused) {
                currentFlat.refusedInspection = refused;
                flatViewModel.update(currentFlat);
                updateTermsText();
            }

            if (refused == 1) {
                Actions.saveAndMarkReady(currentFlat, NotesActivity.this);
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

        // Inicjalizacja funkcjonalności zdjęć uwag
        initNotesPhotosSystem();
    }

    // SYSTEM ZDJĘĆ Z ZAPISYWANIEM I EDYCJĄ OPISU
    private void initNotesPhotosSystem() {
        toggleNotesPhotosButton = findViewById(R.id.toggleNotesPhotosButton);
        addNotesPhotoBtn = findViewById(R.id.addNotesPhotoBtn);
        notesPhotosScrollView = findViewById(R.id.notesPhotosScrollView);
        notesPhotosContainer = findViewById(R.id.notesPhotosContainer);

        toggleNotesPhotosButton.setOnClickListener(v -> {
            isNotesPhotosExpanded = !isNotesPhotosExpanded;
            notesPhotosScrollView.setVisibility(isNotesPhotosExpanded ? View.VISIBLE : View.GONE);
            toggleNotesPhotosButton.setText(isNotesPhotosExpanded ? "Ukryj zdjęcia" : "Pokaż zdjęcia");
        });

        takeNotesPhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && tempNotesPhotoFile != null) {
                        FlatPhoto photo = new FlatPhoto();
                        photo.flatId = flatId;
                        photo.photoPath = tempNotesPhotoFile.getAbsolutePath();
                        photo.type = 1; // Typ 1 oznacza sekcję uwag/usterek
                        photo.description = "";

                        flatPhotoViewModel.insert(photo);
                        Toast.makeText(this, "Dodano zdjęcie usterki", Toast.LENGTH_SHORT).show();
                    }
                    tempNotesPhotoFile = null;
                }
        );

        addNotesPhotoBtn.setOnClickListener(v -> {
            if (catalogId != -1) return;
            try {
                String fileName = "MIESZKANIE_NOTES_" + flatId + "_" + System.currentTimeMillis();
                File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                tempNotesPhotoFile = File.createTempFile(fileName, ".jpg", storageDir);

                Uri photoURI = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".fileprovider",
                        tempNotesPhotoFile
                );
                takeNotesPhotoLauncher.launch(photoURI);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Błąd uruchamiania aparatu", Toast.LENGTH_SHORT).show();
            }
        });

        // Obserwacja bazy z filtrowaniem na uwagi (typ = 1)
        flatPhotoViewModel.getPhotosByFlatAndType(flatId, 1).observe(this, this::renderNotesPhotos);
    }

    private void renderNotesPhotos(List<FlatPhoto> photos) {
        notesPhotosContainer.removeAllViews();
        if (photos != null && !photos.isEmpty()) {
            for (FlatPhoto photo : photos) {
                // Karta usterki (pionowy kontener grupujący widoki)
                LinearLayout cardLayout = new LinearLayout(this);
                cardLayout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        (int) (220 * dpScale), ViewGroup.LayoutParams.WRAP_CONTENT);
                cardParams.setMargins(0, 0, (int) (14 * dpScale), 0);
                cardLayout.setLayoutParams(cardParams);
                cardLayout.setPadding((int) (4 * dpScale), (int) (4 * dpScale), (int) (4 * dpScale), (int) (4 * dpScale));
                cardLayout.setBackground(ContextCompat.getDrawable(this, R.drawable.circuit_row_background));

                // Podgląd dużego zdjęcia (200x200dp w kwadracie, reszta karty na formularz opisu)
                ImageView imageView = new ImageView(this);
                LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, (int) (200 * dpScale));
                imageView.setLayoutParams(imgParams);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setImageURI(Uri.fromFile(new File(photo.photoPath)));
                cardLayout.addView(imageView);

                // Pole wprowadzania opisu usterki do protokołu
                EditText descEditText = new EditText(this);
                LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                etParams.setMargins(0, (int) (8 * dpScale), 0, (int) (6 * dpScale));
                descEditText.setLayoutParams(etParams);
                descEditText.setHint("Opis usterki do protokołu...");
                descEditText.setText(photo.description != null ? photo.description : "");
                descEditText.setTextSize(14);

// =======================================================================
// ZMIANY ZWIĘKSZAJĄCE POLE:
// 1. Włączamy obsługę wielu linii tekstu
                descEditText.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
// 2. Ustalamy minimalną wysokość pola na 3 linie (możesz zmienić na 4 lub 5, zależnie od potrzeb)
                descEditText.setMinLines(3);
// 3. Wyrównujemy tekst i hint do lewego górnego rogu (żeby tekst nie zaczynał się na środku pola)
                descEditText.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
// =======================================================================

                descEditText.setBackground(ContextCompat.getDrawable(this, R.drawable.input));
// Zwiększyłem też delikatnie padding wewnętrzny (z 8 na 12), żeby tekst nie był "przyklejony" do ramek
                descEditText.setPadding((int) (12 * dpScale), (int) (12 * dpScale), (int) (12 * dpScale), (int) (12 * dpScale));

                if (catalogId != -1) descEditText.setEnabled(false);
                cardLayout.addView(descEditText);

                // Przycisk Zapisu Opisu
                Button saveDescBtn = new Button(this);
                LinearLayout.LayoutParams btnDescParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, (int) (42 * dpScale));
                btnDescParams.setMargins(0, 0, 0, (int) (6 * dpScale));
                saveDescBtn.setLayoutParams(btnDescParams);
                saveDescBtn.setText("ZAPISZ OPIS");
                saveDescBtn.setTextSize(12);
                saveDescBtn.setPadding(0, 0, 0, 0);
                saveDescBtn.setBackgroundColor(Color.parseColor("#0099CC"));
                saveDescBtn.setTextColor(Color.WHITE);
                if (catalogId != -1) saveDescBtn.setEnabled(false);

                saveDescBtn.setOnClickListener(v -> {
                    photo.description = descEditText.getText().toString().trim();
                    flatPhotoViewModel.update(photo);
                    Toast.makeText(this, "Zapisano opis usterki", Toast.LENGTH_SHORT).show();
                    hideKeyboard();
                    descEditText.clearFocus();
                });
                cardLayout.addView(saveDescBtn);

                // Przycisk Usuń (Zawsze widoczny krwistoczerwony)
                Button delBtn = new Button(this);
                LinearLayout.LayoutParams btnDelParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, (int) (42 * dpScale));
                delBtn.setLayoutParams(btnDelParams);
                delBtn.setText("USUŃ");
                delBtn.setTextSize(12);
                delBtn.setPadding(0, 0, 0, 0);
                delBtn.setBackgroundColor(Color.parseColor("#D32F2F"));
                delBtn.setTextColor(Color.WHITE);
                if (catalogId != -1) delBtn.setEnabled(false);

                delBtn.setOnClickListener(v -> {
                    File file = new File(photo.photoPath);
                    if (file.exists()) {
                        file.delete();
                    }
                    flatPhotoViewModel.delete(photo);
                    Toast.makeText(this, "Zdjęcie zostało usunięte", Toast.LENGTH_SHORT).show();
                });
                cardLayout.addView(delBtn);

                notesPhotosContainer.addView(cardLayout);
            }
        } else {
            TextView noPhotosTv = new TextView(this);
            noPhotosTv.setText("Brak zdjęć dla tych uwag");
            noPhotosTv.setTextSize(16);
            noPhotosTv.setTextColor(Color.parseColor("#757575"));
            noPhotosTv.setPadding((int) (8 * dpScale), (int) (16 * dpScale), (int) (8 * dpScale), (int) (16 * dpScale));
            notesPhotosContainer.addView(noPhotosTv);
        }
    }

    private void setupSignatureLogic() {
        saveName.setOnClickListener(v-> {
            hideKeyboard();
            etSignerName.clearFocus();
        });
        etSignerName.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard();
                etSignerName.clearFocus();
                return true;
            }
            return false;
        });
        signatureViewModel.getSignatureForFlat(flatId).observe(this, signature -> {
            if (signature != null && signature.signatureData != null) {
                termsContainer.setVisibility(View.GONE);
                signaturePadContainer.setVisibility(View.GONE);
                savedSignatureContainer.setVisibility(View.VISIBLE);

                String dateStr = signature.signatureDate != null ?
                        android.text.format.DateFormat.format("yyyy-MM-dd HH:mm", signature.signatureDate).toString() : "-";
                tvSavedSignatureInfo.setText("Podpis złożony przez: " + signature.signerName + "\nData: " + dateStr);

                Bitmap bitmap = BitmapFactory.decodeByteArray(signature.signatureData, 0, signature.signatureData.length);
                savedSignatureImage.setImageBitmap(bitmap);
            } else {
                if (catalogId == -1) {
                    termsContainer.setVisibility(View.VISIBLE);
                    signaturePadContainer.setVisibility(View.GONE);
                    savedSignatureContainer.setVisibility(View.GONE);
                    etSignerName.setText("");
                    signatureView.clear();
                }
            }
        });

        btnNextToSignature.setOnClickListener(v -> {
            termsContainer.setVisibility(View.GONE);
            signaturePadContainer.setVisibility(View.VISIBLE);
        });

        btnCancelSignature.setOnClickListener(v -> {
            hideKeyboard();
            signatureView.clear();
            etSignerName.setText("");
            signaturePadContainer.setVisibility(View.GONE);
            termsContainer.setVisibility(View.VISIBLE);
        });

        btnClearSignature.setOnClickListener(v -> {
            signatureView.clear();
        });

        btnSaveSignature.setOnClickListener(v -> {
            String name = etSignerName.getText().toString().trim();
            if (name.isEmpty()) {
                etSignerName.setError("Wprowadź imię i nazwisko!");
                return;
            }

            Bitmap signatureBitmap = signatureView.getSignatureBitmap();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            signatureBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();

            Signature newSignature = new Signature();
            newSignature.flatId = flatId;
            newSignature.signatureData = byteArray;
            newSignature.signerName = name;
            newSignature.signatureDate = new Date();

            hideKeyboard();
            signatureViewModel.insert(newSignature);
            Toast.makeText(this, "Podpis został pomyślnie zapisany!", Toast.LENGTH_SHORT).show();
        });

        btnShowTermsPostSign.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Regulamin")
                    .setMessage(currentFlat.refusedInspection == 1 ? LegalTexts.REFUSAL_TERMS : LegalTexts.SIGNATURE_TERMS)
                    .setPositiveButton("Zamknij", (dialog, which) -> dialog.dismiss())
                    .show();
        });

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
        if (isCommonSpace == 1) {
            if (currentFlat != null) {
                blockViewModel.getBlockById(currentFlat.blockId, b -> {
                    if (b == null || b.block == null) return;
                    runOnUiThread(() -> {
                        String buildingLabel = b.block.buildingType == 1 ? "Dom" : "Blok";
                        titleView.setText(buildingLabel + " " + b.block.street + " " + b.block.number + " - podsumowanie");
                    });
                });
            } else {
                titleView.setText("Podsumowanie - " + blockName);
            }
        } else {
            titleView.setText("Mieszkanie numer - " + currentFlat.number + " podsumowanie");
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
                intent.putExtra("commonSpace", 1);
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
                intent.putExtra("commonSpace", 1);
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
                blockViewModel.getBlockById(currentFlat.blockId, b -> {
                    if (b.block.buildingType == 1) {
                        Intent intent = new Intent(this, BlocksActivity.class);
                        intent.putExtra("catalogId", b.block.catalogId);
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(this, BlockActivity.class);
                        intent.putExtra("blockId", currentFlat.blockId);
                        startActivity(intent);
                    }
                });
                return;
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

    private void updateTermsText() {
        if (currentFlat == null) return;
        TextView tvTermsText = findViewById(R.id.tvTermsText);
        if (tvTermsText != null) {
            tvTermsText.setText(currentFlat.refusedInspection == 1 ? LegalTexts.REFUSAL_TERMS : LegalTexts.SIGNATURE_TERMS);
        }
    }
}