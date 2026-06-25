package com.rejner.remapomiary.ui.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.flexbox.FlexboxLayout;
import com.rejner.remapomiary.R;
import com.rejner.remapomiary.data.entities.Contractors;
import com.rejner.remapomiary.ui.viewmodels.ContractorsViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ContractorsActivity extends AppCompatActivity {

    private ContractorsViewModel contractorsViewModel;
    private EditText inputName, inputSurname, inputLicenseE, inputLicenseD;
    private Spinner activeContractorSpinner, activeCheckerSpinner;
    private Spinner defaultContractorSpinner, defaultCheckerSpinner;
    private FlexboxLayout flexboxLayout;

    private boolean isUserActionContractor = false;
    private boolean isUserActionChecker = false;
    private boolean isUserActionDefaultContractor = false;
    private boolean isUserActionDefaultChecker = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contractors);

        contractorsViewModel = new ViewModelProvider(this).get(ContractorsViewModel.class);

        initializeUI();
        observeData();
    }

    private void initializeUI() {
        inputName = findViewById(R.id.inputName);
        inputSurname = findViewById(R.id.inputSurname);
        inputLicenseE = findViewById(R.id.inputLicenseNumberE);
        inputLicenseD = findViewById(R.id.inputLicenseNumberD);
        activeContractorSpinner = findViewById(R.id.activeContractorSpinner);
        activeCheckerSpinner = findViewById(R.id.activeCheckerSpinner);
        defaultContractorSpinner = findViewById(R.id.defaultContractorSpinner);
        defaultCheckerSpinner = findViewById(R.id.defaultCheckerSpinner);
        flexboxLayout = findViewById(R.id.contractorsFlexbox);

        // Ukrywamy stary typeSpinner z poziomu kodu, jeśli nadal znajduje się w układzie XML

        View backButton = findViewById(R.id.backButton);
        if (backButton != null) backButton.setOnClickListener(v -> finish());

        View contractorAdd = findViewById(R.id.contractorAdd);
        if (contractorAdd != null) contractorAdd.setOnClickListener(v -> addContractor());

        View contractorCancel = findViewById(R.id.contractorCancel);
        if (contractorCancel != null) contractorCancel.setOnClickListener(v -> clearInputs());
    }

    private void observeData() {
        // Obserwujemy tylko pełną listę – oba spinnery dostają teraz tych samych ludzi
        if (contractorsViewModel.getAllContractors() != null) {
            contractorsViewModel.getAllContractors().observe(this, contractors -> {
                if (contractors != null) {
                    updateContractorsList(contractors);
                    setupContractorSpinner(activeContractorSpinner, contractors, true, true);
                    setupContractorSpinner(activeCheckerSpinner, contractors, false, true);
                    setupContractorSpinner(defaultContractorSpinner, contractors, true, false);
                    setupContractorSpinner(defaultCheckerSpinner, contractors, false, false);
                }
            });
        }
    }

    private void setupContractorSpinner(Spinner spinner, List<Contractors> contractors, boolean isContractor, boolean isActiveMode) {
        if (spinner == null) return;

        // Używamy dummy obiektu zamiast null, aby uniknąć NPE w wewnętrznych mechanizmach ArrayAdaptera
        Contractors nullContractor = new Contractors("Brak", "", "", "", -1, false);
        nullContractor.id = -1;
        
        List<Contractors> listWithNull = new ArrayList<>();
        listWithNull.add(nullContractor);
        if (contractors != null) {
            listWithNull.addAll(contractors);
        }

        ArrayAdapter<Contractors> adapter = new ArrayAdapter<Contractors>(this, android.R.layout.simple_spinner_item, listWithNull) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull android.view.ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                Contractors item = getItem(position);
                if (item != null && item.id != -1) {
                    tv.setText(item.name + " " + item.surname);
                } else {
                    tv.setText("Brak");
                }
                return tv;
            }

            @Override
            public View getDropDownView(int position, View convertView, @NonNull android.view.ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                Contractors item = getItem(position);
                if (item != null && item.id != -1) {
                    tv.setText(item.name + " " + item.surname);
                } else {
                    tv.setText("Brak");
                }
                return tv;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Ustawienie zaznaczenia na podstawie roli i flagi (aktywności lub domyślności)
        int targetType = isContractor ? 1 : 0;
        for (int i = 0; i < listWithNull.size(); i++) {
            Contractors c = listWithNull.get(i);
            if (c != null && c.type == targetType) {
                if (isActiveMode && c.isActive) {
                    spinner.setSelection(i);
                    break;
                } else if (!isActiveMode && c.isDefault) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }

        spinner.setOnTouchListener((v, event) -> {
            if (isActiveMode) {
                if (isContractor) isUserActionContractor = true;
                else isUserActionChecker = true;
            } else {
                if (isContractor) isUserActionDefaultContractor = true;
                else isUserActionDefaultChecker = true;
            }
            return false;
        });

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isActiveMode) {
                    if ((isContractor && !isUserActionContractor) || (!isContractor && !isUserActionChecker)) return;
                } else {
                    if ((isContractor && !isUserActionDefaultContractor) || (!isContractor && !isUserActionDefaultChecker)) return;
                }

                Contractors selected = (Contractors) parent.getItemAtPosition(position);
                if (selected != null) {
                    if (selected.id != -1) {
                        selected.type = isContractor ? 1 : 0;
                        if (isActiveMode) {
                            contractorsViewModel.setActive(selected);
                        } else {
                            contractorsViewModel.setDefault(selected);
                        }
                    } else {
                        // Opcja "Brak" - usuwamy zaznaczenie dla danej roli
                        if (isActiveMode) {
                            contractorsViewModel.deactivateAll(isContractor ? 1 : 0);
                        } else {
                            contractorsViewModel.deactivateAllDefaults(isContractor ? 1 : 0);
                        }
                    }
                }

                // Resetowanie flagi po wykonaniu akcji
                if (isActiveMode) {
                    if (isContractor) isUserActionContractor = false;
                    else isUserActionChecker = false;
                } else {
                    if (isContractor) isUserActionDefaultContractor = false;
                    else isUserActionDefaultChecker = false;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void addContractor() {
        if (inputName == null || inputSurname == null) return;

        String name = inputName.getText().toString().trim();
        String surname = inputSurname.getText().toString().trim();
        String licenseE = inputLicenseE != null ? inputLicenseE.getText().toString().trim() : "";
        String licenseD = inputLicenseD != null ? inputLicenseD.getText().toString().trim() : "";

        if (name.isEmpty() || surname.isEmpty()) {
            Toast.makeText(this, "Imię i nazwisko są wymagane", Toast.LENGTH_SHORT).show();
            return;
        }

        // Nowy pracownik domyślnie dostaje typ -1 (brak przypisanej roli przy tworzeniu)
        Contractors contractor = new Contractors(name, surname, licenseE, licenseD, -1, false);
        contractorsViewModel.insert(contractor);
        clearInputs();
    }

    private void clearInputs() {
        if (inputName != null) inputName.setText("");
        if (inputSurname != null) inputSurname.setText("");
        if (inputLicenseE != null) inputLicenseE.setText("");
        if (inputLicenseD != null) inputLicenseD.setText("");
        hideKeyboard();
    }

    private void updateContractorsList(List<Contractors> contractors) {
        if (flexboxLayout == null || contractors == null) return;

        flexboxLayout.removeAllViews();
        for (Contractors contractor : contractors) {
            if (contractor == null) continue;

            View itemView = getLayoutInflater().inflate(R.layout.contractor_item, flexboxLayout, false);
            TextView title = itemView.findViewById(R.id.clientAddressTitle);
            TextView nameView = itemView.findViewById(R.id.clientNameContractor);
            TextView surnameView = itemView.findViewById(R.id.clientSurname);
            TextView licenseDView = itemView.findViewById(R.id.clientD);
            TextView licenseEView = itemView.findViewById(R.id.clientE);

            // Dynamiczna zmiana etykiety - uproszczona wg prośby
            if (title != null) {
                title.setText("Pracownik:");
            }

            if (nameView != null) nameView.setText("Imię: " + contractor.name);
            if (surnameView != null) surnameView.setText("Nazwisko: " + contractor.surname);
            if (licenseDView != null) licenseDView.setText("Uprawnienia D: " + contractor.d_permit);
            if (licenseEView != null) licenseEView.setText("Uprawnienia E: " + contractor.e_permit);

            View deleteBtn = itemView.findViewById(R.id.clientDelete);
            if (deleteBtn != null) deleteBtn.setOnClickListener(v -> deleteContractor(contractor));

            View editBtn = itemView.findViewById(R.id.clientEdit);
            if (editBtn != null) editBtn.setOnClickListener(v -> editContractor(contractor, itemView));

            flexboxLayout.addView(itemView);
        }
    }

    private void deleteContractor(Contractors contractor) {
        new AlertDialog.Builder(this)
                .setTitle("Potwierdzenie")
                .setMessage("Czy na pewno chcesz usunąć?")
                .setPositiveButton("Tak", (dialog, which) -> contractorsViewModel.delete(contractor))
                .setNegativeButton("Nie", null)
                .show();
    }

    private void editContractor(Contractors contractor, View itemView) {
        LinearLayout dataLayout = itemView.findViewById(R.id.itemClientData);
        if (dataLayout == null) return;

        dataLayout.removeAllViews();

        EditText nameEdit = createEditText(contractor.name);
        EditText surnameEdit = createEditText(contractor.surname);
        EditText licenseDEdit = createEditText(contractor.d_permit);
        EditText licenseEEdit = createEditText(contractor.e_permit);

        dataLayout.addView(createLabel("Imię:"));
        dataLayout.addView(nameEdit);
        dataLayout.addView(createLabel("Nazwisko:"));
        dataLayout.addView(surnameEdit);
        dataLayout.addView(createLabel("Uprawnienia D:"));
        dataLayout.addView(licenseDEdit);
        dataLayout.addView(createLabel("Uprawnienia E:"));
        dataLayout.addView(licenseEEdit);

        Button editBtn = itemView.findViewById(R.id.clientEdit);
        Button deleteBtn = itemView.findViewById(R.id.clientDelete);

        if (editBtn != null) editBtn.setText("✅ Zapisz");
        if (deleteBtn != null) deleteBtn.setText("❌ Anuluj");

        if (deleteBtn != null) {
            deleteBtn.setOnClickListener(v -> {
                if (contractorsViewModel.getAllContractors() != null && contractorsViewModel.getAllContractors().getValue() != null) {
                    updateContractorsList(contractorsViewModel.getAllContractors().getValue());
                }
            });
        }

        if (editBtn != null) {
            editBtn.setOnClickListener(v -> {
                contractor.name = nameEdit.getText().toString();
                contractor.surname = surnameEdit.getText().toString();
                contractor.d_permit = licenseDEdit.getText().toString();
                contractor.e_permit = licenseEEdit.getText().toString();
                contractorsViewModel.update(contractor);
            });
        }
    }

    private EditText createEditText(String text) {
        EditText editText = new EditText(this);
        editText.setText(text);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        return editText;
    }

    private TextView createLabel(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        label.setPadding(0, 5, 0, 0);
        return label;
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view == null) view = new View(this);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}