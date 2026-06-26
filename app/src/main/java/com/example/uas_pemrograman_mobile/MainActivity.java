package com.example.uas_pemrograman_mobile;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {
    EditText etEmail, etPassword;
    Button btnLogin;
    TextView tvToRegister;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Cek Sesi Login Lokal
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);

        if (isLoggedIn) {
            lanjutKeBeranda();
            return;
        }

        setContentView(R.layout.activity_main);

        // Inisialisasi Firebase
        // Ganti URL jika database Anda berada di region Singapore/Lainnya
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Minta Izin Notifikasi (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        etEmail = findViewById(R.id.etEmailLogin);
        etPassword = findViewById(R.id.etPasswordLogin);
        btnLogin = findViewById(R.id.btnLogin);
        tvToRegister = findViewById(R.id.tvToRegister);

        btnLogin.setOnClickListener(v -> {
            if (validateInputs()) {
                loginUserFirebase();
            }
        });

        tvToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MainActivity2.class);
            startActivity(intent);
        });
    }

    private void loginUserFirebase() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String userId = email.replace(".", ",");

        btnLogin.setEnabled(false);
        btnLogin.setText("Memeriksa...");

        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Masuk");

                if (snapshot.exists()) {
                    // Ambil password dari database
                    String dbPassword = snapshot.child("password").getValue(String.class);

                    if (password.equals(dbPassword)) {
                        // Password Cocok! Simpan Sesi
                        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("user_email", email);
                        editor.putBoolean("is_logged_in", true);

                        // Cek apakah biodata sudah pernah diisi sebelumnya di Firebase
                        boolean biodataFilled = snapshot.hasChild("tinggi") || snapshot.hasChild("usia");
                        editor.putBoolean("is_biodata_filled", biodataFilled);
                        editor.apply();

                        Toast.makeText(MainActivity.this, "Login Berhasil!", Toast.LENGTH_SHORT).show();
                        lanjutKeBeranda();
                    } else {
                        Toast.makeText(MainActivity.this, "Password Salah!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Akun tidak ditemukan. Silakan daftar.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Masuk");
                Toast.makeText(MainActivity.this, "Gagal terhubung ke Firebase: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void lanjutKeBeranda() {
        WaterReminderHelper.scheduleWaterReminder(this);
        ReminderReceiver.scheduleDailyReminder(this);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        if (prefs.getBoolean("is_biodata_filled", false)) {
            startActivity(new Intent(MainActivity.this, MainActivity8.class));
        } else {
            startActivity(new Intent(MainActivity.this, MainActivity4.class));
        }
        finish();
    }

    private boolean validateInputs() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email tidak valid");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password wajib diisi");
            return false;
        }
        return true;
    }
}
