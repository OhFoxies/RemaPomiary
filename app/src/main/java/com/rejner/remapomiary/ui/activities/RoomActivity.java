package com.rejner.remapomiary.ui.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.rejner.remapomiary.R;
import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.data.entities.OutletMeasurement;
import com.rejner.remapomiary.data.entities.RoomInFlat;
import com.rejner.remapomiary.ui.viewmodels.FlatViewModel;
import com.rejner.remapomiary.ui.viewmodels.OutletMeasurementViewModel;
import com.rejner.remapomiary.ui.viewmodels.RoomViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class RoomActivity extends AppCompatActivity {
    private LinearLayout roomsContainer;
    private Spinner roomSpinner;
    private EditText customRoomEditText;
    private Button addRoomButton;
    private RoomViewModel roomViewModel;
    private OutletMeasurementViewModel outletViewModel;
    private int flatId;
    private final String[] roomNames = {"Pokój", "Korytarz", "Łazienka", "Kuchnia", "Inne"};
    private final String[] applianceOptions = {"Gniazdko", "Lodówka", "Pralka", "Grzejnik", "Inne"};
    private final String[] breakerTypes = {"B", "C", "D", "Gg"};
    private final String[] noteOptions = {"brak uwag", "nie podłączony bolec", "Urwane", "Inne"};
    private final String[] ampsOptions = {"3", "6", "10", "16", "20", "25", "32", "40"};
    private final Map<Integer, List<OutletMeasurement>> roomMeasurementsMap = new HashMap<>();
    private String lastDefaultSwitchName = null;
    private String lastDefaultBreakerType = null;
    private Double lastDefaultAmps = null;
    private boolean isInitializing = false;
    private long newlyAddedMeasurementId = -1;
    private Flat flat;
    private android.content.res.ColorStateList greenTint;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);
        flatId = getIntent().getIntExtra("flatId", -1);
        roomViewModel = new ViewModelProvider(this).get(RoomViewModel.class);
        outletViewModel = new ViewModelProvider(this).get(OutletMeasurementViewModel.class);
        FlatViewModel flatViewModel = new ViewModelProvider(this).get(FlatViewModel.class);
        greenTint = ContextCompat.getColorStateList(this, android.R.color.holo_green_dark);

        flatViewModel.getFlatByIdSync(flatId, flat1 -> {
            flat = flat1.flat;
            runOnUiThread(() -> {
                setupUIElements();
            });
        });
        setupUi();
        observeRooms();
    }


    private void setupUIElements() {
        Button backButton = findViewById(R.id.backButton);
        Button notesButton = findViewById(R.id.notesButton);
        Button RCDButton = findViewById(R.id.RCDButton);
        Button boardButton = findViewById(R.id.boardButton);
        TextView titleView = findViewById(R.id.flatTitle);
        titleView.setText("Mieszkanie numer - " + flat.number + " pętla zwarcia");

        boardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RoomActivity.this, BoardActivity.class);
                intent.putExtra("flatId", flat.id);
                startActivity(intent);
            }
        });

        notesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RoomActivity.this, NotesActivity.class);
                intent.putExtra("flatId", flat.id);
                startActivity(intent);
            }
        });


        RCDButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RoomActivity.this, RCDActivity.class);
                intent.putExtra("flatId", flat.id);
                startActivity(intent);
            }
        });


        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (flat == null) return;
                Intent intent = new Intent(RoomActivity.this, FlatsActivity.class);
                intent.putExtra("blockId", flat.blockId);
                startActivity(intent);
            }
        });

    }

    private void setupUi() {
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
        roomsContainer = findViewById(R.id.roomsContainer);
        roomSpinner = findViewById(R.id.roomSpinner);
        customRoomEditText = findViewById(R.id.customRoomEditText);
        addRoomButton = findViewById(R.id.addRoomButton);
        addRoomButton.setBackgroundTintList(greenTint);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roomNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roomSpinner.setAdapter(adapter);
        roomSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                // if (isInitializing) return; // Usunięto warunek blokujący
                String sel = roomNames[position];
                if ("Inne".equals(sel)) {
                    customRoomEditText.setVisibility(View.VISIBLE);
                    customRoomEditText.requestFocus();
                    showKeyboard(customRoomEditText);
                } else {
                    customRoomEditText.setVisibility(View.GONE);
                    hideKeyboard();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
        customRoomEditText.setVisibility(View.GONE);
        customRoomEditText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                customRoomEditText.requestFocus();
                showKeyboard(customRoomEditText);
            }
            return false;
        });
        addRoomButton.setOnClickListener(v -> {
            if (flatId == -1) return;
            String name;
            int pos = roomSpinner.getSelectedItemPosition();
            if (pos >= 0 && pos < roomNames.length && "Inne".equals(roomNames[pos])) {
                name = customRoomEditText.getText() != null ? customRoomEditText.getText().toString().trim() : "";
                if (name.isEmpty()) {
                    customRoomEditText.setError("Wpisz nazwę pokoju");
                    customRoomEditText.requestFocus();
                    return;
                }
            } else {
                name = roomNames[pos];
            }
            RoomInFlat room = new RoomInFlat();
            room.flatId = flatId;
            room.name = name;
            roomViewModel.insert(room);
            customRoomEditText.setText("");
            roomSpinner.setSelection(0);
            customRoomEditText.setVisibility(View.GONE);
            hideKeyboard();
        });
    }

    private void observeRooms() {
        roomViewModel.getRoomsForFlat(flatId).observe(this, rooms -> {
            isInitializing = true;
            roomsContainer.removeAllViews();
            roomMeasurementsMap.clear();
            if (rooms != null && !rooms.isEmpty()) {
                for (RoomInFlat room : rooms) {
                    LinearLayout roomCard = createRoomCard(room);
                    roomsContainer.addView(roomCard);
                    observeMeasurementsForRoom(room.id, roomCard);
                }
            }
            isInitializing = false;
        });
    }

    private LinearLayout createRoomCard(RoomInFlat room) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        int margin = (int) (12 * getResources().getDisplayMetrics().density);
        ((LinearLayout.LayoutParams) card.getLayoutParams()).setMargins(0, margin, 0, margin);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        card.setPadding(padding, padding, padding, padding);
        card.setBackground(ContextCompat.getDrawable(this, R.drawable.round_corners));
        card.setTag("room_card_" + room.id);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView roomTitle = new TextView(this);
        roomTitle.setText(room.name != null ? room.name : ("Pokój " + room.id));
        roomTitle.setTextSize(18f);
        roomTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        roomTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 3f));
        roomTitle.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        Button deleteRoom = new Button(this);
        deleteRoom.setText("USUŃ POKÓJ");
        deleteRoom.setAllCaps(false);
        deleteRoom.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        deleteRoom.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        deleteRoom.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_red_dark));
        deleteRoom.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Usuń pokój")
                    .setMessage("Czy na pewno chcesz usunąć pokój "+ room.name + " wraz ze wszystkimi pomiarami?")
                    .setPositiveButton("Usuń", (dialog, which) -> roomViewModel.delete(room))
                    .setNegativeButton("Anuluj", null)
                    .show();
        });
        header.addView(roomTitle);
        header.addView(deleteRoom);
        card.addView(header);

        LinearLayout measurementsContainer = new LinearLayout(this);
        measurementsContainer.setOrientation(LinearLayout.VERTICAL);
        measurementsContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        measurementsContainer.setTag("measurements_container_" + room.id);
        measurementsContainer.setPadding(0, (int)(8 * getResources().getDisplayMetrics().density), 0, 0);
        card.addView(measurementsContainer);

        Button addMeasurementBtn = new Button(this);
        addMeasurementBtn.setText("➕ Dodaj pomiar");
        addMeasurementBtn.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        addMeasurementBtn.setBackgroundTintList(greenTint);
        addMeasurementBtn.setAllCaps(false);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnParams.gravity = Gravity.END;
        addMeasurementBtn.setLayoutParams(btnParams);
        addMeasurementBtn.setOnClickListener(v -> onAddMeasurementClicked(room.id));
        card.addView(addMeasurementBtn);
        return card;
    }

    private View createHeaderRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        row.setLayoutParams(rp);
        row.setPadding(12, 12, 12, 12);
        row.setGravity(Gravity.CENTER_VERTICAL);

        String[] titles = {"Urządzenie", "Wyłącznik", "Typ", "Amper", "Wartość", "Uwagi", ""};
        float[] weights = {2f, 1.5f, 1f, 1f, 1f, 2f, 0.5f};

        for (int i = 0; i < titles.length; i++) {
            TextView tv = new TextView(this);
            tv.setText(titles[i]);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weights[i]));
            tv.setTextSize(14f);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setGravity(Gravity.CENTER);
            row.addView(tv);
        }
        return row;
    }

    private void observeMeasurementsForRoom(int roomId, LinearLayout roomCard) {
        outletViewModel.getMeasurementsForRoom(roomId).observe(this, measurements -> {
            isInitializing = true;
            roomMeasurementsMap.put(roomId, measurements != null ? new ArrayList<>(measurements) : new ArrayList<>());
            recomputeGlobalDefaults();
            LinearLayout measurementsContainer = roomCard.findViewWithTag("measurements_container_" + roomId);
            if (measurementsContainer != null) {
                measurementsContainer.removeAllViews();
                if (measurements != null && !measurements.isEmpty()) {
                    measurementsContainer.addView(createHeaderRow());
                    for (OutletMeasurement om : measurements) {
                        View row = createMeasurementRow(om);
                        measurementsContainer.addView(row);
                        if (newlyAddedMeasurementId != -1 && om.id == newlyAddedMeasurementId) {
                            row.post(() -> {
                                EditText ohmsEdit = row.findViewWithTag("ohms_edit_" + om.id);
                                if (ohmsEdit != null) {
                                    ohmsEdit.requestFocus();
                                    ohmsEdit.setSelection(ohmsEdit.getText().length()); // Set cursor to the end
                                    ohmsEdit.postDelayed(() -> showKeyboard(ohmsEdit), 200); // Increased delay for keyboard to appear reliably
                                    newlyAddedMeasurementId = -1; // Reset ID here after execution
                                }
                            });
                        }
                    }
                } else {
                    TextView empty = new TextView(this);
                    empty.setText("Brak pomiarów. Dodaj nowy pomiar poniżej.");
                    empty.setPadding(8,12,8,12);
                    measurementsContainer.addView(empty);
                }
            }
        });
    }

    private void recomputeGlobalDefaults() {
        lastDefaultSwitchName = null;
        lastDefaultBreakerType = null;
        lastDefaultAmps = null;
        Map<String, Integer> switchFreq = new HashMap<>();
        Map<String, Integer> breakerFreq = new HashMap<>();
        Map<Integer, Integer> ampsFreq = new HashMap<>();
        for (List<OutletMeasurement> list : roomMeasurementsMap.values()) {
            if (list == null) continue;
            for (OutletMeasurement om : list) {
                if (om == null) continue;
                if (om.switchName != null && !om.switchName.trim().isEmpty()) {
                    String key = om.switchName.trim();
                    switchFreq.put(key, switchFreq.getOrDefault(key, 0) + 1);
                }
                if (om.breakerType != null && !om.breakerType.trim().isEmpty()) {
                    String key = om.breakerType.trim();
                    breakerFreq.put(key, breakerFreq.getOrDefault(key, 0) + 1);
                }
                if (om.amps != null && om.amps > 0) {
                    Integer a = om.amps.intValue();
                    ampsFreq.put(a, ampsFreq.getOrDefault(a, 0) + 1);
                }
            }
        }
        lastDefaultSwitchName = selectModeString(switchFreq);
        lastDefaultBreakerType = selectModeString(breakerFreq);
        Integer ampsMode = selectModeInt(ampsFreq);
        lastDefaultAmps = ampsMode != null ? ampsMode.doubleValue() : null;
    }

    private String selectModeString(Map<String, Integer> freqMap) {
        if (freqMap == null || freqMap.isEmpty()) return null;
        String best = null;
        int bestCount = 0;
        for (Map.Entry<String, Integer> e : freqMap.entrySet()) {
            if (e.getValue() > bestCount) {
                best = e.getKey();
                bestCount = e.getValue();
            }
        }
        return best;
    }

    private Integer selectModeInt(Map<Integer, Integer> freqMap) {
        if (freqMap == null || freqMap.isEmpty()) return null;
        Integer best = null;
        int bestCount = 0;
        for (Map.Entry<Integer, Integer> e : freqMap.entrySet()) {
            if (e.getValue() > bestCount) {
                best = e.getKey();
                bestCount = e.getValue();
            }
        }
        return best;
    }

    private void onAddMeasurementClicked(int roomId) {
        OutletMeasurement newOm = new OutletMeasurement();
        newOm.roomId = roomId;
        newOm.appliance = applianceOptions[0];
        newOm.switchName = "";
        newOm.breakerType = breakerTypes[0];
        newOm.amps = null;
        newOm.ohms = 0.0;
        newOm.note = noteOptions[0];
        newOm.number = 0;
        if (lastDefaultSwitchName != null && (newOm.switchName == null || newOm.switchName.trim().isEmpty())) {
            newOm.switchName = lastDefaultSwitchName;
        }
        if (lastDefaultBreakerType != null && (newOm.breakerType == null || newOm.breakerType.trim().isEmpty())) {
            newOm.breakerType = lastDefaultBreakerType;
        }
        if (lastDefaultAmps != null && (newOm.amps == null || newOm.amps <= 0)) {
            newOm.amps = lastDefaultAmps;
        }
        outletViewModel.insert(newOm, lastId -> {
            newlyAddedMeasurementId = lastId;
        });
    }

    private boolean isCustomNote(String note) {
        if (note == null) return false;
        for (String predefinedNote : noteOptions) {
            if (predefinedNote.equalsIgnoreCase(note)) {
                return false;
            }
        }
        return true;
    }

    private void styleUtilityButton(Button button) {
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(16, 0, 16, 0);
    }

    private View createMeasurementRow(OutletMeasurement om) {
        LinearLayout row = new LinearLayout(this);
        row.setFocusable(true);
        row.setFocusableInTouchMode(true);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = (int) (6 * getResources().getDisplayMetrics().density);
        rp.setMargins(0, margin, 0, margin);
        row.setLayoutParams(rp);
        row.setPadding(12,12,12,12);
        row.setBackground(ContextCompat.getDrawable(this, R.drawable.circuit_row_background));
        row.setGravity(Gravity.CENTER_VERTICAL);

        Spinner applianceSpinner = new Spinner(this);
        ArrayAdapter<String> appAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, applianceOptions);
        appAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        applianceSpinner.setAdapter(appAdapter);

        EditText customApplianceEdit = new EditText(this);
        customApplianceEdit.setHint("Inne urządzenie...");
        customApplianceEdit.setSingleLine(true);
        customApplianceEdit.setImeOptions(EditorInfo.IME_ACTION_DONE);
        customApplianceEdit.setBackground(ContextCompat.getDrawable(this, R.drawable.input));
        customApplianceEdit.setPadding(18, 12, 18, 12);

        Button customApplianceSaveBtn = new Button(this);
        customApplianceSaveBtn.setText("✔");
        customApplianceSaveBtn.setTextSize(14f);
        styleUtilityButton(customApplianceSaveBtn);
        customApplianceSaveBtn.setVisibility(View.GONE);

        Button customApplianceClearBtn = new Button(this);
        customApplianceClearBtn.setText("X");
        customApplianceClearBtn.setTextSize(14f);
        styleUtilityButton(customApplianceClearBtn);
        customApplianceClearBtn.setVisibility(View.GONE);

        LinearLayout applianceContainer = new LinearLayout(this);
        applianceContainer.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams applianceContainerParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f);
        int marginEndAppliance = (int) (8 * getResources().getDisplayMetrics().density);
        applianceContainerParams.setMarginEnd(marginEndAppliance);
        applianceContainer.setLayoutParams(applianceContainerParams);
        applianceContainer.setGravity(Gravity.CENTER_VERTICAL);
        applianceContainer.addView(applianceSpinner, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        applianceContainer.addView(customApplianceEdit, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        applianceContainer.addView(customApplianceSaveBtn);
        applianceContainer.addView(customApplianceClearBtn);

        int applianceIndex = 0;
        boolean foundApplianceInOptions = false;
        if (om.appliance != null) {
            for (int i = 0; i < applianceOptions.length; i++) {
                if (applianceOptions[i].equalsIgnoreCase(om.appliance)) {
                    applianceIndex = i;
                    foundApplianceInOptions = true;
                    break;
                }
            }
        }
        if (!foundApplianceInOptions) {
            for (int i = 0; i < applianceOptions.length; i++) {
                if ("Inne".equalsIgnoreCase(applianceOptions[i])) {
                    applianceIndex = i;
                    break;
                }
            }
            customApplianceEdit.setText(om.appliance != null ? om.appliance : "");
            customApplianceEdit.setVisibility(View.VISIBLE);
            applianceSpinner.setVisibility(View.GONE);
        } else {
            applianceSpinner.setVisibility(View.VISIBLE);
            customApplianceEdit.setVisibility(View.GONE);
        }
        applianceSpinner.setSelection(applianceIndex, false);

        EditText switchEdit = new EditText(this);
        switchEdit.setHint("wyłącznik");
        switchEdit.setText(om.switchName != null ? om.switchName : "");
        switchEdit.setSingleLine(true);
        switchEdit.setImeOptions(EditorInfo.IME_ACTION_DONE);
        switchEdit.setBackground(ContextCompat.getDrawable(this, R.drawable.input));
        switchEdit.setPadding(18, 12, 18, 12);

        Button switchSaveBtn = new Button(this);
        switchSaveBtn.setText("✔");
        switchSaveBtn.setTextSize(14f);
        styleUtilityButton(switchSaveBtn);
        switchSaveBtn.setVisibility(View.GONE);

        final LinearLayout switchContainer = new LinearLayout(this);
        switchContainer.setOrientation(LinearLayout.HORIZONTAL);
        switchContainer.setGravity(Gravity.CENTER_VERTICAL);
        switchContainer.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f));
        switchContainer.addView(switchEdit, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        switchContainer.addView(switchSaveBtn);

        Spinner breakerSpinner = new Spinner(this);
        // Use a custom layout for the spinner items to ensure consistent padding and text size
        ArrayAdapter<String> brAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, breakerTypes);
        brAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        breakerSpinner.setAdapter(brAdapter);
        int brIdx = 0;
        for (int i = 0; i < breakerTypes.length; i++) if (breakerTypes[i].equalsIgnoreCase(om.breakerType)) { brIdx = i; break; }
        breakerSpinner.setSelection(brIdx, false);
        breakerSpinner.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Spinner ampsSpinner = new Spinner(this);
        ArrayAdapter<String> ampsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ampsOptions);
        ampsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ampsSpinner.setAdapter(ampsAdapter);
        int ampsIdx = 0;
        if (om.amps != null) {
            String aStr = String.valueOf(om.amps.longValue());
            for (int i = 0; i < ampsOptions.length; i++) if (ampsOptions[i].equals(aStr)) { ampsIdx = i; break; }
        }
        ampsSpinner.setSelection(ampsIdx, false);
        ampsSpinner.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        EditText ohmsEdit = new EditText(this);
        ohmsEdit.setHint("Ω");
        ohmsEdit.setTag("ohms_edit_" + om.id);
        ohmsEdit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL); // Ensure decimal input
        ohmsEdit.setImeOptions(EditorInfo.IME_ACTION_DONE);
        ohmsEdit.setText(String.format(Locale.GERMANY, "%.2f", om.ohms != null ? om.ohms : 0.0));
        ohmsEdit.setBackground(ContextCompat.getDrawable(this, R.drawable.input));
        ohmsEdit.setPadding(18, 12, 18, 12);
        ohmsEdit.addTextChangedListener(new OhmsTextWatcher(ohmsEdit));

        Button ohmsSaveBtn = new Button(this);
        ohmsSaveBtn.setText("✔");
        ohmsSaveBtn.setTextSize(14f);
        styleUtilityButton(ohmsSaveBtn);
        ohmsSaveBtn.setVisibility(View.GONE);

        LinearLayout ohmsContainer = new LinearLayout(this);
        ohmsContainer.setOrientation(LinearLayout.HORIZONTAL);
        ohmsContainer.setGravity(Gravity.CENTER_VERTICAL);
        ohmsContainer.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ohmsContainer.addView(ohmsEdit, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ohmsContainer.addView(ohmsSaveBtn);

        Spinner noteSpinner = new Spinner(this);
        ArrayAdapter<String> noteAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, noteOptions);
        noteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        noteSpinner.setAdapter(noteAdapter);
        noteSpinner.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f));

        EditText customNoteEdit = new EditText(this);
        customNoteEdit.setHint("Inne...");
        customNoteEdit.setSingleLine(true);
        customNoteEdit.setImeOptions(EditorInfo.IME_ACTION_DONE);
        customNoteEdit.setBackground(ContextCompat.getDrawable(this, R.drawable.input));
        customNoteEdit.setPadding(18, 12, 18, 12);

        Button customNoteSaveBtn = new Button(this);
        customNoteSaveBtn.setText("✔");
        customNoteSaveBtn.setTextSize(14f);
        styleUtilityButton(customNoteSaveBtn);
        customNoteSaveBtn.setVisibility(View.GONE);

        Button customNoteClearBtn = new Button(this);
        customNoteClearBtn.setText("X");
        customNoteClearBtn.setTextSize(14f);
        styleUtilityButton(customNoteClearBtn);
        customNoteClearBtn.setVisibility(View.GONE);

        LinearLayout customNoteContainer = new LinearLayout(this);
        customNoteContainer.setOrientation(LinearLayout.HORIZONTAL);
        customNoteContainer.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams customNoteParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f);
        int marginNote = (int) (8 * getResources().getDisplayMetrics().density);
        customNoteParams.setMarginEnd(marginNote);
        customNoteParams.setMarginStart(marginNote);
        customNoteContainer.setLayoutParams(customNoteParams);
        customNoteContainer.addView(customNoteEdit, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        customNoteContainer.addView(customNoteSaveBtn);
        customNoteContainer.addView(customNoteClearBtn);

        if (isCustomNote(om.note)) {
            customNoteEdit.setText(om.note);
            noteSpinner.setVisibility(View.GONE);
            customNoteContainer.setVisibility(View.VISIBLE);
        } else {
            noteSpinner.setVisibility(View.VISIBLE);
            customNoteContainer.setVisibility(View.GONE);
            int noteIdx = 0;
            for (int i = 0; i < noteOptions.length; i++) if (noteOptions[i].equalsIgnoreCase(om.note)) { noteIdx = i; break; }
            noteSpinner.setSelection(noteIdx, false);
        }

        Button deleteBtn = new Button(this);
        deleteBtn.setText("🗑️");
        deleteBtn.setAllCaps(false);
        deleteBtn.setTextSize(18f);
        deleteBtn.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        deleteBtn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        try { deleteBtn.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_red_dark)); } catch (Exception ignored) { }

        // --- Listeners and save/update logic ---
        customApplianceEdit.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                customApplianceSaveBtn.performClick();
                return true;
            }
            return false;
        });
        switchEdit.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                switchSaveBtn.performClick();
                return true;
            }
            return false;
        });
        noteSpinner.post(() -> applianceSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (isInitializing) return;
                String selected = applianceOptions[position];
                if ("Inne".equals(selected)) {
                    applianceSpinner.setVisibility(View.GONE);
                    customApplianceEdit.setVisibility(View.VISIBLE);
                    customApplianceClearBtn.setVisibility(View.VISIBLE);
                    customApplianceEdit.requestFocus();
                    showKeyboard(customApplianceEdit);
                } else {
                    if (customApplianceEdit.getVisibility() == View.VISIBLE) {
                        customApplianceEdit.setVisibility(View.GONE);
                        customApplianceSaveBtn.setVisibility(View.GONE);
                        customApplianceClearBtn.setVisibility(View.GONE);
                    }
                    applianceSpinner.setVisibility(View.VISIBLE);
                    if ((om.switchName == null || om.switchName.trim().isEmpty()) && lastDefaultSwitchName != null) {
                        om.switchName = lastDefaultSwitchName;
                        switchEdit.setText(om.switchName);
                    }
                    if ((om.breakerType == null || om.breakerType.trim().isEmpty()) && lastDefaultBreakerType != null) {
                        om.breakerType = lastDefaultBreakerType;
                        for (int i = 0; i < breakerTypes.length; i++) if (breakerTypes[i].equalsIgnoreCase(lastDefaultBreakerType)) { breakerSpinner.setSelection(i, false); break; }
                    }
                    if ((om.amps == null || om.amps <= 0) && lastDefaultAmps != null) {
                        om.amps = lastDefaultAmps;
                        String aStr = String.valueOf(om.amps.longValue());
                        for (int i = 0; i < ampsOptions.length; i++) if (ampsOptions[i].equals(aStr)) { ampsSpinner.setSelection(i, false); break; }
                    }
                    if (om.appliance == null || !om.appliance.equals(selected)) {
                        om.appliance = selected;
                        outletViewModel.update(om, null);
                    }
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        }));

        final float originalApplianceWeight = ((LinearLayout.LayoutParams) applianceContainer.getLayoutParams()).weight;
        final float originalSwitchWeight = ((LinearLayout.LayoutParams) switchContainer.getLayoutParams()).weight;

        customApplianceEdit.setOnFocusChangeListener((v, hasFocus) -> {
            customApplianceSaveBtn.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
            customApplianceClearBtn.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
            switchContainer.setVisibility(hasFocus ? View.GONE : View.VISIBLE);
            LinearLayout.LayoutParams applianceParams = (LinearLayout.LayoutParams) applianceContainer.getLayoutParams();
            if (hasFocus) applianceParams.weight = originalApplianceWeight + originalSwitchWeight;
            else applianceParams.weight = originalApplianceWeight;
            applianceContainer.setLayoutParams(applianceParams);
        });
        customApplianceSaveBtn.setOnClickListener(v -> {
            String txt = customApplianceEdit.getText() != null ? customApplianceEdit.getText().toString().trim() : "";
            if (!txt.isEmpty()) {
                if (om.appliance == null || !om.appliance.equals(txt)) {
                    om.appliance = txt;
                    outletViewModel.update(om, null);
                }
            } else {
                om.appliance = applianceOptions[0];
                outletViewModel.update(om, null);
            }
            hideKeyboard();
            customApplianceEdit.clearFocus();
        });
        customApplianceClearBtn.setOnClickListener(v -> {
            customApplianceEdit.setText("");
            om.appliance = applianceOptions[0];
            outletViewModel.update(om, null);
            hideKeyboard();
            customApplianceEdit.clearFocus();
        });

        switchEdit.setOnFocusChangeListener((v, hasFocus) -> switchSaveBtn.setVisibility(hasFocus ? View.VISIBLE : View.GONE));
        switchSaveBtn.setOnClickListener(v -> {
            String newSwitch = switchEdit.getText() != null ? switchEdit.getText().toString().trim() : "";
            if (!newSwitch.equals(om.switchName)) {
                om.switchName = newSwitch;
                outletViewModel.update(om, () -> { if (om.switchName != null && !om.switchName.isEmpty()) lastDefaultSwitchName = om.switchName; });
            }
            hideKeyboard();
            switchEdit.clearFocus();
        });

        breakerSpinner.post(() -> breakerSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                String sel = breakerTypes[pos];
                if (!sel.equalsIgnoreCase(om.breakerType)) {
                    om.breakerType = sel;
                    outletViewModel.update(om, () -> { if (om.breakerType != null && !om.breakerType.isEmpty()) lastDefaultBreakerType = om.breakerType; });
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        }));

        ampsSpinner.post(() -> ampsSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                try {
                    Double dVal = Double.parseDouble(ampsOptions[pos]);
                    if (om.amps == null || !om.amps.equals(dVal)) {
                        om.amps = dVal;
                        outletViewModel.update(om, () -> { if (om.amps != null) lastDefaultAmps = om.amps; });
                    }
                } catch (Exception ignored) {}
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        }));

        ohmsEdit.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                ohmsSaveBtn.performClick();
                return true;
            }
            return false;
        });
        ohmsEdit.setOnFocusChangeListener((v, hasFocus) -> {
            ohmsSaveBtn.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
            LinearLayout.LayoutParams ohmsParams = (LinearLayout.LayoutParams) ohmsContainer.getLayoutParams();
            final float originalOhmsWeight = 1f;
            final float noteWeight = ((LinearLayout.LayoutParams) noteSpinner.getLayoutParams()).weight;
            if (hasFocus) {
                noteSpinner.setVisibility(View.GONE);
                customNoteContainer.setVisibility(View.GONE);
                ohmsParams.weight = originalOhmsWeight + noteWeight;
            } else {
                ohmsParams.weight = originalOhmsWeight;
                // Correctly restore visibility of note spinner/custom note container
                if (isCustomNote(om.note)) {
                    customNoteContainer.setVisibility(View.VISIBLE);
                    noteSpinner.setVisibility(View.GONE);
                } else {
                        noteSpinner.setVisibility(View.VISIBLE);
                        customNoteContainer.setVisibility(View.GONE);
                    }

            }
            ohmsContainer.setLayoutParams(ohmsParams);
        });
        ohmsSaveBtn.setOnClickListener(v -> {
            String txt = ohmsEdit.getText().toString().replace(',', '.');
            Double val = null;
            try { if (!txt.isEmpty()) val = Double.parseDouble(txt); } catch (Exception ignored) {}
            if ((val == null && om.ohms != null) || (val != null && !val.equals(om.ohms))) {
                om.ohms = val;
                outletViewModel.update(om, null);
            }
            hideKeyboard();
            ohmsEdit.clearFocus();
        });

        noteSpinner.post(() -> noteSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                String sel = noteOptions[pos];
                if ("Inne".equalsIgnoreCase(sel)) {
                    noteSpinner.setVisibility(View.GONE);
                    customNoteContainer.setVisibility(View.VISIBLE);
                    customNoteClearBtn.setVisibility(View.VISIBLE);
                    customNoteEdit.requestFocus();
                    showKeyboard(customNoteEdit);
                } else {
                    if (om.note == null || !om.note.equals(sel)) {
                        om.note = sel;
                        outletViewModel.update(om, null);
                    }
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        }));

        customNoteEdit.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                customNoteSaveBtn.performClick();
                return true;
            }
            return false;
        });
        final float originalNoteWeight = ((LinearLayout.LayoutParams) customNoteContainer.getLayoutParams()).weight;
        customNoteEdit.setOnFocusChangeListener((v, hasFocus) -> {
            customNoteSaveBtn.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
            customNoteClearBtn.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
            switchContainer.setVisibility(hasFocus ? View.GONE : View.VISIBLE);
            LinearLayout.LayoutParams noteParams = (LinearLayout.LayoutParams) customNoteContainer.getLayoutParams();
            if (hasFocus) noteParams.weight = originalNoteWeight + originalSwitchWeight;
            else noteParams.weight = originalNoteWeight;
            customNoteContainer.setLayoutParams(noteParams);
        });
        customNoteSaveBtn.setOnClickListener(v -> {
            String txt = customNoteEdit.getText() != null ? customNoteEdit.getText().toString().trim() : "";
            om.note = txt.isEmpty() ? noteOptions[0] : txt;
            outletViewModel.update(om, null);
            hideKeyboard();
            customNoteEdit.clearFocus();
        });
        customNoteClearBtn.setOnClickListener(v -> {
            customNoteEdit.setText("");
            om.note = noteOptions[0];
            outletViewModel.update(om, null);
            hideKeyboard();
            customNoteContainer.setVisibility(View.GONE);
            noteSpinner.setVisibility(View.VISIBLE);
            noteSpinner.setSelection(0);
            row.requestFocus();
        });

        deleteBtn.setOnClickListener(v -> outletViewModel.delete(om, null));

        row.addView(applianceContainer);
        row.addView(switchContainer);
        row.addView(breakerSpinner);
        row.addView(ampsSpinner);
        row.addView(ohmsContainer);
        row.addView(noteSpinner);
        row.addView(customNoteContainer);
        row.addView(deleteBtn);
        return row;
    }

    private void showKeyboard(View view) {
        view.post(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View focused = getCurrentFocus();
        if (imm != null && focused != null) {
            imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }
    }

    private static class OhmsTextWatcher implements TextWatcher {
        private final EditText editText;
        private String current = "";

        OhmsTextWatcher(EditText editText) { this.editText = editText; }

        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override public void afterTextChanged(Editable s) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.toString().equals(current)) {
                return;
            }
            editText.removeTextChangedListener(this);

            String cleanString = s.toString().replaceAll("[^\\d]", "");

            // Remove leading zeros
            if (cleanString.length() > 1) {
                cleanString = cleanString.replaceFirst("^0+", "");
            }

            double parsed;
            try {
                parsed = Double.parseDouble(cleanString);
            } catch (NumberFormatException e) {
                parsed = 0.0;
            }

            String formatted = String.format(Locale.GERMANY, "%.2f", parsed / 100.0);

            current = formatted;
            editText.setText(formatted);
            editText.setSelection(formatted.length());
            editText.addTextChangedListener(this);
        }
    }
}