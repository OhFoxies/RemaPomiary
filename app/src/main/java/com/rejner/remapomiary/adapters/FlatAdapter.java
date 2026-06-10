package com.rejner.remapomiary.adapters;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexboxLayoutManager;
import com.rejner.remapomiary.R;
import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.data.entities.Template;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class FlatAdapter extends ListAdapter<Flat, RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private final OnFlatActionListener listener;
    private final SimpleDateFormat creationDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat editDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    private List<Template> templates;
    private int selectedSortPosition = 0;
    private String flatCountText = "";

    public interface OnFlatActionListener {
        void onFlatClick(Flat flat);
        void onFlatDelete(Flat flat);
        void onFlatEdit(Flat flat, String newNumber);
        void onFlatMark(Flat flat);
        void onGenerateProtocol(Flat flat, int protocolNumber);
        void onCreateFlat(String number, Template template);
        void onSortSelected(int position);
    }

    public FlatAdapter(OnFlatActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setHeaderData(List<Template> templates, int sortPosition, String countText) {
        this.templates = templates;
        this.selectedSortPosition = sortPosition;
        this.flatCountText = countText;
        notifyItemChanged(0);
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_flats_header, parent, false);
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (lp instanceof FlexboxLayoutManager.LayoutParams) {
                ((FlexboxLayoutManager.LayoutParams) lp).setFlexBasisPercent(1.0f);
            }
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.flat_item, parent, false);
            return new FlatViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind();
        } else {
            ((FlatViewHolder) holder).bind(getItem(position - 1));
        }
    }

    @Override
    public int getItemCount() {
        return getCurrentList().size() + 1;
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {
        EditText inputFlatNumber;
        Spinner templatesSpinner, sortBySpinner;
        Button flatAddButton, flatCancelButton;
        TextView noFlatsText;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            inputFlatNumber = itemView.findViewById(R.id.inputFlatNumber);
            templatesSpinner = itemView.findViewById(R.id.templatesSpinner);
            sortBySpinner = itemView.findViewById(R.id.sortBySpinner);
            flatAddButton = itemView.findViewById(R.id.flatAdd);
            flatCancelButton = itemView.findViewById(R.id.flatCancel);
            noFlatsText = itemView.findViewById(R.id.noFlats);

            setupSortSpinner();
        }

        void bind() {
            if (templates != null) {
                ArrayAdapter<Template> adapter = new ArrayAdapter<>(itemView.getContext(), android.R.layout.simple_spinner_item, templates);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                templatesSpinner.setAdapter(adapter);
            }

            if (noFlatsText != null) noFlatsText.setText(flatCountText);
            if (sortBySpinner != null) sortBySpinner.setSelection(selectedSortPosition, false);

            flatAddButton.setOnClickListener(v -> {
                String num = inputFlatNumber.getText().toString().trim();
                Template t = (Template) templatesSpinner.getSelectedItem();
                listener.onCreateFlat(num, t);
                inputFlatNumber.setText("");
            });

            flatCancelButton.setOnClickListener(v -> {
                inputFlatNumber.setText("");
                inputFlatNumber.clearFocus();
            });
        }

        private void setupSortSpinner() {
            String[] sortOptions = {"Numer mieszkania", "Data utworzenia \\/", "Data utworzenia /\\", "Data edycji", "Status", "Uwagi na początku"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(itemView.getContext(), android.R.layout.simple_spinner_item, sortOptions);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sortBySpinner.setAdapter(adapter);
            sortBySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    if (position != selectedSortPosition) {
                        listener.onSortSelected(position);
                    }
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }
    }

    class FlatViewHolder extends RecyclerView.ViewHolder {
        TextView title, creationDate, editDate, status, notes;
        Button markButton, deleteButton, editButton, createProtocolButton;
        EditText protocolNumber, titleEdit;
        View flatMain;

        FlatViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.flatTitle);
            titleEdit = itemView.findViewById(R.id.flatTitleEdit);
            creationDate = itemView.findViewById(R.id.flatCreationDate);
            editDate = itemView.findViewById(R.id.flatLastEdited);
            status = itemView.findViewById(R.id.blockLastEdited);
            notes = itemView.findViewById(R.id.flatNotesDesc);
            markButton = itemView.findViewById(R.id.flatMark);
            deleteButton = itemView.findViewById(R.id.blockDelete);
            editButton = itemView.findViewById(R.id.blockEdit);
            createProtocolButton = itemView.findViewById(R.id.generateProtocol);
            protocolNumber = itemView.findViewById(R.id.inputProtocolNumber);
            flatMain = itemView.findViewById(R.id.flatMain);
        }

        void bind(Flat flat) {
            title.setText("Mieszkanie nr " + flat.number);
            if (flat.creation_date != null) creationDate.setText(creationDateFormat.format(flat.creation_date));
            if (flat.edition_date != null) editDate.setText(editDateFormat.format(flat.edition_date));
            status.setText(flat.status);

            if ((flat.notes != null && !flat.notes.isEmpty()) || (flat.circuitNotes != null && !flat.circuitNotes.isEmpty())) {
                StringBuilder sb = new StringBuilder();
                if (flat.notes != null && !flat.notes.isEmpty()) sb.append("Notatki:\n").append(flat.notes).append("\n");
                if (flat.circuitNotes != null && !flat.circuitNotes.isEmpty()) sb.append("Notatki rozdzielnia:\n").append(flat.circuitNotes);
                notes.setText(sb.toString());
            } else {
                notes.setText("Brak uwag");
            }

            if (flat.status != null && flat.status.contains("gotowy")) {
                markButton.setText("❌ Oznacz jako niewykonany");
                flatMain.setBackgroundResource(R.drawable.border_done);
                markButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF0000")));
            } else {
                markButton.setText("✅ Oznacz jako gotowy");
                flatMain.setBackgroundResource(R.drawable.border);
                markButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#3FAB1F")));
            }

            title.setVisibility(View.VISIBLE);
            titleEdit.setVisibility(View.GONE);
            editButton.setText("✏️ Edytuj");
            deleteButton.setText("🗑️ Usuń");

            itemView.setOnClickListener(v -> {
                if (titleEdit.getVisibility() != View.VISIBLE) listener.onFlatClick(flat);
            });

            markButton.setOnClickListener(v -> listener.onFlatMark(flat));

            editButton.setOnClickListener(v -> {
                if (titleEdit.getVisibility() != View.VISIBLE) {
                    title.setVisibility(View.GONE);
                    titleEdit.setVisibility(View.VISIBLE);
                    titleEdit.setText(flat.number);
                    editButton.setText("✅ Zapisz");
                    deleteButton.setText("❌ Anuluj");
                } else {
                    listener.onFlatEdit(flat, titleEdit.getText().toString().trim());
                }
            });

            deleteButton.setOnClickListener(v -> {
                if (titleEdit.getVisibility() == View.VISIBLE) {
                    title.setVisibility(View.VISIBLE);
                    titleEdit.setVisibility(View.GONE);
                    editButton.setText("✏️ Edytuj");
                    deleteButton.setText("🗑️ Usuń");
                } else {
                    listener.onFlatDelete(flat);
                }
            });

            createProtocolButton.setOnClickListener(v -> {
                int pNum = -1;
                try {
                    String pStr = protocolNumber.getText().toString();
                    if (!pStr.isEmpty()) pNum = Integer.parseInt(pStr);
                } catch (NumberFormatException ignored) {}
                listener.onGenerateProtocol(flat, pNum);
            });
        }
    }

    private static final DiffUtil.ItemCallback<Flat> DIFF_CALLBACK = new DiffUtil.ItemCallback<Flat>() {
        @Override
        public boolean areItemsTheSame(@NonNull Flat oldItem, @NonNull Flat newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Flat oldItem, @NonNull Flat newItem) {
            return oldItem.number.equals(newItem.number) &&
                    (oldItem.status != null && oldItem.status.equals(newItem.status)) &&
                    (oldItem.edition_date != null && oldItem.edition_date.equals(newItem.edition_date)) &&
                    (oldItem.notes != null ? oldItem.notes.equals(newItem.notes) : newItem.notes == null) &&
                    (oldItem.circuitNotes != null ? oldItem.circuitNotes.equals(newItem.circuitNotes) : newItem.circuitNotes == null);
        }
    };
}
