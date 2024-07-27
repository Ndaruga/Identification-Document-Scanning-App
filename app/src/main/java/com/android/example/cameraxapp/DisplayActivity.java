package com.android.example.cameraxapp;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class DisplayActivity extends AppCompatActivity {
    private TopBar topBar;
    private BottomBar bottomBar;

    private TextView textNameView, textDocNoView, textPhoneNoView, selectedDocumentView, selectedCountryView;
    private static final int REQUEST_PHONE_VERIFICATION = 1;

    private ProgressBar progressBar;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

//        Get the intent
        AtomicReference<Intent> intent = new AtomicReference<>(getIntent());


        //          Set the top bar Items
        topBar = findViewById(R.id.top_bar);
        topBar.setTitle("Visitor Details");
        topBar.setBackIconClickListener(view -> {
            intent.set(new Intent(DisplayActivity.this, SelectDocumentActivity.class));
            startActivity(intent.get());
            finish();
        });
        topBar.setMenuIconClickListener(view -> {
            Toast.makeText(this, "Menu Icon clicked", Toast.LENGTH_SHORT).show();
        });

//        Set the bottom bar items
        bottomBar = findViewById(R.id.bottom_bar);


      // Initialize views

        progressBar = findViewById(R.id.Progressbar);
        progressBar.setVisibility(View.GONE);

        textNameView = findViewById(R.id.name_text);
        textDocNoView = findViewById(R.id.doc_no_text);
        textPhoneNoView = findViewById(R.id.phone_number_text);
        selectedDocumentView = findViewById(R.id.selected_document_text);
        selectedCountryView = findViewById(R.id.selected_country_text);

//        Buttons
        Button buttonBack = findViewById(R.id.button_retake);
        ImageView iconBack = findViewById(R.id.back_icon);
        Button buttonSave = findViewById(R.id.save_data);

//  Check firing intent activity

        String source = intent.get().getStringExtra("source");
        if (source != null){
            switch (source) {
                case "CameraActivity":
                    String extractedText = intent.get().getStringExtra("extractedText"); // Get the extracted text passed from CameraActivity
                    String documentType = intent.get().getStringExtra("selectedDocument");
                    String country = intent.get().getStringExtra("selectedCountry");
                    String phoneNumber = intent.get().getStringExtra("phoneNo");

                    selectedDocumentView.setText(documentType);
                    selectedCountryView.setText(country);
                    textPhoneNoView.setText(phoneNumber);


                    if (extractedText != null) {
                        String[] lines = extractedText.split("\n");  // Create a new array to store modified strings

                        // remove all white spaces
                        for (int i = 0; i < lines.length; i++) {
                            lines[i] = lines[i].replaceAll("\\s", "");
                        }

                        if ("ID Card".equals(documentType)){
                            getIDCardDetails(lines);
                        } else if ("Passport".equals(documentType)) {
                            getPassportDetails(lines);
                        } else if ("Driving License".equals(documentType)) {
                            Toast.makeText(DisplayActivity.this, "Selected Doc is DL ", Toast.LENGTH_SHORT).show();
                        } else {
                            intent.set(new Intent(DisplayActivity.this, SelectDocumentActivity.class));
                            startActivity(intent.get());
                            finish();
                        }

                    } else {
                        Toast.makeText(this, "No text found. Please Retake image", Toast.LENGTH_SHORT).show();
                        // Handle case where no text is passed
                        finish();
                    }

                    break;

                case "VerifyPhoneActivity":
                    String countryname = intent.get().getStringExtra("selectedCountry");
                    String phoneNo = intent.get().getStringExtra("phoneNo");

                    selectedDocumentView.setText("No Document");
                    selectedCountryView.setText(countryname);
                    textPhoneNoView.setText(phoneNo);

                    break;
            }
        }





//        Spinners
        Spinner organizationSpinner = findViewById(R.id.organization_spinner);
        List<String> organizations = Arrays.asList("Organization 1", "Organization 2", "Organization 3");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, organizations);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        organizationSpinner.setAdapter(adapter);

        organizationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedOrganization = parent.getItemAtPosition(position).toString();
                // Handle the selected organization
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle the case where no organization is selected
            }
        });

        Spinner floorSpinner = findViewById(R.id.floor_spinner);
        List<String> floors = Arrays.asList("Ground", "1st", "2nd", "3rd", "4th", "5th");

        ArrayAdapter<String> floorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, floors);
        floorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        floorSpinner.setAdapter(floorAdapter);

        floorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedFloor = parent.getItemAtPosition(position).toString();
                // Handle the selected floor
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle the case where no floor is selected
            }
        });

        // Set onClickListener for back buttons
        buttonBack.setOnClickListener(v -> {
            intent.set(new Intent(DisplayActivity.this, SelectDocumentActivity.class));
            startActivity(intent.get());
            finish();
        });

        iconBack.setOnClickListener(v -> {
            intent.set(new Intent(DisplayActivity.this, SelectDocumentActivity.class));
            startActivity(intent.get());
            finish();
        });

//        Save data to database
        buttonSave.setOnClickListener(v -> {
//            TODO
//            get the current time (sign in)


        });
    }

    @SuppressLint("SetTextI18n")
    private void getPassportDetails(String[] text) {
//        Get Passport name
        String nameField = text[text.length - 2].substring(5); //Get second last line
        String[] nameArr = nameField.split("<");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < nameArr.length; i++) {
            if (nameArr[i].length() > 2) {
                sb.append(nameArr[i]).append(" ");
            }
            if (i == 3) {
                break;
            }
        }

        textNameView.setText(sb.toString().trim());
        sb.setLength(0);

        // Get the Passport number from the last Line
        String passNoField = text[text.length - 1].substring(0,8);
        textDocNoView.setText(passNoField);

//        Toast.makeText(DisplayActivity.this, "Finished", Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("SetTextI18n")
    private void getIDCardDetails(String[] lines) {
//        Get ID Card name
        String nameField = lines[lines.length - 1]; //Get last line
        String[] nameArr = nameField.split("<");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < nameArr.length; i++) {
            if (nameArr[i].length() > 2) {
                sb.append(nameArr[i]).append(" ");
            }
            if (i == 3) {
                break;
            }
        }
        textNameView.setText(sb.toString().trim()); // Last line in the scan
        sb.setLength(0);

//        Get ID Card Number
        String iDNoLine = lines[lines.length - 2];
        String reversedText = reverseString(iDNoLine);
        String reversedIdNo = reversedText.substring(4, 13);
        String dummyIdNo = reverseString(reversedIdNo);

        // Check if dummyIdNo starts with '0' or 'O' or 'o'
        if (dummyIdNo.startsWith("0") || dummyIdNo.startsWith("O") || dummyIdNo.startsWith("o")) {
            // Discard the first character
            textDocNoView.setText(dummyIdNo.substring(1));
        } else{
            textDocNoView.setText(dummyIdNo);
        }
    }

    public static String reverseString(String text) {
        return new StringBuilder(text).reverse().toString();
    }
}
