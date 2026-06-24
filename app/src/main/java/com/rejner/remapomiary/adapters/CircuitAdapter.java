package com.rejner.remapomiary.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
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
import com.rejner.remapomiary.ui.utils.Settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CircuitAdapter extends RecyclerView.Adapter<CircuitAdapter.CircuitViewHolder> {
    String[] items = new String[]{"Oświetlenie", "Oświetlenie -", "Gniazda 230V", "Gniazda -", "Piekarnik", "Płyta indukcyjna", "inne"};
    private boolean isWLZ;

    public CircuitAdapter(OnCircuitActionListener listener, boolean isWLZ) {
        this.listener = listener;
        this.isWLZ = isWLZ;
    }

    public void setIsWLZ(boolean isWLZ) {
        this.isWLZ = isWLZ;
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

    private RecyclerView attachedRecyclerView;

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.attachedRecyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        this.attachedRecyclerView = null;
    }

    public void setCircuits(List<CircuitCommonSpace> circuits) {
        int oldSize = this.circuits != null ? this.circuits.size() : 0;
        int newSize = circuits != null ? circuits.size() : 0;

        this.circuits = circuits;

        notifyDataSetChanged();

        if (newSize > oldSize && oldSize > 0) {
            if (attachedRecyclerView != null) {
                attachedRecyclerView.post(() -> {
                    attachedRecyclerView.smoothScrollToPosition(newSize - 1);
                });
            }
        }
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

        holder.isUserAction = false;

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

        holder.phasesGroup.setOnCheckedChangeListener(null);

        if (Settings.installation1f.equals(circuit.type)) {
            holder.phasesGroup.check(R.id.radio1f);
        } else if (Settings.installation3f.equals(circuit.type)) {
            holder.phasesGroup.check(R.id.radio3f);
        } else {
            holder.phasesGroup.clearCheck();
        }

        if (isWLZ && circuit.name != null && circuit.name.toLowerCase().contains("lokal")) {
            Log.e("Wzium", "True dla " + circuit.name.toLowerCase());
            holder.circuitInputName.setEnabled(false);
            if (circuit.notes != null && !circuit.notes.isEmpty()) {
                holder.status.setVisibility(View.VISIBLE);
                holder.status.setText(circuit.notes);
            } else {
                holder.status.setVisibility(View.GONE);
            }

            holder.circuitNameSpinner.setEnabled(false);
            holder.circuitNameSpinner.setVisibility(View.GONE);
            holder.circuitNameSave.setVisibility(View.GONE);
        } else {
            holder.circuitInputName.setEnabled(true);
            holder.circuitNameSpinner.setEnabled(true);
            holder.circuitNameSpinner.setVisibility(View.VISIBLE);
            holder.status.setVisibility(View.GONE);
        }

        holder.phasesGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION && listener != null) {
                holder.hideKeyboard();

                CircuitCommonSpace currentCircuit = circuits.get(adapterPosition);
                if (checkedId == R.id.radio1f) {
                    currentCircuit.type = Settings.installation1f;
                } else if (checkedId == R.id.radio3f) {
                    currentCircuit.type = Settings.installation3f;
                }
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
        TextView status;

        boolean isUserAction = false;

        public CircuitViewHolder(@NonNull View itemView) {
            super(itemView);
            number = itemView.findViewById(R.id.number);
            circuitNameSpinner = itemView.findViewById(R.id.circuitNameSpinner);
            circuitInputName = itemView.findViewById(R.id.circuitInputName);
            circuitNameSave = itemView.findViewById(R.id.circuitNameSave);
            phasesGroup = itemView.findViewById(R.id.phasesGroup);
            status = itemView.findViewById(R.id.status);
            deleteCircuit = itemView.findViewById(R.id.deleteCiruit);

            circuitInputName.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.postDelayed(() -> {
                        ViewParent p = itemView.getParent();
                        RecyclerView mainRv = null;
                        while (p != null) {
                            if (p instanceof RecyclerView) {
                                RecyclerView temp = (RecyclerView) p;
                                if (temp.getId() == R.id.boardsRecyclerView) {
                                    mainRv = temp;
                                    break;
                                }
                            }
                            p = p.getParent();
                        }

                        if (mainRv != null) {
                            int[] viewLocation = new int[2];
                            v.getLocationOnScreen(viewLocation);
                            int viewBottom = viewLocation[1] + v.getHeight();

                            int[] rvLocation = new int[2];
                            mainRv.getLocationOnScreen(rvLocation);
                            int rvBottom = rvLocation[1] + mainRv.getHeight();

                            if (viewBottom > rvBottom) {
                                float density = v.getContext().getResources().getDisplayMetrics().density;
                                int extraMargin = (int) (20 * density);
                                mainRv.smoothScrollBy(0, viewBottom - rvBottom + extraMargin);
                            }
                        }
                    }, 300);
                }
            });

            setupSpinner();
            setupButtons();
        }

        @SuppressLint("ClickableViewAccessibility")
        private void setupSpinner() {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, items);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            circuitNameSpinner.setAdapter(adapter);

            circuitNameSpinner.setOnTouchListener((v, event) -> {
                isUserAction = true;
                return false;
            });

            circuitNameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    int positionV = getBindingAdapterPosition();
                    if (positionV == RecyclerView.NO_POSITION) return;

                    CircuitCommonSpace currentCircuit = circuits.get(positionV);
                    String selected = parent.getItemAtPosition(position).toString();

                    if ("inne".equalsIgnoreCase(selected) || "Oświetlenie -".equalsIgnoreCase(selected) || "Gniazda -".equalsIgnoreCase(selected)) {
                        circuitInputName.setVisibility(View.VISIBLE);
                        circuitNameSave.setVisibility(View.VISIBLE);

                        if (isUserAction) {
                            if ("Oświetlenie -".equalsIgnoreCase(selected)) {
                                circuitInputName.setText("Oświetlenie ");
                            } else if ("Gniazda -".equalsIgnoreCase(selected)) {
                                circuitInputName.setText("Gniazda ");
                            } else if ("inne".equalsIgnoreCase(selected)) {
                                if (Arrays.asList(items).contains(currentCircuit.name) && !currentCircuit.name.equals("inne")) {
                                    circuitInputName.setText("");
                                }
                            }

                            circuitInputName.requestFocus();
                            circuitInputName.postDelayed(() -> {
                                if (circuitInputName.requestFocus()) {
                                    circuitInputName.setSelection(circuitInputName.getText().length());
                                    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                                    if (imm != null) {
                                        imm.showSoftInput(circuitInputName, 0);
                                    }
                                }
                            }, 200);
                        }
                    } else {
                        hideKeyboard();
                        circuitInputName.setVisibility(View.GONE);
                        circuitNameSave.setVisibility(View.GONE);
                        circuitInputName.clearFocus();

                        if (isUserAction && currentCircuit.name != null && !selected.equals(currentCircuit.name) && listener != null) {
                            currentCircuit.name = selected;
                            listener.onCircuitNameSpinner_(currentCircuit, selected);
                        }
                    }

                    isUserAction = false;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        private void setupButtons() {
            circuitNameSave.setOnClickListener(v -> saveName());

            circuitInputName.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                    saveName();
                    return true;
                }
                return false;
            });

            deleteCircuit.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    hideKeyboard();
                    listener.onCircuitDelete_(circuits.get(position));
                }
            });
        }

        private void saveName() {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION && listener != null) {
                hideKeyboard();
                String name = circuitInputName.getText().toString().trim();
                if (name.isEmpty()) {
                    name = items[0]; // "Oświetlenie"
                    circuitNameSpinner.setSelection(0, false);
                    circuitInputName.setVisibility(View.GONE);
                    circuitNameSave.setVisibility(View.GONE);
                }
                circuits.get(position).name = name;
                listener.onCircuitNameSave_(circuits.get(position), name);
                circuitInputName.clearFocus();
            }
        }

        private void hideKeyboard() {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(circuitInputName.getWindowToken(), 0);
            }
        }
    }
}