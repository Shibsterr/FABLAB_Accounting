package com.example.fablab.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.fablab.MainActivity;
import com.example.fablab.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewEquipmentFragment extends Fragment {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 2;

    Spinner spinnerType, spinnerUnit;
    private String selectedType, selectedUnit;
    private View view;
    private Button btnsubmit, btnUploadImage;
    private EditText editcode, editname, editamount, editcrit, editmin, editmax, editdescr;

    // Declare a boolean variable to track whether an image has been captured
    private boolean isImageCaptured = false;
    public NewEquipmentFragment() {
        //Empty constructor (REQUIRED)
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_new_equipment, container, false);

        spinnerType = view.findViewById(R.id.spinner_type);
        spinnerUnit = view.findViewById(R.id.spinner_unit);

        editcode = view.findViewById(R.id.edit_code);
        editname = view.findViewById(R.id.edit_name);
        editamount = view.findViewById(R.id.skaits);

        btnsubmit = view.findViewById(R.id.button_submit);
        btnUploadImage = view.findViewById(R.id.button_upload);
        editdescr = view.findViewById(R.id.description);
        editcrit = view.findViewById(R.id.crit_stock);
        editmax = view.findViewById(R.id.max_stock);
        editmin = view.findViewById(R.id.min_stock);

        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(getContext(), R.array.type_list, android.R.layout.simple_spinner_dropdown_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<CharSequence> unitAdapter = ArrayAdapter.createFromResource(getContext(), R.array.unit_list_assets, android.R.layout.simple_spinner_dropdown_item);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<CharSequence> unitAdapterCons = ArrayAdapter.createFromResource(getContext(), R.array.units_list_cons, android.R.layout.simple_spinner_dropdown_item);
        unitAdapterCons.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerType.setAdapter(typeAdapter);

        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    spinnerUnit.setAdapter(unitAdapter);
                } else if (position == 1) {
                    spinnerUnit.setAdapter(unitAdapterCons);
                }

                spinnerUnit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        selectedUnit = spinnerUnit.getSelectedItem().toString();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // Disable the upload image button initially
        btnUploadImage.setEnabled(false);

        // Set listeners for text changes in EditText fields to enable/disable the upload image button accordingly
        EditText[] editTexts = {editcode, editname, editamount, editdescr, editcrit, editmax, editmin};
        for (EditText editText : editTexts) {
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    // Check if any field is empty
                    boolean anyEmpty = false;
                    for (EditText editText : editTexts) {
                        if (TextUtils.isEmpty(editText.getText())) {
                            anyEmpty = true;
                            break;
                        }
                    }
                    // Enable/disable the upload image button based on whether any field is empty
                    btnUploadImage.setEnabled(!anyEmpty);
                }
            });
        }

        btnsubmit.setOnClickListener(v -> {

            // Proceed with submitting the data
            String code = String.valueOf(editcode.getText());
            String name = String.valueOf(editname.getText());
            selectedType = spinnerType.getSelectedItem().toString();
            selectedUnit = spinnerUnit.getSelectedItem().toString();
            String skaits = String.valueOf(editamount.getText());
            String desc = String.valueOf(editdescr.getText());
            String crit = String.valueOf(editcrit.getText());
            String min = String.valueOf(editmin.getText());
            String max = String.valueOf(editmax.getText());

            // Check for null values
            if (TextUtils.isEmpty(code) || TextUtils.isEmpty(name) || TextUtils.isEmpty(selectedType) ||
                    TextUtils.isEmpty(selectedUnit) || TextUtils.isEmpty(skaits) || TextUtils.isEmpty(desc) ||
                    TextUtils.isEmpty(crit) || TextUtils.isEmpty(min) || TextUtils.isEmpty(max)) {
                Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
                Log.d("MainActivity", "One or more fields are empty");

                if(TextUtils.isEmpty(code)){
                    editcode.setError("Ievadi kodu!");
                    deleteCurrentImage();
                }

                if (TextUtils.isEmpty(name)){
                    editname.setError("Ievadi nosaukumu!");
                    deleteCurrentImage();
                }

                if (TextUtils.isEmpty(skaits)){
                    editamount.setError("Ievadi skaitu!");
                    deleteCurrentImage();
                }

                if(TextUtils.isEmpty(desc)){
                    editdescr.setError("Ievadi aprakastu!");
                }

                if (TextUtils.isEmpty(crit)){
                    editcrit.setError("Aizpildi ar skaitli!");
                }

                if (TextUtils.isEmpty(min)){
                    editmin.setError("Aizpildi ar skaitli!");
                }

                if (TextUtils.isEmpty(max)){
                    editmax.setError("Aizpildi ar skaitli!");
                }

            }else{
                //Checks for negative numbers
                if (skaits.contains("-")) {
                    editamount.setError("Skaits nedrīkst būt negatīvs!");
                    editamount.requestFocus();
                } else {
                        String telpa, stacija;
                    // Convert strings to integers for comparison
                        int critValue = Integer.parseInt(crit);
                        int minValue = Integer.parseInt(min);
                        int maxValue = Integer.parseInt(max);
                    code = code.toUpperCase();
                    String kods = code;

                    Map<String, Object> Stockequips = new HashMap<>();
                    Map<String, Object> equips = new HashMap<>();

                    //If it matches the pattern then continue
                    if (checkCode(kods)) { //true
                        telpa = code.substring(0, code.indexOf("_"));
                        code = code.substring(code.indexOf("_") + 1);
                        stacija = code.substring(0, code.indexOf("_"));
                        String Type;
                        code = code.substring(code.indexOf("_") + 1);
                        Type = code.substring(0, code.indexOf("_"));

                        Log.d("NewEquipmentFragment", Type);
                        // check the stocks
                        if (critValue >= minValue) {
                            editcrit.setError("Crit must be lower than Min");
                            editcrit.requestFocus();
                        }

                        if (minValue >= maxValue) {
                            editmin.setError("Min must be lower than Max");
                            editmin.requestFocus();
                        }

                        //--------------------------checks the list with the code--------------------------------------
                        if (Type.equals("1") && selectedType.equals("Consumables")) {
                            editcode.setError("Nepareizi ievadīts kods kļūda ir ar (koda tipu)!");
                            editcode.requestFocus();
                            // Delete the currently stored picture
                            deleteCurrentImage();
                        } else if (Type.equals("2") && selectedType.equals("Asset")) {
                            editcode.setError("Nepareizi ievadīts kods kļūda ir ar (koda tipu)!");
                            editcode.requestFocus();
                            // Delete the currently stored picture
                            deleteCurrentImage();
                        } else {
                            // Check if an image has been captured
                            if (!isImageCaptured) {
                                Toast.makeText(getContext(), "Please capture an image first", Toast.LENGTH_SHORT).show();
                                return; // Exit the method if no image is captured
                            }

                            //Realtime database
                            DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();
                            DatabaseReference equipsRef = databaseRef.child("equipment").child(kods);
                            DatabaseReference stationRef = databaseRef.child("stations").child(whatStat(stacija)).child("Equipment").child(kods);

                            Log.d("MainActivity", whatStat(stacija));

                            // Construct the reference to the image in Firebase Storage
                            String imagePath = "gs://nomadic-sprite-414512.appspot.com/Equipment_Icons/" + kods + ".png";
                            // Get a reference to the Firebase Storage instance
                            StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imagePath);
                            // Get the download URL for the image
                            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                // Once we have the download URL, we can store it along with other equipment details in the Realtime Database
                                String imageUrl = uri.toString();
                                // Store other equipment details in the Realtime Database
                                Stockequips.put("Telpa", telpa);
                                Stockequips.put("Stacija", stacija);
                                Stockequips.put("Tips", selectedType);
                                Stockequips.put("Kods", kods);
                                Stockequips.put("Nosaukums", name);
                                Stockequips.put("Mērvienība", selectedUnit);
                                Stockequips.put("Skaits", skaits);
                                Stockequips.put("Max Stock", maxValue);
                                Stockequips.put("Min Stock", minValue);
                                Stockequips.put("Critical Stock", critValue);
                                Stockequips.put("Description", desc);
                                Stockequips.put("Attēls", imageUrl);

                                equipsRef.setValue(Stockequips);     //sends to the stock equipment

                                //--------------------------------------------------//
                                equips.put("Telpa", telpa);
                                equips.put("Stacija", stacija);
                                equips.put("Tips", selectedType);
                                equips.put("Kods", kods);
                                equips.put("Nosaukums", name);
                                equips.put("Mērvienība", selectedUnit);
                                equips.put("Skaits", skaits);
                                equips.put("Description", desc);
                                equips.put("Attēls", imageUrl);

                                stationRef.setValue(equips);    //sends to the station equipment to display
                                //--------------------------------------------------//

                                startActivity(new Intent(getContext(), MainActivity.class));
                                getActivity().finish();
                                Log.d("MainActivity", "Added a new equipment");
                            });
                        }
                    }else{
                        deleteCurrentImage();
                        Log.d("NewEquipment","Something went wrong");
                    }
                }
            }

        });

        btnUploadImage.setOnClickListener(v -> {
            // Check if WRITE_EXTERNAL_STORAGE permission is granted
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // Request the permission if not granted
                ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);
            } else {
                // Permission is granted, proceed with image capture
                dispatchTakePictureIntent();
            }
        });

        return view;
    }

    private boolean checkCode(String kods) {
        String patternString = "^[1-9][0-9]?_[1-9]_[1-2]?_[a-zA-Z0-9]+$"; //10_5_1_test1234
        String patternLonger = "^[1-9][0-9]?_[1-9][0-9]?_[1-2]_[a-zA-Z0-9]+$"; //10_15_2_test4321
        String pattern = "^[1-9]_[1-9][0-9]?_[1-2]_[a-zA-Z0-9]+$"; //1_15_1_test2135
        String smallpattern = "^[1-9]_[1-9]_[1-2]_[a-zA-Z0-9]+$"; //5_3_1_test4124

        Pattern pattern1 = Pattern.compile(patternString);
        Pattern pattern2 = Pattern.compile(patternLonger);
        Pattern pattern3 = Pattern.compile(pattern);
        Pattern pattern4 = Pattern.compile(smallpattern);

        Matcher matcher1 = pattern1.matcher(kods);
        Matcher matcher2 = pattern2.matcher(kods);
        Matcher matcher3 = pattern3.matcher(kods);
        Matcher matcher4 = pattern4.matcher(kods);

        boolean matches = false;
        //Matches patterns
        if (matcher1.matches()) {
            matches = true;
            Log.d("MainActivity", "Matched the first pattern");
        } else if (matcher2.matches()) {
            matches = true;
            Log.d("MainActivity", "Matched the second pattern");
        } else if (matcher3.matches()) {
            matches = true;
            Log.d("MainActivity", "Matched the third pattern");
        } else if (matcher4.matches()) {
            matches = true;
            Log.d("MainActivity", "Matched the fourth pattern");
        } else {
            editcode.setError("Nepareizi ievadīts kods!");
            editcode.requestFocus();
            Log.d("MainActivity", "No pattern matched");
            // Delete the currently stored picture
            deleteCurrentImage();
        }
        return matches;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            Toast.makeText(getContext(), "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == getActivity().RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            // Convert Bitmap to Uri
            Uri uri = getImageUri(requireContext(), imageBitmap);
            String imageName = editcode.getText().toString().toUpperCase() + ".png"; // Or any other image format
            uploadImageToStorage(uri, imageName);

            // Update the flag to indicate that an image has been captured
            isImageCaptured = true;
        }
    }

    // Method to delete the currently stored picture
    private void deleteCurrentImage() {
        // Construct the StorageReference for the current picture
        String imageName = editcode.getText().toString().toUpperCase() + ".png";
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("Equipment_Icons/" + imageName);

        // Delete the picture from Firebase Storage
        storageRef.delete().addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Current image deleted successfully", Toast.LENGTH_SHORT).show();
            TextView textViewFileName = view.findViewById(R.id.textViewFileName);
            textViewFileName.setText("No file selected"); // Set the file name in the TextView
        }).addOnFailureListener(exception -> {
            Toast.makeText(getContext(), "Failed to delete current image: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
    private Uri getImageUri(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "Title", null);
        return Uri.parse(path);
    }
    private void uploadImageToStorage(Uri uri, String imageName) {
        // Convert the image to PNG format
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Failed to convert image to PNG format", Toast.LENGTH_SHORT).show();
            return;
        }

        // Compress the bitmap to PNG format with 100% quality
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] data = byteArrayOutputStream.toByteArray();

        // Construct the StorageReference for the PNG image
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("Equipment_Icons/" + imageName);

        // Upload the PNG image to Firebase Storage
        storageRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> {
                    Toast.makeText(getContext(), "Image uploaded successfully", Toast.LENGTH_SHORT).show();
                    TextView textViewFileName = view.findViewById(R.id.textViewFileName);
                    textViewFileName.setText(imageName); // Set the file name in the TextView
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Delete the currently stored image if the upload fails
                    deleteCurrentImage();
                });
    }
    private String whatStat(String stacija) {
        String whatStation = "";

        if (stacija.equals("1")) {
            whatStation = "Heatpress_station";

        } else if (stacija.equals("2")) {
            whatStation = "assembly_station";

        } else if (stacija.equals("3")) {
            whatStation = "cad_station";

        } else if (stacija.equals("4")) {
            whatStation = "casting_station";

        } else if (stacija.equals("5")) {
            whatStation = "cnc_station";

        } else if (stacija.equals("6")) {
            whatStation = "compost_station";

        } else if (stacija.equals("7")) {
            whatStation = "electro_station";

        } else if (stacija.equals("8")) {
            whatStation = "electronic_station";

        } else if (stacija.equals("9")) {
            whatStation = "embroidery_station";

        } else if (stacija.equals("10")) {
            whatStation = "laser_station";

        } else if (stacija.equals("11")) {
            whatStation = "metalworking_station";

        } else if (stacija.equals("12")) {
            whatStation = "microscope_station";

        } else if (stacija.equals("13")) {
            whatStation = "printer_3d";

        } else if (stacija.equals("14")) {
            whatStation = "vinyl_station";

        }
        return whatStation;
    }
}