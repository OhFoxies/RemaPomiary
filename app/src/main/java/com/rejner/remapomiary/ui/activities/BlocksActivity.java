package com.rejner.remapomiary.ui.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.rejner.remapomiary.data.entities.Block;
import com.rejner.remapomiary.data.entities.BlockFullData;
import com.rejner.remapomiary.data.entities.Catalog;
import com.rejner.remapomiary.data.entities.Client;
import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.data.utils.LiveDataUtil;
import com.rejner.remapomiary.generator.ProtocolGenerator;
import com.rejner.remapomiary.ui.utils.PostalCodeTextWatcher;
import com.rejner.remapomiary.ui.utils.ProtocolWorker;
import com.rejner.remapomiary.ui.viewmodels.BlockViewModel;
import com.rejner.remapomiary.ui.viewmodels.CatalogViewModel;
import com.rejner.remapomiary.ui.viewmodels.ClientViewModel;
import com.rejner.remapomiary.ui.viewmodels.ContractorsViewModel;
import com.rejner.remapomiary.ui.viewmodels.FlatViewModel;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlocksActivity extends AppCompatActivity {


    private int catalogId;
    private boolean sortByCreation = false;
    private ArrayAdapter arrayAdapter;
    private BlockViewModel blockViewModel;
    private CatalogViewModel catalogViewModel;
    private ContractorsViewModel contractorsViewModel;
    private Catalog catalog;
    private EditText city;
    private EditText street;
    private EditText postal_code;
    private EditText number;
    private CheckBox checkHouse;
    private List<EditText> inputs;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;

    private Spinner spinnerCreation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocks);
        city = findViewById(R.id.inputBlockCity);
        street = findViewById(R.id.inputBlockStreet);
        number = findViewById(R.id.inputBlockNumber);
        postal_code = findViewById(R.id.inputBlockPostalCode);
        checkHouse = findViewById(R.id.checkHouse);
        spinnerCreation = findViewById(R.id.spinner);
        postal_code.addTextChangedListener(new PostalCodeTextWatcher(postal_code));

        inputs = Arrays.asList(city, street, postal_code, number);
        catalogId = getIntent().getIntExtra("catalogId", 0);
        blockViewModel = new ViewModelProvider(this).get(BlockViewModel.class);
        catalogViewModel = new ViewModelProvider(BlocksActivity.this).get(CatalogViewModel.class);
        contractorsViewModel = new ViewModelProvider(this).get(ContractorsViewModel.class);
        catalogViewModel.getCatalogById(catalogId, catalog1 -> {
            catalog = catalog1;
            runOnUiThread(() -> {
                updateDisplay();
            });
        });
        blockViewModel.getBlocksWithFullData(catalogId).observe(this, this::updateView);
        List<String> sortOptions = Arrays.asList("Data utworzenia", "Data edycji");

        arrayAdapter = new ArrayAdapter(BlocksActivity.this, android.R.layout.simple_spinner_item, sortOptions);
        Spinner spinnerSort = findViewById(R.id.sortBySpinner);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerSort.setAdapter(arrayAdapter);
        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean before = sortByCreation;
                sortByCreation = sortOptions.get(position).equals("Data edycji");
                if (sortByCreation != before) {
                    LiveDataUtil.observeOnce(blockViewModel.getBlocksWithFullData(catalogId), BlocksActivity.this, blocks -> {
                        updateBlocks(blocks);
                    });
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
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

    private void updateView(List<BlockFullData> blocks) {
        updateBlocks(blocks);
    }

    private void resetBlockInput() {
        TextView blocksTitle = findViewById(R.id.blocksTitle);
        blocksTitle.setText("Budynki dla katalogu - " + catalog.title);
        city.setText(catalog.city);
        street.setText(catalog.street);
        postal_code.setText(catalog.postal_code);
        number.setText("");
        hideKeyboard();

        for (EditText editText : inputs) {
            editText.clearFocus();
        }
    }

    private void createClientsSpinner(Spinner spinnerItem) {
        ClientViewModel clientViewModel = new ViewModelProvider(BlocksActivity.this).get(ClientViewModel.class);

        LiveDataUtil.observeOnce(clientViewModel.getClientsInCatalog(catalogId), this, clients -> {

            if (clients.isEmpty()) {
                Client emptyClient = new Client("", "", "", "Brak zleceniodawców", -1);
                clients.add(emptyClient);
            }
            ArrayAdapter clientsAdapter = new ArrayAdapter(BlocksActivity.this, android.R.layout.simple_spinner_dropdown_item, clients);

            spinnerItem.setAdapter(clientsAdapter);
        });
    }

    private void updateDisplay() {

        createClientsSpinner(spinnerCreation);
        resetBlockInput();

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(BlocksActivity.this, CatalogActivity.class);
                intent.putExtra("catalogId", catalog.id);
                startActivity(intent);
            }
        });

        Button createButton = findViewById(R.id.blockAdd);
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createBlock();
            }
        });
        Button cancelButton = findViewById(R.id.blockCancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetBlockInput();
            }
        });
    }

    private void createBlock() {
        for (EditText input : inputs) {
            if (input.getText().toString().isEmpty()) {
                input.setError("Wymagane pole");
                Toast.makeText(BlocksActivity.this, input.getHint().toString() + " nie jest podany/e", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        Client selectedClient = (Client) spinnerCreation.getSelectedItem();
        if (selectedClient.name.equals("Brak zleceniodawców")) {
            Toast.makeText(BlocksActivity.this, "Nie wybrano zleceniodawcy!", Toast.LENGTH_SHORT).show();
            return;
        }

        LiveDataUtil.observeOnce(contractorsViewModel.getAllContractors(), this, contractors -> {
            boolean hasActiveContractor = false;
            boolean hasActiveChecker = false;
            boolean hasDefaultContractor = false;
            boolean hasDefaultChecker = false;

            if (contractors != null) {
                for (com.rejner.remapomiary.data.entities.Contractors c : contractors) {
                    if (c.type == 1 && c.isActive) hasActiveContractor = true;
                    if (c.type == 0 && c.isActive) hasActiveChecker = true;
                    if (c.type == 1 && c.isDefault) hasDefaultContractor = true;
                    if (c.type == 0 && c.isDefault) hasDefaultChecker = true;
                }
            }

            if (!hasActiveContractor || !hasActiveChecker || !hasDefaultContractor || !hasDefaultChecker) {
                Toast.makeText(this, "Błąd: Brak przypisanych aktywnych lub domyślnych pracowników!", Toast.LENGTH_LONG).show();
                return;
            }

            Block newBlock = new Block(catalogId, street.getText().toString(), city.getText().toString(), number.getText().toString(), postal_code.getText().toString(), selectedClient.id, new Date(), new Date(), checkHouse.isChecked() ? 1 : 0);
            blockViewModel.insertWithId(newBlock, id -> {
                Date now = new Date();
                FlatViewModel flatViewModel = new ViewModelProvider(this).get(FlatViewModel.class);

                Flat commonSpace = new Flat();
                commonSpace.isCommonSpace = 1;
                commonSpace.blockId = Math.toIntExact(id);
                commonSpace.number = "Część wspólna";
                commonSpace.creation_date = now;
                commonSpace.edition_date = now;
                commonSpace.status = "";
                flatViewModel.insert(commonSpace);
            });

            catalogViewModel.updateEdition(catalogId);
            resetBlockInput();
        });
    }

    private void updateBlocks(List<BlockFullData> blocks) {

        TextView noBlocks = findViewById(R.id.noBlocks);

        if (blocks.isEmpty()) {
            noBlocks.setVisibility(View.VISIBLE);
            return;
        } else {
            noBlocks.setVisibility(View.GONE);
        }

        if (!sortByCreation) {
            blocks.sort(Comparator.comparing((BlockFullData b) -> b.block.edition_date).reversed());

        } else {
            blocks.sort(Comparator.comparing((BlockFullData b) -> b.block.creation_date).reversed());
        }

        FlexboxLayout flexboxLayout = findViewById(R.id.blocks);
        flexboxLayout.removeAllViews();
        for (BlockFullData block : blocks) {
            View blockView = getLayoutInflater().inflate(R.layout.block_item, flexboxLayout, false);

            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            SimpleDateFormat sdfh = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());

            TextView street = blockView.findViewById(R.id.blockStreet);
            TextView city = blockView.findViewById(R.id.blockCity);
            TextView number = blockView.findViewById(R.id.blockNumber);
            TextView postalCode = blockView.findViewById(R.id.blockPostalCode);
            TextView clientName = blockView.findViewById(R.id.blockClientName);
            TextView clientAddress = blockView.findViewById(R.id.blockClientAddress);
            TextView blockCreationDate = blockView.findViewById(R.id.blockCreationDate);
            TextView blockEditionDate = blockView.findViewById(R.id.blockLastEdited);
            TextView blockTitle = blockView.findViewById(R.id.blockTitle);

            blockCreationDate.setText(sdf.format(block.block.creation_date));
            blockEditionDate.setText(sdfh.format(block.block.edition_date));
            street.setText(block.block.street);
            city.setText(block.block.city);
            number.setText(block.block.number);
            postalCode.setText(block.block.postal_code);
            clientName.setText(block.getClient().name);
            clientAddress.setText(block.getClient().city + ", " + block.getClient().street + ", " + block.getClient().postal_code);
            String typePrefix = block.block.buildingType == 1 ? "Dom - " : "Blok - ";
            blockTitle.setText(typePrefix + block.block.number);

            Button deleteButton = blockView.findViewById(R.id.blockDelete);
            Button editButton = blockView.findViewById(R.id.blockEdit);
            Button createPro = blockView.findViewById(R.id.createProtocols);
            Button quickSummary = blockView.findViewById(R.id.quickSummary);
            Button copyHouse = blockView.findViewById(R.id.copyHouse);

            if (block.block.buildingType == 1) {
                quickSummary.setVisibility(View.GONE);
                copyHouse.setVisibility(View.VISIBLE);
                copyHouse.setOnClickListener(v -> {
                    showCopyHouseDialog(block.block.id);
                });
            } else {
                quickSummary.setVisibility(View.VISIBLE);
                copyHouse.setVisibility(View.GONE);
                quickSummary.setOnClickListener(v -> {
                    startSummaryWorker(block.block.id, catalogId);
                });
            }

            createPro.setOnClickListener(v -> {
                createProtocols(block.block.id, catalogId);
            });

            blockView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openBlock(block);
                }
            });
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteBlock(block);
                }
            });

            editButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateBlock(block, blockView, editButton, deleteButton);
                }
            });
            flexboxLayout.addView(blockView);
        }
    }

    private void createProtocols(int blockId, int catalogId) {
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

                    startProtocolWorker(blockId, catalogId);

                })
                .setNegativeButton("Nie", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void startProtocolWorker(int blockId, int catalogId) {
        Data inputData = new Data.Builder()
                .putInt("blockId", blockId)
                .putInt("catalogId", catalogId)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ProtocolWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(getApplicationContext()).enqueue(request);

        Toast.makeText(this, "🔄 Generowanie protokołów rozpoczęte w tle.", Toast.LENGTH_LONG).show();
    }

    private void startSummaryWorker(int blockId, int catalogId) {
        Data inputData = new Data.Builder()
                .putInt("blockId", blockId)
                .putInt("catalogId", catalogId)
                .putBoolean("isSummary", true)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ProtocolWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(getApplicationContext()).enqueue(request);

        Toast.makeText(this, "🔄 Generowanie podsumowania rozpoczęte w tle.", Toast.LENGTH_LONG).show();
    }

    private void showCopyHouseDialog(int blockId) {
        EditText input = new EditText(this);
        input.setHint("Wpisz nowy numer domu");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);

        new AlertDialog.Builder(this)
                .setTitle("Kopiuj dom")
                .setMessage("Podaj numer dla nowej kopii domu. Wszystkie dane zostaną skopiowane bez wartości pomiarów, zdjęć i uwag.")
                .setView(input)
                .setPositiveButton("Kopiuj", (dialog, which) -> {
                    String newNumber = input.getText().toString().trim();
                    if (newNumber.isEmpty()) {
                        Toast.makeText(this, "Numer nie może być pusty", Toast.LENGTH_SHORT).show();
                    } else {
                        copyHouseTask(blockId, newNumber);
                    }
                })
                .setNegativeButton("Anuluj", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void copyHouseTask(int blockId, String newNumber) {
        Toast.makeText(this, "Rozpoczęto kopiowanie domu...", Toast.LENGTH_SHORT).show();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                com.rejner.remapomiary.data.db.AppDatabase db = com.rejner.remapomiary.data.db.AppDatabase.getDatabase(getApplicationContext());
                Block originalBlock = db.blockDao().getBlockById(blockId).block;
                
                // 1. Copy Block
                Block newBlock = new Block(originalBlock.catalogId, originalBlock.street, originalBlock.city, newNumber, originalBlock.postal_code, originalBlock.clientId, new Date(), new Date(), originalBlock.buildingType);
                long newBlockId = db.blockDao().insertWithId(newBlock);

                // 2. Copy CommonSpaceInfo (if exists)
                com.rejner.remapomiary.data.entities.CommonSpaceInfo csInfo = db.commonSpaceInfoDao().getInfoByBlockIdSync(blockId);
                if (csInfo != null) {
                    com.rejner.remapomiary.data.entities.CommonSpaceInfo newCsInfo = new com.rejner.remapomiary.data.entities.CommonSpaceInfo();
                    newCsInfo.blockId = (int) newBlockId;
                    newCsInfo.switchName = csInfo.switchName;
                    newCsInfo.breakerType = csInfo.breakerType;
                    newCsInfo.amps = csInfo.amps;
                    newCsInfo.ohmsBase = 0.0; // Reset ohmsBase
                    db.commonSpaceInfoDao().insert(newCsInfo);
                }

                // 3. Copy Flat (Common Space)
                Flat originalFlat = db.flatDao().getCommonSpaceSync(blockId);
                if (originalFlat != null) {
                    Flat newFlat = new Flat();
                    newFlat.blockId = (int) newBlockId;
                    newFlat.number = originalFlat.number;
                    newFlat.hasRCD = originalFlat.hasRCD;
                    newFlat.type = originalFlat.type;
                    newFlat.creation_date = new Date();
                    newFlat.edition_date = new Date();
                    newFlat.isCommonSpace = 1;
                    newFlat.status = "";
                    newFlat.grade = 0;
                    newFlat.gradeByUser = 0;
                    newFlat.notes = "";
                    newFlat.notesProtocol = "";
                    newFlat.circuitNotes = "";
                    
                    long newFlatId = db.flatDao().insertWithId(newFlat);

                    // 3.1 Copy Boards
                    List<com.rejner.remapomiary.data.entities.BoardCommonSpace> boards = db.boardCommonSpaceDao().getBoardsForFlatSync(originalFlat.id);
                    for (com.rejner.remapomiary.data.entities.BoardCommonSpace board : boards) {
                        com.rejner.remapomiary.data.entities.BoardCommonSpace newBoard = new com.rejner.remapomiary.data.entities.BoardCommonSpace();
                        newBoard.flatId = (int) newFlatId;
                        newBoard.name = board.name;
                        newBoard.type = board.type;
                        newBoard.notes = "";
                        newBoard.creation_date = new Date();
                        
                        long newBoardId = db.boardCommonSpaceDao().insert(newBoard);

                        // 3.1.1 Copy Circuits (Common Space)
                        List<com.rejner.remapomiary.data.entities.CircuitCommonSpace> circuits = db.circuitCommonSpaceDao().getCircuitsForBoardSync(board.id);
                        if (circuits == null) {
                            // Fallback if generic getCircuitsForBoardSync doesn't exist, we might need to use 1f and 3f separately
                            circuits = new ArrayList<>();
                            circuits.addAll(db.circuitCommonSpaceDao().getCircuitsForBoardSync1f(board.id));
                            circuits.addAll(db.circuitCommonSpaceDao().getCircuitsForBoardSync3f(board.id));
                        }
                        for (com.rejner.remapomiary.data.entities.CircuitCommonSpace circuit : circuits) {
                            com.rejner.remapomiary.data.entities.CircuitCommonSpace newCircuit = new com.rejner.remapomiary.data.entities.CircuitCommonSpace();
                            newCircuit.boardId = (int) newBoardId;
                            newCircuit.name = circuit.name;
                            newCircuit.type = circuit.type;
                            newCircuit.notes = "";
                            db.circuitCommonSpaceDao().insert(newCircuit);
                        }
                    }

                    // 3.2 Copy Rooms
                    List<com.rejner.remapomiary.data.entities.RoomInFlat> rooms = db.roomDao().getRoomsForFlatSync(originalFlat.id);
                    for (com.rejner.remapomiary.data.entities.RoomInFlat room : rooms) {
                        com.rejner.remapomiary.data.entities.RoomInFlat newRoom = new com.rejner.remapomiary.data.entities.RoomInFlat();
                        newRoom.flatId = (int) newFlatId;
                        newRoom.name = room.name;
                        
                        long newRoomId = db.roomDao().insertWithId(newRoom);

                        // 3.2.1 Copy Measurements
                        List<com.rejner.remapomiary.data.entities.OutletMeasurement> measurements = db.outletMeasurementDao().getMeasurementsForRoomSync(room.id);
                        for (com.rejner.remapomiary.data.entities.OutletMeasurement om : measurements) {
                            com.rejner.remapomiary.data.entities.OutletMeasurement newOm = new com.rejner.remapomiary.data.entities.OutletMeasurement();
                            newOm.roomId = (int) newRoomId;
                            newOm.number = om.number;
                            newOm.appliance = om.appliance;
                            newOm.switchName = om.switchName;
                            newOm.breakerType = om.breakerType;
                            newOm.amps = om.amps;
                            newOm.ohms = 0.0; // Reset measurement
                            newOm.note = "brak uwag";
                            newOm.rcdStatus = om.rcdStatus != 0 ? 1 : 0; // If it was RCD, keep it as RCD but reset to good
                            newOm.rcdName = om.rcdName;
                            newOm.rcdCurrent = om.rcdCurrent;
                            newOm.rcdTime = null; // Reset time
                            newOm.photoPath = ""; // Clear photos
                            db.outletMeasurementDao().insert(newOm);
                        }
                    }
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, "Dom został skopiowany pomyślnie!", Toast.LENGTH_SHORT).show();
                    catalogViewModel.updateEdition(catalogId);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Błąd podczas kopiowania domu", Toast.LENGTH_LONG).show());
            }
        });
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

    private void openBlock(BlockFullData block) {
        if (block.block.buildingType == 1) {
            LiveDataUtil.observeOnce(new ViewModelProvider(this).get(FlatViewModel.class).getCommonSpace(block.block.id), this, flat -> {
                if (flat != null) {
                    Intent intent = new Intent(BlocksActivity.this, BoardCommonSpace.class);
                    intent.putExtra("flatId", flat.id);
                    intent.putExtra("blockId", block.block.id);
                    intent.putExtra("commonSpace", 1);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(BlocksActivity.this, BlockActivity.class);
                    intent.putExtra("blockId", block.block.id);
                    startActivity(intent);
                }
            });
            return;
        }
        Intent intent = new Intent(BlocksActivity.this, BlockActivity.class);
        intent.putExtra("blockId", block.block.id);
        startActivity(intent);
    }

    private void deleteBlock(BlockFullData block) {
        AlertDialog.Builder builder = new AlertDialog.Builder(BlocksActivity.this);
        builder.setTitle("Potwierdzenie");
        builder.setMessage("Czy na pewno chcesz usunąć ten budynek?");
        builder.setPositiveButton("Tak", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                blockViewModel.repository.delete(block.block);
                catalogViewModel.updateEdition(catalogId);
                Toast.makeText(BlocksActivity.this, "Budynek oraz jego zawartość została usunięta", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Nie", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    private void updateBlock(BlockFullData block, View blockView, Button editButton, Button deleteButton) {
        blockView.setOnClickListener(null);

        TextView street = blockView.findViewById(R.id.blockStreet);
        TextView city = blockView.findViewById(R.id.blockCity);
        TextView number = blockView.findViewById(R.id.blockNumber);
        TextView postalCode = blockView.findViewById(R.id.blockPostalCode);
        TextView clientName = blockView.findViewById(R.id.blockClientName);
        TextView clientAddress = blockView.findViewById(R.id.blockClientAddress);
        TextView blockCreationDate = blockView.findViewById(R.id.blockCreationDate);
        TextView blockEditionDate = blockView.findViewById(R.id.blockLastEdited);
        TextView blockTitle = blockView.findViewById(R.id.blockTitle);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMarginStart(dpToPx(25));

        List<TextView> locationTextViews = Arrays.asList(city, street, number, postalCode);
        for (TextView textView : locationTextViews) {
            EditText editText = new EditText(BlocksActivity.this);
            editText.setText(textView.getText());
            editText.setLayoutParams(params);
            editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            LinearLayout linearLayout = blockView.findViewById(R.id.itemBlockData);
            if (textView == postalCode) {
                editText.addTextChangedListener(new PostalCodeTextWatcher(editText));
                editText.setMaxLines(1);
                editText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
            }
            int index = linearLayout.indexOfChild(textView);
            linearLayout.removeView(textView);
            linearLayout.addView(editText, index);
        }
        LinearLayout clientLayout = blockView.findViewById(R.id.blockItemClient);
        clientLayout.removeView(clientName);
        int index = clientLayout.indexOfChild(clientAddress);
        clientLayout.removeView(clientAddress);
        Spinner spinner = new Spinner(BlocksActivity.this);
        spinner.setLayoutParams(params);
        createClientsSpinner(spinner);
        clientLayout.addView(spinner, index);

        CheckBox editCheckHouse = new CheckBox(BlocksActivity.this);
        editCheckHouse.setText("Dom jednorodzinny");
        editCheckHouse.setChecked(block.block.buildingType == 1);
        editCheckHouse.setLayoutParams(params);
        clientLayout.addView(editCheckHouse);

        editButton.setText("✅ Zapisz");
        deleteButton.setText("❌ Anuluj");

        deleteButton.setOnClickListener(v -> LiveDataUtil.observeOnce(blockViewModel.getBlocksWithFullData(catalogId), BlocksActivity.this, this::updateBlocks));

        editButton.setOnClickListener(v -> {
            ArrayList<String> list = new ArrayList<>();
            LinearLayout linearLayout = blockView.findViewById(R.id.itemBlockData);
            for (int i = 0; i < linearLayout.getChildCount(); i++) {
                View child = linearLayout.getChildAt(i);
                if (child instanceof EditText) {
                    EditText editText = (EditText) child;
                    String value = editText.getText().toString();
                    list.add(value);
                }
            }
            Client selectedClient = (Client) spinner.getSelectedItem();
            if (selectedClient.name.equals("Brak zleceniodawców")) {
                Toast.makeText(BlocksActivity.this, "Nie wybrano zleceniodawcy!", Toast.LENGTH_SHORT).show();
                return;

            }

            Block newBlock = new Block(catalogId, list.get(1), list.get(0), list.get(2), list.get(3), selectedClient.id, block.block.creation_date, new Date(), editCheckHouse.isChecked() ? 1 : 0);
            newBlock.id = block.block.id;
            blockViewModel.update(newBlock);
            catalogViewModel.updateEdition(catalogId);
        });

    }
}
