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
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.example.fablab.MainActivity;
import com.example.fablab.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewEquipmentFragment extends Fragment {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 2;
    private static final int REQUEST_CAMERA = 101;

    Spinner spinnerType, spinnerUnit;
    private String selectedType, selectedUnit;
    private View view;
    private Button btnsubmit, btnUploadImage;
    private ImageButton infoCode, infoStock, infoIntegerLimit;
    private EditText editcode, editname, editamount, editcrit, editmin, editmax, editdescr, editizcode;
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
        editizcode = view.findViewById(R.id.edit_integer_limit);

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

        // Initialize Buttons
        infoCode = view.findViewById(R.id.info_code);
        infoStock = view.findViewById(R.id.info_stock);
        infoIntegerLimit = view.findViewById(R.id.info_izg_code);

        // Set up Click Listeners
        infoCode.setOnClickListener(v -> showInfoDialog("code"));
        infoStock.setOnClickListener(v -> showInfoDialog("stock"));
        infoIntegerLimit.setOnClickListener(v -> showInfoDialog("integer_limit"));

        // Set listeners for text changes in EditText fields to enable/disable the upload image button accordingly
        EditText[] editTexts = {editcode, editname, editamount, editdescr, editcrit, editmax, editmin, editizcode};
        for (EditText editText : editTexts) {
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

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
            String izg_code = String.valueOf(editizcode.getText());

            // Check for null values
            if (TextUtils.isEmpty(code) || TextUtils.isEmpty(name) || TextUtils.isEmpty(selectedType) ||
                    TextUtils.isEmpty(selectedUnit) || TextUtils.isEmpty(skaits) || TextUtils.isEmpty(desc) ||
                    TextUtils.isEmpty(crit) || TextUtils.isEmpty(min) || TextUtils.isEmpty(max) || TextUtils.isEmpty(izg_code)) {
                Toast.makeText(getContext(), getString(R.string.empty_fields_error), Toast.LENGTH_SHORT).show();
                Log.d("MainActivity", "One or more fields are empty");

                if (TextUtils.isEmpty(code)) {
                    editcode.setError(getString(R.string.missing_code));
                    deleteCurrentImage();
                }

                if (TextUtils.isEmpty(name)) {
                    editname.setError(getString(R.string.missing_name));
                    deleteCurrentImage();
                }

                if (TextUtils.isEmpty(skaits)) {
                    editamount.setError(getString(R.string.missing_amount));
                    deleteCurrentImage();
                }

                if (TextUtils.isEmpty(desc)) {
                    editdescr.setError(getString(R.string.missing_desc));
                }

                if (TextUtils.isEmpty(crit)) {
                    editcrit.setError(getString(R.string.missing_crit));
                }

                if (TextUtils.isEmpty(min)) {
                    editmin.setError(getString(R.string.missing_min));
                }

                if (TextUtils.isEmpty(max)) {
                    editmax.setError(getString(R.string.missing_max));
                }

                if (TextUtils.isEmpty(izg_code)) {
                    editizcode.setError(getString(R.string.missing_izgcode));
                }

            } else {
                //Checks for negative numbers
                if (skaits.contains("-")) {
                    editamount.setError(getString(R.string.error_negativenr));
                    editamount.requestFocus();
                } else {
                    String telpa, stacija;
                    // Convert strings to integers for comparison
                    int critValue = Integer.parseInt(crit);
                    int minValue = Integer.parseInt(min);
                    int maxValue = Integer.parseInt(max);
                    int skaitsint = Integer.parseInt(skaits);
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
                        if (critValue > minValue) {
                            editcrit.setError(getString(R.string.crit_lower_min));
                            editcrit.requestFocus();
                        }

                        if (minValue > maxValue) {
                            editmin.setError(getString(R.string.min_lower_max));
                            editmin.requestFocus();
                        }

                        //--------------------------checks the list with the code--------------------------------------
                        if (Type.equals("1") && selectedType.equals("Consumables")) {
                            editcode.setError(getString(R.string.invalid_code_type));
                            editcode.requestFocus();
                            // Delete the currently stored picture
                            deleteCurrentImage();
                        } else if (Type.equals("2") && selectedType.equals("Asset")) {
                            editcode.setError(getString(R.string.invalid_code_type));
                            editcode.requestFocus();
                            // Delete the currently stored picture
                            deleteCurrentImage();
                        } else {

                            if (izg_code.length() != 8) {
                                editizcode.setError(getString(R.string.unfinished_code));
                                editizcode.requestFocus();
                            } else {
                                // Check if an image has been captured
                                if (!isImageCaptured) {
                                    Toast.makeText(getContext(), getString(R.string.capture_image), Toast.LENGTH_SHORT).show();
                                    return; // Exit the method if no image is captured
                                }

                                //Realtime database
                                DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference();
                                whatStat(stacija, stationName -> {
                                    if (!stationName.isEmpty()) {
                                        // Use stationName here once it's fetched from Firebase
                                        Log.d("MainActivity", "Station: " + stationName);

                                        DatabaseReference equipsRef = databaseRef.child("equipment").child(kods);
                                        DatabaseReference stationRef = FirebaseDatabase.getInstance().getReference()
                                                .child("stations").child(stationName).child("Equipment").child(kods);

                                        // Construct the reference to the image in Firebase Storage
                                        String imagePath = "gs://aaaaa-3cb94.appspot.com/Equipment_Icons/" + kods + ".png";
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
                                            Stockequips.put("Skaits", skaitsint);
                                            Stockequips.put("Max Stock", maxValue);
                                            Stockequips.put("Min Stock", minValue);
                                            Stockequips.put("Critical Stock", critValue);
                                            Stockequips.put("Description", desc);
                                            Stockequips.put("IzgKods", izg_code);
                                            Stockequips.put("Attēls", imageUrl);

                                            equipsRef.setValue(Stockequips);     //sends to the stock equipment

                                            //--------------------------------------------------//
                                            equips.put("Telpa", telpa);
                                            equips.put("Stacija", stacija);
                                            equips.put("Tips", selectedType);
                                            equips.put("Kods", kods);
                                            equips.put("Nosaukums", name);
                                            equips.put("Mērvienība", selectedUnit);
                                            equips.put("Skaits", skaitsint);
                                            equips.put("Description", desc);
                                            equips.put("IzgKods", izg_code);
                                            equips.put("Attēls", imageUrl);

                                            stationRef.setValue(equips);    //sends to the station equipment to display
                                            //--------------------------------------------------//

                                            startActivity(new Intent(getContext(), MainActivity.class));
                                            getActivity().finish();

                                            addLogEntry(name, kods);

                                            Log.d("MainActivity", "Added a new equipment");
                                        });
                                    } else {
                                        Log.d("MainActivity", "Station not found");
                                        deleteCurrentImage();
                                    }
                                });


                            }
                        }
                    } else {
                        deleteCurrentImage();
                        Log.d("NewEquipment", "Something went wrong");
                    }
                }
            }

        });
        btnUploadImage.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
            } else {
                takePicture();
            }
        });
        return view;
    }

    private void whatStat(String stacija, StationCallback callback) {
        int check;
        try {
            check = Integer.parseInt(stacija);  // Convert station ID from String to Integer
        } catch (NumberFormatException e) {
            Log.e("help", "Invalid station ID format");
            callback.onStationFound("");  // Return empty string via callback if parsing fails
            return;
        }

        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference().child("stations");

        // Query the database to find the station by ID
        databaseRef.orderByChild("ID").equalTo(check).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot datasnapshot) {
                if (datasnapshot.exists()) {
                    for (DataSnapshot snapshot : datasnapshot.getChildren()) {
                        // Get the station node key (station name)
                        String statname = snapshot.getKey();
                        Log.d("helpP", "Found station: " + statname);
                        callback.onStationFound(statname);  // Pass station name via callback
                        return;
                    }
                }
                callback.onStationFound("");  // If not found, return empty string
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("help", "Error fetching station data: " + error.getMessage());
                callback.onStationFound("");  // Return empty string on error
            }
        });
    }

    public interface StationCallback {
        void onStationFound(String stationName);
    }

    private void showInfoDialog(String infoType) {
        // Create and show the dialog based on the infoType
        DialogFragment dialog = InfoDialogFragment.newInstance(infoType);
        dialog.show(getChildFragmentManager(), "infoDialog");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // CAMERA permission is granted, proceed with image capture
                takePicture();
            } else {
                // CAMERA permission is denied, show a message or take appropriate action
                Toast.makeText(requireContext(), getString(R.string.lack_permission_camera), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean checkCode(String kods) {
        //Allows spaces aswell
        String patternString = "^[1-9][0-9]?_[1-9]_[1-2]?_[a-zA-Z0-9\\s]+$"; //10_5_1_test1234
        String patternLonger = "^[1-9][0-9]?_[1-9][0-9]?_[1-2]_[a-zA-Z0-9\\s]+$"; //10_15_2_test4321
        String pattern = "^[1-9]_[1-9][0-9]?_[1-2]_[a-zA-Z0-9\\s]+$"; //1_15_1_test2135
        String smallpattern = "^[1-9]_[1-9]_[1-2]_[a-zA-Z0-9\\s]+$"; //5_3_1_test4124


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
            editcode.setError(getString(R.string.invalid_Code_pattern));
            editcode.requestFocus();
            Log.d("MainActivity", "No pattern matched");
            // Delete the currently stored picture
            deleteCurrentImage();
        }
        return matches;
    }

    private void takePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
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

    private void deleteCurrentImage() {
        // Construct the StorageReference for the current picture
        String imageName = editcode.getText().toString().toUpperCase() + ".png";
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("Equipment_Icons/" + imageName);

        // Delete the picture from Firebase Storage
        storageRef.delete().addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), getString(R.string.image_deleted_good), Toast.LENGTH_SHORT).show();
            TextView textViewFileName = view.findViewById(R.id.textViewFileName);
            textViewFileName.setText(getString(R.string.no_file_selected)); // Set the file name in the TextView
        }).addOnFailureListener(exception -> {
            Toast.makeText(getContext(), getString(R.string.image_deleted_bad) + exception.getMessage(), Toast.LENGTH_SHORT).show();
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
        Bitmap bitmap;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), getString(R.string.failed_convert_picture), Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(getContext(), getString(R.string.image_upload_good), Toast.LENGTH_SHORT).show();
                    TextView textViewFileName = view.findViewById(R.id.textViewFileName);
                    textViewFileName.setText(imageName); // Set the file name in the TextView
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), getString(R.string.image_upload_failed) + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Delete the currently stored image if the upload fails
                    deleteCurrentImage();
                });
    }

    private void addLogEntry(String name, String code) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        String email = currentUser.getEmail();

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Log.e("AddLogEntry", "User data not found.");
                    return;
                }

                String fullName = dataSnapshot.child("Vards un uzvards").getValue(String.class);

                // Format current date and time
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
                String dateTime = sdf.format(new Date());

                String title = "Izveidojis jaunu iekārti " + dateTime;

                String summary = fullName + " izveidoja jaunu iekārti ar nosakumu '" + name + "' kuram piešķir šāds kodus '" + code + "'.";

                DatabaseReference logRef = FirebaseDatabase.getInstance().getReference()
                        .child("Logs").child(dateTime);

                Map<String, Object> logEntry = new HashMap<>();
                logEntry.put("user", fullName);
                logEntry.put("email", email);
                logEntry.put("title", title);
                logEntry.put("summary", summary);

                logRef.setValue(logEntry)
                        .addOnSuccessListener(aVoid -> Log.d("AddLogEntry", "Log entry added successfully"))
                        .addOnFailureListener(e -> Log.e("AddLogEntry", "Error adding log entry", e));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("AddLogEntry", "Database error: " + databaseError.getMessage());
            }
        });
    }
}