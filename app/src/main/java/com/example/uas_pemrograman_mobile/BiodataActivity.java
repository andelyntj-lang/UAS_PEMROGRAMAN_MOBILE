package com.example.uas_pemrograman_mobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BiodataActivity extends AppCompatActivity {

    private EditText etUsia, etTinggi, etBerat;
    private AutoCompleteTextView etAktivitas;
    private RadioGroup rgGender;
    private Button btnSimpan;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_biodata);

        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        initViews();
        setupDropdown();

        btnSimpan.setOnClickListener(v -> {
            if (validateInputs()) {
                saveBiodata();
                Toast.makeText(this, "Biodata Berhasil Disimpan!", Toast.LENGTH_SHORT).show();

                // Pindah ke MainActivity8 (Beranda) - Sesuai instruksi sebelumnya untuk masuk ke Beranda
                Intent intent = new Intent(BiodataActivity.this, MainActivity8.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void initViews() {
        etUsia = findViewById(R.id.etUsia);
        etTinggi = findViewById(R.id.etTinggi);
        etBerat = findViewById(R.id.etBerat);
        etAktivitas = findViewById(R.id.etAktivitas);
        rgGender = findViewById(R.id.rgGender);
        btnSimpan = findViewById(R.id.btnSimpan);
    }

    private void setupDropdown() {
        String[] items = {"Jalan santai", "Lari maraton", "Berenang", "Naik gunung", "Bersepeda", "Tidak ada aktivitas"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, items);
        etAktivitas.setAdapter(adapter);
    }

    private boolean validateInputs() {
        if (TextUtils.isEmpty(etUsia.getText().toString())) {
            etUsia.setError("Usia wajib diisi");
            return false;
        }
        if (rgGender.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Pilih jenis kelamin", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(etTinggi.getText().toString())) {
            etTinggi.setError("Tinggi badan wajib diisi");
            return false;
        }
        if (TextUtils.isEmpty(etBerat.getText().toString())) {
            etBerat.setError("Berat badan wajib diisi");
            return false;
        }
        if (TextUtils.isEmpty(etAktivitas.getText().toString())) {
            etAktivitas.setError("Pilih aktivitas");
            return false;
        }
        return true;
    }

    private void saveBiodata() {
        String usiaStr = etUsia.getText().toString();
        String tinggi = etTinggi.getText().toString();
        String beratStr = etBerat.getText().toString();
        String aktivitas = etAktivitas.getText().toString();

        int selectedGenderId = rgGender.getCheckedRadioButtonId();
        RadioButton rbSelected = findViewById(selectedGenderId);
        String gender = rbSelected.getText().toString();

        // LOGIKA PERHITUNGAN BERDASARKAN GAMBAR
        int usia = Integer.parseInt(usiaStr);
        double berat = Double.parseDouble(beratStr);
        int baseTarget = 2000; // Default awal

        if (usia >= 1 && usia <= 3) {
            baseTarget = 1300;
        } else if (usia >= 4 && usia <= 8) {
            baseTarget = 1700;
        } else if (usia >= 9 && usia <= 13) {
            baseTarget = (gender.toLowerCase().contains("laki") || gender.toLowerCase().contains("pria")) ? 2400 : 2100;
        } else if (usia >= 14 && usia <= 18) {
            baseTarget = (gender.toLowerCase().contains("laki") || gender.toLowerCase().contains("pria")) ? 3300 : 2300;
        } else {
            // Dewasa: Berdasarkan berat badan (32.5 ml * berat badan)
            baseTarget = (int) (32.5 * berat);
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("user_age", usiaStr);
        editor.putString("user_gender", gender);
        editor.putString("user_height", tinggi);
        editor.putString("user_weight", beratStr);
        editor.putString("user_activity", aktivitas);
        editor.putInt("base_water_target", baseTarget); // Simpan target dasar hasil hitung
        editor.putBoolean("is_biodata_filled", true);
        editor.apply();

        // Update target hari ini di DailyActivityLogs
        SharedPreferences dailyPrefs = getSharedPreferences("DailyActivityLogs", MODE_PRIVATE);
        String todayDateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        dailyPrefs.edit().putString(todayDateKey + "_water_target", String.valueOf(baseTarget)).apply();
    }
}
