package com.example.uas_pemrograman_mobile;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity3 extends AppCompatActivity {

    private TextView tvUserEmail, tvDispHeight, tvDispWeight;
    private Button btnLogout;
    private MaterialButton btnEditProfile;
    private ImageView ivProfilePic;
    private FloatingActionButton btnAddPhoto;
    private BottomNavigationView bottomNavigationView;
    private SharedPreferences userPrefs;

    private Uri cameraImageUri;

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    saveAndRefreshProfilePic(cameraImageUri);
                }
            }
    );

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    saveAndRefreshProfilePic(selectedImageUri);
                }
            }
    );

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Izin Kamera diperlukan untuk mengambil foto", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);

        userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        initViews();
        displayUserBiodata();
        loadProfilePicture();
        setupBottomNavigation();

        btnLogout.setOnClickListener(v -> {
            SharedPreferences.Editor editor = userPrefs.edit();
            editor.putBoolean("is_logged_in", false);
            editor.apply();

            Toast.makeText(this, "Berhasil Keluar", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity3.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity3.this, MainActivity4.class);
            startActivity(intent);
        });

        // HANYA tombol + yang berfungsi untuk ganti foto
        btnAddPhoto.setOnClickListener(v -> showImageSourceDialog());

        // Fitur klik foto profil untuk melihat gambar full (seperti WA)
        ivProfilePic.setOnClickListener(v -> showFullImageDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        displayUserBiodata();
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    private void initViews() {
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvDispHeight = findViewById(R.id.tvDispHeight);
        tvDispWeight = findViewById(R.id.tvDispWeight);
        btnLogout = findViewById(R.id.btnLogout);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        ivProfilePic = findViewById(R.id.ivProfilePic);
        btnAddPhoto = findViewById(R.id.btnAddPhoto);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }

    private void showFullImageDialog() {
        String path = userPrefs.getString("profile_pic_path", null);
        if (path == null) {
            Toast.makeText(this, "Belum ada foto profil", Toast.LENGTH_SHORT).show();
            return;
        }

        File imgFile = new File(path);
        if (!imgFile.exists()) {
            Toast.makeText(this, "File foto tidak ditemukan", Toast.LENGTH_SHORT).show();
            return;
        }

        // Dialog full screen untuk melihat foto
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_full_image);

        ImageView ivFullImage = dialog.findViewById(R.id.ivFullImage);
        ImageButton btnClose = dialog.findViewById(R.id.btnCloseFullImage);

        Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        ivFullImage.setImageBitmap(myBitmap);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showImageSourceDialog() {
        String[] options = {"Kamera", "Galeri"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pilih Sumber Foto");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                checkCameraPermissionAndOpen();
            } else {
                openGallery();
            }
        });
        builder.show();
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            Toast.makeText(this, "Gagal membuat file gambar", Toast.LENGTH_SHORT).show();
        }

        if (photoFile != null) {
            cameraImageUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            cameraLauncher.launch(takePictureIntent);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void saveAndRefreshProfilePic(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            if (bitmap != null) {
                // Simpan bitmap ke internal storage agar persisten
                File profilePicFile = new File(getFilesDir(), "profile_pic.jpg");
                FileOutputStream fos = new FileOutputStream(profilePicFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.close();

                userPrefs.edit().putString("profile_pic_path", profilePicFile.getAbsolutePath()).apply();
                ivProfilePic.setImageBitmap(bitmap);
                Toast.makeText(this, "Foto Profil Diperbarui", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Gagal menyimpan foto", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProfilePicture() {
        String path = userPrefs.getString("profile_pic_path", null);
        if (path != null) {
            File imgFile = new File(path);
            if (imgFile.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                ivProfilePic.setImageBitmap(myBitmap);
            }
        }
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_beranda) {
                startActivity(new Intent(this, MainActivity8.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_water) {
                startActivity(new Intent(this, MainActivity7.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_calendar) {
                startActivity(new Intent(this, MainActivity5.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_history) {
                startActivity(new Intent(this, MainActivity6.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    private void displayUserBiodata() {
        String email = userPrefs.getString("user_email", "-");
        String height = userPrefs.getString("user_height", "-");
        String weight = userPrefs.getString("user_weight", "-");

        tvUserEmail.setText(email);
        tvDispHeight.setText(getString(R.string.height_unit, height));
        tvDispWeight.setText(getString(R.string.weight_unit, weight));
    }
}
