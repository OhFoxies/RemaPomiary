package com.rejner.remapomiary.ui.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.room.Room;

import com.rejner.remapomiary.R;
import com.rejner.remapomiary.adapters.RoomAdapter;
import com.rejner.remapomiary.data.entities.CommonSpaceInfo;
import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.data.entities.OutletMeasurement;
import com.rejner.remapomiary.data.entities.RoomInFlat;
import com.rejner.remapomiary.data.utils.LiveDataUtil;
import com.rejner.remapomiary.databinding.ActivityRoomBinding;
import com.rejner.remapomiary.ui.viewmodels.CommonSpaceInfoViewModel;
import com.rejner.remapomiary.ui.viewmodels.FlatViewModel;
import com.rejner.remapomiary.ui.viewmodels.OutletMeasurementViewModel;
import com.rejner.remapomiary.ui.viewmodels.RoomViewModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RoomActivity extends AppCompatActivity {

    private ActivityRoomBinding binding;
    private RoomViewModel roomViewModel;
    private OutletMeasurementViewModel outletViewModel;
    private RoomAdapter roomAdapter;
    private int flatId;
    private Flat flat;

    public String[] roomNames = {"Pokój", "Sypialnia", "Korytarz", "Łazienka", "Kuchnia", "Inne"};
    public String[] applianceOptions = {"Gniazdko", "Lodówka", "Piekarnik", "Telewizor", "Pralka", "Grzejnik", "Inne"};
    public final String[] breakerTypes = {"B", "C", "D", "Gg"};
    public final String[] noteOptions = {"brak uwag", "nie podłączony bolec", "Urwane", "zepsute", "Inne"};
    public final String[] ampsOptions = {"3", "6", "10", "16", "20", "25", "32", "40"};

    private final Map<Integer, List<OutletMeasurement>> roomMeasurementsMap = new HashMap<>();
    private String lastDefaultSwitchName = null;
    private String lastDefaultBreakerType = null;
    private Double lastDefaultAmps = null;
    private CommonSpaceInfo currentCommonSpaceInfo;
    private boolean isUiUpdating = false;
    private int catalogId;
    private long newlyAddedMeasurementId = -1;
    private boolean isCommonSpace;
    private CommonSpaceInfoViewModel commonSpaceInfoViewModel;
    private String blockName;
    private FlatViewModel flatViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRoomBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        flatId = getIntent().getIntExtra("flatId", -1);
        catalogId = getIntent().getIntExtra("catalogId", -1);
        isCommonSpace = getIntent().getIntExtra("isCommonSpace", 0) == 1;
        blockName = getIntent().getStringExtra("name");
        roomViewModel = new ViewModelProvider(this).get(RoomViewModel.class);
        outletViewModel = new ViewModelProvider(this).get(OutletMeasurementViewModel.class);
        flatViewModel = new ViewModelProvider(this).get(FlatViewModel.class);
        commonSpaceInfoViewModel = new ViewModelProvider(this).get(CommonSpaceInfoViewModel.class);


        if (isCommonSpace) {
            roomNames = new String[]{"Korytarz", "Garaż", "Piwnica", "Rowerownia", "Pralnia", "Piętro -", "Inne"};
            applianceOptions = new String[]{"Gniazdko", "Brama garażowa", "Inne"};
        }
        LiveDataUtil.observeOnce(flatViewModel.getFlatById(flatId), this, flat1 -> {
            flat = flat1;
            runOnUiThread(this::setupUIElements);

        });

        setupAddRoomUi();
        setupRecyclerView();
        observeRooms();
        observeAllMeasurements();
    }

    private void setupUIElements() {

        if (flat == null) return;
        if (isCommonSpace) {
            binding.flatTitle.setText("Pętla zwarcia - " + blockName);
            binding.customRoomEditText.setHint("Podaj nazwę pomieszczenia");
            binding.addRoomButton.setText("Dodaj pomieszczenie");

        } else {

            binding.flatTitle.setText("Mieszkanie numer - " + flat.number + " pętla zwarcia");
        }
        if (catalogId != -1) {
            binding.notesButton.setVisibility(View.GONE);
        }
        if (isCommonSpace) {
            binding.backSave.setVisibility(View.GONE);
        } else {
            binding.backSave.setOnClickListener(v -> {
                flat.status = "Pomiar gotowy ✅";
                flat.edition_date = new Date();
                flatViewModel.update(flat);
                Intent intent = new Intent(RoomActivity.this, FlatsActivity.class);
                intent.putExtra("blockId", flat.blockId);
                startActivity(intent);
            });
        }



        binding.boardButton.setOnClickListener(v -> {
            if (isCommonSpace) {
                Intent intent = new Intent(RoomActivity.this, BoardCommonSpace.class);
                intent.putExtra("flatId", flatId);
                intent.putExtra("blockId", flat.blockId);

                intent.putExtra("commonSpace", 1);

                startActivity(intent);
            } else {
                Intent intent = new Intent(RoomActivity.this, BoardActivity.class);
                if (catalogId != -1) {
                    intent.putExtra("catalogId", catalogId);

                }
                intent.putExtra("flatId", flat.id);
                startActivity(intent);
            }


        });

        binding.notesButton.setOnClickListener(v -> {
            if (isCommonSpace) {
                Intent intent = new Intent(RoomActivity.this, BoardCommonSpace.class);
                intent.putExtra("flatId", flatId);
                intent.putExtra("blockId", flat.blockId);

                intent.putExtra("commonSpace", 1);

                startActivity(intent);
            } else {
                Intent intent = new Intent(RoomActivity.this, NotesActivity.class);
                if (catalogId != -1) {
                    intent.putExtra("catalogId", catalogId);

                }
                intent.putExtra("flatId", flat.id);
                startActivity(intent);
            }
        });
        if (isCommonSpace) {
            binding.RCDButton.setVisibility(View.GONE);
        } else {
            binding.RCDButton.setOnClickListener(v -> {
                Intent intent = new Intent(RoomActivity.this, RCDActivity.class);
                if (catalogId != -1) {
                    intent.putExtra("catalogId", catalogId);
                }
                intent.putExtra("flatId", flat.id);
                startActivity(intent);
            });
        }


        binding.backButton.setOnClickListener(v -> {
            if (flat == null) return;

            else if (isCommonSpace) {
                Intent intent = new Intent(RoomActivity.this, BlockActivity.class);
                intent.putExtra("blockId", flat.blockId);
                startActivity(intent);
            }
            else if (catalogId != -1) {
                Intent intent = new Intent(RoomActivity.this, TemplatesActivity.class);
                intent.putExtra("catalogId", catalogId);
                startActivity(intent);
            } else {
                Intent intent = new Intent(RoomActivity.this, FlatsActivity.class);
                intent.putExtra("blockId", flat.blockId);
                startActivity(intent);
            }
        });
    }

    private void setupAddRoomUi() {
        binding.addRoomButton.setBackgroundTintList(
                ContextCompat.getColorStateList(this, android.R.color.holo_green_dark)
        );

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roomNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.roomSpinner.setAdapter(adapter);

        binding.roomSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String sel = roomNames[position];
                if ("Inne".equals(sel)) {
                    binding.customRoomEditText.setVisibility(View.VISIBLE);
                    binding.customRoomEditText.requestFocus();
                    showKeyboard(binding.customRoomEditText);

                } else if ("Piętro -".equals(sel)) {
                    binding.customRoomEditText.setVisibility(View.VISIBLE);
                    binding.customRoomEditText.setText("Piętro ");
                    binding.customRoomEditText.requestFocus();
                    showKeyboard(binding.customRoomEditText);
                }
                else {
                    hideKeyboard();
                    binding.customRoomEditText.setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        binding.customRoomEditText.setVisibility(View.GONE);
        binding.customRoomEditText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                binding.customRoomEditText.requestFocus();
                showKeyboard(binding.customRoomEditText);
            }
            return false;
        });

        binding.addRoomButton.setOnClickListener(v -> {
            if (flatId == -1) return;
            String name;
            int pos = binding.roomSpinner.getSelectedItemPosition();
            if (pos >= 0 && pos < roomNames.length && "Inne".equals(roomNames[pos])) {
                name = binding.customRoomEditText.getText() != null ? binding.customRoomEditText.getText().toString().trim() : "";
                if (name.isEmpty()) {
                    if (isCommonSpace ) {
                        binding.customRoomEditText.setError("Wpisz nazwę pomieszczenia");

                    } else {

                        binding.customRoomEditText.setError("Wpisz nazwę pokoju");
                    }
                    binding.customRoomEditText.requestFocus();
                    return;
                }
            } else {
                name = roomNames[pos];
            }
            RoomInFlat room = new RoomInFlat();
            room.flatId = flatId;
            room.name = name;
            roomViewModel.insert(room);
            binding.customRoomEditText.setText("");
            binding.roomSpinner.setSelection(0);
            hideKeyboard();
            binding.customRoomEditText.setVisibility(View.GONE);
        });
    }

    private void setupRecyclerView() {
        roomAdapter = new RoomAdapter(
                roomViewModel,
                outletViewModel,
                this, // LifecycleOwner
                this, // Context
                applianceOptions,
                breakerTypes,
                noteOptions,
                ampsOptions,
                this::onDeleteRoomClicked,
                this::onAddMeasurementClicked,
                catalogId,
                isCommonSpace
        );
        binding.roomRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.roomRecyclerView.setAdapter(roomAdapter);
    }

    private void observeRooms() {
        roomViewModel.getRoomsForFlat(flatId).observe(this, rooms -> {
            roomMeasurementsMap.clear();
            if (rooms != null) {
                roomAdapter.submitList(rooms);
                for (RoomInFlat room : rooms) {
                    observeMeasurementsForRoom(room.id);
                }
            } else {
                roomAdapter.submitList(new ArrayList<>());
            }
        });
    }

    private void observeMeasurementsForRoom(int roomId) {
        outletViewModel.getMeasurementsForRoom(roomId).observe(this, measurements -> {
            roomMeasurementsMap.put(roomId, measurements != null ? new ArrayList<>(measurements) : new ArrayList<>());
            recomputeGlobalDefaults();
        });
    }

    private void onAddMeasurementClicked(int roomId) {
        OutletMeasurement newOm = new OutletMeasurement();
        newOm.roomId = roomId;
        newOm.appliance = applianceOptions[0];
        newOm.switchName = "";
        newOm.breakerType = null;
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
        } else {
            newOm.amps = 16.0;
        }

        outletViewModel.insert(newOm, lastId -> {
            roomAdapter.setNewlyAddedMeasurementId(lastId);
        });
    }

    private void onDeleteRoomClicked(RoomInFlat room) {
        String name;
        if (isCommonSpace) {
            name = "pomieszczenie";
        } else {
            name = "pokój";
        }
        new AlertDialog.Builder(this)
                .setTitle("Usuń " + name)
                .setMessage("Czy na pewno chcesz usunąć  " + name + room.name + " wraz ze wszystkimi pomiarami?")
                .setPositiveButton("Usuń", (dialog, which) -> roomViewModel.delete(room))
                .setNegativeButton("Anuluj", null)
                .show();
    }


    private void observeAllMeasurements() {

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
    private void setupCommonSpaceInfoLogic() {
        if (flat == null) return;

        // Inicjalizacja Spinnerów nowej sekcji
        ArrayAdapter<String> breakerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, breakerTypes);
        breakerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.csBreakerSpinner.setAdapter(breakerAdapter);

        ArrayAdapter<String> ampsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ampsOptions);
        ampsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.csAmpsSpinner.setAdapter(ampsAdapter);

        // Obserwowanie danych z tabeli dla konkretnego blockId
        commonSpaceInfoViewModel.getInfoByBlockId(flat.blockId).observe(this, infoList -> {
            if (isUiUpdating) return;

            isUiUpdating = true;
            if (infoList != null && !infoList.isEmpty()) {
                currentCommonSpaceInfo = infoList.get(0);

                // Wypełnienie pól aktualnymi danymi z bazy
                binding.csSwitchEdit.setText(currentCommonSpaceInfo.switchName);

                int breakerPos = breakerAdapter.getPosition(currentCommonSpaceInfo.breakerType);
                if (breakerPos >= 0) binding.csBreakerSpinner.setSelection(breakerPos);

                String ampStr = currentCommonSpaceInfo.amps != null ? String.valueOf(currentCommonSpaceInfo.amps.intValue()) : "16";
                int ampsPos = ampsAdapter.getPosition(ampStr);
                if (ampsPos >= 0) binding.csAmpsSpinner.setSelection(ampsPos);
            } else {
                // Brak rekordu -> tworzymy nowy obiekt bazowy (zapisze się przy pierwszej edycji)
                currentCommonSpaceInfo = new CommonSpaceInfo();
                currentCommonSpaceInfo.blockId = flat.blockId;
                currentCommonSpaceInfo.amps = 16.0;
                currentCommonSpaceInfo.breakerType = "B";
                currentCommonSpaceInfo.switchName = "";
            }
            isUiUpdating = false;
        });
        binding.saveChanges.setOnClickListener(v -> {
            boolean change = false;
            String switchName = binding.csSwitchEdit.getText().toString();
            if (!switchName.isEmpty() && !switchName.equals(currentCommonSpaceInfo.switchName)) {
                currentCommonSpaceInfo.switchName = switchName;
                change = true;
            }
            if (Double.parseDouble(binding.csAmpsSpinner.getSelectedItem().toString()) != currentCommonSpaceInfo.amps) {
                change = true;
                currentCommonSpaceInfo.amps = Double.parseDouble(binding.csAmpsSpinner.getSelectedItem().toString());

            }
            if (binding.csBreakerSpinner.getSelectedItem().toString().equals(currentCommonSpaceInfo.breakerType)) {
                change = true;
                currentCommonSpaceInfo.breakerType = binding.csBreakerSpinner.getSelectedItem().toString();
            }

            if (change) {
                saveOrUpdateCommonSpaceInfo();
            }
        });

        binding.generateButton.setOnClickListener(v -> {
            boolean change = false;
            String switchName = binding.csSwitchEdit.getText().toString();
            if (!switchName.isEmpty() && !switchName.equals(currentCommonSpaceInfo.switchName)) {
                currentCommonSpaceInfo.switchName = switchName;
                change = true;
            }
            if (Double.parseDouble(binding.csAmpsSpinner.getSelectedItem().toString()) != currentCommonSpaceInfo.amps) {
                change = true;
                currentCommonSpaceInfo.amps = Double.parseDouble(binding.csAmpsSpinner.getSelectedItem().toString());

            }
            if (binding.csBreakerSpinner.getSelectedItem().toString().equals(currentCommonSpaceInfo.breakerType)) {
                change = true;
                currentCommonSpaceInfo.breakerType = binding.csBreakerSpinner.getSelectedItem().toString();
            }

            if (change) {
                saveOrUpdateCommonSpaceInfo();
            }
        });

    }
    private void generateMeasurements() {
        roomViewModel.getOrCreateMainRoom(flatId, room -> {

        });
    }
    private void updateFlatsMeasurements() {

    }
    private void saveOrUpdateCommonSpaceInfo() {
        if (currentCommonSpaceInfo.id == 0) {
            // Brak ID oznacza, że rekord jeszcze nie istnieje w tabeli -> INSERT
            commonSpaceInfoViewModel.insert(currentCommonSpaceInfo);
        } else {
            // Rekord już istnieje -> UPDATE
            commonSpaceInfoViewModel.update(currentCommonSpaceInfo);
        }
    }
    public void showKeyboard(View view) {
        view.post(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View focused = getCurrentFocus();
        if (imm != null && focused != null) {
            imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }
    }

    public static class OhmsTextWatcher implements TextWatcher {
        private final EditText editText;
        private String current = "";

        public OhmsTextWatcher(EditText editText) { this.editText = editText; }

        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override public void afterTextChanged(Editable s) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.toString().equals(current)) {
                return;
            }
            editText.removeTextChangedListener(this);

            String cleanString = s.toString().replaceAll("[^\\d]", "");

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