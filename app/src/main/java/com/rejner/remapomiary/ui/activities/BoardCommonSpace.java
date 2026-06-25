package com.rejner.remapomiary.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.rejner.remapomiary.R;
import com.rejner.remapomiary.adapters.BoardAdapter;
import com.rejner.remapomiary.data.db.AppDatabase;
import com.rejner.remapomiary.data.entities.Block;
import com.rejner.remapomiary.data.entities.BoardsFullData;
import com.rejner.remapomiary.data.entities.CircuitCommonSpace;
import com.rejner.remapomiary.data.entities.FlatFullData;
import com.rejner.remapomiary.ui.utils.Settings;
import com.rejner.remapomiary.ui.viewmodels.BlockViewModel;
import com.rejner.remapomiary.ui.viewmodels.BoardCommonSpaceViewModel;
import com.rejner.remapomiary.ui.viewmodels.CircuitCommonSpaceViewModel;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BoardCommonSpace extends AppCompatActivity {

    private RecyclerView boardsRecyclerView;
    private BoardAdapter boardAdapter;
    private Button backButton;
    private FloatingActionButton scrollToTopButton;

    private Spinner boardNameSpinner;
    private EditText boardNameInput;
    private Button addNewBoard;
    private Button notesButton, boardButton, roomsButton;

    private int blockId;
    private int commonSpaceFlatId;
    private String blockName;
    private Block block;
    private String lastAddedBoardName = null;

    private BoardCommonSpaceViewModel boardCommonSpaceViewModel;
    private CircuitCommonSpaceViewModel circuitCommonSpaceViewModel;

    private ActivityResultLauncher<Uri> takeBoardPictureLauncher;
    private BoardsFullData boardPendingPhoto = null;
    private File tempBoardPhotoFile = null;

    // Zoptymalizowano: Pula wątków do asynchronicznego sortowania i operacji bazodanowych (Zapobieganie ANR)
    private final ExecutorService backgroundSortExecutor = Executors.newSingleThreadExecutor();

    private final String[] commonSpaceCircuits = {"Oświetlenie", "Oświetlenie -", "Gniazda 230V", "Gniazda -", "Winda", "Wentylacja", "inne"};
    private final String[] houseCircuits = {"Oświetlenie", "Oświetlenie -", "Gniazda 230V", "Gniazda -", "Piekarnik", "Indukcja", "Pralka", "Zmywarka", "inne"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board_common_space);

        blockId = getIntent().getIntExtra("blockId", 0);
        commonSpaceFlatId = getIntent().getIntExtra("flatId", 0);

        BlockViewModel blockViewModel = new ViewModelProvider(this).get(BlockViewModel.class);
        circuitCommonSpaceViewModel = new ViewModelProvider(this).get(CircuitCommonSpaceViewModel.class);
        boardCommonSpaceViewModel = new ViewModelProvider(this).get(BoardCommonSpaceViewModel.class);

        blockViewModel.getBlockById(blockId, block1 -> {
            if (block1 == null || block1.block == null) return;
            block = block1.block;
            blockName = block1.block.city + " / " + block1.block.number;
            runOnUiThread(() -> {
                TextView boardTitle = findViewById(R.id.boardTitle);
                String buildingLabel = block.buildingType == 1 ? "Dom" : "Blok";
                if (boardTitle != null) boardTitle.setText(buildingLabel + " " + block.street + " " + block.number + " - rozdzielnie");
                setupRecyclerView();
            });
        });

        initViews();
        setupNavigationButtons();
        setupAddBoardUi();
        observeDatabase();
        blockViewModel.getBlockById(blockId, b -> {
            if (b.block.buildingType == 0) {
                ensureMainBoardExists();
            }
        });

        takeBoardPictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && boardPendingPhoto != null && tempBoardPhotoFile != null) {
                        com.rejner.remapomiary.data.entities.BoardCommonSpace updatedBoard = new com.rejner.remapomiary.data.entities.BoardCommonSpace();
                        updatedBoard.id = boardPendingPhoto.board.id;
                        updatedBoard.flatId = boardPendingPhoto.board.flatId;
                        updatedBoard.name = boardPendingPhoto.board.name;
                        updatedBoard.notes = boardPendingPhoto.board.notes;
                        updatedBoard.type = boardPendingPhoto.board.type;
                        updatedBoard.creation_date = boardPendingPhoto.board.creation_date;

                        String currentPaths = boardPendingPhoto.board.photoPaths;
                        String newPath = tempBoardPhotoFile.getAbsolutePath();

                        if (currentPaths == null || currentPaths.trim().isEmpty()) {
                            updatedBoard.photoPaths = newPath;
                        } else {
                            updatedBoard.photoPaths = currentPaths + "," + newPath;
                        }

                        boardCommonSpaceViewModel.update(updatedBoard);
                        Toast.makeText(this, "Dodano zdjęcie rozdzielni", Toast.LENGTH_SHORT).show();
                    }
                    boardPendingPhoto = null;
                    tempBoardPhotoFile = null;
                }
        );
    }

    private void ensureMainBoardExists() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(this);
            String boardName = Settings.mainBoardName;
            if (db.boardCommonSpaceDao().getBoardByNameSync(commonSpaceFlatId, boardName) == null) {
                com.rejner.remapomiary.data.entities.BoardCommonSpace board = new com.rejner.remapomiary.data.entities.BoardCommonSpace();
                board.flatId = commonSpaceFlatId;
                board.name = boardName;
                board.creation_date = new Date();
                board.type = Settings.installationTypeTNS;
                db.boardCommonSpaceDao().insert(board);
            }
        });
    }

    private void initViews() {
        boardsRecyclerView = findViewById(R.id.boardsRecyclerView);
        backButton = findViewById(R.id.backButton);
        scrollToTopButton = findViewById(R.id.scrollToTopButton);

        boardNameSpinner = findViewById(R.id.boardNameSpinner);
        boardNameInput = findViewById(R.id.boardNameInput);
        addNewBoard = findViewById(R.id.addNewBoard);

        notesButton = findViewById(R.id.notesButton);
        boardButton = findViewById(R.id.boardButton);
        roomsButton = findViewById(R.id.roomsButton);
    }

    private void setupRecyclerView() {
        boardsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        String[] circuitItems = (block != null && block.buildingType == 1) ? houseCircuits : commonSpaceCircuits;
        boardAdapter = new BoardAdapter(new BoardAdapter.OnBoardActionListener() {
            @Override
            public void onDeleteBoard(BoardsFullData board) {
                new AlertDialog.Builder(BoardCommonSpace.this)
                        .setTitle("Potwierdzenie")
                        .setMessage("Czy na pewno chcesz usunąć rozdzielnię " + board.board.name + "?")
                        .setPositiveButton("Usuń", (dialog, which) -> {
                            boardCommonSpaceViewModel.delete(board.board);
                            Toast.makeText(BoardCommonSpace.this, "Usunięto rozdzielnie " + board.board.name, Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Anuluj", null)
                        .show();
            }

            @Override
            public void onAddCircuit(BoardsFullData board) {
                CircuitCommonSpace circuitCommonSpace = new CircuitCommonSpace();
                circuitCommonSpace.boardId = board.board.id;
                circuitCommonSpace.type = Settings.installation1f;
                circuitCommonSpace.name = "Oświetlenie";
                circuitCommonSpaceViewModel.insert(circuitCommonSpace);
            }

            @Override
            public void onSaveNotes(BoardsFullData board, String notes) {
                board.board.notes = notes;
                Toast.makeText(BoardCommonSpace.this, "Zapisano uwagi do rozdzielni", Toast.LENGTH_SHORT).show();
                boardCommonSpaceViewModel.update(board.board);
            }

            @Override
            public void onInstallationTypeChanged(BoardsFullData board, int checkedId) {
                if (checkedId == R.id.radioTNS) {
                    board.board.type = Settings.installationTypeTNS;
                } else if (checkedId == R.id.radioTNC) {
                    board.board.type = Settings.installationTypeTNC;
                }
                boardCommonSpaceViewModel.update(board.board);
            }

            @Override
            public void onCircuitTypeChange(CircuitCommonSpace circuit, int checkedId) {
                if (checkedId == R.id.radio1f) {
                    circuit.type = Settings.installation1f;
                } else {
                    circuit.type = Settings.installation3f;
                }
                circuitCommonSpaceViewModel.update(circuit);
            }

            @Override
            public void onCircuitDelete(CircuitCommonSpace circuit) {
                new AlertDialog.Builder(BoardCommonSpace.this)
                        .setTitle("Potwierdzenie")
                        .setMessage("Czy na pewno chcesz usunąć obwód " + circuit.name + "?")
                        .setPositiveButton("Usuń", (dialog, which) -> {
                            circuitCommonSpaceViewModel.delete(circuit);
                            Toast.makeText(BoardCommonSpace.this, "Usunięto obwód " + circuit.name, Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Anuluj", null)
                        .show();
            }

            @Override
            public void onCircuitNameSave(CircuitCommonSpace circuit, String name) {
                circuit.name = name;
                circuitCommonSpaceViewModel.update(circuit);
            }

            @Override
            public void onCircuitNameSpinner(CircuitCommonSpace circuit, String name) {
                circuit.name = name;
                circuitCommonSpaceViewModel.update(circuit);
            }

            @Override
            public void onAddBoardPhoto(BoardsFullData board) {
                boardPendingPhoto = board;
                try {
                    String fileName = "ROZDZIELNIA_" + board.board.id + "_" + System.currentTimeMillis();
                    File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                    tempBoardPhotoFile = File.createTempFile(fileName, ".jpg", storageDir);

                    Uri photoURI = FileProvider.getUriForFile(
                            BoardCommonSpace.this,
                            getPackageName() + ".fileprovider",
                            tempBoardPhotoFile
                    );
                    takeBoardPictureLauncher.launch(photoURI);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(BoardCommonSpace.this, "Błąd uruchamiania aparatu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onUpdateBoard(com.rejner.remapomiary.data.entities.BoardCommonSpace board) {
                boardCommonSpaceViewModel.update(board);
            }

            @Override
            public void onRefresh(BoardsFullData boardParam) {
                if (block != null && block.buildingType == 1) {
                    Toast.makeText(BoardCommonSpace.this, "Opcja niedostępna dla domów jednorodzinnych", Toast.LENGTH_SHORT).show();
                    return;
                }
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    AppDatabase db = AppDatabase.getDatabase(BoardCommonSpace.this);
                    db.runInTransaction(() -> {
                        List<FlatFullData> flats = db.flatDao().getFlatsSync(blockId);

                        String boardName = Settings.mainBoardName;
                        com.rejner.remapomiary.data.entities.BoardCommonSpace boardEntity = db.boardCommonSpaceDao().getBoardByNameSync(commonSpaceFlatId, boardName);

                        if (boardEntity == null) {
                            boardEntity = new com.rejner.remapomiary.data.entities.BoardCommonSpace();
                            boardEntity.flatId = commonSpaceFlatId;
                            boardEntity.name = boardName;
                            boardEntity.creation_date = new Date();
                            boardEntity.type = Settings.installationTypeTNS;
                            long boardId = db.boardCommonSpaceDao().insert(boardEntity);
                            boardEntity.id = (int) boardId;
                        }

                        for (FlatFullData flatFullData : flats) {
                            if (flatFullData.flat.isCommonSpace == 1) continue;

                            String flatNum = flatFullData.flat.number != null ? flatFullData.flat.number.trim() : "";
                            String circuitName = "Lokal - " + flatNum;
                            CircuitCommonSpace ccsGet = db.circuitCommonSpaceDao().getCircuitByNameSync(boardEntity.id, circuitName);
                            boolean is3f = db.circuitDao().isFlat3fSync(flatFullData.flat.id);

                            if (Settings.measurementDone.equals(flatFullData.flat.status)) {
                                if (ccsGet == null) {
                                    CircuitCommonSpace ccs = new CircuitCommonSpace();
                                    ccs.boardId = boardEntity.id;
                                    ccs.name = circuitName;
                                    ccs.notes = Settings.flatGotAccess;
                                    ccs.type = is3f ? Settings.installation3f : Settings.installation1f;
                                    db.circuitCommonSpaceDao().insert(ccs);
                                } else {
                                    ccsGet.notes = Settings.flatGotAccess;
                                    ccsGet.type = is3f ? Settings.installation3f : Settings.installation1f;
                                    db.circuitCommonSpaceDao().update(ccsGet);
                                }
                            } else {
                                if (ccsGet != null) {
                                    ccsGet.notes = Settings.flatNoAccess;
                                    ccsGet.type = is3f ? Settings.installation3f : Settings.installation1f;
                                    db.circuitCommonSpaceDao().update(ccsGet);
                                } else {
                                    CircuitCommonSpace ccs = new CircuitCommonSpace();
                                    ccs.boardId = boardEntity.id;
                                    ccs.name = circuitName;
                                    ccs.notes = Settings.flatNoAccess;
                                    ccs.type = is3f ? Settings.installation3f : Settings.installation1f;
                                    db.circuitCommonSpaceDao().insert(ccs);
                                }
                            }
                        }
                    });
                    runOnUiThread(() -> Toast.makeText(BoardCommonSpace.this, "Zsynchronizowano obwody WLZ", Toast.LENGTH_SHORT).show());
                });
            }
        }, circuitItems);

        boardsRecyclerView.setAdapter(boardAdapter);

        boardsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (recyclerView.computeVerticalScrollOffset() > 150) {
                    scrollToTopButton.show();
                } else {
                    scrollToTopButton.hide();
                }
            }
        });

        scrollToTopButton.setOnClickListener(v -> {
            boardsRecyclerView.smoothScrollToPosition(0);
            com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
            if (appBarLayout != null) {
                appBarLayout.setExpanded(true, true);
            }
        });
    }

    private void setupNavigationButtons() {
        backButton.setOnClickListener(v -> {
            if (block != null && block.buildingType == 1) {
                Intent intent = new Intent(this, BlocksActivity.class);
                intent.putExtra("catalogId", block.catalogId);
                startActivity(intent);
            } else {
                Intent intent = new Intent(this, BlockActivity.class);
                intent.putExtra("blockId", blockId);
                startActivity(intent);
            }
        });

        notesButton.setOnClickListener(v -> {
            Intent intent = new Intent(BoardCommonSpace.this, NotesActivity.class);
            intent.putExtra("commonSpace", 1);
            intent.putExtra("flatId", commonSpaceFlatId);
            intent.putExtra("name", blockName);
            startActivity(intent);
        });

        roomsButton.setOnClickListener(v -> {
            Intent intent = new Intent(BoardCommonSpace.this, RoomActivity.class);
            intent.putExtra("commonSpace", 1);
            intent.putExtra("flatId", commonSpaceFlatId);
            intent.putExtra("name", blockName);
            startActivity(intent);
        });
    }

    private void setupAddBoardUi() {
        String[] options = new String[]{"Rozdzielnia Główna", "Rozdzielnia Piętro", "Rozdzielnia Garaż", "Rozdzielnia -", "inne"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        boardNameSpinner.setAdapter(adapter);

        boardNameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                if ("inne".equalsIgnoreCase(selected) || "Rozdzielnia -".equalsIgnoreCase(selected)) {
                    boardNameInput.setVisibility(View.VISIBLE);
                    if ("Rozdzielnia -".equalsIgnoreCase(selected)) {
                        boardNameInput.setText("Rozdzielnia ");
                    } else {
                        boardNameInput.setText("");
                    }
                    boardNameInput.postDelayed(() -> {
                        if (boardNameInput.requestFocus()) {
                            boardNameInput.setSelection(boardNameInput.getText().length());
                            showKeyboard(boardNameInput);
                        }
                    }, 150);
                } else {
                    hideKeyboard();
                    boardNameInput.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        addNewBoard.setOnClickListener(v -> {
            String boardName;
            String selectedSpinnerItem = boardNameSpinner.getSelectedItem().toString();

            if ("inne".equalsIgnoreCase(selectedSpinnerItem) || "Rozdzielnia -".equalsIgnoreCase(selectedSpinnerItem)) {
                boardName = boardNameInput.getText().toString().trim();
                if (boardName.isEmpty()) {
                    Toast.makeText(this, "Podaj nazwę rozdzielni", Toast.LENGTH_SHORT).show();
                    boardNameInput.setError("Podaj nazwę!");
                    return;
                }
            } else {
                boardName = selectedSpinnerItem;
            }

            if (boardName.equals(Settings.mainBoardName)) {
                Toast.makeText(BoardCommonSpace.this, "Nie możesz użyć tej nazwy!", Toast.LENGTH_SHORT).show();
                return;
            }

            // NAPRAWA: Chowamy klawiaturę zanim widok boardNameInput zostanie ukryty (GONE)
            hideKeyboard();

            lastAddedBoardName = boardName;
            com.rejner.remapomiary.data.entities.BoardCommonSpace boardCommonSpace = new com.rejner.remapomiary.data.entities.BoardCommonSpace();
            boardCommonSpace.name = boardName;
            boardCommonSpace.flatId = commonSpaceFlatId;
            boardCommonSpace.creation_date = new Date();
            boardCommonSpaceViewModel.insert(boardCommonSpace);

            Toast.makeText(BoardCommonSpace.this, "Dodano rozdzielnie", Toast.LENGTH_SHORT).show();

            boardNameInput.setText("");
            boardNameInput.setVisibility(View.GONE);
            boardNameSpinner.setSelection(0);
        });
    }

    private void showKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, 0);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View focused = getCurrentFocus();
        if (imm != null && focused != null) {
            imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }
    }

    // Zoptymalizowano: Przeniesiono całe wielopoziomowe sortowanie list do wątku tła (Wyciszenie mikroprzycięć)
    private void observeDatabase() {
        boardCommonSpaceViewModel.getBoardsFullData(commonSpaceFlatId).observe(this, boardsList -> {
            if (boardsList == null) {
                boardAdapter.setBoards(null);
                return;
            }

            backgroundSortExecutor.execute(() -> {
                Collections.sort(boardsList, (b1, b2) -> {
                    String name1 = b1.board != null && b1.board.name != null ? b1.board.name : "";
                    String name2 = b2.board != null && b2.board.name != null ? b2.board.name : "";
                    if (Settings.mainBoardName.equals(name1)) return -1;
                    if (Settings.mainBoardName.equals(name2)) return 1;
                    return name1.compareToIgnoreCase(name2);
                });

                for (BoardsFullData boardData : boardsList) {
                    if (boardData.circuits != null) {
                        boolean isMain = boardData.board != null && Settings.mainBoardName.equals(boardData.board.name);
                        Collections.sort(boardData.circuits, (c1, c2) -> {
                            if (isMain) {
                                String n1 = c1.name != null ? c1.name : "";
                                String n2 = c2.name != null ? c2.name : "";

                                String d1 = n1.replaceAll("\\D+", "");
                                String d2 = n2.replaceAll("\\D+", "");

                                if (!d1.isEmpty() && !d2.isEmpty()) {
                                    try {
                                        int i1 = Integer.parseInt(d1);
                                        int i2 = Integer.parseInt(d2);
                                        if (i1 != i2) return Integer.compare(i1, i2);
                                    } catch (NumberFormatException ignored) {}
                                }
                                return n1.compareToIgnoreCase(n2);
                            } else {
                                return Integer.compare(c1.id, c2.id);
                            }
                        });
                    }
                }

                runOnUiThread(() -> {
                    boardAdapter.setBoards(boardsList);

                    if (lastAddedBoardName != null) {
                        for (int i = 0; i < boardsList.size(); i++) {
                            if (boardsList.get(i).board != null && lastAddedBoardName.equals(boardsList.get(i).board.name)) {
                                int finalPosition = i;
                                boardsRecyclerView.postDelayed(() -> {
                                    if (boardsRecyclerView != null) {
                                        boardsRecyclerView.smoothScrollToPosition(finalPosition);
                                    }
                                }, 300);
                                break;
                            }
                        }
                        lastAddedBoardName = null;
                    }
                });
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        backgroundSortExecutor.shutdown(); // Bezpieczne czyszczenie zasobów wątków
    }
}