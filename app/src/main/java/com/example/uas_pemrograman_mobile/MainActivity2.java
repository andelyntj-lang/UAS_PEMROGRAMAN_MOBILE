package com.example.uas_pemrograman_mobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class MainActivity2 extends AppCompatActivity {
    EditText etName, etEmail, etPass, etConfirm;
    Button btnRegister;
    TextView tvToLogin;

    // Tambahkan variabel DatabaseReference
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main2);

        // Inisialisasi Firebase Database
        // Jika masih gagal konek, ganti baris di bawah dengan:
        // mDatabase = FirebaseDatabase.getInstance("URL_DATABASE_ANDA_DISINI").getReference();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etName = findViewById(R.id.etNamaLengkap);
        etEmail = findViewById(R.id.etEmailSignUp);
        etPass = findViewById(R.id.etPasswordSignUp);
        etConfirm = findViewById(R.id.etKonfirmasiPassword);
        btnRegister = findViewById(R.id.btnDaftar);
        tvToLogin = findViewById(R.id.tvToLogin);

        btnRegister.setOnClickListener(view -> {
            if (validateForm()) {
                saveUserToFirebase();
            }
        });

        tvToLogin.setOnClickListener(v -> finish());
    }

    private void saveUserToFirebase() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass = etPass.getText().toString().trim();

        // Firebase tidak mengizinkan tanda titik (.) dalam ID, kita ganti dengan koma (,)
        String userId = email.replace(".", ",");

        // Membuat objek data user
        Map<String, Object> user = new HashMap<>();
        user.put("nama", name);
        user.put("email", email);
        user.put("password", pass);

        // Menonaktifkan tombol agar tidak diklik berkali-kali
        btnRegister.setEnabled(false);

        mDatabase.child("users").child(userId).setValue(user)
                .addOnSuccessListener(aVoid -> {
                    // Berhasil kirim ke Firebase, lalu simpan ke lokal
                    SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("user_email", email);
                    editor.putBoolean("is_logged_in", true);
                    editor.apply();

                    ReminderReceiver.scheduleDailyReminder(this);
                    Toast.makeText(this, "Berhasil Daftar di Firebase!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(MainActivity2.this, MainActivity4.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    Toast.makeText(this, "Koneksi Gagal: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private boolean validateForm() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass = etPass.getText().toString().trim();
        String confirm = etConfirm.getText().toString().trim();

        if (TextUtils.isEmpty(name)) { etName.setError("Wajib diisi"); return false; }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email tidak valid"); return false;
        }
        if (pass.length() < 6) { etPass.setError("Minimal 6 karakter"); return false; }
        if (!pass.equals(confirm)) { etConfirm.setError("Password tidak cocok"); return false; }
        return true;
    }
}
