package com.example.uas_pemrograman_mobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;
import java.util.Locale;

public class MainActivity5 extends AppCompatActivity {

    private CalendarView calendarView;
    private TextView tvSummaryDate, tvSummaryWater, tvSummaryActivity;
    private CardView cvDailySummary;
    private BottomNavigationView bottomNavigationView;
    private String selectedDateKey;
    private SharedPreferences dailyPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main5);

        dailyPrefs = getSharedPreferences("DailyActivityLogs", MODE_PRIVATE);

        initViews();
        setupBottomNavigation();

        // Set default date to today
        Calendar calendar = Calendar.getInstance();
        updateSelectedDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        loadDailySummary();

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            updateSelectedDate(year, month, dayOfMonth);
            loadDailySummary();
        });
    }

    private void initViews() {
        calendarView = findViewById(R.id.calendarView);
        cvDailySummary = findViewById(R.id.cvDailySummary);
        tvSummaryDate = findViewById(R.id.tvSummaryDate);
        tvSummaryWater = findViewById(R.id.tvSummaryWater);
        tvSummaryActivity = findViewById(R.id.tvSummaryActivity);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_calendar);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_calendar) {
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
            } else if (itemId == R.id.nav_history) {
                startActivity(new Intent(this, MainActivity6.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity3.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigationView.setSelectedItemId(R.id.nav_calendar);
    }

    private void updateSelectedDate(int year, int month, int day) {
        selectedDateKey = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, day);
    }

    private void loadDailySummary() {
        String savedWater = dailyPrefs.getString(selectedDateKey + "_water", "0");
        String savedActivity = dailyPrefs.getString(selectedDateKey + "_activity", "-");

        tvSummaryDate.setText(getString(R.string.data_for_label, selectedDateKey));
        tvSummaryWater.setText(getString(R.string.total_drink_label, savedWater));
        tvSummaryActivity.setText(getString(R.string.activity_label, savedActivity));

        cvDailySummary.setVisibility(View.VISIBLE);
    }
}
