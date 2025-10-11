package com.rejner.remapomiary.ui.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.flexbox.FlexboxLayout;
import com.rejner.remapomiary.R;
import com.rejner.remapomiary.data.entities.Block;
import com.rejner.remapomiary.data.entities.BlockFullData;
import com.rejner.remapomiary.data.entities.Catalog;
import com.rejner.remapomiary.data.entities.Client;
import com.rejner.remapomiary.data.utils.LiveDataUtil;
import com.rejner.remapomiary.ui.viewmodels.BlockViewModel;
import com.rejner.remapomiary.ui.viewmodels.CatalogViewModel;
import com.rejner.remapomiary.ui.viewmodels.ClientViewModel;

import java.text.SimpleDateFormat;
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

    Spinner spinner;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocks);
        city = findViewById(R.id.inputBlockCity);
        street = findViewById(R.id.inputBlockStreet);
        number = findViewById(R.id.inputBlockNumber);
        postal_code = findViewById(R.id.inputBlockPostalCode);
        spinner = findViewById(R.id.spinner);

        inputs =  Arrays.asList(city, street, postal_code, number);
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
        Spinner spinner = findViewById(R.id.sortBySpinner);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(arrayAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean before = sortByCreation;
                if (sortOptions.get(position).equals("Data edycji")) {
                    sortByCreation = true;

                } else {
                    sortByCreation = false;
                }
                if (sortByCreation != before) {
                    List<BlockFullData> blocks = blockViewModel.getBlocksWithFullData(catalogId).getValue();
                    if (blocks != null) {
                        updateBlocks(blocks);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

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

        for (EditText editText : inputs) {
            editText.clearFocus();
        }
    }

    private void updateDisplay() {
        ClientViewModel clientViewModel = new ViewModelProvider(BlocksActivity.this).get(ClientViewModel.class);

        LiveDataUtil.observeOnce(clientViewModel.getClientsInCatalog(catalogId), this, clients -> {

            if (clients.isEmpty()) {
                Client emptyClient = new Client("", "", "", "Brak zleceniodawców", -1);
                clients.add(emptyClient);
            }
            ArrayAdapter clientsAdapter = new ArrayAdapter(BlocksActivity.this, android.R.layout.simple_spinner_dropdown_item , clients);

            spinner.setAdapter(clientsAdapter);
        });

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
        Client selectedClient = (Client) spinner.getSelectedItem();
        if (selectedClient.name.equals("Brak zleceniodawców")) {
            Toast.makeText(BlocksActivity.this, "Nie wybrano zleceniodawcy!", Toast.LENGTH_SHORT).show();
            return;
        }


            Block newBlock = new Block(catalogId, street.getText().toString(), city.getText().toString(), number.getText().toString(), postal_code.getText().toString(), selectedClient.id, new Date(), new Date());
            blockViewModel.insert(newBlock);
            resetBlockInput();


    }

    private void updateBlocks(List<BlockFullData> blocks) {
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
        Intent intent = new Intent(BlocksActivity.this, BlockActivity.class);
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

    private void updateBlock(BlockFullData block, View blockView, Button editButton, Button deleteButton) {

    }
}