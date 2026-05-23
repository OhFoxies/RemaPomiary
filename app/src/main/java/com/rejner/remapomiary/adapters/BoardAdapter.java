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

import com.rejner.remapomiary.R;
import com.rejner.remapomiary.data.entities.BoardCommonSpace;
import com.rejner.remapomiary.data.entities.BoardsFullData;
import com.rejner.remapomiary.data.entities.CircuitCommonSpace;

import java.util.ArrayList;
import java.util.List;

public class BoardAdapter extends RecyclerView.Adapter<BoardAdapter.BoardViewHolder> {
    public interface OnBoardActionListener {
        void onDeleteBoard(BoardsFullData board);
        void onAddCircuit(BoardsFullData board);
        void onSaveNotes(BoardsFullData board, String notes);
        void onInstallationTypeChanged(BoardsFullData board, int checkedId);
        void onCircuitTypeChange(CircuitCommonSpace circuit, int checkedId);
        void onCircuitDelete(CircuitCommonSpace circuit);
        void onCircuitNameSave(CircuitCommonSpace circuit, String name);
        void onCircuitNameSpinner(CircuitCommonSpace circuit, String name);
    }
    private final OnBoardActionListener listener;
    private List<BoardsFullData> boards = new ArrayList<>();

    public BoardAdapter(OnBoardActionListener listener) {
        this.listener = listener;
    }
    public void setBoards(List<BoardsFullData> boards) {
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
        BoardsFullData board = boards.get(position);

        holder.boardName.setText(board.board.name);
        holder.boardNotes.setText(board.board.notes);

        // Usuń listener przed check()
        holder.radioButtonsInstallationType.setOnCheckedChangeListener(null);

        if ("TN-S".equals(board.board.type)) {
            holder.radioButtonsInstallationType.check(R.id.radioTNS);
        } else {
            holder.radioButtonsInstallationType.check(R.id.radioTNC);
        }

        // Przywróć listener
        holder.radioButtonsInstallationType.setOnCheckedChangeListener((group, checkedId) -> {
            int adapterPosition = holder.getBindingAdapterPosition();

            if (adapterPosition != RecyclerView.NO_POSITION && listener != null) {
                listener.onInstallationTypeChanged(
                        boards.get(adapterPosition),
                        checkedId
                );
            }
        });
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
            circuitAdapter = new CircuitAdapter(new CircuitAdapter.OnCircuitActionListener() {
                @Override
                public void onCircuitTypeChange_(CircuitCommonSpace circuit, int checkedId) {
                    listener.onCircuitTypeChange(circuit, checkedId);
                }

                @Override
                public void onCircuitDelete_(CircuitCommonSpace circuit) {
                    listener.onCircuitDelete(circuit);
                }

                @Override
                public void onCircuitNameSave_(CircuitCommonSpace circuit, String name) {
                    listener.onCircuitNameSave(circuit, name);

                }

                @Override
                public void onCircuitNameSpinner_(CircuitCommonSpace circuit, String name) {
                    listener.onCircuitNameSpinner(circuit, name);

                }
            });
            circuitsRecyclerView.setAdapter(circuitAdapter);
        }

        private void setupButtons() {
            deleteBoardButton.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onDeleteBoard(boards.get(position));
                }
            });

            addCircuit.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onAddCircuit(boards.get(position));
                }
            });

            saveBoardNotes.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    String notes = boardNotes.getText().toString();
                    listener.onSaveNotes(boards.get(position), notes);
                }
            });

            radioButtonsInstallationType.setOnCheckedChangeListener((group, checkedId) -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onInstallationTypeChanged(boards.get(position), checkedId);
                }
            });
        }
    }
}
