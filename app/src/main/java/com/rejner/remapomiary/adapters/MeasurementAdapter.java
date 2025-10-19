package com.rejner.remapomiary.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.rejner.remapomiary.R;
import com.rejner.remapomiary.data.entities.OutletMeasurement;
import com.rejner.remapomiary.databinding.MeasurementRowItemBinding;
import com.rejner.remapomiary.ui.activities.RoomActivity;
import com.rejner.remapomiary.ui.viewmodels.OutletMeasurementViewModel;

import java.util.Locale;

public class MeasurementAdapter extends ListAdapter<OutletMeasurement, MeasurementAdapter.MeasurementViewHolder> {

    private final RoomActivity activity; // Do zarządzania klawiaturą
    private final OutletMeasurementViewModel outletViewModel;
    private final String[] applianceOptions, breakerTypes, noteOptions, ampsOptions;
    private final int catalogId;
    private boolean isProgrammaticSelection = false;
    private long focusToMeasurementId = -1;

    public MeasurementAdapter(RoomActivity activity, OutletMeasurementViewModel outletViewModel,
                              String[] applianceOptions, String[] breakerTypes, String[] noteOptions, String[] ampsOptions, int catalogId) {
        super(DIFF_CALLBACK);
        this.activity = activity;
        this.outletViewModel = outletViewModel;
        this.applianceOptions = applianceOptions;
        this.breakerTypes = breakerTypes;
        this.noteOptions = noteOptions;
        this.ampsOptions = ampsOptions;
        this.catalogId = catalogId;
    }

    public void setFocusToMeasurementId(long id) {
        this.focusToMeasurementId = id;
        // Powiadom adapter, aby ponownie związał wiersze (choć submitList powinien to załatwić)
        // To jest trudniejsze; ViewHolder musi to sprawdzić podczas bindowania.
        notifyDataSetChanged(); // Nieidealne, ale proste
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

        MeasurementViewHolder(MeasurementRowItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            // Dołącz Watcher do OhmsEdit
            binding.ohmsEdit.addTextChangedListener(new RoomActivity.OhmsTextWatcher(binding.ohmsEdit));
        }

        void bind(OutletMeasurement om) {
            // Zresetuj listenery, aby uniknąć wywołań zwrotnych podczas bindowania
            binding.applianceSpinner.setOnItemSelectedListener(null);
            binding.breakerSpinner.setOnItemSelectedListener(null);
            binding.ampsSpinner.setOnItemSelectedListener(null);
            binding.noteSpinner.setOnItemSelectedListener(null);
            if (catalogId != -1) {
                binding.noteSpinner.setEnabled(false);
                binding.ohmsEdit.setEnabled(false);
            }
            // --- Ustawienie adapterów dla Spinnerów ---
            setupSpinner(binding.applianceSpinner, applianceOptions);
            setupSpinner(binding.breakerSpinner, breakerTypes);
            setupSpinner(binding.ampsSpinner, ampsOptions);
            setupSpinner(binding.noteSpinner, noteOptions);

            // --- Ustawienie wartości początkowych ---
            isProgrammaticSelection = true;
            setupApplianceField(om);
            setupNoteField(om);

            binding.switchEdit.setText(om.switchName != null ? om.switchName : "");
            binding.breakerSpinner.setSelection(findSpinnerIndex(breakerTypes, om.breakerType));
            String ampsStr = (om.amps != null) ? String.valueOf(om.amps.longValue()) : "16";
            binding.ampsSpinner.setSelection(findSpinnerIndex(ampsOptions, ampsStr));
            binding.ohmsEdit.setText(String.format(Locale.GERMANY, "%.2f", om.ohms != null ? om.ohms : 0.0));

            isProgrammaticSelection = false;

            // --- Ustawienie Listenerów ---
            setupApplianceListeners(om);
            setupSwitchListeners(om);
            setupBreakerListener(om);
            setupAmpsListener(om);
            setupOhmsListeners(om);
            setupNoteListeners(om);
            setupDeleteListener(om);

            // --- Logika Fokusu dla nowego elementu ---
            if (focusToMeasurementId != -1 && om.id == focusToMeasurementId) {
                binding.ohmsEdit.requestFocus();
                binding.ohmsEdit.setSelection(binding.ohmsEdit.getText().length());
                activity.showKeyboard(binding.ohmsEdit);
                focusToMeasurementId = -1; // Resetuj ID
            }
        }

        private void styleUtilityButton(Button button) {
            button.setMinWidth(0);
            button.setMinimumWidth(0);
            button.setPadding(16, 0, 16, 0);
        }

        private void setupSpinner(android.widget.Spinner spinner, String[] options) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, options);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
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

