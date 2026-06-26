package com.example.uas_pemrograman_mobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity4 extends AppCompatActivity {

    private EditText etEditUsia, etEditTinggi, etEditBerat;
    private RadioGroup rgEditGender;
    private RadioButton rbEditLaki, rbEditPerempuan;
    private Button btnUpdate, btnBatal;
    private SharedPreferences userPrefs;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main4);

        // Inisialisasi Firebase Database
        mDatabase = FirebaseDatabase.getInstance().getReference();

        userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        initViews();
        loadCurrentData();

        btnUpdate.setOnClickListener(v -> {
            if (validateInputs()) {
                saveUpdatedData();
            }
        });

        btnBatal.setOnClickListener(v -> finish());
    }

    private void initViews() {
        etEditUsia = findViewById(R.id.etEditUsia);
        etEditTinggi = findViewById(R.id.etEditTinggi);
        etEditBerat = findViewById(R.id.etEditBerat);
        rgEditGender = findViewById(R.id.rgEditGender);
        rbEditLaki = findViewById(R.id.rbEditLaki);
        rbEditPerempuan = findViewById(R.id.rbEditPerempuan);
        btnUpdate = findViewById(R.id.btnUpdate);
        btnBatal = findViewById(R.id.btnBatal);
    }

    private void loadCurrentData() {
        etEditUsia.setText(userPrefs.getString("user_age", ""));
        etEditTinggi.setText(userPrefs.getString("user_height", ""));
        etEditBerat.setText(userPrefs.getString("user_weight", ""));

        String gender = userPrefs.getString("user_gender", "");
        if ("Laki-laki".equals(gender)) {
            rbEditLaki.setChecked(true);
        } else if ("Perempuan".equals(gender)) {
            rbEditPerempuan.setChecked(true);
        }
    }

    private boolean validateInputs() {
        if (TextUtils.isEmpty(etEditUsia.getText().toString())) {
            etEditUsia.setError("Usia wajib diisi");
            return false;
        }
        if (rgEditGender.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Pilih jenis kelamin", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(etEditTinggi.getText().toString())) {
            etEditTinggi.setError("Tinggi badan wajib diisi");
            return false;
        }
        if (TextUtils.isEmpty(etEditBerat.getText().toString())) {
            etEditBerat.setError("Berat badan wajib diisi");
            return false;
        }
        return true;
    }

    private void saveUpdatedData() {
        String usiaStr = etEditUsia.getText().toString();
        String tinggi = etEditTinggi.getText().toString();
        String beratStr = etEditBerat.getText().toString();
        String email = userPrefs.getString("user_email", "unknown");

        int selectedGenderId = rgEditGender.getCheckedRadioButtonId();
        RadioButton rbSelected = findViewById(selectedGenderId);
        String gender = rbSelected.getText().toString();

        int usia = Integer.parseInt(usiaStr);
        double berat = Double.parseDouble(beratStr);
        int baseTarget = (int) (32.5 * berat);

        if (usia >= 1 && usia <= 3) baseTarget = 1300;
        else if (usia >= 4 && usia <= 8) baseTarget = 1700;
        else if (usia >= 9 && usia <= 13) {
            baseTarget = (gender.toLowerCase().contains("laki")) ? 2400 : 2100;
        } else if (usia >= 14 && usia <= 18) {
            baseTarget = (gender.toLowerCase().contains("laki")) ? 3300 : 2300;
        }

        // 1. Simpan ke Lokal (SharedPreferences)
        SharedPreferences.Editor editor = userPrefs.edit();
        editor.putString("user_age", usiaStr);
        editor.putString("user_gender", gender);
        editor.putString("user_height", tinggi);
        editor.putString("user_weight", beratStr);
        editor.putInt("base_water_target", baseTarget);
        editor.putBoolean("is_biodata_filled", true);
        editor.apply();

        // 2. Simpan ke Firebase Realtime Database
        // Gunakan email (diganti titiknya) sebagai kunci unik
        String userId = email.replace(".", ",");
        Map<String, Object> userUpdate = new HashMap<>();
        userUpdate.put("usia", usiaStr);
        userUpdate.put("gender", gender);
        userUpdate.put("tinggi", tinggi);
        userUpdate.put("berat", beratStr);
        userUpdate.put("targetAir", baseTarget);

        mDatabase.child("users").child(userId).updateChildren(userUpdate)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MainActivity4.this, "Data Berhasil Disinkronkan ke Cloud!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity4.this, MainActivity8.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity4.this, "Gagal Sinkron: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Tetap lanjut ke dashboard meski firebase gagal (opsional)
                    startActivity(new Intent(MainActivity4.this, MainActivity8.class));
                    finish();
                });

        // Update target harian lokal
        SharedPreferences dailyPrefs = getSharedPreferences("DailyActivityLogs", MODE_PRIVATE);
        String todayDateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        dailyPrefs.edit().putString(todayDateKey + "_water_target", String.valueOf(baseTarget)).apply();
    }
}
