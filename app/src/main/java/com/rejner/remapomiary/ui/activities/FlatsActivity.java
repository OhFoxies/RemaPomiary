package com.rejner.remapomiary.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.flexbox.FlexboxLayout;
import com.rejner.remapomiary.R;
import com.rejner.remapomiary.data.entities.BlockFullData;
import com.rejner.remapomiary.data.entities.Circuit;
import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.data.entities.OutletMeasurement;
import com.rejner.remapomiary.data.entities.RCD;
import com.rejner.remapomiary.data.entities.RoomInFlat;
import com.rejner.remapomiary.data.entities.Template;
import com.rejner.remapomiary.data.utils.LiveDataUtil;
import com.rejner.remapomiary.ui.utils.ProtocolWorker;
import com.rejner.remapomiary.ui.viewmodels.BlockViewModel;
import com.rejner.remapomiary.ui.viewmodels.CatalogViewModel;
import com.rejner.remapomiary.ui.viewmodels.CircuitViewModel;
import com.rejner.remapomiary.ui.viewmodels.FlatViewModel;
import com.rejner.remapomiary.ui.viewmodels.OutletMeasurementViewModel;
import com.rejner.remapomiary.ui.viewmodels.RCDViewModel;
import com.rejner.remapomiary.ui.viewmodels.RoomViewModel;
import com.rejner.remapomiary.ui.viewmodels.TemplateViewModel;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FlatsActivity extends AppCompatActivity {

    private FlatViewModel flatViewModel;
    private EditText inputFlatNumber;
    private Spinner templatesSpinner, sortBySpinner;
    private Button flatAddButton, flatCancelButton;
    private FlexboxLayout flatsContainer;
    private TextView noFlatsText;
    private ScrollView scrollView;

    private BlockViewModel blockViewModel;
    private CircuitViewModel circuitViewModel;
    private RCDViewModel rcdViewModel;
    private OutletMeasurementViewModel outletMeasurementViewModel;
    private RoomViewModel roomViewModel;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;

    private int blockId;
    private List<Flat> currentFlats;
    private final SimpleDateFormat creationDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private CatalogViewModel catalogViewModel;
    private BlockFullData block;
    private List<Template> templatesList;
    private TemplateViewModel templateViewModel;
    private final SimpleDateFormat editDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flats);
        scrollView = findViewById(R.id.mainScroll);

        blockId = getIntent().getIntExtra("blockId", -1);
        catalogViewModel = new ViewModelProvider(FlatsActivity.this).get(CatalogViewModel.class);
        templateViewModel = new ViewModelProvider(FlatsActivity.this).get(TemplateViewModel.class);

        if (blockId == -1) {
            Toast.makeText(this, "Błąd: nie przekazano ID bloku!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        blockViewModel = new ViewModelProvider(this).get(BlockViewModel.class);
        blockViewModel.getBlockById(blockId, block1 -> {
            block = block1;
            runOnUiThread(() -> {
                TextView textView = findViewById(R.id.flatsTitle);
                textView.setText("Mieszkania w bloku - " + block.block.street + "/" + block.block.number);
                setupTemplatesSpinner();
            });
        });
        flatViewModel = new ViewModelProvider(this).get(FlatViewModel.class);
        circuitViewModel = new ViewModelProvider(this).get(CircuitViewModel.class);
        rcdViewModel = new ViewModelProvider(this).get(RCDViewModel.class);
        roomViewModel = new ViewModelProvider(this).get(RoomViewModel.class);
        outletMeasurementViewModel = new ViewModelProvider(this).get(OutletMeasurementViewModel.class);

        inputFlatNumber = findViewById(R.id.inputFlatNumber);
        templatesSpinner = findViewById(R.id.templatesSpinner);
        sortBySpinner = findViewById(R.id.sortBySpinner);
        flatAddButton = findViewById(R.id.flatAdd);
        flatCancelButton = findViewById(R.id.flatCancel);
        flatsContainer = findViewById(R.id.clients);
        noFlatsText = findViewById(R.id.noFlats);

        setupSortSpinner();

        flatAddButton.setOnClickListener(v -> {
          createFlat();

        });

        flatCancelButton.setOnClickListener(v -> {
            inputFlatNumber.setText("");
            hideKeyboard();

            inputFlatNumber.clearFocus();

        });

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FlatsActivity.this, BlockActivity.class);
                intent.putExtra("blockId", block.block.id);
                startActivity(intent);

            }
        });
        flatViewModel.getFlatsByBlockId(blockId).observe(this, flats -> {
            currentFlats = flats;
            updateFlatsDisplay();
        });

    }
    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view == null) {
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
    private void createFlat() {
        String flatNumber = inputFlatNumber.getText().toString().trim();

        if (flatNumber.isEmpty()) {
            Toast.makeText(this, "Podaj numer mieszkania!", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean exists = false;
        if (currentFlats != null) {
            for (Flat f : currentFlats) {
                if (f.number.equalsIgnoreCase(flatNumber)) {
                    exists = true;
                    break;
                }
            }
        }

        if (exists) {
            Toast.makeText(this, "Mieszkanie o tym numerze już istnieje!", Toast.LENGTH_SHORT).show();
            return;
        }
        Template selectedTemplate = (Template) templatesSpinner.getSelectedItem();
        if (selectedTemplate.name.equals("Brak szablonu")) {
            Date now = new Date();
            Flat newFlat = new Flat();
            newFlat.number = flatNumber;
            newFlat.creation_date = now;
            newFlat.edition_date = now;
            newFlat.status = "Pomiar niewykonany ❌";
            newFlat.blockId = blockId;

            flatViewModel.insertWithId(newFlat, id -> {
                RCD newRcd = new RCD();
                newRcd.flatId = Math.toIntExact(id);
                newRcd.type = "A";
                rcdViewModel.insert(newRcd);
            });
            catalogViewModel.updateEdition(block.block.catalogId);
            blockViewModel.updateEdition(blockId);

            Toast.makeText(this, "Dodano mieszkanie nr " + flatNumber, Toast.LENGTH_SHORT).show();
            inputFlatNumber.setText("");
            hideKeyboard();
            inputFlatNumber.clearFocus();
        } else {
            LiveDataUtil.observeOnce(flatViewModel.getFlatById(selectedTemplate.flatId), this, flat -> {
                Date now = new Date();
                Flat newFlat = new Flat();
                newFlat.hasRCD = flat.hasRCD;
                newFlat.type = flat.type;
                newFlat.blockId = blockId;
                newFlat.number = flatNumber;
                newFlat.status = "Pomiar niewykonany ❌";
                newFlat.creation_date = now;
                newFlat.edition_date = now;
                flatViewModel.insertWithId(newFlat, id -> {
                    FlatsActivity.this.runOnUiThread(() -> {
                        LiveDataUtil.observeOnce(circuitViewModel.getCircuitsForFlat(selectedTemplate.flatId), FlatsActivity.this, circuits -> {
                            for (Circuit c : circuits) {
                                Circuit newCircuit = new Circuit();
                                newCircuit.flatId = Math.toIntExact(id);
                                newCircuit.name = c.name;
                                newCircuit.type = c.type;
                                circuitViewModel.insert(newCircuit);
                            }
                        });

                        if (newFlat.hasRCD == 1) {
                            LiveDataUtil.observeOnce(rcdViewModel.getRcdsForFlat(selectedTemplate.flatId), FlatsActivity.this, rcds -> {
                                for (RCD r : rcds) {
                                    RCD newRcd = new RCD();
                                    newRcd.name = r.name;
                                    newRcd.flatId = Math.toIntExact(id);
                                    newRcd.type = r.type;
                                    rcdViewModel.insert(newRcd);
                                }
                            });
                        } else {
                            RCD newRcd = new RCD();
                            newRcd.flatId = Math.toIntExact(id);
                            rcdViewModel.insert(newRcd);
                        }
                        LiveDataUtil.observeOnce(roomViewModel.getRoomsForFlat(selectedTemplate.flatId), FlatsActivity.this, roomInFlats -> {
                            for (RoomInFlat r : roomInFlats) {
                                RoomInFlat room = new RoomInFlat();
                                room.name = r.name;
                                room.flatId = Math.toIntExact(id);
                                roomViewModel.insertWithId(room, roomId -> {
                                    FlatsActivity.this.runOnUiThread(() -> {
                                        LiveDataUtil.observeOnce(outletMeasurementViewModel.getMeasurementsForRoom(r.id), FlatsActivity.this, outletMeasurements -> {
                                            for (OutletMeasurement o : outletMeasurements) {
                                                OutletMeasurement newOutlet = new OutletMeasurement();
                                                newOutlet.amps = o.amps;

                                                newOutlet.appliance = o.appliance;

                                                newOutlet.breakerType = o.breakerType;

                                                newOutlet.switchName = o.switchName;
                                                newOutlet.roomId = Math.toIntExact(roomId);
                                                outletMeasurementViewModel.insert(newOutlet, x -> {
                                                });
                                            }
                                        });
                                    });
                                });
                            }

                            Toast.makeText(this, "Dodano mieszkanie nr " + flatNumber, Toast.LENGTH_SHORT).show();
                            inputFlatNumber.setText("");
                            hideKeyboard();
                            inputFlatNumber.clearFocus();
                        });

                    });
                });
            });
        }

    }
    private void setupTemplatesSpinner() {
        LiveDataUtil.observeOnce(templateViewModel.getTemplatesInCatalog(block.catalog.id), this, templates -> {
            runOnUiThread(() -> {
                templatesList = templates;
                ArrayAdapter<Template> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, templatesList);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                Template template = new Template();
                template.name = "Brak szablonu";
                template.id = -1;
                templatesList.add(template);
                templatesSpinner.setAdapter(adapter);
            });
        });
    }
    private void setupSortSpinner() {
        String[] sortOptions = {"Numer mieszkania", "Data utworzenia \\/", "Data utworzenia /\\", "Data edycji", "Status", "Uwagi na początku"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sortOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortBySpinner.setAdapter(adapter);
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        int savedPosition = prefs.getInt("sort_option", 0);
        sortBySpinner.setSelection(savedPosition);
        sortBySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("sort_option", position);
                editor.apply();
                updateFlatsDisplay();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void createProtocols(int blockId, int catalogId, int flatId, int protocolNumber) {
        new AlertDialog.Builder(this)
                .setTitle("Potwierdzenie")
                .setMessage("Czy na pewno chcesz rozpocząć tworzenie protokołów? To trochę potrwa...")
                .setPositiveButton("Tak", (dialog, which) -> {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                                != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                                    REQUEST_NOTIFICATION_PERMISSION);
                            return;
                        }
                    }

                    startProtocolWorker(blockId, catalogId, flatId, protocolNumber);

                })
                .setNegativeButton("Nie", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void startProtocolWorker(int blockId, int catalogId, int flatId, int protocolNumber) {
        Data inputData = new Data.Builder()
                .putInt("blockId", blockId)
                .putInt("catalogId", catalogId)
                .putInt("flatId", flatId)
                .putInt("protocolNumber", protocolNumber)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ProtocolWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(getApplicationContext()).enqueue(request);

        Toast.makeText(this, "🔄 Generowanie protokołów rozpoczęte w tle.", Toast.LENGTH_LONG).show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "✅ Uprawnienie do powiadomień przyznane. Rozpocznij generowanie jeszcze raz.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "❌ Brak uprawnienia do powiadomień", Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        int scrollY = scrollView.getScrollY();

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        prefs.edit().putInt("scroll_y", scrollY).apply();
    }
    private void updateFlatsDisplay() {
        if (currentFlats == null) return;

        flatsContainer.removeAllViews();
        int readyCount = 0;
        for (Flat f : currentFlats) {
            if (f.status != null && f.status.toLowerCase().contains("gotowy")) {
                readyCount++;
            }
        }
        if (currentFlats.isEmpty()) {
            noFlatsText.setText("Brak mieszkań");
            return;
        } else {
            noFlatsText.setText("Znaleziono " + currentFlats.size() + " mieszkań (gotowe: " +readyCount + "/" + currentFlats.size() +")");

        }
        String selectedSort = (String) sortBySpinner.getSelectedItem();
        if (selectedSort.equals("Numer mieszkania")) {
            Collections.sort(currentFlats, Comparator.comparingInt(f -> {
                try {
                    String cleanedNumber = f.number.replaceAll("\\s+", "");
                    return Integer.parseInt(cleanedNumber);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }));
        } else if (selectedSort.equals("Data utworzenia \\/")) {
            Collections.sort(currentFlats, (f1, f2) -> f2.creation_date.compareTo(f1.creation_date));
        } else if (selectedSort.equals("Data utworzenia /\\")) {
            Collections.sort(currentFlats, (f1, f2) -> f1.creation_date.compareTo(f2.creation_date));
        }
        else if (selectedSort.equals("Data edycji")) {
            Collections.sort(currentFlats, (f1, f2) -> f2.edition_date.compareTo(f1.edition_date));
        } else if (selectedSort.equals("Status")) {
            Collections.sort(currentFlats, (f1, f2) -> {
                boolean done1 = f1.status.contains("gotowy");
                boolean done2 = f2.status.contains("gotowy");
                return Boolean.compare(done1, done2);
            });
        } else if(selectedSort.equals("Uwagi na początku")) {
            Collections.sort(currentFlats, (f1, f2) -> {
                boolean f1HasNotes = (f1.notes != null && !f1.notes.trim().isEmpty()) ||
                        (f1.circuitNotes != null && !f1.circuitNotes.trim().isEmpty());
                boolean f2HasNotes = (f2.notes != null && !f2.notes.trim().isEmpty()) ||
                        (f2.circuitNotes != null && !f2.circuitNotes.trim().isEmpty());

                if (f1HasNotes && !f2HasNotes) return -1; // f1 przed f2
                if (!f1HasNotes && f2HasNotes) return 1;  // f2 przed f1

                try {
                    int num1 = Integer.parseInt(f1.number.replaceAll("\\s+", ""));
                    int num2 = Integer.parseInt(f2.number.replaceAll("\\s+", ""));
                    return Integer.compare(num1, num2);
                } catch (NumberFormatException e) {
                    return f1.number.compareToIgnoreCase(f2.number);
                }
            });
        }

        for (Flat flat : currentFlats) {
            addFlatItemToFlex(flat);
        }
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);

        int scrollY = prefs.getInt("scroll_y", 0);

        if (scrollView != null) {
            scrollView.post(() -> scrollView.scrollTo(0, scrollY));
        } else {
            Log.e("FlatsActivity", "ScrollView is null! Check if R.id.mainScroll exists in activity_flats.xml");
        }

    }

    private void addFlatItemToFlex(Flat flat) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View itemView = inflater.inflate(R.layout.flat_item, flatsContainer, false);

        TextView title = itemView.findViewById(R.id.flatTitle);
        TextView creationDate = itemView.findViewById(R.id.flatCreationDate);
        TextView editDate = itemView.findViewById(R.id.flatLastEdited);
        TextView status = itemView.findViewById(R.id.blockLastEdited);
        TextView notes = itemView.findViewById(R.id.flatNotesDesc);
        Button markButton = itemView.findViewById(R.id.flatMark);
        Button deleteButton = itemView.findViewById(R.id.blockDelete);
        Button editButton = itemView.findViewById(R.id.blockEdit);
        Button createProtocolButton = itemView.findViewById(R.id.generateProtocol);
        EditText protocolNumber = itemView.findViewById(R.id.inputProtocolNumber);
        LinearLayout flatMain = itemView.findViewById(R.id.flatMain);

        title.setText("Mieszkanie nr " + flat.number);
        creationDate.setText(creationDateFormat.format(flat.creation_date));
        editDate.setText(editDateFormat.format(flat.edition_date));
        status.setText(flat.status);
        EditText titleEdit = new EditText(this);

        if ((flat.notes != null && !flat.notes.isEmpty()) || (flat.circuitNotes != null && !flat.circuitNotes.isEmpty())) {
            String finalString = "";
            if (!flat.notes.isEmpty()) {
                finalString += "Notaki:\n" + flat.notes + "\n";
            }
            if (!flat.circuitNotes.isEmpty()) {
                finalString += "Notatki rozdzielnia:\n" + flat.circuitNotes;

            }
            notes.setText(finalString);
        }
        titleEdit.setText(flat.number);
        titleEdit.setTextSize(24);
        titleEdit.setVisibility(View.GONE);

        flatMain.addView(titleEdit, 0);
        if (flat.status.contains("gotowy")) {
            markButton.setText("❌ Oznacz jako niewykonany");
            flatMain.setBackgroundResource(R.drawable.border_done);
            markButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF0000")));
        } else {
            markButton.setText("✅ Oznacz jako gotowy");
            flatMain.setBackgroundResource(R.drawable.border);
            markButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#3FAB1F")));
        }


        Button markBtn = markButton;
        TextView statusView = status;

        markBtn.setOnClickListener(v -> {
            boolean isDone = flat.status.contains("gotowy");

            if (isDone) {
                flat.status = "Pomiar niewykonany ❌";
                markBtn.setText("✅ Oznacz jako gotowy");
                flatMain.setBackgroundResource(R.drawable.border);
            } else {
                flat.status = "Pomiar gotowy ✅";
                markBtn.setText("❌ Oznacz jako niewykonany");
                flatMain.setBackgroundResource(R.drawable.border_done);
            }

            flat.edition_date = new Date();
            flatViewModel.update(flat);
            catalogViewModel.updateEdition(block.block.catalogId);
            blockViewModel.updateEdition(blockId);

            statusView.setText(flat.status);
        });

        editButton.setOnClickListener(v -> {
            boolean isEditing = titleEdit.getVisibility() == View.VISIBLE;

            if (!isEditing) {
                title.setVisibility(View.GONE);
                titleEdit.setVisibility(View.VISIBLE);

                editButton.setText("✅ Zapisz");
                deleteButton.setText("❌ Anuluj");
            } else {
                String newNumber = titleEdit.getText().toString().trim();

                if (newNumber.isEmpty()) {
                    Toast.makeText(this, "Numer mieszkania nie może być pusty!", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean exists = false;
                for (Flat f : currentFlats) {
                    if (f.number.equalsIgnoreCase(newNumber) && f.id != flat.id) {
                        exists = true;
                        break;
                    }
                }

                if (exists) {
                    Toast.makeText(this, "Mieszkanie o tym numerze już istnieje!", Toast.LENGTH_SHORT).show();
                    return;
                }
                hideKeyboard();
                titleEdit.clearFocus();
                flat.number = newNumber;
                flat.edition_date = new Date();
                flatViewModel.update(flat);
                blockViewModel.updateEdition(blockId);
                catalogViewModel.updateEdition(block.block.catalogId);


                title.setText(flat.number);
                title.setVisibility(View.VISIBLE);
                titleEdit.setVisibility(View.GONE);
                editButton.setText("✏️ Edytuj");
                deleteButton.setText("🗑️ Usuń");
            }
        });

        createProtocolButton.setOnClickListener(v ->{
            int protocolNumberNum;
            if (protocolNumber.getText().toString().isEmpty()) {
                protocolNumberNum = -1;
            } else {
                protocolNumberNum = Integer.parseInt(protocolNumber.getText().toString());
                Log.d("test", String.valueOf(protocolNumberNum));
            }

            createProtocols(blockId, block.catalog.id, flat.id, protocolNumberNum);
        });
        deleteButton.setOnClickListener(v -> {
            boolean isEditing = titleEdit.getVisibility() == View.VISIBLE;

            if (isEditing) {
                hideKeyboard();
                titleEdit.clearFocus();
                titleEdit.setVisibility(View.GONE);
                title.setVisibility(View.VISIBLE);
                editButton.setText("✏️ Edytuj");
                deleteButton.setText("🗑️ Usuń");
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(FlatsActivity.this);
                builder.setTitle("Potwierdzenie");
                builder.setMessage("Czy na pewno chcesz usunąć to mieszkanie?");
                builder.setPositiveButton("Tak", (dialog, which) -> {
                    flatViewModel.delete(flat);
                    blockViewModel.updateEdition(blockId);
                    catalogViewModel.updateEdition(block.block.catalogId);
                    Toast.makeText(FlatsActivity.this, "Usunięto mieszkanie nr " + flat.number, Toast.LENGTH_SHORT).show();
                });
                builder.setNegativeButton("Nie", (dialog, which) -> dialog.dismiss());
                builder.show();
            }
        });

        itemView.setOnClickListener(v -> {
            boolean isEditing = titleEdit.getVisibility() == View.VISIBLE;
            if (isEditing) {
                return;
            }
            Intent intent = new Intent(FlatsActivity.this, BoardActivity.class);
            intent.putExtra("flatId", flat.id);
            startActivity(intent);
        });
        flatsContainer.addView(itemView);
    }

}
