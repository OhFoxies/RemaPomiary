package com.rejner.remapomiary.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class BoardAdapter extends RecyclerView.Adapter<BoardAdapter.BoardViewHolder> {

    private List<Board> boards = new ArrayList<>();

    public void setBoards(List<Board> boards) {
        this.boards = boards;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BoardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.board_common_space, parent, false);
        return new BoardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BoardViewHolder holder, int position) {
        Board board = boards.get(position);
        holder.boardName.setText(board.name != null ? board.name : "Rozdzielnia " + (position + 1));

        // Zasilenie zagnieżdżonego RecyclerView listą obwodów z danego Boarda
        if (board.circuits != null) {
            holder.circuitAdapter.setCircuits(board.circuits);
        }
    }

    @Override
    public int getItemCount() {
        return boards != null ? boards.size() : 0;
    }

    class BoardViewHolder extends RecyclerView.ViewHolder {
        TextView boardName;
        Button deleteBoardButton, addCircuit, saveBoardNotes;
        RadioGroup radioButtonsInstallationType;
        EditText boardNotes;
        RecyclerView circuitsRecyclerView;
        CircuitAdapter circuitAdapter;

        public BoardViewHolder(@NonNull View itemView) {
            super(itemView);
            boardName = itemView.findViewById(R.id.boardName);
            deleteBoardButton = itemView.findViewById(R.id.deleteBoardButton);
            addCircuit = itemView.findViewById(R.id.addCircuit);
            saveBoardNotes = itemView.findViewById(R.id.saveBoardNotes);
            radioButtonsInstallationType = itemView.findViewById(R.id.radioButtons);
            boardNotes = itemView.findViewById(R.id.boardNotes);
            circuitsRecyclerView = itemView.findViewById(R.id.circuitsRecyclerView);

            setupNestedRecyclerView();
            setupButtons();
        }

        private void setupNestedRecyclerView() {
            circuitsRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            circuitAdapter = new CircuitAdapter();
            circuitsRecyclerView.setAdapter(circuitAdapter);
        }

        private void setupButtons() {
            deleteBoardButton.setOnClickListener(v -> {
                // TODO: Usunięcie rozdzielni z bazy
            });

            addCircuit.setOnClickListener(v -> {
                // TODO: Dodanie nowego obwodu do tej rozdzielni
            });

            saveBoardNotes.setOnClickListener(v -> {
                // TODO: Zapisanie uwag z boardNotes
            });

            radioButtonsInstallationType.setOnCheckedChangeListener((group, checkedId) -> {
                // TODO: Zmiana typu instalacji (TN-C / TN-S)
            });
        }
    }
}
