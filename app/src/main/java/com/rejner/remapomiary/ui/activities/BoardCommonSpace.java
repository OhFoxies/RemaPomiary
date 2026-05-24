package com.rejner.remapomiary.ui.activities;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rejner.remapomiary.R;
import com.rejner.remapomiary.adapters.BoardAdapter;
import com.rejner.remapomiary.data.entities.BoardsFullData;
import com.rejner.remapomiary.data.entities.CircuitCommonSpace;
import com.rejner.remapomiary.ui.viewmodels.BlockViewModel;
import com.rejner.remapomiary.ui.viewmodels.BoardCommonSpaceViewModel;
import com.rejner.remapomiary.ui.viewmodels.CircuitCommonSpaceViewModel;

import java.util.Date;

public class BoardCommonSpace extends AppCompatActivity {

    private RecyclerView boardsRecyclerView;
    private BoardAdapter boardAdapter;

    // Elementy UI
    private Button backButton, notesButton, roomsButton;
    private Spinner boardNameSpinner;

    private EditText boardNameInput;
    private int blockId;
    private BoardCommonSpaceViewModel boardCommonSpaceViewModel;
    private CircuitCommonSpaceViewModel circuitCommonSpaceViewModel;
    private int commonSpaceFlatId;
    private Button confirmBoardName, cancelBoardName, addNewBoard;
    private int catalogId;
    private String blockName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board_common_space);

        blockId = getIntent().getIntExtra("blockId", 0);
        commonSpaceFlatId = getIntent().getIntExtra("flatId", 0);


        BlockViewModel blockViewModel = new ViewModelProvider(this).get(BlockViewModel.class);
        circuitCommonSpaceViewModel = new ViewModelProvider(this).get(CircuitCommonSpaceViewModel.class);
        boardCommonSpaceViewModel = new ViewModelProvider(this).get(BoardCommonSpaceViewModel.class);
        catalogId = -1;
        blockViewModel.getBlockById(blockId, block1 -> {
                blockName = block1.block.city + "/" + block1.block.number;
                catalogId = block1.catalog.id;
        });

        initViews();
        setupRecyclerView();
        setupNavigationButtons();
        setupBoardSpinnerLogic();

        observeDatabase();
    }

    private void initViews() {
        boardsRecyclerView = findViewById(R.id.boardsRecyclerView);
        backButton = findViewById(R.id.backButton);
        notesButton = findViewById(R.id.notesButton);
        roomsButton = findViewById(R.id.roomsButton);
        TextView boardTitle = findViewById(R.id.boardTitle);

        boardNameSpinner = findViewById(R.id.boardNameSpinner);
        boardNameInput = findViewById(R.id.boardNameInput);
        confirmBoardName = findViewById(R.id.confirmBoardName);
        cancelBoardName = findViewById(R.id.cancelBoardName);
        addNewBoard = findViewById(R.id.addNewBoard);

        boardTitle.setText("Rozdzielnie - " + blockName);
    }

    private void setupRecyclerView() {
        boardsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        boardAdapter = new BoardAdapter(new BoardAdapter.OnBoardActionListener() {
            @Override
            public void onDeleteBoard(BoardsFullData board) {

                new AlertDialog.Builder(BoardCommonSpace.this)
                        .setTitle("Potwierdzenie")
                        .setMessage("Czy na pewno chcesz usunąć rozdzielnię " + board.board.name + "?")
                        .setPositiveButton("Usuń", (dialog, which) -> {
                            boardCommonSpaceViewModel.delete(board.board);

                            Toast.makeText(
                                    BoardCommonSpace.this,
                                    "Usunięto rozdzielnie " + board.board.name,
                                    Toast.LENGTH_SHORT
                            ).show();
                        })
                        .setNegativeButton("Anuluj", null)
                        .show();
            }

            @Override
            public void onAddCircuit(BoardsFullData board) {
                CircuitCommonSpace circuitCommonSpace = new CircuitCommonSpace();
                circuitCommonSpace.boardId = board.board.id;
                circuitCommonSpace.type = "1f";
                circuitCommonSpace.name = "Obwód";
                circuitCommonSpaceViewModel.insert(circuitCommonSpace);

            }

            @Override
            public void onSaveNotes(BoardsFullData board, String notes) {
                board.board.notes = notes;
                boardCommonSpaceViewModel.update(board.board);
                hideKeyboard();
            }

            @Override
            public void onInstallationTypeChanged(BoardsFullData board, int checkedId) {
                if (checkedId == R.id.radioTNS) {
                    board.board.type = "TN-S";
                }
                if (checkedId == R.id.radioTNC) {
                    board.board.type = "TN-C";

                }
                boardCommonSpaceViewModel.update(board.board);
            }

            @Override
            public void onCircuitTypeChange(CircuitCommonSpace circuit, int checkedId) {
                if (checkedId == R.id.radio1f) {
                    circuit.type = "1f";
                } else {
                    circuit.type = "3f";
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

                            Toast.makeText(
                                    BoardCommonSpace.this,
                                    "Usunięto obwód " + circuit.name,
                                    Toast.LENGTH_SHORT
                            ).show();
                        })
                        .setNegativeButton("Anuluj", null)
                        .show();
            }

            @Override
            public void onCircuitNameSave(CircuitCommonSpace circuit, String name) {
                if (name.isEmpty()) {
                    Toast.makeText(BoardCommonSpace.this,
                            "Nazwa nie może być pusta!",
                            Toast.LENGTH_SHORT
                            ).show();

                } else {
                    circuit.name = name;
                    circuitCommonSpaceViewModel.update(circuit);
                }
            }

            @Override
            public void onCircuitNameSpinner(CircuitCommonSpace circuit, String name) {
                circuit.name = name;
                circuitCommonSpaceViewModel.update(circuit);
            }
        });
        boardsRecyclerView.setAdapter(boardAdapter);
    }

    private void setupNavigationButtons() {
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, BlockActivity.class);
            intent.putExtra("blockId", blockId);
            startActivity(intent);
        });
        notesButton.setOnClickListener(v -> {

            Intent intent = new Intent(this, NotesActivity.class);
            if (catalogId != -1) {
                intent.putExtra("catalogId", catalogId);

            }
            intent.putExtra("isCommonSpace", 1);
            intent.putExtra("flatId", commonSpaceFlatId);
            startActivity(intent);
        });

