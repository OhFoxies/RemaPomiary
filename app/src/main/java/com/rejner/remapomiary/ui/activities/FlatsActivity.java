package com.rejner.remapomiary.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.rejner.remapomiary.R;
import com.rejner.remapomiary.adapters.FlatAdapter;
import com.rejner.remapomiary.data.entities.BlockFullData;
import com.rejner.remapomiary.data.entities.Circuit;
import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.data.entities.OutletMeasurement;
import com.rejner.remapomiary.data.entities.RCD;
import com.rejner.remapomiary.data.entities.RoomInFlat;
import com.rejner.remapomiary.data.entities.Template;
import com.rejner.remapomiary.data.utils.LiveDataUtil;
import com.rejner.remapomiary.ui.utils.Actions;
import com.rejner.remapomiary.ui.utils.ProtocolWorker;
import com.rejner.remapomiary.ui.viewmodels.BlockViewModel;
import com.rejner.remapomiary.ui.viewmodels.CatalogViewModel;
import com.rejner.remapomiary.ui.viewmodels.CircuitViewModel;
import com.rejner.remapomiary.ui.viewmodels.FlatViewModel;
import com.rejner.remapomiary.ui.viewmodels.OutletMeasurementViewModel;
import com.rejner.remapomiary.ui.viewmodels.RCDViewModel;
import com.rejner.remapomiary.ui.viewmodels.RoomViewModel;
import com.rejner.remapomiary.ui.viewmodels.TemplateViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class FlatsActivity extends AppCompatActivity implements FlatAdapter.OnFlatActionListener {

    private FlatViewModel flatViewModel;
    private FlatAdapter flatAdapter;
    private RecyclerView recyclerView;

    private BlockViewModel blockViewModel;
    private CircuitViewModel circuitViewModel;
    private RCDViewModel rcdViewModel;
    private OutletMeasurementViewModel outletMeasurementViewModel;
    private RoomViewModel roomViewModel;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;
    
    private int blockId;
    private List<Flat> currentFlats;
    private CatalogViewModel catalogViewModel;
    private BlockFullData block;
    private List<Template> templatesList;
    private TemplateViewModel templateViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flats);

        blockId = getIntent().getIntExtra("blockId", -1);
        catalogViewModel = new ViewModelProvider(this).get(CatalogViewModel.class);
        templateViewModel = new ViewModelProvider(this).get(TemplateViewModel.class);

        if (blockId == -1) {
            Toast.makeText(this, "Błąd: nie przekazano ID bloku!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        blockViewModel = new ViewModelProvider(this).get(BlockViewModel.class);
        blockViewModel.getBlockById(blockId, blockData -> {
            block = blockData;
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

        setupRecyclerView();

        findViewById(R.id.backButton).setOnClickListener(v -> {
            if (block != null) {
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

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.flatsRecyclerView);
        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(this);
        layoutManager.setFlexDirection(FlexDirection.ROW);
        layoutManager.setFlexWrap(FlexWrap.WRAP); // Now items will correctly move to next row
        layoutManager.setJustifyContent(JustifyContent.SPACE_EVENLY);
        recyclerView.setLayoutManager(layoutManager);
        
        flatAdapter = new FlatAdapter(this);
        recyclerView.setAdapter(flatAdapter);
    }

    private void setupTemplatesSpinner() {
        LiveDataUtil.observeOnce(templateViewModel.getTemplatesInCatalog(block.catalog.id), this, templates -> {
            runOnUiThread(() -> {
                templatesList = new ArrayList<>(templates);
                Template empty = new Template();
                empty.name = "Brak szablonu";
                empty.id = -1;
                templatesList.add(0, empty);
                updateFlatsDisplay();
            });
        });
    }

    @Override
    public void onCreateFlat(String flatNumber, Template selectedTemplate) {
        if (flatNumber.isEmpty()) {
            Toast.makeText(this, "Podaj numer mieszkania!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentFlats != null) {
            for (Flat f : currentFlats) {
                if (f.number.equalsIgnoreCase(flatNumber)) {
                    Toast.makeText(this, "Mieszkanie o tym numerze już istnieje!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }

        if (selectedTemplate == null || selectedTemplate.id == -1) {
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
            updateMetadata();
            Toast.makeText(this, "Dodano mieszkanie nr " + flatNumber, Toast.LENGTH_SHORT).show();
            hideKeyboard();
        } else {
            applyTemplate(selectedTemplate, flatNumber);
        }
    }

    private void applyTemplate(Template selectedTemplate, String flatNumber) {
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
                long newFlatId = id;
                runOnUiThread(() -> {
                    LiveDataUtil.observeOnce(circuitViewModel.getCircuitsForFlat(selectedTemplate.flatId), FlatsActivity.this, circuits -> {
                        for (Circuit c : circuits) {
                            Circuit nc = new Circuit();
                            nc.flatId = (int) newFlatId;
                            nc.name = c.name;
                            nc.type = c.type;
                            circuitViewModel.insert(nc);
                        }
                    });

                    if (newFlat.hasRCD == 1) {
                        LiveDataUtil.observeOnce(rcdViewModel.getRcdsForFlat(selectedTemplate.flatId), FlatsActivity.this, rcds -> {
                            for (RCD r : rcds) {
                                RCD newRcd = new RCD();
                                newRcd.name = r.name;
                                newRcd.flatId = (int) newFlatId;
                                newRcd.type = r.type;
                                rcdViewModel.insert(newRcd);
                            }
                        });
                    } else {
                        RCD newRcd = new RCD();
                        newRcd.flatId = (int) newFlatId;
                        rcdViewModel.insert(newRcd);
                    }

                    LiveDataUtil.observeOnce(roomViewModel.getRoomsForFlat(selectedTemplate.flatId), FlatsActivity.this, roomInFlats -> {
                        for (RoomInFlat r : roomInFlats) {
                            RoomInFlat room = new RoomInFlat();
                            room.name = r.name;
                            room.flatId = (int) newFlatId;
                            roomViewModel.insertWithId(room, roomId -> {
                                runOnUiThread(() -> {
                                    LiveDataUtil.observeOnce(outletMeasurementViewModel.getMeasurementsForRoom(r.id), FlatsActivity.this, outletMeasurements -> {
                                        for (OutletMeasurement o : outletMeasurements) {
                                            OutletMeasurement newOutlet = new OutletMeasurement();
                                            newOutlet.amps = o.amps;
                                            newOutlet.appliance = o.appliance;
                                            newOutlet.breakerType = o.breakerType;
                                            newOutlet.switchName = o.switchName;
                                            newOutlet.roomId = Math.toIntExact(roomId);
                                            outletMeasurementViewModel.insert(newOutlet, x -> {});
                                        }
                                    });
                                });
                            });
                        }
                        Toast.makeText(this, "Dodano mieszkanie nr " + flatNumber, Toast.LENGTH_SHORT).show();
                        hideKeyboard();
                    });
                });
            });
        });
    }

    @Override
    public void onSortSelected(int position) {
        getSharedPreferences("settings", MODE_PRIVATE).edit().putInt("sort_option", position).apply();
        updateFlatsDisplay();
    }

    private void updateMetadata() {
        if (block != null) {
            catalogViewModel.updateEdition(block.block.catalogId);
            blockViewModel.updateEdition(blockId);
        }
    }

    private void updateFlatsDisplay() {
        if (currentFlats == null) return;
        List<Flat> sortedFlats = new ArrayList<>(currentFlats);
        
        int readyCount = 0;
        for (Flat f : sortedFlats) if (f.status != null && f.status.contains("gotowy")) readyCount++;
        String countText = sortedFlats.isEmpty() ? "Brak mieszkań" : "Znaleziono " + sortedFlats.size() + " mieszkań (gotowe: " + readyCount + "/" + sortedFlats.size() + ")";

        int sortPos = getSharedPreferences("settings", MODE_PRIVATE).getInt("sort_option", 0);
        
        switch (sortPos) {
            case 0: // Numer mieszkania
                Collections.sort(sortedFlats, Comparator.comparingInt(f -> {
                    try { return Integer.parseInt(f.number.replaceAll("\\s+", "")); } 
                    catch (NumberFormatException e) { return 0; }
                }));
                break;
            case 1: // Data utworzenia \/
                Collections.sort(sortedFlats, (f1, f2) -> f2.creation_date.compareTo(f1.creation_date));
                break;
            case 2: // Data utworzenia /\
                Collections.sort(sortedFlats, (f1, f2) -> f1.creation_date.compareTo(f2.creation_date));
                break;
            case 3: // Data edycji
                Collections.sort(sortedFlats, (f1, f2) -> f2.edition_date.compareTo(f1.edition_date));
                break;
            case 4: // Status
                Collections.sort(sortedFlats, (f1, f2) -> {
                    boolean done1 = f1.status != null && f1.status.contains("gotowy");
                    boolean done2 = f2.status != null && f2.status.contains("gotowy");
                    return Boolean.compare(done1, done2);
                });
                break;
            case 5: // Uwagi na początku
                Collections.sort(sortedFlats, (f1, f2) -> {
                    boolean f1HasNotes = (f1.notes != null && !f1.notes.trim().isEmpty()) || (f1.circuitNotes != null && !f1.circuitNotes.trim().isEmpty());
                    boolean f2HasNotes = (f2.notes != null && !f2.notes.trim().isEmpty()) || (f2.circuitNotes != null && !f2.circuitNotes.trim().isEmpty());
                    if (f1HasNotes && !f2HasNotes) return -1;
                    if (!f1HasNotes && f2HasNotes) return 1;
                    return f1.number.compareToIgnoreCase(f2.number);
                });
                break;
        }
        
        flatAdapter.setHeaderData(templatesList, sortPos, countText);
        flatAdapter.submitList(sortedFlats);
    }

    @Override public void onFlatClick(Flat flat) {
        startActivity(new Intent(this, BoardActivity.class).putExtra("flatId", flat.id));
    }

    @Override public void onFlatDelete(Flat flat) {
        new AlertDialog.Builder(this).setTitle("Potwierdzenie").setMessage("Usunąć mieszkanie?")
                .setPositiveButton("Tak", (d, w) -> { flatViewModel.delete(flat); updateMetadata(); })
                .setNegativeButton("Nie", null).show();
    }

    @Override public void onFlatEdit(Flat flat, String newNumber) {
        if (newNumber.isEmpty()) return;
        flat.number = newNumber;
        flat.edition_date = new Date();
        flatViewModel.update(flat);
        updateMetadata();
    }

    @Override public void onFlatMark(Flat flat) {
        if (flat.status != null && flat.status.contains("gotowy")) Actions.markUnready(flat, this);
        else Actions.saveAndMarkReady(flat, this);
        flat.edition_date = new Date();
        flatViewModel.update(flat);
        updateMetadata();
    }

    @Override public void onGenerateProtocol(Flat flat, int protocolNumber) {
        new AlertDialog.Builder(this).setTitle("Potwierdzenie").setMessage("Generować protokół?")
                .setPositiveButton("Tak", (d, w) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
                        return;
                    }
                    startProtocolWorker(blockId, block.catalog.id, flat.id, protocolNumber);
                })
                .setNegativeButton("Nie", null).show();
    }

    private void startProtocolWorker(int b, int c, int f, int p) {
        Data data = new Data.Builder().putInt("blockId", b).putInt("catalogId", c).putInt("flatId", f).putInt("protocolNumber", p).build();
        WorkManager.getInstance(getApplicationContext()).enqueue(new OneTimeWorkRequest.Builder(ProtocolWorker.class).setInputData(data).build());
        Toast.makeText(this, "🔄 Rozpoczęto generowanie.", Toast.LENGTH_SHORT).show();
    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View v = getCurrentFocus();
        if (imm != null && v != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }
}