        // --- Logika pól "Inne" (Appliance i Note) ---

        private void setupApplianceField(OutletMeasurement om) {
            if (isCustomValue(applianceOptions, om.appliance)) {
                binding.customApplianceEdit.setText(om.appliance);
                binding.applianceSpinner.setVisibility(View.GONE);
                binding.customApplianceContainer.setVisibility(View.VISIBLE);
                binding.applianceSpinner.setSelection(findSpinnerIndex(applianceOptions, "Inne"));
            } else {
                binding.customApplianceEdit.setText("");
                binding.applianceSpinner.setVisibility(View.VISIBLE);
                binding.customApplianceContainer.setVisibility(View.GONE);
                binding.applianceSpinner.setSelection(findSpinnerIndex(applianceOptions, om.appliance));
            }
        }

        private void setupNoteField(OutletMeasurement om) {
            if (isCustomValue(noteOptions, om.note)) {
                binding.customNoteEdit.setText(om.note);
                binding.noteSpinner.setVisibility(View.GONE);
                binding.customNoteContainer.setVisibility(View.VISIBLE);
                binding.noteSpinner.setSelection(findSpinnerIndex(noteOptions, "Inne"));
            } else {
                binding.customNoteEdit.setText("");
                binding.noteSpinner.setVisibility(View.VISIBLE);
                binding.customNoteContainer.setVisibility(View.GONE);
                binding.noteSpinner.setSelection(findSpinnerIndex(noteOptions, om.note));
            }
        }

        // --- Listenery ---

