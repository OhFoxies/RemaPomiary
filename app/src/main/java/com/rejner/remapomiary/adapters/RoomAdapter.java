// RoomAdapter.java
package com.rejner.remapomiary.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.rejner.remapomiary.data.entities.OutletMeasurement;
import com.rejner.remapomiary.data.entities.RoomInFlat;
import com.rejner.remapomiary.databinding.RoomCardItemBinding;
import com.rejner.remapomiary.ui.activities.RoomActivity;
import com.rejner.remapomiary.ui.utils.Settings;
import com.rejner.remapomiary.ui.viewmodels.OutletMeasurementViewModel;
import com.rejner.remapomiary.ui.viewmodels.RoomViewModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class RoomAdapter extends ListAdapter<RoomInFlat, RoomAdapter.RoomViewHolder> {
    private final RoomViewModel roomViewModel;
    private final OutletMeasurementViewModel outletViewModel;
    private final LifecycleOwner lifecycleOwner;
    private final Context context;
    private final String[] applianceOptions, breakerTypes, noteOptions, ampsOptions;
    private final Consumer<RoomInFlat> deleteListener;
    private final Consumer<Integer> addMeasurementListener;
    private final MeasurementAdapter.OnMeasurementActionListener photoListener;
    private long newlyAddedMeasurementId = -1;
    private final int catalogId;
    private final boolean isCommonSpace;
    private final Set<Integer> expandedRoomIds = new HashSet<>();
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private RecyclerView parentRecyclerView;

    public RoomAdapter(RoomViewModel roomViewModel, OutletMeasurementViewModel outletViewModel,
                       LifecycleOwner lifecycleOwner, Context context,
                       String[] applianceOptions, String[] breakerTypes, String[] noteOptions, String[] ampsOptions,
                       Consumer<RoomInFlat> deleteListener, Consumer<Integer> addMeasurementListener,
                       MeasurementAdapter.OnMeasurementActionListener photoListener, int catalogId,
                       boolean isCommonSpace) {
        super(DIFF_CALLBACK);
        this.roomViewModel = roomViewModel;
        this.outletViewModel = outletViewModel;
        this.lifecycleOwner = lifecycleOwner;
        this.context = context;
        this.applianceOptions = applianceOptions;
        this.breakerTypes = breakerTypes;
        this.noteOptions = noteOptions;
        this.ampsOptions = ampsOptions;
        this.deleteListener = deleteListener;
        this.addMeasurementListener = addMeasurementListener;
        this.photoListener = photoListener;
        this.catalogId = catalogId;
        this.isCommonSpace = isCommonSpace;
    }

    public void setNewlyAddedMeasurementId(long id) {
        this.newlyAddedMeasurementId = id;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.parentRecyclerView = recyclerView;
    }

    @Override
    public void onCurrentListChanged(@NonNull List<RoomInFlat> previousList, @NonNull List<RoomInFlat> currentList) {
        super.onCurrentListChanged(previousList, currentList);
        if (currentList.size() > previousList.size() && parentRecyclerView != null) {
            parentRecyclerView.post(() -> parentRecyclerView.smoothScrollToPosition(currentList.size() - 1));
        }
    }

    @Override
    public void onViewRecycled(@NonNull RoomViewHolder holder) {
        super.onViewRecycled(holder);
        holder.unbind();
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RoomCardItemBinding binding = RoomCardItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new RoomViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class RoomViewHolder extends RecyclerView.ViewHolder {
        private final RoomCardItemBinding binding;
        private MeasurementAdapter measurementAdapter;

        private LiveData<List<OutletMeasurement>> currentLiveData;
        private Observer<List<OutletMeasurement>> currentObserver;
        private List<OutletMeasurement> currentMeasurements = null;

        private boolean isLoadingChunks = false;
        private boolean isFullyLoaded = false;
        private int currentChunkIndex = 0;
        private static final int CHUNK_SIZE = 25;
        private final List<OutletMeasurement> loadedChunksList = new ArrayList<>();
        private final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        private int currentRoomId = -1;

        RoomViewHolder(RoomCardItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void unbind() {
            cancelChunkedLoading();
            if (currentLiveData != null && currentObserver != null) {
                currentLiveData.removeObserver(currentObserver);
            }
            currentMeasurements = null;
            currentRoomId = -1;
            loadedChunksList.clear();
            isFullyLoaded = false;
            isLoadingChunks = false;
        }

        void bind(RoomInFlat room) {
            boolean roomChanged = (this.currentRoomId != room.id);

            if (roomChanged) {
                unbind();
                this.currentRoomId = room.id;
                setupNestedRecyclerView(room.name);
            }

            boolean isLokale = Settings.mainRoomName.equalsIgnoreCase(room.name);

            if (isCommonSpace) {
                binding.deleteRoomButton.setText("Usuń pomieszczenie");
                if (room.name.equals(Settings.mainRoomName)) {
                    binding.deleteRoomButton.setVisibility(View.GONE);
                }
                binding.roomTitle.setText(room.name != null ? room.name : ("Pomieszczenie " + room.id));
            } else {
                binding.deleteRoomButton.setText("USUŃ POKÓJ");
                binding.roomTitle.setText(room.name != null ? room.name : ("Pokój " + room.id));
            }

            if (isCommonSpace && isLokale) {
                binding.toggleMeasurementsButton.setVisibility(View.VISIBLE);

                boolean isExpanded = expandedRoomIds.contains(room.id);
                updateExpansionUI(isExpanded);

                binding.toggleMeasurementsButton.setOnClickListener(v -> {
                    if (isLoadingChunks) {
                        expandedRoomIds.remove(room.id);
                        cancelChunkedLoading();
                        updateExpansionUI(false);
                        measurementAdapter.submitList(null);
                        adjustRecyclerViewHeight(0);
                        return;
                    }

                    boolean isNowExpanded = !expandedRoomIds.contains(room.id);
                    if (isNowExpanded) {
                        expandedRoomIds.add(room.id);
                        updateExpansionUI(true);
                        startChunkedLoading();
                    } else {
                        expandedRoomIds.remove(room.id);
                        updateExpansionUI(false);
                        cancelChunkedLoading();
                        measurementAdapter.submitList(null);
                        adjustRecyclerViewHeight(0);
                        isFullyLoaded = false;
                    }
                });
            } else {
                binding.toggleMeasurementsButton.setVisibility(View.GONE);
                updateExpansionUI(true);
            }

            binding.deleteRoomButton.setOnClickListener(v -> deleteListener.accept(room));
            binding.addMeasurementBtn.setOnClickListener(v -> addMeasurementListener.accept(room.id));

            if (roomChanged) {
                currentLiveData = outletViewModel.getMeasurementsForRoom(room.id);
                currentObserver = measurements -> {
                    this.currentMeasurements = measurements;

                    if (measurements != null && !measurements.isEmpty()) {
                        binding.measurementsHeader.setVisibility(View.VISIBLE);
                        binding.emptyMeasurementsText.setVisibility(View.GONE);
                    } else {
                        binding.measurementsHeader.setVisibility(View.GONE);
                        binding.emptyMeasurementsText.setVisibility(View.VISIBLE);
                    }

                    final long currentFocusId = newlyAddedMeasurementId;
                    if (currentFocusId != -1) {
                        measurementAdapter.setFocusToMeasurementId(currentFocusId);
                    }

                    Runnable handleFocusAfterSubmit = () -> {
                        if (currentFocusId != -1 && measurements != null) {
                            for (int i = 0; i < measurements.size(); i++) {
                                if (measurements.get(i).id == currentFocusId) {
                                    int finalI = i;
                                    binding.measurementsRecyclerView.post(() -> binding.measurementsRecyclerView.scrollToPosition(finalI));
                                    if (newlyAddedMeasurementId == currentFocusId) {
                                        newlyAddedMeasurementId = -1;
                                    }
                                    break;
                                }
                            }
                        }
                    };

                    if (expandedRoomIds.contains(room.id) || !isCommonSpace || !isLokale) {
                        if (isCommonSpace && isLokale) {
                            if (isFullyLoaded && !isLoadingChunks) {
                                measurementAdapter.submitList(new ArrayList<>(measurements), handleFocusAfterSubmit);
                                adjustRecyclerViewHeight(measurements.size());
                            } else if (!isLoadingChunks) {
                                startChunkedLoading();
                            }
                        } else {
                            measurementAdapter.submitList(measurements, handleFocusAfterSubmit);
                            adjustRecyclerViewHeight(measurements != null ? measurements.size() : 0);
                        }
                    } else {
                        cancelChunkedLoading();
                        measurementAdapter.submitList(null);
                        adjustRecyclerViewHeight(0);
                    }
                };
                currentLiveData.observe(lifecycleOwner, currentObserver);
            }
        }

        // Zoptymalizowano: Unikanie niepotrzebnych requestLayout() poprzez sprawdzanie aktualnej wartości wysokości
        private void adjustRecyclerViewHeight(int itemCount) {
            ViewGroup.LayoutParams params = binding.measurementsRecyclerView.getLayoutParams();
            int targetHeight = (itemCount > 7) ? (int) (350 * context.getResources().getDisplayMetrics().density) : ViewGroup.LayoutParams.WRAP_CONTENT;
            boolean targetNestedScrolling = itemCount > 7;

            if (params.height != targetHeight || binding.measurementsRecyclerView.isNestedScrollingEnabled() != targetNestedScrolling) {
                params.height = targetHeight;
                binding.measurementsRecyclerView.setNestedScrollingEnabled(targetNestedScrolling);
                binding.measurementsRecyclerView.setLayoutParams(params);
            }
        }

        private void startChunkedLoading() {
            cancelChunkedLoading();
            if (currentMeasurements == null || currentMeasurements.isEmpty()) {
                binding.toggleMeasurementsButton.setText("Ukryj");
                measurementAdapter.submitList(null);
                adjustRecyclerViewHeight(0);
                return;
            }

            isLoadingChunks = true;
            isFullyLoaded = false;
            currentChunkIndex = 0;
            loadedChunksList.clear();

            binding.toggleMeasurementsButton.setText("⏳ Anuluj...");
            if (binding.progressBar != null) {
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.progressBar.setMax(currentMeasurements.size());
                binding.progressBar.setProgress(0);
            }

            adjustRecyclerViewHeight(currentMeasurements.size());

            loadNextChunk();
        }

        private void loadNextChunk() {
            if (!isLoadingChunks || currentMeasurements == null) return;

            int start = currentChunkIndex * CHUNK_SIZE;
            if (start >= currentMeasurements.size()) {
                finishChunkLoading();
                return;
            }

            int end = Math.min(start + CHUNK_SIZE, currentMeasurements.size());

            backgroundExecutor.execute(() -> {
                if (!isLoadingChunks) return;
                List<OutletMeasurement> nextChunk = currentMeasurements.subList(start, end);

                List<OutletMeasurement> newListToSubmit = new ArrayList<>(loadedChunksList);
                newListToSubmit.addAll(nextChunk);
                loadedChunksList.addAll(nextChunk);

                mainHandler.post(() -> {
                    if (!isLoadingChunks) return;

                    final long currentFocusId = newlyAddedMeasurementId;
                    if (currentFocusId != -1) {
                        measurementAdapter.setFocusToMeasurementId(currentFocusId);
                    }

                    measurementAdapter.submitList(newListToSubmit, () -> {
                        if (!isLoadingChunks) return;

                        currentChunkIndex++;
                        if (binding.progressBar != null) {
                            binding.progressBar.setProgress(Math.min(currentChunkIndex * CHUNK_SIZE, currentMeasurements.size()));
                        }

                        if (currentFocusId != -1) {
                            for (int i = 0; i < newListToSubmit.size(); i++) {
                                if (newListToSubmit.get(i).id == currentFocusId) {
                                    int finalI = i;
                                    binding.measurementsRecyclerView.post(() -> binding.measurementsRecyclerView.scrollToPosition(finalI));
                                    if (newlyAddedMeasurementId == currentFocusId) {
                                        newlyAddedMeasurementId = -1;
                                    }
                                    break;
                                }
                            }
                        }

                        loadNextChunk();
                    });
                });
            });
        }

        private void finishChunkLoading() {
            isLoadingChunks = false;
            isFullyLoaded = true;
            binding.toggleMeasurementsButton.setText("Ukryj");
            if (binding.progressBar != null) {
                binding.progressBar.setVisibility(View.GONE);
            }
        }

        private void cancelChunkedLoading() {
            isLoadingChunks = false;
            if (binding.progressBar != null) {
                binding.progressBar.setVisibility(View.GONE);
            }
        }

        private void updateExpansionUI(boolean isExpanded) {
            if (isExpanded) {
                binding.measurementsContainer.setVisibility(View.VISIBLE);
                binding.addMeasurementBtn.setVisibility(View.VISIBLE);
                if (!isLoadingChunks && isFullyLoaded) {
                    binding.toggleMeasurementsButton.setText("Ukryj");
                }
            } else {
                binding.measurementsContainer.setVisibility(View.GONE);
                binding.addMeasurementBtn.setVisibility(View.GONE);
                binding.toggleMeasurementsButton.setText("Pokaż");
            }
        }

        private void setupNestedRecyclerView(String roomName) {
            measurementAdapter = new MeasurementAdapter(
                    (RoomActivity) context,
                    outletViewModel,
                    applianceOptions,
                    breakerTypes,
                    noteOptions,
                    ampsOptions,
                    catalogId,
                    isCommonSpace,
                    roomName,
                    photoListener
            );
            binding.measurementsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            binding.measurementsRecyclerView.setAdapter(measurementAdapter);

            binding.measurementsRecyclerView.setOnTouchListener((v, event) -> {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                }
                return false;
            });
        }
    }

    private static final DiffUtil.ItemCallback<RoomInFlat> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<RoomInFlat>() {
                @Override
                public boolean areItemsTheSame(@NonNull RoomInFlat oldItem, @NonNull RoomInFlat newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull RoomInFlat oldItem, @NonNull RoomInFlat newItem) {
                    return (oldItem.name == null ? newItem.name == null : oldItem.name.equals(newItem.name))
                            && oldItem.flatId == newItem.flatId;
                }
            };
}