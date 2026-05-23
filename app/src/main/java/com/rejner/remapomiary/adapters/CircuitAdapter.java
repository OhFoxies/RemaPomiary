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
import com.rejner.remapomiary.data.entities.Circuit;

import java.util.ArrayList;
import java.util.List;

public class CircuitAdapter extends RecyclerView.Adapter<CircuitAdapter.CircuitViewHolder> {

    private List<Circuit> circuits = new ArrayList<>();
    private Context context;

    public void setCircuits(List<Circuit> circuits) {
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
        Circuit circuit = circuits.get(position);
        holder.number.setText((position + 1) + ".");

        // TODO: Przypisz dane z obiektu 'circuit' do widoków (RadioGroup itp.)
    }

    @Override
    public int getItemCount() {
        return circuits != null ? circuits.size() : 0;
    }

    class CircuitViewHolder extends RecyclerView.ViewHolder {
        TextView number;
        Spinner circuitNameSpinner;
        EditText circuitInputName;
        Button circuitNameSave;
        RadioGroup radioGroupPhases;
        Button deleteCircuit;

        public CircuitViewHolder(@NonNull View itemView) {
            super(itemView);
            number = itemView.findViewById(R.id.number);
            circuitNameSpinner = itemView.findViewById(R.id.circuitNameSpinner);
            circuitInputName = itemView.findViewById(R.id.circuitInputName);
            circuitNameSave = itemView.findViewById(R.id.circuitNameSave);
            radioGroupPhases = itemView.findViewById(R.id.radioGroup); // Uwaga: w XML brak ID dla RadioGroup obwodu, upewnij się że dodałeś ID np. id="@+id/phasesRadioGroup"
            deleteCircuit = itemView.findViewById(R.id.deleteCiruit);

            setupSpinner();
            setupButtons();
        }

        private void setupSpinner() {
            String[] items = new String[]{"Oświetlenie", "Gniazda 230V", "Piekarnik", "Płyta indukcyjna", "inne"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, items);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            circuitNameSpinner.setAdapter(adapter);

            circuitNameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String selected = parent.getItemAtPosition(position).toString();
                    if ("inne".equalsIgnoreCase(selected)) {
                        circuitInputName.setVisibility(View.VISIBLE);
                        circuitNameSave.setVisibility(View.VISIBLE);

                        // Focus na EditText i wysunięcie klawiatury
                        circuitInputName.requestFocus();
                        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.showSoftInput(circuitInputName, InputMethodManager.SHOW_IMPLICIT);
                        }
                    } else {
                        circuitInputName.setVisibility(View.GONE);
                        circuitNameSave.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        private void setupButtons() {
            circuitNameSave.setOnClickListener(v -> {
                // TODO: Logika zapisu własnej nazwy obwodu do bazy
            });

            deleteCircuit.setOnClickListener(v -> {
                // TODO: Logika usunięcia obwodu
            });
        }
    }
}