        private void setupApplianceListeners(OutletMeasurement om) {
            binding.applianceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (isProgrammaticSelection) return;
                    String selected = applianceOptions[position];
                    if ("Inne".equals(selected)) {
                        binding.applianceSpinner.setVisibility(View.GONE);
                        binding.customApplianceContainer.setVisibility(View.VISIBLE);
                        binding.customApplianceEdit.requestFocus();
                        activity.showKeyboard(binding.customApplianceEdit);
                    } else if (om.appliance == null || !om.appliance.equals(selected)) {
                        om.appliance = selected;
                        outletViewModel.update(om, null);
                    }
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });

            binding.customApplianceEdit.setOnFocusChangeListener((v, hasFocus) -> {
                toggleFieldExpansion(binding.applianceContainer, binding.switchContainer, hasFocus, 2f, 2f);
                binding.customApplianceSaveBtn.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
                binding.customApplianceClearBtn.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
            });

            binding.customApplianceSaveBtn.setOnClickListener(v -> {
                String txt = binding.customApplianceEdit.getText().toString().trim();
                om.appliance = txt.isEmpty() ? applianceOptions[0] : txt;
                outletViewModel.update(om, null);
                activity.hideKeyboard();
                binding.customApplianceEdit.clearFocus();
                if (txt.isEmpty()) setupApplianceField(om); // Wróć do spinnera
            });

            binding.customApplianceClearBtn.setOnClickListener(v -> {
                binding.customApplianceEdit.setText("");
                om.appliance = applianceOptions[0];
                outletViewModel.update(om, null);
                activity.hideKeyboard();
                binding.customApplianceEdit.clearFocus();
                setupApplianceField(om); // Wróć do spinnera
            });
        }

        private void setupSwitchListeners(OutletMeasurement om) {
            binding.switchEdit.setOnFocusChangeListener((v, hasFocus) -> {
                binding.switchSaveBtn.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
            });

            binding.switchEdit.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    binding.switchSaveBtn.performClick();
                    return true;
                }
                return false;
            });

            binding.switchSaveBtn.setOnClickListener(v -> {
                String newSwitch = binding.switchEdit.getText().toString().trim();
                if (!newSwitch.equals(om.switchName)) {
                    om.switchName = newSwitch;
                    outletViewModel.update(om, null); // Logika `lastDefaultSwitchName` jest w Activity
                }
                activity.hideKeyboard();
                binding.switchEdit.clearFocus();
            });
        }

        private void setupBreakerListener(OutletMeasurement om) {
            binding.breakerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                    if (isProgrammaticSelection) return;
                    String sel = breakerTypes[pos];
                    if (!sel.equalsIgnoreCase(om.breakerType)) {
                        om.breakerType = sel;
                        outletViewModel.update(om, null);
                    }
                }
                @Override public void onNothingSelected(AdapterView<?> p) { }
            });
        }

        private void setupAmpsListener(OutletMeasurement om) {
            binding.ampsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                    if (isProgrammaticSelection) return;
                    try {
                        Double dVal = Double.parseDouble(ampsOptions[pos]);
                        if (om.amps == null || !om.amps.equals(dVal)) {
                            om.amps = dVal;
                            outletViewModel.update(om, null);
                        }
                    } catch (Exception ignored) {}
                }
                @Override public void onNothingSelected(AdapterView<?> p) { }
            });
        }

        private void setupOhmsListeners(OutletMeasurement om) {
            binding.ohmsEdit.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    binding.ohmsSaveBtn.performClick();
                    return true;
                }
                return false;
            });

            binding.ohmsEdit.setOnFocusChangeListener((v, hasFocus) -> {
                binding.ohmsSaveBtn.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
                // Logika rozszerzania pola
                toggleFieldExpansion(binding.ohmsContainer, binding.noteSpinner, hasFocus, 1f, 2f);
                if (hasFocus) binding.customNoteContainer.setVisibility(View.GONE);
                else setupNoteField(om); // Przywróć właściwy widok notatek
            });

            binding.ohmsSaveBtn.setOnClickListener(v -> {
                String txt = binding.ohmsEdit.getText().toString().replace(',', '.');
                Double val = null;
                try { if (!txt.isEmpty()) val = Double.parseDouble(txt); } catch (Exception ignored) {}
                if ((val == null && om.ohms != null) || (val != null && !val.equals(om.ohms))) {
                    om.ohms = val;
                    outletViewModel.update(om, null);
                }
                activity.hideKeyboard();
                binding.ohmsEdit.clearFocus();
            });
        }

        private void setupNoteListeners(OutletMeasurement om) {
            binding.noteSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                    if (isProgrammaticSelection) return;
                    String sel = noteOptions[pos];
                    if ("Inne".equalsIgnoreCase(sel)) {
                        binding.noteSpinner.setVisibility(View.GONE);
                        binding.customNoteContainer.setVisibility(View.VISIBLE);
                        binding.customNoteEdit.requestFocus();
                        activity.showKeyboard(binding.customNoteEdit);
                    } else if (om.note == null || !om.note.equals(sel)) {
                        om.note = sel;
                        outletViewModel.update(om, null);
                    }
                }
                @Override public void onNothingSelected(AdapterView<?> p) { }
            });

            binding.customNoteEdit.setOnFocusChangeListener((v, hasFocus) -> {
                toggleFieldExpansion(binding.customNoteContainer, binding.switchContainer, hasFocus, 2f, 2f);
                binding.customNoteSaveBtn.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
                binding.customNoteClearBtn.setVisibility(hasFocus ? View.VISIBLE : View.GONE);
            });

            binding.customNoteEdit.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    binding.customNoteSaveBtn.performClick();
                    return true;
                }
                return false;
            });

            binding.customNoteSaveBtn.setOnClickListener(v -> {
                String txt = binding.customNoteEdit.getText().toString().trim();
                om.note = txt.isEmpty() ? noteOptions[0] : txt;
                outletViewModel.update(om, null);
                activity.hideKeyboard();
                binding.customNoteEdit.clearFocus();
                if (txt.isEmpty()) setupNoteField(om); // Wróć do spinnera
            });

            binding.customNoteClearBtn.setOnClickListener(v -> {
                binding.customNoteEdit.setText("");
                om.note = noteOptions[0];
                outletViewModel.update(om, null);
                activity.hideKeyboard();
                binding.customNoteEdit.clearFocus();
                setupNoteField(om); // Wróć do spinnera
            });
        }

        private void setupDeleteListener(OutletMeasurement om) {
            binding.deleteBtn.setOnClickListener(v -> outletViewModel.delete(om, null));
        }

        // Pomocnik do rozszerzania pól (jak w oryginale)
        private void toggleFieldExpansion(View fieldToExpand, View fieldToHide, boolean expand,
                                          float originalWeight, float hiddenWeight) {
            fieldToHide.setVisibility(expand ? View.GONE : View.VISIBLE);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) fieldToExpand.getLayoutParams();
            params.weight = expand ? (originalWeight + hiddenWeight) : originalWeight;
            fieldToExpand.setLayoutParams(params);
        }
    }

    // DiffUtil Callback
    private static final DiffUtil.ItemCallback<OutletMeasurement> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<OutletMeasurement>() {
                @Override
                public boolean areItemsTheSame(@NonNull OutletMeasurement oldItem, @NonNull OutletMeasurement newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull OutletMeasurement oldItem, @NonNull OutletMeasurement newItem) {
                    // Sprawdź wszystkie pola, które mogą się zmienić
                    return oldItem.equals(newItem);
                }
            };
}