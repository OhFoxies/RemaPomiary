package com.rejner.remapomiary.adapters;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rejner.remapomiary.R;
import com.rejner.remapomiary.data.entities.BoardsFullData;
import com.rejner.remapomiary.data.entities.CircuitCommonSpace;
import com.rejner.remapomiary.ui.utils.Settings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        void onRefresh(BoardsFullData board);
    }

    private final OnBoardActionListener listener;
    private List<BoardsFullData> boards = new ArrayList<>();
    private final Set<Integer> expandedBoardIds = new HashSet<>();
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

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
        holder.bind(board);
    }

    private int dpToPx(View view, int dp) {
        float density = view.getResources().getDisplayMetrics().density;
        return (int) (dp * density);
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
        LinearLayout extraInfo;

        Button toggleCircuitsButton;
        ProgressBar progressBarCircuits;
        LinearLayout circuitsHeader;

        private boolean isLoadingChunks = false;
        private boolean isFullyLoaded = false;
        private int currentChunkIndex = 0;
        private static final int CHUNK_SIZE = 25;
        private final List<CircuitCommonSpace> loadedChunksList = new ArrayList<>();
        private List<CircuitCommonSpace> fullCircuitsList = null;
        private BoardsFullData currentBoard;
        private boolean isWLZ;
        private TextView statusHeader;
        Button refresh;

        public BoardViewHolder(@NonNull View itemView) {
            super(itemView);
            boardName = itemView.findViewById(R.id.boardName);
            deleteBoardButton = itemView.findViewById(R.id.deleteBoardButton);
            addCircuit = itemView.findViewById(R.id.addCircuit);
            saveBoardNotes = itemView.findViewById(R.id.saveBoardNotes);
            circuitsRecyclerView = itemView.findViewById(R.id.circuitsRecyclerView);
            radioButtonsInstallationType = itemView.findViewById(R.id.radioButtons);
            boardNotes = itemView.findViewById(R.id.boardNotes);
            extraInfo = itemView.findViewById(R.id.extraInfo);
            statusHeader = itemView.findViewById(R.id.statusHeader);
            refresh = itemView.findViewById(R.id.generateButton);
            toggleCircuitsButton = itemView.findViewById(R.id.toggleCircuitsButton);
            progressBarCircuits = itemView.findViewById(R.id.progressBarCircuits);
            circuitsHeader = itemView.findViewById(R.id.circuitsHeader);

            boardNotes.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.postDelayed(() -> {
                        ViewParent p = itemView.getParent();
                        if (p instanceof RecyclerView) {
                            RecyclerView mainRv = (RecyclerView) p;
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

            setupNestedRecyclerView();
            setupButtons();
        }

        void bind(BoardsFullData board) {
            this.currentBoard = board;
            this.fullCircuitsList = board.circuits;

            cancelChunkedLoading();

            boardName.setText(board.board.name);
            isWLZ = Settings.mainBoardName.equals(board.board.name);

            if (circuitAdapter != null) {
                circuitAdapter.setIsWLZ(isWLZ);
            }

            if (isWLZ) {
                deleteBoardButton.setVisibility(View.GONE);
                extraInfo.setVisibility(View.VISIBLE);
                toggleCircuitsButton.setVisibility(View.VISIBLE);
                statusHeader.setVisibility(View.VISIBLE);
                boolean isExpanded = expandedBoardIds.contains(board.board.id);

                updateExpansionUI(isExpanded);
                refresh.setOnClickListener(v -> {
                    listener.onRefresh(board);
                });
                toggleCircuitsButton.setOnClickListener(v -> {
                    if (isLoadingChunks) {
                        expandedBoardIds.remove(board.board.id);
                        cancelChunkedLoading();
                        updateExpansionUI(false);
                        circuitAdapter.setCircuits(null);
                        adjustRecyclerViewHeight(0);
                        return;
                    }

                    boolean isNowExpanded = !expandedBoardIds.contains(board.board.id);
                    if (isNowExpanded) {
                        expandedBoardIds.add(board.board.id);
                        updateExpansionUI(true);
                        startChunkedLoading();
                    } else {
                        expandedBoardIds.remove(board.board.id);
                        updateExpansionUI(false);
                        cancelChunkedLoading();
                        circuitAdapter.setCircuits(null);
                        adjustRecyclerViewHeight(0);
                        isFullyLoaded = false;
                    }
                });
            } else {
                extraInfo.setVisibility(View.GONE);
                toggleCircuitsButton.setVisibility(View.GONE);
                updateExpansionUI(true);
            }

            boardNotes.setText(board.board.notes);

            radioButtonsInstallationType.setOnCheckedChangeListener(null);
            if (Settings.installationTypeTNS.equals(board.board.type)) {
                radioButtonsInstallationType.check(R.id.radioTNS);
            } else {
                radioButtonsInstallationType.check(R.id.radioTNC);
            }

            radioButtonsInstallationType.setOnCheckedChangeListener((group, checkedId) -> {
                if (listener != null) {
                    listener.onInstallationTypeChanged(board, checkedId);
                }
            });

            if (expandedBoardIds.contains(board.board.id) || !isWLZ) {
                if (isWLZ) {
                    if (isFullyLoaded && !isLoadingChunks) {
                        circuitAdapter.setCircuits(fullCircuitsList);
                        adjustRecyclerViewHeight(fullCircuitsList != null ? fullCircuitsList.size() : 0);
                    } else if (!isLoadingChunks) {
                        startChunkedLoading();
                    }
                } else {
                    circuitAdapter.setCircuits(fullCircuitsList);
                    adjustRecyclerViewHeight(fullCircuitsList != null ? fullCircuitsList.size() : 0);
                }
            } else {
                circuitAdapter.setCircuits(null);
                adjustRecyclerViewHeight(0);
            }
        }

        private void startChunkedLoading() {
            cancelChunkedLoading();
            if (fullCircuitsList == null || fullCircuitsList.isEmpty()) {
                toggleCircuitsButton.setText("Ukryj");
                circuitAdapter.setCircuits(null);
                adjustRecyclerViewHeight(0);
                return;
            }

            isLoadingChunks = true;
            isFullyLoaded = false;
            currentChunkIndex = 0;
            loadedChunksList.clear();

            toggleCircuitsButton.setText("⏳ Anuluj...");
            if (progressBarCircuits != null) {
                progressBarCircuits.setVisibility(View.VISIBLE);
                progressBarCircuits.setMax(fullCircuitsList.size());
                progressBarCircuits.setProgress(0);
            }

            adjustRecyclerViewHeight(fullCircuitsList.size());
            loadNextChunk();
        }

        private void loadNextChunk() {
            if (!isLoadingChunks || fullCircuitsList == null) return;

            int start = currentChunkIndex * CHUNK_SIZE;
            if (start >= fullCircuitsList.size()) {
                finishChunkLoading();
                return;
            }

            int end = Math.min(start + CHUNK_SIZE, fullCircuitsList.size());

            backgroundExecutor.execute(() -> {
                if (!isLoadingChunks) return;
                List<CircuitCommonSpace> nextChunk = fullCircuitsList.subList(start, end);
                loadedChunksList.addAll(nextChunk);
                List<CircuitCommonSpace> newListToSubmit = new ArrayList<>(loadedChunksList);

                mainHandler.post(() -> {
                    if (!isLoadingChunks) return;
                    circuitAdapter.setCircuits(newListToSubmit);

                    currentChunkIndex++;
                    if (progressBarCircuits != null) {
                        progressBarCircuits.setProgress(Math.min(currentChunkIndex * CHUNK_SIZE, fullCircuitsList.size()));
                    }
                    loadNextChunk();
                });
            });
        }

        private void finishChunkLoading() {
            isLoadingChunks = false;
            isFullyLoaded = true;
            toggleCircuitsButton.setText("Ukryj");
            if (progressBarCircuits != null) {
                progressBarCircuits.setVisibility(View.GONE);
            }
        }

        private void cancelChunkedLoading() {
            isLoadingChunks = false;
            if (progressBarCircuits != null) {
                progressBarCircuits.setVisibility(View.GONE);
            }
        }

        private void updateExpansionUI(boolean isExpanded) {
            if (isExpanded) {
                circuitsRecyclerView.setVisibility(View.VISIBLE);
                addCircuit.setVisibility(View.VISIBLE);
                circuitsHeader.setVisibility(View.VISIBLE);
                if (!isLoadingChunks && isFullyLoaded) {
                    toggleCircuitsButton.setText("Ukryj");
                }
            } else {
                circuitsRecyclerView.setVisibility(View.GONE);
                addCircuit.setVisibility(View.GONE);
                circuitsHeader.setVisibility(View.GONE);
                toggleCircuitsButton.setText("Pokaż obwody");
            }
        }

        private void adjustRecyclerViewHeight(int itemCount) {
            int itemHeightDp = 80;
            int maxHeightDp = 300;
            int desiredHeight = itemCount * dpToPx(itemView, itemHeightDp);
            int maxHeight = dpToPx(itemView, maxHeightDp);

            ViewGroup.LayoutParams params = circuitsRecyclerView.getLayoutParams();
            params.height = Math.min(desiredHeight, maxHeight);
            circuitsRecyclerView.setLayoutParams(params);
        }

        private void setupNestedRecyclerView() {
            circuitsRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            circuitsRecyclerView.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                        if (v.canScrollVertically(1) || v.canScrollVertically(-1)) {
                            v.getParent().requestDisallowInterceptTouchEvent(true);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }
                return false;
            });
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
            }, isWLZ);
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

                    InputMethodManager imm = (InputMethodManager) itemView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(boardNotes.getWindowToken(), 0);
                    }
                }
            });
        }
    }
}