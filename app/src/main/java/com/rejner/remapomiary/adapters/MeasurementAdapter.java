// MeasurementAdapter.java
package com.rejner.remapomiary.adapters;

import android.content.Context;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.rejner.remapomiary.data.entities.OutletMeasurement;
import com.rejner.remapomiary.databinding.MeasurementRowItemBinding;
import com.rejner.remapomiary.ui.activities.RoomActivity;
import com.rejner.remapomiary.ui.utils.Settings;
import com.rejner.remapomiary.ui.viewmodels.OutletMeasurementViewModel;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MeasurementAdapter extends ListAdapter<OutletMeasurement, MeasurementAdapter.MeasurementViewHolder> {

    public interface OnMeasurementActionListener {
        void onAddPhoto(OutletMeasurement measurement);
    }

    private final OnMeasurementActionListener listener;
    private final RoomActivity activity;
    private final OutletMeasurementViewModel outletViewModel;
    private final String[] applianceOptions;
    private final String[] breakerTypes;
    private String[] noteOptions;
    private final String[] ampsOptions;
    private final int catalogId;
    private final boolean isCommonSpace;
    private long focusToMeasurementId = -1;
    private final String roomName;

    private final java.util.Set<Integer> expandedPhotoIds = new java.util.HashSet<>();

    private final ArrayAdapter<String> rcdStateAdapter;
    private final ArrayAdapter<String> applianceSpinnerAdapter;
    private final ArrayAdapter<String> breakerSpinnerAdapter;
    private final ArrayAdapter<String> noteSpinnerAdapter;
    private final ArrayAdapter<String> ampsSpinnerAdapter;

    // Jedna współdzielona pula wątków zamiast tworzenia nowej przy każdym kliknięciu spinnera
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    public MeasurementAdapter(RoomActivity activity, OutletMeasurementViewModel outletViewModel,
                              String[] applianceOptions, String[] breakerTypes, String[] noteOptions,
                              String[] ampsOptions, int catalogId, boolean isCommonSpace, String roomName,
                              OnMeasurementActionListener listener) {
        super(DIFF_CALLBACK);
        this.activity = activity;
        this.outletViewModel = outletViewModel;
        this.applianceOptions = applianceOptions;
        this.breakerTypes = breakerTypes;
        this.noteOptions = noteOptions;
        this.ampsOptions = ampsOptions;
        this.catalogId = catalogId;
        this.isCommonSpace = isCommonSpace;
        this.roomName = roomName;
        this.listener = listener;

        if (Settings.mainRoomName.equalsIgnoreCase(roomName)) {
            this.noteOptions = new String[]{Settings.noNotes, "Inne"};
        }

        this.rcdStateAdapter = createSpinnerAdapter(new String[]{"Brak różnicówki", "Różnicówka sprawna", "Różnicówka niesprawna"});
        this.applianceSpinnerAdapter = createSpinnerAdapter(this.applianceOptions);
        this.breakerSpinnerAdapter = createSpinnerAdapter(this.breakerTypes);
        this.noteSpinnerAdapter = createSpinnerAdapter(this.noteOptions);
        this.ampsSpinnerAdapter = createSpinnerAdapter(this.ampsOptions);
    }

    private ArrayAdapter<String> createSpinnerAdapter(String[] options) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    public void setFocusToMeasurementId(long id) {
        this.focusToMeasurementId = id;
    }

    @NonNull
    @Override
    public MeasurementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        MeasurementRowItemBinding binding = MeasurementRowItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new MeasurementViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MeasurementViewHolder holder, int position) {
        OutletMeasurement om = getItem(position);
        holder.bind(om);
    }

    class MeasurementViewHolder extends RecyclerView.ViewHolder {
        private final MeasurementRowItemBinding binding;
        private OutletMeasurement currentItem;
        private final RoomActivity.OhmsTextWatcher ohmsWatcher;

        private int currentAppliancePos = -1;
        private int currentBreakerPos = -1;
        private int currentAmpsPos = -1;
        private int currentNotePos = -1;
        private int currentRcdStatePos = -1;

        private boolean isBinding = false;

        MeasurementViewHolder(MeasurementRowItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            this.ohmsWatcher = new RoomActivity.OhmsTextWatcher(binding.ohmsEdit);

            binding.rcdStateSpinner.setAdapter(MeasurementAdapter.this.rcdStateAdapter);
            binding.applianceSpinner.setAdapter(MeasurementAdapter.this.applianceSpinnerAdapter);
            binding.breakerSpinner.setAdapter(MeasurementAdapter.this.breakerSpinnerAdapter);
            binding.ampsSpinner.setAdapter(MeasurementAdapter.this.ampsSpinnerAdapter);
            binding.noteSpinner.setAdapter(MeasurementAdapter.this.noteSpinnerAdapter);

            initAllListeners();
        }

        private void scrollToView(View view) {
            view.postDelayed(() -> {
                if (itemView.isAttachedToWindow()) {
                    view.requestRectangleOnScreen(new Rect(0, 0, view.getWidth(), view.getHeight() + (int)(200 * activity.getResources().getDisplayMetrics().density)), true);
                }
            }, 300);
        }

        private void initAllListeners() {
            binding.rcdStateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (isBinding || currentItem == null) return;
                    if (position == currentRcdStatePos) return;
                    currentRcdStatePos = position;
                    currentItem.rcdStatus = currentRcdStatePos;

                    if (position == 0) {
                        currentItem.rcdName = "";
                        currentItem.rcdTime = null;
                        currentItem.rcdCurrent = null;
                        binding.rcdNameEdit.setText("");
                        binding.rcdTimeEdit.setText("");
                        binding.rcdCurrentEdit.setText("");
                        outletViewModel.update(currentItem, null);
                        enableRcdEdits(false);
                    } else {
                        enableRcdEdits(true);
                        if (currentItem.rcdName == null || currentItem.rcdName.trim().isEmpty()) {
                            // OPTYMALIZACJA: Użycie stałego dbExecutor zamiast ciągłego tworzenia nowego wątku
                            dbExecutor.execute(() -> {
                                String fetchedName = outletViewModel.getLastRCDName(currentItem.roomId);
                                final String finalName = (fetchedName != null) ? fetchedName : "";

                                activity.runOnUiThread(() -> {
                                    if (currentItem != null) {
                                        currentItem.rcdName = finalName;
                                        binding.rcdNameEdit.setText(finalName);
                                        outletViewModel.update(currentItem, null);
                                    }
                                });
                            });
                        } else {
                            outletViewModel.update(currentItem, null);
                        }
                    }
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });

            binding.rcdNameEdit.setOnFocusChangeListener((v, hasFocus) -> {
                binding.rcdNameSaveBtn.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
                if (hasFocus) scrollToView(v);
            });

            binding.rcdNameEdit.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    binding.rcdNameSaveBtn.performClick();
                    return true;
                }
                return false;
            });
            binding.rcdNameSaveBtn.setOnClickListener(v -> {
                if (currentItem == null) return;
                String nameText = binding.rcdNameEdit.getText().toString().trim();
                currentItem.rcdName = nameText;
                outletViewModel.update(currentItem, null);
                activity.hideKeyboard(binding.rcdNameEdit);
                binding.rcdNameEdit.clearFocus();
            });

            binding.rcdTimeEdit.setOnFocusChangeListener((v, hasFocus) -> {
                binding.rcdTimeSaveBtn.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
                if (hasFocus) scrollToView(v);
            });
            binding.rcdTimeEdit.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    binding.rcdTimeSaveBtn.performClick();
                    return true;
                }
                return false;
            });
            binding.rcdTimeSaveBtn.setOnClickListener(v -> {
                if (currentItem == null) return;
                String timeText = binding.rcdTimeEdit.getText().toString().trim();
                if (timeText.isEmpty()) {
                    currentItem.rcdTime = null;
                } else {
                    try {
                        currentItem.rcdTime = Integer.parseInt(timeText);
                    } catch (NumberFormatException e) {
                        binding.rcdTimeEdit.setError("Błąd");
                        return;
                    }
                }
                outletViewModel.update(currentItem, null);
                activity.hideKeyboard(binding.rcdTimeEdit);
                binding.rcdTimeEdit.clearFocus();
            });

            binding.rcdCurrentEdit.setOnFocusChangeListener((v, hasFocus) -> {
                binding.rcdCurrentSaveBtn.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
                if (hasFocus) scrollToView(v);
            });
            binding.rcdCurrentEdit.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    binding.rcdCurrentSaveBtn.performClick();
                    return true;
                }
                return false;
            });
            binding.rcdCurrentSaveBtn.setOnClickListener(v -> {
                if (currentItem == null) return;
                String currentText = binding.rcdCurrentEdit.getText().toString().trim();
                if (currentText.isEmpty()) {
                    currentItem.rcdCurrent = null;
                } else {
                    try {
                        currentItem.rcdCurrent = Integer.parseInt(currentText);
                    } catch (NumberFormatException e) {
                        binding.rcdCurrentEdit.setError("Błąd");
                        return;
                    }
                }
                outletViewModel.update(currentItem, null);
                activity.hideKeyboard(binding.rcdCurrentEdit);
                binding.rcdCurrentEdit.clearFocus();
            });

            binding.applianceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (isBinding || currentItem == null) return;
                    if (position == currentAppliancePos) return;
                    currentAppliancePos = position;

                    String selected = applianceOptions[position];
                    if ("Inne".equals(selected)) {
                        binding.applianceSpinner.setVisibility(View.GONE);
                        binding.customApplianceContainer.setVisibility(View.VISIBLE);
                        binding.customApplianceEdit.requestFocus();
                        activity.showKeyboard(binding.customApplianceEdit);
                    } else if (!selected.equals(currentItem.appliance)) {
                        currentItem.appliance = selected;
                        outletViewModel.update(currentItem, null);
                    }
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });

            binding.customApplianceEdit.setOnFocusChangeListener((v, hasFocus) -> {
                toggleFieldExpansion(binding.applianceContainer, binding.switchContainer, hasFocus, 2f, 2f);
                binding.customApplianceSaveBtn.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
                binding.customApplianceClearBtn.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
                if (hasFocus) scrollToView(v);
            });

            binding.customApplianceEdit.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    binding.customApplianceSaveBtn.performClick();
                    return true;
                }
                return false;
            });

            binding.customApplianceSaveBtn.setOnClickListener(v -> {
                if (currentItem == null) return;
                String txt = binding.customApplianceEdit.getText().toString().trim();
                currentItem.appliance = txt.isEmpty() ? applianceOptions[0] : txt;
                outletViewModel.update(currentItem, null);
                activity.hideKeyboard(binding.customApplianceEdit);
                binding.customApplianceEdit.clearFocus();
                if (txt.isEmpty()) setupApplianceField(currentItem);
            });

            binding.customApplianceClearBtn.setOnClickListener(v -> {
                if (currentItem == null) return;
                binding.customApplianceEdit.setText("");
                currentItem.appliance = applianceOptions[0];
                outletViewModel.update(currentItem, null);
                activity.hideKeyboard(binding.customApplianceEdit);
                binding.customApplianceEdit.clearFocus();
                setupApplianceField(currentItem);
            });

            binding.switchEdit.setOnFocusChangeListener((v, hasFocus) -> {
                binding.switchSaveBtn.setVisibility(hasFocus ? View.VISIBLE : View.GONE);

                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) binding.switchContainer.getLayoutParams();
                if (hasFocus) {
                    params.weight = 3.5f;
                    binding.breakerSpinner.setVisibility(View.GONE);
                    binding.ampsSpinner.setVisibility(View.GONE);
                } else {
                    params.weight = 1.5f;
                    binding.breakerSpinner.setVisibility(View.VISIBLE);
                    binding.ampsSpinner.setVisibility(View.VISIBLE);
                }
                binding.switchContainer.setLayoutParams(params);

                LinearLayout.LayoutParams editParams = (LinearLayout.LayoutParams) binding.switchEdit.getLayoutParams();
                editParams.weight = 1.0f;
                binding.switchEdit.setLayoutParams(editParams);

                if (hasFocus) scrollToView(v);
            });

            binding.switchEdit.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    binding.switchSaveBtn.performClick();
                    return true;
                }
                return false;
            });

            binding.switchSaveBtn.setOnClickListener(v -> {
                if (currentItem == null) return;
                String newSwitch = binding.switchEdit.getText().toString().trim();
                if (!newSwitch.equals(currentItem.switchName)) {
                    currentItem.switchName = newSwitch;
                    outletViewModel.update(currentItem, null);
                }
                activity.hideKeyboard(binding.switchEdit);
                binding.switchEdit.clearFocus();
            });

            binding.breakerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                    if (isBinding || currentItem == null) return;
                    if (pos == currentBreakerPos) return;
                    currentBreakerPos = pos;

                    String sel = breakerTypes[pos];
                    if (!sel.equalsIgnoreCase(currentItem.breakerType)) {
                        currentItem.breakerType = sel;
                        outletViewModel.update(currentItem, null);
                    }
                }
                @Override public void onNothingSelected(AdapterView<?> p) { }
            });

            binding.ampsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                    if (isBinding || currentItem == null) return;
                    if (pos == currentAmpsPos) return;
                    currentAmpsPos = pos;

                    try {
                        Double dVal = Double.parseDouble(ampsOptions[pos]);
                        if (currentItem.amps == null || !currentItem.amps.equals(dVal)) {
                            currentItem.amps = dVal;
                            outletViewModel.update(currentItem, null);
                        }
                    } catch (Exception ignored) {}
                }
                @Override public void onNothingSelected(AdapterView<?> p) { }
            });

            binding.ohmsEdit.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    binding.ohmsSaveBtn.performClick();
                    return true;
                }
                return false;
            });

            binding.ohmsEdit.setOnFocusChangeListener((v, hasFocus) -> {
                binding.ohmsSaveBtn.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
                toggleFieldExpansion(binding.ohmsContainer, binding.noteSpinner, hasFocus, 1f, 2f);
                if (hasFocus) {
                    binding.customNoteContainer.setVisibility(View.GONE);
                    scrollToView(v);
                } else if (currentItem != null) {
                    setupNoteField(currentItem);
                }
            });

            binding.ohmsSaveBtn.setOnClickListener(v -> {
                if (currentItem == null) return;
                String txt = binding.ohmsEdit.getText().toString().replace(',', '.');
                Double val = null;
                try { if (!txt.isEmpty()) val = Double.parseDouble(txt); } catch (Exception ignored) {}
                if ((val == null && currentItem.ohms != null) || (val != null && !val.equals(currentItem.ohms))) {
                    currentItem.ohms = val;
                    outletViewModel.update(currentItem, null);
                }
                activity.hideKeyboard(binding.ohmsEdit);
                binding.ohmsEdit.clearFocus();
            });

            binding.noteSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                    if (isBinding || currentItem == null) return;
                    if (pos == currentNotePos) return;
                    currentNotePos = pos;

                    String sel = noteOptions[pos];
                    if ("Inne".equalsIgnoreCase(sel)) {
                        binding.noteSpinner.setVisibility(View.GONE);
                        binding.customNoteContainer.setVisibility(View.VISIBLE);
                        binding.customNoteEdit.requestFocus();
                        activity.showKeyboard(binding.customNoteEdit);
                    } else if (!sel.equals(currentItem.note)) {
                        currentItem.note = sel;
                        outletViewModel.update(currentItem, null);
                    }
                }
                @Override public void onNothingSelected(AdapterView<?> p) { }
            });

            binding.customNoteEdit.setOnFocusChangeListener((v, hasFocus) -> {
                toggleFieldExpansion(binding.customNoteContainer, binding.switchContainer, hasFocus, 2f, 2f);
                binding.customNoteSaveBtn.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
                binding.customNoteClearBtn.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
                if (hasFocus) scrollToView(v);
            });

            binding.customNoteEdit.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                    binding.customNoteSaveBtn.performClick();
                    return true;
                }
                return false;
            });

            binding.customNoteSaveBtn.setOnClickListener(v -> {
                if (currentItem == null) return;
                String txt = binding.customNoteEdit.getText().toString().trim();
                currentItem.note = txt.isEmpty() ? noteOptions[0] : txt;
                outletViewModel.update(currentItem, null);
                activity.hideKeyboard(binding.customNoteEdit);
                binding.customNoteEdit.clearFocus();
                if (txt.isEmpty()) setupNoteField(currentItem);
            });

            binding.customNoteClearBtn.setOnClickListener(v -> {
                if (currentItem == null) return;
                binding.customNoteEdit.setText("");
                currentItem.note = noteOptions[0];
                outletViewModel.update(currentItem, null);
                activity.hideKeyboard(binding.customNoteEdit);
                binding.customNoteEdit.clearFocus();
                setupNoteField(currentItem);
            });

            binding.deleteBtn.setOnClickListener(v -> {
                if (currentItem != null) {
                    View focused = activity.getCurrentFocus();
                    if (focused != null) {
                        focused.clearFocus();
                        activity.hideKeyboard();
                    }
                    outletViewModel.delete(currentItem, null);
                }
            });

            binding.photoBtn.setOnClickListener(v -> {
                if (currentItem == null) return;
                if (currentItem.photoPath == null || currentItem.photoPath.isEmpty()) {
                    if (listener != null) {
                        listener.onAddPhoto(currentItem);
                    }
                } else {
                    if (binding.photoContainer.getVisibility() == View.VISIBLE) {
                        binding.photoContainer.setVisibility(View.GONE);
                        expandedPhotoIds.remove(currentItem.id);
                    } else {
                        binding.photoContainer.setVisibility(View.VISIBLE);
                        expandedPhotoIds.add(currentItem.id);
                        if (currentItem.photoPath != null) {
                            binding.measurementPhoto.setImageURI(android.net.Uri.fromFile(new java.io.File(currentItem.photoPath)));
                        }
                        scrollToView(binding.photoContainer);
                    }
                }
            });

            binding.deletePhotoBtn.setOnClickListener(v -> {
                if (currentItem != null) {
                    OutletMeasurement updateMe = currentItem.copy();
                    updateMe.photoPath = null;
                    outletViewModel.update(updateMe, null);

                    // Natychmiastowa aktualizacja UI, aby uniknąć problemów z brakiem odświeżenia przez DiffUtil
                    binding.photoBtn.setText("📷");
                    binding.photoBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#8C8C8C")));
                    binding.photoContainer.setVisibility(View.GONE);
                    expandedPhotoIds.remove(currentItem.id);
                }
            });
        }

        void bind(OutletMeasurement om) {
            this.currentItem = om;
            isBinding = true;

            binding.ohmsEdit.removeTextChangedListener(ohmsWatcher);

            binding.customApplianceEdit.setEnabled(true);
            binding.customApplianceEdit.setVisibility(View.VISIBLE);
            binding.flatNumberTextView.setVisibility(View.GONE);

            if (catalogId != -1) {
                binding.noteSpinner.setEnabled(false);
                binding.ohmsEdit.setEnabled(false);
            } else {
                binding.noteSpinner.setEnabled(true);
                binding.ohmsEdit.setEnabled(true);
            }

            if (isCommonSpace && Settings.mainRoomName.equalsIgnoreCase(roomName) && om.appliance.toLowerCase().contains("lokal")) {
                binding.customApplianceEdit.setEnabled(false);

                binding.flatNumberTextView.setVisibility(View.VISIBLE);
                binding.flatNumberTextView.setText(om.appliance);
                binding.customApplianceEdit.setVisibility(View.GONE);
            }
            if (isCommonSpace && !Settings.mainRoomName.equalsIgnoreCase(roomName)) {
                binding.rcdHeaders.setVisibility(View.VISIBLE);
                binding.rcdRowContainer.setVisibility(View.VISIBLE);
                setupRcdFields(om);
            } else {
                binding.rcdHeaders.setVisibility(View.GONE);
                binding.rcdRowContainer.setVisibility(View.GONE);
            }

            setupApplianceField(om);
            setupNoteField(om);

            binding.switchEdit.setText(om.switchName != null ? om.switchName : "");

            currentBreakerPos = findSpinnerIndex(breakerTypes, om.breakerType);
            binding.breakerSpinner.setSelection(currentBreakerPos, false);

            String ampsStr = (om.amps != null) ? String.valueOf(om.amps.longValue()) : "16";
            currentAmpsPos = findSpinnerIndex(ampsOptions, ampsStr);
            binding.ampsSpinner.setSelection(currentAmpsPos, false);

            binding.ohmsEdit.setText(String.format(Locale.GERMANY, "%.2f", om.ohms != null ? om.ohms : 0.0));

            if (focusToMeasurementId != -1 && om.id == focusToMeasurementId) {
                focusToMeasurementId = -1;
                binding.ohmsEdit.postDelayed(() -> {
                    if (itemView.isAttachedToWindow()) {
                        binding.ohmsEdit.requestFocus();
                        binding.ohmsEdit.setSelection(binding.ohmsEdit.getText().length());
                        activity.showKeyboard(binding.ohmsEdit);
                        scrollToView(binding.ohmsEdit);
                    }
                }, 150);
            }

            binding.ohmsEdit.addTextChangedListener(ohmsWatcher);

            if (om.photoPath != null && !om.photoPath.isEmpty()) {
                binding.photoBtn.setText("🖼️");
                binding.photoBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(activity, android.R.color.holo_blue_dark)));

                if (expandedPhotoIds.contains(om.id)) {
                    binding.photoContainer.setVisibility(View.VISIBLE);
                    binding.measurementPhoto.setImageURI(android.net.Uri.fromFile(new java.io.File(om.photoPath)));
                } else {
                    binding.photoContainer.setVisibility(View.GONE);
                }
            } else {
                binding.photoBtn.setText("📷");
                binding.photoBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#8C8C8C")));
                binding.photoContainer.setVisibility(View.GONE);
                binding.measurementPhoto.setImageDrawable(null);
                expandedPhotoIds.remove(om.id);
            }

            isBinding = false;
        }

        private int findSpinnerIndex(String[] options, String value) {
            if (value == null) return 0;
            for (int i = 0; i < options.length; i++) {
                if (options[i].equalsIgnoreCase(value)) {
                    return i;
                }
            }
            return 0;
        }

        private boolean isCustomValue(String[] options, String value) {
            if (value == null) return false;
            for (String predefined : options) {
                if (predefined.equalsIgnoreCase(value)) {
                    return false;
                }
            }
            return true;
        }

        private void setupApplianceField(OutletMeasurement om) {
            if (isCustomValue(applianceOptions, om.appliance)) {
                binding.customApplianceEdit.setText(om.appliance);
                binding.applianceSpinner.setVisibility(View.GONE);
                binding.customApplianceContainer.setVisibility(View.VISIBLE);
                currentAppliancePos = findSpinnerIndex(applianceOptions, "Inne");
                binding.applianceSpinner.setSelection(currentAppliancePos, false);
            } else {
                binding.customApplianceEdit.setText("");
                binding.applianceSpinner.setVisibility(View.VISIBLE);
                binding.customApplianceContainer.setVisibility(View.GONE);
                currentAppliancePos = findSpinnerIndex(applianceOptions, om.appliance);
                binding.applianceSpinner.setSelection(currentAppliancePos, false);
            }
        }

        private void setupNoteField(OutletMeasurement om) {
            if (isCustomValue(noteOptions, om.note)) {
                binding.customNoteEdit.setText(om.note);
                binding.noteSpinner.setVisibility(View.GONE);
                binding.customNoteContainer.setVisibility(View.VISIBLE);
                currentNotePos = findSpinnerIndex(noteOptions, "Inne");
                binding.noteSpinner.setSelection(currentNotePos, false);
            } else {
                binding.customNoteEdit.setText("");
                binding.noteSpinner.setVisibility(View.VISIBLE);
                binding.customNoteContainer.setVisibility(View.GONE);
                currentNotePos = findSpinnerIndex(noteOptions, om.note);
                binding.noteSpinner.setSelection(currentNotePos, false);
            }
        }

        private void setupRcdFields(OutletMeasurement om) {
            currentRcdStatePos = om.rcdStatus;
            binding.rcdStateSpinner.setSelection(currentRcdStatePos, false);
            binding.rcdNameEdit.setText(om.rcdName != null ? om.rcdName : "");
            binding.rcdTimeEdit.setText(om.rcdTime != null ? om.rcdTime.toString() : "");
            binding.rcdCurrentEdit.setText(om.rcdCurrent != null ? om.rcdCurrent.toString() : "");
            enableRcdEdits(om.rcdStatus != 0);
        }

        private void enableRcdEdits(boolean enable) {
            binding.rcdNameEdit.setEnabled(enable);
            binding.rcdTimeEdit.setEnabled(enable);
            binding.rcdCurrentEdit.setEnabled(enable);
            if (!enable) {
                binding.rcdNameSaveBtn.setVisibility(View.GONE);
                binding.rcdTimeSaveBtn.setVisibility(View.GONE);
                binding.rcdCurrentSaveBtn.setVisibility(View.GONE);
            }
        }

        private void toggleFieldExpansion(View fieldToExpand, View fieldToHide, boolean expand,
                                          float originalWeight, float hiddenWeight) {
            fieldToHide.setVisibility(expand ? View.GONE : View.VISIBLE);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) fieldToExpand.getLayoutParams();
            params.weight = expand ? (originalWeight + hiddenWeight) : originalWeight;
            fieldToExpand.setLayoutParams(params);
        }
    }

    private static final DiffUtil.ItemCallback<OutletMeasurement> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<OutletMeasurement>() {
                @Override
                public boolean areItemsTheSame(@NonNull OutletMeasurement oldItem, @NonNull OutletMeasurement newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull OutletMeasurement oldItem, @NonNull OutletMeasurement newItem) {
                    return oldItem.equals(newItem);
                }
            };
}