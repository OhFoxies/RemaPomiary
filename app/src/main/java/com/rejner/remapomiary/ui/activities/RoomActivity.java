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
import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.data.entities.OutletMeasurement;
import com.rejner.remapomiary.data.entities.RoomInFlat;
import com.rejner.remapomiary.data.utils.LiveDataUtil;
import com.rejner.remapomiary.databinding.ActivityRoomBinding;
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

    // Przeniesiono tablice opcji tutaj, aby można je było łatwo przekazać do adapterów
    public final String[] roomNames = {"Pokój", "Korytarz", "Łazienka", "Kuchnia", "Inne"};
    public final String[] applianceOptions = {"Gniazdko", "Lodówka", "Pralka", "Grzejnik", "Inne"};
    public final String[] breakerTypes = {"B", "C", "D", "Gg"};
    public final String[] noteOptions = {"brak uwag", "nie podłączony bolec", "Urwane", "Inne"};
    public final String[] ampsOptions = {"3", "6", "10", "16", "20", "25", "32", "40"};

    private final Map<Integer, List<OutletMeasurement>> roomMeasurementsMap = new HashMap<>();
    private String lastDefaultSwitchName = null;
    private String lastDefaultBreakerType = null;
    private Double lastDefaultAmps = null;
    private int catalogId;
    private long newlyAddedMeasurementId = -1;
    private FlatViewModel flatViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Użyj View Binding
        binding = ActivityRoomBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        flatId = getIntent().getIntExtra("flatId", -1);
        catalogId = getIntent().getIntExtra("catalogId", -1);
        roomViewModel = new ViewModelProvider(this).get(RoomViewModel.class);
        outletViewModel = new ViewModelProvider(this).get(OutletMeasurementViewModel.class);
        flatViewModel = new ViewModelProvider(this).get(FlatViewModel.class);

        LiveDataUtil.observeOnce(flatViewModel.getFlatById(flatId), this, flat1 -> {
            flat = flat1;
            runOnUiThread(this::setupUIElements);

        });

        setupAddRoomUi();
        setupRecyclerView();
        observeRooms();
        observeAllMeasurements(); // Do obliczania wartości domyślnych
    }

    private void setupUIElements() {
        if (flat == null) return;
        binding.flatTitle.setText("Mieszkanie numer - " + flat.number + " pętla zwarcia");
        if (catalogId != -1) {
            binding.notesButton.setVisibility(View.GONE);
        }

        binding.backSave.setOnClickListener(v -> {
            flat.status = "Pomiar gotowy ✅";
            flat.edition_date = new Date();
            flatViewModel.update(flat);
            Intent intent = new Intent(RoomActivity.this, FlatsActivity.class);
            intent.putExtra("blockId", flat.blockId);
            startActivity(intent);
        });


        binding.boardButton.setOnClickListener(v -> {
            Intent intent = new Intent(RoomActivity.this, BoardActivity.class);
            if (catalogId != -1) {
                intent.putExtra("catalogId", catalogId);

            }
            intent.putExtra("flatId", flat.id);
            startActivity(intent);
        });

        binding.notesButton.setOnClickListener(v -> {
            Intent intent = new Intent(RoomActivity.this, NotesActivity.class);
            if (catalogId != -1) {
                intent.putExtra("catalogId", catalogId);

            }
            intent.putExtra("flatId", flat.id);
            startActivity(intent);
        });

        binding.RCDButton.setOnClickListener(v -> {
            Intent intent = new Intent(RoomActivity.this, RCDActivity.class);
            if (catalogId != -1) {
                intent.putExtra("catalogId", catalogId);

            }
            intent.putExtra("flatId", flat.id);
            startActivity(intent);
        });

        binding.backButton.setOnClickListener(v -> {
            if (flat == null) return;
            if (catalogId != -1) {
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
                } else {
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
                    binding.customRoomEditText.setError("Wpisz nazwę pokoju");
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
                // Przekazanie opcji do adapterów
                applianceOptions,
                breakerTypes,
                noteOptions,
                ampsOptions,
                // Przekazanie listenerów
                this::onDeleteRoomClicked,
                this::onAddMeasurementClicked,
                catalogId
        );
        binding.roomRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.roomRecyclerView.setAdapter(roomAdapter);
    }

    private void observeRooms() {
        roomViewModel.getRoomsForFlat(flatId).observe(this, rooms -> {
            roomMeasurementsMap.clear(); // Wyczyść mapę przy aktualizacji listy pokoi
            if (rooms != null) {
                roomAdapter.submitList(rooms);
                // Dla każdego pokoju obserwuj jego pomiary (do obliczeń domyślnych)
                for (RoomInFlat room : rooms) {
                    observeMeasurementsForRoom(room.id);
                }
            } else {
                roomAdapter.submitList(new ArrayList<>());
            }
        });
    }

    // Obserwuje pomiary TYLKO na potrzeby obliczania wartości domyślnych
    private void observeMeasurementsForRoom(int roomId) {
        outletViewModel.getMeasurementsForRoom(roomId).observe(this, measurements -> {
            roomMeasurementsMap.put(roomId, measurements != null ? new ArrayList<>(measurements) : new ArrayList<>());
            recomputeGlobalDefaults();
        });
    }

    // Ta metoda jest teraz wywoływana z adaptera, ale zachowuje logikę domyślnych
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
            // Przekaż ID do adaptera, aby mógł ustawić fokus
            roomAdapter.setNewlyAddedMeasurementId(lastId);
        });
    }

    // Wywoływane z adaptera
    private void onDeleteRoomClicked(RoomInFlat room) {
        new AlertDialog.Builder(this)
                .setTitle("Usuń pokój")
                .setMessage("Czy na pewno chcesz usunąć pokój "+ room.name + " wraz ze wszystkimi pomiarami?")
                .setPositiveButton("Usuń", (dialog, which) -> roomViewModel.delete(room))
                .setNegativeButton("Anuluj", null)
                .show();
    }

    // --- Logika wartości domyślnych (pozostaje w Activity) ---

    // Ta obserwacja jest potrzebna tylko do wyzwalania `recomputeGlobalDefaults`
    // Alternatywnie, `observeRooms` może to robić
    private void observeAllMeasurements() {
        // To jest uproszczenie. W idealnym świecie ViewModel agregowałby te dane.
        // Na razie będziemy polegać na `observeMeasurementsForRoom` wywoływanym z `observeRooms`.
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

    // --- Metody pomocnicze (publiczne, by adaptery miały dostęp) ---

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

    // --- OhmsTextWatcher (bez zmian, zgodnie z prośbą) ---
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