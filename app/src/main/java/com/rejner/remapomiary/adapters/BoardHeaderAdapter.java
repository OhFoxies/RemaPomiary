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
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rejner.remapomiary.R;

public class BoardHeaderAdapter extends RecyclerView.Adapter<BoardHeaderAdapter.HeaderViewHolder> {

    private final HeaderListener listener;

    public interface HeaderListener {
        void onAddNewBoard(String boardName);
        void onNotesButtonClicked();
        void onRoomsButtonClicked();
    }

    public BoardHeaderAdapter(HeaderListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public HeaderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.headers_commonspace_board, parent, false);
        return new HeaderViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull HeaderViewHolder holder, int position) {
        // Logika w konstruktorze ViewHoldera
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        Spinner boardNameSpinner;
        EditText boardNameInput;
        Button cancelBoardName, addNewBoard;
        Button notesButton, boardButton, roomsButton;

        public HeaderViewHolder(@NonNull View itemView, HeaderListener listener) {
            super(itemView);

            boardNameSpinner = itemView.findViewById(R.id.boardNameSpinner);
            boardNameInput = itemView.findViewById(R.id.boardNameInput);
            cancelBoardName = itemView.findViewById(R.id.cancelBoardName);
            addNewBoard = itemView.findViewById(R.id.addNewBoard);

            notesButton = itemView.findViewById(R.id.notesButton);
            boardButton = itemView.findViewById(R.id.boardButton);
            roomsButton = itemView.findViewById(R.id.roomsButton);

            setupSpinner();
            setupButtons(listener);
        }

        private void setupSpinner() {
            String[] options = new String[]{"Rozdzielnia Główna", "Rozdzielnia Piętro", "Rozdzielnia Garaż", "Rozdzielnia -", "inne"};

            ArrayAdapter<String> adapter = new ArrayAdapter<>(itemView.getContext(), android.R.layout.simple_spinner_item, options);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            boardNameSpinner.setAdapter(adapter);

            boardNameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String selected = parent.getItemAtPosition(position).toString();
                    if ("inne".equalsIgnoreCase(selected) || "Rozdzielnia -".equalsIgnoreCase(selected)) {

                        // 1. Najpierw pokazujemy widoki
                        boardNameInput.setVisibility(View.VISIBLE);
                        cancelBoardName.setVisibility(View.VISIBLE);

                        // 2. Ustawiamy bazowy tekst
                        if ("Rozdzielnia -".equalsIgnoreCase(selected)) {
                            boardNameInput.setText("Rozdzielnia ");
                        } else {
                            boardNameInput.setText("");
                        }

                        // 3. Całą resztę wrzucamy do kolejki wątku głównego (post)
                        // Pozwoli to systemowi na ułożenie widoku w pamięci przed modyfikacją kursora i klawiatury
                        boardNameInput.postDelayed(() -> {
                            if (boardNameInput.requestFocus()) {
                                // Kursor ląduje na samym końcu aktualnego tekstu
                                boardNameInput.setSelection(boardNameInput.getText().length());
                                // Podnosimy klawiaturę
                                showKeyboard(boardNameInput);
                            }
                        }, 150);
                    } else {
                        hideFormControls();
                        hideKeyboard(boardNameInput);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        private void setupButtons(HeaderListener listener) {


            cancelBoardName.setOnClickListener(v -> {
                hideFormControls();
                boardNameSpinner.setSelection(0);
                hideKeyboard(boardNameInput);
            });

            addNewBoard.setOnClickListener(v -> {
                String boardName;
                String selectedSpinnerItem = boardNameSpinner.getSelectedItem().toString();

                if ("inne".equalsIgnoreCase(selectedSpinnerItem) || "Rozdzielnia -".equalsIgnoreCase(selectedSpinnerItem)) {
                    if (!boardNameInput.getText().toString().isEmpty()) {
                        boardName = boardNameInput.getText().toString();
                        hideFormControls();
                        boardNameSpinner.setSelection(0);
                        hideKeyboard(boardNameInput);
                    } else {
                        Toast.makeText(itemView.getContext(), "Podaj nazwę rozdzielni", Toast.LENGTH_SHORT).show();
                        boardNameInput.setError("Podaj nazwę!");
                        return;
                    }
                } else {
                    boardName = selectedSpinnerItem;
                }

                listener.onAddNewBoard(boardName);
            });

            notesButton.setOnClickListener(v -> listener.onNotesButtonClicked());
            roomsButton.setOnClickListener(v -> listener.onRoomsButtonClicked());
        }

        private void hideFormControls() {
            boardNameInput.setText("");
            boardNameInput.setVisibility(View.GONE);
            cancelBoardName.setVisibility(View.GONE);
        }

        private void showKeyboard(View view) {
            InputMethodManager imm = (InputMethodManager) itemView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                // Flaga 0 jest bardziej niezawodna przy bezpośrednim wywołaniu na zafokusowanym widoku
                imm.showSoftInput(view, 0);
            }
        }

        private void hideKeyboard(View view) {
            InputMethodManager imm = (InputMethodManager) itemView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}