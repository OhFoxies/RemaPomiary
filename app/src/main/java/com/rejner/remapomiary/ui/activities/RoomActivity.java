package com.rejner.remapomiary.ui.activities;

import static com.rejner.remapomiary.ui.utils.Actions.randomOhms;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.rejner.remapomiary.R;
import com.rejner.remapomiary.adapters.RoomAdapter;
import com.rejner.remapomiary.data.entities.CommonSpaceInfo;
import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.data.entities.FlatFullData;
import com.rejner.remapomiary.data.entities.OutletMeasurement;
import com.rejner.remapomiary.data.entities.RoomInFlat;
import com.rejner.remapomiary.data.utils.LiveDataUtil;
import com.rejner.remapomiary.databinding.ActivityRoomBinding;
import com.rejner.remapomiary.ui.utils.Actions;
import com.rejner.remapomiary.ui.utils.Settings;
import com.rejner.remapomiary.ui.viewmodels.CommonSpaceInfoViewModel;
import com.rejner.remapomiary.ui.viewmodels.FlatViewModel;
import com.rejner.remapomiary.ui.viewmodels.OutletMeasurementViewModel;
import com.rejner.remapomiary.ui.viewmodels.RoomViewModel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RoomActivity extends AppCompatActivity {

    private ActivityRoomBinding binding;
    private RoomViewModel roomViewModel;
    private OutletMeasurementViewModel outletViewModel;
    private RoomAdapter roomAdapter;
    private int flatId;
    private Flat flat;
    private com.google.android.material.floatingactionbutton.FloatingActionButton scrollToTopButton;

    public String[] roomNames = {"Pokój", "Sypialnia", "Korytarz", "Łazienka", "Kuchnia", "Inne"};
    public String[] applianceOptions = {"Gniazdko", "Lodówka", "Piekarnik", "Telewizor", "Pralka", "Grzejnik", "Inne"};
    public final String[] breakerTypes = {"B", "C", "D", "Gg"};
    public final String[] noteOptions = {Settings.noNotes, Settings.noGroundingBolt, "Urwane", Settings.brokenOutlet, "Inne"};
    public final String[] ampsOptions = {"3", "6", "10", "16", "20", "25", "32", "40"};

    private final Map<Integer, List<OutletMeasurement>> roomMeasurementsMap = new HashMap<>();

    private final Map<Integer, LiveData<List<OutletMeasurement>>> observedRoomLiveData = new HashMap<>();
    private final Map<Integer, Observer<List<OutletMeasurement>>> roomObserversMap = new HashMap<>();

    private volatile String lastDefaultSwitchName = null;
    private volatile String lastDefaultBreakerType = null;
    private volatile Double lastDefaultAmps = null;
    private CommonSpaceInfo currentCommonSpaceInfo;
    private int catalogId;
    private boolean isCommonSpace;
    private CommonSpaceInfoViewModel commonSpaceInfoViewModel;
    private String blockName;
    private FlatViewModel flatViewModel;

    private ActivityResultLauncher<Uri> takePhotoLauncher;
    private OutletMeasurement measurementPendingPhoto;
    private File tempPhotoFile;

    // Pula wątków dla obliczeń domyślnych w tle (zapobieganie zamrażaniu UI przy starcie)
    private final ExecutorService defaultCalcExecutor = Executors.newSingleThreadExecutor();

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

        takePhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && measurementPendingPhoto != null && tempPhotoFile != null) {
                        OutletMeasurement updateMe = measurementPendingPhoto.copy();
                        updateMe.photoPath = tempPhotoFile.getAbsolutePath();

                        outletViewModel.update(updateMe, null);
                        Toast.makeText(this, "Dodano zdjęcie do pomiaru", Toast.LENGTH_SHORT).show();
                    }
                    measurementPendingPhoto = null;
                    tempPhotoFile = null;
                }
        );

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
        setupScrollToTop();
        observeRooms();
    }

    private void setupScrollToTop() {
        scrollToTopButton = findViewById(R.id.scrollToTopButton);
        scrollToTopButton.setOnClickListener(v -> {
            binding.roomRecyclerView.smoothScrollToPosition(0);
            binding.appBarLayout.setExpanded(true, true);
        });

        binding.roomRecyclerView.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull androidx.recyclerview.widget.RecyclerView recyclerView, int dx, int dy) {
                if (recyclerView.computeVerticalScrollOffset() > 150) {
                    scrollToTopButton.show();
                } else {
                    scrollToTopButton.hide();
                }
            }
        });
    }

    private void setupUIElements() {
        if (flat == null) return;
        if (isCommonSpace) {
            binding.flatTitle.setText("Pętla zwarcia - " + blockName);
            binding.customRoomEditText.setHint("Podaj nazwę pomieszczenia");
            binding.addRoomButton.setText("Dodaj pomieszczenie");
            binding.commonSpaceInfoContainer.setVisibility(View.VISIBLE);
            setupCommonSpaceInfoLogic();
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
                Actions.saveAndMarkReady(flat, this);
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
                intent.putExtra("name", blockName);
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
            if (isCommonSpace) {
                Intent intent = new Intent(RoomActivity.this, BlockActivity.class);
                intent.putExtra("blockId", flat.blockId);
                startActivity(intent);
            } else if (catalogId != -1) {
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
                    binding.customRoomEditText.setSelection(binding.customRoomEditText.getText().length());
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
            if (pos >= 0 && pos < roomNames.length && "Inne".equals(roomNames[pos]) || "Piętro -".equals(roomNames[pos])) {
                name = binding.customRoomEditText.getText() != null ? binding.customRoomEditText.getText().toString().trim() : "";
                if (name.isEmpty()) {
                    if (isCommonSpace) {
                        binding.customRoomEditText.setError("Wpisz nazwę pomieszczenia");
                        Toast.makeText(this, "Podaj nazwe pomieszczenia", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Podaj nazwe pokoju", Toast.LENGTH_SHORT).show();
                        binding.customRoomEditText.setError("Wpisz nazwę pokoju");
                    }
                    binding.customRoomEditText.requestFocus();
                    return;
                }
            } else {
                name = roomNames[pos];
            }
            if (isCommonSpace && name.equals(Settings.mainRoomName)) {
                Toast.makeText(this, "Nie możesz tak nazwać pomieszczenia", Toast.LENGTH_SHORT).show();
                return;
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
                this,
                this,
                applianceOptions,
                breakerTypes,
                noteOptions,
                ampsOptions,
                this::onDeleteRoomClicked,
                this::onAddMeasurementClicked,
                this::onAddMeasurementPhoto,
                catalogId,
                isCommonSpace
        );
        binding.roomRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.roomRecyclerView.setAdapter(roomAdapter);
    }

    private void observeRooms() {
        roomViewModel.getRoomsForFlat(flatId).observe(this, rooms -> {
            Set<Integer> currentRoomIds = new HashSet<>();
            if (rooms != null) {
                for (RoomInFlat r : rooms) currentRoomIds.add(r.id);
            }

            List<Integer> idsToRemove = new ArrayList<>();
            for (Integer oldId : roomObserversMap.keySet()) {
                if (!currentRoomIds.contains(oldId)) {
                    idsToRemove.add(oldId);
                }
            }
            for (Integer id : idsToRemove) {
                LiveData<List<OutletMeasurement>> ld = observedRoomLiveData.remove(id);
                Observer<List<OutletMeasurement>> obs = roomObserversMap.remove(id);
                if (ld != null && obs != null) {
                    ld.removeObserver(obs);
                }
                roomMeasurementsMap.remove(id);
            }

            if (rooms != null) {
                roomAdapter.submitList(rooms);
                for (RoomInFlat room : rooms) {
                    if (!roomObserversMap.containsKey(room.id)) {
                        observeMeasurementsForRoom(room.id);
                    }
                }
            } else {
                roomAdapter.submitList(new ArrayList<>());
            }
        });
    }

    private void observeMeasurementsForRoom(int roomId) {
        LiveData<List<OutletMeasurement>> liveData = outletViewModel.getMeasurementsForRoom(roomId);
        Observer<List<OutletMeasurement>> observer = measurements -> {
            roomMeasurementsMap.put(roomId, measurements != null ? new ArrayList<>(measurements) : new ArrayList<>());
            recomputeGlobalDefaults();
        };
        observedRoomLiveData.put(roomId, liveData);
        roomObserversMap.put(roomId, observer);
        liveData.observe(this, observer);
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

        String sw = lastDefaultSwitchName;
        String br = lastDefaultBreakerType;
        Double am = lastDefaultAmps;

        if (sw != null && (newOm.switchName == null || newOm.switchName.trim().isEmpty())) {
            newOm.switchName = sw;
        }
        if (br != null && (newOm.breakerType == null || newOm.breakerType.trim().isEmpty())) {
            newOm.breakerType = br;
        }
        if (am != null && (newOm.amps == null || newOm.amps <= 0)) {
            newOm.amps = am;
        } else {
            newOm.amps = 16.0;
        }

        outletViewModel.insert(newOm, lastId -> {
            roomAdapter.setNewlyAddedMeasurementId(lastId);
        });
    }

    private void onAddMeasurementPhoto(OutletMeasurement measurement) {
        measurementPendingPhoto = measurement;
        try {
            String fileName = "POMIAR_" + measurement.id + "_" + System.currentTimeMillis();
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            tempPhotoFile = File.createTempFile(fileName, ".jpg", storageDir);

            Uri photoURI = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    tempPhotoFile
            );
            takePhotoLauncher.launch(photoURI);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Błąd uruchamiania aparatu", Toast.LENGTH_SHORT).show();
        }
    }

    private void onDeleteRoomClicked(RoomInFlat room) {
        String name;
        if (isCommonSpace) {
            name = "pomieszczenie";
            if (room.name.equals(Settings.mainRoomName)) {
                Toast.makeText(this, "Nie można usunąć tego pomieszczenia", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            name = "pokój";
        }

        new AlertDialog.Builder(this)
                .setTitle("Usuń " + name)
                .setMessage("Czy na pewno chcesz usunąć  " + name + " " + room.name + " wraz ze wszystkimi pomiarami?")
                .setPositiveButton("Usuń", (dialog, which) -> roomViewModel.delete(room))
                .setNegativeButton("Anuluj", null)
                .show();
    }

    // Zoptymalizowano: Przeniesiono ciężkie obliczenia statystyczne do wątku tła
    private void recomputeGlobalDefaults() {
        // Robimy bezpieczną kopię danych, aby uniknąć ConcurrentModificationException na innym wątku
        final List<List<OutletMeasurement>> snapshotLists = new ArrayList<>();
        for (List<OutletMeasurement> list : roomMeasurementsMap.values()) {
            if (list != null) {
                snapshotLists.add(new ArrayList<>(list));
            }
        }

        defaultCalcExecutor.execute(() -> {
            Map<String, Integer> switchFreq = new HashMap<>();
            Map<String, Integer> breakerFreq = new HashMap<>();
            Map<Integer, Integer> ampsFreq = new HashMap<>();

            for (List<OutletMeasurement> list : snapshotLists) {
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
        });
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

        binding.csBaseValue.addTextChangedListener(new OhmsTextWatcher(binding.csBaseValue));

        View.OnFocusChangeListener focusChangeListener = (v, hasFocus) -> {
            if (hasFocus) {
                binding.appBarLayout.setExpanded(true, true);
            }
        };
        binding.csSwitchEdit.setOnFocusChangeListener(focusChangeListener);
        binding.csBaseValue.setOnFocusChangeListener(focusChangeListener);

        ArrayAdapter<String> breakerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, breakerTypes);
        breakerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.csBreakerSpinner.setAdapter(breakerAdapter);

        ArrayAdapter<String> ampsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ampsOptions);
        ampsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.csAmpsSpinner.setAdapter(ampsAdapter);

        commonSpaceInfoViewModel.getInfoByBlockId(flat.blockId).observe(this, infoList -> {
            if (infoList != null && !infoList.isEmpty()) {
                currentCommonSpaceInfo = infoList.get(0);

                binding.csSwitchEdit.setText(currentCommonSpaceInfo.switchName);

                int breakerPos = breakerAdapter.getPosition(currentCommonSpaceInfo.breakerType);
                if (breakerPos >= 0) binding.csBreakerSpinner.setSelection(breakerPos);

                String ohmsStr = currentCommonSpaceInfo.ohmsBase != null ?
                        String.valueOf(currentCommonSpaceInfo.ohmsBase).replace(".", ",") : "0,00";
                binding.csBaseValue.setText(ohmsStr);

                String ampStr = currentCommonSpaceInfo.amps != null ? String.valueOf(currentCommonSpaceInfo.amps.intValue()) : "16";
                int ampsPos = ampsAdapter.getPosition(ampStr);
                if (ampsPos >= 0) binding.csAmpsSpinner.setSelection(ampsPos);
            } else {
                currentCommonSpaceInfo = new CommonSpaceInfo();
                currentCommonSpaceInfo.blockId = flat.blockId;
                currentCommonSpaceInfo.amps = 16.0;
                currentCommonSpaceInfo.breakerType = "B";
                currentCommonSpaceInfo.switchName = "";
                currentCommonSpaceInfo.ohmsBase = 0.0;
                binding.csBaseValue.setText("0,00");
            }
        });

        binding.generateButton.setOnClickListener(v -> {
            boolean change = false;
            String switchName = binding.csSwitchEdit.getText().toString().trim();

            if (switchName.isEmpty()) {
                binding.csSwitchEdit.setError("Podaj nazwe");
                Toast.makeText(this, "Podaj nazwę wyłącznika", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!switchName.equals(currentCommonSpaceInfo.switchName)) {
                currentCommonSpaceInfo.switchName = switchName;
                binding.csSwitchEdit.clearFocus();
                change = true;
            }

            double enteredOhms = safeParseDouble(binding.csBaseValue.getText().toString());
            if (enteredOhms != currentCommonSpaceInfo.ohmsBase) {
                change = true;
                currentCommonSpaceInfo.ohmsBase = enteredOhms;
            }

            try {
                double enteredAmps = Double.parseDouble(binding.csAmpsSpinner.getSelectedItem().toString());
                if (enteredAmps != currentCommonSpaceInfo.amps) {
                    change = true;
                    currentCommonSpaceInfo.amps = enteredAmps;
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

            String selectedBreaker = binding.csBreakerSpinner.getSelectedItem().toString();
            if (!selectedBreaker.equals(currentCommonSpaceInfo.breakerType)) {
                change = true;
                currentCommonSpaceInfo.breakerType = selectedBreaker;
            }

            hideKeyboard();
            binding.csBaseValue.clearFocus();
            binding.csSwitchEdit.clearFocus();

            if (change) {
                if (currentCommonSpaceInfo.ohmsBase != 0.0) {
                    saveOrUpdateCommonSpaceInfo();
                    Toast.makeText(this, "Dane pętli zwarcia zostały zaktualizowane", Toast.LENGTH_SHORT).show();
                } else {
                    binding.csBaseValue.setError("Podaj wartość bazową omów");
                    Toast.makeText(this, "Podaj wartość bazową", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Brak zmian, brak efektu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private double safeParseDouble(String text) {
        if (text == null || text.trim().isEmpty()) return 0.0;
        try {
            String cleanString = text.replaceAll("[^0-9,.]", "").replace(",", ".");
            return cleanString.isEmpty() ? 0.0 : Double.parseDouble(cleanString);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    private void generateMeasurements() {
        if (flat == null || currentCommonSpaceInfo == null) return;

        roomViewModel.getOrCreateMainRoom(flatId, room -> {
            flatViewModel.getFlatsSync(flat.blockId, flats -> {
                for (FlatFullData ffd : flats) {
                    if (ffd.flat.isCommonSpace == 1) continue;

                    String applianceName = "Lokal - " + ffd.flat.number;
                    OutletMeasurement om = outletViewModel.getOutletMeasurementSync(room.id, applianceName);

                    if (om == null) {
                        OutletMeasurement om_new = new OutletMeasurement();
                        om_new.roomId = room.id;
                        om_new.appliance = applianceName;
                        om_new.switchName = currentCommonSpaceInfo.switchName;
                        om_new.breakerType = currentCommonSpaceInfo.breakerType;
                        om_new.amps = currentCommonSpaceInfo.amps;

                        int number;
                        try {
                            number = Integer.parseInt(ffd.flat.number);
                        } catch (NumberFormatException e) {
                            number = 10;
                        }
                        om_new.ohms = Math.round(
                                (((number - 1) / 20.0) * 0.05 + currentCommonSpaceInfo.ohmsBase + randomOhms())
                                        * 100.0
                        ) / 100.0;

                        if (ffd.flat.status.equals(Settings.measurementDone)) {
                            om_new.note = Settings.noNotes;
                        } else {
                            om_new.note = Settings.flatNoAccess;
                            om_new.ohms = 0.0;
                        }
                        om_new.number = number;

                        outletViewModel.insert(om_new, null);
                    } else {
                        om.switchName = currentCommonSpaceInfo.switchName;
                        om.breakerType = currentCommonSpaceInfo.breakerType;
                        om.amps = currentCommonSpaceInfo.amps;

                        int number;
                        try {
                            number = Integer.parseInt(ffd.flat.number);
                        } catch (NumberFormatException e) {
                            number = 10;
                        }
                        om.ohms = Math.round(
                                (((number - 1) / 20.0) * 0.05 + currentCommonSpaceInfo.ohmsBase + randomOhms())
                                        * 100.0
                        ) / 100.0;
                        if (ffd.flat.status.equals(Settings.measurementDone)) {
                            om.note = Settings.noNotes;
                        } else {
                            om.note = Settings.flatNoAccess;
                            om.ohms = 0.0;
                        }
                        om.number = number;

                        outletViewModel.update(om, null);
                    }
                }
            });
        });
    }

    private void saveOrUpdateCommonSpaceInfo() {
        if (currentCommonSpaceInfo.id == 0) {
            commonSpaceInfoViewModel.insert(currentCommonSpaceInfo);
        } else {
            commonSpaceInfoViewModel.update(currentCommonSpaceInfo);
        }
        generateMeasurements();
    }

    public void showKeyboard(View view) {
        view.postDelayed(() -> {
            view.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(view, 0);
            }
        }, 100);
    }

    public void hideKeyboard() {
        hideKeyboard(getCurrentFocus());
    }

    public void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        defaultCalcExecutor.shutdown(); // Czyszczenie zasobów w celu uniknięcia leaków pamięci
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