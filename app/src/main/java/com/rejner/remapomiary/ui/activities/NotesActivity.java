
package com.rejner.remapomiary.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.rejner.remapomiary.R;
import com.rejner.remapomiary.data.entities.Flat;
import com.rejner.remapomiary.data.utils.LiveDataUtil;
import com.rejner.remapomiary.ui.viewmodels.FlatViewModel;

public class NotesActivity extends AppCompatActivity {

    private FlatViewModel flatViewModel;
    private Flat currentFlat;
    private int flatId;

    private RadioGroup radioGroup;
    private RadioButton radioDopuszczona;
    private RadioButton radioDopuszczonaUsterki;
    private RadioButton radioNiedopuszczona;
    private EditText notesEditText;
    private TextView currentMode;
    private Button saveButton;
    private Button blockGrade;
    private boolean areButtonsSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);


        flatId = getIntent().getIntExtra("flatId", -1);
        if (flatId == -1) {
            Toast.makeText(this, "Nieprawidłowe ID mieszkania", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        radioGroup = findViewById(R.id.radioGroup_instalacja);
        radioDopuszczona = findViewById(R.id.radio_dopuszczona);
        radioDopuszczonaUsterki = findViewById(R.id.radio_dopuszczona_usterki);
        radioNiedopuszczona = findViewById(R.id.radio_niedopuszczona);
        notesEditText = findViewById(R.id.notedEditText);
        saveButton = findViewById(R.id.notesSave);
        blockGrade = findViewById(R.id.blockGrade);
        currentMode = findViewById(R.id.currentMode);

        flatViewModel = new ViewModelProvider(this).get(FlatViewModel.class);

        // Main observer - single source of truth for UI updates
        flatViewModel.getCombinedFlat(flatId).observe(this, flat -> {
            if (flat != null) {
                currentFlat = flat;
                notesEditText.setText(flat.notes);
                setGradeSelection();
                gradeButtonState();
            }
            if (!areButtonsSet) {
                setupUIElements();
                areButtonsSet = true;
            }
        });

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (currentFlat == null) return;

            int newGrade = currentFlat.grade;
            if (checkedId == R.id.radio_dopuszczona) {
                newGrade = 0;
            } else if (checkedId == R.id.radio_dopuszczona_usterki) {
                newGrade = 1;
            } else if (checkedId == R.id.radio_niedopuszczona) {
                newGrade = 2;
            }

            if (currentFlat.grade != newGrade) {
                currentFlat.grade = newGrade;
                flatViewModel.update(currentFlat);
            }
        });

        blockGrade.setOnClickListener(v -> {
            if (currentFlat == null) return;

            flatViewModel.toggleGradeBlock(currentFlat.id); // Załóżmy, że Flat ma metodę getId()

        });

        saveButton.setOnClickListener(v -> {
            if (currentFlat != null) {
                currentFlat.notes = notesEditText.getText().toString();
                hideKeyboard();
                notesEditText.clearFocus();
                flatViewModel.update(currentFlat);
            }
        });
    }
    private void setupUIElements() {
        Button backButton = findViewById(R.id.backButton);
        Button roomButton = findViewById(R.id.roomsButton);
        Button RCDButton = findViewById(R.id.RCDButton);
        Button boardButton = findViewById(R.id.boardButton);
        TextView titleView = findViewById(R.id.rcdTitle);
        titleView.setText("Mieszkanie numer - " + currentFlat.number + " podsumowanie");

        boardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NotesActivity.this, BoardActivity.class);
                intent.putExtra("flatId", currentFlat.id);
                startActivity(intent);
            }
        });

        roomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NotesActivity.this, RoomActivity.class);
                intent.putExtra("flatId", currentFlat.id);
                startActivity(intent);
            }
        });


        RCDButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NotesActivity.this, RCDActivity.class);
                intent.putExtra("flatId", currentFlat.id);
                startActivity(intent);
            }
        });


        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentFlat == null) return;
                Intent intent = new Intent(NotesActivity.this, FlatsActivity.class);
                intent.putExtra("blockId", currentFlat.blockId);
                startActivity(intent);
            }
        });

    }
    private void gradeButtonState() {
        if (currentFlat != null) {
            if (currentFlat.gradeByUser == 0) {
                currentMode.setText("Automatyczna aktualizacja włączona!");
                blockGrade.setText("Wyłącz automatyczną aktualizacje");
                blockGrade.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F44336")));
            } else {
                currentMode.setText("Automatyczna aktualizacja wyłączona!");

                blockGrade.setText("Włącz automatyczną aktualizacje");
                blockGrade.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ff669900")));
            }
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view == null) {
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void setGradeSelection() {
        if (currentFlat != null) {
            if (currentFlat.grade == 0) {
                radioDopuszczona.setChecked(true);
            } else if (currentFlat.grade == 1) {
                radioDopuszczonaUsterki.setChecked(true);
            } else if (currentFlat.grade == 2) {
                radioNiedopuszczona.setChecked(true);
            }
        }
    }
}