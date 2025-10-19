package com.rejner.remapomiary.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.rejner.remapomiary.R;
import com.rejner.remapomiary.data.entities.RoomInFlat;
import com.rejner.remapomiary.databinding.RoomCardItemBinding;
import com.rejner.remapomiary.ui.activities.RoomActivity;
import com.rejner.remapomiary.ui.viewmodels.OutletMeasurementViewModel;
import com.rejner.remapomiary.ui.viewmodels.RoomViewModel;

import java.util.function.Consumer;

public class RoomAdapter extends ListAdapter<RoomInFlat, RoomAdapter.RoomViewHolder> {

    private final RoomViewModel roomViewModel;
    private final OutletMeasurementViewModel outletViewModel;
    private final LifecycleOwner lifecycleOwner;
    private final Context context;
    private final String[] applianceOptions, breakerTypes, noteOptions, ampsOptions;
    private final Consumer<RoomInFlat> deleteListener;
    private final Consumer<Integer> addMeasurementListener;
    private long newlyAddedMeasurementId = -1;
    private int catalogId;

    public RoomAdapter(RoomViewModel roomViewModel, OutletMeasurementViewModel outletViewModel,
                       LifecycleOwner lifecycleOwner, Context context,
                       String[] applianceOptions, String[] breakerTypes, String[] noteOptions, String[] ampsOptions,
                       Consumer<RoomInFlat> deleteListener, Consumer<Integer> addMeasurementListener, int catalogId) {
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
        this.catalogId = catalogId;
    }

    public void setNewlyAddedMeasurementId(long id) {
        this.newlyAddedMeasurementId = id;
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
        RoomInFlat room = getItem(position);
        holder.bind(room);
    }

    class RoomViewHolder extends RecyclerView.ViewHolder {
        private final RoomCardItemBinding binding;
        private MeasurementAdapter measurementAdapter;

        RoomViewHolder(RoomCardItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(RoomInFlat room) {
            binding.roomTitle.setText(room.name != null ? room.name : ("Pokój " + room.id));

            // Ustawienie usuwania pokoju
            binding.deleteRoomButton.setOnClickListener(v -> deleteListener.accept(room));

            // Ustawienie dodawania pomiaru
            binding.addMeasurementBtn.setOnClickListener(v -> addMeasurementListener.accept(room.id));

            // Konfiguracja zagnieżdżonego RecyclerView
            setupNestedRecyclerView(room.id);

            // Obserwacja pomiarów dla TEGO pokoju
            outletViewModel.getMeasurementsForRoom(room.id).observe(lifecycleOwner, measurements -> {
                if (measurements != null && !measurements.isEmpty()) {
                    binding.measurementsHeader.setVisibility(View.VISIBLE);
                    binding.emptyMeasurementsText.setVisibility(View.GONE);
                } else {
                    binding.measurementsHeader.setVisibility(View.GONE);
                    binding.emptyMeasurementsText.setVisibility(View.VISIBLE);
                }
                measurementAdapter.submitList(measurements);

                // Sprawdź, czy nowo dodany element należy do tego pokoju
                if (newlyAddedMeasurementId != -1) {
                    boolean found = false;
                    for (int i = 0; i < measurements.size(); i++) {
                        if (measurements.get(i).id == newlyAddedMeasurementId) {
                            int finalI = i;
                            binding.measurementsRecyclerView.post(() -> {
                                binding.measurementsRecyclerView.smoothScrollToPosition(finalI);
                                // Przekaż ID do adaptera pomiarów, aby ustawił fokus
                                measurementAdapter.setFocusToMeasurementId(newlyAddedMeasurementId);
                                newlyAddedMeasurementId = -1; // Resetuj
                            });
                            found = true;
                            break;
                        }
                    }
                }
            });
        }

        private void setupNestedRecyclerView(int roomId) {
            measurementAdapter = new MeasurementAdapter(
                    (RoomActivity) context, // Przekaż aktywność jako kontekst i managera klawiatury
                    outletViewModel,
                    applianceOptions,
                    breakerTypes,
                    noteOptions,
                    ampsOptions,
                    catalogId
            );
            binding.measurementsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            binding.measurementsRecyclerView.setAdapter(measurementAdapter);
            // Wyłączenie zagnieżdżonego przewijania, aby główny RecyclerView przewijał się płynnie
            binding.measurementsRecyclerView.setNestedScrollingEnabled(false);
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
                    return oldItem.name.equals(newItem.name) && oldItem.flatId == newItem.flatId;
                }
            };
}