//        RCDButton.setOnClickListener(v -> { /* TODO: Przejdź do różnicówki */ });

        roomsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, RoomActivity.class);
            if (catalogId != -1) {
                intent.putExtra("catalogId", catalogId);

            }
            intent.putExtra("isCommonSpace", 1);
            intent.putExtra("flatId", commonSpaceFlatId);
            startActivity(intent);
        });
    }

    private void setupBoardSpinnerLogic() {
        String[] options = new String[]{"Rozdzielnia Główna", "Rozdzielnia Piętro", "Rozdzielnia Garaż", "Rozdzielnia -", "inne"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        boardNameSpinner.setAdapter(adapter);

        boardNameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                if ("inne".equalsIgnoreCase(selected)) {
                    boardNameInput.setVisibility(View.VISIBLE);
                    confirmBoardName.setVisibility(View.VISIBLE);
                    cancelBoardName.setVisibility(View.VISIBLE);

                    // Focus
                    boardNameInput.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.showSoftInput(boardNameInput, InputMethodManager.SHOW_IMPLICIT);
                } else if ("Rozdzielnia -".equalsIgnoreCase(selected)) {
                    boardNameInput.setVisibility(View.VISIBLE);
                    confirmBoardName.setVisibility(View.VISIBLE);
                    cancelBoardName.setVisibility(View.VISIBLE);

                    boardNameInput.setText("Rozdzielnia ");
                    // Focus
                    boardNameInput.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.showSoftInput(boardNameInput, InputMethodManager.SHOW_IMPLICIT);
                }
                else {
                    boardNameInput.setVisibility(View.GONE);
                    confirmBoardName.setVisibility(View.GONE);
                    cancelBoardName.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        confirmBoardName.setOnClickListener(v -> {
            if (!boardNameInput.getText().toString().isEmpty()) {
                hideKeyboard();
            } else {
                Toast.makeText(this, "Podaj nazwę rozdzielni", Toast.LENGTH_SHORT).show();
            }

        });
        cancelBoardName.setOnClickListener(v -> {
            boardNameInput.setText("");
            boardNameSpinner.setSelection(0);
            boardNameInput.setVisibility(View.GONE);
            confirmBoardName.setVisibility(View.GONE);
            cancelBoardName.setVisibility(View.GONE);
            hideKeyboard();
        });
        addNewBoard.setOnClickListener(v -> {
            String boardName;
            if ("inne".equalsIgnoreCase(boardNameSpinner.getSelectedItem().toString())) {
                if (!boardNameInput.getText().toString().isEmpty()) {
                    boardName = boardNameInput.getText().toString();
                } else {
                    Toast.makeText(this, "Podaj nazwę rozdzielni", Toast.LENGTH_SHORT).show();

                    return;
                }
            } else {
                boardName = boardNameSpinner.getSelectedItem().toString();
            }
            com.rejner.remapomiary.data.entities.BoardCommonSpace boardCommonSpace = new com.rejner.remapomiary.data.entities.BoardCommonSpace();
            boardCommonSpace.name = boardName;
            boardCommonSpace.flatId = commonSpaceFlatId;
            boardCommonSpace.creation_date = new Date();
            boardCommonSpaceViewModel.insert(boardCommonSpace);
            Toast.makeText(this, "Dodano rozdzielnie", Toast.LENGTH_SHORT).show();


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
    private void observeDatabase() {
         boardCommonSpaceViewModel.getBoardsFullData(commonSpaceFlatId).observe(this, boardsList -> boardAdapter.setBoards(boardsList));
    }
}