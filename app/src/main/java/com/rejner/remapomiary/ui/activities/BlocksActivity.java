package com.rejner.remapomiary.ui.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.opengl.Visibility;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.flexbox.FlexboxLayout;
import com.rejner.remapomiary.R;
import com.rejner.remapomiary.data.entities.Block;
import com.rejner.remapomiary.data.entities.BlockFullData;
import com.rejner.remapomiary.data.entities.Catalog;
import com.rejner.remapomiary.data.entities.Client;
import com.rejner.remapomiary.data.utils.LiveDataUtil;
import com.rejner.remapomiary.ui.utils.PostalCodeTextWatcher;
import com.rejner.remapomiary.ui.viewmodels.BlockViewModel;
import com.rejner.remapomiary.ui.viewmodels.CatalogViewModel;
import com.rejner.remapomiary.ui.viewmodels.ClientViewModel;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BlocksActivity extends AppCompatActivity {


    private int catalogId;
    private boolean sortByCreation = false;
    private ArrayAdapter arrayAdapter;
    private BlockViewModel blockViewModel;
    private CatalogViewModel catalogViewModel;
    private Catalog catalog;
    private EditText city;
    private EditText street;
    private EditText postal_code;
    private EditText number;
    private List<EditText> inputs;

    private Spinner spinnerCreation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocks);
        city = findViewById(R.id.inputBlockCity);
        street = findViewById(R.id.inputBlockStreet);
        number = findViewById(R.id.inputBlockNumber);
        postal_code = findViewById(R.id.inputBlockPostalCode);
        spinnerCreation = findViewById(R.id.spinner);
        postal_code.addTextChangedListener(new PostalCodeTextWatcher(postal_code));

        inputs = Arrays.asList(city, street, postal_code, number);
        catalogId = getIntent().getIntExtra("catalogId", 0);
        blockViewModel = new ViewModelProvider(this).get(BlockViewModel.class);
        catalogViewModel = new ViewModelProvider(BlocksActivity.this).get(CatalogViewModel.class);
        catalogViewModel.getCatalogById(catalogId, catalog1 -> {
            catalog = catalog1;
            runOnUiThread(() -> {
                updateDisplay();
            });
        });
        blockViewModel.getBlocksWithFullData(catalogId).observe(this, this::updateView);
        List<String> sortOptions = Arrays.asList("Data utworzenia", "Data edycji");

        arrayAdapter = new ArrayAdapter(BlocksActivity.this, android.R.layout.simple_spinner_item, sortOptions);
        Spinner spinnerSort = findViewById(R.id.sortBySpinner);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerSort.setAdapter(arrayAdapter);
        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean before = sortByCreation;
                sortByCreation = sortOptions.get(position).equals("Data edycji");
                if (sortByCreation != before) {
                    LiveDataUtil.observeOnce(blockViewModel.getBlocksWithFullData(catalogId), BlocksActivity.this, blocks -> {
                        updateBlocks(blocks);
                    });
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
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
    private void updateView(List<BlockFullData> blocks) {
        updateBlocks(blocks);
    }

    private void resetBlockInput() {
        TextView blocksTitle = findViewById(R.id.blocksTitle);
        blocksTitle.setText("Bloki dla katalogu - " + catalog.title);
        city.setText(catalog.city);
        street.setText(catalog.street);
        postal_code.setText(catalog.postal_code);
        number.setText("");
        hideKeyboard();

        for (EditText editText : inputs) {
            editText.clearFocus();
        }
    }

    private void createClientsSpinner(Spinner spinnerItem) {
        ClientViewModel clientViewModel = new ViewModelProvider(BlocksActivity.this).get(ClientViewModel.class);

        LiveDataUtil.observeOnce(clientViewModel.getClientsInCatalog(catalogId), this, clients -> {

            if (clients.isEmpty()) {
                Client emptyClient = new Client("", "", "", "Brak zleceniodawców", -1);
                clients.add(emptyClient);
            }
            ArrayAdapter clientsAdapter = new ArrayAdapter(BlocksActivity.this, android.R.layout.simple_spinner_dropdown_item, clients);

            spinnerItem.setAdapter(clientsAdapter);
        });
    }

    private void updateDisplay() {

        createClientsSpinner(spinnerCreation);
        resetBlockInput();

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        Button createButton = findViewById(R.id.blockAdd);
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createBlock();
            }
        });
        Button cancelButton = findViewById(R.id.blockCancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetBlockInput();
            }
        });
    }

    private void createBlock() {
        for (EditText input : inputs) {
            if (input.getText().toString().isEmpty()) {
                input.setError("Wymagane pole");
                Toast.makeText(BlocksActivity.this, input.getHint().toString() + " nie jest podany/e", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        Client selectedClient = (Client) spinnerCreation.getSelectedItem();
        if (selectedClient.name.equals("Brak zleceniodawców")) {
            Toast.makeText(BlocksActivity.this, "Nie wybrano zleceniodawcy!", Toast.LENGTH_SHORT).show();
            return;
        }
        Block newBlock = new Block(catalogId, street.getText().toString(), city.getText().toString(), number.getText().toString(), postal_code.getText().toString(), selectedClient.id, new Date(), new Date());
        blockViewModel.insert(newBlock);
        catalogViewModel.updateEdition(catalogId);
        resetBlockInput();


    }

    private void updateBlocks(List<BlockFullData> blocks) {

        TextView noBlocks = findViewById(R.id.noBlocks);

        if (blocks.isEmpty()) {
            noBlocks.setVisibility(View.VISIBLE);
            return;
        } else {
            noBlocks.setVisibility(View.GONE);
        }

        if (!sortByCreation) {
            blocks.sort(Comparator.comparing((BlockFullData b) -> b.block.edition_date).reversed());

        } else {
            blocks.sort(Comparator.comparing((BlockFullData b) -> b.block.creation_date).reversed());
        }

        FlexboxLayout flexboxLayout = findViewById(R.id.blocks);
        flexboxLayout.removeAllViews();
        for (BlockFullData block : blocks) {
            View blockView = getLayoutInflater().inflate(R.layout.block_item, flexboxLayout, false);

            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            SimpleDateFormat sdfh = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());

            TextView street = blockView.findViewById(R.id.blockStreet);
            TextView city = blockView.findViewById(R.id.blockCity);
            TextView number = blockView.findViewById(R.id.blockNumber);
            TextView postalCode = blockView.findViewById(R.id.blockPostalCode);
            TextView clientName = blockView.findViewById(R.id.blockClientName);
            TextView clientAddress = blockView.findViewById(R.id.blockClientAddress);
            TextView blockCreationDate = blockView.findViewById(R.id.blockCreationDate);
            TextView blockEditionDate = blockView.findViewById(R.id.blockLastEdited);
            TextView blockTitle = blockView.findViewById(R.id.blockTitle);

            blockCreationDate.setText(sdf.format(block.block.creation_date));
            blockEditionDate.setText(sdfh.format(block.block.edition_date));
            street.setText(block.block.street);
            city.setText(block.block.city);
            number.setText(block.block.number);
            postalCode.setText(block.block.postal_code);
            clientName.setText(block.getClient().name);
            clientAddress.setText(block.getClient().city + ", " + block.getClient().street + ", " + block.getClient().postal_code);
            blockTitle.setText("Blok - " + block.block.number);

            Button deleteButton = blockView.findViewById(R.id.blockDelete);
            Button editButton = blockView.findViewById(R.id.blockEdit);

            blockView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openBlock(block);
                }
            });
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteBlock(block);
                }
            });

            editButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateBlock(block, blockView, editButton, deleteButton);
                }
            });
            flexboxLayout.addView(blockView);
        }
    }

    private void openBlock(BlockFullData block) {
        Intent intent = new Intent(BlocksActivity.this, FlatsActivity.class);
        intent.putExtra("blockId", block.block.id);
        startActivity(intent);
    }

    private void deleteBlock(BlockFullData block) {
        AlertDialog.Builder builder = new AlertDialog.Builder(BlocksActivity.this);
        builder.setTitle("Potwierdzenie");
        builder.setMessage("Czy na pewno chcesz usunąć ten blok?");
        builder.setPositiveButton("Tak", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                blockViewModel.repository.delete(block.block);
                catalogViewModel.updateEdition(catalogId);
                Toast.makeText(BlocksActivity.this, "Blok oraz jego zawartość została usunięta", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Nie", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    private void updateBlock(BlockFullData block, View blockView, Button editButton, Button deleteButton) {
        blockView.setOnClickListener(null);

        TextView street = blockView.findViewById(R.id.blockStreet);
        TextView city = blockView.findViewById(R.id.blockCity);
        TextView number = blockView.findViewById(R.id.blockNumber);
        TextView postalCode = blockView.findViewById(R.id.blockPostalCode);
        TextView clientName = blockView.findViewById(R.id.blockClientName);
        TextView clientAddress = blockView.findViewById(R.id.blockClientAddress);
        TextView blockCreationDate = blockView.findViewById(R.id.blockCreationDate);
        TextView blockEditionDate = blockView.findViewById(R.id.blockLastEdited);
        TextView blockTitle = blockView.findViewById(R.id.blockTitle);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMarginStart(dpToPx(25));

        List<TextView> locationTextViews = Arrays.asList(city, street, number, postalCode);
        for (TextView textView : locationTextViews) {
            EditText editText = new EditText(BlocksActivity.this);
            editText.setText(textView.getText());
            editText.setLayoutParams(params);
            editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            LinearLayout linearLayout = blockView.findViewById(R.id.itemBlockData);
            if (textView == postalCode) {
                editText.addTextChangedListener(new PostalCodeTextWatcher(editText));
                editText.setMaxLines(1);
                editText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
            }
            int index = linearLayout.indexOfChild(textView);
            linearLayout.removeView(textView);
            linearLayout.addView(editText, index);
        }
        LinearLayout clientLayout = blockView.findViewById(R.id.blockItemClient);
        clientLayout.removeView(clientName);
        int index = clientLayout.indexOfChild(clientAddress);
        clientLayout.removeView(clientAddress);
        Spinner spinner = new Spinner(BlocksActivity.this);
        spinner.setLayoutParams(params);
        createClientsSpinner(spinner);
        clientLayout.addView(spinner, index);

        editButton.setText("✅ Zapisz");
        deleteButton.setText("❌ Anuluj");

        deleteButton.setOnClickListener(v -> LiveDataUtil.observeOnce(blockViewModel.getBlocksWithFullData(catalogId), BlocksActivity.this, this::updateBlocks));

        editButton.setOnClickListener(v -> {
            ArrayList<String> list = new ArrayList<>();
            LinearLayout linearLayout = blockView.findViewById(R.id.itemBlockData);
            for (int i = 0; i < linearLayout.getChildCount(); i++) {
                View child = linearLayout.getChildAt(i);
                if (child instanceof EditText) {
                    EditText editText = (EditText) child;
                    String value = editText.getText().toString();
                    list.add(value);
                }
            }
            Client selectedClient = (Client) spinner.getSelectedItem();
            if (selectedClient.name.equals("Brak zleceniodawców")) {
                Toast.makeText(BlocksActivity.this, "Nie wybrano zleceniodawcy!", Toast.LENGTH_SHORT).show();
                return;

            }

            Block newBlock = new Block(catalogId, list.get(1), list.get(0), list.get(2), list.get(3), selectedClient.id, block.block.creation_date, new Date());
            newBlock.id = block.block.id;
            blockViewModel.update(newBlock);
            catalogViewModel.updateEdition(catalogId);
        });

    }
}
