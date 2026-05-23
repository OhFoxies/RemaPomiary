package com.rejner.remapomiary.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rejner.remapomiary.R;
import com.rejner.remapomiary.data.entities.CircuitCommonSpace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CircuitAdapter extends RecyclerView.Adapter<CircuitAdapter.CircuitViewHolder> {
    String[] items = new String[]{"Oświetlenie", "Gniazda 230V", "Piekarnik", "Płyta indukcyjna", "inne"};

    public CircuitAdapter(OnCircuitActionListener listener) {
        this.listener = listener;
    }

    public interface OnCircuitActionListener {
        void onCircuitTypeChange_(CircuitCommonSpace circuit, int checkedId);
        void onCircuitDelete_(CircuitCommonSpace circuit);
        void onCircuitNameSave_(CircuitCommonSpace circuit, String name);
        void onCircuitNameSpinner_(CircuitCommonSpace circuit, String name);
    }

    private final OnCircuitActionListener listener;
    private List<CircuitCommonSpace> circuits = new ArrayList<>();
    private Context context;

    public void setCircuits(List<CircuitCommonSpace> circuits) {
        this.circuits = circuits;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CircuitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.circuit_item, parent, false);
        return new CircuitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CircuitViewHolder holder, int position) {
        CircuitCommonSpace circuit = circuits.get(position);
        holder.number.setText((position + 1) + ".");

        int inneIndex = Arrays.asList(items).indexOf("inne");
        int nameIndex = Arrays.asList(items).indexOf(circuit.name);

        if (nameIndex >= 0 && nameIndex != inneIndex) {
            holder.circuitNameSpinner.setSelection(nameIndex, false);
            holder.circuitInputName.setVisibility(View.GONE);
            holder.circuitNameSave.setVisibility(View.GONE);
        } else {
            holder.circuitNameSpinner.setSelection(inneIndex, false);
            holder.circuitInputName.setVisibility(View.VISIBLE);
            holder.circuitNameSave.setVisibility(View.VISIBLE);
            holder.circuitInputName.setText(circuit.name != null && circuit.name.equals("inne") ? "" : circuit.name);
        }

        // 1. Zdejmujemy listener
        holder.phasesGroup.setOnCheckedChangeListener(null);

        // 2. Ustawiamy stan na podstawie circuit.type
        // UWAGA: Zmień 'R.id.radio_1f' i 'R.id.radio_3f' na poprawne ID z Twojego pliku XML!
        if ("1f".equals(circuit.type)) {
            holder.phasesGroup.check(R.id.radio1f);
        } else if ("3f".equals(circuit.type)) {
            holder.phasesGroup.check(R.id.radio3f);
        } else {
            // Jeśli circuit.type jest null lub pusty, odznaczamy (ważne dla recyclingu)
            holder.phasesGroup.clearCheck();
        }

        // 3. Przypinamy listener, który zaktualizuje obiekt i bazę przy kliknięciu
        holder.phasesGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int adapterPosition = holder.getBindingAdapterPosition();

            if (adapterPosition != RecyclerView.NO_POSITION && listener != null) {
                CircuitCommonSpace currentCircuit = circuits.get(adapterPosition);

                // Aktualizujemy obiekt lokalnie (żeby przewijanie działało poprawnie)
                // UWAGA: Tutaj też użyj tych samych ID co wyżej
                if (checkedId == R.id.radio1f) {
                    currentCircuit.type = "1f";
                } else if (checkedId == R.id.radio3f) {
                    currentCircuit.type = "3f";
                }

                // Wysyłamy do Activity/Fragmentu w celu zapisu do bazy
                listener.onCircuitTypeChange_(currentCircuit, checkedId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return circuits != null ? circuits.size() : 0;
    }

    class CircuitViewHolder extends RecyclerView.ViewHolder {
        RadioGroup phasesGroup;
        TextView number;
        Spinner circuitNameSpinner;
        EditText circuitInputName;
        Button circuitNameSave;
        Button deleteCircuit;

        public CircuitViewHolder(@NonNull View itemView) {
            super(itemView);
            number = itemView.findViewById(R.id.number);
            circuitNameSpinner = itemView.findViewById(R.id.circuitNameSpinner);
            circuitInputName = itemView.findViewById(R.id.circuitInputName);
            circuitNameSave = itemView.findViewById(R.id.circuitNameSave);
            phasesGroup = itemView.findViewById(R.id.phasesGroup);
            deleteCircuit = itemView.findViewById(R.id.deleteCiruit);

            setupSpinner();
            setupButtons();
        }

        private void setupSpinner() {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, items);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            circuitNameSpinner.setAdapter(adapter);

            circuitNameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    int positionV = getBindingAdapterPosition();
                    if (positionV == RecyclerView.NO_POSITION) return;

                    CircuitCommonSpace currentCircuit = circuits.get(positionV);
                    String selected = parent.getItemAtPosition(position).toString();

                    if ("inne".equalsIgnoreCase(selected)) {
                        if (circuitInputName.getVisibility() != View.VISIBLE) {
                            circuitInputName.setVisibility(View.VISIBLE);
                            circuitNameSave.setVisibility(View.VISIBLE);

                            if (Arrays.asList(items).contains(currentCircuit.name) && !currentCircuit.name.equals("inne")) {
                                circuitInputName.setText("");
                            }

                            circuitInputName.requestFocus();
                            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null) {
                                imm.showSoftInput(circuitInputName, InputMethodManager.SHOW_IMPLICIT);
                            }
                        }
                    } else {
                        circuitInputName.setVisibility(View.GONE);
                        circuitNameSave.setVisibility(View.GONE);

                        if (currentCircuit.name != null && !selected.equals(currentCircuit.name) && listener != null) {
                            currentCircuit.name = selected;
                            listener.onCircuitNameSpinner_(currentCircuit, selected);
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        private void setupButtons() {
            circuitNameSave.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    circuits.get(position).name = circuitInputName.getText().toString();
                    listener.onCircuitNameSave_(circuits.get(position), circuitInputName.getText().toString());
                }
            });

            deleteCircuit.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onCircuitDelete_(circuits.get(position));
                }
            });
        }
    }
}