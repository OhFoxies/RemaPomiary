package com.rejner.remapomiary.ui.activities;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.rejner.remapomiary.data.entities.Catalog;
import com.rejner.remapomiary.data.entities.Client;
import com.rejner.remapomiary.ui.viewmodels.CatalogViewModel;
import com.rejner.remapomiary.ui.viewmodels.ClientViewModel;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ClientsActivity extends AppCompatActivity {
    private int catalogId;
    private Catalog catalog;

    private ClientViewModel clientViewModel;
    private EditText clientName;
    private EditText clientCity;
    private EditText clientStreet;
    private EditText clientPostalCode;

    private List<EditText> inputs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clients);
        clientViewModel = new ViewModelProvider(this).get(ClientViewModel.class);
        clientName = findViewById(R.id.inputClientName);
        clientCity = findViewById(R.id.inputClientCity);
        clientStreet = findViewById(R.id.inputClientStreet);
        clientPostalCode = findViewById(R.id.inputClientPostalCode);
        inputs = new ArrayList<>(Arrays.asList(clientName, clientCity, clientStreet, clientPostalCode));

        catalogId = getIntent().getIntExtra("catalogId", 0);
        CatalogViewModel catalogViewModel = new ViewModelProvider(this).get(CatalogViewModel.class);

        catalogViewModel.getCatalogById(catalogId, catalog1 -> {
            catalog = catalog1;
            initializeElements();
        });

        clientViewModel.getClientsInCatalog(catalogId).observe(this, this::updateClientsView);
    }

    public void initializeElements() {
        TextView title = findViewById(R.id.clientsTitle);
        title.setText("Zleceniodawcy, dla katalogu - " + catalog.title);

        Button cancelButton = findViewById(R.id.clientCancel);
        Button clientAdd = findViewById(R.id.clientAdd);
        Button backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();

            }
        });

        clientAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addClient();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearInput();
            }
        });
    }

    public void updateClientsView(List<Client> clients) {
        FlexboxLayout flexboxLayout = findViewById(R.id.clients);
        flexboxLayout.removeAllViews();
        TextView noClients = findViewById(R.id.noClients);

        if (clients.isEmpty()) {
            noClients.setVisibility(View.VISIBLE);
        } else {
            noClients.setVisibility(View.GONE);
        }
        int id = 1;
        for (Client client : clients) {
            View clientView = getLayoutInflater().inflate(R.layout.client_item, flexboxLayout, false);
            TextView name = clientView.findViewById(R.id.clientName);
            TextView city = clientView.findViewById(R.id.clientCity);
            TextView street = clientView.findViewById(R.id.clientStreet);
            TextView postalCode = clientView.findViewById(R.id.clientPostalCode);
            TextView title = clientView.findViewById(R.id.clientTitle);
            title.setText("Zleceniodawca - " + id);
            id++;

            name.setText(client.name);
            city.setText(client.city);
            street.setText(client.street);
            postalCode.setText(client.postal_code);
            flexboxLayout.addView(clientView);

            Button editButton = clientView.findViewById(R.id.clientEdit);
            Button deleteButton = clientView.findViewById(R.id.clientDelete);
            editButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clientEdit(client, clientView, editButton, deleteButton);
                }
            });

            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clientDelete(client);

                }
            });
        }
    }


    public void clientDelete(Client client){
        AlertDialog.Builder builder = new AlertDialog.Builder(ClientsActivity.this);
        builder.setTitle("Potwierdzenie");
        builder.setMessage("Czy na pewno chcesz usunąć tego zleceniodawce? Może to spowodać braki w protokole!");
        builder.setPositiveButton("Tak", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                clientViewModel.delete(client);
                Toast.makeText(ClientsActivity.this, "Zleceniodawca został usunięty! ", Toast.LENGTH_SHORT).show();
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
    public void clientEdit(Client client, View clientView, Button editButton, Button deleteButton) {
        clientView.setOnClickListener(null);
        TextView name = clientView.findViewById(R.id.clientName);
        TextView city = clientView.findViewById(R.id.clientCity);
        TextView street = clientView.findViewById(R.id.clientStreet);
        TextView postalCode = clientView.findViewById(R.id.clientPostalCode);

        List<TextView> list = Arrays.asList(street, city, postalCode);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMarginStart(dpToPx(25));

        for (TextView element: list) {
            EditText editText = new EditText(ClientsActivity.this);
            editText.setText(element.getText().toString());
            LinearLayout linearLayout =  clientView.findViewById(R.id.itemClientData);
            editText.setLayoutParams(params);
            editText.setTextSize(TypedValue.COMPLEX_UNIT_SP,20);
            int index = linearLayout.indexOfChild(element);
            linearLayout.removeView(element);
            linearLayout.addView(editText, index);
        }

        LinearLayout linearLayout =  clientView.findViewById(R.id.clientNameContainer);

        EditText titleEditText = new EditText(ClientsActivity.this);
        titleEditText.setText(name.getText().toString());
        titleEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP,24);


        linearLayout.removeView(name);
        linearLayout.addView(titleEditText, 1);

        editButton.setText("✅ Zapisz");
        deleteButton.setText("❌ Anuluj");

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clientViewModel.update(client.street, client.city, client.postal_code, client.name, catalog.id);

            }
        });

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<String> list = new ArrayList<>();
                LinearLayout linearLayout =  clientView.findViewById(R.id.itemClientData);
                for (int i = 0; i < linearLayout.getChildCount(); i++) {
                    View child = linearLayout.getChildAt(i);
                    if (child instanceof EditText) {
                        EditText editText = (EditText) child;
                        String value = editText.getText().toString();
                        list.add(value);
                    }
                }
                clientViewModel.update(list.get(1), list.get(0), list.get(2), titleEditText.getText().toString(), catalog.id);
            }
        });
    }
    public void clearInput() {
        for (EditText input : inputs) {
            input.setText("");
            input.clearFocus();
        }
    }
    public void addClient() {
        for (EditText input : inputs) {
            if (input.getText().toString().isEmpty()) {
                input.setError("Wymagane pole");
                Toast.makeText(ClientsActivity.this, input.getHint().toString() + " nie jest podany/e", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        runOnUiThread(() -> {
            Client newClient = new Client(clientStreet.getText().toString(), clientCity.getText().toString(), clientPostalCode.getText().toString(), clientName.getText().toString(), catalogId);
            clientViewModel.insert(newClient);
            clearInput();
        });
    }
}


